import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

// ===== 팀원 (팀원 관리 + 주간 일정표) =====
class TeamPanel extends JPanel {
    LocalDate ws = LocalDate.now().with(DayOfWeek.MONDAY);
    JLabel lbl = new JLabel(); JTable table = new JTable();

    TeamPanel() {
        setLayout(new BorderLayout(0, 8)); setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(Store.cell("◀", e -> { ws = ws.minusWeeks(1); refresh(); }));
        top.add(lbl);
        top.add(Store.cell("▶", e -> { ws = ws.plusWeeks(1); refresh(); }));
        top.add(Store.btn("팀원 추가", e -> {
            String n = JOptionPane.showInputDialog(this, "팀원 이름");
            if (n != null && !n.isBlank()) { Store.members.add(n.trim()); Store.changed(); }
        }));
        Store.UI.table(table);
        add(top, BorderLayout.NORTH); add(new JScrollPane(table), BorderLayout.CENTER);
        Store.listeners.add(this::refresh); refresh();
    }

    void refresh() {
        lbl.setText(ws + " ~ " + ws.plusDays(6) + " 주간 일정");
        String[] cols = new String[8]; cols[0] = "팀원";
        for (int i = 0; i < 7; i++) { LocalDate d = ws.plusDays(i); cols[i + 1] = d.getMonthValue() + "/" + d.getDayOfMonth() + " " + "월화수목금토일".charAt(i); }
        DefaultTableModel mo = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        for (String p : Store.members) {
            Object[] row = new Object[8]; row[0] = p;
            for (int i = 0; i < 7; i++) {
                StringBuilder sb = new StringBuilder();
                for (Store.Event e : Store.on(ws.plusDays(i))) if (e.people.contains(p)) sb.append(e.start).append(" ").append(e.title).append("<br>");
                row[i + 1] = sb.length() == 0 ? "" : "<html>" + sb + "</html>";
            }
            mo.addRow(row);
        }
        table.setModel(mo);
    }
}
