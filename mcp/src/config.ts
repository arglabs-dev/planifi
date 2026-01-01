import { z } from "zod";

const configSchema = z.object({
  PLANIFI_MCP_SERVER_NAME: z.string().default("planifi-mcp"),
  PLANIFI_MCP_SERVER_VERSION: z.string().default("0.1.0"),
  PLANIFI_BACKEND_BASE_URL: z.string().url().default("http://localhost:8080/api/v1"),
  PLANIFI_MCP_API_KEY: z.string().min(1),
  PLANIFI_MCP_API_KEY_HEADER: z.string().min(1).default("X-MCP-API-Key"),
  PLANIFI_MCP_TIMEOUT_MS: z.coerce.number().int().positive().default(10000),
  PLANIFI_MCP_MAX_RETRIES: z.coerce.number().int().min(0).max(5).default(2),
  PLANIFI_MCP_RETRY_DELAY_MS: z.coerce.number().int().min(100).default(250)
});

export type McpConfig = z.infer<typeof configSchema>;

export const config: McpConfig = configSchema.parse(process.env);
