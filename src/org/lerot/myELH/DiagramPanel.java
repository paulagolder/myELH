package org.lerot.myELH;

import org.lerot.mywidgets.jswTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.Consumer;

// Shows an ElhDiagram on screen, inside a scroll pane. Clicking a box
// selects its node; Ctrl + mouse wheel zooms.
public class DiagramPanel extends JComponent
{
    private static final long serialVersionUID = 1L;
    private static final double MINZOOM = 0.25;
    private static final double MAXZOOM = 4.0;
    private static final Color SELECTEDCOLOR = Color.red;
    private static final int SELECTEDWIDTH = 4;
    private ElhDiagram diagram;
    private ElhNode selected;
    private double zoom = 1.0;

    public DiagramPanel(Consumer<ElhNode> onselect)
    {
        setOpaque(true);
        setBackground(Color.decode(jswTheme.DETAILAREA));
        MouseAdapter mouse = new MouseAdapter()
        {
            public void mouseClicked(MouseEvent e)
            {
                ElhDrawNode clicked = nodeAt(e.getPoint());
                if (clicked != null)
                {
                    onselect.accept(clicked.source);
                }
            }

            public void mouseMoved(MouseEvent e)
            {
                setCursor(nodeAt(e.getPoint()) != null
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            }

            public void mouseWheelMoved(MouseWheelEvent e)
            {
                if (e.isControlDown())
                {
                    setZoom(zoom * Math.pow(1.1, -e.getPreciseWheelRotation()));
                } else
                {
                    // let the scroll pane scroll
                    getParent().dispatchEvent(SwingUtilities.convertMouseEvent(DiagramPanel.this, e, getParent()));
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    // shows a new diagram (or nothing, if null), outlining the selected node's box
    public void setDiagram(ElhDiagram diagram, ElhNode selected)
    {
        this.diagram = diagram;
        this.selected = selected;
        revalidate();
        repaint();
    }

    public ElhDiagram getDiagram()
    {
        return diagram;
    }

    public double getZoom()
    {
        return zoom;
    }

    public void setZoom(double newzoom)
    {
        this.zoom = Math.max(MINZOOM, Math.min(MAXZOOM, newzoom));
        revalidate();
        repaint();
    }

    private ElhDrawNode nodeAt(Point p)
    {
        if (diagram == null) return null;
        return diagram.nodeAt(p.x / zoom, p.y / zoom);
    }

    public Dimension getPreferredSize()
    {
        if (diagram == null) return new Dimension(0, 0);
        // the frame round the diagram is 4 wide, half of it outside
        return new Dimension((int) Math.ceil((diagram.getWidth() + 2) * zoom),
                (int) Math.ceil((diagram.getHeight() + 2) * zoom));
    }

    protected void paintComponent(Graphics g)
    {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        if (diagram == null) return;
        Graphics2D g2d = (Graphics2D) g.create();
        try
        {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.scale(zoom, zoom);
            diagram.draw(g2d);
            ElhDrawNode selectedbox = selected == null ? null : diagram.findNode(selected);
            if (selectedbox != null)
            {
                selectedbox.drawborder(g2d, SELECTEDCOLOR, SELECTEDWIDTH);
            }
        } finally
        {
            g2d.dispose();
        }
    }
}
