package com.phonestore.ui.inventory;

import com.phonestore.model.Customer;
import com.phonestore.model.ExportReceipt;
import com.phonestore.model.ExportReceiptLine;
import com.phonestore.util.MoneyVND;
import com.phonestore.util.invoice.InstallmentInfo;
import com.phonestore.util.payment.PaymentConfig;
import com.phonestore.util.qr.VietQrPayloadBuilder;
import com.phonestore.util.qr.ZxingQr;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExportReceiptPaymentDialog extends JDialog {

    public enum Result {
        CANCELLED,
        CASH_FULL,
        CASH_INSTALLMENT,
        TRANSFER_CONFIRMED
    }

    private Result result = Result.CANCELLED;

    private boolean printRequested;
    private String paymentRefToPersist;
    private Long cashGivenAmount;

    private final JRadioButton rbCash = new JRadioButton("Ti\u1ec1n m\u1eb7t");
    private final JRadioButton rbTransfer = new JRadioButton("Chuy\u1ec3n kho\u1ea3n (QR)");

    private final JButton btnCashFull = new JButton("Thanh to\u00e1n h\u1ebft");
    private final JButton btnCashInstallment = new JButton("Tr\u1ea3 g\u00f3p");

    private final JComboBox<Integer> cbMonths = new JComboBox<>(new Integer[]{12, 6, 3});
    private final JTextField txtUpfront = new JTextField();
    private final JLabel lblRemaining = new JLabel("0");
    private final JLabel lblMonthly = new JLabel("0");
    private final JButton btnInstallmentConfirm = new JButton("X\u00e1c nh\u1eadn");

    private final JLabel qrLabel = new JLabel("", SwingConstants.CENTER);
    private final JTextArea transferInfo = new JTextArea();
    private final JButton btnTransferConfirmed = new JButton("X\u00e1c nh\u1eadn \u0111\u00e3 chuy\u1ec3n");
    private boolean transferConfirmed;
    private final JLabel lblTransferStatus = new JLabel("");
    private ScheduledExecutorService transferPoller;

    private final JTextField txtCustName = new JTextField();
    private final JTextField txtCustPhone = new JTextField();
    private final JTextField txtCustEmail = new JTextField();
    private final JTextField txtCustAddress = new JTextField();
    private final JTextField txtPayTotal = new JTextField();

    private final LineTableModel lineTableModel = new LineTableModel();
    private final JTable tblLines = new JTable(lineTableModel);

    private final JTabbedPane tabs = new JTabbedPane();
    private final JPanel step2Cards = new JPanel(new CardLayout());
    private static final String CARD_INSTALLMENT = "installment";
    private static final String CARD_TRANSFER = "transfer";
    private static final String CARD_EMPTY = "empty";

    private final ExportReceipt draft;
    private final Customer customer;
    private final String transferRef;

    public ExportReceiptPaymentDialog(Window owner, ExportReceipt draft, Customer customer) {
        super(owner, "Thanh to\u00e1n", ModalityType.APPLICATION_MODAL);
        this.draft = draft;
        this.customer = customer;
        this.transferRef = resolveTransferReference(draft);
        initUi();
    }

    private String generateTransferReference() {
        String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rnd = 1000 + new java.util.Random().nextInt(9000);
        return "PX" + ts + rnd;
    }

    private String resolveTransferReference(ExportReceipt receipt) {
        if (receipt != null) {
            String code = safe(receipt.getReceiptCode());
            if (!code.isBlank() && !code.endsWith("#NEW")) return code;
        }
        return generateTransferReference();
    }

    public Result getResult() {
        return result;
    }

    public String getPaymentMethodLabel() {
        if (result == Result.TRANSFER_CONFIRMED) return "Chuy\u1ec3n kho\u1ea3n";
        if (result == Result.CASH_INSTALLMENT) return "Tr\u1ea3 g\u00f3p";
        return "Ti\u1ec1n m\u1eb7t";
    }

    public boolean isPrintRequested() {
        return printRequested;
    }

    public String getPaymentRefToPersist() {
        return paymentRefToPersist;
    }

    public long getEditedTotalAmount() {
        return parseMoneyToLong(txtPayTotal.getText());
    }

    public Customer getEditedCustomer() {
        Customer c = new Customer();
        if (customer != null) {
            c.setId(customer.getId());
            c.setStatus(customer.getStatus());
        }
        c.setName(safe(txtCustName.getText()));
        c.setPhone(safe(txtCustPhone.getText()));
        c.setEmail(safe(txtCustEmail.getText()));
        c.setAddress(safe(txtCustAddress.getText()));
        return c;
    }

    public String getTransferReference() {
        return transferRef;
    }

    private void initUi() {
        setSize(960, 680);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout(10, 10));

        tabs.addTab("Ph\u01b0\u01a1ng th\u1ee9c", buildStep1());
        tabs.addTab("Tr\u1ea3 g\u00f3p/QR", buildStep2());
        tabs.addTab("Th\u00f4ng tin", buildStep3());
        add(tabs, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClose = new JButton("\u0110\u00f3ng");
        btnClose.addActionListener(e -> {
            result = Result.CANCELLED;
            dispose();
        });
        actions.add(btnClose);
        add(actions, BorderLayout.SOUTH);

        bindDefaults();
        wireEvents();
    }

    private JComponent buildStep1() {
        JPanel p = new JPanel(new BorderLayout(12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel heading = new JLabel("Chọn phương thức thanh toán:");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        p.add(heading, BorderLayout.NORTH);

        ButtonGroup g = new ButtonGroup();
        g.add(rbCash);
        g.add(rbTransfer);
        rbCash.setSelected(true);

        // two-column center panel: cash options | transfer info
        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));

        JPanel cashBox = new JPanel(new BorderLayout(6, 6));
        cashBox.setBorder(BorderFactory.createTitledBorder("Tiền mặt tại cửa hàng"));
        JPanel cashBtns = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
        stylePrimaryButton(btnCashFull);
        stylePrimaryButton(btnCashInstallment);
        btnCashFull.setPreferredSize(new Dimension(160, 36));
        btnCashInstallment.setPreferredSize(new Dimension(160, 36));
        cashBtns.add(btnCashFull);
        cashBtns.add(btnCashInstallment);
        JPanel cashTop = new JPanel(new GridLayout(2, 1));
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        radios.add(rbCash);
        radios.add(rbTransfer);
        cashTop.add(radios);
        cashTop.add(new JLabel("Chọn Thanh toán tại cửa hàng hoặc chuyển khoản."));
        cashBox.add(cashTop, BorderLayout.NORTH);
        cashBox.add(cashBtns, BorderLayout.CENTER);

        JPanel transferHint = new JPanel(new BorderLayout(6, 6));
        transferHint.setBorder(BorderFactory.createTitledBorder("Chuyển khoản (QR)"));
        JLabel hint = new JLabel("Chọn Chuyển khoản (QR) rồi sang tab Trả góp/QR để xác nhận.");
        hint.setVerticalAlignment(SwingConstants.TOP);
        transferHint.add(hint, BorderLayout.NORTH);

        center.add(cashBox);
        center.add(transferHint);
        p.add(center, BorderLayout.CENTER);

        return p;
    }

    private JComponent buildStep2() {
        step2Cards.add(buildEmptyStep2(), CARD_EMPTY);
        step2Cards.add(buildInstallmentStep2(), CARD_INSTALLMENT);
        step2Cards.add(buildTransferStep2(), CARD_TRANSFER);
        return step2Cards;
    }

    private JComponent buildEmptyStep2() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        p.add(new JLabel("Ch\u1ecdn Tr\u1ea3 g\u00f3p ho\u1eb7c Chuy\u1ec3n kho\u1ea3n \u1edf tab Ph\u01b0\u01a1ng th\u1ee9c."), BorderLayout.NORTH);
        return p;
    }

    private JComponent buildInstallmentStep2() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        y = row(form, gbc, y, "S\u1ed1 ti\u1ec1n tr\u1ea3 tr\u01b0\u1edbc", txtUpfront);
        y = row(form, gbc, y, "S\u1ed1 ti\u1ec1n c\u1ea7n g\u00f3p", lblRemaining);
        y = row(form, gbc, y, "G\u00f3i tr\u1ea3 g\u00f3p", cbMonths);
        y = row(form, gbc, y, "M\u1ed7i th\u00e1ng", lblMonthly);

        p.add(form, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        stylePrimaryButton(btnInstallmentConfirm);
        btnInstallmentConfirm.setPreferredSize(new Dimension(140, 34));
        actions.add(btnInstallmentConfirm);
        p.add(actions, BorderLayout.SOUTH);

        return p;
    }

    private JComponent buildTransferStep2() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        qrLabel.setPreferredSize(new Dimension(320, 320));
        qrLabel.setBorder(BorderFactory.createTitledBorder("QR chuy\u1ec3n kho\u1ea3n"));
        qrLabel.setText("(\u0110ang t\u1ea1o QR...)");

        transferInfo.setEditable(false);
        transferInfo.setLineWrap(true);
        transferInfo.setWrapStyleWord(true);
        transferInfo.setText(buildTransferInfoText());

        p.add(qrLabel, BorderLayout.WEST);
        p.add(new JScrollPane(transferInfo), BorderLayout.CENTER);

        // status area and confirm button
        lblTransferStatus.setText("Chờ xác nhận chuyển khoản...");
        lblTransferStatus.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JPanel southWrap = new JPanel(new BorderLayout());
        southWrap.add(lblTransferStatus, BorderLayout.WEST);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        stylePrimaryButton(btnTransferConfirmed);
        btnTransferConfirmed.setPreferredSize(new Dimension(160, 34));
        bottom.add(btnTransferConfirmed);
        southWrap.add(bottom, BorderLayout.EAST);

        p.add(southWrap, BorderLayout.SOUTH);

        return p;
    }

    private JComponent buildStep3() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel left = new JPanel(new GridBagLayout());
        left.setBorder(BorderFactory.createTitledBorder("Th\u00f4ng tin kh\u00e1ch h\u00e0ng"));
        left.setPreferredSize(new Dimension(380, 260));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int y = 0;
        y = row(left, gbc, y, "H\u1ecd t\u00ean", txtCustName);
        y = row(left, gbc, y, "SDT", txtCustPhone);
        y = row(left, gbc, y, "Email", txtCustEmail);
        y = row(left, gbc, y, "\u0110\u1ecba ch\u1ec9", txtCustAddress);
        y = row(left, gbc, y, "T\u1ed5ng thanh to\u00e1n", txtPayTotal);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(BorderFactory.createTitledBorder("Th\u00f4ng tin \u0111i\u1ec7n tho\u1ea1i"));
        right.setPreferredSize(new Dimension(420, 260));
        tblLines.setRowHeight(28);
        tblLines.setFillsViewportHeight(true);
        tblLines.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLines.setDefaultEditor(Object.class, null);
        if (tblLines.getTableHeader() != null) tblLines.getTableHeader().setReorderingAllowed(false);
        right.add(new JScrollPane(tblLines), BorderLayout.CENTER);

        JPanel center = new JPanel(new GridLayout(1, 2, 10, 10));
        center.add(left);
        center.add(right);
        root.add(center, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnPrint = new JButton("In bill");
        JButton btnClose = new JButton("\u0110\u00f3ng");

        btnPrint.addActionListener(e -> onPrintClicked());
        btnClose.addActionListener(e -> {
            result = Result.CANCELLED;
            dispose();
        });

        stylePrimaryButton(btnPrint);
        btnPrint.setPreferredSize(new Dimension(120, 34));
        styleSecondaryButton(btnClose);
        btnClose.setPreferredSize(new Dimension(100, 34));
        actions.add(btnClose);
        actions.add(btnPrint);
        root.add(actions, BorderLayout.SOUTH);

        return root;
    }

    private int row(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
        return y + 1;
    }

    private void stylePrimaryButton(JButton b) {
        if (b == null) return;
        b.setBackground(new Color(33, 150, 243));
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
    }

    private void styleSecondaryButton(JButton b) {
        if (b == null) return;
        b.setBackground(new Color(238, 238, 238));
        b.setForeground(Color.BLACK);
        b.setOpaque(true);
        b.setBorderPainted(true);
    }

    private void bindDefaults() {
        String name = customer == null ? "" : safe(customer.getName());
        String phone = customer == null ? "" : safe(customer.getPhone());
        String email = customer == null ? "" : safe(customer.getEmail());
        String addr = customer == null ? "" : safe(customer.getAddress());

        txtCustName.setText(name);
        txtCustPhone.setText(phone);
        txtCustEmail.setText(email);
        txtCustAddress.setText(addr);

        long total = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
        txtPayTotal.setText(total <= 0 ? "" : MoneyVND.format(total));

        List<ExportReceiptLine> lines = draft == null ? null : draft.getLines();
        if (lines == null) lines = List.of();
        lineTableModel.setRows(lines);

        showStep2Card(CARD_EMPTY);
        tabs.setSelectedIndex(0);
    }

    private void wireEvents() {
        rbCash.addActionListener(e -> {
            updateStep1Enable();
            tabs.setSelectedIndex(0);
        });
        rbTransfer.addActionListener(e -> {
            updateStep1Enable();
            // Show QR transfer UI and start polling for external confirmation
            tabs.setSelectedIndex(1);
            showStep2Card(CARD_TRANSFER);
            loadQrAsync();
            long totalForTransfer = parseMoneyToLong(txtPayTotal.getText());
            if (totalForTransfer <= 0) {
                totalForTransfer = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
            }
            lblTransferStatus.setText("Chờ xác nhận chuyển khoản...");
            startTransferPoller(transferRef, Math.max(0, totalForTransfer));
        });

        btnCashFull.addActionListener(e -> {
            rbCash.setSelected(true);
            updateStep1Enable();
            goToReview(Result.CASH_FULL, false);
        });

        btnCashInstallment.addActionListener(e -> {
            rbCash.setSelected(true);
            updateStep1Enable();
            showStep2Card(CARD_INSTALLMENT);
            tabs.setSelectedIndex(1);
            updateInstallmentPreview();
        });

        cbMonths.addActionListener(e -> updateInstallmentPreview());
        txtUpfront.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateInstallmentPreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateInstallmentPreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateInstallmentPreview();
            }
        });

        btnInstallmentConfirm.addActionListener(e -> {
            InstallmentInfo info = buildInstallmentInfoOrWarn();
            if (info == null) return;
            paymentRefToPersist = info.encodeToPaymentRef();
            goToReview(Result.CASH_INSTALLMENT, false);
        });

        btnTransferConfirmed.addActionListener(e -> {
            int c = JOptionPane.showConfirmDialog(this,
                    "X\u00e1c nh\u1eadn kh\u00e1ch h\u00e0ng \u0111\u00e3 chuy\u1ec3n kho\u1ea3n th\u00e0nh c\u00f4ng?",
                    "X\u00e1c nh\u1eadn chuy\u1ec3n kho\u1ea3n",
                    JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;

            long totalForTransfer = parseMoneyToLong(txtPayTotal.getText());
            if (totalForTransfer <= 0) {
                totalForTransfer = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
            }
            // For bank transfer we enforce the transfer amount to be the total (no extra input)
            long transferred = totalForTransfer;
            transferConfirmed = true;
            paymentRefToPersist = "TRANSFER|ref=" + transferRef + "|amount=" + transferred;
            stopTransferPoller();
            goToReview(Result.TRANSFER_CONFIRMED, true);
        });

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedIndex() == 1 && rbTransfer.isSelected()) {
                showStep2Card(CARD_TRANSFER);
                loadQrAsync();
            }
        });

        updateStep1Enable();
    }

    private void updateStep1Enable() {
        boolean cash = rbCash.isSelected();
        boolean transfer = rbTransfer.isSelected();

        btnCashFull.setEnabled(cash);
        btnCashInstallment.setEnabled(cash);

        if (transfer) {
            showStep2Card(CARD_TRANSFER);
        } else {
            showStep2Card(CARD_EMPTY);
            transferConfirmed = false;
            stopTransferPoller();
        }
    }

    private void goToReview(Result r, boolean lockAmount) {
        long total = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
        if (parseMoneyToLong(txtPayTotal.getText()) <= 0 && total > 0) {
            txtPayTotal.setText(MoneyVND.format(total));
        }
        txtPayTotal.setEditable(!lockAmount);
        txtPayTotal.setEnabled(true);
        result = r;
        tabs.setSelectedIndex(2);
    }

    private void onPrintClicked() {
        if (result == Result.CANCELLED) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng x\u00e1c nh\u1eadn kh\u00e1ch h\u00e0ng \u0111\u00e3 chuy\u1ec3n kho\u1ea3n.", "Thi\u1ebfu d\u1eef li\u1ec7u", JOptionPane.WARNING_MESSAGE);
            tabs.setSelectedIndex(0);
            return;
        }

        long total = parseMoneyToLong(txtPayTotal.getText());
        if (total < 0) total = 0;
        if (total == 0) {
            int c = JOptionPane.showConfirmDialog(this,
                    "T\u1ed5ng thanh to\u00e1n \u0111ang l\u00e0 0. B\u1ea1n c\u00f3 ch\u1eafc mu\u1ed1n in bill?",
                    "X\u00e1c nh\u1eadn",
                    JOptionPane.YES_NO_OPTION);
            if (c != JOptionPane.YES_OPTION) return;
        }

        if (result == Result.TRANSFER_CONFIRMED && !transferConfirmed) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng x\u00e1c nh\u1eadn kh\u00e1ch h\u00e0ng \u0111\u00e3 chuy\u1ec3n kho\u1ea3n.", "Thi\u1ebfu d\u1eef li\u1ec7u", JOptionPane.WARNING_MESSAGE);
            tabs.setSelectedIndex(1);
            return;
        }

        if (result == Result.CASH_INSTALLMENT) {
            InstallmentInfo info = buildInstallmentInfoOrWarn();
            if (info == null) return;
            paymentRefToPersist = info.encodeToPaymentRef();
        }

        if (result == Result.CASH_FULL) {
            long totalForCash = parseMoneyToLong(txtPayTotal.getText());
            if (totalForCash <= 0) {
                totalForCash = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
            }

            Long given = askCashGivenAmount(totalForCash);
            if (given == null) return;

            cashGivenAmount = given;
            paymentRefToPersist = "CASH|given=" + given;
        }

        if (result == Result.TRANSFER_CONFIRMED) {
            if (paymentRefToPersist == null || paymentRefToPersist.isBlank()) {
                long totalForTransfer = parseMoneyToLong(txtPayTotal.getText());
                if (totalForTransfer <= 0) {
                    totalForTransfer = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
                }
                paymentRefToPersist = "TRANSFER|ref=" + transferRef + "|amount=" + Math.max(0, totalForTransfer);
            }
        }

        printRequested = true;
        dispose();
    }

    private Long askCashGivenAmount(long totalAmount) {
        long total = Math.max(0, totalAmount);
        String defaultValue = total <= 0 ? "" : MoneyVND.format(total);
        String input = JOptionPane.showInputDialog(this,
                "Ti\u1ec1n m\u1eb7t: S\u1ed1 ti\u1ec1n kh\u00e1ch \u0111\u01b0a",
                defaultValue);
        if (input == null) return null;

        long given = parseMoneyToLong(input);
        if (given <= 0 && total > 0) {
            given = total;
        }
        if (given < total) {
            JOptionPane.showMessageDialog(this,
                    "S\u1ed1 ti\u1ec1n kh\u00e1ch \u0111\u01b0a ph\u1ea3i >= t\u1ed5ng thanh to\u00e1n.",
                    "D\u1eef li\u1ec7u kh\u00f4ng h\u1ee3p l\u1ec7",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        long change = Math.max(0, given - total);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "S\u1ed1 ti\u1ec1n ph\u1ea3i tr\u1ea3: " + MoneyVND.format(total) + "\n"
                        + "Kh\u00e1ch \u0111\u01b0a: " + MoneyVND.format(given) + "\n"
                        + "Ti\u1ec1n tr\u1ea3 l\u1ea1i: " + MoneyVND.format(change) + "\n\n"
                        + "X\u00e1c nh\u1eadn th\u00f4ng tin n\u00e0y?",
                "X\u00e1c nh\u1eadn ti\u1ec1n m\u1eb7t",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return null;
        return given;
    }

    private Long askTransferAmount(long totalAmount) {
        long total = Math.max(0, totalAmount);
        String defaultValue = total <= 0 ? "" : MoneyVND.format(total);
        String input = JOptionPane.showInputDialog(this,
                "Ng\u00e2n h\u00e0ng: S\u1ed1 ti\u1ec1n kh\u00e1ch chuy\u1ec3n kho\u1ea3n",
                defaultValue);
        if (input == null) return null;

        long amount = parseMoneyToLong(input);
        if (amount <= 0 && total > 0) {
            amount = total;
        }
        if (amount < total) {
            JOptionPane.showMessageDialog(this,
                    "S\u1ed1 ti\u1ec1n chuy\u1ec3n kho\u1ea3n ph\u1ea3i >= t\u1ed5ng thanh to\u00e1n.",
                    "D\u1eef li\u1ec7u kh\u00f4ng h\u1ee3p l\u1ec7",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        long change = Math.max(0, amount - total);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "S\u1ed1 ti\u1ec1n ph\u1ea3i tr\u1ea3: " + MoneyVND.format(total) + "\n"
                        + "Kh\u00e1ch chuy\u1ec3n: " + MoneyVND.format(amount) + "\n"
                        + "Ti\u1ec1n d\u01b0: " + MoneyVND.format(change) + "\n\n"
                        + "X\u00e1c nh\u1eadn th\u00f4ng tin n\u00e0y?",
                "X\u00e1c nh\u1eadn chuy\u1ec3n kho\u1ea3n",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) return null;
        return amount;
    }

    private void showStep2Card(String card) {
        CardLayout cl = (CardLayout) step2Cards.getLayout();
        cl.show(step2Cards, card);
    }

    private void updateInstallmentPreview() {
        long total = parseMoneyToLong(txtPayTotal.getText());
        if (total <= 0) {
            total = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
        }
        Integer monthsObj = (Integer) cbMonths.getSelectedItem();
        int months = monthsObj == null ? 0 : monthsObj;
        long upfront = parseMoneyToLong(txtUpfront.getText());
        InstallmentInfo info = InstallmentInfo.of(months, total, upfront);
        lblRemaining.setText(info.remainingAmount() <= 0 ? "0" : MoneyVND.format(info.remainingAmount()));
        lblMonthly.setText(info.monthlyAmount() <= 0 ? "0" : MoneyVND.format(info.monthlyAmount()));
    }

    private InstallmentInfo buildInstallmentInfoOrWarn() {
        long total = parseMoneyToLong(txtPayTotal.getText());
        if (total <= 0) {
            total = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
        }
        if (total <= 0) {
            JOptionPane.showMessageDialog(this, "Kh\u00f4ng c\u00f3 t\u1ed5ng ti\u1ec1n \u0111\u1ec3 t\u00ednh tr\u1ea3 g\u00f3p.", "Thi\u1ebfu d\u1eef li\u1ec7u", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        Integer monthsObj = (Integer) cbMonths.getSelectedItem();
        int months = monthsObj == null ? 0 : monthsObj;
        long upfront = parseMoneyToLong(txtUpfront.getText());
        if (upfront < 0) upfront = 0;
        if (upfront > total) {
            JOptionPane.showMessageDialog(this, "S\u1ed1 ti\u1ec1n tr\u1ea3 tr\u01b0\u1edbc kh\u00f4ng \u0111\u01b0\u1ee3c l\u1edbn h\u01a1n t\u1ed5ng thanh to\u00e1n.", "D\u1eef li\u1ec7u kh\u00f4ng h\u1ee3p l\u1ec7", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (months <= 0) {
            JOptionPane.showMessageDialog(this, "Vui l\u00f2ng ch\u1ecdn g\u00f3i tr\u1ea3 g\u00f3p (12/6/3 th\u00e1ng).", "Thi\u1ebfu d\u1eef li\u1ec7u", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return InstallmentInfo.of(months, total, upfront);
    }

    private long parseMoneyToLong(String text) {
        if (text == null) return 0;
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return 0;
        try {
            return Long.parseLong(digits);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String buildTransferInfoText() {
        PaymentConfig.Bank bank = PaymentConfig.loadBank();

        String custName = customer == null ? "" : safe(customer.getName());
        String custPhone = customer == null ? "" : safe(customer.getPhone());

        String amountText = (draft == null || draft.getTotal() == null) ? "" : MoneyVND.format(draft.getTotal());
        long amountValue = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
        String content = "Thanh to\u00e1n PhoneStore " + transferRef;

        StringBuilder sb = new StringBuilder();
        sb.append("TH\u00d4NG TIN CHUY\u1ec2N KHO\u1ea2N\\n");
        sb.append("===================\n\n");

        sb.append("Ng\u00e2n h\u00e0ng : ").append(safe(bank.bankName())).append("\n");
        sb.append("S\u1ed1 TK    : ").append(safe(bank.accountNo())).append("\n");
        sb.append("Ch\u1ee7 TK   : ").append(safe(bank.accountName())).append("\n\n");

        if (!custName.isBlank() || !custPhone.isBlank()) {
            sb.append("Kh\u00e1ch h\u00e0ng: ").append(custName);
            if (!custPhone.isBlank()) sb.append(" (SDT: ").append(custPhone).append(")");
            sb.append("\n\n");
        }

        if (amountValue > 0) {
            sb.append("S\u1ed1 ti\u1ec1n  : ").append(amountText).append(" (VND: ").append(amountValue).append(")\n");
        } else {
            sb.append("S\u1ed1 ti\u1ec1n  : (ch\u01b0a c\u00f3)\\n");
        }
        sb.append("M\u00e3 \u0111\u01a1n h\u00e0ng: ").append(transferRef).append("\n");
        sb.append("N\u1ed9i dung : ").append(content).append("\n\n");

        boolean missing = safe(bank.bankName()).isBlank() || safe(bank.accountNo()).isBlank() || safe(bank.accountName()).isBlank();
        if (missing) {
            sb.append("L\u01b0u \u00fd: Ch\u01b0a c\u1ea5u h\u00ecnh \u0111\u1ee7 th\u00f4ng tin chuy\u1ec3n kho\u1ea3n (payment.properties), QR ch\u1ec9 mang t\u00ednh tham kh\u1ea3o.\\n");
        }
        return sb.toString();
    }

    private void loadQrAsync() {
        qrLabel.setText("\u0110ang t\u1ea1o QR...");
        qrLabel.setIcon(null);

        SwingWorker<Icon, Void> w = new SwingWorker<>() {
            @Override
            protected Icon doInBackground() {
                try {
                    BufferedImage img = ZxingQr.render(buildQrPayload(), 300);
                    return new ImageIcon(img);
                } catch (Exception ex) {
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Icon icon = get();
                    if (icon == null) {
                        qrLabel.setText("Kh\u00f4ng t\u1ea1o \u0111\u01b0\u1ee3c QR");
                        qrLabel.setIcon(null);
                    } else {
                        qrLabel.setText("");
                        qrLabel.setIcon(icon);
                    }
                } catch (Exception e) {
                    qrLabel.setText("Kh\u00f4ng t\u1ea1o \u0111\u01b0\u1ee3c QR");
                    qrLabel.setIcon(null);
                }
            }
        };
        w.execute();
    }

    private void startTransferPoller(String transferRef, long expectedAmount) {
        stopTransferPoller();
        transferPoller = new ScheduledThreadPoolExecutor(1);
        Runnable task = () -> {
            try {
                long found = checkTransferMarker(transferRef);
                if (found >= expectedAmount && found > 0) {
                    SwingUtilities.invokeLater(() -> {
                        transferConfirmed = true;
                        paymentRefToPersist = "TRANSFER|ref=" + transferRef + "|amount=" + found;
                        lblTransferStatus.setText("Đã nhận chuyển khoản: " + com.phonestore.util.MoneyVND.format(found));
                        goToReview(Result.TRANSFER_CONFIRMED, true);
                    });
                    stopTransferPoller();
                }
            } catch (Throwable ignored) {
            }
        };
        transferPoller.scheduleAtFixedRate(task, 2, 3, TimeUnit.SECONDS);
    }

    private void stopTransferPoller() {
        try {
            if (transferPoller != null) {
                transferPoller.shutdownNow();
                transferPoller = null;
            }
        } catch (Exception ignored) {}
    }

    /**
     * Check for a local transfer confirmation marker.
     * If a file data/payments/transfer_<ref>.done exists, try to parse an amount from it.
     * Return parsed amount or 0 if not found/parse failed.
     */
    private long checkTransferMarker(String transferRef) {
        try {
            Path p = Paths.get("data", "payments", "transfer_" + transferRef + ".done");
            if (!Files.exists(p)) return 0L;
            String txt = Files.readString(p, StandardCharsets.UTF_8).trim();
            if (txt.isBlank()) return 0L;
            // extract first continuous digits
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(txt);
            if (m.find()) {
                String num = m.group(1);
                try { return Long.parseLong(num); } catch (Exception ignored) {}
            }
            return 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public void dispose() {
        stopTransferPoller();
        super.dispose();
    }

    private String buildQrPayload() {
        PaymentConfig.Bank bank = PaymentConfig.loadBank();
        long amountValue = parseMoneyToLong(txtPayTotal.getText());
        if (amountValue <= 0) amountValue = (draft == null || draft.getTotal() == null) ? 0 : Math.round(draft.getTotal());
        String content = "Thanh to\u00e1n PhoneStore " + transferRef;

        String bankBin = VietQrPayloadBuilder.bankBinFromBankName(bank.bankName());
        String accountNo = safe(bank.accountNo());
        String accountName = safe(bank.accountName());

        try {
            if (!bankBin.isBlank() && !accountNo.isBlank()) {
                return VietQrPayloadBuilder.build(bankBin, accountNo, accountName, amountValue, content);
            }
        } catch (Exception ignored) {
        }

        return "PhoneStore - TH\u00d4NG TIN CHUY\u1ec2N KHO\u1ea2N\\n"
                + "Bank: " + safe(bank.bankName()) + "\n"
                + "Account: " + accountNo + "\n"
                + "Name: " + accountName + "\n"
                + "Amount(VND): " + (amountValue <= 0 ? "" : String.valueOf(amountValue)) + "\n"
                + "Content: " + content;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static final class LineTableModel extends AbstractTableModel {

        private final String[] cols = {"STT", "T\u00ean SP/C\u1ea5u h\u00ecnh", "SL", "Th\u00e0nh ti\u1ec1n"};
        private List<ExportReceiptLine> rows = new ArrayList<>();

        public void setRows(List<ExportReceiptLine> rows) {
            this.rows = rows == null ? new ArrayList<>() : new ArrayList<>(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows == null ? 0 : rows.size();
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public String getColumnName(int column) {
            return cols[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ExportReceiptLine r = rows.get(rowIndex);
            if (r == null) return "";
            String name = r.getProductName() == null ? "" : r.getProductName().trim();
            String variant = r.getVariantLabel() == null ? "" : r.getVariantLabel().trim();
            String label = name;
            if (!variant.isBlank()) label = label + " - " + variant;
            int qty = r.getQuantity() == null ? 0 : r.getQuantity();
            long total = r.getLineTotal();
            return switch (columnIndex) {
                case 0 -> rowIndex + 1;
                case 1 -> label;
                case 2 -> qty;
                case 3 -> total <= 0 ? "" : MoneyVND.format(total);
                default -> "";
            };
        }
    }
}
