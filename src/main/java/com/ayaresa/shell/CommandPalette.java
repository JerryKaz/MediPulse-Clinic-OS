package com.ayaresa.shell;

import com.ayaresa.domain.Patient;
import com.ayaresa.security.AccessPolicy;
import com.ayaresa.security.Screen;
import com.ayaresa.ui.Card;
import com.ayaresa.ui.Icons;
import com.ayaresa.ui.Overlay;
import com.ayaresa.ui.TablerIcon;
import com.ayaresa.ui.Theme;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ctrl+K / Cmd+K: one search box for patients (by name, ID, phone, Ghana Card or NHIS
 * number), screens and common actions. Arrow keys move, Enter runs, Esc closes.
 * Results respect the access policy, so nobody is offered something they can't open.
 */
final class CommandPalette extends Overlay {

    /** One result row. */
    record Item(String title, String subtitle, TablerIcon icon, Color tint, Runnable run) {
    }

    /** An action offered in the palette, shown only if the role can edit its screen. */
    record Action(String title, String keywords, TablerIcon icon, Screen requires, Runnable run) {
    }

    private final AppContext app;
    private final List<Action> actions;
    private final JTextField input = new JTextField();
    private final DefaultListModel<Item> model = new DefaultListModel<>();
    private final JList<Item> list = new JList<>(model);

    private CommandPalette(AppContext app, List<Action> actions) {
        super(app.window(), null, true);
        this.app = app;
        this.actions = actions;
        setLayout(new TopLayout());

        Card card = new Card(new BorderLayout(0, 8), 22);
        card.fill(Theme.RAISED_BG);
        card.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel field = new JPanel(new BorderLayout(10, 0));
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        field.add(new JLabel(Icons.of(TablerIcon.SEARCH, 20, Theme.SECONDARY_LABEL)), BorderLayout.WEST);
        input.setFont(Theme.font(Font.PLAIN, 18));
        input.setForeground(Theme.LABEL);
        input.setCaretColor(Theme.BLUE);
        input.setBorder(BorderFactory.createEmptyBorder());
        input.setOpaque(false);
        input.getAccessibleContext().setAccessibleName("Search patients, screens and actions");
        field.add(input, BorderLayout.CENTER);
        card.add(field, BorderLayout.NORTH);

        list.setCellRenderer(new Renderer());
        list.setOpaque(false);
        list.setBackground(Theme.RAISED_BG);
        list.setFixedCellHeight(52);
        list.setVisibleRowCount(8);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SEPARATOR));
        scroll.getViewport().setBackground(Theme.RAISED_BG);
        card.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("↑↓ to move   ↵ to open   esc to close");
        hint.setFont(Theme.footnote());
        hint.setForeground(Theme.TERTIARY_LABEL);
        hint.setBorder(BorderFactory.createEmptyBorder(2, 8, 0, 0));
        card.add(hint, BorderLayout.SOUTH);
        add(card);

        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { search(); }
            public void removeUpdate(DocumentEvent e) { search(); }
            public void changedUpdate(DocumentEvent e) { search(); }
        });
        bind(KeyEvent.VK_DOWN, () -> move(1));
        bind(KeyEvent.VK_UP, () -> move(-1));
        bind(KeyEvent.VK_ENTER, this::runSelected);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                runSelected();
            }
        });
        search();
    }

    static void show(AppContext app, List<Action> actions) {
        CommandPalette p = new CommandPalette(app, actions);
        p.present();
        p.input.requestFocusInWindow();
    }

    private void bind(int key, Runnable r) {
        input.getInputMap().put(KeyStroke.getKeyStroke(key, 0), "palette-" + key);
        input.getActionMap().put("palette-" + key, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                r.run();
            }
        });
    }

    private void move(int delta) {
        if (model.isEmpty()) {
            return;
        }
        int i = Math.floorMod(list.getSelectedIndex() + delta, model.size());
        list.setSelectedIndex(i);
        list.ensureIndexIsVisible(i);
    }

    private void runSelected() {
        Item item = list.getSelectedValue();
        if (item != null) {
            close();
            item.run().run();
        }
    }

    private void search() {
        String q = input.getText().trim().toLowerCase(Locale.ROOT);
        List<Item> out = new ArrayList<>();
        LocalDate today = app.store().today();

        if (!q.isEmpty() && app.user().canView(Screen.PATIENTS)) {
            for (Patient p : app.store().searchPatients(q, 6)) {
                String nhis = p.nhisActive(today) ? "NHIS active" : p.hasNhis() ? "NHIS expired" : "No NHIS";
                out.add(new Item(p.name(), p.id() + "  ·  " + p.age(today) + " y  ·  " + nhis,
                        TablerIcon.USER, Theme.BLUE, () -> app.openPatient(p.id())));
            }
        }
        for (Action a : actions) {
            if (app.user().canEdit(a.requires()) && (q.isEmpty() || (a.title() + " " + a.keywords()).toLowerCase(Locale.ROOT).contains(q))) {
                out.add(new Item(a.title(), "Action", a.icon(), Theme.ORANGE, a.run()));
            }
        }
        for (Screen s : AccessPolicy.visibleScreens(app.user().role())) {
            if (q.isEmpty() || s.title().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(new Item("Go to " + s.title(), "Screen", ScreenStyle.icon(s), ScreenStyle.tint(s), () -> app.open(s)));
            }
        }
        model.clear();
        out.forEach(model::addElement);
        if (!out.isEmpty()) {
            list.setSelectedIndex(0);
        }
    }

    private static final class Renderer extends JPanel implements ListCellRenderer<Item> {
        private final JLabel icon = new JLabel();
        private final JLabel title = new JLabel();
        private final JLabel sub = new JLabel();

        Renderer() {
            super(new BorderLayout(12, 0));
            setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            JPanel text = new JPanel(new java.awt.GridLayout(2, 1));
            text.setOpaque(false);
            title.setFont(Theme.bodyBold());
            sub.setFont(Theme.footnote());
            text.add(title);
            text.add(sub);
            add(icon, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Item> l, Item v, int i, boolean sel, boolean f) {
            icon.setIcon(Icons.of(v.icon(), 20, v.tint()));
            title.setText(v.title());
            title.setForeground(Theme.LABEL);
            sub.setText(v.subtitle());
            sub.setForeground(Theme.TERTIARY_LABEL);
            setBackground(sel ? Theme.SELECTION : Theme.RAISED_BG);
            setOpaque(true);
            return this;
        }
    }

    /** Centres the card horizontally, a fifth of the way down. */
    private static final class TopLayout implements LayoutManager {
        @Override public void addLayoutComponent(String n, Component c) { }
        @Override public void removeLayoutComponent(Component c) { }
        @Override public Dimension preferredLayoutSize(Container p) { return p.getSize(); }
        @Override public Dimension minimumLayoutSize(Container p) { return new Dimension(0, 0); }

        @Override
        public void layoutContainer(Container p) {
            if (p.getComponentCount() > 0) {
                int w = Math.min(640, p.getWidth() - 40);
                int h = Math.min(520, p.getHeight() - 120);
                p.getComponent(0).setBounds((p.getWidth() - w) / 2, Math.max(40, p.getHeight() / 7), w, h);
            }
        }
    }

    @Override
    protected void focusFirst() {
        input.requestFocusInWindow();
    }
}
