# Finance Manager

**English** | [فارسی](README.fa.md)

A local-first personal finance app for Android, written in **Java + XML** with
Material Design 3. It tracks accounts, income, expenses, transfers, budgets,
recurring payments, savings goals, household expenses and reports. Data stays on
the device in a Room/SQLite database — there is no account, cloud service,
network API, exchange-rate feed or bundled sample data.

The app supports English and Persian (including RTL layout), and money is
handled in integer minor units with `BigDecimal` parsing. AFN is a first-class
currency alongside USD, PKR and other ISO codes.

## Requirements

| Tool | Version used |
| --- | --- |
| Android Gradle Plugin | 9.3.3 |
| Gradle wrapper | 9.5.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 (Android 8.0) |
| Java source/target | 17 |
| JDK for building | 17 or newer (built with JDK 25) |

Install Android SDK Platform 37 through Android Studio's SDK Manager. The
project uses `local.properties` (`sdk.dir=...`) like any standard Android
project; Android Studio generates it if absent.

## Build and run

```bash
# Debug APK -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# Install on a connected device/emulator
./gradlew :app:installDebug
```

On a machine without network access, add `--offline`:

```bash
./gradlew :app:assembleDebug --offline
./gradlew :app:testDebugUnitTest --offline
```

The debug APK is ~11 MB and is signed with the debug keystore.

### Release signing

A local release keystore and its credentials are already set up:

- `keystore/finance-manager-release.jks` — 4096-bit RSA key, alias
  `financemanager`, valid for 10,000 days.
- `keystore.properties` — store/key passwords and paths, readable only by the
  current user. Both this file and `keystore/` are ignored by `.gitignore`.

`app/build.gradle` loads `keystore.properties` when present and signs the
release variant with it:

```bash
./gradlew :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Verify it with:

```bash
$ANDROID_HOME/build-tools/37.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Back up the keystore and `keystore.properties` somewhere safe: losing them
means future releases cannot be signed as updates to this app. Do not commit
either file.

If `assembleRelease` stalls fetching the ~87 MB of Android lint tooling on a
slow or offline connection, package without vital lint:

```bash
./gradlew :app:assembleRelease -x lintVitalRelease
```

Run the normal `assembleRelease` once the lint dependencies are available.

## Features

Planned work and researched feature candidates are tracked in
[ROADMAP.md](ROADMAP.md).

### Everyday finance

- **Dashboard** — net worth, monthly income/expense with the change versus last
  month, spending chart, budget and savings summaries, recent transactions,
  empty/loading states, a weekly/monthly chart toggle and quick actions.
- **Transactions** — income, expense and transfer entries; search; filters by
  type, category, account and date range; edit, duplicate and delete with undo.
  A transfer is one row
  moving money between two accounts, never counted as income or expense.
- **Accounts** — cash, bank, savings, credit and wallet accounts, each with its
  own currency, colour and Material Symbol icon; computed balances; per-account
  history; account detail screen.
- **Budgets** — per-category spending limits with progress bars, remaining
  amounts and notifications at 50%, 80%, 100% and over-budget, once per
  threshold per period. A budget can optionally roll over the unspent amount of
  the budget that ends the day before it starts.
- **Savings goals** — target amount/date, add and withdraw money, progress
  ring/bar, reminders and completion state.
- **Recurring transactions** — daily/weekly/monthly/yearly schedules, automatic
  generation of missed occurrences (up to 400 catch-up entries per schedule),
  upcoming-payment reminders and pause/resume.
- **Reports** — selectable period, income/expense totals and net cash flow,
  category breakdown with pie chart and monthly trend bar chart.
- **Settings** — display name, default currency, light/dark/system theme,
  four accent palettes (Blue, Emerald, Violet, Amber) that apply to both
  themes, language (English/Persian), hide balances, app lock (PIN + optional
  biometrics) with PIN change, screenshot blocking, auto-lock delay, notification
  toggles, backup/restore and destructive-data confirmation.

### Data portability

- **PDF statement** — a printable A4 statement for the current month with a
  summary, expense breakdown and full transaction list, paginated automatically
  and limited to accounts in the reporting currency.
- **CSV import** — brings transactions back from a CSV exported by this app.
  Accounts and categories are matched by name and created when missing; rows with
  an unknown type, an unreadable date/amount or a transfer without a destination
  are skipped and counted. The whole file is inserted in one transaction.
- **Encrypted backups** — a backup can be protected with a password. Records are
  encrypted with AES-256-GCM using a PBKDF2-HMAC-SHA256 key and a random salt and
  IV; the file is detected automatically on restore. Without a password the backup
  stays readable JSON.
- **Restore modes** — **Merge** keeps current records and adds the backup;
  **Replace** deletes all current data in the same transaction and then restores,
  so restoring onto a populated database no longer duplicates totals.
- **Backup status** — the backup row shows the date and time of the last
  successful backup made on this device.

### Household expenses

Found under **More → Household expenses**. A household is a local profile layered
on the existing ledger; it is not a second accounting system.

- **Profiles** — name, optional description, creation date, optional members,
  monthly budget and default currency. New households default to the app's
  configured currency.
- **Expenses** — amount, currency, category, account, date, description, optional
  member, receipt, notes; add/edit/delete/details, search, filter and sort.
  Each expense is an ordinary `transactions` row tagged with a household, so
  balances, history, budgets and reports stay in sync with no double counting.
  Deleting a household expense deletes the linked transaction; deleting a
  household keeps its transactions in the ledger.
- **Categories** — household categories link existing global categories; names
  are never duplicated. Each link can have its own monthly limit.
- **Budget** — total monthly budget plus per-category limits, spending,
  remaining and percent used, over-limit categories, previous-month comparison
  and Material 3 progress indicators.
- **Overview** — spending and remaining this month, percent of budget used,
  top categories, recent expenses, upcoming recurring payments, monthly trend
  and empty states.
- **Reports** — weekly, monthly and custom ranges, two-month comparison,
  category spending, trends and CSV export.
- **Recurring household expenses** — reuse the existing `RecurringTransaction`
  scheduler and notifications, including idempotent generation.
- **Receipts** — copied to app-private `files/receipts` and opened through a
  `FileProvider` (`${applicationId}.fileprovider`); they are not part of JSON
  backups.

## Architecture

MVVM with manual dependency injection, one Activity and Navigation Component
fragments. ViewModels expose `LiveData`; all Room access runs on a shared
`ExecutorService`, with results posted back to the main thread.

```
app/src/main/java/com/financemanager/app/
├── FinanceManagerApp.java        Application, owns AppContainer
├── di/AppContainer               database, repository, settings, notifications
├── data/
│   ├── entity/                    10 Room entities, including households
│   ├── dao/                       DAOs (LiveData and synchronous queries)
│   ├── FinanceDatabase.java       Room database, schema v3
│   ├── Migration1To2.java         non-destructive v1 -> v2 migration
│   ├── Migration2To3.java         non-destructive v2 -> v3 migration
│   ├── FinanceRepository.java     single data facade used by ViewModels
│   └── Converters.java            enum <-> TEXT converters
├── models/                        enums, query projections, HouseholdTotals
├── ui/
│   ├── common/                    ViewModelFactory, pickers, chart helpers
│   ├── dashboard|transactions|accounts|budgets|categories|
│   │   savings|recurring|reports|settings|data|more|about|lock
│   ├── household/                 profiles, expenses, categories, budget, reports
│   └── MainActivity.java          bottom navigation + NavHost
├── security/                      SessionManager, SettingsRepository, biometrics
├── notifications/                 channel setup, budget alert checker
├── workers/                       WorkManager jobs and scheduler
└── utils/                         Money, DateUtils, CSV import/export, BackupCrypto,
                                   backup/report writers, DataEraser
```

Room is the only source of truth. There is no fake or hardcoded financial data:
first launch seeds only default categories and one cash account, using the
configured default currency with a zero opening balance.

## Money and currency rules

- Money is stored as `long` **minor units** (cents). `float`/`double` are never
  used for stored or summed amounts.
- Input is parsed with `BigDecimal`, accepting `1234.56`, `1,234.56`,
  `1.234,56` and `1,234` styles, then rounded half-up to cents once.
- Each account has a currency. The picker includes USD, EUR, GBP, **AFN
  (Afghan Afghani, `؋`)**, PKR, INR, CAD, AUD, JPY and AED, plus any valid
  three-letter ISO code. AFN amounts display the `؋` sign.
- The default currency drives account seeding, new households and new expense
  editors. The editor prefers an account in the household's currency and warns
  when a different-currency account is selected.
- Totals and reports are computed for one currency at a time, so different
  currencies are never silently added together. Household totals include only
  expenses in the household's currency; foreign-currency rows are labeled, not
  restated or converted. Household CSV export likewise writes one currency.
- Transfers are one row with `account_id` (source) and `to_account_id`
  (destination). They are neutral in income/expense reports.
- Account balance =
  `initial_balance_minor + income − expense − transfers out + transfers in`,
  computed in SQL over the ledger.

## Privacy and security

- No analytics, network permission or cloud sync. Entry is manual and the app
  works offline; there are no bank, payment or exchange-rate APIs.
- The optional app lock uses a 4–6 digit PIN hashed with PBKDF2-HMAC-SHA256
  (120,000 iterations, random salt) and stored in `EncryptedSharedPreferences`;
  the plain PIN is never persisted.
- `BiometricPrompt` can be enabled only after a PIN exists; biometrics are a
  secondary unlock method, with the PIN as fallback.
- Auto-lock can be configured after a delay; the app returns to the lock screen
  when the session expires.
- Exports and backups are written only to a user-selected Storage Access
  Framework location.
- Backups can optionally be encrypted with AES-256-GCM; the key is derived from a
  user password with PBKDF2-HMAC-SHA256 (120,000 iterations, random salt) and is
  never stored. Wrong passwords and modified files fail authentication instead of
  returning partial data.

## Database and migrations

Room schema version 3 is exported to
`app/schemas/com.financemanager.app.data.FinanceDatabase/1.json`, `2.json` and
`3.json`. See [docs/DATABASE.md](docs/DATABASE.md) for tables, columns, keys and
deletion behaviour.

`Migration1To2` is a real, non-destructive migration. It adds `households`,
`household_members` and `household_categories`, and rebuilds `transactions` and
`recurring_transactions` with nullable `household_id`, `household_category_id`
and `household_member_id` foreign keys plus `transactions.receipt_path`, copying
every existing row unchanged.

`Migration2To3` adds the optional `budgets.rollover` flag with
`DEFAULT 0`, so existing budgets keep their exact limits and no data is
rewritten. Destructive migrations are not enabled; any future schema change
must ship a similar migration.

## Tests

Unit tests (JVM, no device required) — 51 tests covering money parsing and
formatting, date bounds, CSV escaping/output and import parsing, encrypted backup
round-trips/wrong passwords/tamper detection, recurrence advancement,
transaction and household filtering, household money formatting and
per-currency totals. CI runs the unit tests and builds the debug APK on every
push and pull request (`.github/workflows/android.yml`).

```bash
./gradlew :app:testDebugUnitTest
# offline: add --offline
```

Instrumentation tests — 3 tests: the app smoke test and household database
tests covering household relationships, one-ledger-row expenses, per-currency
totals, category budgets and idempotent recurring household entries:

```bash
./gradlew :app:connectedDebugAndroidTest
```

If Gradle cannot download the Unified Test Platform artifacts on an offline
machine, install both APKs and run the tests directly:

```bash
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r \
  com.financemanager.app.test/androidx.test.runner.AndroidJUnitRunner
```

To run a single class, add `-e class com.financemanager.app.SmokeTest` (or
`com.financemanager.app.HouseholdDatabaseTest`) before the package name.

## Backup format

A plain backup (`finance-manager-backup-YYYY-MM-DD.json`) is a single JSON
document with a `format` marker, `version: 2`, app version, export timestamp and
arrays for accounts, categories, transactions, budgets, savings goals, recurring
transactions, households, household members and household categories. Each
record keeps the same field names as the database columns. Version 1 backups
remain restorable; their household arrays are simply absent.

When a password is supplied the same JSON is encrypted and saved as
`finance-manager-backup-YYYY-MM-DD.fmb`. The envelope starts with `FMBENC1`,
followed by the PBKDF2 iteration count, base64 salt, IV and AES-256-GCM
ciphertext (which includes the authentication tag). Restore detects the marker
and asks for the password; a wrong password or a modified file is rejected.

Restore offers two modes. **Merge** inserts new accounts and households (using
unique names such as `Cash (2)`), reuses existing categories with the same name
and type, and remaps foreign keys, so restoring the same backup twice duplicates
records. **Replace** deletes every current record and then applies the backup,
both inside one Room transaction, so a failed restore rolls back to the previous
data. Receipt image files are not included in either kind of backup.

CSV exports are separate files for transactions, reports and household
expenses, all written through the Storage Access Framework. The transaction CSV
can be imported again from **Backup & restore → Import transactions**; import is
additive, so importing the same file twice duplicates rows.

Separate CSV exports are available for transactions, reports and household
expenses; all are written through the Storage Access Framework.

## Known limitations

- No currency conversion or exchange rates; totals intentionally exclude
  accounts or household expenses recorded in another currency.
- Merge restore (and CSV import) is additive: applying the same file twice
  duplicates accounts, households or transactions. Use **Replace** restore when
  the goal is to restore one exact snapshot.
- Receipt attachments exist only for household expenses; regular transactions
  cannot attach one. Receipts are stored only on the device and are not backed
  up, encrypted or otherwise. Backups made before version 2 contain no household
  profiles.
- Budget rollover uses one adjacent predecessor only: the previous budget for
  the same category must end exactly the day before the current one starts, and
  its own rolled-over amount is not compounded.
- No home-screen widget or launcher shortcuts; new entries start from the app.
- PDF statements cover the current month only; an arbitrary date range is not
  selectable yet.
- Deleting an account also deletes its transactions through the foreign-key
  cascade. The confirmation dialog shows the affected transaction count.
- Deleting a category reassigns its transactions to the built-in "Other"
  category of the same type; default categories cannot be deleted. The final
  account cannot be deleted.
- Notifications require the system notification permission and are subject to
  WorkManager's approximate scheduling; they are not exact alarms.
- Release signing uses a locally generated keystore, but Play Store packaging
  (bundle, upload key, store listing) is not configured.
- Release builds enable R8 with `app/proguard-rules.pro`; the release APK builds
  successfully, but minified runtime behaviour has not been exercised on a device
  yet.
- Lint could not run offline in the development environment; unit tests,
  instrumentation tests and manual emulator checks were still run. Pre-existing
  lint warnings remain: `GestureBackNavigation`, `NewApi` and `DiffUtilEquals`.

## Verification notes

The latest build passed `:app:assembleDebug --offline`,
`:app:testDebugUnitTest --offline` (51 tests, including the new
`BackupCryptoTest` and `CsvImportParserTest`) and
`:app:assembleRelease --offline -x lintVitalRelease` with R8 enabled. Earlier
instrumentation runs passed all three tests via `adb shell am instrument`; the
new backup/import screens have not been exercised on a device yet. Manual checks
on a Persian/RTL emulator with AFN confirmed the dashboard quick action
(`مصرف هزینه`), household profiles, AFN budget/remaining values and the household
expense list header (`۰ تراکنش · ؋۰٫۰۰`).
