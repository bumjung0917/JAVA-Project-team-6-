import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

// ===== 메인 프레임 =====
public class Main extends JFrame {
    CardLayout cl = new CardLayout(); JPanel center = new JPanel(cl);
    String[] names = {"홈", "챗봇", "프로젝트", "팀원", "캘린더"};
    SideButton[] navs = new SideButton[names.length];

    Main() {
        super("팀플 협업");
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1100, 720); setLocationRelativeTo(null);
        ChatbotPanel bot = new ChatbotPanel();
        HomePanel home = new HomePanel(q -> { select(1); bot.ask(q); });
        JPanel[] pages = {home, bot, new ProjectPanel(), new TeamPanel(), new CalendarPanel()};

        JPanel side = new JPanel(); side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Store.UI.DARK); side.setPreferredSize(new Dimension(200, 0));
        JPanel back = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); back.setBackground(Store.UI.DARK);
        back.setBorder(BorderFactory.createEmptyBorder(40, 36, 50, 0));
        back.add(new BackIcon(() -> select(0)));
        back.setAlignmentX(0f); back.setPreferredSize(new Dimension(200, 160)); back.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        side.add(back);
        for (int i = 0; i < names.length; i++) {
            int idx = i;
            center.add(page(names[i], pages[i]), names[i]);
            navs[i] = new SideButton(names[i], e -> select(idx)); side.add(navs[i]);
        }
        side.add(Box.createVerticalGlue());
        add(side, BorderLayout.WEST); add(center, BorderLayout.CENTER);
        select(0);
    }

    // 화면 위쪽 큰 제목 + 아래 구분선
    static JPanel page(String title, JPanel body) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(Store.UI.BG);
        JLabel h = new JLabel(title); h.setFont(h.getFont().deriveFont(Font.BOLD, 22f)); h.setForeground(Store.UI.TEXT);
        h.setOpaque(true); h.setBackground(Color.WHITE);
        h.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Store.UI.LINE), BorderFactory.createEmptyBorder(30, 34, 26, 34)));
        p.add(h, BorderLayout.NORTH); p.add(body, BorderLayout.CENTER);
        return p;
    }

    void select(int i) {
        cl.show(center, names[i]);
        for (int j = 0; j < navs.length; j++) { navs[j].on = j == i; navs[j].repaint(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            for (Object k : Collections.list(UIManager.getDefaults().keys()))
                if (UIManager.get(k) instanceof Font) UIManager.put(k, new Font("맑은 고딕", Font.PLAIN, 14));
            for (String k : new String[]{"Panel", "CheckBox", "OptionPane"}) UIManager.put(k + ".background", Color.WHITE);
            new Main().setVisible(true);
            Store.Notifier.start();
        });
    }

    // ===== 사이드바 컴포넌트 =====
    static class SideButton extends JButton {
        boolean on, over;
        SideButton(String s, ActionListener a) {
            super(s); addActionListener(a);
            setForeground(Color.WHITE); setFont(getFont().deriveFont(15f)); setHorizontalAlignment(LEFT);
            setContentAreaFilled(false); setBorderPainted(false); setFocusPainted(false); setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 26, 0, 0));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 56)); setMaximumSize(new Dimension(Integer.MAX_VALUE, 56)); setAlignmentX(0f);
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { over = true; repaint(); }
                public void mouseExited(MouseEvent e) { over = false; repaint(); }
            });
        }
        protected void paintComponent(Graphics g) {
            if (on || over) { g.setColor(new Color(255, 255, 255, on ? 55 : 25)); g.fillRect(0, 0, getWidth(), getHeight()); }
            super.paintComponent(g);
        }
    }

    static class BackIcon extends JComponent {
        BackIcon(Runnable r) {
            setPreferredSize(new Dimension(70, 70)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { r.run(); } });
        }
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Color.WHITE); g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawOval(2, 2, getWidth() - 5, getHeight() - 5);
            int cx = getWidth() / 2, cy = getHeight() / 2;
            g.drawLine(cx - 16, cy, cx + 16, cy); g.drawLine(cx - 16, cy, cx - 5, cy - 11); g.drawLine(cx - 16, cy, cx - 5, cy + 11);
            g.dispose();
        }
    }

    // ===== 홈 =====
    static class HomePanel extends JPanel {
        JTextArea a = new JTextArea();
        HomePanel(java.util.function.Consumer<String> onAsk) {
            setLayout(new BorderLayout());
            a.setEditable(false); a.setMargin(new Insets(16, 16, 16, 16)); a.setFont(a.getFont().deriveFont(15f));
            add(new JScrollPane(a), BorderLayout.CENTER); add(ChatbotPanel.askBar(onAsk), BorderLayout.SOUTH);
            Store.listeners.add(this::refresh); refresh();
        }
        void refresh() {
            StringBuilder s = new StringBuilder("■ 마감 임박 업무\n");
            Store.tasks.stream().filter(t -> t.status != 2).sorted(Comparator.comparing(t -> t.due)).limit(5)
                    .forEach(t -> s.append("  · ").append(t).append("\n"));
            s.append("\n■ 오늘 일정\n");
            Store.on(LocalDate.now()).forEach(e -> s.append("  · ").append(e).append("\n"));
            s.append("\n■ D-day 일정\n");
            Store.events.stream().filter(e -> !e.alerts.isEmpty() && !e.date.isBefore(LocalDate.now())).sorted(Comparator.comparing(e -> e.date))
                    .forEach(e -> s.append("  · ").append(Store.Event.dday(e.date.toEpochDay() - LocalDate.now().toEpochDay()))
                            .append("  ").append(e.title).append(" (").append(e.date).append(")\n"));
            s.append("\n■ 최근 인수인계\n");
            for (int i = Store.handovers.size() - 1, c = 0; i >= 0 && c < 5; i--, c++) s.append("  · ").append(Store.handovers.get(i)).append("\n");
            a.setText(s.toString());
        }
    }

    // ===== 챗봇 (지금은 임시 답변. 나중에 reply()에 LLM API 연결) =====
    static class ChatbotPanel extends JPanel {
        JPanel list = new JPanel(); JScrollPane sp; JTextField input = new JTextField();

        ChatbotPanel() {
            setLayout(new BorderLayout()); setBackground(Store.UI.BG);
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setOpaque(false);
            JPanel wrap = new JPanel(new BorderLayout()); wrap.setBackground(Store.UI.BG); wrap.add(list, BorderLayout.NORTH);
            sp = new JScrollPane(wrap); sp.setBorder(null); sp.getVerticalScrollBar().setUnitIncrement(16);
            input.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Store.UI.LINE), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            ActionListener send = e -> { String q = input.getText().trim(); if (!q.isEmpty()) { input.setText(""); ask(q); } };
            input.addActionListener(send);
            JPanel bar = new JPanel(new BorderLayout(10, 0)); bar.setBackground(Color.WHITE);
            bar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Store.UI.LINE), BorderFactory.createEmptyBorder(14, 20, 14, 20)));
            bar.add(input, BorderLayout.CENTER); bar.add(Store.btn("전송", send), BorderLayout.EAST);
            add(sp, BorderLayout.CENTER); add(bar, BorderLayout.SOUTH);
            bubble("안녕하세요! 업무나 일정에 대해 궁금한 걸 물어보세요.", false);
        }

        // 질문 처리 (홈의 입력줄에서도 호출됨). 답변은 백그라운드 스레드에서 만들어서 화면이 안 멈춤
        void ask(String q) {
            bubble(q, true);
            new SwingWorker<String, Void>() {
                protected String doInBackground() { return reply(q); }
                protected void done() { try { bubble(get(), false); } catch (Exception ex) { bubble("오류가 발생했어요.", false); } }
            }.execute();
        }

        // TODO: 여기에 LLM API 호출을 넣으면 됨 (Store의 업무/일정 데이터를 프롬프트에 같이 넣기)
        String reply(String q) { return "아직 LLM API가 연결되지 않았어요. 연결되면 여기에 답변이 표시돼요.\n(받은 질문: " + q + ")"; }

        void bubble(String text, boolean mine) {
            String html = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
            JLabel l = new JLabel("<html>" + (text.length() > 28 || text.contains("\n") ? "<div style='width:340px'>" : "<div>") + html + "</div></html>");
            l.setForeground(mine ? Color.WHITE : Store.UI.TEXT);
            JPanel b = new JPanel(new BorderLayout()) {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(mine ? Store.UI.PRIMARY : Color.WHITE); g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                    g.dispose();
                }
            };
            b.setOpaque(false); b.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14)); b.add(l);
            JPanel row = new JPanel(new BorderLayout()) {
                public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
            };
            row.setOpaque(false); row.setBorder(BorderFactory.createEmptyBorder(6, 20, 6, 20));
            row.add(b, mine ? BorderLayout.EAST : BorderLayout.WEST);
            list.add(row); list.revalidate(); list.repaint();
            SwingUtilities.invokeLater(() -> sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMaximum()));
        }

        // 홈 화면 아래 "AI에게 물어보기" 입력줄
        static JPanel askBar(java.util.function.Consumer<String> onAsk) {
            JTextField f = new JTextField();
            f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Store.UI.LINE), BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            ActionListener go = e -> { String q = f.getText().trim(); if (!q.isEmpty()) { f.setText(""); onAsk.accept(q); } };
            f.addActionListener(go);
            JLabel lb = new JLabel("AI에게 물어보기"); lb.setFont(lb.getFont().deriveFont(Font.BOLD)); lb.setForeground(Store.UI.PRIMARY);
            JPanel p = new JPanel(new BorderLayout(10, 0)); p.setBackground(Color.WHITE);
            p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Store.UI.LINE), BorderFactory.createEmptyBorder(14, 20, 14, 20)));
            p.add(lb, BorderLayout.WEST); p.add(f, BorderLayout.CENTER); p.add(Store.btn("질문", go), BorderLayout.EAST);
            return p;
        }
    }
}
