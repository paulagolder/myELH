package org.lerot.myELH;

import java.awt.*;
import java.awt.image.BufferedImage;

// The diagram of an ELH tree: the boxes laid out once, ready to be painted
// on screen or into an SVG file. The style map decides how it looks (the
// screen and print styles differ); the layout style which symbols are drawn.
public final class ElhDiagram
{
    // how many levels below the top node are drawn
    static final int MAXDEPTH = 6;

    final StyleMap stylemap;
    final LayoutStyle layoutstyle;
    final Font docfont;
    final double vs;
    final double hs;
    final ElhDrawNode top;
    // the state indicators written under the actions, or null if they are not shown
    final StateIndicators states;
    private double maxwidth = 0.0;
    private double maxheight = 0.0;

    // lays the diagram out, measuring text with the given graphics
    public ElhDiagram(ElhNode root, StyleMap stylemap, LayoutStyle layoutstyle, Graphics2D measure)
    {
        this(root, stylemap, layoutstyle, measure, false);
    }

    // as above, writing each action's state indicator values under its box
    // (valid prior / sets to) if showstates is true
    public ElhDiagram(ElhNode root, StyleMap stylemap, LayoutStyle layoutstyle, Graphics2D measure,
            boolean showstates)
    {
        this.states = showstates ? StateIndicators.of(root) : null;
        this.stylemap = stylemap;
        this.layoutstyle = layoutstyle;
        NodeStyle documentstyle = stylemap.getNodeStyle("document");
        this.docfont = new Font("SansSerif", Font.PLAIN, documentstyle.getInteger("textFontSize"));
        this.vs = documentstyle.getDouble("vspace");
        this.hs = documentstyle.getDouble("hspace");
        this.top = new ElhDrawNode(this, root, null, 0);
        layout(this.top, this.hs, measure);
    }

    // lays the diagram out, measuring text as it would appear on screen
    public ElhDiagram(ElhNode root, StyleMap stylemap, LayoutStyle layoutstyle)
    {
        this(root, stylemap, layoutstyle, measuringGraphics());
    }

    public ElhDiagram(ElhNode root, StyleMap stylemap, LayoutStyle layoutstyle, boolean showstates)
    {
        this(root, stylemap, layoutstyle, measuringGraphics(), showstates);
    }

    private static Graphics2D measuringGraphics()
    {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }

    // virtually places adnode and everything below it, starting at xanchor; returns
    // the width the subtree takes up
    private double layout(ElhDrawNode adnode, double xanchor, Graphics2D g2d)
    {
        adnode.measure(g2d);
        double width;
        double topy;
        if (adnode.hasParent())
        {
            ElhDrawNode parent = adnode.getparent();
            topy = parent.bounds.y + parent.bounds.height + vs;
        } else topy = vs;
        adnode.bounds.x = xanchor;
        adnode.bounds.y = topy;
        if (adnode.bounds.y + adnode.bounds.height + vs > maxheight)
            maxheight = adnode.bounds.y + adnode.bounds.height + vs;
        width = adnode.bounds.width;
        if (adnode.hasChildren())
        {
            if (adnode.getChildren().size() > 1)
            {
                width = -hs;
                for (ElhDrawNode adrawnode : adnode.getChildren())
                {
                    double childwidth = layout(adrawnode, xanchor, g2d);
                    xanchor += childwidth + hs;
                    width += childwidth + hs;
                }
                ElhDrawNode first = adnode.getChildren().getFirst();
                ElhDrawNode last = adnode.getChildren().getLast();
                adnode.bounds.x = (first.bounds.x + last.bounds.x + last.bounds.width) / 2 - adnode.bounds.width / 2;
            } else
            {
                ElhDrawNode only = adnode.getChildren().getFirst();
                width = layout(only, xanchor, g2d);
                adnode.bounds.x = only.bounds.x + only.bounds.width / 2 - adnode.bounds.width / 2;
            }
        }
        if (adnode.bounds.x + adnode.bounds.width + hs > maxwidth)
            maxwidth = adnode.bounds.x + adnode.bounds.width + hs;
        return width;
    }

    public double getWidth()
    {
        return maxwidth;
    }

    public double getHeight()
    {
        return maxheight;
    }

    public void draw(Graphics2D g2d)
    {
        top.drawNode(g2d);
        // the blue frame round the whole diagram
        g2d.setStroke(new BasicStroke(4));
        g2d.setColor(Color.blue);
        g2d.drawRect(0, 0, (int) maxwidth, (int) maxheight);
    }

    // the box at (x, y), or null
    public ElhDrawNode nodeAt(double x, double y)
    {
        return top.getTarget(x, y);
    }

    // the box drawn for the given tree node, or null if it is not drawn
    public ElhDrawNode findNode(ElhNode source)
    {
        return top.find(source);
    }
}
