package org.lerot.myELH;

import java.util.HashMap;

public class StyleMap extends HashMap<String, NodeStyle>
{
    private static final long serialVersionUID = 1L;
    //public styleMap docstylemap;

    public static StyleMap makedefault()
    {
        StyleMap astylemap = new StyleMap();
        astylemap.put("default", NodeStyle.makeDefaultNodeStyle());
        astylemap.put("entity", NodeStyle.makeDefaultNodeStyle());

        return astylemap;
    }


    public NodeStyle getStyleMap(String stylename)
    {
        NodeStyle stylemap = this.get(stylename);
        if(stylemap == null)
        {
            System.out.println( "Not found style "+stylename);
            stylemap = this.get("default");
        }
        if(stylemap == null)
        {
            System.out.println( "Not found fdefault style ");
        }
        return stylemap;
    }

    public void include(StyleMap instylemap)
    {
        for(Entry<String, NodeStyle> amap : instylemap.entrySet())
        {
            String key = amap.getKey();
            NodeStyle anodestyle = amap.getValue();
            this.put(key,anodestyle);
        }
    }

    public NodeStyle getNodeStyle(String key)
    {
        NodeStyle anodestyle = new NodeStyle();
        anodestyle.include(get("default"))  ;
        anodestyle.include(get(key));
        return anodestyle;
    }
}
