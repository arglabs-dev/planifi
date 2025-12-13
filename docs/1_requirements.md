# Requirements for Expense Management Application

## Key Features

1. **Income and Expense Management**: Detailed record of each transaction, including:

   - Amount
   - Date
   - Tags: free selection of one or multiple non-hierarchical tags.
   - Description
   - Entry method: text, voice, image
   - Attachments: Allow attaching files if the entry method has been voice or image, and also add more attachments if needed.

2. **Expense Tagging**: Flexible tagging system:
   - No hierarchical categories or subcategories.
   - Each expense can have multiple tags.
   - Tags can be created dynamically at any time.

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
   - **tags**: list of tags associated with the expense or income (can be empty and enriched later).
   - **description**: optional
   - **account**: account identifier

7. **Simplified Income and Expense Entry**: Entry happens primarily through conversational interaction exposed by the MCP server, and still supports simple methods such as:

   - Text messages
   - Photos of receipts
   - Voice messages
   - Combination of the above methods

   The backend will automatically process these entries to identify key information (amount, date, suggested tags) and confirm the transaction with the user. If the conversational client (e.g., a chat connected to the MCP) loses connectivity to the backend, movements remain pending and sync once the connection returns; any other interface (such as a future mobile app) would follow the same optional behavior. Image-based inputs must allow automatic data extraction and tag suggestions using the LLM's multimodal capabilities.

8. **Backend-first with MCP as the interface**:
   - The backend is the system core and exposes operations to register expenses/income and manage tags.
   - There is no dependency on a mobile app at this stage; any client is optional and can arrive later.
   - An **MCP (Model Context Protocol)** server sits between the backend and LLMs to expose structured actions such as creating tags or logging expenses.

9. **Conversational Interaction**:
   - Users interact in natural language.
   - The LLM interprets intent and translates conversation into structured MCP calls.
   - Conversational flow is the primary way to register movements, create tags, and query information.

10. **Cloud or Local Data**: Users can define whether the data will be stored locally only or uploaded to the cloud for access from the web portal. This point can be optional and should be discussed further during the design.

11. **Cloud or Local Backups**:

   - **Backup Frequency**: Users can define how often backups will be performed.
   - **Backup Location**: Backups can be stored locally or in the cloud (e.g., Google Drive, Dropbox).
   - **Format and Compression**: Backups should be in JSON format and compressed to optimize space.
   - **Backup Types**:
     - **Incremental**: Backups should be incremental to save space.
     - **Full**: Users can generate a full backup at any time and continue from there.
   - **Descriptive File**: Each backup should have a descriptive file that includes the date, whether it is incremental or full, and details about its contents.

12. **Alerts and Budgets**:

   - **Spending Limits by Tag**: Ability to define spending limits per tag and receive alerts when approaching those limits.
   - **Budget Plans**:
     - Users can define a budget plan for one or multiple tags and track it.
     - Each budget has a defined period: weekly, biweekly, monthly, quarterly, semiannually, or annually.
   - **Surplus or Deficit Management**:
     - The budget automatically resets at the start of each period.
     - Users can decide whether to carry over the previous period's balance (surplus or deficit) to the next period or discard it.
     - Example: If the budget is 1000 pesos biweekly and only 800 pesos are spent, users can choose to carry over the remaining 200 pesos to the next period or discard them.

13. **Mobile Compatibility**: The application must be easy to use from mobile devices, ensuring a good user experience. This is optional in the initial backend-first phase but can include:

   - **Native App**: An application installed directly on the device, offering better performance and access to specific hardware features.
   - **Mobile Web Portal**: A mobile-optimized website accessible from any browser without installation.

   Depending on user needs, a mobile web version may be sufficient, but a native app can provide a smoother experience and advanced capabilities.

14. **Data Deletion and Removal**: Users must be able to delete all stored information. To do this, a special authorization code is required, which is provided after a security check to prevent accidental deletion. This code can be obtained through a registered email or two-factor authentication, ensuring the legitimacy of the request.

15. **Data Export**: Users must be able to export all their information in different formats, such as Excel or JSON.

16. **Financial Reports**: Users have access to a list of reports that help them understand their finances and payment capacity. Some available reports include:

   - **Expense Report by Tags**: Within a specific period (monthly, biweekly, weekly, semiannual, annual, or customized with start and end dates). Includes graphs to facilitate visualization.
   - **Budget Report**: Tracking budgets by tag within a specific period.
