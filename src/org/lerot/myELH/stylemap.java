  package org.lerot.myELH;
  
  import java.awt.Color;
  import java.util.HashMap;
  
  
  
  public class stylemap
    extends HashMap<String, nodestyle>
  {
    private static final long serialVersionUID = 1L;
    
    public double getDouble(String elhtype, String key) {
      nodestyle astyle = get(elhtype);
      if (astyle != null) {
        if (astyle.containsKey(key)) {
          return astyle.getDouble(key);
        }
        astyle = get("document");
        if (astyle.containsKey(key)) {
          return astyle.getDouble(key);
        }
        astyle = get("default");
        if (astyle.containsKey(key)) {
          return astyle.getDouble(key);
        }
      } else {
        
        astyle = get("document");
        if (astyle.containsKey(key)) {
          return astyle.getDouble(key);
        }
        astyle = get("default");
        if (astyle.containsKey(key)) {
          return astyle.getDouble(key);
        }
      } 
      return 0.0D;
    }
    
    public boolean getBoolean(String elhtype, String key) {
      nodestyle astyle = get(elhtype);
      if (astyle != null) {
        if (astyle.containsKey(key)) {
          return astyle.getBoolean(key);
        }
        astyle = get("document");
        if (astyle.containsKey(key)) {
          return astyle.getBoolean(key);
        }
        astyle = get("default");
        if (astyle.containsKey(key)) {
          return astyle.getBoolean(key);
        }
      } else {
        
        astyle = get("document");
        if (astyle.containsKey(key)) {
          return astyle.getBoolean(key);
        }
        astyle = get("default");
        if (astyle.containsKey(key)) {
          return astyle.getBoolean(key);
        }
      } 
      return false;
    }
    
    public Integer getInteger(String elhtype, String key) {
      nodestyle astyle = get(elhtype);
      if (astyle != null) {
        if (astyle.containsKey(key)) {
          return astyle.getInteger(key);
        }
        astyle = get("document");
        if (astyle.containsKey(key)) {
          return astyle.getInteger(key);
        }
        astyle = get("default");
        if (astyle.containsKey(key)) {
          return astyle.getInteger(key);
        }
      } else {
        
        astyle = get("document");
        if (astyle.containsKey(key)) {
          return astyle.getInteger(key);
        }
        astyle = get("default");
        if (astyle.containsKey(key)) {
          return astyle.getInteger(key);
        }
      } 
      return Integer.valueOf(0);
    }
    
    public Color getColor(String elhtype, String key) {
      nodestyle astyle = get(elhtype);
      if (astyle != null) {
        if (astyle.containsKey(key)) {
          return astyle.getColor(key);
        }
        astyle = get("document");
        if (astyle.containsKey(key)) {
          return astyle.getColor(key);
        }
        astyle = get("default");
        if (astyle.containsKey(key)) {
          return astyle.getColor(key);
        }
      } else {
        
        astyle = get("document");
        if (astyle.containsKey(key)) {
          return astyle.getColor(key);
        }
        astyle = get("default");
        if (astyle.containsKey(key)) {
          return astyle.getColor(key);
        }
      } 
      return Color.BLACK;
    }
  }


/* Location:              /home/paul/applications/myELH.jar!/org/lerot/myELH/stylemap.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */