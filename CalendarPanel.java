import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

// ===== 캘린더 (연간 12개월 → 월 클릭 → 월간 달력 → 날짜 클릭 → 일정 관리) =====
class CalendarPanel extends JPanel {
    int year = LocalDate.now().getYear(), month = LocalDate.now().getMonthValue();
    boolean monthMode = false;

    CalendarPanel() { setLayout(new BorderLayout(0, 8)); setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); Store.listeners.add(this::build); build(); }

    void build() {
        removeAll();
        JPanel top = new JPanel(); JLabel title = new JLabel(); JPanel body;
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JButton prev = Store.cell("◀", null), next = Store.cell("▶", null);
        top.add(prev); top.add(title); top.add(next);
        if (!monthMode) {
            title.setText(year + "년");
            prev.addActionListener(e -> { year--; build(); });
            next.addActionListener(e -> { year++; build(); });
            body = new JPanel(new GridLayout(3, 4, 8, 8));
            for (int m = 1; m <= 12; m++) {
                int mm = m;
                long c = Store.events.stream().filter(ev -> ev.date.getYear() == year && ev.date.getMonthValue() == mm).count();
                body.add(Store.cell("<html><center><h2>" + m + "월</h2>일정 " + c + "개</center></html>", e -> { month = mm; monthMode = true; build(); }));
            }
        } else {
            title.setText(year + "년 " + month + "월");
            prev.addActionListener(e -> { if (--month < 1) { month = 12; year--; } build(); });
            next.addActionListener(e -> { if (++month > 12) { month = 1; year++; } build(); });
            top.add(Store.btn("연간 보기", e -> { monthMode = false; build(); }));
            body = new JPanel(new GridLayout(0, 7, 4, 4));
            for (String d : "월 화 수 목 금 토 일".split(" ")) body.add(new JLabel(d, SwingConstants.CENTER));
            LocalDate first = LocalDate.of(year, month, 1);
            for (int i = 1; i < first.getDayOfWeek().getValue(); i++) body.add(new JLabel());
            for (int d = 1; d <= first.lengthOfMonth(); d++) {
                LocalDate date = first.withDayOfMonth(d); int c = Store.on(date).size();
                boolean al = Store.on(date).stream().anyMatch(ev -> !ev.alerts.isEmpty());
                long dd = date.toEpochDay() - LocalDate.now().toEpochDay();
                String h = "<html><b>" + d + "</b>" + (c == 0 ? "" : "<br>● " + c)
                        + (al && dd >= 0 ? "<br><font color='#E64A19'>" + Store.Event.dday(dd) + "</font>" : "") + "</html>";
                body.add(date.equals(LocalDate.now()) ? Store.btn(h, e -> dayDialog(date)) : Store.cell(h, e -> dayDialog(date)));
            }
        }
        add(top, BorderLayout.NORTH); add(body, BorderLayout.CENTER); revalidate(); repaint();
    }

    void dayDialog(LocalDate d) {
        StringBuilder sb = new StringBuilder(d + "\n\n");
        for (Store.Event e : Store.on(d)) sb.append(e).append("\n");
        if (Store.on(d).isEmpty()) sb.append("일정 없음");
        int r = JOptionPane.showOptionDialog(this, sb.toString(), "일정", 0, JOptionPane.PLAIN_MESSAGE, null, new String[]{"일정 추가", "일정 삭제", "닫기"}, "닫기");
        if (r == 0) addEvent(d);
        else if (r == 1) deleteEvent(d);
    }

    // 선택한 날짜의 일정 삭제 (여러 개 선택 가능)
    void deleteEvent(LocalDate d) {
        List<Store.Event> day = Store.on(d);
        if (day.isEmpty()) { Store.msg("삭제할 일정이 없어요"); return; }
        JList<Store.Event> list = new JList<>(day.toArray(new Store.Event[0]));
        Object[] msg = {"삭제할 일정을 선택하세요 (Ctrl 누르고 여러 개 선택 가능)", new JScrollPane(list)};
        if (JOptionPane.showConfirmDialog(this, msg, d + " 일정 삭제", JOptionPane.OK_CANCEL_OPTION) != 0) return;
        List<Store.Event> picked = list.getSelectedValuesList();
        if (picked.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, picked.size() + "개 일정을 삭제할까요?", "삭제 확인", JOptionPane.YES_NO_OPTION) != 0) return;
        Store.events.removeAll(picked);
        Store.changed();
    }

    void addEvent(LocalDate d) {
        JTextField t = new JTextField(), s = new JTextField("10:00"), en = new JTextField("11:00");
        JPanel cb = new JPanel(); List<JCheckBox> boxes = new ArrayList<>();
        for (String m : Store.members) { JCheckBox c = new JCheckBox(m, true); boxes.add(c); cb.add(c); }
        int[] days = {0, 1, 3, 7}; String[] labels = {"당일", "1일 전", "3일 전", "7일 전"};
        JPanel al = new JPanel(); List<JCheckBox> ab = new ArrayList<>();
        for (String lb : labels) { JCheckBox x = new JCheckBox(lb); ab.add(x); al.add(x); }
        Object[] msg = {"제목", t, "시작 (HH:mm)", s, "종료 (HH:mm)", en, "참여자", cb, "D-day 알림", al};
        if (JOptionPane.showConfirmDialog(this, msg, d + " 일정 추가", JOptionPane.OK_CANCEL_OPTION) != 0 || t.getText().isBlank()) return;
        try {
            List<String> ppl = new ArrayList<>();
            for (JCheckBox c : boxes) if (c.isSelected()) ppl.add(c.getText());
            Store.Event ev = new Store.Event(t.getText().trim(), d, LocalTime.parse(s.getText().trim()), LocalTime.parse(en.getText().trim()), ppl);
            for (int i = 0; i < days.length; i++) if (ab.get(i).isSelected()) ev.alerts.add(days[i]);
            Store.events.add(ev);
            Store.changed();
        } catch (Exception ex) { Store.msg("시간 형식이 올바르지 않아요 (예: 09:30)"); }
    }
}
