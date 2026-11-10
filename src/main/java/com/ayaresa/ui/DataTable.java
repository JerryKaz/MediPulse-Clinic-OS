package com.ayaresa.ui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A typed, styled table: columns are declared as functions of the row object, so screens
 * never deal with Object[][] or column indexes. Double-click or Enter opens a row.
 */
public final class DataTable<T> extends Card {

    /** How a column's value is drawn. */
    public enum Kind { TEXT, SECONDARY, BOLD, RIGHT, PILL, PERSON, ITEM }

    /** A status pill value. */
    public record Pill(String text, StatusPill.Kind kind) {
    }

    /** Name with a smaller line underneath, and an avatar. */
    public record Person(String name, String subtitle) {
    }

    public record Column<T>(String title, Function<T, Object> value, int width, Kind kind) {
    }

    private final List<Column<T>> columns;
    private final Model model = new Model();
    private final JTable table;
    private final JLabel empty = new JLabel("", SwingConstants.CENTER);
    private final JPanel stack = new JPanel(new BorderLayout());
    private final JScrollPane scroll;
    private Consumer<T> onOpen = t -> { };
    private int hoverRow = -1;

    public DataTable(List<Column<T>> columns) {
        super(new BorderLayout(), Theme.RADIUS_CARD);
        setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        this.columns = List.copyOf(columns);

        table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (c instanceof JComponent jc) {
                    jc.setOpaque(true);
                    jc.setBackground(isRowSelected(row) ? Theme.SELECTION : row == hoverRow ? Theme.hover() : Theme.CARD_BG);
                }
                return c;
            }
        };
        table.setRowHeight(46);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.SEPARATOR);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(Theme.CARD_BG);
        table.setForeground(Theme.LABEL);
        table.setFont(Theme.body());
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(false);
        table.getAccessibleContext().setAccessibleName("Table");

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer(new HeaderRenderer());
        header.setBackground(Theme.CARD_BG);
        header.setPreferredSize(new Dimension(0, 34));

        for (int i = 0; i < columns.size(); i++) {
            Column<T> c = columns.get(i);
            var tc = table.getColumnModel().getColumn(i);
            tc.setCellRenderer(new CellRenderer(c.kind()));
            if (c.width() > 0) {
                tc.setPreferredWidth(c.width());
                if (c.kind() == Kind.PILL || c.kind() == Kind.RIGHT) {
                    tc.setMaxWidth(c.width() + 60);
                }
            }
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    selected().ifPresent(onOpen);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverRow = -1;
                table.repaint();
            }
        });
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r != hoverRow) {
                    hoverRow = r;
                    table.repaint();
                }
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "open-row");
        table.getActionMap().put("open-row", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                selected().ifPresent(onOpen);
            }
        });

        scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Theme.CARD_BG);
        scroll.setOpaque(false);

        empty.setFont(Theme.body());
        empty.setForeground(Theme.SECONDARY_LABEL);
        stack.setOpaque(false);
        stack.add(scroll, BorderLayout.CENTER);
        add(stack, BorderLayout.CENTER);
    }

    public void setRows(List<T> rows, String emptyMessage) {
        Optional<T> keep = selected();
        model.rows = new ArrayList<>(rows);
        model.fireTableDataChanged();
        keep.ifPresent(k -> {
            int i = model.rows.indexOf(k);
            if (i >= 0) {
                table.setRowSelectionInterval(i, i);
            }
        });
        stack.removeAll();
        if (rows.isEmpty()) {
            empty.setText(emptyMessage);
            stack.add(empty, BorderLayout.CENTER);
        } else {
            stack.add(scroll, BorderLayout.CENTER);
        }
        stack.revalidate();
        stack.repaint();
    }

    public Optional<T> selected() {
        int r = table.getSelectedRow();
        return r < 0 || r >= model.rows.size() ? Optional.empty() : Optional.of(model.rows.get(r));
    }

    public void onOpen(Consumer<T> handler) {
        this.onOpen = handler;
    }

    public void onSelect(Consumer<Optional<T>> handler) {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handler.accept(selected());
            }
        });
    }

    public JTable table() {
        return table;
    }

    public List<T> rows() {
        return List.copyOf(model.rows);
    }

    private final class Model extends AbstractTableModel {
        List<T> rows = new ArrayList<>();

        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.size(); }
        @Override public String getColumnName(int c) { return columns.get(c).title(); }
        @Override public Object getValueAt(int r, int c) { return columns.get(c).value().apply(rows.get(r)); }
        @Override public boolean isCellEditable(int r, int c) { return false; }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, false, false, r, c);
            l.setText(v == null ? "" : v.toString().toUpperCase());
            l.setFont(Theme.caption());
            l.setForeground(Theme.TERTIARY_LABEL);
            l.setBackground(Theme.CARD_BG);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.SEPARATOR),
                    BorderFactory.createEmptyBorder(0, 12, 0, 12)));
            return l;
        }
    }

    private static final class CellRenderer implements TableCellRenderer {
        private final Kind kind;
        private final DefaultTableCellRenderer text = new DefaultTableCellRenderer();

        CellRenderer(Kind kind) {
            this.kind = kind;
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            switch (kind) {
                case PILL -> {
                    JPanel p = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 11));
                    if (v instanceof Pill pill) {
                        p.add(new StatusPill(pill.text(), pill.kind()));
                    }
                    return p;
                }
                case PERSON, ITEM -> {
                    JPanel p = new JPanel(new BorderLayout(10, 0));
                    p.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 8));
                    if (v instanceof Person person) {
                        if (kind == Kind.PERSON) {
                            p.add(new Avatar(Avatar.initialsOf(person.name()), person.name(), 30), BorderLayout.WEST);
                        }
                        JPanel lines = new JPanel(new java.awt.GridLayout(2, 1));
                        lines.setOpaque(false);
                        JLabel n = new JLabel(person.name());
                        n.setFont(Theme.bodyBold());
                        n.setForeground(Theme.LABEL);
                        JLabel sub = new JLabel(person.subtitle());
                        sub.setFont(Theme.footnote());
                        sub.setForeground(Theme.TERTIARY_LABEL);
                        lines.add(n);
                        lines.add(sub);
                        p.add(lines, BorderLayout.CENTER);
                    }
                    return p;
                }
                default -> {
                    JLabel l = (JLabel) text.getTableCellRendererComponent(t, v == null ? "" : v, false, false, r, c);
                    l.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                    l.setFont(kind == Kind.BOLD ? Theme.bodyBold() : kind == Kind.SECONDARY ? Theme.footnote() : Theme.body());
                    l.setForeground(kind == Kind.SECONDARY ? Theme.SECONDARY_LABEL : Theme.LABEL);
                    l.setHorizontalAlignment(kind == Kind.RIGHT ? SwingConstants.RIGHT : SwingConstants.LEFT);
                    return l;
                }
            }
        }
    }

    /** Convenience for declaring columns tersely. */
    public static <T> Column<T> col(String title, Function<T, Object> value, int width, Kind kind) {
        return new Column<>(title, value, width, kind);
    }
}
