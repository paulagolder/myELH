  package org.lerot.myELH;
  
  import java.util.HashMap;
  
  
  
  
  
  public class stylemaps
    extends HashMap<String, stylemap>
  {
    private static final long serialVersionUID = 1L;
    
    public nodestyle get(String mapkey, String elhtype) {
      stylemap astylemap = get(mapkey);
      if (astylemap == null) {
        astylemap = get("default");
      }
      if (astylemap == null) {
        return null;
      }
      nodestyle anodestyle = astylemap.get(elhtype);
      if (anodestyle == null) {
        anodestyle = astylemap.get("document");
      }
      if (anodestyle == null) {
        anodestyle = astylemap.get("default");
      }
      return anodestyle;
    }
  }


/* Location:              /home/paul/applications/myELH.jar!/org/lerot/myELH/stylemaps.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */