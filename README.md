# Linova One ERP

`Linova One ERP` is a Java PC desktop ERP prototype for manufacturing companies.

The name keeps `Lin` as the project identity, uses `nova` for a clean innovation feeling, and uses `One` to express an integrated ERP workspace.

## Features

- Java Swing desktop application
- Default language: English
- UI languages: English, Simplified Chinese, Japanese
- Login page with language switcher
- ERP cockpit after login
- SAP / mcframe inspired modules and process flow
- MySQL-backed user, company, role, menu, and role-menu tables
- MySQL-backed module pages, actions, process steps, KPI cards, worklist columns, worklist rows, and focus items
- Automatic database initialization
- One-click startup with `LinovaOneERP.exe`

## Run

Install JDK 8 or newer and Apache Maven 3.8 or newer.

On the first run, Maven downloads the open-source dependencies declared in `pom.xml`:

- FlatLaf
- MySQL Connector/J

Generated files under `target/` and `build/` are ignored by Git and can be recreated at any time.

Silent desktop startup:

```text
Double-click LinovaOneERP.exe
```

Fallback script startup:

```text
Double-click LinovaOneERP.vbs
```

Daily desktop use should start from `LinovaOneERP.exe`. It launches the hidden VBS desktop starter without opening a command window. `run.bat` is kept mainly for maintenance commands.

When `LinovaOneERP.exe` or `LinovaOneERP.vbs` is opened, the launcher runs Maven in the background, builds `target/linova-one-erp.jar`, then starts the desktop app with `javaw`.

Maintenance command-line startup:

```bat
run.bat
```

Compile only:

```bat
run.bat --compile-only
```

Rebuild the EXE launcher:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-launcher.ps1
```

Diagnose login and module loading:

```bat
run.bat --diagnose-login
```

Initialize MySQL tables only:

```bat
run.bat --init-db
```

## Logs

Runtime logs are stored locally in:

```text
logs/YYYYMMDD/
```

Useful files:

- `logs/YYYYMMDD/startup-YYYYMMDD-HHMMSS.log`: hidden launcher compile/startup output
- `logs/YYYYMMDD/app-YYYYMMDD-HHMMSS-SSS-PID.log`: application events, sign-in flow, database query errors, uncaught exceptions
- `logs/YYYYMMDD/console-YYYYMMDD-HHMMSS-SSS-PID.log`: redirected standard output/error from `javaw`

Log files are ignored by Git, but the `logs` folder is kept in the project.
Every startup creates new UTF-8 log files, so previous runs are kept for troubleshooting and are not overwritten.
User operations are written in English with the `>>> USER_ACTION` prefix, including login, page clicks, toolbar actions, form saves, workflow confirmations, and exports.

## Desktop UI

The application uses Swing with FlatLaf. Dependencies are resolved by Maven from `pom.xml`; jar files are not committed.

Normal messages, such as sign-in success or operation completion, appear as small toast windows and close after 3 seconds. Error messages use blocking alert dialogs and stay open until confirmed.

Main navigation uses an mcframe-style three-level layout: dark root module menu, secondary business-area rail, and a function-button grid with teal section headers. Clicking a function opens a separate desktop window. `Master Data -> Master Maintenance -> Item Master Management` opens the item master window backed by `erp_item_masters`, with create, edit, delete, refresh, and CSV export actions.

## MySQL

The application reads local database settings from:

```text
config/db.properties
```

This file is ignored by Git because it contains secrets. The committed template is:

```text
config/db.properties.example
```

Example local database settings:

```text
host: localhost
port: 3306
database: linova_erp
username: YOUR_DB_USER
```

Copy `config/db.properties.example` to `config/db.properties` and fill in real local connection values there. Never commit `config/db.properties`, database dumps, exported CSV files, or runtime logs.

The schema file is:

```text
database/schema.mysql.sql
```

The program creates these initial tables:

- `erp_companies`
- `erp_roles`
- `erp_users`
- `erp_menus`
- `erp_role_menus`
- `erp_modules`
- `erp_module_actions`
- `erp_module_process_steps`
- `erp_module_metrics`
- `erp_module_table_columns`
- `erp_module_table_rows`
- `erp_module_focus_items`
- `erp_item_masters`

Most screen data shown after login now comes from MySQL. This includes module names, three-level menus, toolbar actions, process flows, KPI cards, worklist tables, focus items, and item master records.

## Demo Application Users

These users are seeded into MySQL by `run.bat --init-db`:

```text
User ID: admin
Password: admin123
Role: System Administrator
```

```text
User ID: planner
Password: plan123
Role: Production Planner
```

## Dependencies

Runtime dependencies are managed by Maven:

```text
pom.xml
```

Jar files in `lib/`, build outputs in `build/`, and Maven outputs in `target/` are ignored by Git. After cloning, install JDK and Maven, copy `config/db.properties.example` to `config/db.properties`, fill in real database values, then run `run.bat --init-db` or double-click `LinovaOneERP.exe`.

## Modules

- Cockpit
- Master Data
- Procurement
- Sales
- Inventory
- Manufacturing
- Finance
- AI Assistant
- Administration

## Notes

- The left navigation uses a custom Swing button style so the dark ERP menu is not overridden by the Windows native button theme.
- User authentication now queries MySQL first.
- `last_login_at` is updated after a successful database login.
- `config/db.properties`, runtime logs, exports, database dumps, local SQL data files, and jar files are intentionally not committed.
- The main workspace uses a softer light navigation and card style for a more modern ERP / AI-era feel.
- `LinovaOneERP.vbs` builds with Maven in the background and starts the login window with `javaw`, so no command window is shown for daily desktop use.
- `run.bat` delegates normal startup to the VBS launcher when possible; `--compile-only` and `--init-db` still show command-line output for maintenance.
- The application uses a custom Linova ERP icon drawn in Java, so title bars no longer use the default Java icon.
