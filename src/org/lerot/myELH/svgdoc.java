  package org.lerot.myELH;
  
  import java.awt.BasicStroke;
  import java.awt.Color;
  import java.awt.Dimension;
  import java.awt.Font;
  import java.awt.FontMetrics;
  import java.awt.Graphics;
  import java.awt.Graphics2D;
  import java.awt.geom.Rectangle2D;
  import java.io.File;
  import java.io.FileNotFoundException;
  import java.io.FileOutputStream;
  import java.io.IOException;
  import java.io.OutputStream;
  import java.io.OutputStreamWriter;
  import java.io.UnsupportedEncodingException;
  import java.io.Writer;
  import java.nio.charset.StandardCharsets;
  import java.util.HashMap;
  import java.util.Map;
  import javax.swing.JPanel;
  import org.apache.batik.dom.GenericDOMImplementation;
  import org.apache.batik.svggen.SVGGraphics2D;
  import org.apache.batik.svggen.SVGGraphics2DIOException;
  import org.w3c.dom.DOMImplementation;
  import org.w3c.dom.Document;
  
  
  
  
  
  
  public class svgdoc
    extends JPanel
  {
    private static final long serialVersionUID = 1L;
    elhdrawnode topdrawnode;
    String docstyle;
    private double columnoffset;
    private int originx;
    int width;
    int height;
    public SVGGraphics2D svgGenerator;
    public Document document;
    public Graphics2D g;
    private Font docfont;
    private int[] rowtopy;
    private double vs;
    private double hs;
    private double rowmaxy;
    private static stylemap docstylemap;
    private static Map<Integer, Double> columns;
    private static Map<Integer, Double> rows;
    
    public svgdoc() {
      this.docstyle = "default";
      this.originx = 0;
      this.width = 0;
      this.height = 0;
      setstyles();
      setBackground(Color.magenta);
     // DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
   //   String svgNS = "http://www.w3.org/2000/svg";
     // this.document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
       DOMImplementation domImpl =  GenericDOMImplementation.getDOMImplementation();

    // Create an instance of org.w3c.dom.Document.
    String svgNS = "http://www.w3.org/2000/svg";
    Document document = domImpl.createDocument(svgNS, "svg", null);
    this.svgGenerator = new SVGGraphics2D(this.document);
    }
    
    private void drawNode(Graphics g2d, elhdrawnode adnode) {
      DrawShape(g2d, adnode);
      if (adnode.hasParent()) {
        drawUpLine(g2d, adnode);
      }
      if (adnode.hasChildren()) {
        drawTypeSymbols(g2d, adnode);
        drawHorizontalLine(g2d, adnode);
        for (elhdrawnode adrawnode : adnode.children) {
          drawNode(g2d, adrawnode);
        }
      } 
    }
    
    private void drawJacksonNode(Graphics g2d, elhdrawnode adnode) {
      DrawShape(g2d, adnode);
      if (adnode.hasParent()) {
        drawUpLine(g2d, adnode);
      }
      drawJacksonSymbols(g2d, adnode);
      if (adnode.hasChildren()) {
        drawDownLine(g2d, adnode);
        drawHorizontalLine(g2d, adnode);
        for (elhdrawnode adrawnode : adnode.children) {
          drawJacksonNode(g2d, adrawnode);
        }
      } 
    }
    
    private void drawDownLine(Graphics g2d, elhdrawnode adnode) {
      String stylename = adnode.getStylename();
      double w = docstylemap.getDouble(stylename, "rwidth");
      double h = docstylemap.getDouble(stylename, "rheight");
      double vs = docstylemap.getDouble(stylename, "vspace");
      double hs = docstylemap.getDouble(stylename, "hspace");
      double lw = docstylemap.getDouble(stylename, "lineWidth");
      ((Graphics2D)g2d).setStroke(new BasicStroke((float)lw));
      double x = this.originx + ((adnode.col - this.columnoffset) * (w + hs) - w) / 2.0D + w / 2.0D;
      double y = this.rowtopy[adnode.row] + h;
      double x2 = x;
      double y2 = y + vs / 2.0D;
      g2d.setColor(docstylemap.getColor(stylename, "lineColor"));
      g2d.drawLine((int)x, (int)y, (int)x2, (int)y2);
    }
    
    private void drawHorizontalLine(Graphics g2d, elhdrawnode adnode) {
      String stylename = adnode.getStylename();
      double w = docstylemap.getDouble(stylename, "rwidth");
      double h = docstylemap.getDouble(stylename, "rheight");
      double lw = docstylemap.getDouble(stylename, "lineWidth");
      ((Graphics2D)g2d).setStroke(new BasicStroke((float)lw));
      double x = this.originx + ((adnode.mincol - this.columnoffset) * (w + this.hs) - w) / 2.0D + w / 2.0D;
      double y = this.rowtopy[adnode.row] + h + this.vs / 2.0D;
      double x2 = this.originx + ((adnode.maxcol - this.columnoffset) * (w + this.hs) - w) / 2.0D + w / 2.0D;
      double y2 = y;
      g2d.setColor(docstylemap.getColor(stylename, "lineColor"));
      g2d.drawLine((int)x, (int)y, (int)x2, (int)y2);
    }
    
    private void drawJacksonLine(Graphics g2d, elhdrawnode adnode) {
      String stylename = adnode.getStylename();
      double w = docstylemap.getDouble(stylename, "rwidth");
      double h = docstylemap.getDouble(stylename, "rheight");
      double lw = docstylemap.getDouble(stylename, "lineWidth");
      ((Graphics2D)g2d).setStroke(new BasicStroke((float)lw));
      double x = this.originx + ((adnode.mincol - this.columnoffset) * (w + this.hs) - w) / 2.0D + w / 2.0D;
      double y = this.rowtopy[adnode.row] + h + this.vs / 2.0D;
      y += 5.0D;
      double x2 = this.originx + ((adnode.maxcol - this.columnoffset) * (w + this.hs) - w) / 2.0D + w / 2.0D;
      double y2 = y;
      g2d.setColor(docstylemap.getColor(stylename, "lineColor"));
      g2d.drawLine((int)x, (int)y, (int)x2, (int)y2);
    }
    
    private void drawJacksonSymbol(Graphics g2d, elhdrawnode adnode, String text) {
      String stylename = adnode.getStylename();
      double w = docstylemap.getDouble(stylename, "rwidth");
      
      double x = this.originx + ((adnode.col - this.columnoffset) * (w + this.hs) - w) / 2.0D + w;
      double y = this.rowtopy[adnode.row];
      FontMetrics fm = g2d.getFontMetrics();
      Rectangle2D r = fm.getStringBounds(text, g2d);
      int sx = (int)(x - r.getWidth());
      double sy = y + fm.getAscent();
      g2d.drawString(text, sx, (int)sy);
    }
    
    private void drawJacksonSymbols(Graphics g2d, elhdrawnode adnode) {
      String action = adnode.getType().toLowerCase(); String str1;
      switch ((str1 = action).hashCode()) { case -1996165411: if (!str1.equals("iteration")); break;case -1715965556: if (!str1.equals("selection")); break;case 1171402247: if (!str1.equals("parallel")) {
            break;
          }
  
  
  
  
          
          drawJacksonLine(g2d, adnode);
          break;
  
        
        case 1349547969:
          if (!str1.equals("sequence"));
          break; }
  
      
      if (adnode.getparent() == null) {
        return;
      }
      String action2 = adnode.getparent().getType().toLowerCase(); String str2;
      switch ((str2 = action2).hashCode()) { case -1996165411: if (!str2.equals("iteration"))
            break; 
          drawJacksonSymbol(g2d, adnode, "*"); break;
        case -1715965556:
          if (!str2.equals("selection"))
            break; 
          drawJacksonSymbol(g2d, adnode, "O");
          break;
        case 1171402247:
          if (!str2.equals("parallel"));
          break;
        case 1349547969:
          if (!str2.equals("sequence"));
          break; }
    
    }
  
  
  
    
    private void drawSymbols(Graphics g2d, elhdrawnode adnode, double xmid, double ymid, double r) {
      g2d.setColor(Color.BLACK);
      String type = adnode.getType().toLowerCase(); String str1;
      switch ((str1 = type).hashCode()) { case -1996165411: if (!str1.equals("iteration"))
            break; 
          drawSymbolStar(g2d, xmid, ymid, r); break;
        case -1715965556:
          if (!str1.equals("selection"))
            break; 
          drawSymbolCircle(g2d, xmid, ymid, r + 2.0D); break;
        case 1171402247:
          if (!str1.equals("parallel"))
            break; 
          drawSymbolParallel(g2d, xmid, ymid, r);
          break;
        case 1349547969:
          if (!str1.equals("sequence"));
          break; }
    
    }
  
  
  
    
    private void drawSymbolStar(Graphics g2d, double xmid, double ymid, double r) {
      double x1 = xmid;
      double y1 = ymid;
      double x2 = x1 + r;
      double y2 = y1;
      double r2 = r * 0.7D;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
      x2 = x1 - r;
      y2 = y1;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
      x2 = x1;
      y2 = y1 - r;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
      x2 = x1;
      y2 = y1 + r;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
      x2 = x1 + r2;
      y2 = y1 + r2;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
      x2 = x1 - r2;
      y2 = y1 - r2;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
      x2 = x1 - r2;
      y2 = y1 + r2;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
      x2 = x1 + r2;
      y2 = y1 - r2;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
    }
    
    private void drawSymbolParallel(Graphics g2d, double xmid, double ymid, double r) {
      double x1 = xmid - r;
      double y1 = ymid - 1.0D;
      double x2 = xmid + r;
      double y2 = y1;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
      x1 = xmid - r;
      y1 = ymid + 1.0D;
      x2 = xmid + r;
      y2 = y1;
      g2d.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
    }
    
    private void DrawShape(Graphics g2d, elhdrawnode adnode) {
      String stylename = adnode.getStylename();
      double w = docstylemap.getDouble(stylename, "rwidth");
      double h = docstylemap.getDouble(stylename, "rheight");
      g2d.setFont(this.docfont);
      String[] text = formatLabel(adnode.text, g2d, w);
      FontMetrics fm = g2d.getFontMetrics();
      Dimension r = sizeString(text, g2d, this.docfont);
      if (docstylemap.getBoolean(stylename, "canexpand") && r.getWidth() > w) {
        w = r.getWidth() + 8.0D;
      }
      double x = this.originx + ((adnode.col - this.columnoffset) * (w + this.hs) - w) / 2.0D;
      
      double y = this.rowtopy[adnode.row];
      double anchorx = x + w / 2.0D;
      double anchory = y;
      if (!docstylemap.getBoolean(stylename, "textattop")) {
        anchory = y + h / 2.0D - fm.getAscent();
      }
      if (docstylemap.getBoolean(stylename, "fill")) {
        g2d.setColor(docstylemap.getColor(stylename, "fillColor"));
        g2d.fillRect((int)x, (int)y, (int)w, (int)h);
      } 
      double bw = docstylemap.getDouble(stylename, "borderWidth");
      ((Graphics2D)g2d).setStroke(new BasicStroke((float)bw));
      g2d.setColor(docstylemap.getColor(stylename, "borderColor"));
      g2d.drawRect((int)x, (int)y, (int)w, (int)h);
      g2d.setColor(docstylemap.getColor(stylename, "textColor"));
      drawString(text, g2d, this.docfont, anchorx, anchory);
      if (docstylemap.getBoolean(stylename, "underlinetitle")) {
        g2d.setColor(docstylemap.getColor(stylename, "borderColor"));
        g2d.drawLine((int)x, (int)(y + r.height), (int)(x + w), (int)(y + r.height));
      } 
      adnode.setDimension(x, y, w, h);
    }
    
    private void drawString(String[] label, Graphics g2d, Font afont, double x, double y) {
      g2d.setFont(afont);
      FontMetrics fm = g2d.getFontMetrics();
      double sy = y + fm.getAscent(); byte b; int i; String[] arrayOfString;
      for (i = (arrayOfString = label).length, b = 0; b < i; ) { String text = arrayOfString[b];
        Rectangle2D r = fm.getStringBounds(text, g2d);
        double sx = x - r.getWidth() / 2.0D;
        g2d.drawString(text, (int)sx, (int)sy);
        sy += r.getHeight();
        b++; }
    
    }
    private void drawSymbolString(String text, Graphics g2d, Font afont, double x, double y) {
      g2d.setFont(afont);
      FontMetrics fm = g2d.getFontMetrics();
      Rectangle2D r = fm.getStringBounds(text, g2d);
      double sx = x - r.getWidth() / 2.0D;
      double sy = y - r.getHeight() / 2.0D;
      g2d.drawString(text, (int)sx, (int)sy);
    }
    
    private void drawSymbolCircle(Graphics g2d, double x, double y, double r) {
      double sx = x - r / 2.0D + 1.0D;
      double sy = y - 2.0D;
      g2d.drawOval((int)sx, (int)sy, (int)r, (int)r);
    }
    
    private void drawTypeSymbols(Graphics g2d, elhdrawnode adnode) {
      String stylename = adnode.getStylename();
      double w = docstylemap.getDouble(stylename, "rwidth");
      double h = docstylemap.getDouble(stylename, "rheight");
      g2d.setColor(docstylemap.getColor(stylename, "lineColor"));
      double lw = docstylemap.getDouble(stylename, "lineWidth");
      ((Graphics2D)g2d).setStroke(new BasicStroke((float)lw));
      double x = this.originx + ((adnode.col - this.columnoffset) * (w + this.hs) - w) / 2.0D + w / 2.0D;
      double y = this.rowtopy[adnode.row] + h;
      if (adnode.countChildren() > 1) {
        double x2 = x;
        double y2 = y + this.vs / 2.0D;
        double dx = this.vs / 3.464D;
        int[] xPoints = new int[3];
        int[] yPoints = new int[3];
        xPoints[0] = (int)(x - dx);
        yPoints[0] = (int)y;
        xPoints[1] = (int)(x + dx);
        yPoints[1] = (int)y;
        xPoints[2] = (int)x2;
        yPoints[2] = (int)y2;
        g2d.drawPolygon(xPoints, yPoints, 3);
        double xmid = x;
        double r = this.vs / 4.0D * Math.sqrt(3.0D);
        double ymid = y + r;
        drawSymbols(g2d, adnode, xmid, ymid, r);
      } else {
        
        double x2 = x;
        double y2 = y + this.vs / 2.0D;
        g2d.drawLine((int)x, (int)y, (int)x2, (int)y2);
      } 
    }
    
    private void drawUpLine(Graphics g2d, elhdrawnode adnode) {
      String stylename = adnode.getStylename();
      double w = docstylemap.getDouble(stylename, "rwidth");
      double h = docstylemap.getDouble(stylename, "rheight");
      double x = this.originx + ((adnode.col - this.columnoffset) * (w + this.hs) - w) / 2.0D + w / 2.0D;
      double y = this.rowtopy[adnode.row];
      double x2 = x;
      double y2 = y - this.vs / 2.0D;
      g2d.setColor(docstylemap.getColor(stylename, "lineColor"));
      double lw = docstylemap.getDouble(stylename, "lineWidth");
      ((Graphics2D)g2d).setStroke(new BasicStroke((float)lw));
      g2d.drawLine((int)x, (int)y, (int)x2, (int)y2);
    }
    
    private String[] formatLabel(String text, Graphics g2d, double w) {
      FontMetrics fm = g2d.getFontMetrics();
      Rectangle2D r = fm.getStringBounds(text, g2d);
      if (r.getWidth() <= w) {
        String[] textarray = { text };
        return textarray;
      } 
      int targetwidth = (int)(w / r.getWidth() * text.length());
      String[] parts = text.split(" ");
      if (parts.length > 1) {
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
        
        String[] textarray2 = parts;
        return textarray2;
      } 
      String[] textarray3 = { text };
      return textarray3;
    }
    
    public dimensionp getExportSize() {
      updateRowsandColumns();
      return getExportSize(this.topdrawnode);
    }
    
    public dimensionp getExportSize(elhdrawnode adnode) {
      double cols = adnode.getmaxchildcol();
      int rows = adnode.getmaxchildRow();
      String stylename = adnode.getStylename();
      double rw = docstylemap.getDouble(stylename, "rwidth");
      double ch = docstylemap.getDouble(stylename, "rheight");
      double width = (cols + 1.0D) * (rw + this.hs);
      double height = this.rowmaxy;
      return new dimensionp(width, height);
    }
    
    public void output(File outfile) {
      dimensionp dim = getExportSize();
      boolean useCSS = true;
      try {
        OutputStream outputStream = new FileOutputStream(outfile);
        Writer out = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        this.svgGenerator.setSVGCanvasSize(new Dimension((int)dim.width, (int)dim.height));
        this.svgGenerator.stream(out, true);
        outputStream.flush();
        outputStream.close();
      }
      catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
      catch (SVGGraphics2DIOException e2) {
        e2.printStackTrace();
      }
      catch (FileNotFoundException e3) {
        e3.printStackTrace();
      }
      catch (IOException e4) {
        e4.printStackTrace();
      } 
    }
    
    public void paint(Graphics2D g2d) {
      this.g = g2d;
      this.originx = this.width / 2;
      this.columnoffset = this.topdrawnode.col;
      g2d.setColor(docstylemap.getColor("document", "fillColor"));
      int fs = docstylemap.getInteger("document", "textFontSize").intValue();
      g2d.setFont(this.docfont = new Font("SansSerif", 0, fs));
      g2d.setColor(docstylemap.getColor("document", "borderColor"));
      this.vs = docstylemap.getDouble("document", "vspace");
      this.hs = docstylemap.getDouble("document", "hspace");
      if (myELHgui.mframe.jacksonstyle) {
        drawJacksonNode(g2d, this.topdrawnode);
      } else {
        
        drawNode(g2d, this.topdrawnode);
      } 
    }
    
    public void setstyles() {
      stylemap astylemap = new stylemap();
      nodestyle docstyle = new nodestyle();
      docstyle.put("fill", Boolean.valueOf(false));
      docstyle.put("fillColor", Color.green);
      docstyle.put("lineColor", Color.black);
      astylemap.put("document", docstyle);
      nodestyle anodestyle = new nodestyle();
      anodestyle.put("textattop", Boolean.valueOf(false));
      anodestyle.put("rheight", Double.valueOf(40.0D));
      astylemap.put("event", anodestyle);
      nodestyle anodestyle2 = new nodestyle();
      anodestyle2.put("fill", Boolean.valueOf(false));
      anodestyle2.put("fillColor", Color.yellow);
      anodestyle2.put("textattop", Boolean.valueOf(true));
      anodestyle2.put("underlinetitle", Boolean.valueOf(true));
      astylemap.put("entity", anodestyle2);
      nodestyle rolestyle = new nodestyle();
      rolestyle.put("fill", Boolean.valueOf(true));
      rolestyle.put("fillColor", Color.pink);
      rolestyle.put("textattop", Boolean.valueOf(true));
      rolestyle.put("underlinetitle", Boolean.valueOf(true));
      astylemap.put("role", rolestyle);
      nodestyle rolegstyle = new nodestyle();
      rolegstyle.put("fill", Boolean.valueOf(true));
      rolegstyle.put("fillColor", Color.green);
      rolegstyle.put("textattop", Boolean.valueOf(true));
      rolegstyle.put("underlinetitle", Boolean.valueOf(true));
      astylemap.put("rolegroup", rolegstyle);
      nodestyle defstyle = nodestyle.getdefault();
      astylemap.put("default", defstyle);
      myELHgui.mframe.mystylemaps.put("default", astylemap);
    }
    
    public void setup(elhdrawnode topdrawnode2) {
      dimensionp d = getExportSize(this.topdrawnode);
      setup(this.topdrawnode, d.width, d.height);
    }
    
    public void setup(elhdrawnode adrawnode, double aw, double ah) {
      (this.topdrawnode = adrawnode).updatetree();
      this.rowtopy = new int[20];
      if (this.topdrawnode.getStylename() != null) {
        docstylemap = myELHgui.mframe.mystylemaps.get(this.topdrawnode.getStylename());
      }
      if (docstylemap == null) {
        docstylemap = myELHgui.mframe.mystylemaps.get("default");
      }
      updateRowsandColumns();
      this.width = (int)aw;
      this.height = (int)ah;
      setSize(this.width, this.height);
      paint(this.svgGenerator);
    }
    
    private Dimension sizeString(String[] label, Graphics g2d, Font afont) {
      g2d.setFont(afont);
      FontMetrics fm = g2d.getFontMetrics();
      Dimension lsize = new Dimension(0, 0);
      double h = 0.0D;
      double w = 0.0D; byte b; int i; String[] arrayOfString;
      for (i = (arrayOfString = label).length, b = 0; b < i; ) { String text = arrayOfString[b];
        Rectangle2D r = fm.getStringBounds(text, g2d);
        if (r.getWidth() > w) {
          w = r.getWidth();
        }
        h += r.getHeight(); b++; }
      
      lsize.width = (int)w;
      lsize.height = (int)h;
      return lsize;
    }
    
    public void updateRowsandColumns() {
      columns = new HashMap<>();
      rows = new HashMap<>();
      String stylename = this.topdrawnode.getStylename();
      double w = docstylemap.getDouble(stylename, "rwidth");
      double h = docstylemap.getDouble(stylename, "rheight");
      this.vs = docstylemap.getDouble("document", "vspace");
      this.hs = docstylemap.getDouble("document", "hspace");
      int c = (int)this.topdrawnode.col * 10;
      int r = this.topdrawnode.row;
      columns.put(Integer.valueOf(c), Double.valueOf(w));
      rows.put(Integer.valueOf(r), Double.valueOf(h));
      updateRowsandColumns(this.topdrawnode);
      int ty = (int)(this.vs / 2.0D);
      for (int i = 0; i < this.topdrawnode.getmaxchildRow() + 1; i++) {
        this.rowtopy[i] = ty;
        ty += (int)(rows.get(Integer.valueOf(i)).doubleValue() + this.vs);
      } 
      this.rowmaxy = ty;
    }
    
    public void updateRowsandColumns(elhdrawnode adnode) {
      String stylename = adnode.getStylename();
      double w = docstylemap.getDouble(stylename, "rwidth");
      double h = docstylemap.getDouble(stylename, "rheight");
      int c = (int)adnode.col * 10;
      int r = adnode.row;
      double maxw = 0.0D;
      if (columns.containsKey(Integer.valueOf(c))) {
        maxw = columns.get(Integer.valueOf(c)).doubleValue();
      }
      if (w > maxw) {
        columns.put(Integer.valueOf(c), Double.valueOf(w));
      }
      double maxr = 0.0D;
      if (rows.containsKey(Integer.valueOf(r))) {
        maxr = rows.get(Integer.valueOf(r)).doubleValue();
      }
      if (h > maxr) {
        rows.put(Integer.valueOf(r), Double.valueOf(h));
      }
      for (elhdrawnode cadnode : adnode.children)
        updateRowsandColumns(cadnode); 
    }
  }


/* Location:              /home/paul/applications/myELH.jar!/org/lerot/myELH/svgdoc.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */