package com.lin.erp.ui;

import com.lin.erp.auth.UserSession;
import com.lin.erp.db.MenuNode;
import com.lin.erp.i18n.I18n;
import com.lin.erp.i18n.Language;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;

public class AiAssistantPanel extends JPanel {
    private final UserSession session;
    private final MenuNode function;
    private final AiPageContent content;

    public AiAssistantPanel(Window owner, UserSession session, MenuNode function) {
        this.session = session;
        this.function = function;
        this.content = contentFor(function == null ? "" : function.getCode());
        buildPage();
    }

    private void buildPage() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.PAGE_BACKGROUND);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        body.setBorder(AppTheme.emptyBorder(18, 18, 22, 18));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 14, 0);

        gbc.gridy = 0;
        body.add(createHeaderCard(), gbc);

        gbc.gridy = 1;
        body.add(createPromptCard(), gbc);

        gbc.gridy = 2;
        body.add(createInsightGrid(), gbc);

        gbc.gridy = 3;
        body.add(createTableCard(), gbc);

        gbc.gridy = 4;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        body.add(filler, gbc);

        JScrollPane scrollPane = new JScrollPane(body);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderCard() {
        RoundedPanel panel = new RoundedPanel(Color.WHITE, 10);
        panel.setLayout(new BorderLayout(12, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(18, 20, 18, 20)
        ));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(I18n.t(language(), function.getNameKey()));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 24));
        JLabel subtitle = new JLabel(content.subtitle.text(language()));
        subtitle.setForeground(AppTheme.TEXT_MUTED);
        subtitle.setFont(AppTheme.font(Font.PLAIN, 13));
        textBlock.add(title);
        textBlock.add(Box.createVerticalStrut(6));
        textBlock.add(subtitle);
        panel.add(textBlock, BorderLayout.CENTER);

        JLabel mode = createPill(content.mode.text(language()), content.accent);
        panel.add(mode, BorderLayout.EAST);
        return panel;
    }

    private JPanel createPromptCard() {
        RoundedPanel panel = new RoundedPanel(Color.WHITE, 10);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(16, 18, 16, 18)
        ));

        JLabel title = new JLabel(content.promptTitle.text(language()));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 15));
        panel.add(title, BorderLayout.NORTH);

        JTextArea prompt = new JTextArea(content.promptText.text(language()));
        prompt.setEditable(false);
        prompt.setLineWrap(true);
        prompt.setWrapStyleWord(true);
        prompt.setFont(AppTheme.font(Font.PLAIN, 13));
        prompt.setForeground(AppTheme.TEXT_PRIMARY);
        prompt.setBackground(new Color(250, 252, 255));
        prompt.setBorder(AppTheme.emptyBorder(12, 12, 12, 12));
        panel.add(prompt, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createInsightGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 12, 0));
        grid.setOpaque(false);
        for (AiCard card : content.cards) {
            grid.add(createInsightCard(card));
        }
        return grid;
    }

    private JPanel createInsightCard(AiCard card) {
        RoundedPanel panel = new RoundedPanel(Color.WHITE, 10);
        panel.setLayout(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(14, 14, 14, 14)
        ));

        JLabel title = new JLabel(card.title.text(language()));
        title.setForeground(card.color);
        title.setFont(AppTheme.font(Font.BOLD, 14));
        panel.add(title, BorderLayout.NORTH);

        JTextArea body = new JTextArea(card.body.text(language()));
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setFont(AppTheme.font(Font.PLAIN, 12));
        body.setForeground(AppTheme.TEXT_PRIMARY);
        body.setBackground(Color.WHITE);
        body.setBorder(null);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTableCard() {
        RoundedPanel panel = new RoundedPanel(Color.WHITE, 10);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                AppTheme.emptyBorder(16, 18, 16, 18)
        ));

        JLabel title = new JLabel(content.tableTitle.text(language()));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 15));
        panel.add(title, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(localized(content.tableRows), localized(content.tableColumns)) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setSelectionBackground(AppTheme.ACCENT_SOFT);
        table.setSelectionForeground(AppTheme.TEXT_PRIMARY);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(AppTheme.font(Font.BOLD, 12));
        header.setBackground(new Color(242, 246, 251));
        header.setForeground(AppTheme.TEXT_PRIMARY);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(AppTheme.BORDER));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JLabel createPill(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(new Color(
                Math.min(255, color.getRed() + 214),
                Math.min(255, color.getGreen() + 92),
                Math.min(255, color.getBlue() + 92)
        ));
        label.setForeground(color.darker());
        label.setFont(AppTheme.font(Font.BOLD, 12));
        label.setBorder(AppTheme.emptyBorder(8, 14, 8, 14));
        return label;
    }

    private Object[] localized(LocalText[] values) {
        Object[] result = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i].text(language());
        }
        return result;
    }

    private Object[][] localized(LocalText[][] rows) {
        Object[][] result = new Object[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            result[i] = localized(rows[i]);
        }
        return result;
    }

    private Language language() {
        return session == null ? Language.EN : session.getLanguage();
    }

    private static AiPageContent contentFor(String code) {
        if ("AI_EXPLAIN".equals(code)) {
            return exceptionExplanation();
        }
        if ("AI_SUMMARY".equals(code)) {
            return reportSummary();
        }
        return naturalQuery();
    }

    private static AiPageContent naturalQuery() {
        return new AiPageContent(
                tr("Read-only business query", "只读业务查询", "読み取り専用業務検索"),
                tr("Ask permitted ERP data in natural language and review a traceable sample answer.",
                        "用自然语言查询有权限的 ERP 数据，并查看可追溯的模拟回答。",
                        "自然言語で許可された ERP データを検索し、追跡可能なサンプル回答を確認します。"),
                tr("Query prompt workspace", "查询提示词工作区", "検索プロンプトワークスペース"),
                tr("Example questions:\n- Which materials may block next week's production orders?\n- Which purchase orders are late and affect manufacturing?\n- Which sales orders have shipment risk this week?\n\nThe answer area should show source modules, filtered fields, and read-only facts.",
                        "示例问题：\n- 哪些物料可能影响下周生产订单？\n- 哪些采购订单延期并影响制造？\n- 本周哪些销售订单存在出货风险？\n\n回答区域应显示来源模块、权限过滤字段和只读事实。",
                        "質問例：\n- 来週の製造指図を止める可能性がある品目はどれですか？\n- 遅延して製造に影響する購買発注はどれですか？\n- 今週の出荷リスクがある販売受注はどれですか？\n\n回答エリアには、参照モジュール、権限で絞り込んだ項目、読み取り専用の事実を表示します。"),
                tr("Query examples and data scope", "查询示例和数据范围", "検索例とデータ範囲"),
                new AiCard[]{
                        card(tr("Data scope", "数据范围", "データ範囲"), tr("Inventory, MRP, purchase, sales, shipment, and finance summaries. Access is filtered by role and organization.",
                                "库存、MRP、采购、销售、出货和财务摘要。访问范围按角色和组织过滤。",
                                "在庫、MRP、購買、販売、出荷、財務サマリー。アクセス範囲はロールと組織で絞り込みます。"), AppTheme.ACCENT),
                        card(tr("Sample answer", "模拟回答", "サンプル回答"), tr("RM-1008 is below safety stock. PO-45000127 is late by 2 days and affects MO-2608-004. Suggested next step: expedite receipt or reschedule the order.",
                                "RM-1008 低于安全库存。PO-45000127 延期 2 天，并影响 MO-2608-004。建议下一步：催收到货或重排生产订单。",
                                "RM-1008 は安全在庫を下回っています。PO-45000127 は 2 日遅延し、MO-2608-004 に影響しています。次の対応案：入荷を前倒しするか製造指図を再計画します。"), AppTheme.WARNING),
                        card(tr("Control points", "控制点", "制御ポイント"), tr("No write-back from the answer. Prompt, user, data source, and timestamp are logged for audit review.",
                                "回答不回写业务数据。提示词、用户、数据源和时间会进入审计日志。",
                                "回答から業務データへの書き戻しは行いません。プロンプト、ユーザー、データソース、時刻を監査ログに記録します。"), AppTheme.SUCCESS)
                },
                cols(),
                new LocalText[][]{
                        row("Inventory + MRP", "库存 + MRP", "在庫 + MRP", "Shortage risk by item", "按品目查询缺料风险", "品目別欠品リスク", "RM-1008 shortage affects MO-2608-004", "RM-1008 缺料影响 MO-2608-004", "RM-1008 の欠品が MO-2608-004 に影響"),
                        row("Purchase orders", "采购订单", "購買発注", "Late inbound impact", "延期到货影响", "遅延入荷の影響", "PO-45000127 is late by 2 days", "PO-45000127 延期 2 天", "PO-45000127 は 2 日遅延"),
                        row("Sales + shipment", "销售 + 出货", "販売 + 出荷", "Delivery risk", "交付风险", "納期リスク", "SO-2608-104 needs stock confirmation", "SO-2608-104 需要库存确认", "SO-2608-104 は在庫確認が必要")
                },
                AppTheme.ACCENT
        );
    }

    private static AiPageContent exceptionExplanation() {
        return new AiPageContent(
                tr("Exception root-cause analysis", "异常原因分析", "例外原因分析"),
                tr("Explain blocked or late ERP documents with source evidence and follow-up actions.",
                        "结合来源证据解释阻塞、延期等异常单据，并给出后续处理动作。",
                        "ブロックや遅延のある ERP 伝票を根拠付きで説明し、後続アクションを提示します。"),
                tr("Exception investigation", "异常调查", "例外調査"),
                tr("Selected exception:\nMO-2608-004 is blocked before release.\n\nAnalysis path:\n1. Check BOM demand and MRP shortage.\n2. Trace purchase order and inbound receipt.\n3. Verify approval and inventory movement logs.\n4. Produce a business explanation and next action.",
                        "选中异常：\nMO-2608-004 下达前被阻塞。\n\n分析路径：\n1. 检查 BOM 需求和 MRP 缺料。\n2. 追踪采购订单和到货入库。\n3. 校验审批与库存移动日志。\n4. 输出业务解释和下一步动作。",
                        "選択した例外：\nMO-2608-004 はリリース前にブロックされています。\n\n分析手順：\n1. BOM 需要と MRP 欠品を確認します。\n2. 購買発注と入荷を追跡します。\n3. 承認と在庫移動ログを確認します。\n4. 業務説明と次アクションを出力します。"),
                tr("Exception chain and recommended handling", "异常链路和处理建议", "例外チェーンと対応案"),
                new AiCard[]{
                        card(tr("Cause chain", "原因链路", "原因チェーン"), tr("MO demand consumes RM-1008. Current stock is below safety stock. The linked purchase order is late, so release is blocked.",
                                "生产需求消耗 RM-1008。当前库存低于安全库存，关联采购订单延期，因此生产下达被阻塞。",
                                "製造需要が RM-1008 を消費します。現在在庫は安全在庫を下回り、関連する購買発注が遅延しているためリリースがブロックされています。"), AppTheme.ERROR),
                        card(tr("Evidence", "证据", "根拠"), tr("BOM line RM-1008, stock ledger WH-A, PO-45000127, and MRP run MRP-2608-W34 are linked in the audit view.",
                                "BOM 行 RM-1008、WH-A 库存流水、PO-45000127 和 MRP-2608-W34 会在审计视图中关联展示。",
                                "BOM 明細 RM-1008、WH-A 在庫元帳、PO-45000127、MRP-2608-W34 を監査ビューで関連付けて表示します。"), AppTheme.WARNING),
                        card(tr("Actions", "处理动作", "対応アクション"), tr("Expedite inbound receipt, substitute approved material, or reschedule MO-2608-004. Every action requires normal ERP authorization.",
                                "催收到货、替换已批准替代料，或重排 MO-2608-004。所有动作仍需走 ERP 正常权限。",
                                "入荷を前倒しする、承認済み代替品を使う、または MO-2608-004 を再計画します。すべての操作は通常の ERP 権限が必要です。"), AppTheme.SUCCESS)
                },
                cols(),
                new LocalText[][]{
                        row("Production order", "生产订单", "製造指図", "Blocked release", "下达阻塞", "リリースブロック", "MO-2608-004 waits for material availability", "MO-2608-004 等待物料可用", "MO-2608-004 は品目利用可能待ち"),
                        row("Inventory ledger", "库存流水", "在庫元帳", "Safety stock breach", "安全库存不足", "安全在庫割れ", "RM-1008 available quantity below threshold", "RM-1008 可用量低于阈值", "RM-1008 の利用可能数量がしきい値未満"),
                        row("Purchase order", "采购订单", "購買発注", "Late receipt", "延期到货", "入荷遅延", "PO-45000127 expected receipt moved out", "PO-45000127 预计到货后移", "PO-45000127 の入荷予定が後ろ倒し")
                },
                AppTheme.ERROR
        );
    }

    private static AiPageContent reportSummary() {
        return new AiPageContent(
                tr("Report summary workspace", "报表总结工作区", "レポート要約ワークスペース"),
                tr("Turn permitted ERP reports into a short management note with KPIs, risks, and next steps.",
                        "把有权限的 ERP 报表整理成管理层摘要，包含指标、风险和下一步。",
                        "許可された ERP レポートを、KPI、リスク、次アクションを含む管理サマリーに変換します。"),
                tr("Summary setup", "总结设置", "要約設定"),
                tr("Report package:\n- Sales detail and shipment status\n- Purchase execution and late receipt list\n- Inventory risk and AR/AP balance\n\nOutput format:\nExecutive summary, KPI snapshot, major risks, and action owners.",
                        "报表包：\n- 销售明细和出货状态\n- 采购执行和延期到货清单\n- 库存风险和应收/应付余额\n\n输出格式：\n管理摘要、关键指标、主要风险和行动负责人。",
                        "レポートパッケージ：\n- 販売明細と出荷状況\n- 購買実行と遅延入荷一覧\n- 在庫リスクと売掛/買掛残高\n\n出力形式：\n経営サマリー、KPI スナップショット、主要リスク、アクション担当者。"),
                tr("Management summary sample", "管理层摘要示例", "経営サマリー例"),
                new AiCard[]{
                        card(tr("KPI snapshot", "关键指标", "KPI"), tr("Sales backlog: 16 orders. Material shortage risks: 5 items. Late purchase orders: 2. AR open balance: $318,400.",
                                "销售待交付：16 单。缺料风险：5 个品目。延期采购订单：2 单。应收未清：$318,400。",
                                "販売未出荷：16 件。欠品リスク：5 品目。遅延購買発注：2 件。未回収売掛：$318,400。"), AppTheme.ACCENT),
                        card(tr("Risk summary", "风险摘要", "リスク要約"), tr("The main risk is RM-1008 shortage affecting production and shipment commitments. Finance should watch AR collection for large customers.",
                                "主要风险是 RM-1008 缺料影响生产和出货承诺。财务需关注大客户应收回款。",
                                "主なリスクは RM-1008 欠品による製造と出荷約束への影響です。財務は大口顧客の売掛回収を確認します。"), AppTheme.WARNING),
                        card(tr("Next steps", "下一步", "次の対応"), tr("Procurement owns supplier follow-up. Planning owns rescheduling. Finance owns AR collection review. Export keeps the source report links.",
                                "采购负责供应商跟进，计划负责重排，财务负责应收回款复核。导出保留来源报表链接。",
                                "購買はサプライヤーフォロー、計画は再計画、財務は売掛回収確認を担当します。エクスポートには元レポートのリンクを残します。"), AppTheme.SUCCESS)
                },
                cols(),
                new LocalText[][]{
                        row("Sales report", "销售报表", "販売レポート", "Shipment backlog", "出货积压", "出荷滞留", "16 orders need delivery review", "16 单需要交付复核", "16 件は納期確認が必要"),
                        row("Inventory report", "库存报表", "在庫レポート", "Shortage risk", "缺料风险", "欠品リスク", "5 items below planning threshold", "5 个品目低于计划阈值", "5 品目が計画しきい値未満"),
                        row("Finance report", "财务报表", "財務レポート", "AR/AP focus", "应收应付关注", "売掛/買掛確認", "AR collection and AP cash timing need review", "需复核应收回款和应付现金节奏", "売掛回収と買掛支払時期を確認")
                },
                AppTheme.SUCCESS
        );
    }

    private static LocalText[] cols() {
        return new LocalText[]{
                tr("Source", "来源", "ソース"),
                tr("Focus", "关注点", "確認観点"),
                tr("Example output", "示例输出", "出力例")
        };
    }

    private static LocalText[] row(String sourceEn, String sourceZh, String sourceJa,
                                   String focusEn, String focusZh, String focusJa,
                                   String outputEn, String outputZh, String outputJa) {
        return new LocalText[]{tr(sourceEn, sourceZh, sourceJa), tr(focusEn, focusZh, focusJa), tr(outputEn, outputZh, outputJa)};
    }

    private static AiCard card(LocalText title, LocalText body, Color color) {
        return new AiCard(title, body, color);
    }

    private static LocalText tr(String en, String zh, String ja) {
        return new LocalText(en, zh, ja);
    }

    private static final class AiPageContent {
        private final LocalText mode;
        private final LocalText subtitle;
        private final LocalText promptTitle;
        private final LocalText promptText;
        private final LocalText tableTitle;
        private final AiCard[] cards;
        private final LocalText[] tableColumns;
        private final LocalText[][] tableRows;
        private final Color accent;

        private AiPageContent(LocalText mode, LocalText subtitle, LocalText promptTitle, LocalText promptText,
                              LocalText tableTitle, AiCard[] cards, LocalText[] tableColumns,
                              LocalText[][] tableRows, Color accent) {
            this.mode = mode;
            this.subtitle = subtitle;
            this.promptTitle = promptTitle;
            this.promptText = promptText;
            this.tableTitle = tableTitle;
            this.cards = cards;
            this.tableColumns = tableColumns;
            this.tableRows = tableRows;
            this.accent = accent;
        }
    }

    private static final class AiCard {
        private final LocalText title;
        private final LocalText body;
        private final Color color;

        private AiCard(LocalText title, LocalText body, Color color) {
            this.title = title;
            this.body = body;
            this.color = color;
        }
    }

    private static final class LocalText {
        private final String en;
        private final String zh;
        private final String ja;

        private LocalText(String en, String zh, String ja) {
            this.en = en;
            this.zh = zh;
            this.ja = ja;
        }

        private String text(Language language) {
            if (language == Language.ZH) {
                return zh;
            }
            if (language == Language.JA) {
                return ja;
            }
            return en;
        }
    }
}
