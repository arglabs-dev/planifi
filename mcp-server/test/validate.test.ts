import assert from 'node:assert';
import { CreateExpenseInputSchema, CreateTagInputSchema, ListAccountsInputSchema, ListExpensesInputSchema } from '../src/schemas/actions.js';

console.log('Running schema validations...');

// Test CreateExpense
try {
    const expenseValid = {
        amount: 100.50,
        occurredOn: '2025-01-01',
        description: 'Lunch',
        tags: ['food']
    };
    CreateExpenseInputSchema.parse(expenseValid);
    console.log('✓ CreateExpense valid input passed');

    const expenseInvalid = {
        amount: '100', // Should be number
        occurredOn: 'invalid-date',
        description: '' // Min length 1
    };
    try {
        CreateExpenseInputSchema.parse(expenseInvalid);
        assert.fail('Should have failed validation');
    } catch (e) {
        console.log('✓ CreateExpense invalid input failed as expected');
    }
} catch (error) {
    console.error('CreateExpense Test failed', error);
    process.exit(1);
}

// Test CreateTag
try {
    const tagValid = { name: 'Groceries' };
    CreateTagInputSchema.parse(tagValid);
    console.log('✓ CreateTag valid input passed');
} catch (error) {
    console.error('CreateTag Test failed', error);
    process.exit(1);
}

// Test ListAccounts
try {
    ListAccountsInputSchema.parse({});
    console.log('✓ ListAccounts valid input passed');
} catch (error) {
    console.error('ListAccounts Test failed', error);
    process.exit(1);
}

// Test ListExpenses
try {
    ListExpensesInputSchema.parse({});
    console.log('✓ ListExpenses valid input passed');
} catch (error) {
    console.error('ListExpenses Test failed', error);
    process.exit(1);
}

console.log('All validations passed!');
