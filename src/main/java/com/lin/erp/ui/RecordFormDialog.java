package com.lin.erp.ui;

import com.lin.erp.i18n.I18n;
import com.lin.erp.i18n.Language;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RecordFormDialog extends JDialog {
    private final Language language;
    private final String[] columnKeys;
    private final JComponent[] editors;
    private boolean saved;

    public RecordFormDialog(Window owner, Language language, String moduleName, String actionName,
                            String actionKey, String[] columnKeys, String[] initialValues) {
        super(owner, actionName + " - " + moduleName, ModalityType.APPLICATION_MODAL);
        this.language = language;
        this.columnKeys = columnKeys;
        this.editors = new JComponent[columnKeys.length];

        setIconImages(AppIcon.images());
        setMinimumSize(new Dimension(780, 580));
        setContentPane(createContent(moduleName, actionName, actionKey, initialValues));
        pack();
        setLocationRelativeTo(owner);
    }

    public boolean isSaved() {
        return saved;
    }

    public String[] getValues() {
        String[] values = new String[columnKeys.length];
        for (int i = 0; i < editors.length; i++) {
            JComponent editor = editors[i];
            if (editor instanceof JComboBox) {
                Object item = ((JComboBox<?>) editor).getSelectedItem();
                values[i] = item == null ? "" : item.toString();
            } else if (editor instanceof JTextArea) {
                values[i] = ((JTextArea) editor).getText();
            } else if (editor instanceof JTextField) {
                values[i] = ((JTextField) editor).getText();
            }
        }
        return values;
    }

    private JPanel createContent(String moduleName, String actionName, String actionKey, String[] initialValues) {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.PAGE_BACKGROUND);
        root.add(createHeader(moduleName, actionName, actionKey), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);
        center.setBorder(AppTheme.emptyBorder(18, 20, 16, 20));
        center.add(createForm(initialValues), BorderLayout.CENTER);
        center.add(createInsightRail(moduleName), BorderLayout.EAST);
        root.add(center, BorderLayout.CENTER);
        root.add(createFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel createHeader(String moduleName, String actionName, String actionKey) {
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setBackground(AppTheme.PAGE_BACKGROUND);
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 235, 244)),
                AppTheme.emptyBorder(18, 20, 16, 20)
        ));

        JLabel icon = new JLabel("", SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(AppTheme.ACCENT_SOFT);
        icon.setIcon(new ActionIcon(actionKey, AppTheme.ACCENT));
        icon.setPreferredSize(new Dimension(46, 46));
        header.add(icon, BorderLayout.WEST);

        JPanel titles = new JPanel(new GridLayout(0, 1, 0, 4));
        titles.setOpaque(false);
        JLabel title = new JLabel(actionName);
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 21));
        JLabel subtitle = new JLabel(moduleName + " / " + t("form.transactionRecord"));
        subtitle.setForeground(AppTheme.TEXT_MUTED);
        subtitle.setFont(AppTheme.font(Font.PLAIN, 12));
        titles.add(title);
        titles.add(subtitle);
        header.add(titles, BorderLayout.CENTER);
        return header;
    }

    private JScrollPane createForm(String[] initialValues) {
        RoundedPanel card = new RoundedPanel(Color.WHITE, 8);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 18, 16)
        ));

        JLabel sectionTitle = new JLabel(t("form.transactionRecord"));
        sectionTitle.setForeground(AppTheme.TEXT_PRIMARY);
        sectionTitle.setFont(AppTheme.font(Font.BOLD, 15));
        card.add(sectionTitle, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new MatteBorder(1, 0, 0, 0, new Color(238, 242, 247)));

        for (int i = 0; i < columnKeys.length; i++) {
            GridBagConstraints labelGbc = new GridBagConstraints();
            labelGbc.gridx = i % 2 == 0 ? 0 : 2;
            labelGbc.gridy = (i / 2) * 2;
            labelGbc.anchor = GridBagConstraints.WEST;
            labelGbc.insets = new Insets(14, 0, 6, 10);

            JLabel label = new JLabel(I18n.textOrValue(language, columnKeys[i]));
            label.setForeground(AppTheme.TEXT_MUTED);
            label.setFont(AppTheme.font(Font.BOLD, 11));
            form.add(label, labelGbc);

            GridBagConstraints fieldGbc = new GridBagConstraints();
            fieldGbc.gridx = labelGbc.gridx;
            fieldGbc.gridy = labelGbc.gridy + 1;
            fieldGbc.fill = GridBagConstraints.HORIZONTAL;
            fieldGbc.weightx = 1;
            fieldGbc.insets = new Insets(0, 0, 18, i % 2 == 0 ? 18 : 0);

            JComponent editor = createEditor(columnKeys[i], valueAt(initialValues, i));
            editors[i] = editor;
            form.add(editor, fieldGbc);
        }

        card.add(form, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(card, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        return scrollPane;
    }

    private JPanel createInsightRail(String moduleName) {
        RoundedPanel rail = new RoundedPanel(Color.WHITE, 8);
        rail.setLayout(new BorderLayout(0, 12));
        rail.setPreferredSize(new Dimension(220, 1));
        rail.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 235, 244)),
                AppTheme.emptyBorder(16, 16, 16, 16)
        ));

        JLabel title = new JLabel(t("form.side.title"));
        title.setForeground(AppTheme.TEXT_PRIMARY);
        title.setFont(AppTheme.font(Font.BOLD, 14));
        rail.add(title, BorderLayout.NORTH);

        JTextArea body = new JTextArea(
                t("form.side.module") + ": " + moduleName + "\n\n"
                        + t("form.side.audit") + "\n"
                        + t("form.side.validation") + "\n"
                        + t("form.side.workflow")
        );
        body.setEditable(false);
        body.setLineWrap(true);
        body.setWrapStyleWord(true);
        body.setOpaque(false);
        body.setForeground(AppTheme.TEXT_MUTED);
        body.setFont(AppTheme.font(Font.PLAIN, 12));
        rail.add(body, BorderLayout.CENTER);
        return rail;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setBackground(AppTheme.PAGE_BACKGROUND);
        footer.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(229, 235, 244)),
                AppTheme.emptyBorder(14, 20, 14, 20)
        ));

        JButton cancel = new JButton(t("form.cancel"));
        cancel.putClientProperty("JButton.buttonType", "roundRect");
        cancel.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 0");
        cancel.setBackground(Color.WHITE);
        cancel.setForeground(AppTheme.TEXT_PRIMARY);
        cancel.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(213, 222, 235)),
                AppTheme.emptyBorder(8, 18, 8, 18)
        ));
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saved = false;
                dispose();
            }
        });
        footer.add(cancel);

        JButton save = new JButton(t("form.save"));
        save.putClientProperty("JButton.buttonType", "roundRect");
        save.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 0; focusWidth: 0");
        save.setBackground(AppTheme.ACCENT);
        save.setForeground(Color.WHITE);
        save.setBorder(AppTheme.emptyBorder(8, 20, 8, 20));
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saved = true;
                dispose();
            }
        });
        footer.add(save);
        return footer;
    }

    private JComponent createEditor(String columnKey, String value) {
        if ("column.status".equals(columnKey)) {
            return styledCombo(new String[]{
                    "status.open", "status.draft", "status.waitingApproval", "status.ready",
                    "status.released", "status.posted", "status.blocked", "status.late", "status.shortage"
            }, value);
        }
        if ("column.risk".equals(columnKey)) {
            return styledCombo(new String[]{"risk.low", "risk.medium", "risk.high"}, value);
        }
        if ("column.next".equals(columnKey)) {
            return styledCombo(new String[]{
                    "action.details", "action.edit", "action.approve", "action.release", "action.post",
                    "action.simulate", "term.purchaseRequest", "term.purchaseOrder", "term.materialIssue",
                    "term.shipment", "term.confirmation", "term.costing"
            }, value);
        }
        JTextField field = new JTextField(value == null ? "" : value);
        styleField(field);
        return field;
    }

    private JComboBox<String> styledCombo(String[] values, String selectedValue) {
        JComboBox<String> comboBox = new JComboBox<String>(values);
        if (selectedValue != null && selectedValue.trim().length() > 0) {
            comboBox.setSelectedItem(selectedValue);
        }
        comboBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setText(I18n.textOrValue(language, value == null ? "" : value.toString()));
                return label;
            }
        });
        comboBox.setPreferredSize(new Dimension(220, 36));
        comboBox.setBackground(new Color(248, 250, 252));
        comboBox.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        comboBox.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 1");
        return comboBox;
    }

    private void styleField(JTextField field) {
        field.setPreferredSize(new Dimension(220, 36));
        field.setForeground(AppTheme.TEXT_PRIMARY);
        field.setBackground(new Color(248, 250, 252));
        field.putClientProperty("JComponent.roundRect", Boolean.TRUE);
        field.putClientProperty("FlatLaf.style", "arc: 8; borderWidth: 1; focusWidth: 1");
        field.setBorder(new CompoundBorder(
                BorderFactory.createLineBorder(new Color(222, 229, 238)),
                AppTheme.emptyBorder(0, 11, 0, 11)
        ));
    }

    private String valueAt(String[] values, int index) {
        if (values == null || index >= values.length || values[index] == null) {
            return "";
        }
        return values[index];
    }

    private String t(String key) {
        return I18n.t(language, key);
    }
}
