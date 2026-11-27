# Finance Manager — Development Roadmap

Finance Manager is a **free, offline-first, privacy-focused personal finance application for Android**. Its purpose is to help individuals manage their money, understand spending habits, track financial obligations, plan savings and maintain control over their finances.

The application is built with Java, XML and Material Design 3. It uses Room/SQLite for local storage and does not require an account, cloud service, internet connection, bank integration or third-party financial service.

Every planned feature must respect the application's core principles:

- **Offline-first:** All primary functionality must work without an internet connection.
- **Privacy-focused:** Financial information stays on the user's device.
- **Account-free:** No registration, login or cloud account is required.
- **Free:** No advertisements, subscription paywalls or premium feature restrictions.
- **Reliable:** Financial calculations must use integer minor units and avoid floating-point arithmetic.
- **Accessible:** Support English and Persian, including complete RTL layouts.
- **Practical:** Features should solve real financial management problems rather than add unnecessary complexity.

---

# 1. Shipped Features

The following functionality is already implemented.

## Version 1.1.0 — Core Financial Management

### Dashboard
- Net worth overview.
- Monthly income and expense summaries.
- Comparison with the previous month.
- Spending charts.
- Budget and savings summaries.
- Recent transactions.
- Quick actions.
- Weekly and monthly chart views.
- Empty and loading states.

### Transaction Management
- Income, expense and transfer transactions.
- Transaction search.
- Filtering by type, category, account and date range.
- Edit, duplicate and delete transactions.
- Undo support for deletion.
- Transfers between accounts without incorrectly counting them as income or expenses.

### Account Management
- Cash accounts.
- Bank accounts.
- Savings accounts.
- Credit accounts.
- Wallet accounts.
- Individual account currencies.
- Account colors and icons.
- Automatically calculated balances.
- Account transaction history.

### Budget Management
- Category-based budgets.
- Spending progress indicators.
- Remaining budget calculations.
- Notifications at 50%, 80% and 100%.
- Over-budget notifications.
- Optional budget rollover.

### Savings Goals
- Target amount and date.
- Add and withdraw savings.
- Progress indicators.
- Reminders.
- Goal completion status.

### Recurring Transactions
- Daily, weekly, monthly and yearly schedules.
- Automatic generation of missed occurrences.
- Idempotent recurrence processing.
- Upcoming payment reminders.
- Pause and resume functionality.

### Reports
- Custom reporting periods.
- Income and expense summaries.
- Net cash flow.
- Category-based expense breakdown.
- Monthly spending trends.
- Pie and bar charts.

### Household Expenses
- Household profiles.
- Optional household members.
- Monthly household budgets.
- Household expense management.
- Household-specific categories linked to global categories.
- Per-category limits.
- Expense receipts and notes.
- Household spending summaries.
- Monthly and weekly reports.
- Household CSV exports.
- Recurring household expenses.

### Security and Settings
- PIN-based application lock.
- Optional biometric authentication.
- PIN change functionality.
- Configurable automatic locking.
- Screenshot blocking.
- Hide balances.
- Light, dark and system themes.
- Four accent color palettes.
- English and Persian languages.
- Notification controls.

### Data Portability
- JSON backups.
- Optional AES-256-GCM encrypted backups.
- Password-based key derivation using PBKDF2-HMAC-SHA256.
- Merge and Replace restore modes.
- Transaction CSV import and export.
- Household expense CSV export.
- Financial PDF statements.
- Backup status and timestamps.

### Currency Support
- Afghan Afghani (AFN).
- US Dollar (USD).
- Euro (EUR).
- Pakistani Rupee (PKR).
- Indian Rupee (INR).
- British Pound (GBP).
- Canadian Dollar (CAD).
- Australian Dollar (AUD).
- UAE Dirham (AED).
- Other valid three-letter ISO currency codes.

Money is stored using integer minor units and parsed with BigDecimal.

---

# 2. Version 1.2 — Everyday Experience and Productivity

**Objective:** Make daily financial management faster, more convenient and less repetitive.

This release focuses on improving existing workflows without significantly increasing application complexity.

## 2.1 Home-Screen Widgets

Introduce Android home-screen widgets that provide useful financial information without requiring users to open the application.

Planned widgets:

- **Balance Widget:** Display total balance for a selected account or reporting currency.
- **Monthly Overview Widget:** Show current month's income and expenses.
- **Budget Widget:** Display budget progress and remaining amounts.
- **Quick Add Widget:** Provide shortcuts to create income and expense transactions.

Requirements:
- Support light and dark themes.
- Follow the application's accent color.
- Refresh data when relevant transactions change.
- Respect the Hide Balances setting.
- Allow users to choose which account or information to display.
- Avoid exposing sensitive financial information unnecessarily.

## 2.2 Launcher Shortcuts

Add Android launcher shortcuts accessible by long-pressing the application icon.

Shortcuts:

- Add Expense.
- Add Income.
- Transfer Money.
- View Dashboard.

Each shortcut should navigate directly to the appropriate screen or transaction editor.

## 2.3 Transaction Templates

Allow users to save frequently used transactions as reusable templates.

A template may contain:

- Transaction name.
- Amount.
- Transaction type.
- Category.
- Preferred account.
- Description.
- Optional tags.

Examples:
- Daily transportation.
- Internet payment.
- Monthly electricity bill.
- Grocery shopping.
- Regular salary.

Requirements:
- Create, edit and delete templates.
- Create transactions from templates.
- Allow users to modify template values before saving.
- Do not automatically create transactions from templates.
- Templates must remain separate from recurring transactions.

## 2.4 Transaction Tags

Introduce optional tags to organize transactions beyond the existing category system.

Examples:
- Travel.
- Education.
- University.
- Business.
- Personal.
- Emergency.

Requirements:
- Support multiple tags per transaction.
- Create, rename and delete tags.
- Filter transactions by tags.
- Support tag-based reporting.
- Avoid duplicating category functionality.
- Include tags in backups and restore operations.

## 2.5 Custom Budget Alert Thresholds

Allow users to configure notification thresholds for individual budgets.

Examples:
- 60%.
- 75%.
- 90%.
- 100%.

Requirements:
- Support multiple thresholds per budget.
- Notify once per threshold per budget period.
- Prevent duplicate notifications.
- Allow users to disable individual thresholds.
- Preserve existing default thresholds for older budgets.

## 2.6 Duplicate Transaction Detection

Warn users when they may be entering a transaction that already exists.

Potential matching conditions:
- Same account.
- Same transaction type.
- Same amount.
- Same transaction date.
- Similar description.

Requirements:
- Show a warning before saving.
- Allow users to continue if the transaction is intentional.
- Never automatically delete or modify transactions.
- Keep duplicate detection local.
- Avoid blocking legitimate repeated transactions.

---

# 3. Version 1.3 — Financial Planning and Intelligence

**Objective:** Help users understand their financial situation and make better spending and saving decisions.

This release introduces calculations and planning tools based entirely on locally stored financial information.

## 3.1 Safe to Spend / Daily Spending Limit

Introduce a daily spending recommendation calculated from the user's available budget and upcoming financial obligations.

Example:

- Remaining budget: ؋15,000.
- Upcoming payments: ؋5,000.
- Days remaining: 20.
- Suggested daily spending amount: ؋500.

Calculation:

`(Remaining Budget - Upcoming Obligations) / Remaining Days`

Requirements:
- Calculate daily spending recommendations.
- Include upcoming recurring expenses.
- Consider the selected budget period.
- Display days remaining.
- Recalculate when transactions or scheduled payments change.
- Support category-specific and overall spending limits.
- Clearly distinguish remaining budget from actual available account balances.
- Avoid subtracting obligations that have already been included in recorded spending.

The result should be presented as a spending guideline, not a guarantee of available cash.

## 3.2 Cash-Flow Forecast

Introduce a 30-day financial forecast using existing accounts, transactions and recurring schedules.

Features:
- Current account balances.
- Expected recurring income.
- Expected recurring expenses.
- Scheduled transfers.
- Projected daily balances.
- Estimated future cash flow.
- Warnings for potentially insufficient account balances.

Requirements:
- Perform all calculations locally.
- Clearly distinguish actual and projected balances.
- Never treat unscheduled income as guaranteed.
- Allow users to inspect the transactions contributing to a projection.
- Avoid double-counting recurring transactions already generated in the ledger.

## 3.3 Financial Calendar

Introduce a calendar dedicated to financial activities.

**Prerequisite — Persian (Jalali) calendar support:** date handling currently relies entirely on `java.time` and Gregorian dates (`DateUtils`). Displaying Persian dates, Persian month names and Jalali-based periods is new work and should be delivered as its own change before, or alongside, the calendar itself.

Calendar events:
- Upcoming recurring payments.
- Expected income.
- Loan repayment dates.
- Budget period endings.
- Savings goal deadlines.
- Manually scheduled financial reminders.

Requirements:
- Monthly and weekly calendar views.
- Event details.
- Navigation to related transactions or financial records.
- Local notifications for upcoming events.
- Support Persian dates and RTL presentation where appropriate.
- Avoid creating duplicate reminders for the same event.

## 3.4 Recurring Payments Manager

Improve the existing recurring transaction functionality with a dedicated payment management screen.

This feature is broader than conventional subscription tracking.

Supported payments:
- Rent.
- Electricity.
- Water.
- Internet.
- Mobile credit and telephone bills.
- School and university fees.
- Insurance.
- Membership fees.
- Other recurring obligations.

Features:
- Active and paused payments.
- Monthly and yearly payment totals.
- Next payment date.
- Payment history.
- Payment categories.
- Price-change history.
- Upcoming payment reminders.

Requirements:
- Reuse the existing RecurringTransaction scheduler.
- Avoid creating a second recurring payment engine.
- Preserve idempotent transaction generation.
- Track historical amounts without modifying past transactions.

## 3.5 Debt and Loan Tracker

Introduce dedicated functionality for managing money borrowed and money lent.

Supported records:
- Money borrowed from another person.
- Money lent to another person.
- Personal loans.
- Interest-free loans.
- Loans with optional interest rates.

Loan information:
- Loan name or person.
- Original principal.
- Remaining balance.
- Interest rate, if applicable.
- Start date.
- Due date.
- Repayment schedule.
- Notes.

Features:
- Partial repayments.
- Repayment history.
- Outstanding balance.
- Upcoming repayment reminders.
- Completed and active loan statuses.
- Loan progress indicators.

Requirements:
- Support interest-free loans without unnecessary calculations.
- Maintain accurate repayment records.
- Distinguish money lent from money borrowed.
- Integrate repayments with the financial ledger without double-counting balances.
- Support optional interest calculations for loans that require them.

## 3.6 Financial Health Overview

Introduce a dedicated financial overview that helps users understand their overall financial condition.

Indicators:
- Monthly savings rate.
- Income versus expenses.
- Monthly net cash flow.
- Spending trends.
- Savings goal progress.
- Outstanding debts.
- Total assets and liabilities.
- Emergency savings progress.

Requirements:
- Calculate indicators using local financial data.
- Allow selectable reporting periods.
- Explain how each indicator is calculated.
- Exclude incompatible currencies from combined totals.
- Avoid presenting estimates as professional financial advice.

---

# 4. Version 2.0 — Advanced Financial Management

**Objective:** Expand the application's financial capabilities while maintaining reliability, privacy and offline operation.

This release focuses on more advanced financial workflows and data management.

## 4.1 Multi-Currency Conversion

Introduce optional manual exchange-rate management.

The application already supports multiple currencies, but balances and reports currently remain separated by currency.

Features:
- Manual exchange-rate entry.
- Base reporting currency.
- Exchange-rate history.
- Rate date.
- Optional rate description or source.
- Converted net worth.
- Converted account balances.
- Multi-currency financial reports.

Requirements:
- Work completely offline.
- Never require an exchange-rate API.
- Preserve original transaction amounts.
- Store the exchange rate used for historical conversions.
- Clearly display the original and converted amounts.
- Identify the date of the exchange rate.
- Never silently combine currencies.
- Allow users to update rates manually.

Example:

A user with:
- ؋20,000 in cash.
- $100 in a bank account.
- PKR 5,000 in savings.

Can view individual balances and an estimated combined net worth in AFN using manually configured rates.

## 4.2 Advanced CSV and Statement Import

Improve the existing CSV import functionality.

Current limitation:
The transaction importer accepts files exported by Finance Manager.

Planned improvements:
- Import CSV files from other financial applications.
- Custom column mapping.
- Saved import templates.
- Date format selection.
- Amount format selection.
- Category mapping.
- Account mapping.
- Duplicate detection during import.
- Import preview.
- Validation and error summaries.

PDF statement import may be explored as a separate enhancement.

Requirements:
- Never silently import invalid records.
- Provide a preview before committing.
- Perform database insertion transactionally.
- Preserve existing data when an import fails.
- Support common CSV formats.
- Do not require an online document-processing service.

## 4.3 Split Transactions

Allow a single purchase to be divided across multiple categories.

Example:

A grocery purchase of ؋3,000:

- Food: ؋1,500.
- Household supplies: ؋1,000.
- Personal care: ؋500.

Requirements:
- Support multiple category allocations.
- Ensure allocations equal the original transaction amount.
- Keep account balances accurate.
- Support editing and deleting split transactions.
- Include split allocations in budget calculations.
- Include split allocations in reports.
- Preserve transaction history and data integrity.

A suitable database structure should be designed before implementation, potentially using a transaction split table.

## 4.4 Household Debt Settlement

Extend the existing Household Expenses module with shared expense settlement.

Features:
- Record who paid an expense.
- Record who participated in an expense.
- Support equal and custom expense splitting.
- Calculate who owes whom.
- Display outstanding household balances.
- Record partial and full settlements.
- Maintain settlement history.

Requirements:
- Support household members.
- Avoid creating duplicate ledger entries.
- Keep expense ownership and payment responsibility clear.
- Support unequal contributions.
- Ensure calculations remain accurate when expenses are edited or deleted.

## 4.5 Receipt Attachments for Regular Transactions

Extend the existing receipt functionality to ordinary income and expense transactions.

The `transactions.receipt_path` column and its migration already exist, and backups carry the field; only the household expense editor currently exposes receipt capture. The remaining work is the attachment UI, `FileProvider` access and lifecycle handling in the regular transaction editor.

Features:
- Attach receipt images.
- View receipts.
- Replace or remove attachments.
- Support multiple receipts where appropriate.

Requirements:
- Use app-private storage.
- Open files through FileProvider.
- Avoid exposing internal storage paths.
- Include receipt files in encrypted backups when explicitly selected.
- Never include sensitive receipt images in unencrypted backups without clear user consent.
- Handle missing or corrupted receipt files safely.

## 4.6 Advanced Backup and Restore

Improve the existing backup system.

Features:
- Backup history.
- Backup integrity verification.
- Improved restore previews.
- Optional receipt inclusion.
- Backup reminders.
- Backup file information.
- Improved error reporting.

Requirements:
- Preserve AES-256-GCM encryption.
- Maintain backward compatibility with previous backup formats.
- Verify backup integrity before restoring.
- Keep restore operations transactional.
- Avoid modifying existing data if restoration fails.
- Continue using the Android Storage Access Framework.

## 4.7 Custom Budget Periods

Allow users to create budgets based on their personal income schedules.

Supported periods:
- Monthly.
- Weekly.
- Every two weeks.
- Custom date ranges.
- Monthly periods beginning on a selected day.

Requirements:
- Preserve existing budget history.
- Support rollover rules.
- Keep notifications consistent with the selected period.
- Update dashboard and reports accordingly.
- Handle overlapping periods explicitly.
- Ensure that changing a budget schedule does not rewrite historical transactions.

---

# 5. Version 2.x — Platform and User Experience Expansion

**Objective:** Improve usability across different Android devices and provide additional convenience without compromising the primary phone experience.

## 5.1 Tablet Layouts

Introduce responsive layouts for Android tablets.

Features:
- Two-pane navigation.
- Expanded dashboard.
- Side-by-side transaction lists and details.
- Improved charts.
- Better use of available screen space.

## 5.2 Wear OS Support

Explore a lightweight Wear OS companion.

Potential functionality:
- Quick expense entry.
- View selected account balances.
- View daily spending.
- Display budget progress.

Requirements:
- Keep the Android phone application as the primary financial data manager.
- Do not introduce mandatory cloud synchronization.
- Evaluate a privacy-preserving local communication approach before implementation.

## 5.3 Accessibility Improvements

Improve accessibility throughout the application.

Features:
- Screen reader support.
- Accessible chart descriptions.
- Scalable text.
- Improved contrast.
- Accessible touch targets.
- Clear error messages.
- Consistent keyboard navigation where supported.

## 5.4 Application Performance

Continue improving the application as its database grows.

Areas:
- Database query optimization.
- RecyclerView performance.
- Background processing.
- Large transaction history handling.
- Backup and restore performance.
- Chart rendering.
- Memory consumption.

---

# 6. Security, Privacy and Reliability — Ongoing

These are continuous development responsibilities, not features that should be postponed until a future major release.

## 6.1 Data Protection

- Keep financial data in app-private storage.
- Maintain secure PIN handling.
- Preserve biometric authentication as an optional secondary unlock method.
- Review encryption and key-management implementations.
- Protect exported financial information.
- Avoid logging sensitive transaction information.
- Handle application lock timeouts consistently.

## 6.2 Database Reliability

- Use non-destructive Room migrations.
- Export updated Room schemas.
- Maintain foreign-key integrity.
- Test cascading deletion behavior.
- Validate financial calculations.
- Preserve historical records.
- Avoid destructive migration fallbacks.

## 6.3 Backup Reliability

- Test backup and restore across application versions.
- Test encrypted and unencrypted backups.
- Test incorrect passwords and modified files.
- Test large backup files.
- Test interrupted restore operations.
- Verify rollback behavior after failures.

## 6.4 Testing

Every significant release should include:

- Unit tests for financial calculations.
- Database tests.
- Migration tests.
- Backup and restore tests.
- CSV import and export tests.
- Recurring transaction tests.
- Currency handling tests.
- RTL and Persian localization tests.
- Android instrumentation tests.
- Manual testing on different Android versions.

---

# 7. Not Planned

The following features are intentionally excluded because they conflict with the application's design principles.

### Bank Account Integration

Automatic bank linking and financial aggregation are not planned.

Reasons:
- Require external financial services.
- Require network access.
- May require credentials and sensitive permissions.
- Complicate the application's privacy model.

### Cloud Accounts and Synchronization

Mandatory cloud accounts and server-based synchronization are not planned.

The existing backup and restore system remains the primary data portability mechanism.

### Advertisements and Tracking

The application will not include:
- Advertising networks.
- Analytics SDKs.
- User tracking.
- Behavioral profiling.
- Third-party data collection.

### Subscription Paywalls

The application will remain free and fully functional without paid subscriptions.

### Mandatory Internet Connectivity

Core functionality must remain available without an internet connection.

### AI-Based Financial Processing

Automatic uploading of financial records to external AI services is not planned.

Any future intelligent financial assistance must prioritize local processing and explicit user consent.

---

# 8. Technical Development Guidelines

All future development should follow the existing architecture and coding conventions.

## Architecture

- Java.
- XML layouts.
- Material Design 3.
- MVVM architecture.
- Room/SQLite.
- LiveData.
- Repository pattern.
- Manual dependency injection.
- Navigation Component.
- WorkManager for background jobs.

## Database

- Room remains the single source of truth.
- All schema changes must have non-destructive migrations.
- Export updated schemas.
- Preserve backward compatibility.
- Maintain appropriate indexes and foreign keys.
- Use database transactions for operations involving multiple records.

## Financial Calculations

- Store money using integer minor units.
- Use BigDecimal for parsing and calculations requiring decimal precision.
- Never use float or double for stored or summed financial amounts.
- Do not silently combine different currencies.
- Preserve original transaction values.
- Validate all user-entered amounts.

## Localization

- English and Persian must remain synchronized.
- Support RTL layouts.
- Use localized strings rather than hardcoded text.
- Format amounts and dates according to the selected locale.
- Test Persian translations on actual Android layouts.
- Verify Persian digit and number rendering in charts, dashboards and generated PDF/CSV files, not only in form fields and labels.

## Data Portability

- Keep backups versioned.
- Maintain compatibility with previous backup formats.
- Include new entities and relationships in backup and restore logic.
- Test both Merge and Replace restore modes.
- Ensure that imports and restores do not create unintended duplicate financial records.

---

# 9. Release Planning

The following release sequence is a development target, not a fixed delivery schedule.

| Release | Focus | Priority |
|---|---|---|
| 1.1.0 | Core financial management | Shipped |
| 1.2 | Everyday experience and productivity | Next |
| 1.3 | Financial planning and intelligence | Planned |
| 2.0 | Advanced financial management | Planned |
| 2.x | Platform and user experience expansion | Later |

Features may be reordered based on user feedback, technical dependencies and testing results.

---

# 10. Feature Acceptance Criteria

A feature should not be considered complete merely because its interface has been implemented.

Before marking a feature as shipped, verify the following:

- [ ] The feature works without internet access.
- [ ] Financial calculations are accurate.
- [ ] Database changes have appropriate migrations.
- [ ] Existing records remain intact.
- [ ] Backup and restore support is implemented where applicable.
- [ ] English and Persian translations are complete.
- [ ] RTL layouts have been tested.
- [ ] Loading, empty and error states are implemented.
- [ ] Unit tests cover important business logic.
- [ ] Instrumentation tests cover relevant database or UI behavior.
- [ ] No sensitive financial information is unnecessarily exposed.
- [ ] Existing application functionality continues to work.

---

# 11. How to Propose a Feature

Feature requests and contributions should explain:

1. The problem being solved.
2. The expected user workflow.
3. The screens or modules affected.
4. Whether database changes are required.
5. Whether the feature affects existing financial calculations.
6. Whether backup and restore formats need updating.
7. How the feature maintains offline functionality.

All database migrations must be non-destructive and exported to `app/schemas/`.

English and Persian translations must remain synchronized.

Features that introduce network access, mandatory accounts, advertising, tracking or cloud dependencies will not be accepted unless they are explicitly compatible with the application's privacy principles.

---

# Project Philosophy

Finance Manager is designed to give people control over their money without requiring them to surrender their financial information to an external service.

It should remain simple enough for everyday users while providing sufficiently powerful tools for people who want more control over their finances.

**Your money. Your device. Your privacy.**
