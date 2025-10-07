  package org.lerot.myELH;
  
  import java.awt.Color;
  import java.util.HashMap;
  
  
  
  
  
  public class nodestyle
    extends HashMap<String, Object>
  {
    private static final long serialVersionUID = 1L;
    
    public static nodestyle getdefault() {
      nodestyle anodestyle = new nodestyle();
      anodestyle.put("rwidth", Double.valueOf(80.0D));
      anodestyle.put("rwidth", Double.valueOf(80.0D));
      anodestyle.put("rheight", Double.valueOf(60.0D));
      anodestyle.put("borderColor", Color.gray);
      anodestyle.put("textColor", Color.black);
      anodestyle.put("textFontSize", Integer.valueOf(14));
      anodestyle.put("symbolFontSize", Integer.valueOf(2));
      anodestyle.put("symbolColor", Color.blue);
      anodestyle.put("lineColor", Color.red);
      anodestyle.put("lineWidth", Double.valueOf(1.0D));
      anodestyle.put("fillColor", Color.white);
      anodestyle.put("fill", Boolean.valueOf(false));
      anodestyle.put("borderWidth", Double.valueOf(2.0D));
      anodestyle.put("hspace", Double.valueOf(20.0D));
      anodestyle.put("vspace", Double.valueOf(30.0D));
      anodestyle.put("canexpand", Boolean.valueOf(true));
      anodestyle.put("textattop", Boolean.valueOf(false));
      anodestyle.put("underlinetitle", Boolean.valueOf(false));
      return anodestyle;
    }
    
    public double getDouble(String key) {
      return ((Double)get(key)).doubleValue();
    }
    
    public boolean getBoolean(String key) {
      return ((Boolean)get(key)).booleanValue();
    }
    
    public Color getColor(String key) {
      return (Color)get(key);
    }
    
    public Integer getInteger(String key) {
      return (Integer)get(key);
    }
  }


/* Location:              /home/paul/applications/myELH.jar!/org/lerot/myELH/nodestyle.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */