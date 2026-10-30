package org.lerot.myELH;

import org.lerot.mywidgets.jswTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;

// Shows an event's ProcessFlowchart on screen, inside a scroll pane;
// Ctrl + mouse wheel zooms.
public class FlowchartPanel extends JComponent
{
    private static final long serialVersionUID = 1L;
    private static final double MINZOOM = 0.25;
    private static final double MAXZOOM = 4.0;
    private ProcessFlowchart chart;
    private double zoom = 1.0;

    public FlowchartPanel()
    {
        setOpaque(true);
        setBackground(Color.decode(jswTheme.DETAILAREA));
        addMouseWheelListener(new MouseAdapter()
        {
            public void mouseWheelMoved(MouseWheelEvent e)
            {
                if (e.isControlDown())
                {
                    setZoom(zoom * Math.pow(1.1, -e.getPreciseWheelRotation()));
                } else
                {
                    // let the scroll pane scroll
                    getParent().dispatchEvent(SwingUtilities.convertMouseEvent(FlowchartPanel.this, e, getParent()));
                }
            }
        });
    }

    // shows a new flowchart, or nothing if null
    public void setChart(ProcessFlowchart chart)
    {
        this.chart = chart;
        revalidate();
        repaint();
    }

    public ProcessFlowchart getChart()
    {
        return this.chart;
    }

    public double getZoom()
    {
        return this.zoom;
    }

    public void setZoom(double newzoom)
    {
        this.zoom = Math.max(MINZOOM, Math.min(MAXZOOM, newzoom));
        revalidate();
        repaint();
    }

    public Dimension getPreferredSize()
    {
        if (this.chart == null) return new Dimension(0, 0);
        return new Dimension((int) Math.ceil((this.chart.getWidth() + 8) * this.zoom),
                (int) Math.ceil((this.chart.getHeight() + 8) * this.zoom));
    }

    protected void paintComponent(Graphics g)
    {
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        if (this.chart == null) return;
        Graphics2D g2d = (Graphics2D) g.create();
        try
        {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.scale(this.zoom, this.zoom);
            g2d.translate(4, 4);
            this.chart.draw(g2d);
        } finally
        {
            g2d.dispose();
        }
    }
}
