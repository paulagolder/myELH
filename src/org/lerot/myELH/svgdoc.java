package org.lerot.myELH;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class svgdoc //extends JPanel
{
    private static final long serialVersionUID = 1L;
    public SVGGraphics2D svggraphic;
    public Document document;
    public Color backgroundcolor;
    Map<Integer, Double> columns;
    Map<Integer, Double> rows;
    styleMap docstylemap;
    elhDrawNode topdrawnode;
    String docstyle;
    int width;
    int height;
    int originx;
    Font docfont;
    double vs;
    double hs;
    double maxheight;
    double maxwidth;

    public svgdoc(String stylename)
    {
        this.docstyle = stylename;
        maxheight = 0.0;
        maxwidth = 0.0;
        this.originx = 0;
        this.width = 0;
        this.height = 0;
        docstylemap = myELHgui.mframe.stylemaps.getStyleMap(stylename);
        nodeStyle backgroundstyle = docstylemap.getNodeStyle("document");
        backgroundcolor = backgroundstyle.getColor("fillcolor");
        int fs = backgroundstyle.getInteger("textFontSize");
        docfont = new Font("SansSerif", Font.PLAIN, fs);
        this.vs = backgroundstyle.getDouble("vspace");
        this.hs = backgroundstyle.getDouble("hspace");
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        this.document = domImpl.createDocument(svgNS, "svg", null);
        this.svggraphic = new SVGGraphics2D(this.document);
        svggraphic.setColor(backgroundcolor);
    }

    public dimensionp createLayout(elhDrawNode adnode, double xanchor)
    {
        HashMap<String, Double> d0 = adnode.getNodeSize(svggraphic);
        adnode.bounds.setDimension(d0.get("width"), d0.get("height"));
        double width = 0.0;
        double height = 0.0;
        double topy = 0.0D;
        if (adnode.hasParent())
        {
            elhDrawNode parent = adnode.getparent();
            topy = parent.bounds.y + parent.bounds.height + vs;
        } else topy = vs;
        adnode.bounds.x = xanchor;
        adnode.bounds.y = topy;
        if (adnode.bounds.y + adnode.bounds.height + vs > maxheight)
            maxheight = adnode.bounds.y + adnode.bounds.height + vs;
        width = adnode.bounds.width;
        height = adnode.bounds.height;
        if (adnode.hasChildren())
        {
            if (adnode.getChildren().size() > 1)
            {
                width = -hs;
                Vector<elhDrawNode> children = adnode.children;
                for (elhDrawNode adrawnode : children)
                {
                    dimensionp d = createLayout(adrawnode, xanchor);
                    xanchor += d.width + hs;
                    width += d.width + hs;
                }
                adnode.bounds.x = (adnode.children.getFirst().bounds.x +adnode.children.getLast().bounds.x+adnode.children.getLast().bounds.width)/2-adnode.bounds.width/2;
            } else
            {
                dimensionp d = createLayout(adnode.children.getFirst(), xanchor);
                adnode.bounds.x = adnode.children.getFirst().bounds.x + adnode.children.getFirst().bounds.width / 2 - adnode.bounds.width / 2;
                width = d.width ;
            }
        }
        if (adnode.bounds.x + adnode.bounds.width + hs > maxwidth)  maxwidth = adnode.bounds.x + adnode.bounds.width + hs;
        return new dimensionp(width, height);
    }

    public void printsvg(File outfile)
    {
        createLayout(this.topdrawnode, vs);
        dimensionp dim = new dimensionp(maxwidth, maxheight);
        boolean useCSS = true;
        try
        {
            OutputStream outputStream = new FileOutputStream(outfile);
            Writer out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            this.svggraphic.setSVGCanvasSize(new Dimension((int) dim.width, (int) maxheight));
            this.svggraphic.stream(out, true);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e4)
        {
            e4.printStackTrace();
        }
    }

    public void inittree(elhDrawNode adrawnode, double aw, double ah)
    {
        this.topdrawnode = adrawnode;
        this.width = (int) aw;
        this.height = (int) ah;
        this.originx = this.width / 2;
        // this.topdrawnode.drawNode(svggraphic);
    }

    public void drawtree(elhDrawNode adrawnode)
    {
        adrawnode.drawNode(svggraphic);
    }

    public void xinittree(elhDrawNode adrawnode, double aw, double ah)
    {
        this.topdrawnode = adrawnode;
        this.width = (int) aw;
        this.height = (int) ah;
        this.originx = this.width / 2;
    }
}

