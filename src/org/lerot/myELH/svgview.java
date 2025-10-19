package org.lerot.myELH;

import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class svgview extends JPanel
{
    private static final long serialVersionUID = 1L;
    elhDrawNode topdrawnode;
    JSVGCanvas canvas;
    private styleMap stylemap;
    private elhDrawNode clickednode = null;

    public svgview(styleMap astylemap)
    {
        this.stylemap = astylemap;
        setVisible(true);
        setBackground(Color.lightGray);
    }

    public void showview(svgdoc asvgdoc)
    {
        Element root = asvgdoc.document.getDocumentElement();
        asvgdoc.svggraphic.getRoot(root);
        this.topdrawnode = asvgdoc.topdrawnode;
        dimensionp d = asvgdoc.createLayout(this.topdrawnode, asvgdoc.hs);
        System.out.println("========================================");
        this.canvas = new JSVGCanvas();
        this.canvas.setDocumentState(1);
        this.canvas.addMouseMotionListener(new MyMouseListener());
        this.canvas.addMouseListener(new MyMouseListener());
        add(this.canvas);
        this.canvas.setDocument(asvgdoc.document);
        this.canvas.setBackground(asvgdoc.backgroundcolor);
        this.canvas.setVisible(true);
        this.canvas.setSize((int) d.width, (int) d.height);
        setVisible(true);
    }

    public void setNodeStyle(styleMap aDefault)
    {
        stylemap = aDefault;
    }

    class MyMouseListener extends MouseAdapter
    {
        private int startx;
        private int starty;

        public void mouseDragged(MouseEvent event)
        {
            int x = event.getX();
            int y = event.getY();
            System.out.println("dragged " + x + ":" + y);
            if (svgview.this.clickednode != null)
            {
                ShapeI si = svgview.this.clickednode.bounds.getShapeI();
                Graphics graphics = svgview.this.getGraphics();
                graphics.setXORMode(svgview.this.getBackground());
                graphics.drawRect(x, y, si.width, si.height);
                graphics.dispose();
            }
        }

        public void mouseMoved(MouseEvent event)
        {
            int x = event.getX();
            int y = event.getY();
            // System.out.println("moving " + x + ":" + y);
            if (svgview.this.topdrawnode.getTarget(x, y) != null)
            {
                svgview.this.setCursor(Cursor.getPredefinedCursor(1));
            } else
            {
                svgview.this.setCursor(Cursor.getDefaultCursor());
            }
        }

        public void mousePressed(MouseEvent evt)
        {

        }

        public void mouseClicked(MouseEvent evt)
        {
            int x = evt.getX();
            int y = evt.getY();
            System.out.println("clicked " + x + ":" + y);
            elhDrawNode oldactivenode = clickednode;
            elhDrawNode clickednode = svgview.this.topdrawnode.getTarget(x, y);
            if (clickednode == null) return;
            int selectednodewidth = 4;
            Color selectednodecolor = Color.red;
            Graphics graphics = svgview.this.canvas.getGraphics();
            Graphics2D g2d = (Graphics2D) graphics;
            if (oldactivenode != null)
            {
                oldactivenode.drawbackgroundborder(g2d, selectednodewidth);
                oldactivenode.drawnodeborder(g2d);
            }
            svgview.this.clickednode = clickednode;
            clickednode.drawborder(g2d, selectednodecolor, selectednodewidth);
        }
    }

    class MyMouseMotionListener extends MouseMotionAdapter
    {
        public void mouseReleased(MouseEvent e)
        {
            int x = e.getX();
            int y = e.getY();
            System.out.println("released " + x + ":" + y);
        }

        public void mouseEntered(MouseEvent e)
        {
            int x = e.getX();
            int y = e.getY();
            System.out.println("entered " + x + ":" + y);
        }

        public void mouseExited(MouseEvent e)
        {
            int x = e.getX();
            int y = e.getY();
            System.out.println("exited " + x + ":" + y);
        }
    }
}


