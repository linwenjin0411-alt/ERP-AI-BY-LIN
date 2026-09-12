# 联系方式
- linwenjin0411@gmail.com

# Linova One ERP

语言：[English](README.md) | [简体中文](README.zh-CN.md) | [日本語](README.ja.md)

`Linova One ERP` 是面向制造业公司的 Java PC 桌面 ERP 原型系统。

名称中的 `Lin` 保留项目身份，`nova` 表达清爽、创新的感觉，`One` 表达一体化 ERP 工作空间。

## 功能特性

- Java Swing 桌面应用
- 默认语言：英文
- UI 语言：英文、简体中文、日文
- 带语言切换的登录画面
- 打开工作台前进行 License 校验，支持在线 API 验证和离线签名 License 回退
- 登录后的 ERP Cockpit 工作台
- 参考 SAP / mcframe 的模块和业务流程结构
- 基于 MySQL 的用户、公司、角色、菜单、角色菜单权限表
- 基于 MySQL 的模块页面、动作、流程步骤、KPI 卡片、工作列表列、工作列表行和关注事项
- 面向采购、销售、制造、库存、BOM、业务伙伴、仓库的独立业务表
- 根据影响库存的业务单据自动生成库存台账
- 业务状态主数据、统一状态流转模型和操作审计日志基础
- 查询、报表、AI、库存台账页面为只读，并支持筛选、排序、刷新和导出
- MRP、生产成本、审批队列、报表和 AI 上下文页面
- 自动数据库初始化
- 通过 `LinovaOneERP.exe` 一键启动

## 系统概要

Linova One ERP 是面向制造业业务的桌面 ERP 原型。当前覆盖主数据、采购、销售、库存、制造、财务、报表、AI 辅助查询、系统管理和 License 管理。

系统采用模块化工作台设计。用户登录并通过 License 校验后，从 mcframe 风格的多级菜单进入各功能页面。功能页面保持统一的 CRUD 体验，同时在可用范围内使用业务专用字段和业务专用表。

核心业务数据存储在 MySQL 中。当前模型包括品目主数据、BOM、客户/供应商主数据、仓库主数据、采购单据、销售单据、制造单据、库存记录、自动生成的库存台账、业务状态、状态流转和操作审计日志。

## 运行

安装 JDK 8 或更高版本，以及 Apache Maven 3.8 或更高版本。

首次构建时，Maven 会下载 `pom.xml` 中声明的开源依赖：

- FlatLaf
- MySQL Connector/J

`target/` 和 `build/` 下的生成文件已被 Git 忽略，可以随时重新生成。

静默桌面启动：

```text
Double-click LinovaOneERP.exe
```

日常桌面使用建议从 `LinovaOneERP.exe` 启动。它会在后台启动 `run.bat`，不会打开命令行窗口。

打开 `LinovaOneERP.exe` 时，启动器会用 `javaw` 启动现有的 `target/linova-one-erp.jar`。日常启动不会运行 Maven。

维护用命令行启动：

```bat
run.bat
```

仅编译：

```bat
run.bat --compile-only
```

重新构建 EXE 启动器：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-launcher.ps1
```

构建交付包：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-distribution.ps1
```

如需在交付包中包含本地 Windows JRE，可传入 `-RuntimePath C:\path\to\jre`。生成的交付目录位于 `build/dist/LinovaOneERP`，包含启动器、`run.bat`、已构建 jar、数据库 schema、配置模板和交付说明。

诊断登录和模块加载：

```bat
run.bat --diagnose-login
```

仅初始化 MySQL 表：

```bat
run.bat --init-db
```

## 日志

运行日志保存在本地：

```text
logs/YYYYMMDD/
```

常用文件：

- `logs/YYYYMMDD/app-YYYYMMDD-HHMMSS-SSS-PID.log`：应用事件、登录流程、数据库查询错误、未捕获异常
- `logs/YYYYMMDD/console-YYYYMMDD-HHMMSS-SSS-PID.log`：`javaw` 重定向后的标准输出/错误

日志文件已被 Git 忽略，但项目中保留 `logs` 文件夹。
每次启动都会创建新的 UTF-8 日志文件，因此历史运行日志会保留用于故障排查，不会被覆盖。
用户操作会以英文写入日志，并带有 `>>> USER_ACTION` 前缀，包括登录、页面点击、工具栏动作、表单保存、流程确认和导出。

## 桌面 UI

应用使用 Swing 和 FlatLaf。依赖由 Maven 从 `pom.xml` 解析，jar 文件不提交到仓库。

普通消息，例如登录成功或操作完成，会以小型 toast 窗口显示，并在 3 秒后关闭。错误消息使用阻塞式提示框，直到用户确认为止。

主导航采用 mcframe 风格的三级布局：深色根模块菜单、二级业务区域导航，以及带青绿色区域标题的功能按钮网格。点击功能会打开独立桌面窗口。`Master Data -> Master Maintenance -> Item Master Management` 会打开由 `erp_item_masters` 支撑的品目主数据窗口，支持新增、编辑、删除、刷新和 CSV 导出。

业务页面使用统一的 mcframe 风格工作画面：工具栏动作、可搜索可排序表格、右侧业务上下文、单据编号规则、组织/期间上下文和 CSV 导出。查询、报表、AI、库存台账页面按设计为只读。

## License

用户 ID 和密码通过后，登录流程会在打开 ERP 工作台前校验 License。如果不存在有效 License，登录窗口会显示英文、简体中文或日文的本地化提示，并要求输入 License key。

License 校验支持两种模式：

- 通过 `license.verifyApiUrl` 进行在线 API 验证
- 离线签名 License 验证

正式部署时，在以下文件中配置在线验证 API：

```text
config/license.properties
```

示例：

```text
license.verifyApiUrl=https://your-license-server.example/api.php?action=verify
license.cacheDays=30
license.timeoutMs=5000
```

应用会自动追加 `product_code=LinovaOneERP`、`user_code`，并在用户输入 License 时追加 `license_key`。

启用 MySQL 时，License 记录会缓存在 `erp_licenses`。如果存在 active 且未过期的 License，应用仍会通过配置的 API 再次确认，然后才允许登录。如果不存在有效 License 或验证失败，登录窗口会要求用户输入 License key。

离线签名 License 使用以下格式：

```text
LINOVA-yyyyMMdd-signature
```

日期部分是 License 到期日。签名部分会使用应用内置公钥验证，因此只写一个未来日期不能生成有效 License。

```text
Example structure only: LINOVA-20271231-<signature>
```

已过期、格式错误或未签名的 key 会被拒绝，登录窗口保持打开。

`config/license.properties` 是本地 License 配置文件。它可以保存 API endpoint，也可以在 demo/offline 模式下保存本地缓存的 License key。该本地 License 文件已被 Git 忽略，不能提交。

管理员也可以打开 License 页面查看当前 License 状态并登记新的 License：

```text
Administration -> Security -> Roles -> License Management
系统管理 -> 安全权限 -> 角色 -> 许可证管理
システム管理 -> セキュリティ -> ロール -> ライセンス管理
```

## MySQL

应用从以下文件读取本地数据库设置：

```text
config/db.properties
```

该文件包含敏感信息，因此已被 Git 忽略。提交到仓库的模板是：

```text
config/db.properties.example
```

本地数据库配置示例：

```text
host: localhost
port: 3306
database: linova_erp
username: YOUR_DB_USER
```

将 `config/db.properties.example` 复制为 `config/db.properties`，并填写真实的本地连接值。不要提交 `config/db.properties`、`config/license.properties`、数据库 dump、导出的 CSV 文件或运行日志。

生产 MySQL 连接应保持 `db.allowPublicKeyRetrieval=false`。只有在受控的本地兼容性测试中，且数据库认证方式确实需要时，才临时开启。

生产 MySQL 连接应保持 `db.useSsl=true`，并根据部署环境配置数据库服务器证书和信任设置。只有在隔离的本地数据库不支持 TLS 时，才设置为 `false`。

schema 文件是：

```text
database/schema.mysql.sql
```

schema 包含以下主要表组：

- 安全：`erp_users`、`erp_roles`、`erp_role_menus`
- 组织和菜单：`erp_companies`、`erp_menus`、`erp_modules`
- 模块元数据：`erp_module_actions`、`erp_module_process_steps`、`erp_module_metrics`、`erp_module_table_columns`、`erp_module_table_rows`、`erp_module_focus_items`
- 主数据：`erp_item_masters`、`erp_business_partners`、`erp_warehouse_masters`、`erp_bom_components`
- 业务单据：`erp_purchase_documents`、`erp_purchase_document_lines`、`erp_sales_documents`、`erp_sales_document_lines`、`erp_manufacturing_documents`、`erp_inventory_records`
- 库存移动：`erp_inventory_movements`
- 流程和审计：`erp_business_statuses`、`erp_business_status_transitions`、`erp_business_operation_logs`
- 通用 fallback 和 License：`erp_function_records`、`erp_licenses`

登录后的大多数导航、模块元数据、品目主数据、License 数据和核心业务工作列表记录会持久化到 MySQL。部分原型默认数据仍会在业务表为空时自动生成。

## 初始应用用户

`run.bat --init-db` 不再写入固定数据库密码。如果 MySQL 用户表为空，初始化器会创建第一个管理员账号，并要求设置初始密码。

交互式设置：

```bat
run.bat --init-db
```

非交互式设置：

```bat
set LINOVA_ADMIN_PASSWORD=change-this-before-use
run.bat --init-db
set LINOVA_ADMIN_PASSWORD=
```

初始数据库账号：

```text
User ID: admin
Role: System Administrator
```

内置 demo fallback 账号仅用于 MySQL 禁用时的本地开发，不能用于生产部署。

## 依赖

运行依赖由 Maven 管理：

```text
pom.xml
```

jar 文件、`build/` 构建输出和 `target/` Maven 输出已被 Git 忽略。克隆后，在构建机器上安装 JDK 和 Maven，运行 `run.bat --compile-only`，将 `config/db.properties.example` 复制为 `config/db.properties`，填写真实数据库值，然后运行 `run.bat --init-db` 或双击 `LinovaOneERP.exe`。

## 模块

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

## 备注

- 左侧导航使用自定义 Swing 按钮样式，避免深色 ERP 菜单被 Windows 原生按钮主题覆盖。
- 用户认证优先查询 MySQL。
- 成功数据库登录后会更新 `last_login_at`。
- `config/db.properties`、`config/license.properties`、运行日志、导出文件、数据库 dump、本地 SQL 数据文件和 jar 文件不会提交。
- 主工作台使用更柔和的浅色导航和卡片风格，以贴近现代 ERP / AI 时代的观感。
- `LinovaOneERP.exe` 会隐藏启动 `run.bat`，因此日常桌面使用不会显示命令行窗口。
- `run.bat` 默认启动现有 jar。Maven 构建只通过 `run.bat --compile-only` 执行。
- `run.bat` 还支持 `--init-db` 和 `--diagnose-login` 维护命令。
- `build-distribution.ps1` 会在 `build/dist/` 下创建正式交付目录，并可通过 `-RuntimePath` 打包 Windows JRE。
- 应用使用 Java 绘制的自定义 Linova ERP 图标，因此标题栏不再使用默认 Java 图标。
- Windows Explorer 可能会缓存旧的 `.exe` 图标。判断交付截图前，请从文件属性验证实际嵌入图标，或将重建后的 `LinovaOneERP.exe` 复制到新文件夹/新名称；必要时刷新 Explorer 或清理 Windows 图标缓存。

## 截图

以下截图展示登录流程、License 提示、主工作台和主数据菜单页面。

<img width="1040" height="650" alt="001_login_sign-in" src="https://github.com/user-attachments/assets/d0637711-584e-43a0-89e2-e69220d53314" />
<img width="1040" height="650" alt="072_login_with_license_prompt" src="https://github.com/user-attachments/assets/005db6da-dff0-4833-a900-d99c9f613cee" />
<img width="1455" height="880" alt="002_main_dashboard" src="https://github.com/user-attachments/assets/a3b2add1-81c3-427e-88ac-9aba64ad2687" />
<img width="1455" height="880" alt="003_main_master-master-maint" src="https://github.com/user-attachments/assets/9380d098-0f62-41da-b84f-18a1c6c26594" />
