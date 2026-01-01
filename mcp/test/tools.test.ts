import { describe, expect, it, vi } from "vitest";
import { createPlanifiTools } from "../src/tools.js";
import type { BackendClient } from "../src/backend-client.js";

const logger = {
  info: vi.fn(),
  error: vi.fn()
};

describe("createPlanifiTools", () => {
  it("creates expense tool and forwards payload to backend client", async () => {
    const requestJson = vi.fn().mockResolvedValue({ id: "expense-1" });
    const backendClient = { requestJson } as unknown as BackendClient;
    const tools = createPlanifiTools(backendClient, logger);

    const tool = tools.find((entry) => entry.name === "planifi.createExpense.v1");
    expect(tool).toBeDefined();

    const result = await tool!.handler({
      idempotencyKey: "idem-12345678",
      correlationId: "corr-1",
      accountId: "11111111-1111-1111-1111-111111111111",
      amount: 12.5,
      occurredOn: "2024-01-01",
      description: "Coffee",
      tags: ["food"],
      createMissingTags: true
    });

    expect(requestJson).toHaveBeenCalledWith({
      method: "POST",
      path: "/expenses",
      body: {
        accountId: "11111111-1111-1111-1111-111111111111",
        amount: 12.5,
        occurredOn: "2024-01-01",
        description: "Coffee",
        tags: ["food"],
        createMissingTags: true
      },
      idempotencyKey: "idem-12345678",
      correlationId: "corr-1",
      allowRetry: true
    });

    expect(result.structuredContent).toEqual({
      correlationId: "corr-1",
      expense: { id: "expense-1" }
    });
  });
});
