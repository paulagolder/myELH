package org.lerot.myELH;

import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class svgview extends JPanel
{
    private static final long serialVersionUID = 1L;
    private styleMap stylemap;
    elhDrawNode topdrawnode;
    private elhDrawNode clickednode = null;
    JSVGCanvas canvas;

    public svgview(styleMap astylemap)
    {
        this.stylemap = astylemap;
        setVisible(true);
        setBackground(Color.lightGray);
    }


    public void showview(svgdoc asvgdoc)
    {
        dimensionp d = asvgdoc.getExportSize();
        Element root = asvgdoc.document.getDocumentElement();
        asvgdoc.svggraphic.getRoot(root);
        this.topdrawnode = asvgdoc.topdrawnode;
        this.canvas = new JSVGCanvas();
        this.canvas.setDocumentState(1);
        this.canvas.addMouseMotionListener(new MyMouseListener());
        this.canvas.addMouseListener(new MyMouseListener());
        add(this.canvas);
        this.canvas.setDocument(asvgdoc.document);
        this.canvas.setBackground(Color.yellow);
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
                int w = (svgview.this.clickednode.getDimension()).width;
                int h = (svgview.this.clickednode.getDimension()).height;
                Graphics graphics = svgview.this.getGraphics();
                graphics.setXORMode(svgview.this.getBackground());
                graphics.drawRect(x, y, w, h);
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
            int x = evt.getX();
            int y = evt.getY();
            if (svgview.this.clickednode != null)
            {
                System.out.println("moved to  " + x + ":" + y);
            } else
            {
                svgview.this.clickednode = svgview.this.topdrawnode.getTarget(x, y);
                System.out.println("pressed " + x + ":" + y);
                if (svgview.this.topdrawnode.getTarget(x, y) != null)
                {
                    this.startx = x;
                    this.starty = y;
                    Rectangle position = svgview.this.clickednode.getBounds();
                    System.out.println("found " + svgview.this.clickednode + position);
                    Graphics graphics = svgview.this.canvas.getGraphics();
                    graphics.setColor(Color.RED);
                    graphics.drawRect(position.x + 1, position.y + 1, position.width, position.height);
                    graphics.dispose();
                }
            }
        }

        public void mouseClicked(MouseEvent evt)
        {
            int x = evt.getX();
            int y = evt.getY();
            elhDrawNode oldactivenode = clickednode;
            oldactivenode.drawborder(svgview.this,Color.BLUE,1);
            svgview.this.clickednode = svgview.this.topdrawnode.getTarget(x, y);
            System.out.println("clicked " + x + ":" + y);
            clickednode.drawborder(svgview.this,Color.RED,4);
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


