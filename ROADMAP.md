# Finance Manager — Roadmap

Finance Manager is deliberately **offline-first and account-free**: no bank
linking, no cloud sync, no analytics, and no network permission. Every item
below respects that constraint. Features that require sending financial data to
a server are listed under [Not planned](#not-planned).

The priorities were checked against public comparisons of Android budgeting
apps for 2026 — offline trackers such as Ledgerly, Spend, Pocket Clear and
Money Vault, and mainstream apps such as YNAB, PocketGuard, Monarch and Rocket
Money. The recurring themes are fast entry, a "safe to spend" number,
subscription and debt awareness, and strong offline reporting. Where the app
already covers a theme (budgets with rollover, recurring schedules, savings
goals, household expenses, encrypted backups) it is marked as shipped instead
of repeated here.

## Shipped

- **1.1.0** — accounts with per-account currency, transfers, categories,
  budgets with optional rollover, savings goals, recurring transactions,
  reports, household expenses with receipts and monthly budgets, PIN/biometric
  lock, encrypted backups, CSV import/export, PDF statement, English/Persian,
  and light/OLED-dark themes.
- **Themes** — four accent palettes (Blue, Emerald, Violet, Amber) applied to
  both light and dark, plus follow-system mode.

## Next — 1.2 (quality of daily use)

These are low-risk additions to the existing schema and screens.

1. **Home-screen widgets** — a balance/this-month widget and a quick-add
   widget. Research consistently ranks widgets as the difference between an
   app people keep and one they forget; the app currently has neither, and
   there are no launcher shortcuts.
2. **Launcher shortcuts and quick-add sheet** — long-press shortcuts for
   "Add expense", "Add income" and "Transfer", opening the editor directly.
3. **Transaction templates** — save a frequent entry (amount, category,
   account, note) and reuse it in one tap. Complements recurring transactions,
   which are schedule-driven rather than manual.
4. **Tags** — an optional free-form tag per transaction, with filtering in the
   transaction list and reports. Useful for trips and projects that cut across
   categories.
5. **Custom budget alert thresholds** — today alerts fire at fixed 50/80/100%
   levels. Let each budget choose its own thresholds (for example 75% and
   90%) and alert once per threshold per period.

## Next — 1.3 (cash-flow confidence)

1. **Safe to spend / daily cap** — one number derived from remaining budget,
   upcoming recurring bills and days left in the period, recalculated as
   transactions land. This is the headline feature of PocketGuard and Spend
   and directly extends the dashboard and budget engine.
2. **Cash-flow forecast** — a 30-day projection combining current account
   balances with scheduled recurring income and expenses, shown as a line on
   the dashboard or reports screen.
3. **Custom budget periods** — budgets currently use explicit start/end dates.
   Add pay-cycle presets (monthly on a chosen day, every two weeks, weekly) so
   the dashboard, budgets, alerts and reports follow the user's payday.
4. **Subscription manager** — derive a subscriptions list from recurring
   expenses, show monthly and yearly cost, next charge date, and price-change
   history. Rocket Money popularised this; the recurring model already holds
   most of the data.
5. **Debt and loan tracker** — record a loan with principal, rate and term,
   show an amortisation schedule, remaining balance, and payoff progress.
   Frequent in offline-first competitors and a natural extension of accounts
   (credit accounts already exist but have no payoff view).

## Later — 2.0 (breadth)

1. **Receipt attachments for regular transactions** — the household module
   already stores receipt images; extend the same private-storage flow to
   income/expense entries, with an option to include receipts in backups.
2. **Currency conversion** — today totals intentionally exclude accounts in
   another currency. Add an optional manual exchange-rate table (no network
   calls) so net worth and reports can include all accounts, with the rate
   and date visible wherever a converted total appears.
3. **Statement and CSV import improvements** — map arbitrary bank CSV columns
   with a saved template, and add PDF statement import. CSV import currently
   only accepts files exported by this app.
4. **Household debt settlement** — extend household expenses with "who paid"
   and "who owes whom", then settle balances. Research shows bill splitting is
   a top reason people install a second finance app.
5. **Split transactions** — one purchase split across several categories,
   stored as child rows or a split table, so reports stay accurate.
6. **Wear OS / tablet layouts** — a dashboard tile and a two-pane tablet
   layout. Lower priority than the phone experience.

## Not planned

- **Bank account linking / aggregation** — requires network access, a
  third-party aggregator and credentials, which breaks the app's privacy
  model.
- **Cloud accounts and multi-device sync** — backups through a user-chosen
  file or storage provider remain the sync story.
- **Ads, analytics or tracking SDKs**.
- **Subscription paywall** — the app stays free and complete offline.

## How to propose an item

Open an issue describing the workflow, the screens involved, and whether it
needs a schema migration. Migrations must be non-destructive and exported to
`app/schemas/`, and English/Persian strings must stay in sync.
