# Database schema (Room v3)

Database file: `finance_manager.db`
Room version: **3** (`exportSchema = true`)
Schema JSON: `app/schemas/com.financemanager.app.data.FinanceDatabase/1.json`,
`2.json` and `3.json`

All timestamps/dates are stored as `INTEGER` epoch milliseconds in local time
(midnight for date-only fields). Enums are stored as `TEXT` names by
`Converters` (`INCOME`, `EXPENSE`, `TRANSFER`, `CASH`, `BANK`, `SAVINGS`,
`CREDIT`, `WALLET`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`). Money columns end
in `_minor` and are `INTEGER` cents.

## Relationships

```
categories 1 ──< budgets >── 1 categories
categories 1 ──< transactions (category_id, nullable)
categories 1 ──< recurring_transactions (category_id, nullable)
accounts   1 ──< transactions (account_id; to_account_id nullable, transfers)
accounts   1 ──< recurring_transactions (account_id; to_account_id nullable)
budgets    1 ──< budget_alerts
recurring_transactions 1 ──< transactions (recurring_id, nullable)
```

Foreign keys are declared in Room but are not deferred. Account/category
deletion is handled in the repository so history is not silently orphaned:
category deletes reassign transactions to the "Other" category of the same
type, and the last account cannot be deleted.

## Tables

### `accounts`

| Column | Type | Constraints / notes |
| --- | --- | --- |
| `id` | INTEGER | PK, autoincrement |
| `name` | TEXT | not null, **unique index** |
| `type` | TEXT | `AccountType`, default `CASH` |
| `initial_balance_minor` | INTEGER | opening balance in cents |
| `currency` | TEXT | ISO code, default `USD` |
| `color` | TEXT | hex colour, default `#3E6DB5` |
| `icon` | TEXT | Material Symbol key, default `account_cash` |
| `created_at` | INTEGER | epoch ms |
| `updated_at` | INTEGER | epoch ms |

Balance is derived, never stored:

```sql
initial_balance_minor
+ SUM(INCOME  where account_id = account)
- SUM(EXPENSE where account_id = account)
- SUM(TRANSFER where account_id = account)
+ SUM(TRANSFER where to_account_id = account)
```

### `categories`

| Column | Type | Constraints / notes |
| --- | --- | --- |
| `id` | INTEGER | PK, autoincrement |
| `name` | TEXT | not null |
| `type` | TEXT | `INCOME` or `EXPENSE` |
| `icon` | TEXT | Material Symbol key |
| `color` | TEXT | hex colour |
| `is_default` | INTEGER | boolean 0/1; default categories cannot be deleted |
| `created_at` | INTEGER | epoch ms |

Unique index on `(name, type)`. The app seeds default income/expense categories
plus an "Other" category for each type on first launch.

### `transactions`

| Column | Type | Constraints / notes |
| --- | --- | --- |
| `id` | INTEGER | PK, autoincrement |
| `title` | TEXT | payee/description |
| `amount_minor` | INTEGER | positive cents; sign comes from `type` |
| `type` | TEXT | `INCOME`, `EXPENSE`, `TRANSFER` |
| `category_id` | INTEGER | FK → `categories.id`, `ON DELETE SET NULL`, nullable |
| `account_id` | INTEGER | FK → `accounts.id`, `ON DELETE CASCADE`, not null |
| `to_account_id` | INTEGER | FK → `accounts.id`, `ON DELETE CASCADE`, nullable; transfer destination |
| `date` | INTEGER | epoch ms at local midnight |
| `description` | TEXT | optional |
| `notes` | TEXT | optional |
| `recurring_id` | INTEGER | FK → `recurring_transactions.id`, `ON DELETE SET NULL`, nullable; marks generated entries |
| `created_at` | INTEGER | epoch ms |
| `updated_at` | INTEGER | epoch ms |

Indices: `account_id`, `to_account_id`, `category_id`, `date`, `type`,
`recurring_id`. A transfer is exactly one row: `account_id` is the source,
`to_account_id` the destination. Transfers are excluded from income/expense
aggregates but included in account balances.

### `budgets`

| Column | Type | Constraints / notes |
| --- | --- | --- |
| `id` | INTEGER | PK, autoincrement |
| `name` | TEXT | display name |
| `category_id` | INTEGER | FK → `categories.id`, `ON DELETE CASCADE` |
| `amount_minor` | INTEGER | limit in cents |
| `start_date` | INTEGER | period start, epoch ms |
| `end_date` | INTEGER | period end, epoch ms |
| `rollover` | INTEGER | boolean, `DEFAULT 0`; adds the unspent amount of the adjacent preceding budget |
| `created_at` | INTEGER | epoch ms |
| `updated_at` | INTEGER | epoch ms |

Indices: `category_id`, `start_date`. Spending is summed from `transactions`
of type `EXPENSE` in the budget's category and date range.

### `budget_alerts`

Records which threshold was already notified for a budget period, preventing
duplicate notifications.

| Column | Type | Constraints / notes |
| --- | --- | --- |
| `id` | INTEGER | PK, autoincrement |
| `budget_id` | INTEGER | FK → `budgets.id`, `ON DELETE CASCADE` |
| `period_key` | TEXT | identifies the budget period |
| `threshold` | INTEGER | percentage threshold that was notified |
| `notified_at` | INTEGER | epoch ms |

Unique index on `(budget_id, period_key, threshold)`.

### `savings_goals`

| Column | Type | Constraints / notes |
| --- | --- | --- |
| `id` | INTEGER | PK, autoincrement |
| `name` | TEXT | goal name |
| `target_minor` | INTEGER | target amount in cents |
| `current_minor` | INTEGER | saved amount in cents |
| `target_date` | INTEGER | target date, epoch ms |
| `description` | TEXT | optional |
| `icon` | TEXT | Material Symbol key |
| `color` | TEXT | hex colour |
| `created_at` | INTEGER | epoch ms |
| `updated_at` | INTEGER | epoch ms |

Add/withdraw actions update `current_minor` directly; contributions are not
kept as a separate ledger.

### `recurring_transactions`

| Column | Type | Constraints / notes |
| --- | --- | --- |
| `id` | INTEGER | PK, autoincrement |
| `title` | TEXT | payee/title copied to generated transactions |
| `amount_minor` | INTEGER | positive cents |
| `type` | TEXT | `INCOME`, `EXPENSE`, `TRANSFER` |
| `category_id` | INTEGER | FK → `categories.id`, `ON DELETE SET NULL`, nullable |
| `account_id` | INTEGER | FK → `accounts.id`, `ON DELETE CASCADE` |
| `to_account_id` | INTEGER | FK → `accounts.id`, `ON DELETE CASCADE`, nullable |
| `frequency` | TEXT | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY` |
| `next_run_date` | INTEGER | next occurrence, epoch ms at local midnight |
| `description` | TEXT | optional |
| `notes` | TEXT | optional |
| `is_active` | INTEGER | boolean 0/1; inactive schedules are not processed |
| `created_at` | INTEGER | epoch ms |
| `updated_at` | INTEGER | epoch ms |

Indices: `account_id`, `to_account_id`, `category_id`, `next_run_date`.

`RecurringWorker` runs at startup and daily. For each active schedule whose
`next_run_date` is on or before today, it inserts a transaction only if one
does not already exist for `(recurring_id, date)`, advances `next_run_date`,
and repeats until the schedule is in the future (guard: 400 catch-up entries
per run).

## Migrations

Version 1 is the initial schema. Version 2 adds local household support via
`Migration1To2`:

- New tables `households`, `household_members`, `household_categories` (the
  latter links existing `categories` rows instead of duplicating them).
- `transactions` and `recurring_transactions` are rebuilt with nullable
  `household_id`, `household_category_id`, `household_member_id` columns and
  real foreign keys; `transactions` also gains `receipt_path`. Every existing
  row is copied unchanged.

Version 3 adds optional budget rollover via `Migration2To3`:

- `budgets` gains `rollover INTEGER NOT NULL DEFAULT 0`, so existing budgets
  keep their limits and no rows are rewritten.
- Carryover itself is not stored: `BudgetDao` derives it from the adjacent
  preceding budget for the same category, which is why editing or deleting a
  period immediately updates the next one.

The database builder does not call `fallbackToDestructiveMigration()`. Any
future schema change must bump `version` and provide a `Migration` that
preserves existing financial records.
