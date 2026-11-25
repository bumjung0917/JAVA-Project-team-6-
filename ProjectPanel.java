import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

// ===== 프로젝트 (칸반 + 업무 인수인계) =====
class ProjectPanel extends JPanel {
    @SuppressWarnings("unchecked") DefaultListModel<Store.Task>[] m = new DefaultListModel[3];
    @SuppressWarnings("unchecked") JList<Store.Task>[] l = new JList[3];

    ProjectPanel() {
        setLayout(new BorderLayout(0, 8)); setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel board = new JPanel(new GridLayout(1, 3, 10, 0));
        String[] cols = {"할 일", "진행 중", "완료"};
        for (int i = 0; i < 3; i++) {
            int n = i;
            m[i] = new DefaultListModel<>(); l[i] = new JList<>(m[i]);
            l[i].addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && l[n].getSelectedValue() != null)
                    for (int j = 0; j < 3; j++) if (j != n) l[j].clearSelection();
            });
            JScrollPane sp = new JScrollPane(l[i]); sp.setBorder(BorderFactory.createTitledBorder(cols[i])); board.add(sp);
        }
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(Store.btn("카드 추가", e -> addTask()));
        bar.add(Store.cell("◀ 이전 단계", e -> move(-1)));
        bar.add(Store.cell("다음 단계 ▶", e -> move(1)));
        bar.add(Store.btn("인수인계", e -> handover()));
        bar.add(Store.danger("삭제", e -> { Store.Task t = sel(); if (t != null) { Store.tasks.remove(t); Store.changed(); } }));
        add(bar, BorderLayout.NORTH); add(board, BorderLayout.CENTER);
        Store.listeners.add(this::refresh); refresh();
    }

    Store.Task sel() {
        for (JList<Store.Task> x : l) if (x.getSelectedValue() != null) return x.getSelectedValue();
        Store.msg("카드를 먼저 선택하세요"); return null;
    }

    void refresh() {
        for (DefaultListModel<Store.Task> x : m) x.clear();
        for (Store.Task t : Store.tasks) m[t.status].addElement(t);
    }

    void move(int d) {
        Store.Task t = sel(); if (t == null) return;
        int s = t.status + d; if (s < 0 || s > 2) return;
        t.status = s; Store.changed();
    }

    void addTask() {
        JTextField t = new JTextField(), due = new JTextField(LocalDate.now().plusDays(7).toString());
        JComboBox<String> who = new JComboBox<>(Store.members.toArray(new String[0]));
        Object[] msg = {"업무 제목", t, "담당자", who, "마감일 (yyyy-MM-dd)", due};
        if (JOptionPane.showConfirmDialog(this, msg, "카드 추가", JOptionPane.OK_CANCEL_OPTION) != 0 || t.getText().isBlank()) return;
        try { Store.tasks.add(new Store.Task(t.getText().trim(), (String) who.getSelectedItem(), LocalDate.parse(due.getText().trim()))); Store.changed(); }
        catch (Exception ex) { Store.msg("날짜 형식이 올바르지 않아요 (예: 2026-12-01)"); }
    }

    void handover() {
        Store.Task t = sel(); if (t == null) return;
        JComboBox<String> to = new JComboBox<>(Store.members.toArray(new String[0]));
        JTextArea note = new JTextArea(4, 25);
        Object[] msg = {"업무: " + t.title + " (현재 담당: " + t.assignee + ")", "인수받을 사람", to, "인수인계 메모 (진행 상황, 주의할 점)", new JScrollPane(note)};
        if (JOptionPane.showConfirmDialog(this, msg, "업무 인수인계", JOptionPane.OK_CANCEL_OPTION) != 0) return;
        String nt = (String) to.getSelectedItem();
        Store.handovers.add(LocalDate.now() + " [" + t.title + "] " + t.assignee + " → " + nt
                + (note.getText().isBlank() ? "" : " | " + note.getText().trim().replace("\n", " ")));
        t.assignee = nt; Store.changed();
    }
}
