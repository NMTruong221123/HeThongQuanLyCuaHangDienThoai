package com.phonestore.ui.employee;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DatePickerDialog extends JDialog {
    private LocalDate selected;
    private YearMonth month;

    public DatePickerDialog(Window owner, LocalDate initial) {
        super(owner, "Chọn ngày", ModalityType.APPLICATION_MODAL);
        this.month = initial == null ? YearMonth.now() : YearMonth.from(initial);
        this.selected = initial;
        initUi();
    }

    private void initUi() {
        setUndecorated(true);
        setLayout(new BorderLayout());
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton bPrev2 = new JButton("<<");
        JButton bPrev = new JButton("<");
        JButton bNext = new JButton(">");
        JButton bNext2 = new JButton(">>");

        // clickable month and year labels
        JPanel centerHdr = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        JLabel lblMonth = new JLabel();
        lblMonth.setFont(lblMonth.getFont().deriveFont(Font.PLAIN, 14f));
        lblMonth.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel lblYear = new JLabel();
        lblYear.setFont(lblYear.getFont().deriveFont(Font.PLAIN, 14f));
        lblYear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        centerHdr.add(lblMonth);
        centerHdr.add(lblYear);

        nav.add(bPrev2);
        nav.add(bPrev);
        header.add(nav, BorderLayout.WEST);
        header.add(centerHdr, BorderLayout.CENTER);
        JPanel navR = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        navR.add(bNext);
        navR.add(bNext2);
        header.add(navR, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        JPanel dowPanel = new JPanel(new GridLayout(0, 7));
        dowPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // day of week headers (Vietnamese short)
        String[] dow = new String[] {"Th 2","Th 3","Th 4","Th 5","Th 6","Th 7","CN"};
        for (String s : dow) {
            JLabel l = new JLabel(s, SwingConstants.CENTER);
            l.setOpaque(true);
            l.setBackground(new Color(240,240,240));
            dowPanel.add(l);
        }

        JPanel gridHolder = new JPanel(new BorderLayout());
        JPanel grid = new JPanel(new GridLayout(6,7,4,4));
        gridHolder.add(grid, BorderLayout.CENTER);

        // months panel and years panel for quick selection
        JPanel monthsPanel = new JPanel(new GridLayout(3,4,6,6));
        JPanel yearsPanel = new JPanel(new GridLayout(5,4,6,6));

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(dowPanel, BorderLayout.NORTH);
        centerContainer.add(gridHolder, BorderLayout.CENTER);

        add(centerContainer, BorderLayout.CENTER);

        // footer
        JPanel footer = new JPanel(new BorderLayout());
        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLbl = new JLabel();
        info.add(infoLbl);
        footer.add(info, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton bToday = new JButton("Hôm nay");
        JButton bClear = new JButton("Xóa");
        actions.add(bClear);
        actions.add(bToday);
        footer.add(actions, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        // helpers
        final int minYearsBack = 100; // earliest year = now - 100
        final int minAge = 13; // users must be at least 13 years old
        int currentYear = LocalDate.now().getYear();
        int minYear = currentYear - minYearsBack;
        int maxYear = currentYear - minAge; // inclusive

        final int windowSize = 44; // show a sliding window of years (e.g., 1970-2013)
        final int[] yearGridStart = new int[1];
        int defaultStart = Math.max(minYear, maxYear - (windowSize - 1));
        yearGridStart[0] = defaultStart;

        final boolean[] monthMode = new boolean[]{false};
        final boolean[] yearMode = new boolean[]{false};

        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            // header labels
            // lblMonth and lblYear are in scope via closure from earlier
            try {
                lblMonth.setText("Tháng " + month.getMonthValue());
                lblYear.setText(String.valueOf(month.getYear()));
            } catch (Exception ignored) {}

            centerContainer.removeAll();
            if (monthMode[0]) {
                monthsPanel.removeAll();
                for (int m = 1; m <= 12; m++) {
                    final int mm = m;
                    JButton mb = new JButton("Th " + m);
                    mb.addActionListener(ae -> {
                        month = YearMonth.of(month.getYear(), mm);
                        monthMode[0] = false;
                        rebuild[0].run();
                    });
                    monthsPanel.add(mb);
                }
                centerContainer.add(monthsPanel, BorderLayout.CENTER);
            } else if (yearMode[0]) {
                yearsPanel.removeAll();
                int start = yearGridStart[0];
                int count = Math.min(windowSize, maxYear - minYear + 1);
                for (int i = 0; i < count; i++) {
                    final int y = start + i;
                    JButton yb = new JButton(String.valueOf(y));
                    if (y > maxYear || y < minYear) {
                        yb.setEnabled(false);
                    }
                    yb.addActionListener(ae -> {
                        if (y <= maxYear && y >= minYear) {
                            month = YearMonth.of(y, month.getMonthValue());
                            yearMode[0] = false;
                            rebuild[0].run();
                        }
                    });
                    yearsPanel.add(yb);
                }
                centerContainer.add(yearsPanel, BorderLayout.CENTER);
            } else {
                centerContainer.add(dowPanel, BorderLayout.NORTH);
                grid.removeAll();
                LocalDate first = month.atDay(1);
                int shift = first.getDayOfWeek() == DayOfWeek.SUNDAY ? 6 : first.getDayOfWeek().getValue() - 1; // Monday=1
                int days = month.lengthOfMonth();
                int total = 42; // 6*7
                int day = 1;
                LocalDate cutoff = LocalDate.now().minusYears(minAge);
                for (int i = 0; i < total; i++) {
                    if (i < shift || day > days) {
                        grid.add(new JLabel());
                    } else {
                        final int d = day;
                        JButton bd = new JButton(String.valueOf(d));
                        LocalDate thisDate = month.atDay(d);
                        if (thisDate.equals(LocalDate.now())) bd.setBackground(new Color(220,235,255));
                        if (thisDate.isAfter(cutoff)) {
                            bd.setEnabled(false);
                        } else {
                            bd.addActionListener((ActionEvent ae) -> {
                                selected = thisDate;
                                dispose();
                            });
                        }
                        grid.add(bd);
                        day++;
                    }
                }
                infoLbl.setText("Hôm nay: " + LocalDate.now().getDayOfMonth() + " thg " + LocalDate.now().getMonthValue() + ", " + LocalDate.now().getYear());
                centerContainer.add(gridHolder, BorderLayout.CENTER);
            }
            grid.revalidate();
            grid.repaint();
            monthsPanel.revalidate();
            monthsPanel.repaint();
            yearsPanel.revalidate();
            yearsPanel.repaint();
            centerContainer.revalidate();
            centerContainer.repaint();
            pack();
        };

        bPrev.addActionListener(ae -> {
            if (yearMode[0]) {
                yearGridStart[0] = Math.max(minYear, yearGridStart[0] - windowSize);
            } else {
                month = month.minusMonths(1);
                monthMode[0] = false;
                yearMode[0] = false;
            }
            rebuild[0].run();
        });
        bNext.addActionListener(ae -> {
            if (yearMode[0]) {
                yearGridStart[0] = Math.min(maxYear - (windowSize - 1), yearGridStart[0] + windowSize);
            } else {
                month = month.plusMonths(1);
                monthMode[0] = false;
                yearMode[0] = false;
            }
            rebuild[0].run();
        });
        bPrev2.addActionListener(ae -> { month = month.minusYears(1); monthMode[0]=false; yearMode[0]=false; rebuild[0].run(); });
        bNext2.addActionListener(ae -> { month = month.plusYears(1); monthMode[0]=false; yearMode[0]=false; rebuild[0].run(); });
        bToday.addActionListener(ae -> { selected = LocalDate.now(); dispose(); });
        bClear.addActionListener(ae -> { selected = null; dispose(); });

        // header month/year click handlers and year paging
        JLabel lblMonthRef = (JLabel) ((JPanel) header.getComponent(1)).getComponent(0);
        JLabel lblYearRef = (JLabel) ((JPanel) header.getComponent(1)).getComponent(1);
        lblMonthRef.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                monthMode[0] = !monthMode[0];
                yearMode[0] = false;
                rebuild[0].run();
            }
        });
        lblYearRef.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                yearMode[0] = !yearMode[0];
                monthMode[0] = false;
                rebuild[0].run();
            }
        });

        // year paging handled in the same prev/next handlers above when in year mode

        rebuild[0].run();
    }

    public static LocalDate showDialog(Component parent, LocalDate initial) {
        Window win = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        DatePickerDialog d = new DatePickerDialog(win, initial);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return d.selected;
    }
}
