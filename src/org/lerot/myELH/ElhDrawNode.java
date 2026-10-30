package org.lerot.myELH;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

// One box of an ElhDiagram, with the boxes for its children below it
public class ElhDrawNode
{
    final ElhDiagram diagram;
    // the tree node this box shows
    final ElhNode source;
    String text;
    String elhtype;
    List<ElhDrawNode> children = new ArrayList<>();
    NodeStyle nodestylemap;
    ShapeD bounds = new ShapeD();
    private final ChildGrouping childgrouptype;
    private final ElhDrawNode parent;
    private final double symbolshift = 10;

    ElhDrawNode(ElhDiagram diagram, ElhNode anode, ElhDrawNode parent, int depth)
    {
        this.diagram = diagram;
        this.source = anode;
        this.parent = parent;
        this.text = anode.getName();
        this.childgrouptype = anode.getChildgrouptype();
        this.elhtype = anode.getType();
        // the top box is styled by its style name, the others by their type
        // every box is drawn in the style of its type
        nodestylemap = diagram.stylemap.getNodeStyle(elhtype);
        if (depth < ElhDiagram.MAXDEPTH)
        {
            for (ElhNode acnode : anode.getChildren())
            {
                this.children.add(new ElhDrawNode(diagram, acnode, this, depth + 1));
            }
        }
    }

    static Dimension sizeString(String[] label, Graphics g2d, Font afont)
    {
        g2d.setFont(afont);
        FontMetrics fm = g2d.getFontMetrics();
        Dimension lsize = new Dimension(0, 0);
        double h = 0.0D;
        double w = 0.0D;
        byte b;
        int i;
        String[] arrayOfString;
        for (i = (arrayOfString = label).length, b = 0; b < i; )
        {
            String text = arrayOfString[b];
            Rectangle2D r = fm.getStringBounds(text, g2d);
            if (r.getWidth() > w)
            {
                w = r.getWidth();
            }
            h += r.getHeight();
            b++;
        }
        lsize.width = (int) w;
        lsize.height = (int) h;
        return lsize;
    }

    static String[] formatLabel(String text, Graphics g2d, double w)
    {
        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(text, g2d);
        if (r.getWidth() <= w)
        {
            String[] textarray = {text};
            return textarray;
        }
        int targetwidth = (int) (w / r.getWidth() * text.length());
        String[] parts = text.split(" ");
        if (parts.length > 1)
        {
            String[] textarray2 = parts;
            return textarray2;
        }
        String[] textarray3 = {text};
        return textarray3;
    }

    public boolean hasChildren()
    {
        return !this.children.isEmpty();
    }

    public List<ElhDrawNode> getChildren()
    {
        return this.children;
    }

    ChildGrouping getChildgrouptype()
    {
        if (this.childgrouptype == null)
        {
            return ChildGrouping.SEQUENCE;
        }
        return this.childgrouptype;
    }

    public ElhDrawNode getparent()
    {
        return this.parent;
    }

    public String getText()
    {
        return this.text;
    }

    public int countChildren()
    {
        return this.children.size();
    }

    public boolean hasParent()
    {
        return (this.parent != null);
    }

    public String toString()
    {
        return this.text ;
    }

    boolean containsPoint(double px, double py)
    {
        return bounds.containsPoint(px, py);
    }

    // the box for source, here or below
    ElhDrawNode find(ElhNode source)
    {
        if (this.source == source) return this;
        for (ElhDrawNode adnode : this.children)
        {
            ElhDrawNode fnode = adnode.find(source);
            if (fnode != null) return fnode;
        }
        return null;
    }

    ElhDrawNode getTarget(double px, double py)
    {
        if (hasChildren())
        {
            for (ElhDrawNode adnode : this.children)
            {
                ElhDrawNode fnode = adnode.getTarget(px, py);
                if (fnode != null) return fnode;
            }
        }
        if (containsPoint(px, py)) return this;
        return null;
    }

    public void drawHardborder(Graphics2D g2d, Color acolor, int width)
    {
        g2d.setStroke(new BasicStroke((float) width));
        g2d.setColor(acolor);
        bounds.drawRect(g2d);
    }

    public void drawSoftborder(Graphics2D g2d, Color acolor, int width)
    {
        g2d.setStroke(new BasicStroke((float) width));
        g2d.setColor(acolor);
        bounds.drawSoftRect(g2d);
    }

    public void drawnodeborder(Graphics2D g2d)
    {
        Color acolor = nodestylemap.getColor("borderColor");
        int bw = (int) nodestylemap.getDouble("borderWidth");
        String rectstyle = nodestylemap.getString( "rectanglestyle");
        switch (rectstyle)
        {
            case ("hard") -> drawHardborder(g2d, acolor, bw);
            case ("soft")  ->drawSoftborder(g2d, acolor, bw);
            default ->drawHardborder(g2d, acolor, bw);
        }




    }

    ShapeD DrawShape(Graphics2D g2d)
    {
        double w = bounds.width;
        double h = bounds.height;
        g2d.setFont(diagram.docfont);
        String[] ftext = formatLabel(text, g2d, w);
        FontMetrics fm = g2d.getFontMetrics();
        Dimension r = sizeString(ftext, g2d, diagram.docfont);
        double x = bounds.x;
        double y = bounds.y;
        double anchorx = x + w / 2.0D;
        double anchory = y;
        if (!nodestylemap.getBoolean("textattop"))
        {
            anchory = y + h / 2.0D - fm.getAscent();
        }
        if (nodestylemap.getBoolean("fill"))
        {
            g2d.setColor(nodestylemap.getColor("fillColor"));
            bounds.fillRect(g2d);
        }
        g2d.setColor(nodestylemap.getColor("textColor"));
        drawString(ftext, g2d, diagram.docfont, anchorx, anchory);
        g2d.setColor(nodestylemap.getColor("borderColor"));
        drawnodeborder(g2d);
        if (nodestylemap.getBoolean("underlinetitle"))
        {
            g2d.drawLine((int) x, (int) (y + r.height), (int) (x + w), (int) (y + r.height));
        }
        return bounds;
    }

    // sets the box's size from its style and the width of its text
    void measure(Graphics2D g2d)
    {
        double w = nodestylemap.getDouble("rwidth");
        double h = nodestylemap.getDouble("rheight");
        g2d.setFont(diagram.docfont);
        String[] ftext = formatLabel(text, g2d, w);
        FontMetrics fm = g2d.getFontMetrics();
        Dimension d = sizeString(ftext, g2d, diagram.docfont);
        if (nodestylemap.getBoolean("canexpand") && d.getWidth() > w)
        {
            w = d.getWidth() + 8.0D;
        }
        bounds.setDimension(w, h);
    }

    void drawNode(Graphics g2d)
    {
        ElhDrawNode adnode = this;
        adnode.DrawShape((Graphics2D) g2d);
        adnode.drawSymbols(g2d);
        if (diagram.states != null && diagram.states.stateOf(this.source) != null)
        {
            drawState(g2d, diagram.states.stateOf(this.source).label());
        }
        if (adnode.hasParent())
        {
            drawUpLine(g2d);
        }
        if (diagram.layoutstyle == LayoutStyle.JACKSON) drawJacksonSymbols(g2d);
        if (adnode.hasChildren())
        {
            if (diagram.layoutstyle != LayoutStyle.SIMPLE) drawTriangle(g2d);
            if (diagram.layoutstyle == LayoutStyle.JACKSON || diagram.layoutstyle == LayoutStyle.SIMPLE) drawDownLine(g2d);
            else drawSymbols(g2d);
            drawHorizontalLine(g2d);
            for (ElhDrawNode adrawnode : adnode.children)
            {
                adrawnode.drawNode(g2d);
            }
        }
    }

    // the action's state indicator values, valid prior / sets to, under its box
    private void drawState(Graphics g2d, String label)
    {
        Font font = diagram.docfont.deriveFont(diagram.docfont.getSize2D() * 0.8f);
        g2d.setFont(font);
        g2d.setColor(Color.DARK_GRAY);
        FontMetrics fm = g2d.getFontMetrics();
        int x = (int) (bounds.x + bounds.width / 2 - fm.stringWidth(label) / 2.0);
        int y = (int) (bounds.y + bounds.height) + fm.getAscent() + 2;
        g2d.drawString(label, x, y);
    }

    private void drawDownLine(Graphics g2d)
    {

        double h = bounds.height;
        double  w= bounds.width;
        double vs =  diagram.vs;
        double lw = nodestylemap.getDouble("lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        double x2 = bounds.x+w/2.0D;
        double y = bounds.y+h;
        double y2 = y + vs / 2.0D;
        g2d.setColor(nodestylemap.getColor("lineColor"));
        g2d.drawLine((int) x2, (int) y, (int) x2, (int) y2);
    }

    private void drawHorizontalLine(Graphics g2d)
    {
        double lw = nodestylemap.getDouble("lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        List<ElhDrawNode> kids = this.getChildren();
        ElhDrawNode firstchild = kids.getFirst();
        ElhDrawNode lastchild = kids.getLast();
        double x = firstchild.bounds.x + firstchild.bounds.width/2.0D;
        double x2 = lastchild.bounds.x + lastchild.bounds.width/2.0D;
        double y =  this.bounds.y + this.bounds.height+diagram.vs/2.0D;
        g2d.setColor(nodestylemap.getColor("lineColor"));
        g2d.drawLine((int) x, (int) y, (int) x2, (int) y);
    }

    private void drawParallellLines(Graphics g2d)
    {
        double lw = nodestylemap.getDouble("lineWidth");
        int dy = 6;
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        List<ElhDrawNode> kids = this.getChildren();
        ElhDrawNode firstchild = kids.getFirst();
        ElhDrawNode lastchild = kids.getLast();
        double x = firstchild.bounds.x + firstchild.bounds.width/2.0D;
        double x2 = lastchild.bounds.x + lastchild.bounds.width/2.0D;
        double y =  this.bounds.y + this.bounds.height+diagram.vs/2.0D;
        g2d.setColor(nodestylemap.getColor("lineColor"));
        g2d.drawLine((int) x, (int) y, (int) x2, (int) y);
        g2d.drawLine((int) x, (int) (y+dy), (int) x2, (int) (y+dy));
    }

    private void drawJacksonLine(Graphics g2d)
    {

        drawHorizontalLine(g2d);
    }

    private void drawJacksonSymbol(Graphics g2d, String text)
    {

        double x = bounds.x + bounds.width/2;
        double y = bounds.y;
        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(text, g2d);
        int sx = (int) (x - r.getWidth());
        double sy = y + fm.getAscent();
        g2d.drawString(text, sx, (int) sy);
    }

    private void drawJacksonSymbols(Graphics g2d)
    {
        if (this.getChildgrouptype() == ChildGrouping.ROLEGROUP)
        {
            drawJacksonLine(g2d);
        }
        if (this.getparent() == null)
        {
            return;
        }
        switch (this.getparent().getChildgrouptype())
        {
            case REPETITION -> drawJacksonSymbol(g2d, "*");
            case OPTION -> drawJacksonSymbol(g2d, "O");
            default -> { }
        }
    }

    private void drawSymbols(Graphics g2d)
    {
        g2d.setColor(Color.BLACK);
        switch (this.getChildgrouptype())
        {
            case REPETITION -> drawSymbolStar(g2d);
            case OPTION -> drawSymbolCircle(g2d);
            case ROLEGROUP -> drawParallellLines(g2d);
            default -> { }
        }
    }

    private void drawSymbolStar(Graphics g2d)
    {
        double r=5;
        double x1 = bounds.x+bounds.width-symbolshift;
        double y1 = bounds.y+symbolshift;
        double x2 = x1 + r;
        double y2 = y1;
        double r2 = r * 0.7D;
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        x2 = x1 - r;
        y2 = y1;
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        x2 = x1;
        y2 = y1 - r;
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        x2 = x1;
        y2 = y1 + r;
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        x2 = x1 + r2;
        y2 = y1 + r2;
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        x2 = x1 - r2;
        y2 = y1 - r2;
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        x2 = x1 - r2;
        y2 = y1 + r2;
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        x2 = x1 + r2;
        y2 = y1 - r2;
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
    }

    private void drawString(String[] label, Graphics g2d, Font afont, double x, double y)
    {
        g2d.setFont(afont);
        FontMetrics fm = g2d.getFontMetrics();
        double sy = y + fm.getAscent();
        byte b;
        int i;
        String[] arrayOfString;
        for (i = (arrayOfString = label).length, b = 0; b < i; )
        {
            String text = arrayOfString[b];
            Rectangle2D r = fm.getStringBounds(text, g2d);
            double sx = x - r.getWidth() / 2.0D;
            g2d.drawString(text, (int) sx, (int) sy);
            sy += r.getHeight();
            b++;
        }
    }

    private void drawSymbolCircle(Graphics g2d)
    {
        double r=5;
        double x1 = bounds.x+bounds.width-symbolshift;
        double y1 = bounds.y+symbolshift;
      //  double sx = x - r / 2.0D + 1.0D;
      //  double sy = y - 2.0D;
        g2d.drawOval((int) x1, (int) y1, (int) r, (int) r);
    }

    private void drawTriangle(Graphics g2d)
    {
        ElhDrawNode adnode = this;
        NodeStyle nodestylemap = diagram.stylemap.getNodeStyle(this.elhtype);
        g2d.setColor(nodestylemap.getColor("lineColor"));
        double lw = nodestylemap.getDouble("lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        double x = bounds.x + bounds.width/2;
        double y = bounds.y+bounds.height;

            double x2 = x;
            double y2 = y + diagram.vs / 2.0D;
            double dx = diagram.vs / 3.464D;
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];
            xPoints[0] = (int) (x - dx);
            yPoints[0] = (int) y;
            xPoints[1] = (int) (x + dx);
            yPoints[1] = (int) y;
            xPoints[2] = (int) x2;
            yPoints[2] = (int) y2;
            g2d.drawPolygon(xPoints, yPoints, 3);

    }

    private void drawUpLine(Graphics g2d)
    {
        double w= this.bounds.width;
        double x = this.bounds.x+w/2.0D;
        double y = this.bounds.y;
        double x2 = x;
        double y2 =   this.parent.bounds.y+this.parent.bounds.height+diagram.vs/2.0D;
        g2d.setColor(nodestylemap.getColor("lineColor"));
        double lw = nodestylemap.getDouble("lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        g2d.drawLine((int) x, (int) y, (int) x2, (int) y2);
    }
}

