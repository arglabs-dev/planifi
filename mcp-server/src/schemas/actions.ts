import { z } from 'zod';

// ------------- Helper Schemas -------------

export const ExpenseSchema = z.object({
    id: z.string().uuid(),
    accountId: z.string().uuid().nullable().optional(),
    amount: z.number(),
    occurredOn: z.string().date(), // ISO date YYYY-MM-DD
    description: z.string().min(1).max(255),
    tags: z.array(z.string().max(80)).optional(), // In Tag schema it's objects, but create request uses string array? Let's check api.
    // In CreateExpenseRequest, tags is array of strings. In Expense response, tags is array of Tag objects.
    // MCP Actions usually define the INPUT for the tool.
    createdAt: z.string().datetime(),
});

export const AccountSchema = z.object({
    id: z.string().uuid(),
    name: z.string(),
    // Add other fields if needed from Account schema
});

export const TagSchema = z.object({
    id: z.string().uuid(),
    name: z.string(),
    createdAt: z.string().datetime(),
});

// ------------- Action Input Schemas -------------

/**
 * Input for createExpense action.
 * Maps to CreateExpenseRequest in OpenAPI.
 */
export const CreateExpenseInputSchema = z.object({
    accountId: z.string().uuid().nullable().optional().describe("ID of the account to charge the expense to. If null, might be unassigned."),
    amount: z.number().describe("Amount of the expense. Must be a positive number."), // OpenAPI says number format decimal
    occurredOn: z.string().date().describe("Date of the expense in YYYY-MM-DD format."),
    description: z.string().min(1).max(255).describe("Description of the expense."),
    tags: z.array(z.string().max(80)).optional().describe("List of tag names to apply."),
    createMissingTags: z.boolean().default(false).describe("Whether to create tags that don't exist."),
});

/**
 * Input for createTag action.
 * Maps to CreateTagRequest in OpenAPI.
 */
export const CreateTagInputSchema = z.object({
    name: z.string().max(80).describe("Name of the tag to create."),
});

/**
 * Input for listAccounts action.
 * Currently no parameters in OpenAPI.
 */
export const ListAccountsInputSchema = z.object({});

/**
 * Input for listExpenses action.
 * Currently no parameters in OpenAPI.
 */
export const ListExpensesInputSchema = z.object({});

export type CreateExpenseInput = z.infer<typeof CreateExpenseInputSchema>;
export type CreateTagInput = z.infer<typeof CreateTagInputSchema>;
export type ListAccountsInput = z.infer<typeof ListAccountsInputSchema>;
export type ListExpensesInput = z.infer<typeof ListExpensesInputSchema>;
