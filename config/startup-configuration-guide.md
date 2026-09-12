# Startup Configuration Guide

Languages: **English** | 日本語 | 简体中文

## English

This project supports a zero-manual-configuration demo startup when Java and Maven are installed.

Demo startup:

1. Download or clone the Git project.
2. Double-click `LinovaOneERP.exe`, or run `run.bat`.
3. On first startup, `run.bat` automatically creates local configuration files from the templates when they are missing.
4. If `target/linova-one-erp.jar` is missing, `run.bat` automatically builds it with Maven.
5. Sign in with the demo account.

```text
admin / admin123
```

Demo mode is controlled only by `config/db.properties`:

```properties
db.enabled=false
db.fallbackToDemo=true
```

In demo mode, the login flow still shows the license input window when no local demo license exists. The entered license text is accepted locally without calling the license API, then the user enters the ERP workspace.

Formal database mode:

```properties
db.enabled=true
```

When `db.enabled=true`, the application must verify the license through the configured license API. Demo license bypass is not used in database mode.

Configure the formal license API in `config/license.properties`:

```properties
license.verifyApiUrl=https://your-license-server.example/api.php?action=verify
license.cacheDays=30
license.timeoutMs=5000
```

The application appends `product_code=LinovaOneERP`, `user_code`, and the entered `license_key` automatically.

For MySQL startup, edit `config/db.properties`:

```properties
db.enabled=true
db.host=localhost
db.port=3306
db.database=linova_erp
db.username=YOUR_DB_USER
db.password=YOUR_DB_PASSWORD
```

Then initialize the database:

```bat
run.bat --init-db
```

## 日本語

このプロジェクトは、Java と Maven がインストールされていれば、手動設定なしのデモ起動に対応します。

デモ起動：

1. Git プロジェクトをダウンロード、または clone します。
2. `LinovaOneERP.exe` をダブルクリックするか、`run.bat` を実行します。
3. 初回起動時、ローカル設定ファイルが存在しない場合は、`run.bat` がテンプレートから自動作成します。
4. `target/linova-one-erp.jar` が存在しない場合は、`run.bat` が Maven で自動ビルドします。
5. デモアカウントでログインします。

```text
admin / admin123
```

デモモードは `config/db.properties` だけで制御します。

```properties
db.enabled=false
db.fallbackToDemo=true
```

デモモードでは、ローカルのデモ License が存在しない場合、ログイン時に License 入力画面を表示します。入力された License 文字列は License API を呼び出さずにローカルで成功扱いになり、そのまま ERP ワークスペースへ進みます。

正式なデータベースモード：

```properties
db.enabled=true
```

`db.enabled=true` の場合、アプリケーションは設定済みの License API で必ず License を検証します。データベースモードではデモ用の License バイパスは使用しません。

正式な License API は `config/license.properties` に設定します。

```properties
license.verifyApiUrl=https://your-license-server.example/api.php?action=verify
license.cacheDays=30
license.timeoutMs=5000
```

アプリケーションは `product_code=LinovaOneERP`、`user_code`、入力された `license_key` を自動的に追加します。

MySQL で起動する場合は、`config/db.properties` を編集します。

```properties
db.enabled=true
db.host=localhost
db.port=3306
db.database=linova_erp
db.username=YOUR_DB_USER
db.password=YOUR_DB_PASSWORD
```

その後、データベースを初期化します。

```bat
run.bat --init-db
```

## 简体中文

本项目在已安装 Java 和 Maven 的情况下，支持无需手动配置的演示启动。

演示启动：

1. 下载或 clone Git 项目。
2. 双击 `LinovaOneERP.exe`，或执行 `run.bat`。
3. 首次启动时，如果本地配置文件不存在，`run.bat` 会自动从模板创建。
4. 如果 `target/linova-one-erp.jar` 不存在，`run.bat` 会自动使用 Maven 构建。
5. 使用演示账号登录。

```text
admin / admin123
```

演示模式只由 `config/db.properties` 控制：

```properties
db.enabled=false
db.fallbackToDemo=true
```

演示模式下，如果本地还没有 demo license，登录时仍然会弹出 License 输入窗口。用户输入任意 License 文本后，程序不会请求 License API，而是在本地直接判定成功并进入 ERP 工作台。

正式数据库模式：

```properties
db.enabled=true
```

当 `db.enabled=true` 时，程序必须通过配置的 License API 校验 License。数据库模式不会使用演示 License 跳过逻辑。

正式 License API 配置在 `config/license.properties`：

```properties
license.verifyApiUrl=https://your-license-server.example/api.php?action=verify
license.cacheDays=30
license.timeoutMs=5000
```

程序会自动追加 `product_code=LinovaOneERP`、`user_code` 和用户输入的 `license_key`。

使用 MySQL 时，编辑 `config/db.properties`：

```properties
db.enabled=true
db.host=localhost
db.port=3306
db.database=linova_erp
db.username=YOUR_DB_USER
db.password=YOUR_DB_PASSWORD
```

然后初始化数据库：

```bat
run.bat --init-db
```
