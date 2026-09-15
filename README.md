# Contact
- linwenjin0411@gmail.com

# Linova One ERP

Languages: **English** | [日本語](README.ja.md) | [简体中文](README.zh-CN.md)

`Linova One ERP` is a Java PC desktop ERP prototype for manufacturing companies.

The name keeps `Lin` as the project identity, uses `nova` for a clean innovation feeling, and uses `One` to express an integrated ERP workspace.

## Features

- Java Swing desktop application
- Default language: English
- UI languages: English, Japanese, Simplified Chinese
- Login page with language switcher
- License validation before the workspace opens. Database mode requires online API verification
- ERP cockpit after login
- SAP / mcframe inspired modules and process flow
- MySQL-backed user, company, role, menu, and role-menu tables
- MySQL-backed module pages, actions, process steps, KPI cards, worklist columns, worklist rows, and focus items
- Independent business tables for procurement, sales, manufacturing, inventory, BOM, partners, and warehouses
- Generated inventory ledger for stock-changing business documents
- Business status master, workflow transition model, and operation audit log foundation
- Read-only query, report, AI, and stock-ledger pages with filter, sort, refresh, and export
- MRP, production costing, approval queue, report, and AI context pages
- Automatic database initialization
- One-click startup with `LinovaOneERP.exe`

## System Overview

Linova One ERP is a desktop ERP prototype focused on manufacturing operations. It covers master data, procurement, sales, inventory, manufacturing, finance, reports, AI-assisted inquiry, administration, and license management.

The system is designed as a modular workspace. Users sign in, pass license validation, then work from an mcframe-style multi-level menu. Function pages share a consistent CRUD experience while using business-specific fields and tables where available.

Core business data is stored in MySQL. The current model includes item master, BOM, customer/supplier master, warehouse master, procurement documents, sales documents, manufacturing documents, inventory records, generated inventory ledger, business statuses, workflow transitions, and operation audit logs.

## Run

Install JDK 8 or newer and Apache Maven 3.8 or newer.

On the first run, Maven downloads the open-source dependencies declared in `pom.xml`:

- FlatLaf
- MySQL Connector/J

Generated files under `target/` and `build/` are build outputs and can be recreated at any time.

Silent desktop startup:

```text
Double-click LinovaOneERP.exe
```

Daily desktop use should start from `LinovaOneERP.exe`. It launches `run.bat` in the background without opening a command window and shows a small startup window while the app is being prepared.

When `LinovaOneERP.exe` is opened, the launcher starts `run.bat`. If `target/linova-one-erp.jar` is missing after a fresh clone, `run.bat` builds it with Maven first; this can take a few minutes on the first run. If startup fails before the app window opens, the launcher shows an error and writes a startup log under `logs/YYYYMMDD/`.

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

Log files are written locally, while the `logs` folder remains part of the project layout.
Every startup creates new UTF-8 log files, so previous runs are kept for troubleshooting and are not overwritten.
User operations are written in English with the `>>> USER_ACTION` prefix, including login, page clicks, toolbar actions, form saves, workflow confirmations, and exports.

## Desktop UI

The application uses Swing with FlatLaf. Dependencies are resolved by Maven from `pom.xml`; application jars are generated during build.

Normal messages, such as sign-in success or operation completion, appear as small toast windows and close after 3 seconds. Error messages use blocking alert dialogs and stay open until confirmed.

Main navigation uses an mcframe-style three-level layout: dark root module menu, secondary business-area rail, and a function-button grid with teal section headers. Clicking a function opens a separate desktop window. `Master Data -> Master Maintenance -> Item Master Management` opens the item master window backed by `erp_item_masters`, with create, edit, delete, refresh, and CSV export actions.

Business pages use a unified mcframe-style work screen: toolbar actions, searchable and sortable tables, side context, document numbering rule, organization/period context, and CSV export. Query, report, AI, and inventory ledger pages are read-only by design.

## License

After a user ID and password are accepted, the login flow validates the license before opening the ERP workspace. If no valid license exists, the login window shows a localized English, Simplified Chinese, or Japanese prompt and asks for a license key.

Database mode requires online API verification through `license.verifyApiUrl`. A local demo license is used only when MySQL is disabled.

For normal deployment, configure the online verification API in:

```text
config/license.properties
```

Example:

```text
license.verifyApiUrl=https://your-license-server.example/api.php?action=verify
license.cacheDays=30
license.timeoutMs=5000
```

The application automatically appends `product_code=LinovaOneERP`, `user_code`, and, when entered by the user, `license_key`.

When MySQL is enabled, license records are stored in `erp_licenses`, but they are not used as an offline grant. If a stored key exists, the application confirms that exact key with the configured API before allowing login. If no key exists or verification fails, the login window asks the user to enter a license key and still requires the API to return `ok=true`, a matching product, and a non-expired `expires_at`.

The legacy offline signature verifier remains in the codebase for compatibility review, but it is not a database-mode release path. Historical signed licenses used this format:

```text
LINOVA-yyyyMMdd-signature
```

The date portion is the license expiration date. The signature portion is verified with the application public key, so a plain future date is not enough to create a valid license. Online rejection is not bypassed by this compatibility verifier.

```text
Example structure only: LINOVA-20271231-<signature>
```

Expired, malformed, or unsigned keys are rejected, and the login window remains open.

`config/license.properties` is the local license configuration file. It can hold the API endpoint and, in demo/offline mode, a locally cached license key. Treat this file as environment-specific runtime configuration.

Administrators can also open the license page to view the current license status and register a new license:

```text
Administration -> Security -> Roles -> License Management
系统管理 -> 安全权限 -> 角色 -> 许可证管理
システム管理 -> セキュリティ -> ロール -> ライセンス管理
```

## MySQL

The application reads local database settings from:

```text
config/db.properties
```

This file stores environment-specific connection values. The template is:

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

Copy `config/db.properties.example` to `config/db.properties` and fill in real local connection values there.

For production MySQL connections, keep `db.allowPublicKeyRetrieval=false`. Only enable it temporarily for a controlled local compatibility test when the database authentication method requires it.

For production MySQL connections, keep `db.useSsl=true` and configure the database server certificate/trust settings as required by the deployment environment. Set it to `false` only for an isolated local database that does not support TLS.

The schema file is:

```text
database/schema.mysql.sql
```

The schema includes these main table groups:

- Security: `erp_users`, `erp_roles`, `erp_role_menus`
- Organization and menu: `erp_companies`, `erp_menus`, `erp_modules`
- Module metadata: `erp_module_actions`, `erp_module_process_steps`, `erp_module_metrics`, `erp_module_table_columns`, `erp_module_table_rows`, `erp_module_focus_items`
- Master data: `erp_item_masters`, `erp_business_partners`, `erp_warehouse_masters`, `erp_bom_components`
- Business documents: `erp_purchase_documents`, `erp_purchase_document_lines`, `erp_sales_documents`, `erp_sales_document_lines`, `erp_manufacturing_documents`, `erp_inventory_records`
- Inventory movement: `erp_inventory_movements`
- Workflow and audit: `erp_business_statuses`, `erp_business_status_transitions`, `erp_business_operation_logs`
- Generic fallback and license: `erp_function_records`, `erp_licenses`

Most navigation, module metadata, item master data, license data, and core business worklist records are persisted in MySQL. Some prototype defaults are still seeded automatically when business tables are empty.

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

Application jars, build outputs in `build/`, and Maven outputs in `target/` are generated locally. After cloning, install JDK and Maven, then run `run.bat` for local demo startup. For database deployment, edit the generated `config/db.properties`, fill in real database values, configure `config/license.properties`, then run `run.bat --init-db`.

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
- Environment-specific configuration, runtime logs, exports, database dumps, local SQL data files, and generated jars are local runtime artifacts.
- The main workspace uses a softer light navigation and card style for a more modern ERP / AI-era feel.
- `LinovaOneERP.exe` starts `run.bat` hidden, so no command window is shown for daily desktop use.
- `run.bat` normally starts the existing jar. Maven build is only run through `run.bat --compile-only`.
- `run.bat` also supports `--init-db` and `--diagnose-login` for maintenance.
- `build-distribution.ps1` creates the formal delivery folder under `build/dist/` and can include a bundled Windows JRE through `-RuntimePath`.
- The application uses a custom Linova ERP icon drawn in Java, so title bars no longer use the default Java icon.
- Windows Explorer can cache an old `.exe` icon after the launcher is rebuilt. Verify the actual embedded icon from the file properties or by copying the rebuilt `LinovaOneERP.exe` to a new folder/name; refresh Explorer or clear the Windows icon cache before judging delivery screenshots.

## Screenshots

The following screenshots show the sign-in flow, license prompt, main dashboard, and master menu page.

<img width="1040" height="650" alt="001_login_sign-in" src="https://github.com/user-attachments/assets/d0637711-584e-43a0-89e2-e69220d53314" />
<img width="1040" height="650" alt="072_login_with_license_prompt" src="https://github.com/user-attachments/assets/005db6da-dff0-4833-a900-d99c9f613cee" />
<img width="1455" height="880" alt="002_main_dashboard" src="https://github.com/user-attachments/assets/a3b2add1-81c3-427e-88ac-9aba64ad2687" />
<img width="1455" height="880" alt="003_main_master-master-maint" src="https://github.com/user-attachments/assets/9380d098-0f62-41da-b84f-18a1c6c26594" />


