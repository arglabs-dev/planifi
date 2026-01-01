import { randomUUID } from "node:crypto";
import { BackendClient, BackendError } from "./backend-client.js";
import {
  createAccountInputSchema,
  createAccountOutputSchema,
  createExpenseInputSchema,
  createExpenseOutputSchema,
  createTagInputSchema,
  createTagOutputSchema,
  createTransactionInputSchema,
  createTransactionOutputSchema,
  disableAccountInputSchema,
  disableAccountOutputSchema,
  listAccountsInputSchema,
  listAccountsOutputSchema,
  listExpensesInputSchema,
  listExpensesOutputSchema,
  listTagsInputSchema,
  listTagsOutputSchema,
  listTransactionsInputSchema,
  listTransactionsOutputSchema
} from "./schemas.js";

export type Logger = {
  info(entry: { message: string; action?: string; correlationId?: string; status?: string; latencyMs?: number; details?: Record<string, unknown> }): void;
  error(entry: { message: string; action?: string; correlationId?: string; status?: string; latencyMs?: number; details?: Record<string, unknown> }): void;
};

export type ToolDefinition = {
  name: string;
  description: string;
  inputSchema: object;
  outputSchema: object;
  handler: (input: any) => Promise<{ content: Array<{ type: "text"; text: string }>; structuredContent?: Record<string, unknown>; isError?: boolean }>;
};

const toolResult = (structuredContent: Record<string, unknown>) => ({
  content: [
    {
      type: "text" as const,
      text: JSON.stringify(structuredContent, null, 2)
    }
  ],
  structuredContent
});

const toolErrorResult = (error: unknown) => {
  const details = formatError(error);
  return {
    content: [
      {
        type: "text" as const,
        text: JSON.stringify(details, null, 2)
      }
    ],
    isError: true,
    structuredContent: details
  };
};

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

const withLogging = async <T>(logger: Logger, action: string, correlationId: string, run: () => Promise<T>) => {
  const startedAt = Date.now();
  try {
    const result = await run();
    logger.info({
      message: "mcp.action.completed",
      action,
      correlationId,
      status: "success",
      latencyMs: Date.now() - startedAt
    });
    return result;
  } catch (error) {
    const details = formatError(error);
    logger.error({
      message: "mcp.action.failed",
      action,
      correlationId,
      status: "error",
      latencyMs: Date.now() - startedAt,
      details
    });
    throw error;
  }
};

export const createPlanifiTools = (backendClient: BackendClient, logger: Logger): ToolDefinition[] => [
  {
    name: "planifi.createExpense.v1",
    description: "Crear un gasto y devolver el gasto creado.",
    inputSchema: createExpenseInputSchema,
    outputSchema: createExpenseOutputSchema,
    handler: async (input) => {
      const correlationId = input.correlationId ?? randomUUID();
      try {
        const expense = await withLogging(logger, "planifi.createExpense.v1", correlationId, () =>
          backendClient.requestJson({
            method: "POST",
            path: "/expenses",
            body: {
              accountId: input.accountId,
              amount: input.amount,
              occurredOn: input.occurredOn,
              description: input.description,
              tags: input.tags ?? [],
              createMissingTags: input.createMissingTags ?? false
            },
            idempotencyKey: input.idempotencyKey,
            correlationId,
            allowRetry: true
          })
        );
        return toolResult({ correlationId, expense: expense as Record<string, unknown> });
      } catch (error) {
        return toolErrorResult(error);
      }
    }
  },
  {
    name: "planifi.listAccounts.v1",
    description: "Listar cuentas activas.",
    inputSchema: listAccountsInputSchema,
    outputSchema: listAccountsOutputSchema,
    handler: async (input) => {
      const correlationId = input.correlationId ?? randomUUID();
      try {
        const accounts = await withLogging(logger, "planifi.listAccounts.v1", correlationId, () =>
          backendClient.requestJson({
            method: "GET",
            path: "/accounts",
            correlationId,
            allowRetry: true
          })
        );
        return toolResult({ correlationId, accounts: accounts as Record<string, unknown>[] });
      } catch (error) {
        return toolErrorResult(error);
      }
    }
  },
  {
    name: "planifi.listExpenses.v1",
    description: "Listar gastos.",
    inputSchema: listExpensesInputSchema,
    outputSchema: listExpensesOutputSchema,
    handler: async (input) => {
      const correlationId = input.correlationId ?? randomUUID();
      try {
        const expenses = await withLogging(logger, "planifi.listExpenses.v1", correlationId, () =>
          backendClient.requestJson({
            method: "GET",
            path: "/expenses",
            correlationId,
            allowRetry: true
          })
        );
        return toolResult({ correlationId, expenses: expenses as Record<string, unknown>[] });
      } catch (error) {
        return toolErrorResult(error);
      }
    }
  },
  {
    name: "planifi.listTags.v1",
    description: "Listar tags.",
    inputSchema: listTagsInputSchema,
    outputSchema: listTagsOutputSchema,
    handler: async (input) => {
      const correlationId = input.correlationId ?? randomUUID();
      try {
        const tags = await withLogging(logger, "planifi.listTags.v1", correlationId, () =>
          backendClient.requestJson({
            method: "GET",
            path: "/tags",
            correlationId,
            allowRetry: true
          })
        );
        return toolResult({ correlationId, tags: tags as Record<string, unknown>[] });
      } catch (error) {
        return toolErrorResult(error);
      }
    }
  },
  {
    name: "planifi.createTag.v1",
    description: "Crear un tag.",
    inputSchema: createTagInputSchema,
    outputSchema: createTagOutputSchema,
    handler: async (input) => {
      const correlationId = input.correlationId ?? randomUUID();
      try {
        const tag = await withLogging(logger, "planifi.createTag.v1", correlationId, () =>
          backendClient.requestJson({
            method: "POST",
            path: "/tags",
            body: { name: input.name },
            idempotencyKey: input.idempotencyKey,
            correlationId,
            allowRetry: true
          })
        );
        return toolResult({ correlationId, tag: tag as Record<string, unknown> });
      } catch (error) {
        return toolErrorResult(error);
      }
    }
  },
  {
    name: "planifi.createAccount.v1",
    description: "Crear una cuenta.",
    inputSchema: createAccountInputSchema,
    outputSchema: createAccountOutputSchema,
    handler: async (input) => {
      const correlationId = input.correlationId ?? randomUUID();
      try {
        const account = await withLogging(logger, "planifi.createAccount.v1", correlationId, () =>
          backendClient.requestJson({
            method: "POST",
            path: "/accounts",
            body: { name: input.name, type: input.type },
            idempotencyKey: input.idempotencyKey,
            correlationId,
            allowRetry: true
          })
        );
        return toolResult({ correlationId, account: account as Record<string, unknown> });
      } catch (error) {
        return toolErrorResult(error);
      }
    }
  },
  {
    name: "planifi.disableAccount.v1",
    description: "Deshabilitar una cuenta.",
    inputSchema: disableAccountInputSchema,
    outputSchema: disableAccountOutputSchema,
    handler: async (input) => {
      const correlationId = input.correlationId ?? randomUUID();
      try {
        await withLogging(logger, "planifi.disableAccount.v1", correlationId, () =>
          backendClient.requestJson({
            method: "POST",
            path: `/accounts/${input.accountId}/disable`,
            idempotencyKey: input.idempotencyKey,
            correlationId,
            allowRetry: true
          })
        );
        return toolResult({ correlationId, accountId: input.accountId, status: "disabled" });
      } catch (error) {
        return toolErrorResult(error);
      }
    }
  },
  {
    name: "planifi.listTransactions.v1",
    description: "Listar movimientos por cuenta y rango de fechas.",
    inputSchema: listTransactionsInputSchema,
    outputSchema: listTransactionsOutputSchema,
    handler: async (input) => {
      const correlationId = input.correlationId ?? randomUUID();
      try {
        const page = await withLogging(logger, "planifi.listTransactions.v1", correlationId, () =>
          backendClient.requestJson({
            method: "GET",
            path: "/transactions",
            query: {
              accountId: input.accountId,
              from: input.from,
              to: input.to,
              page: input.page,
              size: input.size
            },
            correlationId,
            allowRetry: true
          })
        );
        return toolResult({ correlationId, page: page as Record<string, unknown> });
      } catch (error) {
        return toolErrorResult(error);
      }
    }
  },
  {
    name: "planifi.createTransaction.v1",
    description: "Crear un movimiento con tags.",
    inputSchema: createTransactionInputSchema,
    outputSchema: createTransactionOutputSchema,
    handler: async (input) => {
      const correlationId = input.correlationId ?? randomUUID();
      try {
        const transaction = await withLogging(logger, "planifi.createTransaction.v1", correlationId, () =>
          backendClient.requestJson({
            method: "POST",
            path: "/transactions",
            body: {
              accountId: input.accountId,
              amount: input.amount,
              occurredOn: input.occurredOn,
              description: input.description,
              tags: input.tags ?? [],
              createMissingTags: input.createMissingTags ?? false
            },
            idempotencyKey: input.idempotencyKey,
            correlationId,
            allowRetry: true
          })
        );
        return toolResult({ correlationId, transaction: transaction as Record<string, unknown> });
      } catch (error) {
        return toolErrorResult(error);
      }
    }
  }
];
