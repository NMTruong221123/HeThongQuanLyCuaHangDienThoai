package com.phonestore.ui.common;

import com.github.lgooddatepicker.components.DatePicker;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DatePickerYearScroller {

    public static void enable(DatePicker dp) {
        if (dp == null) return;
        try {
            // Try to attach after calendar toggle opens
            JButton toggle = dp.getComponentToggleCalendarButton();
            if (toggle != null) {
                toggle.addActionListener(ae -> {
                    // when toggle clicked, repeatedly scan for popup list for a short period
                    startRepeatedScan();
                });
            } else {
                // fallback: try repeated scans
                startRepeatedScan();
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean scanAndAttach() {
        Window[] wins = Window.getWindows();
        for (Window w : wins) {
            if (attachRecursively(w)) return true;
        }
        return false;
    }

    private static void startRepeatedScan() {
        final int[] attempts = {0};
        final int maxAttempts = 30; // ~6 seconds at 200ms interval
        Timer t = new Timer(200, ae -> {
            attempts[0]++;
            try {
                boolean ok = scanAndAttach();
                if (ok || attempts[0] >= maxAttempts) {
                    ((Timer) ae.getSource()).stop();
                }
            } catch (Throwable ignored) {
                if (attempts[0] >= maxAttempts) ((Timer) ae.getSource()).stop();
            }
        });
        t.setRepeats(true);
        t.start();
    }

    private static boolean attachRecursively(Component c) {
        if (c == null) return false;
        if (c instanceof JList<?> jl) {
            if (tryAttachToYearList(jl)) return true;
        }
        if (c instanceof Container cont) {
            for (Component child : cont.getComponents()) {
                if (attachRecursively(child)) return true;
            }
        }
        return false;
    }

    private static boolean tryAttachToYearList(JList<?> jl) {
        ListModel<?> model = jl.getModel();
        if (model == null) return false;
        int size = model.getSize();
        if (size < 6) return false; // likely not the years list
        // check if items look like years (numbers)
        int numericCount = 0;
        for (int i = 0; i < Math.min(size, 10); i++) {
            Object v = model.getElementAt(i);
            if (v == null) continue;
            String s = String.valueOf(v).trim();
            if (s.matches("\\d{4}")) numericCount++;
        }
        if (numericCount < 3) return false;

        // Replace model with mutable DefaultListModel so we can expand
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (int i = 0; i < model.getSize(); i++) listModel.addElement(String.valueOf(model.getElementAt(i)));
        jl.setModel((ListModel) listModel);

        // center current year if present
        String current = String.valueOf(LocalDate.now().getYear());
        int idx = -1;
        for (int i = 0; i < listModel.size(); i++) if (current.equals(listModel.get(i))) { idx = i; break; }
        if (idx >= 0) {
            jl.setSelectedIndex(idx);
            jl.ensureIndexIsVisible(Math.max(0, idx - 4));
        }

        // attach wheel listener to expand when near edges
        jl.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) return;
                int first = jl.getFirstVisibleIndex();
                int last = jl.getLastVisibleIndex();
                if (first < 3 && e.getWheelRotation() < 0) {
                    // prepend earlier years
                    int added = prependYears(listModel);
                    // keep selection roughly at same year
                    jl.setSelectedIndex(jl.getSelectedIndex() + added);
                } else if (last > listModel.getSize() - 4 && e.getWheelRotation() > 0) {
                    appendYears(listModel);
                }
            }
        });
        return true;
    }

    private static int prependYears(DefaultListModel<String> model) {
        try {
            int firstYear = Integer.parseInt(model.get(0).replaceAll("[^0-9]", ""));
            List<String> toAdd = new ArrayList<>();
            for (int i = 1; i <= 20; i++) toAdd.add(String.valueOf(firstYear - i));
            for (int i = toAdd.size() - 1; i >= 0; i--) model.add(0, toAdd.get(i));
            return toAdd.size();
        } catch (Exception ignored) { return 0; }
    }

    private static void appendYears(DefaultListModel<String> model) {
        try {
            int lastYear = Integer.parseInt(model.get(model.getSize() - 1).replaceAll("[^0-9]", ""));
            for (int i = 1; i <= 20; i++) model.addElement(String.valueOf(lastYear + i));
        } catch (Exception ignored) {}
    }
}
