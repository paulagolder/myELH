package org.lerot.myELH;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Vector;

public class elhDrawNode
{
    static svgdoc view;
    static String layoutstyle;
    private static int columncounter;
    public double anchor;
    int row;
    double col;
    String text = "deftext";
    String elhtype;
    Vector<elhDrawNode> children;
    nodeStyle nodestylemap;
    ShapeD bounds;
    private String childgrouptype;
    private elhDrawNode parent;
    private String stylename;
    private double symbolshift=10;

    public elhDrawNode(svgdoc asvgdoc, String layout)
    {
        // stylemap = astylemap;
        view = asvgdoc;
        layoutstyle = layout;
        bounds = new ShapeD();
    }

    public elhDrawNode(elhnode anode, int r, int maxrow)
    {
        bounds = new ShapeD();
        this.parent = null;
        this.children = new Vector<>();
        this.row = r;
        this.col = 1.0D;
        this.text = anode.getName();
        this.childgrouptype = anode.getChildgrouptype();
        this.elhtype = anode.getClass().getSimpleName();
        this.stylename = anode.getStylename();
        nodestylemap = view.docstylemap.getNodeStyle(elhtype);
        int count = anode.countChildren();
        if (r <= maxrow && count > 0)
        {
            for (elhnode acnode : anode.getChildren())
            {
                if (acnode != null)
                {
                    elhDrawNode newdnode = new elhDrawNode(acnode, r + 1, maxrow);
                    newdnode.setParent(this);
                    this.children.add(newdnode);
                }
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

    public void makeDrawNodeTree(elhnode anode, int r, int maxrow)
    {
        this.parent = null;
        this.children = new Vector<>();
        this.row = r;
        this.col = 1.0D;
        this.text = anode.getName();
        this.childgrouptype = anode.getChildgrouptype();
        this.elhtype = anode.getClass().getSimpleName();
        this.stylename = anode.getStylename();
        this.nodestylemap = view.docstylemap.getNodeStyle(stylename);
        int count = anode.countChildren();
        if (r <= maxrow && count > 0)
        {
            for (elhnode acnode : anode.getChildren())
            {
                if (acnode != null)
                {
                    elhDrawNode newdnode = new elhDrawNode(acnode, r + 1, maxrow);
                    newdnode.setParent(this);
                    this.children.add(newdnode);
                }
            }
        }
    }

    private void setParent(elhDrawNode adnode)
    {
        this.parent = adnode;
    }




    public boolean hasChildren()
    {
        return !this.children.isEmpty();
    }

    public Vector<elhDrawNode> getChildren()
    {
        return this.children;
    }






    String getChildgrouptype()
    {
        if (this.childgrouptype == null)
        {
            return "Sequence";
        }
        return this.childgrouptype;
    }

    void setChildgrouptype(String childgrouptype)
    {
        this.childgrouptype = childgrouptype;
    }

    public elhDrawNode getparent()
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

    public String getStylename()
    {
        if (this.stylename != null && !this.stylename.isEmpty())
        {
            return this.stylename;
        }
        return this.elhtype;
    }

    public void setStylename(String stylename)
    {
        this.stylename = stylename;
    }

    public boolean hasParent()
    {
        return (this.parent != null);
    }

    public String toString()
    {
        return this.text + " row =" + this.row + " col=" + this.col;
    }

    boolean containsPoint(double px, double py)
    {
        return bounds.containsPoint(px, py);
    }

    elhDrawNode getTarget(double px, double py)
    {
        if (hasChildren())
        {
            for (elhDrawNode adnode : this.children)
            {
                elhDrawNode fnode = adnode.getTarget(px, py);
                if (fnode != null) return fnode;
            }
        }
        if (containsPoint(px, py)) return this;
        return null;
    }

    public HashMap<String, Double> getDimension()
    {
        return bounds.getDimension();
    }

    public void drawborder(Graphics2D g2d, Color acolor, int width)
    {
        g2d.setStroke(new BasicStroke((float) width));
        g2d.setColor(acolor);
        bounds.drawRect(g2d);
        // g2d.drawRect((int)bounds.x, (int)bounds.y, (int)bounds.width, (int)bounds.height);
    }

    public void drawnodeborder(Graphics2D g2d)
    {
        Color acolor = nodestylemap.getColor("borderColor");
        int bw = (int) nodestylemap.getDouble("borderWidth");
        drawborder(g2d, acolor, bw);
    }

    public void drawbackgroundborder(Graphics2D g2d, int bw)
    {
        Color acolor = view.backgroundcolor;
        if (nodestylemap.getBoolean("fill"))
        {
            acolor = nodestylemap.getColor("fillColor");
        }
        drawborder(g2d, acolor, bw);
    }

    ShapeD DrawShape(Graphics2D g2d)
    {
        double w = bounds.width;
        double h = bounds.height;
        g2d.setFont(view.docfont);
        String[] ftext = formatLabel(text, g2d, w);
        FontMetrics fm = g2d.getFontMetrics();
        Dimension r = sizeString(ftext, g2d, view.docfont);
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
        drawString(ftext, g2d, view.docfont, anchorx, anchory);
        g2d.setColor(nodestylemap.getColor("borderColor"));
        drawnodeborder(g2d);
        if (nodestylemap.getBoolean("underlinetitle"))
        {
            g2d.drawLine((int) x, (int) (y + r.height), (int) (x + w), (int) (y + r.height));
        }
        return bounds;
    }

   /* ShapeD xDrawShape(Graphics2D g2d)
    {
        double w = nodestylemap.getDouble("rwidth");
        double h = nodestylemap.getDouble("rheight");
        g2d.setFont(view.docfont);
        String[] ftext = formatLabel(text, g2d, w);
        FontMetrics fm = g2d.getFontMetrics();
        Dimension r = sizeString(ftext, g2d, view.docfont);
        if (nodestylemap.getBoolean("canexpand") && r.getWidth() > w)
        {
            w = r.getWidth() + 8.0D;
        }
     //   double x = view.originx + ((col - view.columnoffset) * (w + view.hs) - w) / 2.0D;
   //     double y = view.rowtopy[row];
        double anchorx = x + w / 2.0D;
        double anchory = y;
        if (!nodestylemap.getBoolean("textattop"))
        {
            anchory = y + h / 2.0D - fm.getAscent();
        }
        bounds.setShape(x, y, w, h);
        ShapeI shapei = bounds.getShapeI();
        if (nodestylemap.getBoolean("fill"))
        {
            g2d.setColor(nodestylemap.getColor("fillColor"));
            bounds.fillRect(g2d);
        }
        g2d.setColor(nodestylemap.getColor("textColor"));
        drawString(ftext, g2d, view.docfont, anchorx, anchory);
        g2d.setColor(nodestylemap.getColor("borderColor"));
        drawnodeborder(g2d);
        if (nodestylemap.getBoolean("underlinetitle"))
        {
            g2d.drawLine((int) x, (int) (y + r.height), (int) (x + w), (int) (y + r.height));
        }
        return bounds;
    }*/


    HashMap<String, Double> getNodeSize(Graphics2D g2d)
    {
        double w = nodestylemap.getDouble("rwidth");
        double h = nodestylemap.getDouble("rheight");
        g2d.setFont(view.docfont);
        String[] ftext = formatLabel(text, g2d, w);
        FontMetrics fm = g2d.getFontMetrics();
        Dimension d = sizeString(ftext, g2d, view.docfont);
        if (nodestylemap.getBoolean("canexpand") && d.getWidth() > w)
        {
            w = d.getWidth() + 8.0D;
        }
        bounds.setDimension(w, h);
        return bounds.getDimension();
    }

    void drawNode(Graphics g2d)
    {
        elhDrawNode adnode = this;
        adnode.DrawShape((Graphics2D) g2d);
        adnode.drawSymbols(g2d);
        if (adnode.hasParent())
        {
            drawUpLine(g2d);
        }
        if (layoutstyle.equalsIgnoreCase("jackson")) drawJacksonSymbols(g2d);
        if (adnode.hasChildren())
        {
            if (!layoutstyle.equalsIgnoreCase("simple")) drawTriangle(g2d);
            if (layoutstyle.equalsIgnoreCase("jackson")||layoutstyle.equalsIgnoreCase("simple")) drawDownLine(g2d);
            else drawSymbols(g2d);
            drawHorizontalLine(g2d);
            for (elhDrawNode adrawnode : adnode.children)
            {

                adrawnode.drawNode(g2d);
            }
        }
    }



    private void drawDownLine(Graphics g2d)
    {

        double h = bounds.height;
        double  w= bounds.width;
        double vs =  view.vs;
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
        Vector<elhDrawNode> kids = this.getChildren();
        elhDrawNode firstchild = kids.getFirst();
        elhDrawNode lastchild = kids.getLast();
        double x = firstchild.bounds.x + firstchild.bounds.width/2.0D;
        double x2 = lastchild.bounds.x + lastchild.bounds.width/2.0D;
        double y =  this.bounds.y + this.bounds.height+view.vs/2.0D;
        g2d.setColor(nodestylemap.getColor("lineColor"));
        g2d.drawLine((int) x, (int) y, (int) x2, (int) y);
    }

    private void drawParallellLines(Graphics g2d)
    {
        double lw = nodestylemap.getDouble("lineWidth");
        int dy = 6;
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        Vector<elhDrawNode> kids = this.getChildren();
        elhDrawNode firstchild = kids.getFirst();
        elhDrawNode lastchild = kids.getLast();
        double x = firstchild.bounds.x + firstchild.bounds.width/2.0D;
        double x2 = lastchild.bounds.x + lastchild.bounds.width/2.0D;
        double y =  this.bounds.y + this.bounds.height+view.vs/2.0D;
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
        //{"sequence", "option", "repetition", "rolegroup"};
        String action = this.getChildgrouptype().toLowerCase();
        switch (action)
        {
            case "repetition":
                break;
            case "option":
                break;
            case "rolegroup":
                drawJacksonLine(g2d);
                break;
            case "sequence":
                break;
        }
        if (this.getparent() == null)
        {
            return;
        }
        String action2 = this.getparent().getChildgrouptype().toLowerCase();
        switch (action2)
        {
            case "repetition":
                drawJacksonSymbol(g2d, "*");
                break;
            case "option":
                drawJacksonSymbol(g2d, "O");
                break;
            case "rolegroup":
                break;
            case "sequence":
                break;
        }
    }

    private void drawSymbols(Graphics g2d)
    {
        g2d.setColor(Color.BLACK);
        String type = this.getChildgrouptype().toLowerCase();
        switch (type)
        {
            case "repetition":
                drawSymbolStar(g2d);
                break;
            case "option":
                drawSymbolCircle(g2d);
                break;
            case "rolegroup":
                drawParallellLines(g2d);
                break;
            case "sequence":
                break;
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

    private void drawSymbolString(String text, Graphics g2d, Font afont, double x, double y)
    {
        g2d.setFont(afont);
        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(text, g2d);
        double sx = x - r.getWidth() / 2.0D;
        double sy = y - r.getHeight() / 2.0D;
        g2d.drawString(text, (int) sx, (int) sy);
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
        elhDrawNode adnode = this;
        String stylename = getStylename();
        nodeStyle nodestylemap = view.docstylemap.getNodeStyle(stylename);
        g2d.setColor(nodestylemap.getColor("lineColor"));
        double lw = nodestylemap.getDouble("lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        double x = bounds.x + bounds.width/2;
        double y = bounds.y+bounds.height;

            double x2 = x;
            double y2 = y + view.vs / 2.0D;
            double dx = view.vs / 3.464D;
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
        double y2 =   this.parent.bounds.y+this.parent.bounds.height+view.vs/2.0D;
        g2d.setColor(nodestylemap.getColor("lineColor"));
        double lw = nodestylemap.getDouble("lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        g2d.drawLine((int) x, (int) y, (int) x2, (int) y2);
    }
}


