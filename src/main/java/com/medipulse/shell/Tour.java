package com.medipulse.shell;

import com.medipulse.ui.Card;
import com.medipulse.ui.GlassPanel;
import com.medipulse.ui.Icons;
import com.medipulse.ui.Overlay;
import com.medipulse.ui.PillButton;
import com.medipulse.ui.TablerIcon;
import com.medipulse.ui.Theme;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Supplier;

/**
 * Guided tour: dims the window, cuts a highlighted hole around one part of the interface
 * at a time and explains it in a card beside it. Keyboard: Right / Enter next, Left back,
 * Esc to skip. Steps may run an action first (for example opening a screen).
 */
final class Tour extends Overlay {

    /** One stop on the tour. {@code target} may return null for a centred step with no highlight. */
    record Step(Supplier<Component> target, TablerIcon icon, String title, String body, Runnable before) {
        static Step centred(TablerIcon icon, String title, String body) {
            return new Step(() -> null, icon, title, body, null);
        }
    }

    private static final int PAD = 6;
    private static final int RADIUS = 18;
    private static final int CARD_WIDTH = 380;
    private static final int GAP = 18;

    private final List<Step> steps;
    private final Runnable onFinish;
    private final Card card = new Card(new BorderLayout(0, 12), 22);
    private final JLabel counter = new JLabel();
    private final JLabel title = new JLabel();
    private final JLabel body = new JLabel();
    private final Dots dots;
    private final PillButton back = new PillButton("Back", null, PillButton.Style.SECONDARY);
    private final PillButton next = new PillButton("Next", null, PillButton.Style.PRIMARY);
    private int index;
    private Rectangle hole;
    private boolean finished;

    private Tour(Component anchor, List<Step> steps, Runnable onFinish) {
        super(anchor, null, false);
        this.steps = List.copyOf(steps);
        this.onFinish = onFinish;
        this.dots = new Dots(steps.size());

        card.fill(Theme.RAISED_BG);
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
        card.getAccessibleContext().setAccessibleName("Guided tour");

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        counter.setFont(Theme.caption());
        counter.setForeground(Theme.BLUE);
        head.add(counter, BorderLayout.WEST);
        head.add(dots, BorderLayout.EAST);

        JPanel text = new JPanel(new BorderLayout(0, 6));
        text.setOpaque(false);
        title.setFont(Theme.headline());
        title.setForeground(Theme.LABEL);
        title.setIconTextGap(10);
        body.setFont(Theme.body());
        body.setForeground(Theme.SECONDARY_LABEL);
        text.add(title, BorderLayout.NORTH);
        text.add(body, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.setOpaque(false);
        PillButton skip = new PillButton("Skip tour", null, PillButton.Style.PLAIN);
        skip.setFont(Theme.font(java.awt.Font.PLAIN, 13));
        skip.setForeground(Theme.SECONDARY_LABEL);
        skip.addActionListener(e -> close());
        buttons.add(skip, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        back.addActionListener(e -> go(index - 1));
        next.addActionListener(e -> {
            if (index == this.steps.size() - 1) {
                finish();
            } else {
                go(index + 1);
            }
        });
        right.add(back);
        right.add(next);
        buttons.add(right, BorderLayout.EAST);

        card.add(head, BorderLayout.NORTH);
        card.add(text, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        add(card);

        bind(KeyEvent.VK_RIGHT, () -> next.doClick());
        bind(KeyEvent.VK_ENTER, () -> next.doClick());
        bind(KeyEvent.VK_LEFT, () -> {
            if (index > 0) {
                go(index - 1);
            }
        });

        // Keep the highlight on target when the window is resized.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                SwingUtilities.invokeLater(Tour.this::place);
            }
        });
        onClose(() -> {
            if (!finished) {
                onFinish.run();
            }
        });
    }

    /** Starts the tour over {@code window}. {@code onFinish} runs once, whether finished or skipped. */
    static void start(Component window, List<Step> steps, Runnable onFinish) {
        if (steps.isEmpty()) {
            return;
        }
        Tour t = new Tour(window, steps, onFinish);
        t.present();
        t.go(0);
    }

    private void bind(int key, Runnable r) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), "tour-" + key);
        getActionMap().put("tour-" + key, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                r.run();
            }
        });
    }

    private void go(int i) {
        index = Math.max(0, Math.min(steps.size() - 1, i));
        Step s = steps.get(index);
        if (s.before() != null) {
            s.before().run();
        }
        counter.setText("STEP " + (index + 1) + " OF " + steps.size());
        dots.current = index;
        title.setText(s.title());
        title.setIcon(Icons.of(s.icon(), 22, Theme.BLUE));
        body.setText("<html><div style='width:" + (CARD_WIDTH - 100) + "px'>" + s.body() + "</div></html>");
        back.setVisible(index > 0);
        next.setText(index == steps.size() - 1 ? "Done" : "Next");
        card.getAccessibleContext().setAccessibleDescription(s.title() + ". " + s.body().replaceAll("<[^>]+>", ""));
        // Let the screen we may have just opened lay itself out before we measure it.
        SwingUtilities.invokeLater(this::place);
    }

    private void place() {
        Component target = steps.get(index).target().get();
        hole = null;
        if (target != null && target.isShowing()) {
            Rectangle r = SwingUtilities.convertRectangle(target.getParent(), target.getBounds(), this);
            if (target instanceof GlassPanel) {
                int s = GlassPanel.shadowInset();
                r.grow(-s, -s); // highlight the glass shape, not its shadow margin
            }
            r.grow(PAD, PAD);
            hole = r.intersection(new Rectangle(0, 0, getWidth(), getHeight()));
        }

        // Size the card from its content: never narrower than CARD_WIDTH, never clipping the text.
        Dimension pref = card.getPreferredSize();
        Dimension size = new Dimension(Math.max(CARD_WIDTH, pref.width), pref.height);
        int x;
        int y;
        boolean large = hole != null && hole.width * hole.height > getWidth() * getHeight() * 0.45;
        if (hole == null) {
            x = (getWidth() - size.width) / 2;
            y = (getHeight() - size.height) / 2;
        } else if (large) {
            x = hole.x + hole.width - size.width - 24;           // a whole screen: sit in its bottom-right corner
            y = hole.y + hole.height - size.height - 24;
        } else if (hole.x + hole.width + GAP + size.width <= getWidth() - 12) {
            x = hole.x + hole.width + GAP;                       // right of the target
            y = hole.y + Math.min(40, hole.height / 4);
        } else if (hole.x - GAP - size.width >= 12) {
            x = hole.x - GAP - size.width;                       // left of it
            y = hole.y + Math.min(40, hole.height / 4);
        } else if (hole.y + hole.height + GAP + size.height <= getHeight() - 12) {
            x = hole.x + hole.width - size.width;                // below
            y = hole.y + hole.height + GAP;
        } else {
            x = hole.x + (hole.width - size.width) / 2;          // over a large area: centre on it
            y = hole.y + (hole.height - size.height) / 2;
        }
        x = Math.max(12, Math.min(x, getWidth() - size.width - 12));
        y = Math.max(12, Math.min(y, getHeight() - size.height - 12));
        card.setBounds(x, y, size.width, size.height);
        card.revalidate();
        repaint();
        next.requestFocusInWindow();
    }

    private void finish() {
        finished = true;
        close();
        onFinish.run();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Area dim = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
        RoundRectangle2D cut = null;
        if (hole != null) {
            cut = new RoundRectangle2D.Float(hole.x, hole.y, hole.width, hole.height, RADIUS * 2, RADIUS * 2);
            dim.subtract(new Area(cut));
        }
        g2.setColor(new Color(0, 0, 0, Theme.isDark() ? 150 : 110));
        g2.fill(dim);
        if (cut != null) {
            g2.setStroke(new BasicStroke(6f));
            g2.setColor(Theme.withAlpha(Theme.BLUE, 70));
            g2.draw(cut);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Theme.BLUE);
            g2.draw(cut);
        }
        g2.dispose();
    }

    @Override
    protected void focusFirst() {
        next.requestFocusInWindow();
    }

    /** Small progress dots. */
    private static final class Dots extends JComponent {
        private final int count;
        int current;

        Dots(int count) {
            this.count = count;
            setPreferredSize(new Dimension(count * 12, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < count; i++) {
                g2.setColor(i == current ? Theme.BLUE : Theme.FILL.darker());
                int w = i == current ? 14 : 6;
                int x = i * 12 + (i > current ? 8 : 0); // the current dot is wider
                g2.fillRoundRect(x, 2, w, 6, 6, 6);
            }
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(count * 12 + 8, 10);
        }

        @Override
        public javax.accessibility.AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJComponent() { };
            }
            return accessibleContext;
        }
    }
}
