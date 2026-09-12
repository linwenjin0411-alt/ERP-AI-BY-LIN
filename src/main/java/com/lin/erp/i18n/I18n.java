package com.lin.erp.i18n;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class I18n {
    public static final String APP_NAME = "Linova One ERP";
    public static final Language DEFAULT_LANGUAGE = Language.EN;

    private static final Map<Language, Map<String, String>> TEXTS =
            new EnumMap<Language, Map<String, String>>(Language.class);

    static {
        for (Language language : Language.values()) {
            TEXTS.put(language, new HashMap<String, String>());
        }

        put("app.tagline",
                "Manufacturing ERP for integrated operations",
                "面向制造业的一体化 ERP",
                "製造業向け統合 ERP");
        put("app.company",
                "Linova Smart Manufacturing Ltd.",
                "灵诺智能制造有限公司",
                "Linova スマート製造株式会社");

        put("login.window.title", "Sign in - Linova One ERP", "登录 - Linova One ERP", "ログイン - Linova One ERP");
        put("login.title", "Sign in", "登录系统", "ログイン");
        put("login.subtitle", "Use your enterprise account to continue.", "请输入企业账号和密码。", "企業アカウントで続行してください。");
        put("login.user", "User ID", "用户 ID", "ユーザー ID");
        put("login.password", "Password", "密码", "パスワード");
        put("login.company", "Company", "公司", "会社");
        put("login.language", "Language", "语言", "言語");
        put("login.remember", "Remember this workstation", "记住本机登录信息", "この端末を記憶する");
        put("login.button", "Sign in", "登录", "ログイン");
        put("login.test.account", "Demo account: admin / admin123", "测试账号：admin / admin123", "デモアカウント：admin / admin123");
        put("login.footer", "Next: user master, RBAC, organization switch, and audit log.",
                "后续可接入用户表、RBAC 权限、组织切换和操作审计。",
                "次はユーザーマスタ、RBAC、組織切替、監査ログを接続します。");
        put("login.signingIn", "Signing in and loading workspace...",
                "正在登录并加载工作台...",
                "ログインしてワークスペースを読み込んでいます...");
        put("login.interrupted", "Sign-in was interrupted. Please try again.",
                "登录被中断，请重试。",
                "ログインが中断されました。もう一度お試しください。");
        put("login.failed.generic", "Sign-in failed. Please check the app log or contact the administrator.",
                "登录失败，请查看应用日志或联系管理员。",
                "ログインに失敗しました。アプリログを確認するか、管理者に連絡してください。");
        put("login.success", "Signed in. Opening workspace...",
                "登录成功，正在进入系统...",
                "ログインしました。ワークスペースを開いています...");
        put("message.error.title", "Action required", "需要处理", "確認が必要です");
        put("message.login.success", "Signed in successfully.", "登录成功。", "ログインしました。");
        put("message.refresh.done", "Data refreshed.", "数据已刷新。", "データを更新しました。");
        put("message.operation.success", "Operation completed successfully.", "操作成功。", "操作が完了しました。");
        put("message.edit.success", "Changes saved successfully.", "修改成功。", "変更を保存しました。");
        put("message.language.changed", "Language changed.", "语言已切换。", "言語を切り替えました。");
        put("message.select.row", "Select a row first.", "请先选择一行数据。", "先に行を選択してください。");
        put("message.create.success", "Record created successfully.", "新建成功。", "レコードを作成しました。");
        put("message.approve.success", "Approval completed.", "审批完成。", "承認が完了しました。");
        put("message.release.success", "Release completed.", "下达完成。", "リリースが完了しました。");
        put("message.post.success", "Posting completed.", "过账完成。", "転記が完了しました。");
        put("message.export.success", "Export finished: ", "导出完成：", "エクスポート完了: ");
        put("message.refresh.failed", "Refresh failed. See the app log for details.", "刷新失败，请查看应用日志。", "更新に失敗しました。アプリログを確認してください。");
        put("message.save.failed", "Save failed. See the app log for details.", "保存失败，请查看应用日志。", "保存に失敗しました。アプリログを確認してください。");
        put("message.export.failed", "Export failed. See the app log for details.", "导出失败，请查看应用日志。", "エクスポートに失敗しました。アプリログを確認してください。");
        put("message.status.failed", "Status update failed. See the app log for details.", "状态更新失败，请查看应用日志。", "ステータス更新に失敗しました。アプリログを確認してください。");
        put("message.id.required", "The first column is required.", "第一列为必填项。", "最初の列は必須です。");
        put("message.no.status", "This table has no status column for the selected workflow action.", "当前表格没有可用于该流程操作的状态列。", "この表には選択したワークフロー操作に使えるステータス列がありません。");
        put("message.delete.success", "Deleted successfully.", "删除成功。", "削除しました。");
        put("message.delete.failed", "Delete failed. See the app log for details.", "删除失败，请查看应用日志。", "削除に失敗しました。アプリログを確認してください。");
        put("message.delete.confirm", "Delete selected record", "删除所选记录", "選択したレコードを削除します");
        put("message.permission.denied", "You do not have permission to perform this action.",
                "您没有权限执行此操作。",
                "この操作を実行する権限がありません。");
        put("message.item.required", "Item code and item name are required.", "品目编号和品目名称为必填项。", "品目コードと品目名は必須です。");
        put("license.prompt.title", "License required", "需要许可证", "ライセンスが必要です");
        put("license.prompt.message", "Enter a signed license key. Format: LINOVA-yyyyMMdd-signature.",
                "请输入签名许可证。格式：LINOVA-yyyyMMdd-signature。",
                "署名付きライセンスキーを入力してください。形式：LINOVA-yyyyMMdd-signature。");
        put("license.required", "A valid license is required before sign-in can continue.",
                "必须输入有效许可证后才能继续登录。",
                "ログインを続行するには有効なライセンスが必要です。");
        put("license.invalid", "The license key is invalid or expired.",
                "许可证无效或已过期。",
                "ライセンスキーが無効、または期限切れです。");
        put("license.register.success", "License registered. Valid until: ",
                "许可证已登记，有效期至：",
                "ライセンスを登録しました。有効期限：");
        put("license.check.failed", "License status could not be checked. See the app log for details.",
                "无法检查许可证状态，请查看应用日志。",
                "ライセンス状態を確認できません。アプリログを確認してください。");
        put("license.save.failed", "License could not be saved. See the app log for details.",
                "许可证保存失败，请查看应用日志。",
                "ライセンスを保存できません。アプリログを確認してください。");
        put("license.field.key", "License Key", "许可证密钥", "ライセンスキー");
        put("license.field.validFrom", "Valid From", "有效开始日", "有効開始日");
        put("license.field.validUntil", "Valid Until", "有效截止日", "有効期限");
        put("license.action.register", "Register License", "登记许可证", "ライセンス登録");
        put("license.status.valid", "Valid until: ", "有效期至：", "有効期限：");
        put("license.status.invalid", "No valid license", "没有有效许可证", "有効なライセンスがありません");
        put("dialog.confirm.title", "Confirm action", "确认操作", "操作確認");
        put("dialog.confirm.action", "Action", "操作", "操作");
        put("dialog.confirm.record", "Record", "记录", "レコード");
        put("dialog.confirm.status", "Target status", "目标状态", "変更後ステータス");
        put("dialog.confirm.ok", "Confirm", "确认", "確認");
        put("dialog.confirm.cancel", "Cancel", "取消", "キャンセル");
        put("dialog.simulate.title", "Simulation result", "模拟结果", "シミュレーション結果");
        put("dialog.simulate.body", "Simulation completed. Shortage, schedule, and cost signals were recalculated for the selected workspace.",
                "模拟完成，已重新计算当前工作台的缺料、排程和成本信号。",
                "シミュレーションが完了しました。選択中のワークスペースで欠品、日程、原価シグナルを再計算しました。");
        put("form.transactionRecord", "Transaction record", "业务记录", "業務レコード");
        put("form.side.title", "Business context", "业务上下文", "業務コンテキスト");
        put("form.side.module", "Module", "模块", "モジュール");
        put("form.side.audit", "Audit trail will capture the user and timestamp.", "系统会记录用户与时间审计轨迹。", "ユーザーと時刻が監査証跡に記録されます。");
        put("form.side.validation", "Required values are checked before database save.", "保存前会校验关键字段。", "保存前に主要項目を検証します。");
        put("form.side.workflow", "Workflow status can drive approval, release, and posting.", "流程状态可驱动审批、下达和过账。", "ワークフロー状態により承認、リリース、転記を制御できます。");
        put("form.save", "Save", "保存", "保存");
        put("form.cancel", "Cancel", "取消", "キャンセル");
        put("submenu.title", "Submenu", "次级菜单", "サブメニュー");
        put("submenu.opened", "Function page", "功能页面", "機能ページ");
        put("submenu.placeholder", "This function page has been opened. Detailed business fields, approval rules, and transaction-specific tables can be extended from this page.",
                "该功能页面已打开。后续可在此扩展专用业务字段、审批规则和事务表。",
                "この機能ページを開きました。専用業務項目、承認ルール、トランザクション別テーブルをここから拡張できます。");
        put("submenu.code", "Menu code", "菜单代码", "メニューコード");
        put("function.operation", "Operation", "操作类别", "操作種別");
        put("function.mode.register", "Register", "登记", "登録する");
        put("function.mode.correct", "Correct", "修正", "訂正する");
        put("function.mode.cancel", "Cancel", "取消", "取消する");
        put("function.mode.reference", "Reference", "参照", "参照する");
        put("function.search.target", "Target", "对象", "対象");
        put("function.search", "Search", "检索", "検索する");
        put("function.tab.system", "System Information", "系统信息", "システム情報");
        put("function.field.documentNo", "Document No.", "单据号", "伝票番号");
        put("function.field.businessDate", "Business Date", "业务日期", "業務日付");
        put("function.field.status", "Status", "状态", "ステータス");
        put("function.field.partner", "Partner", "业务伙伴", "取引先");
        put("function.field.item", "Item", "品目", "品目");
        put("function.field.quantity", "Quantity", "数量", "数量");
        put("function.field.warehouse", "Warehouse", "仓库", "倉庫");
        put("function.field.owner", "Owner", "负责人", "担当者");
        put("function.field.memo", "Memo", "备注", "メモ");
        put("function.table.line", "Line", "行号", "行");
        put("function.context.flow", "Business flow", "业务流程", "業務フロー");
        put("function.context.upstream", "Upstream", "上游", "上流");
        put("function.context.downstream", "Downstream", "下游", "下流");
        put("item.count.prefix", "Records: ", "记录数：", "件数: ");
        put("item.side.title", "Item signals", "品目信号", "品目シグナル");
        put("item.side.total", "Total items", "全部品目", "全品目");
        put("item.side.released", "Released", "已发布", "リリース済み");
        put("item.side.open", "Open", "打开", "未処理");

        put("brand.edition", "PC desktop suite", "PC 桌面套件", "PC デスクトップスイート");
        put("brand.flow", "Plan, buy, make, stock, sell, ship, and settle in one workspace.",
                "计划、采购、生产、库存、销售、出货、结算一体化。",
                "計画、購買、製造、在庫、販売、出荷、決済を一つの画面で。");
        put("brand.note",
                "Built around modular enterprise applications, manufacturing workflows, and deterministic business transactions.",
                "参考模块化企业应用思想，结合制造业流程和可追溯业务事务。",
                "モジュール型業務アプリ、製造業務フロー、追跡可能な業務トランザクションを中心に設計。");

        put("cap.master", "Master Data", "主数据", "マスタ");
        put("cap.procurement", "Procurement", "采购管理", "購買管理");
        put("cap.sales", "Sales", "销售管理", "販売管理");
        put("cap.inventory", "Inventory", "库存管理", "在庫管理");
        put("cap.manufacturing", "Manufacturing", "生产管理", "生産管理");
        put("cap.finance", "Finance", "财务联动", "会計連携");
        put("cap.admin", "Authorization", "权限控制", "権限管理");
        put("cap.ai", "AI Copilot", "AI 助手", "AI アシスタント");
        put("cap.analytics", "Analytics", "经营分析", "経営分析");

        put("auth.user.required", "Enter a user ID.", "请输入用户 ID。", "ユーザー ID を入力してください。");
        put("auth.password.required", "Enter a password.", "请输入密码。", "パスワードを入力してください。");
        put("auth.user.notFound", "User does not exist.", "用户不存在。", "ユーザーが存在しません。");
        put("auth.bad.credentials", "User ID or password is incorrect.", "用户 ID 或密码不正确。", "ユーザー ID またはパスワードが正しくありません。");
        put("auth.database.unavailable", "Database login is unavailable. Check local database settings or contact the administrator.",
                "数据库登录不可用。请检查本地数据库设置或联系管理员。",
                "データベースログインを利用できません。ローカルDB設定を確認するか、管理者に連絡してください。");
        put("auth.database.failed", "Database login failed. Please check the app log or contact the administrator.",
                "数据库登录失败。请查看应用日志或联系管理员。",
                "データベースログインに失敗しました。アプリログを確認するか、管理者に連絡してください。");
        put("user.admin.name", "System Administrator", "系统管理员", "システム管理者");
        put("user.planner.name", "Production Planner", "生产计划员", "生産計画担当");
        put("role.admin", "System Administrator", "系统管理员", "システム管理者");
        put("role.planner", "Production Planner", "生产计划员", "生産計画担当");

        put("module.dashboard", "Cockpit", "系统首页", "コックピット");
        put("module.master", "Master Data", "主数据管理", "マスタ管理");
        put("module.procurement", "Procurement", "采购管理", "購買管理");
        put("module.sales", "Sales", "销售管理", "販売管理");
        put("module.inventory", "Inventory", "库存管理", "在庫管理");
        put("module.manufacturing", "Manufacturing", "生产管理", "生産管理");
        put("module.finance", "Finance", "财务联动", "会計連携");
        put("module.reports", "Reports", "基础报表", "基本レポート");
        put("module.ai", "AI Assistant", "AI 业务助手", "AI 業務アシスタント");
        put("module.admin", "Administration", "系统管理", "システム管理");

        put("top.search", "Search transaction, item, order...", "搜索事务、物料、订单...", "伝票、品目、オーダーを検索...");
        put("top.period", "FY2026 / Period 08", "2026 财年 / 第 08 期间", "2026年度 / 第08期間");
        put("action.refresh", "Refresh", "刷新", "更新");
        put("action.new", "New", "新建", "新規");
        put("action.edit", "Edit", "编辑", "編集");
        put("action.approve", "Approve", "审批", "承認");
        put("action.release", "Release", "下达", "リリース");
        put("action.post", "Post", "过账", "転記");
        put("action.simulate", "Simulate", "模拟", "シミュレーション");
        put("action.export", "Export", "导出", "エクスポート");
        put("action.delete", "Delete", "删除", "削除");
        put("action.details", "Details", "详情", "詳細");
        put("action.ask", "Ask", "提问", "質問");

        put("menu.area.master.maintenance", "Master Maintenance", "主数据维护", "マスタ保守");
        put("menu.area.governance", "Governance", "治理管理", "ガバナンス");
        put("menu.area.procurement.source", "Source Management", "寻源采购", "購買管理");
        put("menu.area.procurement.receiving", "Receiving Management", "收货管理", "入荷管理");
        put("menu.area.sales.domestic", "Sales Management", "销售管理", "販売管理");
        put("menu.area.sales.export", "Export Management", "出口管理", "輸出管理");
        put("menu.area.approval", "Approval Management", "审批管理", "承認管理");
        put("menu.area.inventory.control", "Inventory Control", "库存控制", "在庫管理");
        put("menu.area.inventory.trace", "Traceability", "追溯管理", "トレーサビリティ");
        put("menu.area.manufacturing.planning", "Production Planning", "生产计划", "生産計画");
        put("menu.area.manufacturing.execution", "Shop-floor Execution", "现场执行", "製造実行");
        put("menu.area.finance.accounting", "Accounting", "会计处理", "会計処理");
        put("menu.area.finance.close", "Closing", "结账管理", "締処理");
        put("menu.area.ai.copilot", "AI Copilot", "AI 助手", "AI コパイロット");
        put("menu.area.admin.security", "Security", "安全权限", "セキュリティ");
        put("menu.area.admin.audit", "Audit", "审计", "監査");
        put("menu.section.product", "Product Data", "产品数据", "品目データ");
        put("menu.section.partner", "Partner Data", "伙伴数据", "取引先データ");
        put("menu.section.logistics", "Logistics Data", "物流数据", "物流データ");
        put("menu.section.controls", "Controls", "控制项", "管理項目");
        put("menu.section.buying", "Buying", "采购", "購買");
        put("menu.section.inbound", "Inbound", "入库", "入荷");
        put("menu.section.settlement", "Settlement", "结算", "照合");
        put("menu.section.ordering", "Order", "订单", "受注");
        put("menu.section.fulfillment", "Fulfillment", "履行", "出荷");
        put("menu.section.billing", "Billing", "开票", "売上");
        put("menu.section.export", "Export", "出口", "輸出");
        put("menu.section.approval", "Approval", "审批", "承認");
        put("menu.section.stock", "Stock", "库存", "在庫");
        put("menu.section.trace", "Trace", "追溯", "追跡");
        put("menu.section.counting", "Counting", "盘点", "棚卸");
        put("menu.section.planning", "Planning", "计划", "計画");
        put("menu.section.execution", "Execution", "执行", "実行");
        put("menu.section.costing", "Costing", "成本", "原価");
        put("menu.section.receivables", "Receivables", "应收", "売掛");
        put("menu.section.payables", "Payables", "应付", "買掛");
        put("menu.section.ledger", "Ledger", "总账", "元帳");
        put("menu.section.close", "Close", "结账", "締め");
        put("menu.section.aiAssist", "Assist", "辅助", "支援");
        put("menu.section.reports", "Reports", "报表", "レポート");
        put("menu.section.users", "Users", "用户", "ユーザー");
        put("menu.section.roles", "Roles", "角色", "ロール");
        put("menu.section.audit", "Audit", "审计", "監査");
        put("menu.master.item", "Item Master Management", "品目主数据管理", "品目マスタ管理");
        put("menu.master.bom", "BOM Management", "BOM 管理", "BOM管理");
        put("menu.master.customer", "Customer Master Management", "客户主数据管理", "得意先マスタ管理");
        put("menu.master.supplier", "Supplier Master Management", "供应商主数据管理", "仕入先マスタ管理");
        put("menu.master.warehouse", "Warehouse Master Management", "仓库主数据管理", "倉庫マスタ管理");
        put("menu.master.changeAudit", "Master Change Audit", "主数据变更审计", "マスタ変更監査");
        put("menu.procurement.pr", "Purchase Request", "采购申请", "購買依頼");
        put("menu.procurement.po", "Purchase Order", "采购订单", "購買発注");
        put("menu.procurement.poQuery", "Purchase Order Query", "采购订单查询", "購買発注照会");
        put("menu.procurement.receipt", "Goods Receipt", "收货入库", "入荷");
        put("menu.procurement.receiptQuery", "Receipt Query", "收货查询", "入荷照会");
        put("menu.procurement.confirmation", "Purchase Confirmation", "采购确认", "仕入確認");
        put("menu.procurement.invoice", "Invoice Verification", "发票校验", "請求書照合");
        put("menu.procurement.return", "Purchase Return", "采购退货", "仕入返品");
        put("menu.sales.quotation", "Quotation", "报价", "見積");
        put("menu.sales.order", "Sales Order", "销售订单", "販売受注");
        put("menu.sales.orderQuery", "Sales Order Query", "销售订单查询", "販売受注照会");
        put("menu.sales.shipment", "Shipment", "出货", "出荷");
        put("menu.sales.shipmentQuery", "Shipment Query", "出货查询", "出荷照会");
        put("menu.sales.confirmation", "Sales Confirmation", "销售确认", "売上確認");
        put("menu.sales.billing", "Billing", "开票", "請求");
        put("menu.sales.return", "Sales Return", "销售退货", "売上返品");
        put("menu.sales.exportOrder", "Export Order", "出口订单", "輸出受注");
        put("menu.sales.approvalReview", "Approval Review", "审批复核", "承認確認");
        put("menu.inventory.stock", "Stock Overview", "库存总览", "在庫照会");
        put("menu.inventory.ledger", "Stock Ledger Query", "库存流水查询", "在庫元帳照会");
        put("menu.inventory.lot", "Lot Trace", "批次追溯", "ロット追跡");
        put("menu.inventory.transfer", "Stock Transfer", "库存调拨", "在庫振替");
        put("menu.inventory.count", "Cycle Count", "循环盘点", "循環棚卸");
        put("menu.manufacturing.mrp", "MRP Run", "MRP 运算", "MRP実行");
        put("menu.manufacturing.order", "Production Order", "生产订单", "製造指図");
        put("menu.manufacturing.orderQuery", "Production Order Query", "生产订单查询", "製造指図照会");
        put("menu.manufacturing.issue", "Material Issue", "生产领料", "材料払出");
        put("menu.manufacturing.complete", "Production Completion", "生产完工登记", "製造完了登録");
        put("menu.manufacturing.return", "Material Return", "生产退料", "材料戻入");
        put("menu.manufacturing.cost", "Cost Collection", "成本归集", "原価集計");
        put("menu.finance.ar", "Accounts Receivable", "应收账款", "売掛金");
        put("menu.finance.collection", "Collection Register", "收款登记", "入金登録");
        put("menu.finance.collectionQuery", "Collection Query", "收款查询", "入金照会");
        put("menu.finance.ap", "Accounts Payable", "应付账款", "買掛金");
        put("menu.finance.payment", "Payment Register", "付款登记", "支払登録");
        put("menu.finance.paymentQuery", "Payment Query", "付款查询", "支払照会");
        put("menu.finance.gl", "General Ledger", "总账", "総勘定元帳");
        put("menu.finance.close", "Period Close", "期间结账", "期間締め");
        put("menu.report.salesDetail", "Sales Detail", "销售明细", "売上明細");
        put("menu.report.purchaseDetail", "Purchase Detail", "采购明细", "購買明細");
        put("menu.report.inventoryDetail", "Inventory Detail", "库存明细", "在庫明細");
        put("menu.report.arBalance", "AR Balance", "应收余额", "売掛残高");
        put("menu.report.apBalance", "AP Balance", "应付余额", "買掛残高");
        put("menu.ai.query", "Natural Query", "自然语言查询", "自然言語検索");
        put("menu.ai.explain", "Exception Explanation", "异常解释", "例外説明");
        put("menu.ai.summary", "Report Summary", "报表总结", "レポート要約");
        put("menu.admin.users", "User Management", "用户管理", "ユーザー管理");
        put("menu.admin.roles", "Role Management", "角色管理", "ロール管理");
        put("menu.admin.permissions", "Permission Management", "权限管理", "権限管理");
        put("menu.admin.license", "License Management", "许可证管理", "ライセンス管理");
        put("menu.admin.audit", "Audit Log", "审计日志", "監査ログ");

        put("dashboard.subtitle", "Operational command center inspired by SAP-style process integration and mcframe-style manufacturing flow.",
                "参考 SAP 式流程集成与 mcframe 式制造业业务流的经营驾驶舱。",
                "SAP 型のプロセス統合と mcframe 型の製造業務フローを意識した業務コックピット。");
        put("dashboard.kpi.purchase", "Pending Purchase Requests", "待处理采购申请", "未処理購買依頼");
        put("dashboard.kpi.shortage", "Materials Below Safety Stock", "低于安全库存物料", "安全在庫割れ品目");
        put("dashboard.kpi.production", "Production Orders This Week", "本周生产订单", "今週の製造指図");
        put("dashboard.kpi.shipment", "Sales Orders Not Shipped", "销售未出货订单", "未出荷販売受注");
        put("dashboard.process.title", "End-to-end manufacturing flow", "制造业端到端流程", "製造業エンドツーエンドフロー");
        put("dashboard.alerts.title", "Exception monitor", "异常监控", "例外監視");
        put("dashboard.alert.1", "MRP found 5 shortage risks for next week.", "MRP 发现下周 5 个缺料风险。", "MRP が来週の欠品リスクを 5 件検出しました。");
        put("dashboard.alert.2", "Two purchase orders are late and affect production order MO-2608-004.", "2 张采购订单延期，影响生产订单 MO-2608-004。", "購買発注 2 件の遅延が製造指図 MO-2608-004 に影響します。");
        put("dashboard.alert.3", "Warehouse A has abnormal inventory movement on item RM-1008.", "A 仓库物料 RM-1008 出现异常库存移动。", "倉庫 A で品目 RM-1008 の異常な在庫移動があります。");

        put("section.process", "Process", "流程", "プロセス");
        put("section.worklist", "Worklist", "待办", "ワークリスト");
        put("section.master", "Master records", "主记录", "マスタレコード");
        put("section.analytics", "Analytics", "分析", "分析");
        put("section.controls", "Controls", "控制项", "管理項目");

        put("master.subtitle", "Enterprise master data used by every transaction.",
                "支撑所有业务事务的企业主数据。",
                "すべての業務トランザクションで使用する企業マスタ。");
        put("sales.subtitle", "Quote-to-cash pipeline: quotation, sales order, shipment, billing, and receivable.",
                "从报价到收款：报价、销售订单、出货、开票、应收。",
                "見積から入金まで：見積、販売受注、出荷、請求、売掛。");
        put("procurement.subtitle", "Source-to-pay pipeline: request, purchase order, receipt, invoice, and payable.",
                "从申请到付款：采购申请、采购订单、收货、发票、应付。",
                "依頼から支払まで：購買依頼、発注、入荷、請求、買掛。");
        put("inventory.subtitle", "Real-time stock, lot traceability, transfers, adjustments, and safety stock control.",
                "实时库存、批次追溯、调拨、调整与安全库存控制。",
                "リアルタイム在庫、ロット追跡、振替、調整、安全在庫管理。");
        put("manufacturing.subtitle", "BOM, routing, MRP, production orders, material issue, reporting, and cost collection.",
                "BOM、工艺路线、MRP、生产订单、领料、报工与成本归集。",
                "BOM、工程順序、MRP、製造指図、払出、実績報告、原価集計。");
        put("finance.subtitle", "Financial integration for AR, AP, inventory valuation, cost accounting, and GL posting.",
                "应收、应付、库存估价、成本会计与总账过账的财务联动。",
                "売掛、買掛、在庫評価、原価計算、総勘定元帳転記の会計連携。");
        put("reports.subtitle", "Core business reports for sales, procurement, inventory, receivables, and payables.",
                "覆盖销售、采购、库存、应收、应付的基础业务报表。",
                "売上、購買、在庫、売掛、買掛を対象とする基本業務レポート。");
        put("ai.subtitle", "Natural-language ERP assistant for query, exception explanation, and report summary.",
                "支持自然语言查询、异常解释和报表总结的 ERP 助手。",
                "自然言語検索、例外説明、レポート要約を行う ERP アシスタント。");
        put("admin.subtitle", "Users, roles, menu permissions, workflow approval, and operation audit.",
                "用户、角色、菜单权限、工作流审批与操作审计。",
                "ユーザー、ロール、メニュー権限、ワークフロー承認、操作監査。");

        put("column.id", "ID", "编号", "番号");
        put("column.name", "Name", "名称", "名称");
        put("column.type", "Type", "类型", "タイプ");
        put("column.role", "Role", "角色", "ロール");
        put("column.department", "Department", "部门", "部門");
        put("column.email", "Email", "邮箱", "メール");
        put("column.language", "Language", "语言", "言語");
        put("column.permission", "Permission", "权限", "権限");
        put("column.scope", "Scope", "范围", "範囲");
        put("column.status", "Status", "状态", "ステータス");
        put("column.owner", "Owner", "负责人", "担当者");
        put("column.date", "Date", "日期", "日付");
        put("column.due", "Due", "到期", "期限");
        put("column.item", "Item", "物料", "品目");
        put("column.qty", "Qty", "数量", "数量");
        put("column.amount", "Amount", "金额", "金額");
        put("column.customer", "Customer", "客户", "得意先");
        put("column.supplier", "Supplier", "供应商", "仕入先");
        put("column.plant", "Plant", "工厂", "プラント");
        put("column.warehouse", "Warehouse", "仓库", "倉庫");
        put("column.risk", "Risk", "风险", "リスク");
        put("column.next", "Next step", "下一步", "次の処理");
        put("column.itemCode", "Item Code", "品目编号", "品目コード");
        put("column.itemName", "Item Name", "品目名称", "品目名");
        put("column.itemType", "Item Type", "品目类型", "品目タイプ");
        put("column.uom", "UoM", "单位", "単位");
        put("column.safetyStock", "Safety Stock", "安全库存", "安全在庫");
        put("column.leadTime", "Lead Time Days", "提前期天数", "リードタイム日数");
        put("column.version", "Version", "版本", "バージョン");
        put("column.effectiveFrom", "Effective From", "生效日", "有効開始日");
        put("column.alternateItem", "Alternate Item", "替代料", "代替品目");
        put("column.scrapRate", "Scrap Rate", "损耗率", "歩留/ロス率");
        put("column.operation", "Operation", "工序", "工程");
        put("column.address", "Address", "地址", "住所");
        put("column.contact", "Contact", "联系人", "連絡先");
        put("column.paymentTerms", "Payment Terms", "付款条件", "支払条件");
        put("column.taxArea", "Tax Area", "税区", "税区分");
        put("column.currency", "Currency", "币种", "通貨");
        put("column.creditLimit", "Credit Limit", "信用额度", "与信限度");
        put("column.zone", "Zone", "库区", "ゾーン");
        put("column.bin", "Bin", "库位", "棚番");
        put("column.inboundPolicy", "Inbound Policy", "入库策略", "入庫方針");
        put("column.outboundPolicy", "Outbound Policy", "出库策略", "出庫方針");
        put("column.lotControl", "Lot Control", "批次管理", "ロット管理");
        put("column.demand", "Demand", "需求", "需要");
        put("column.stock", "Stock", "库存", "在庫");
        put("column.ordered", "Ordered", "已下单", "発注済");
        put("column.wip", "WIP", "在制", "仕掛");
        put("column.netDemand", "Net Demand", "净需求", "正味所要量");
        put("column.materialCost", "Material Cost", "材料费", "材料費");
        put("column.laborCost", "Labor Cost", "人工费", "労務費");
        put("column.overheadCost", "Overhead Cost", "制造费用", "製造間接費");
        put("column.standardCost", "Standard Cost", "标准成本", "標準原価");
        put("column.actualCost", "Actual Cost", "实际成本", "実際原価");
        put("column.variance", "Variance", "差异", "差異");
        put("column.dataSource", "Data Source", "数据来源", "データソース");
        put("column.prompt", "Prompt", "提示词", "プロンプト");
        put("column.audit", "Audit", "审计", "監査");
        put("column.approvalHistory", "Approval History", "审批历史", "承認履歴");
        put("column.rejectReason", "Reject Reason", "驳回原因", "差戻理由");

        put("status.open", "Open", "打开", "未処理");
        put("status.waitingApproval", "Waiting approval", "等待审批", "承認待ち");
        put("status.released", "Released", "已下达", "リリース済");
        put("status.late", "Late", "延期", "遅延");
        put("status.shortage", "Shortage", "缺料", "欠品");
        put("status.blocked", "Blocked", "阻塞", "ブロック");
        put("status.ready", "Ready", "就绪", "準備完了");
        put("status.posted", "Posted", "已过账", "転記済");
        put("status.draft", "Draft", "草稿", "ドラフト");
        put("status.cancelled", "Cancelled", "已取消", "取消済");
        put("risk.high", "High", "高", "高");
        put("risk.medium", "Medium", "中", "中");
        put("risk.low", "Low", "低", "低");
        put("owner.sales", "Sales", "销售", "販売");
        put("owner.planner", "Planner", "计划", "計画");
        put("owner.procurement", "Procurement", "采购", "購買");
        put("owner.production", "Production", "生产", "生産");
        put("owner.finance", "Finance", "财务", "会計");
        put("owner.system", "System", "系统", "システム");

        put("flow.salesOrder", "Sales order", "销售订单", "販売受注");
        put("flow.demand", "Demand", "需求", "需要");
        put("flow.mrp", "MRP", "MRP", "MRP");
        put("flow.purchase", "Purchase", "采购", "購買");
        put("flow.production", "Production", "生产", "生産");
        put("flow.goodsReceipt", "Goods receipt", "收货入库", "入荷");
        put("flow.shipment", "Shipment", "出货", "出荷");
        put("flow.billing", "Billing", "开票", "請求");
        put("flow.settlement", "Settlement", "结算", "決済");

        put("metric.service", "Service level", "服务水平", "サービスレベル");
        put("metric.inventoryTurn", "Inventory turns", "库存周转", "在庫回転");
        put("metric.otd", "On-time delivery", "准时交付", "納期遵守率");
        put("metric.costVariance", "Cost variance", "成本差异", "原価差異");

        put("table.worklist", "Operational worklist", "业务待办", "業務ワークリスト");
        put("table.records", "Records", "记录", "レコード");
        put("table.sample", "Sample transactions", "示例业务单据", "サンプル伝票");
        put("panel.process", "Reference process", "参考流程", "参照プロセス");
        put("panel.kpi", "Key indicators", "关键指标", "主要指標");
        put("panel.todo", "Focus items", "重点事项", "重点項目");
        put("focus.master.itemBom", "Review item master and BOM release status.",
                "复核物料主数据与 BOM 下达状态。",
                "品目マスタと BOM のリリース状況を確認してください。");
        put("focus.master.permissions", "Review menu permissions for new planners.",
                "复核新计划员的菜单权限。",
                "新しい計画担当者のメニュー権限を確認してください。");
        put("focus.master.audit", "Export master data audit trail for this period.",
                "导出本期间主数据审计记录。",
                "当期間のマスタ監査証跡をエクスポートしてください。");
        put("focus.procurement.poReceipt", "PO-45000127 -> goods receipt follow-up.",
                "PO-45000127 -> 跟进收货入库。",
                "PO-45000127 -> 入荷フォロー。");
        put("focus.procurement.invoicePayable", "Invoice verification -> accounts payable posting.",
                "发票校验 -> 应付过账。",
                "請求書照合 -> 買掛転記。");
        put("focus.sales.soShipment", "SO-2608-104 -> shipment preparation.",
                "SO-2608-104 -> 出货准备。",
                "SO-2608-104 -> 出荷準備。");
        put("focus.sales.billingReceivable", "Billing -> accounts receivable follow-up.",
                "开票 -> 应收跟进。",
                "請求 -> 売掛フォロー。");
        put("focus.sales.service", "Service level is holding at 98.2%.",
                "服务水平保持在 98.2%。",
                "サービスレベルは 98.2% を維持しています。");
        put("focus.inventory.transfer", "Transfer stock from WH-A to WH-B.",
                "从 WH-A 调拨库存到 WH-B。",
                "WH-A から WH-B へ在庫振替。");
        put("focus.inventory.cycle", "Cycle count is required for WH-A.",
                "WH-A 需要执行循环盘点。",
                "WH-A で循環棚卸が必要です。");
        put("focus.manufacturing.moIssue", "MO-2608-004 -> material issue review.",
                "MO-2608-004 -> 复核生产领料。",
                "MO-2608-004 -> 材料払出の確認。");
        put("focus.manufacturing.costing", "Collect costing for FG-3007.",
                "归集 FG-3007 的生产成本。",
                "FG-3007 の原価を集計してください。");
        put("focus.finance.inventoryGl", "Inventory valuation -> general ledger posting.",
                "库存估价 -> 总账过账。",
                "在庫評価 -> 総勘定元帳転記。");
        put("focus.finance.close", "Prepare FY2026 / Period 08 close.",
                "准备 2026 财年 / 第 08 期间结账。",
                "2026年度 / 第08期間の締め準備。");
        put("focus.admin.permissions", "Procurement menu permissions need review.",
                "采购菜单权限需要复核。",
                "購買メニュー権限の確認が必要です。");
        put("focus.admin.workflow", "Purchase order approval workflow is active.",
                "采购订单审批工作流已启用。",
                "購買発注の承認ワークフローが有効です。");
        put("focus.admin.audit", "Export audit logs for compliance review.",
                "导出审计日志用于合规复核。",
                "コンプライアンス確認用に監査ログをエクスポートしてください。");

        put("term.organization", "Organization", "组织", "組織");
        put("term.itemMaster", "Item master", "物料主数据", "品目マスタ");
        put("term.customerMaster", "Customer master", "客户主数据", "得意先マスタ");
        put("term.supplierMaster", "Supplier master", "供应商主数据", "仕入先マスタ");
        put("term.warehouseMaster", "Warehouse master", "仓库主数据", "倉庫マスタ");
        put("term.bom", "BOM", "BOM", "BOM");
        put("term.routing", "Routing", "工艺路线", "工程順序");
        put("term.quotation", "Quotation", "报价单", "見積");
        put("term.salesOrder", "Sales order", "销售订单", "販売受注");
        put("term.delivery", "Delivery", "交货", "納品");
        put("term.shipment", "Shipment", "出货", "出荷");
        put("term.billing", "Billing", "开票", "請求");
        put("term.receivable", "Receivable", "应收", "売掛");
        put("term.purchaseRequest", "Purchase request", "采购申请", "購買依頼");
        put("term.purchaseOrder", "Purchase order", "采购订单", "購買発注");
        put("term.receipt", "Receipt", "收货", "入荷");
        put("term.invoiceCheck", "Invoice verification", "发票校验", "請求書照合");
        put("term.payable", "Payable", "应付", "買掛");
        put("term.stockOverview", "Stock overview", "库存总览", "在庫照会");
        put("term.lotTrace", "Lot trace", "批次追溯", "ロット追跡");
        put("term.transfer", "Transfer", "库存调拨", "在庫振替");
        put("term.adjustment", "Adjustment", "库存调整", "在庫調整");
        put("term.cycleCount", "Cycle count", "循环盘点", "循環棚卸");
        put("term.mrpRun", "MRP run", "MRP 运算", "MRP 実行");
        put("term.productionOrder", "Production order", "生产订单", "製造指図");
        put("term.weeklyPlan", "Weekly plan", "周计划", "週間計画");
        put("term.materialIssue", "Material issue", "生产领料", "材料払出");
        put("term.confirmation", "Confirmation", "生产报工", "実績報告");
        put("term.costing", "Costing", "成本归集", "原価集計");
        put("term.ar", "Accounts receivable", "应收账款", "売掛金");
        put("term.ap", "Accounts payable", "应付账款", "買掛金");
        put("term.gl", "General ledger", "总账", "総勘定元帳");
        put("term.inventoryValuation", "Inventory valuation", "库存估价", "在庫評価");
        put("term.close", "Period close", "期间结账", "期間締め");
        put("term.users", "Users", "用户", "ユーザー");
        put("term.roles", "Roles", "角色", "ロール");
        put("term.permissions", "Permissions", "权限", "権限");
        put("term.workflow", "Workflow", "工作流", "ワークフロー");
        put("term.audit", "Audit log", "审计日志", "監査ログ");
        put("term.query", "Natural query", "自然语言查询", "自然言語検索");
        put("term.explain", "Exception explanation", "异常解释", "例外説明");
        put("term.summary", "Report summary", "报表总结", "レポート要約");

        put("ai.placeholder", "Example: Which materials may block production orders next week?",
                "示例：下周哪些物料可能影响生产订单？",
                "例：来週の製造指図を止める可能性がある品目は？");
        put("ai.answer", "Sample insight: RM-1008 and PK-2210 require purchase follow-up. MO-2608-004 should be replanned if PO-45000127 is not received by Aug 20.",
                "示例洞察：RM-1008 与 PK-2210 需要跟进采购；如果 PO-45000127 在 8 月 20 日前未收货，MO-2608-004 需要重排。",
                "サンプル洞察：RM-1008 と PK-2210 は購買フォローが必要です。PO-45000127 が 8月20日 までに入荷しない場合、MO-2608-004 の再計画が必要です。");
    }

    private I18n() {
    }

    public static String t(Language language, String key) {
        Language actualLanguage = language == null ? DEFAULT_LANGUAGE : language;
        Map<String, String> localized = TEXTS.get(actualLanguage);
        if (localized != null && localized.containsKey(key)) {
            return localized.get(key);
        }
        Map<String, String> english = TEXTS.get(Language.EN);
        if (english != null && english.containsKey(key)) {
            return english.get(key);
        }
        return "!" + key + "!";
    }

    public static String textOrValue(Language language, String value) {
        if (value == null) {
            return "";
        }
        if (has(value)) {
            return t(language, value);
        }
        return value;
    }

    public static boolean has(String key) {
        Map<String, String> english = TEXTS.get(Language.EN);
        return english != null && english.containsKey(key);
    }

    public static Locale locale(Language language) {
        if (language == Language.ZH) {
            return Locale.SIMPLIFIED_CHINESE;
        }
        if (language == Language.JA) {
            return Locale.JAPANESE;
        }
        return Locale.ENGLISH;
    }

    private static void put(String key, String en, String zh, String ja) {
        TEXTS.get(Language.EN).put(key, en);
        TEXTS.get(Language.ZH).put(key, zh);
        TEXTS.get(Language.JA).put(key, ja);
    }
}
