package org.lerot.myELH;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
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
    //   private  nodeStyle backgroundstyle;
    public SVGGraphics2D svggraphic;
    public Document document;
    //  private final nodeStyle docstylemap;
    public Color backgroundcolor;
    Map<Integer, Double> columns;
    Map<Integer, Double> rows;
    //public Graphics2D svggraphic;
    styleMap docstylemap;
    elhDrawNode topdrawnode;
    String docstyle;
    int width;
    int height;
    //double columnoffset;
    int originx;
    Font docfont;
    //int[] rowtopy;
    double vs;
    double hs;
    private double rowmaxy;

    public svgdoc(String stylename)
    {
        this.docstyle = stylename;
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

    public dimensionp xgetExportSize()
    {
        //updateRowsandColumns();
        return createLayout(this.topdrawnode, 0);
    }



    public dimensionp createLayout(elhDrawNode adnode, double xanchor)
    {
        HashMap<String, Double> d0 = adnode.getNodeSize(svggraphic);
        adnode.bounds.setDimension(d0.get("width"), d0.get("height"));
        dimensionp rectsize= getSize(adnode,0);
        double width = 0.0;
        double height = 0.0;
        double cumx = 0.0;
        double topy = 0.0D;
        if (adnode.hasParent())
        {
            elhDrawNode parent = adnode.getparent();
            topy = parent.bounds.y + parent.bounds.height + vs;
        }

        adnode.bounds.x = xanchor+rectsize.width/2-hs;
        adnode.bounds.y = topy;
        width = adnode.bounds.width;
        height = adnode.bounds.height;
        if (adnode.hasChildren())
        {
            double anchor = xanchor;
            width= -hs/2;
            Vector<elhDrawNode> children = adnode.children;
            for (elhDrawNode adrawnode : children)
            {
                dimensionp d = createLayout(adrawnode, anchor);
                anchor += d.width+hs;
                width +=  d.width+hs;
                if ( adrawnode.bounds.height + 2*vs > height) height = adrawnode.bounds.height + 2*vs;
            }


        }
        return new dimensionp(width, height);
    }
    public dimensionp getSize(elhDrawNode adnode, int row)
    {

        double width = 0.0;
        double height = 0.0;
        double cumheight;
        adnode.getNodeSize(svggraphic);
        width = adnode.bounds.width;
        cumheight = adnode.bounds.height;

        if (adnode.hasChildren())
        {
            width= -hs;
            height = 0.0;
            Vector<elhDrawNode> children = adnode.children;
            for (elhDrawNode adrawnode : children)
            {
                dimensionp d = getSize(adrawnode, row + 1);
                width = width + d.width + this.hs;
                if ( adrawnode.bounds.height + vs > height) height = adrawnode.bounds.height + vs;
            }
           cumheight += height;

        }
        return new dimensionp(width, cumheight);
    }

    public void output(File outfile)
    {
        dimensionp dim = createLayout(this.topdrawnode, 0);
        boolean useCSS = true;
        try
        {
            OutputStream outputStream = new FileOutputStream(outfile);
            Writer out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            this.svggraphic.setSVGCanvasSize(new Dimension((int) dim.width, (int) dim.height));
            this.svggraphic.stream(out, true);
            outputStream.flush();
            outputStream.close();
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        } catch (SVGGraphics2DIOException e2)
        {
            e2.printStackTrace();
        } catch (FileNotFoundException e3)
        {
            e3.printStackTrace();
        } catch (IOException e4)
        {
            e4.printStackTrace();
        }
    }

   /* public void drawTree(Graphics2D g2d)
    {
       this.svggraphic = g2d;
        this.originx = this.width / 2;
        this.columnoffset = this.topdrawnode.col;
        nodeStyle backgroundstyle = docstylemap.getNodeStyle("document");
        g2d.setColor(backgroundstyle.getColor("fillColor"));
        int fs = backgroundstyle.getInteger("textFontSize");
        docfont = new Font("SansSerif", 0, fs);
        this.vs = backgroundstyle.getDouble("vspace");
        this.hs = backgroundstyle.getDouble("hspace");
        this.topdrawnode.drawNode(g2d);
    }*/

    public void drawtree(elhDrawNode adrawnode, double aw, double ah)
    {
        this.topdrawnode = adrawnode;
        //   adrawnode.updatetree();
        //  this.rowtopy = new int[20];
        // updateRowsandColumns();
        this.width = (int) aw;
        this.height = (int) ah;
        this.originx = this.width / 2;
        //  this.columnoffset = this.topdrawnode.col;
        this.topdrawnode.drawNode(svggraphic);
    }

    public void drawtree2(elhDrawNode adrawnode)
    {
        this.topdrawnode.drawNode(svggraphic);
    }

    public void inittree(elhDrawNode adrawnode, double aw, double ah)
    {
        this.topdrawnode = adrawnode;
        this.width = (int) aw;
        this.height = (int) ah;
        this.originx = this.width / 2;
    }





}

