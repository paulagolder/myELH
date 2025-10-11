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

import static org.lerot.myELH.styleMaps.makeStyles;
//import static org.lerot.myELH.elhdrawnode.formatLabel;

public class svgdoc //extends JPanel
{
    private static final long serialVersionUID = 1L;
    private static styleMaps docstylemaps;
    static nodeStyle currentnodestyle;
    private static Map<Integer, Double> columns;
    private static Map<Integer, Double> rows;
    public SVGGraphics2D svggraphic;
    public Document document;
    public Graphics2D g;
    elhDrawNode topdrawnode;
    String docstyle;
    int width;
    int height;
    double columnoffset;
    int originx;
    Font docfont;
    int[] rowtopy;
    double vs;
    double hs;
    private double rowmaxy;

    public svgdoc()
    {
        docstylemaps = new styleMaps();
        docstylemaps.put("default", makeStyles());
        this.docstyle = "default";
        this.originx = 0;
        this.width = 0;
        this.height = 0;
        currentnodestyle = docstylemaps.get("default").get("default");
        //setBackground(Color.magenta);
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        String svgNS = "http://www.w3.org/2000/svg";
        this.document = domImpl.createDocument(svgNS, "svg", null);
        this.svggraphic = new SVGGraphics2D(this.document);
    }





    public dimensionp getExportSize()
    {
        updateRowsandColumns();
        return getExportSize(this.topdrawnode);
    }

    public dimensionp getExportSize(elhDrawNode adnode)
    {
        double cols = adnode.getmaxchildcol();
        int rows = adnode.getmaxchildRow();
        String stylename = adnode.getStylename();
        double rw = currentnodestyle.getDouble("rwidth");
        double ch = currentnodestyle.getDouble( "rheight");
        double width = (cols + 1.0D) * (rw + this.hs);
        double height = this.rowmaxy;
        return new dimensionp(width, height);
    }

    public void output(File outfile)
    {
        dimensionp dim = getExportSize();
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

    public void paint(Graphics2D g2d)
    {
        this.g = g2d;
        this.originx = this.width / 2;
        this.columnoffset = this.topdrawnode.col;
        g2d.setColor(currentnodestyle.getColor( "fillColor"));
        int fs = currentnodestyle.getInteger("textFontSize").intValue();
        g2d.setFont(this.docfont = new Font("SansSerif", 0, fs));
        g2d.setColor(currentnodestyle.getColor( "borderColor"));
        this.vs = currentnodestyle.getDouble( "vspace");
        this.hs = currentnodestyle.getDouble( "hspace");

            this.topdrawnode.drawNode(g2d);

    }

    public void setup(elhDrawNode topdrawnode2)
    {
        dimensionp d = getExportSize(this.topdrawnode);
        setup(this.topdrawnode, d.width, d.height);
    }

    public void setup(elhDrawNode adrawnode, double aw, double ah)
    {
        this.topdrawnode = adrawnode;
        adrawnode.updatetree();
        this.rowtopy = new int[20];
        updateRowsandColumns();
        this.width = (int) aw;
        this.height = (int) ah;
       // setSize(this.width, this.height);
        paint(this.svggraphic);
    }

    public void updateRowsandColumns()
    {
        columns = new HashMap<>();
        rows = new HashMap<>();
        String stylename = this.topdrawnode.getStylename();
        double w = currentnodestyle.getDouble( "rwidth");
        double h = currentnodestyle.getDouble( "rheight");
        this.vs = currentnodestyle.getDouble( "vspace");
        this.hs = currentnodestyle.getDouble("hspace");
        int c = (int) this.topdrawnode.col * 10;
        int r = this.topdrawnode.row;
        columns.put(Integer.valueOf(c), Double.valueOf(w));
        rows.put(Integer.valueOf(r), Double.valueOf(h));
        updateRowsandColumns(this.topdrawnode);
        int ty = (int) (this.vs / 2.0D);
        for (int i = 0; i < this.topdrawnode.getmaxchildRow() + 1; i++)
        {
            this.rowtopy[i] = ty;
            ty += (int) (rows.get(Integer.valueOf(i)).doubleValue() + this.vs);
        }
        this.rowmaxy = ty;
    }

    public void updateRowsandColumns(elhDrawNode adnode)
    {
        String stylename = adnode.getStylename();
        double w = currentnodestyle.getDouble( "rwidth");
        double h = currentnodestyle.getDouble( "rheight");
        int c = (int) adnode.col * 10;
        int r = adnode.row;
        double maxw = 0.0D;
        if (columns.containsKey(Integer.valueOf(c)))
        {
            maxw = columns.get(Integer.valueOf(c)).doubleValue();
        }
        if (w > maxw)
        {
            columns.put(Integer.valueOf(c), Double.valueOf(w));
        }
        double maxr = 0.0D;
        if (rows.containsKey(Integer.valueOf(r)))
        {
            maxr = rows.get(Integer.valueOf(r)).doubleValue();
        }
        if (h > maxr)
        {
            rows.put(Integer.valueOf(r), Double.valueOf(h));
        }
        for (elhDrawNode cadnode : adnode.children)
            updateRowsandColumns(cadnode);
    }
}

