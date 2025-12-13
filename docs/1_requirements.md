# Requirements for Expense Management Application

## Key Features

1. **Income and Expense Management**: Detailed record of each transaction, including:

   - Amount
   - Date
   - Category (selected from a list)
   - Subcategory (related to the main category)
   - Description
   - Entry method: text, voice, image
   - Attachments: Allow attaching files if the entry method has been voice or image, and also add more attachments if needed.

2. **Expense Categorization**: Provide a clear view of expenses by classifying each transaction within a main category and, optionally, a specific subcategory.

3. **Multiple Account Management**: Manage different types of accounts, such as:

   - Credit cards
   - Debit cards
   - Bank accounts
   - Cash (cash account)
   - Currency: Ability to manage accounts in different currencies like MXN, USD, or Bitcoin.
   - Account creation and deletion: Users can create, delete, or disable accounts as needed.
   - Account types: Accounts with specific characteristics, such as debit accounts, credit cards, loans, debts, or payroll accounts with scheduled income. This will help understand payment capacity for planning future expenses.

4. **Recurring Charges**: Register recurring charges to identify and monitor automatic payments, such as services and subscriptions. These charges should have options to:

   - Specify the frequency and date of charge to accounts.
   - Confirm each charge with the user (transactions are generated automatically but must be accepted by the user unless set to automatic mode).
   - End the charge according to:
     - A specific date.
     - A defined number of payments.
     - Reaching a predefined total amount.

5. **Collaborative Account Management**: Allow multiple people to manage the same account, facilitating that users (spouse, child, etc.) register their own expenses.

6. **Mass Data Import**: Import data through Excel, CSV, or JSON files with the following specific structure:

   - **transactionType**: (income or expense)
   - **amount**: transaction amount
   - **date**: in ISO 8601 format (e.g., "2024-11-25T15:30:00Z")
   - **category**: selected from a list
   - **subcategory**: related to the main category
   - **description**: optional
   - **account**: account identifier

7. **Simplified Income and Expense Entry**: The application should allow the registration of transactions through simple methods such as:

   - Text messages
   - Photos of receipts
   - Voice messages
   - Combination of the above methods

   The application will automatically process these entries to identify key information (amount, date, category) and confirm the transaction with the user.

8. **Cloud or Local Data**: Users can define whether the data will be stored locally only or uploaded to the cloud for access from the web portal. This point can be optional and should be discussed further during the design.

9. **Cloud or Local Backups**:

   - **Backup Frequency**: Users can define how often backups will be performed.
   - **Backup Location**: Backups can be stored locally or in the cloud (e.g., Google Drive, Dropbox).
   - **Format and Compression**: Backups should be in JSON format and compressed to optimize space.
   - **Backup Types**:
     - **Incremental**: Backups should be incremental to save space.
     - **Full**: Users can generate a full backup at any time and continue from there.
   - **Descriptive File**: Each backup should have a descriptive file that includes the date, whether it is incremental or full, and details about its contents.

10. **Alerts and Budgets**:

   - **Spending Limits by Category**: Ability to define spending limits for each category and receive alerts when approaching those limits.
   - **Budget Plans**:
     - Users can define a budget plan for one or multiple categories and track it.
     - Each budget has a defined period: weekly, biweekly, monthly, quarterly, semiannually, or annually.
   - **Surplus or Deficit Management**:
     - The budget automatically resets at the start of each period.
     - Users can decide whether to carry over the previous period's balance (surplus or deficit) to the next period or discard it.
     - Example: If the budget is 1000 pesos biweekly and only 800 pesos are spent, users can choose to carry over the remaining 200 pesos to the next period or discard them.

11. **Mobile Compatibility**: The application must be easy to use from mobile devices, ensuring a good user experience. This can be achieved through:

   - **Native App**: An application installed directly on the device, offering better performance and access to specific hardware features.
   - **Mobile Web Portal**: A mobile-optimized website accessible from any browser without installation.

   Depending on user needs, a mobile web version may be sufficient, but a native app can provide a smoother experience and advanced capabilities.

12. **Data Deletion and Removal**: Users must be able to delete all stored information. To do this, a special authorization code is required, which is provided after a security check to prevent accidental deletion. This code can be obtained through a registered email or two-factor authentication, ensuring the legitimacy of the request.

13. **Data Export**: Users must be able to export all their information in different formats, such as Excel or JSON.

14. **Financial Reports**: Users have access to a list of reports that help them understand their finances and payment capacity. Some available reports include:

   - **Expense Report by Category**: Within a specific period (monthly, biweekly, weekly, semiannual, annual, or customized with start and end dates). Includes graphs to facilitate visualization.
   - **Budget Report**: Tracking budgets within a specific period.
