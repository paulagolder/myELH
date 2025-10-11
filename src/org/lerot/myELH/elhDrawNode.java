package org.lerot.myELH;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Vector;
import java.awt.BasicStroke;

//import static org.lerot.myELH.svgdoc.currentstylemap;

public class elhDrawNode
{
    private static int columncounter;
   // private static styleMaps currentstylemaps;
    int row;
    double col;
    String text="deftext";
    String elhtype;
    double width;
    double height;
    double x = 0.0D;
    double y = 0.0D;
    Vector<elhDrawNode> children;
    double mincol;
    double maxcol;
    int maxrow;
    private String childgrouptype;
    private elhDrawNode parent;
    private String stylename;
    nodeStyle style;
    static svgdoc view;
    static styleMap stylemap;
    nodeStyle nodestylemap;
    static String layoutstyle;

    public elhDrawNode(styleMap astylemap,svgdoc asvgdoc,String layout)
    {
       stylemap = astylemap;
       view = asvgdoc;
       layoutstyle = layout;
    }

    public void makeDrawNodeTree(elhnode anode, int r, int maxrow)
    {
        this.parent = null;
        this.children = new Vector<>();
        this.row = r;
        this.col = 1.0D;
        this.mincol = 1000.0D;
        this.maxcol = 0.0D;
        this.text = anode.getName();
        this.childgrouptype = anode.getChildgrouptype();
        this.elhtype = anode.getClass().getSimpleName();
        this.stylename = anode.getStylename();
        this.nodestylemap = stylemap.getNodeStyle(stylename);
        int count = anode.countChildren();
        if (r <= maxrow && count > 0)
        {
            for (elhnode acnode : anode.getChildren())
            {
                if (acnode instanceof elhnode)
                {
                    elhDrawNode newdnode = new elhDrawNode(acnode, r + 1, maxrow);
                    newdnode.setParent(this);
                    this.children.add(newdnode);
                }
            }
        }
    }



    public elhDrawNode(elhnode anode, int r, int maxrow)
    {
        this.parent = null;
        this.children = new Vector<>();
        this.row = r;
        this.col = 1.0D;
        this.mincol = 1000.0D;
        this.maxcol = 0.0D;
        this.text = anode.getName();
        this.childgrouptype = anode.getChildgrouptype();
        this.elhtype = anode.getClass().getSimpleName();
        this.stylename = anode.getStylename();
        nodestylemap = stylemap.getNodeStyle(elhtype);
        int count = anode.countChildren();
        if (r <= maxrow && count > 0)
        {
            for (elhnode acnode : anode.getChildren())
            {
                if (acnode instanceof elhnode)
                {
                    elhDrawNode newdnode = new elhDrawNode(acnode, r + 1, maxrow);
                    newdnode.setParent(this);
                    this.children.add(newdnode);
                }
            }
        }
    }

 /*   public static void setNodeStyle(svgdoc aview,styleMaps defstyles)
    {
        currentstylemaps = defstyles;
        view= aview;
    }*/

    private void setParent(elhDrawNode adnode)
    {
        this.parent = adnode;
    }

    public void updatetree()
    {
        this.mincol = 1000.0D;
        this.maxcol = 0.0D;
        this.col = 1.0D;
        this.row = 0;
        columncounter = 1;
        for (elhDrawNode adnode : this.children)
        {
            adnode.updateNode(this.row + 1);
            if (adnode.col < this.mincol)
            {
                this.mincol = adnode.col;
            }
            if (adnode.col > this.maxcol)
            {
                this.maxcol = adnode.col;
            }
        }
        this.col = (this.mincol + this.maxcol) / 2.0D;
        if (this.children.size() < 2)
        {
            this.mincol = this.col;
            this.maxcol = this.col;
        }
    }

    private void updateNode(int nrow)
    {
        this.mincol = 1000.0D;
        this.maxcol = 0.0D;
        this.row = nrow;
        if (this.children.isEmpty())
        {
            this.col = columncounter;
            this.mincol = columncounter;
            this.maxcol = columncounter;
            columncounter += 2;
        } else
        {
            for (elhDrawNode adnode : this.children)
            {
                adnode.updateNode(this.row + 1);
                if (adnode.col < this.mincol)
                {
                    this.mincol = adnode.col;
                }
                if (adnode.col > this.maxcol)
                {
                    this.maxcol = adnode.col;
                }
            }
            this.col = (this.mincol + this.maxcol) / 2.0D;
            if (this.children.size() < 2)
            {
                this.mincol = this.col;
                this.maxcol = this.col;
            }
        }
    }

    public boolean hasChildren()
    {
        return !this.children.isEmpty();
    }

    public Vector<elhDrawNode> getChildren()
    {
        return this.children;
    }

    public double getminchildcol()
    {
        double acol = this.col;
        for (elhDrawNode adnode : this.children)
        {
            double cmincol = adnode.getminchildcol();
            if (cmincol < acol)
            {
                acol = cmincol;
            }
        }
        return acol;
    }

    public double getmaxchildcol()
    {
        double acol = this.maxcol;
        for (elhDrawNode adnode : this.children)
        {
            double cmaxcol = adnode.getmaxchildcol();
            if (cmaxcol > acol)
            {
                acol = cmaxcol;
            }
        }
        return acol;
    }

    public int getmaxchildRow()
    {
        int mrow = this.row;
        if (hasChildren())
        {
            for (elhDrawNode adnode : this.children)
            {
                int cmaxrow = adnode.getmaxchildRow();
                if (cmaxrow > mrow)
                {
                    mrow = cmaxrow;
                }
            }
        }
        return mrow;
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

    public void setDimension(double x2, double y2, double w, double h)
    {
        this.x = x2;
        this.y = y2;
        this.width = w;
        this.height = h;
    }

    boolean containsPoint(double px, double py)
    {
        return px < this.x + this.width && px > this.x && py < this.y + this.height && py > this.y;
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

    public Dimension getDimension()
    {
        return new Dimension((int) this.width, (int) this.height);
    }

    public Rectangle getBounds()
    {
        return new Rectangle((int) this.x, (int) this.y, (int) this.width, (int) this.height);
    }

    public void drawborder(svgview thisview, Color color, int width)
    {
        Rectangle position = getBounds();
        System.out.println("drawing border" + this + position);
        Graphics graphics = thisview.canvas.getGraphics();
        graphics.setColor(color);
        BasicStroke stroke = new BasicStroke(10);
        Graphics2D g2 = (Graphics2D) graphics;
        g2.setStroke(new BasicStroke(3));
        graphics.drawRect(position.x + 1, position.y + 1, position.width, position.height);
        graphics.dispose();
    }


    void DrawShape(Graphics g2d)
    {

        double w = nodestylemap.getDouble( "rwidth");
        double h = nodestylemap.getDouble( "rheight");
        g2d.setFont(view.docfont);
        String[] ftext = formatLabel(text, g2d, w);
        FontMetrics fm = g2d.getFontMetrics();
        Dimension r = sizeString(ftext, g2d, view.docfont);
        if (nodestylemap.getBoolean("canexpand") && r.getWidth() > w)
        {
            w = r.getWidth() + 8.0D;
        }
        double x = view.originx + ((col - view.columnoffset) * (w + view.hs) - w) / 2.0D;
        double y = view.rowtopy[row];
        double anchorx = x + w / 2.0D;
        double anchory = y;
        if (!nodestylemap.getBoolean("textattop"))
        {
            anchory = y + h / 2.0D - fm.getAscent();
        }
        if (nodestylemap.getBoolean("fill"))
        {
            g2d.setColor(nodestylemap.getColor( "fillColor"));
            g2d.fillRect((int) x, (int) y, (int) w, (int) h);
        }
        double bw = nodestylemap.getDouble("borderWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) bw));
        g2d.setColor(nodestylemap.getColor("borderColor"));
        g2d.drawRect((int) x, (int) y, (int) w, (int) h);
        g2d.setColor(nodestylemap.getColor( "textColor"));
        drawString(ftext, g2d, view.docfont, anchorx, anchory);
        if (nodestylemap.getBoolean("underlinetitle"))
        {
            g2d.setColor(nodestylemap.getColor( "borderColor"));
            g2d.drawLine((int) x, (int) (y + r.height), (int) (x + w), (int) (y + r.height));
        }
        setDimension(x, y, w, h);
    }


    private static Dimension sizeString(String[] label, Graphics g2d, Font afont)
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

    private static String[] formatLabel(String text, Graphics g2d, double w)
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


    void drawNode(Graphics g2d)
    {
        elhDrawNode adnode = this;
        adnode.DrawShape(g2d);
        if (adnode.hasParent())
        {
            drawUpLine(g2d);
        }
        if(layoutstyle.equalsIgnoreCase("jackson") )drawJacksonSymbols(g2d, adnode);
        if (adnode.hasChildren())
        {
            if(layoutstyle.equalsIgnoreCase("jackson") ) drawDownLine(g2d);
            else drawTypeSymbols(g2d);
            drawHorizontalLine(g2d);
            for (elhDrawNode adrawnode : adnode.children)
            {
                adrawnode.drawNode(g2d);
            }
        }
    }

    void xdrawJacksonNode(Graphics g2d, elhDrawNode adnode)
    {
        adnode.DrawShape(g2d);
        if (adnode.hasParent())
        {
            drawUpLine(g2d);
        }
        drawJacksonSymbols(g2d, adnode);
        if (adnode.hasChildren())
        {
            drawDownLine(g2d);
            drawHorizontalLine(g2d);
            for (elhDrawNode adrawnode : adnode.children)
            {
                xdrawJacksonNode(g2d, adrawnode);
            }
        }
    }

    private void drawDownLine(Graphics g2d)
    {
        elhDrawNode adnode = this;
        String stylename = adnode.getStylename();
        double w = nodestylemap.getDouble( "rwidth");
        double h = nodestylemap.getDouble( "rheight");
        double vs = nodestylemap.getDouble( "vspace");
        double hs = nodestylemap.getDouble( "hspace");
        double lw = nodestylemap.getDouble( "lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        double x = view.originx + ((adnode.col - view.columnoffset) * (w + hs) - w) / 2.0D + w / 2.0D;
        double y = view.rowtopy[adnode.row] + h;
        double x2 = x;
        double y2 = y + vs / 2.0D;
        g2d.setColor(nodestylemap.getColor( "lineColor"));
        g2d.drawLine((int) x, (int) y, (int) x2, (int) y2);
    }

    private void drawHorizontalLine(Graphics g2d)
    {
        elhDrawNode adnode = this;
        String stylename = adnode.getStylename();
        double w = nodestylemap.getDouble( "rwidth");
        double h = nodestylemap.getDouble( "rheight");
        double lw = nodestylemap.getDouble( "lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        double x = view.originx + ((adnode.mincol - view.columnoffset) * (w + view.hs) - w) / 2.0D + w / 2.0D;
        double y = view.rowtopy[adnode.row] + h + view.vs / 2.0D;
        double x2 = view.originx + ((adnode.maxcol - view.columnoffset) * (w + view.hs) - w) / 2.0D + w / 2.0D;
        double y2 = y;
        g2d.setColor(nodestylemap.getColor( "lineColor"));
        g2d.drawLine((int) x, (int) y, (int) x2, (int) y2);
    }

    private void drawJacksonLine(Graphics g2d, elhDrawNode adnode)
    {
        String stylename = adnode.getStylename();
        double w = nodestylemap.getDouble( "rwidth");
        double h = nodestylemap.getDouble( "rheight");
        double lw = nodestylemap.getDouble( "lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        double x = view.originx + ((adnode.mincol - view.columnoffset) * (w + view.hs) - w) / 2.0D + w / 2.0D;
        double y = view.rowtopy[adnode.row] + h +view.vs / 2.0D;
        y += 5.0D;
        double x2 = view.originx + ((adnode.maxcol - view.columnoffset) * (w + view.hs) - w) / 2.0D + w / 2.0D;
        double y2 = y;
        g2d.setColor(nodestylemap.getColor( "lineColor"));
        g2d.drawLine((int) x, (int) y, (int) x2, (int) y2);
    }

    private void drawJacksonSymbol(Graphics g2d, elhDrawNode adnode, String text)
    {
        String stylename = adnode.getStylename();
        double w = nodestylemap.getDouble( "rwidth");
        double x = view.originx + ((adnode.col - view.columnoffset) * (w + view.hs) - w) / 2.0D + w;
        double y = view.rowtopy[adnode.row];
        FontMetrics fm = g2d.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(text, g2d);
        int sx = (int) (x - r.getWidth());
        double sy = y + fm.getAscent();
        g2d.drawString(text, sx, (int) sy);
    }

    private void drawJacksonSymbols(Graphics g2d, elhDrawNode adnode)
    {
        //{"sequence", "option", "repetition", "rolegroup"};
        String action = adnode.getChildgrouptype().toLowerCase();
        switch (action)
        {
            case "repetition":
                break;
            case "option":
                break;
            case "rolegroup":
                drawJacksonLine(g2d, adnode);
                break;
            case "sequence":
                break;
        }
        if (adnode.getparent() == null)
        {
            return;
        }
        String action2 = adnode.getparent().getChildgrouptype().toLowerCase();
        switch (action2)
        {
            case "repetition":
                drawJacksonSymbol(g2d, adnode, "*");
                break;
            case "option":
                drawJacksonSymbol(g2d, adnode, "O");
                break;
            case "rolegroup":
                break;
            case "sequence":
                break;
        }
    }

    private void drawSymbols(Graphics g2d, elhDrawNode adnode, double xmid, double ymid, double r)
    {
        g2d.setColor(Color.BLACK);
        String type = adnode.getChildgrouptype().toLowerCase();
        switch (type)
        {
            case "repetition":
                drawSymbolStar(g2d, xmid, ymid, r);
                break;
            case "option":
                drawSymbolCircle(g2d, xmid, ymid, r + 2.0D);
                break;
            case "rolegroup":
                drawSymbolParallel(g2d, xmid, ymid, r);
                break;
            case "sequence":
                break;
        }
    }

    private void drawSymbolStar(Graphics g2d, double xmid, double ymid, double r)
    {
        double x1 = xmid;
        double y1 = ymid;
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

    private void drawSymbolParallel(Graphics g2d, double xmid, double ymid, double r)
    {
        double x1 = xmid - r;
        double y1 = ymid - 1.0D;
        double x2 = xmid + r;
        double y2 = y1;
        g2d.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
        x1 = xmid - r;
        y1 = ymid + 1.0D;
        x2 = xmid + r;
        y2 = y1;
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

    private void drawSymbolCircle(Graphics g2d, double x, double y, double r)
    {
        double sx = x - r / 2.0D + 1.0D;
        double sy = y - 2.0D;
        g2d.drawOval((int) sx, (int) sy, (int) r, (int) r);
    }

    private void drawTypeSymbols(Graphics g2d)
    {
        elhDrawNode adnode = this;
        String stylename = getStylename();
        nodeStyle nodestylemap = stylemap.getNodeStyle(stylename);
        double w = nodestylemap.getDouble( "rwidth");
        double h = nodestylemap.getDouble( "rheight");
        g2d.setColor(nodestylemap.getColor( "lineColor"));
        double lw = nodestylemap.getDouble( "lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        double x = view.originx + ((adnode.col - view.columnoffset) * (w + view.hs) - w) / 2.0D + w / 2.0D;
        double y = view.rowtopy[adnode.row] + h;
        if (adnode.countChildren() > 1)
        {
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
            double xmid = x;
            double r = view.vs / 4.0D * Math.sqrt(3.0D);
            double ymid = y + r;
            drawSymbols(g2d, adnode, xmid, ymid, r);
        } else
        {
            double x2 = x;
            double y2 = y + view.vs / 2.0D;
            g2d.drawLine((int) x, (int) y, (int) x2, (int) y2);
        }
    }

    private void drawUpLine(Graphics g2d)
    {
        elhDrawNode adnode = this;
        String stylename = adnode.getStylename();
        double w = nodestylemap.getDouble( "rwidth");
        double h = nodestylemap.getDouble( "rheight");
        double x = view.originx + ((adnode.col - view.columnoffset) * (w + view.hs) - w) / 2.0D + w / 2.0D;
        double y = view.rowtopy[adnode.row];
        double x2 = x;
        double y2 = y - view.vs / 2.0D;
        g2d.setColor(nodestylemap.getColor( "lineColor"));
        double lw = nodestylemap.getDouble( "lineWidth");
        ((Graphics2D) g2d).setStroke(new BasicStroke((float) lw));
        g2d.drawLine((int) x, (int) y, (int) x2, (int) y2);
    }


}


