# 連絡先
- linwenjin0411@gmail.com

# Linova One ERP

言語：[English](README.md) | [简体中文](README.zh-CN.md) | [日本語](README.ja.md)

`Linova One ERP` は、製造業向けの Java PC デスクトップ ERP プロトタイプです。

名称の `Lin` はプロジェクトのアイデンティティを表し、`nova` は新しさと革新性を表し、`One` は統合 ERP ワークスペースを表します。

## 機能

- Java Swing デスクトップアプリケーション
- 既定言語：英語
- UI 言語：英語、簡体字中国語、日本語
- 言語切替付きログイン画面
- ワークスペースを開く前の License 検証。オンライン API 検証とオフライン署名 License のフォールバックに対応
- ログイン後の ERP Cockpit
- SAP / mcframe を参考にしたモジュール構成と業務フロー
- MySQL ベースのユーザー、会社、ロール、メニュー、ロールメニュー権限テーブル
- MySQL ベースのモジュールページ、アクション、プロセスステップ、KPI カード、ワークリスト列、ワークリスト行、フォーカス項目
- 購買、販売、製造、在庫、BOM、取引先、倉庫向けの独立した業務テーブル
- 在庫に影響する業務伝票から自動生成される在庫台帳
- 業務ステータスマスタ、統一ステータス遷移モデル、操作監査ログの基盤
- 照会、レポート、AI、在庫台帳ページは読み取り専用で、フィルタ、ソート、更新、エクスポートに対応
- MRP、生産原価、承認キュー、レポート、AI コンテキストページ
- データベース自動初期化
- `LinovaOneERP.exe` によるワンクリック起動

## システム概要

Linova One ERP は、製造業務に焦点を当てたデスクトップ ERP プロトタイプです。対象範囲は、マスタデータ、購買、販売、在庫、製造、財務、レポート、AI 支援照会、管理、License 管理です。

システムはモジュール型ワークスペースとして設計されています。ユーザーはログインし、License 検証を通過した後、mcframe 風の多階層メニューから各機能ページを開きます。機能ページは共通の CRUD 体験を保ちながら、利用可能な範囲で業務固有の項目と業務固有テーブルを使用します。

中核業務データは MySQL に保存されます。現在のモデルには、品目マスタ、BOM、得意先/仕入先マスタ、倉庫マスタ、購買伝票、販売伝票、製造伝票、在庫レコード、自動生成される在庫台帳、業務ステータス、ステータス遷移、操作監査ログが含まれます。

## 起動方法

JDK 8 以上と Apache Maven 3.8 以上をインストールしてください。

初回ビルド時、Maven は `pom.xml` に定義されたオープンソース依存関係をダウンロードします。

- FlatLaf
- MySQL Connector/J

`target/` と `build/` 配下の生成ファイルは Git 管理対象外であり、いつでも再生成できます。

サイレントデスクトップ起動：

```text
Double-click LinovaOneERP.exe
```

日常利用では `LinovaOneERP.exe` から起動してください。バックグラウンドで `run.bat` を起動し、コマンドウィンドウは表示されません。

`LinovaOneERP.exe` を開くと、ランチャーは既存の `target/linova-one-erp.jar` を `javaw` で起動します。日常起動時に Maven は実行されません。

メンテナンス用コマンドライン起動：

```bat
run.bat
```

コンパイルのみ：

```bat
run.bat --compile-only
```

EXE ランチャーの再構築：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-launcher.ps1
```

配布パッケージの作成：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\build-distribution.ps1
```

ローカル Windows JRE をパッケージに含める場合は、`-RuntimePath C:\path\to\jre` を指定します。生成されたパッケージは `build/dist/LinovaOneERP` に出力され、ランチャー、`run.bat`、ビルド済み jar、データベース schema、設定テンプレート、配布メモを含みます。

ログインとモジュール読込の診断：

```bat
run.bat --diagnose-login
```

MySQL テーブルのみ初期化：

```bat
run.bat --init-db
```

## ログ

実行時ログはローカルに保存されます。

```text
logs/YYYYMMDD/
```

主なファイル：

- `logs/YYYYMMDD/app-YYYYMMDD-HHMMSS-SSS-PID.log`：アプリケーションイベント、ログインフロー、データベース照会エラー、未捕捉例外
- `logs/YYYYMMDD/console-YYYYMMDD-HHMMSS-SSS-PID.log`：`javaw` からリダイレクトされた標準出力/標準エラー

ログファイルは Git 管理対象外ですが、`logs` フォルダはプロジェクト内に保持されます。
起動ごとに新しい UTF-8 ログファイルが作成されるため、過去の実行ログはトラブルシューティング用に保持され、上書きされません。
ユーザー操作は英語で `>>> USER_ACTION` プレフィックス付きで記録されます。対象はログイン、ページクリック、ツールバー操作、フォーム保存、ワークフロー確認、エクスポートです。

## デスクトップ UI

アプリケーションは Swing と FlatLaf を使用します。依存関係は Maven が `pom.xml` から解決し、jar ファイルはコミットしません。

ログイン成功や操作完了などの通常メッセージは小さな toast ウィンドウで表示され、3 秒後に閉じます。エラーメッセージはブロッキングのアラートダイアログで表示され、ユーザーが確認するまで閉じません。

メインナビゲーションは mcframe 風の 3 階層レイアウトです。濃色のルートモジュールメニュー、二次業務エリアのレール、青緑色のセクションヘッダー付き機能ボタングリッドで構成されます。機能をクリックすると独立したデスクトップウィンドウが開きます。`Master Data -> Master Maintenance -> Item Master Management` は `erp_item_masters` を使用する品目マスタ画面を開き、作成、編集、削除、更新、CSV エクスポートを提供します。

業務ページは統一された mcframe 風ワーク画面を使用します。ツールバー操作、検索可能でソート可能なテーブル、サイドコンテキスト、伝票番号ルール、組織/期間コンテキスト、CSV エクスポートを備えます。照会、レポート、AI、在庫台帳ページは設計上読み取り専用です。

## License

ユーザー ID とパスワードが承認された後、ログインフローは ERP ワークスペースを開く前に License を検証します。有効な License が存在しない場合、ログイン画面は英語、簡体字中国語、または日本語のローカライズ済みプロンプトを表示し、License key の入力を求めます。

License 検証は 2 つの方式に対応します。

- `license.verifyApiUrl` によるオンライン API 検証
- オフライン署名 License 検証

通常のデプロイでは、オンライン検証 API を次のファイルに設定します。

```text
config/license.properties
```

例：

```text
license.verifyApiUrl=https://your-license-server.example/api.php?action=verify
license.cacheDays=30
license.timeoutMs=5000
```

アプリケーションは `product_code=LinovaOneERP`、`user_code`、およびユーザーが License を入力した場合の `license_key` を自動的に追加します。

MySQL が有効な場合、License レコードは `erp_licenses` にキャッシュされます。active で未期限切れの License が存在する場合でも、アプリケーションは設定済み API で確認してからログインを許可します。有効な License が存在しない、または検証に失敗した場合、ログイン画面は License key の入力を求めます。

オフライン署名 License は次の形式を使用します。

```text
LINOVA-yyyyMMdd-signature
```

日付部分は License の有効期限です。署名部分はアプリケーションの公開鍵で検証されるため、未来日付だけでは有効な License を作成できません。

```text
Example structure only: LINOVA-20271231-<signature>
```

期限切れ、不正形式、未署名の key は拒否され、ログイン画面は開いたままになります。

`config/license.properties` はローカル License 設定ファイルです。API endpoint を保持でき、demo/offline モードではローカルにキャッシュされた License key も保持できます。このローカル License ファイルは Git 管理対象外であり、コミットしてはいけません。

管理者は License ページを開き、現在の License 状態の確認と新しい License の登録を行えます。

```text
Administration -> Security -> Roles -> License Management
系统管理 -> 安全权限 -> 角色 -> 许可证管理
システム管理 -> セキュリティ -> ロール -> ライセンス管理
```

## MySQL

アプリケーションはローカルデータベース設定を次のファイルから読み込みます。

```text
config/db.properties
```

このファイルには機密情報が含まれるため、Git 管理対象外です。コミット済みテンプレートは次のファイルです。

```text
config/db.properties.example
```

ローカルデータベース設定例：

```text
host: localhost
port: 3306
database: linova_erp
username: YOUR_DB_USER
```

`config/db.properties.example` を `config/db.properties` にコピーし、実際のローカル接続値を入力してください。`config/db.properties`、`config/license.properties`、データベース dump、エクスポート CSV、実行ログはコミットしないでください。

本番 MySQL 接続では `db.allowPublicKeyRetrieval=false` を維持してください。データベース認証方式が必要とする場合のみ、管理されたローカル互換性テストで一時的に有効化してください。

本番 MySQL 接続では `db.useSsl=true` を維持し、デプロイ環境に応じてデータベースサーバー証明書と信頼設定を構成してください。TLS 非対応の隔離されたローカルデータベースでのみ `false` に設定します。

schema ファイル：

```text
database/schema.mysql.sql
```

schema は次の主要テーブルグループを含みます。

- セキュリティ：`erp_users`、`erp_roles`、`erp_role_menus`
- 組織とメニュー：`erp_companies`、`erp_menus`、`erp_modules`
- モジュールメタデータ：`erp_module_actions`、`erp_module_process_steps`、`erp_module_metrics`、`erp_module_table_columns`、`erp_module_table_rows`、`erp_module_focus_items`
- マスタデータ：`erp_item_masters`、`erp_business_partners`、`erp_warehouse_masters`、`erp_bom_components`
- 業務伝票：`erp_purchase_documents`、`erp_purchase_document_lines`、`erp_sales_documents`、`erp_sales_document_lines`、`erp_manufacturing_documents`、`erp_inventory_records`
- 在庫移動：`erp_inventory_movements`
- ワークフローと監査：`erp_business_statuses`、`erp_business_status_transitions`、`erp_business_operation_logs`
- 汎用 fallback と License：`erp_function_records`、`erp_licenses`

ログイン後の多くのナビゲーション、モジュールメタデータ、品目マスタデータ、License データ、中核業務ワークリストレコードは MySQL に永続化されます。一部のプロトタイプ既定データは、業務テーブルが空の場合に自動投入されます。

## 初期アプリケーションユーザー

`run.bat --init-db` は固定データベースパスワードを投入しません。MySQL ユーザーテーブルが空の場合、初期化処理は最初の管理者アカウントを作成し、初期パスワードの設定を求めます。

対話式セットアップ：

```bat
run.bat --init-db
```

非対話式セットアップ：

```bat
set LINOVA_ADMIN_PASSWORD=change-this-before-use
run.bat --init-db
set LINOVA_ADMIN_PASSWORD=
```

初期データベースアカウント：

```text
User ID: admin
Role: System Administrator
```

組み込み demo fallback アカウントは、MySQL が無効なローカル開発専用であり、本番デプロイでは使用してはいけません。

## 依存関係

実行時依存関係は Maven で管理されます。

```text
pom.xml
```

jar ファイル、`build/` のビルド出力、`target/` の Maven 出力は Git 管理対象外です。クローン後、ビルドマシンに JDK と Maven をインストールし、`run.bat --compile-only` を実行し、`config/db.properties.example` を `config/db.properties` にコピーして実データベース値を設定した後、`run.bat --init-db` または `LinovaOneERP.exe` のダブルクリックで起動します。

## モジュール

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

## 備考

- 左ナビゲーションはカスタム Swing ボタンスタイルを使用し、濃色 ERP メニューが Windows ネイティブボタンテーマに上書きされないようにしています。
- ユーザー認証はまず MySQL を照会します。
- データベースログイン成功後、`last_login_at` が更新されます。
- `config/db.properties`、`config/license.properties`、実行ログ、エクスポート、データベース dump、ローカル SQL データファイル、jar ファイルは意図的にコミットされません。
- メインワークスペースは、現代的な ERP / AI 時代の印象に合わせて、柔らかいライトナビゲーションとカードスタイルを使用します。
- `LinovaOneERP.exe` は `run.bat` を非表示で起動するため、日常のデスクトップ利用ではコマンドウィンドウが表示されません。
- `run.bat` は通常、既存の jar を起動します。Maven ビルドは `run.bat --compile-only` でのみ実行されます。
- `run.bat` は `--init-db` と `--diagnose-login` のメンテナンスコマンドにも対応します。
- `build-distribution.ps1` は `build/dist/` 配下に正式配布フォルダを作成し、`-RuntimePath` により Windows JRE を同梱できます。
- アプリケーションは Java で描画したカスタム Linova ERP アイコンを使用するため、タイトルバーに既定の Java アイコンは表示されません。
- Windows Explorer はランチャー再構築後も古い `.exe` アイコンをキャッシュする場合があります。配布スクリーンショットを判断する前に、ファイルプロパティで実際の埋め込みアイコンを確認するか、再構築した `LinovaOneERP.exe` を新しいフォルダ/名前にコピーしてください。必要に応じて Explorer を更新するか、Windows アイコンキャッシュをクリアしてください。

## スクリーンショット

以下のスクリーンショットは、ログインフロー、License プロンプト、メインダッシュボード、マスタメニューページを示します。

<img width="1040" height="650" alt="001_login_sign-in" src="https://github.com/user-attachments/assets/d0637711-584e-43a0-89e2-e69220d53314" />
<img width="1040" height="650" alt="072_login_with_license_prompt" src="https://github.com/user-attachments/assets/005db6da-dff0-4833-a900-d99c9f613cee" />
<img width="1455" height="880" alt="002_main_dashboard" src="https://github.com/user-attachments/assets/a3b2add1-81c3-427e-88ac-9aba64ad2687" />
<img width="1455" height="880" alt="003_main_master-master-maint" src="https://github.com/user-attachments/assets/9380d098-0f62-41da-b84f-18a1c6c26594" />
