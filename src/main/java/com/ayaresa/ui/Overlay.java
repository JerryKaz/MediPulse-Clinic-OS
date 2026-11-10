package com.ayaresa.ui;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Dimmed in-window layer for sheets, drawers and the command palette. Lives in the frame's
 * layered pane (not a separate OS window), so it works the same on every platform, follows
 * window resizes and appears in screenshots. Esc closes it; clicks behind it are blocked.
 */
public class Overlay extends JPanel {

    private final JLayeredPane layers;
    private final ComponentAdapter resizer;
    private final Component previousFocus;
    private Runnable onClose = () -> { };
    private boolean closed;

    protected Overlay(Component anchor, LayoutManager layout, boolean closeOnBackdropClick) {
        super(layout);
        setOpaque(false);
        RootPaneContainer window = anchor instanceof RootPaneContainer r ? r
                : (RootPaneContainer) SwingUtilities.getAncestorOfClass(RootPaneContainer.class, anchor);
        if (window == null) {
            throw new IllegalStateException("Overlay needs a component inside a window");
        }
        layers = window.getLayeredPane();
        previousFocus = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

        MouseAdapter swallow = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (closeOnBackdropClick && e.getComponent() == Overlay.this) {
                    close();
                }
            }
        };
        addMouseListener(swallow);
        addMouseMotionListener(new MouseAdapter() { });
        addMouseWheelListener(e -> { });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-overlay");
        getActionMap().put("close-overlay", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                close();
            }
        });

        resizer = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setBounds(0, 0, layers.getWidth(), layers.getHeight());
                revalidate();
            }
        };
    }

    /** Adds the overlay to the window. */
    protected void present() {
        setBounds(0, 0, layers.getWidth(), layers.getHeight());
        layers.add(this, JLayeredPane.MODAL_LAYER);
        layers.addComponentListener(resizer);
        layers.revalidate();
        layers.repaint();
        SwingUtilities.invokeLater(this::focusFirst);
    }

    public void onClose(Runnable r) {
        this.onClose = r;
    }

    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        layers.removeComponentListener(resizer);
        layers.remove(this);
        layers.repaint();
        if (previousFocus != null) {
            previousFocus.requestFocusInWindow();
        }
        onClose.run();
    }

    protected void focusFirst() {
        Component c = firstFocusable(this);
        if (c != null) {
            c.requestFocusInWindow();
        }
    }

    private static Component firstFocusable(Container root) {
        for (Component c : root.getComponents()) {
            if (c instanceof javax.swing.text.JTextComponent || c instanceof javax.swing.JComboBox<?>) {
                if (c.isEnabled() && c.isFocusable()) {
                    return c;
                }
            }
            if (c instanceof Container k) {
                Component found = firstFocusable(k);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(new Color(0, 0, 0, Theme.isDark() ? 120 : 70));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }

    /** Overlays always cover the window, so let Swing skip optimisations that assume otherwise. */
    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    protected static JComponent bare(JComponent c) {
        c.setOpaque(false);
        return c;
    }
}
