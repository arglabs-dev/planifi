import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { BackendClient, BackendError } from "./backend-client.js";
import { config } from "./config.js";
import { logger } from "./logger.js";
import { createPlanifiTools } from "./tools.js";

const backendClient = new BackendClient(config);

const server = new McpServer({
  name: config.PLANIFI_MCP_SERVER_NAME,
  version: config.PLANIFI_MCP_SERVER_VERSION
});

const formatError = (error: unknown) => {
  if (error instanceof BackendError) {
    return {
      error: {
        message: error.message,
        statusCode: error.statusCode,
        errorCode: error.errorCode,
        traceId: error.traceId
      }
    };
  }

  if (error instanceof Error) {
    return { error: { message: error.message } };
  }

  return { error: { message: "Unknown error" } };
};

const tools = createPlanifiTools(backendClient, logger);
tools.forEach((tool) => {
  server.registerTool(
    tool.name,
    {
      description: tool.description,
      inputSchema: tool.inputSchema,
      outputSchema: tool.outputSchema
    },
    tool.handler
  );
});

const start = async () => {
  await server.connect(new StdioServerTransport());
  logger.info({
    message: "mcp.server.started",
    status: "listening"
  });
};

start().catch((error) => {
  logger.error({
    message: "mcp.server.failed",
    status: "error",
    details: formatError(error)
  });
  process.exit(1);
});
