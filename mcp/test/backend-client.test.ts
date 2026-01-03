import { beforeEach, describe, expect, it, vi } from "vitest";
import { BackendClient, BackendError } from "../src/backend-client.js";
import type { McpConfig } from "../src/config.js";
import { request } from "undici";

vi.mock("undici", () => ({
  request: vi.fn()
}));

type MockResponse = {
  statusCode: number;
  body: { text: () => Promise<string> };
};

const baseConfig: McpConfig = {
  PLANIFI_MCP_SERVER_NAME: "planifi-mcp",
  PLANIFI_MCP_SERVER_VERSION: "0.1.0",
  PLANIFI_BACKEND_BASE_URL: "http://localhost:8080/api/v1",
  PLANIFI_MCP_API_KEY: "test-key",
  PLANIFI_MCP_API_KEY_HEADER: "X-MCP-API-Key",
  PLANIFI_MCP_TIMEOUT_MS: 1000,
  PLANIFI_MCP_MAX_RETRIES: 2,
  PLANIFI_MCP_RETRY_DELAY_MS: 0
};

describe("BackendClient", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("sends required headers and parses response", async () => {
    const response: MockResponse = {
      statusCode: 201,
      body: { text: async () => JSON.stringify({ ok: true }) }
    };
    vi.mocked(request).mockResolvedValue(response as any);

    const client = new BackendClient(baseConfig);
    const result = await client.requestJson<{ ok: boolean }>({
      method: "POST",
      path: "/expenses",
      body: { name: "test" },
      correlationId: "corr-123",
      idempotencyKey: "idem-12345678",
      allowRetry: true
    });

    expect(result).toEqual({ ok: true });
    expect(request).toHaveBeenCalledTimes(1);
    const [, options] = vi.mocked(request).mock.calls[0];
    expect(options?.headers).toMatchObject({
      "X-MCP-API-Key": "test-key",
      "Idempotency-Key": "idem-12345678",
      "correlation-id": "corr-123",
      "Content-Type": "application/json",
      Accept: "application/json"
    });
  });

  it("retries on transient errors", async () => {
    const response: MockResponse = {
      statusCode: 200,
      body: { text: async () => JSON.stringify({ ok: true }) }
    };
    const transientError = new BackendError("backend error", 503, { errorCode: "TEMP" });
    vi.mocked(request)
      .mockImplementationOnce(() => {
        throw transientError;
      })
      .mockResolvedValue(response as any);

    const client = new BackendClient(baseConfig);
    const result = await client.requestJson<{ ok: boolean }>({
      method: "GET",
      path: "/accounts",
      allowRetry: true
    });

    expect(result).toEqual({ ok: true });
    expect(request).toHaveBeenCalledTimes(2);
  });
});
