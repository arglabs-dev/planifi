import { z } from "zod";

export const correlationIdSchema = z.string().min(1).optional();
export const idempotencyKeySchema = z.string().min(8).max(255);

export const tagSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  createdAt: z.string().datetime()
});

export const accountSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  type: z.enum(["CASH", "BANK", "DEBIT_CARD", "CREDIT_CARD"]),
  currency: z.string(),
  createdAt: z.string().datetime()
});

export const expenseSchema = z.object({
  id: z.string().uuid(),
  accountId: z.string().uuid().nullable(),
  amount: z.number(),
  occurredOn: z.string(),
  description: z.string(),
  createdAt: z.string().datetime(),
  tags: z.array(tagSchema)
});

export const transactionSchema = z.object({
  id: z.string().uuid(),
  accountId: z.string().uuid(),
  amount: z.number(),
  occurredOn: z.string(),
  description: z.string(),
  createdAt: z.string().datetime(),
  tags: z.array(tagSchema)
});

export const createExpenseInputSchema = z.object({
  idempotencyKey: idempotencyKeySchema,
  correlationId: correlationIdSchema,
  accountId: z.string().uuid(),
  amount: z.number(),
  occurredOn: z.string(),
  description: z.string().min(1).max(255),
  tags: z.array(z.string().min(1).max(80)).max(25).optional(),
  createMissingTags: z.boolean().optional()
});

export const createExpenseOutputSchema = z.object({
  correlationId: correlationIdSchema,
  expense: expenseSchema
});

export const listAccountsInputSchema = z.object({
  correlationId: correlationIdSchema
});

export const listAccountsOutputSchema = z.object({
  correlationId: correlationIdSchema,
  accounts: z.array(accountSchema)
});

export const listExpensesInputSchema = z.object({
  correlationId: correlationIdSchema
});

export const listExpensesOutputSchema = z.object({
  correlationId: correlationIdSchema,
  expenses: z.array(expenseSchema)
});

export const listTagsInputSchema = z.object({
  correlationId: correlationIdSchema
});

export const listTagsOutputSchema = z.object({
  correlationId: correlationIdSchema,
  tags: z.array(tagSchema)
});

export const createTagInputSchema = z.object({
  idempotencyKey: idempotencyKeySchema,
  correlationId: correlationIdSchema,
  name: z.string().min(1).max(80)
});

export const createTagOutputSchema = z.object({
  correlationId: correlationIdSchema,
  tag: tagSchema
});

export const createAccountInputSchema = z.object({
  idempotencyKey: idempotencyKeySchema,
  correlationId: correlationIdSchema,
  name: z.string().min(1).max(150),
  type: z.enum(["CASH", "BANK", "DEBIT_CARD", "CREDIT_CARD"])
});

export const createAccountOutputSchema = z.object({
  correlationId: correlationIdSchema,
  account: accountSchema
});

export const disableAccountInputSchema = z.object({
  idempotencyKey: idempotencyKeySchema,
  correlationId: correlationIdSchema,
  accountId: z.string().uuid()
});

export const disableAccountOutputSchema = z.object({
  correlationId: correlationIdSchema,
  accountId: z.string().uuid(),
  status: z.literal("disabled")
});

export const listTransactionsInputSchema = z.object({
  correlationId: correlationIdSchema,
  accountId: z.string().uuid(),
  from: z.string(),
  to: z.string(),
  page: z.number().int().min(0).optional(),
  size: z.number().int().min(1).max(200).optional()
});

export const transactionPageSchema = z.object({
  items: z.array(transactionSchema),
  page: z.number().int().min(0),
  size: z.number().int().min(1),
  totalItems: z.number().int().min(0),
  totalPages: z.number().int().min(0)
});

export const listTransactionsOutputSchema = z.object({
  correlationId: correlationIdSchema,
  page: transactionPageSchema
});

export const createTransactionInputSchema = z.object({
  idempotencyKey: idempotencyKeySchema,
  correlationId: correlationIdSchema,
  accountId: z.string().uuid(),
  amount: z.number(),
  occurredOn: z.string(),
  description: z.string().min(1).max(255),
  tags: z.array(z.string().min(1).max(80)).max(25).optional(),
  createMissingTags: z.boolean().optional()
});

export const createTransactionOutputSchema = z.object({
  correlationId: correlationIdSchema,
  transaction: transactionSchema
});
