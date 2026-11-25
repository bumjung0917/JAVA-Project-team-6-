import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

// 메모리 저장소. DB 붙일 때 여기 메서드들만 SQL로 교체하면 됨
class Store {
    static List<String> members = new CopyOnWriteArrayList<>(List.of("김민수", "이서연", "박지훈"));
    static List<Task> tasks = new CopyOnWriteArrayList<>();
    static List<Event> events = new CopyOnWriteArrayList<>();
    static List<String> handovers = new CopyOnWriteArrayList<>();
    static List<Runnable> listeners = new ArrayList<>();

    static {
        LocalDate t = LocalDate.now();
        tasks.add(new Task("기획서 작성", "김민수", t.plusDays(1)));
        tasks.add(new Task("DB 설계", "이서연", t.plusDays(5)));
        Task done = new Task("주제 선정", "박지훈", t.minusDays(2)); done.status = 2; tasks.add(done);
        LocalDateTime n = LocalDateTime.now().plusMinutes(10).withSecond(0).withNano(0);
        events.add(new Event("팀 회의", n.toLocalDate(), n.toLocalTime(), n.toLocalTime().plusHours(1), List.of("김민수", "이서연", "박지훈")));
        Event pres = new Event("중간 발표", t.plusDays(3), LocalTime.of(14, 0), LocalTime.of(15, 0), List.of("김민수", "이서연", "박지훈"));
        pres.alerts.addAll(List.of(3, 0)); events.add(pres);
    }

    static void changed() { listeners.forEach(Runnable::run); }
    static List<Event> on(LocalDate d) {
        List<Event> r = new ArrayList<>();
        for (Event e : events) if (e.date.equals(d)) r.add(e);
        r.sort(Comparator.comparing(e -> e.start));
        return r;
    }
    static JButton btn(String s, ActionListener a) { return new Store.UI.RoundButton(s, a, Store.UI.PRIMARY, Store.UI.PRIMARY_H, Color.WHITE, null); }
    static JButton danger(String s, ActionListener a) { return new Store.UI.RoundButton(s, a, Store.UI.DANGER, Store.UI.DANGER_H, Color.WHITE, null); }
    static JButton cell(String s, ActionListener a) { return new Store.UI.RoundButton(s, a, Color.WHITE, Store.UI.TODAY, Store.UI.TEXT, Store.UI.LINE); }
    static void msg(String s) { JOptionPane.showMessageDialog(null, s); }

    // ===== 데이터 모델 (나중에 DB 테이블로 바뀔 부분) =====
    static class Task {
        String title, assignee; LocalDate due; int status; // 0 할 일, 1 진행 중, 2 완료
        Task(String t, String a, LocalDate d) { title = t; assignee = a; due = d; }
        public String toString() { return title + " (" + assignee + ", ~" + due.getMonthValue() + "/" + due.getDayOfMonth() + ")"; }
    }

    static class Event {
        String title; LocalDate date; LocalTime start, end; List<String> people;
        List<Integer> alerts = new ArrayList<>(); // D-day 알림: 며칠 전에 울릴지 (0 = 당일)
        static String dday(long n) { return n == 0 ? "D-DAY" : "D-" + n; }
        Event(String t, LocalDate d, LocalTime s, LocalTime e, List<String> p) { title = t; date = d; start = s; end = e; people = p; }
        public String toString() {
            StringBuilder a = new StringBuilder();
            List<Integer> sorted = new ArrayList<>(alerts); sorted.sort(Comparator.reverseOrder());
            for (int n : sorted) a.append(a.length() == 0 ? " [알림 " : ", ").append(dday(n));
            if (a.length() > 0) a.append("]");
            return start + "-" + end + " " + title + " (" + String.join(", ", people) + ")" + a;
        }
    }

    // ===== 디자인 (색상, 둥근 버튼, 테이블 스타일) =====
    static class UI {
        static final Color PRIMARY = new Color(63, 81, 181), PRIMARY_H = new Color(48, 63, 159);
        static final Color DARK = new Color(51, 54, 138), DARK_H = new Color(70, 74, 160);
        static final Color BG = new Color(238, 238, 238);
        static final Color DANGER = new Color(239, 68, 68), DANGER_H = new Color(220, 38, 38);
        static final Color TEXT = new Color(35, 35, 45), LINE = new Color(228, 228, 232);
        static final Color ZEBRA = new Color(248, 250, 252), TODAY = new Color(236, 238, 252);

        static void table(JTable t) {
            t.setRowHeight(70); t.setGridColor(LINE); t.setShowGrid(true); t.setFillsViewportHeight(true);
            t.setSelectionBackground(new Color(226, 229, 250)); t.setSelectionForeground(TEXT);
            JTableHeader h = t.getTableHeader(); h.setReorderingAllowed(false); h.setPreferredSize(new Dimension(0, 38));
            h.setDefaultRenderer(new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(tb, v, sel, foc, r, c);
                    setBackground(DARK); setForeground(Color.WHITE); setFont(getFont().deriveFont(Font.BOLD));
                    setHorizontalAlignment(CENTER); setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, DARK_H));
                    return this;
                }
            });
            t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel, boolean foc, int r, int c) {
                    super.getTableCellRendererComponent(tb, v, sel, false, r, c);
                    LocalDate n = LocalDate.now();
                    boolean today = tb.getColumnName(c).startsWith(n.getMonthValue() + "/" + n.getDayOfMonth() + " ");
                    setVerticalAlignment(TOP); setHorizontalAlignment(c == 0 ? CENTER : LEFT);
                    setBorder(BorderFactory.createEmptyBorder(8, 8, 6, 8));
                    setFont(getFont().deriveFont(c == 0 ? Font.BOLD : Font.PLAIN));
                    if (!sel) { setBackground(today ? TODAY : r % 2 == 0 ? Color.WHITE : ZEBRA); setForeground(TEXT); }
                    return this;
                }
            });
        }

        static class RoundButton extends JButton {
            Color bg, hover, line; boolean on, over;
            RoundButton(String s, ActionListener a, Color bg, Color hover, Color fg, Color line) {
                super(s); this.bg = bg; this.hover = hover; this.line = line;
                if (a != null) addActionListener(a);
                setForeground(fg); setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                    public void mouseExited(MouseEvent e) { over = false; repaint(); }
                });
            }
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(on ? Store.UI.PRIMARY : over ? hover : bg);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                if (line != null) { g.setColor(line); g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12); }
                g.dispose();
                super.paintComponent(g0);
            }
        }
    }

    // ===== 알림 push (마감/일정 임박 시 시스템 트레이 알림) =====
    static class Notifier {
        static Set<String> sent = ConcurrentHashMap.newKeySet();
        static TrayIcon tray;

        static void start() {
            if (SystemTray.isSupported()) {
                try {
                    BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = img.createGraphics(); g.setColor(Store.UI.PRIMARY); g.fillRoundRect(0, 0, 16, 16, 6, 6); g.dispose();
                    tray = new TrayIcon(img, "팀플 협업 툴");
                    SystemTray.getSystemTray().add(tray);
                } catch (Exception ignored) { }
            }
            Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r); t.setDaemon(true); return t; })
                    .scheduleAtFixedRate(Notifier::check, 0, 30, TimeUnit.SECONDS);
        }

        static void check() {
            LocalDateTime now = LocalDateTime.now();
            for (Store.Task t : Store.tasks)
                if (t.status != 2 && !t.due.isAfter(LocalDate.now().plusDays(1)))
                    push("마감 임박", t.assignee + " - " + t.title + " (마감 " + t.due + ")", "t" + System.identityHashCode(t) + LocalDate.now());
            for (Store.Event e : Store.events) {
                LocalDateTime s = LocalDateTime.of(e.date, e.start);
                if (s.isAfter(now) && s.isBefore(now.plusMinutes(30)))
                    push("일정 곧 시작", e.title + " (" + e.start + ")", "e" + System.identityHashCode(e));
            }
            for (Store.Event e : Store.events)
                for (int n : e.alerts) {
                    LocalDateTime at = e.date.minusDays(n).atTime(9, 0); // 알림은 해당 날짜 오전 9시부터
                    if (n == 0 && at.isAfter(LocalDateTime.of(e.date, e.start))) at = e.date.atStartOfDay();
                    if (!now.isBefore(at) && now.isBefore(LocalDateTime.of(e.date, e.end)))
                        alarm(Store.Event.dday(n), e.title + "\n" + e.date + " " + e.start + " ~ " + e.end, "d" + System.identityHashCode(e) + "_" + n);
                }
        }

        // D-day 알람: 비프음 + 항상 위에 뜨는 알림 창 (+ 트레이 알림)
        static void alarm(String dday, String msg, String key) {
            if (!sent.add(key)) return;
            if (tray != null) tray.displayMessage(dday + " 알림", msg.replace("\n", " "), TrayIcon.MessageType.WARNING);
            Thread ring = new Thread(() -> {
                for (int i = 0; i < 3; i++) { Toolkit.getDefaultToolkit().beep(); try { Thread.sleep(500); } catch (InterruptedException ignored) { } }
            });
            ring.setDaemon(true); ring.start();
            SwingUtilities.invokeLater(() -> {
                JDialog d = new JDialog((Frame) null, "D-day 알림", false);
                d.setAlwaysOnTop(true);
                JLabel big = new JLabel(dday, SwingConstants.CENTER); big.setFont(big.getFont().deriveFont(Font.BOLD, 34f)); big.setForeground(Store.UI.PRIMARY);
                JLabel text = new JLabel("<html><center>" + msg.replace("\n", "<br>") + "</center></html>", SwingConstants.CENTER);
                JPanel p = new JPanel(new BorderLayout(0, 12)); p.setBorder(BorderFactory.createEmptyBorder(24, 36, 20, 36));
                JPanel bt = new JPanel(); bt.add(Store.btn("확인", e -> d.dispose()));
                p.add(big, BorderLayout.NORTH); p.add(text, BorderLayout.CENTER); p.add(bt, BorderLayout.SOUTH);
                d.setContentPane(p); d.pack(); d.setMinimumSize(new Dimension(340, 0));
                d.setLocationRelativeTo(null); d.setVisible(true); d.toFront();
            });
        }

        static void push(String title, String msg, String key) {
            if (!sent.add(key)) return;
            if (tray != null) tray.displayMessage(title, msg, TrayIcon.MessageType.INFO);
            else SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, msg, title, JOptionPane.INFORMATION_MESSAGE));
        }
    }
}
