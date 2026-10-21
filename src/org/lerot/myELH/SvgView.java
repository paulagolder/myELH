package org.lerot.myELH;

import org.apache.batik.swing.JSVGCanvas;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SvgView extends JPanel
{
    private static final long serialVersionUID = 1L;
    ElhDrawNode topdrawnode;
    JSVGCanvas canvas;
    private StyleMap stylemap;
    private ElhDrawNode clickednode = null;

    public SvgView(StyleMap astylemap)
    {
        this.stylemap = astylemap;
        setVisible(true);
        setBackground(Color.decode(org.lerot.mywidgets.jswTheme.DETAILAREA));
    }

    public void showview(SvgDoc asvgdoc)
    {
        Element root = asvgdoc.document.getDocumentElement();
        asvgdoc.svggraphic.getRoot(root);
        this.topdrawnode = asvgdoc.topdrawnode;
        DimensionP d = asvgdoc.createLayout(this.topdrawnode, asvgdoc.hs);
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

    public void setNodeStyle(StyleMap aDefault)
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
            if (SvgView.this.clickednode != null)
            {
                ShapeI si = SvgView.this.clickednode.bounds.getShapeI();
                Graphics graphics = SvgView.this.getGraphics();
                graphics.setXORMode(SvgView.this.getBackground());
                graphics.drawRect(x, y, si.width, si.height);
                graphics.dispose();
            }
        }

        public void mouseMoved(MouseEvent event)
        {
            int x = event.getX();
            int y = event.getY();
            if (SvgView.this.topdrawnode.getTarget(x, y) != null)
            {
                SvgView.this.setCursor(Cursor.getPredefinedCursor(1));
            } else
            {
                SvgView.this.setCursor(Cursor.getDefaultCursor());
            }
        }

        public void mousePressed(MouseEvent evt)
        {

        }

        public void mouseClicked(MouseEvent evt)
        {
            int x = evt.getX();
            int y = evt.getY();
            ElhDrawNode oldactivenode = SvgView.this.clickednode;
            ElhDrawNode newclickednode = SvgView.this.topdrawnode.getTarget(x, y);
            if (newclickednode == null) return;
            int selectednodewidth = 4;
            Color selectednodecolor = Color.red;
            Graphics graphics = SvgView.this.canvas.getGraphics();
            Graphics2D g2d = (Graphics2D) graphics;
            if (oldactivenode != null)
            {
                oldactivenode.drawbackgroundborder(g2d, selectednodewidth);
                oldactivenode.drawnodeborder(g2d);
            }
            SvgView.this.clickednode = newclickednode;
            newclickednode.drawborder(g2d, selectednodecolor, selectednodewidth);
        }
    }
}
