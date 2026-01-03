import { request } from "undici";
import { setTimeout as sleep } from "node:timers/promises";
import { McpConfig } from "./config.js";

export type BackendRequestOptions = {
  method: "GET" | "POST";
  path: string;
  query?: Record<string, string | number | undefined>;
  body?: unknown;
  correlationId?: string;
  idempotencyKey?: string;
  allowRetry?: boolean;
};

export class BackendError extends Error {
  readonly statusCode: number;
  readonly errorCode?: string;
  readonly traceId?: string;
  readonly responseBody?: unknown;

  constructor(message: string, statusCode: number, responseBody?: unknown) {
    super(message);
    this.name = "BackendError";
    this.statusCode = statusCode;
    this.responseBody = responseBody;
    if (responseBody && typeof responseBody === "object") {
      const body = responseBody as Record<string, unknown>;
      this.errorCode = typeof body.errorCode === "string" ? body.errorCode : undefined;
      this.traceId = typeof body.traceId === "string" ? body.traceId : undefined;
    }
  }
}

export class BackendClient {
  private readonly baseUrl: string;
  private readonly apiKey: string;
  private readonly apiKeyHeader: string;
  private readonly timeoutMs: number;
  private readonly maxRetries: number;
  private readonly retryDelayMs: number;

  constructor(config: McpConfig) {
    this.baseUrl = config.PLANIFI_BACKEND_BASE_URL;
    this.apiKey = config.PLANIFI_MCP_API_KEY;
    this.apiKeyHeader = config.PLANIFI_MCP_API_KEY_HEADER;
    this.timeoutMs = config.PLANIFI_MCP_TIMEOUT_MS;
    this.maxRetries = config.PLANIFI_MCP_MAX_RETRIES;
    this.retryDelayMs = config.PLANIFI_MCP_RETRY_DELAY_MS;
  }

  async requestJson<T>(options: BackendRequestOptions): Promise<T> {
    const attempts = options.allowRetry ? this.maxRetries + 1 : 1;
    let lastError: unknown;

    for (let attempt = 0; attempt < attempts; attempt += 1) {
      try {
        return await this.requestOnce<T>(options);
      } catch (error) {
        lastError = error;
        if (!options.allowRetry || !this.shouldRetry(error) || attempt >= attempts - 1) {
          throw error;
        }
        await sleep(this.retryDelayMs * (attempt + 1));
      }
    }

    throw lastError ?? new Error("Unknown backend error");
  }

  private shouldRetry(error: unknown): boolean {
    if (error instanceof BackendError) {
      return [408, 429, 500, 502, 503, 504].includes(error.statusCode);
    }
    return true;
  }

  private async requestOnce<T>(options: BackendRequestOptions): Promise<T> {
    const url = this.buildUrl(options.path, options.query);
    const headers: Record<string, string> = {
      Accept: "application/json",
      [this.apiKeyHeader]: this.apiKey
    };

    if (options.correlationId) {
      headers["correlation-id"] = options.correlationId;
    }

    let body: string | undefined;
    if (options.body !== undefined) {
      headers["Content-Type"] = "application/json";
      body = JSON.stringify(options.body);
    }

    if (options.idempotencyKey) {
      headers["Idempotency-Key"] = options.idempotencyKey;
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.timeoutMs);

    try {
      const response = await request(url, {
        method: options.method,
        headers,
        body,
        signal: controller.signal
      });
      const responseText = await response.body.text();
      const parsedBody = this.parseJson(responseText);

      if (response.statusCode >= 400) {
        throw new BackendError(
          `Backend responded with status ${response.statusCode}`,
          response.statusCode,
          parsedBody
        );
      }

      return parsedBody as T;
    } finally {
      clearTimeout(timeout);
    }
  }

  private buildUrl(path: string, query?: Record<string, string | number | undefined>): string {
    const normalizedBase = this.baseUrl.endsWith("/") ? this.baseUrl : `${this.baseUrl}/`;
    const normalizedPath = path.replace(/^\//, "");
    const url = new URL(normalizedPath, normalizedBase);

    if (query) {
      Object.entries(query).forEach(([key, value]) => {
        if (value !== undefined) {
          url.searchParams.set(key, String(value));
        }
      });
    }

    return url.toString();
  }

  private parseJson(payload: string): unknown {
    if (!payload) {
      return null;
    }
    try {
      return JSON.parse(payload);
    } catch {
      return { raw: payload };
    }
  }
}
