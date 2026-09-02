# Contact
- linwenjin0411@gmail.com

# Linova One ERP

`Linova One ERP` is a Java PC desktop ERP prototype for manufacturing companies.

The name keeps `Lin` as the project identity, uses `nova` for a clean innovation feeling, and uses `One` to express an integrated ERP workspace.

## Features

- Java Swing desktop application
- Default language: English
- UI languages: English, Simplified Chinese, Japanese
- Login page with language switcher
- License validation before the workspace opens
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

Daily desktop use should start from `LinovaOneERP.exe`. It launches `run.bat` in the background without opening a command window.

When `LinovaOneERP.exe` is opened, the launcher starts the existing `target/linova-one-erp.jar` with `javaw`. It does not run Maven during daily startup.

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

Build a delivery package:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-distribution.ps1
```

To include a local Windows JRE in the package, pass `-RuntimePath C:\path\to\jre`. The generated package is written under `build/dist/LinovaOneERP` and contains the launcher, `run.bat`, the built jar, database schema, config template, and delivery notes.

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

- `logs/YYYYMMDD/app-YYYYMMDD-HHMMSS-SSS-PID.log`: application events, sign-in flow, database query errors, uncaught exceptions
- `logs/YYYYMMDD/console-YYYYMMDD-HHMMSS-SSS-PID.log`: redirected standard output/error from `javaw`

Log files are ignored by Git, but the `logs` folder is kept in the project.
Every startup creates new UTF-8 log files, so previous runs are kept for troubleshooting and are not overwritten.
User operations are written in English with the `>>> USER_ACTION` prefix, including login, page clicks, toolbar actions, form saves, workflow confirmations, and exports.

## Desktop UI

The application uses Swing with FlatLaf. Dependencies are resolved by Maven from `pom.xml`; jar files are not committed.

Normal messages, such as sign-in success or operation completion, appear as small toast windows and close after 3 seconds. Error messages use blocking alert dialogs and stay open until confirmed.

Main navigation uses an mcframe-style three-level layout: dark root module menu, secondary business-area rail, and a function-button grid with teal section headers. Clicking a function opens a separate desktop window. `Master Data -> Master Maintenance -> Item Master Management` opens the item master window backed by `erp_item_masters`, with create, edit, delete, refresh, and CSV export actions.

## License

After a user ID and password are accepted, the login flow checks whether an active license is still within its valid date range. If no valid license exists, the login window shows a localized English, Simplified Chinese, or Japanese prompt and asks for a license key before opening the ERP workspace.

Supported license key format:

```text
LINOVA-yyyyMMdd-signature
```

The date portion is the license expiration date. The signature portion is verified with the application public key, so a plain future date is not enough to create a valid license.

```text
Example structure only: LINOVA-20271231-<signature>
```

Expired, malformed, or unsigned keys are rejected, and the login window remains open.

When MySQL is enabled, licenses are stored in `erp_licenses`. When demo mode is used without MySQL, the license is stored locally in `config/license.properties`. This local license file is ignored by Git and must not be committed.

Administrators can also open `Administration -> Security -> Roles -> License Management` / `系统管理 -> 安全权限 -> 角色 -> 许可证管理` / `システム管理 -> セキュリティ -> ロール -> ライセンス管理` to view the current license status and register a new license.

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

Copy `config/db.properties.example` to `config/db.properties` and fill in real local connection values there. Never commit `config/db.properties`, `config/license.properties`, database dumps, exported CSV files, or runtime logs.

For production MySQL connections, keep `db.allowPublicKeyRetrieval=false`. Only enable it temporarily for a controlled local compatibility test when the database authentication method requires it.

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
- `erp_function_records`
- `erp_licenses`

Most screen data shown after login now comes from MySQL. This includes module names, three-level menus, toolbar actions, process flows, KPI cards, worklist tables, focus items, and item master records.

## Initial Application User

`run.bat --init-db` no longer seeds a fixed database password. If the MySQL user table is empty, the initializer creates the first administrator account and requires an initial password.

Interactive setup:

```bat
run.bat --init-db
```

Non-interactive setup:

```bat
set LINOVA_ADMIN_PASSWORD=change-this-before-use
run.bat --init-db
set LINOVA_ADMIN_PASSWORD=
```

The initial database account is:

```text
User ID: admin
Role: System Administrator
```

The built-in demo fallback accounts are only for local development when MySQL is disabled and must not be used for production deployment.

## Dependencies

Runtime dependencies are managed by Maven:

```text
pom.xml
```

Jar files, build outputs in `build/`, and Maven outputs in `target/` are ignored by Git. After cloning, install JDK and Maven, run `run.bat --compile-only` on a build machine, copy `config/db.properties.example` to `config/db.properties`, fill in real database values, then run `run.bat --init-db` or double-click `LinovaOneERP.exe`.

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
- Reports

## Notes

- The left navigation uses a custom Swing button style so the dark ERP menu is not overridden by the Windows native button theme.
- User authentication now queries MySQL first.
- `last_login_at` is updated after a successful database login.
- `config/db.properties`, `config/license.properties`, runtime logs, exports, database dumps, local SQL data files, and jar files are intentionally not committed.
- The main workspace uses a softer light navigation and card style for a more modern ERP / AI-era feel.
- `LinovaOneERP.exe` starts `run.bat` hidden, so no command window is shown for daily desktop use.
- `run.bat` starts the existing jar for daily use and only builds with Maven when `--compile-only` is specified; it still supports `--init-db` and `--diagnose-login` for maintenance.
- `build-distribution.ps1` creates the formal delivery folder under `build/dist/` and can include a bundled Windows JRE through `-RuntimePath`.
- The application icon is loaded from `src/main/resources/com/lin/erp/ui/app-icon.png`, and the Windows launcher embeds the matching `launcher/LinovaOneERP.ico`.
- Windows Explorer can cache an old `.exe` icon after the launcher is rebuilt. Verify the actual embedded icon from the file properties or by copying the rebuilt `LinovaOneERP.exe` to a new folder/name; refresh Explorer or clear the Windows icon cache before judging delivery screenshots.
