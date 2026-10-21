package org.lerot.myELH;

import java.awt.*;
import java.util.HashMap;

public class StyleMap extends HashMap<String, NodeStyle>
{
    private static final long serialVersionUID = 1L;
    //public styleMap docstylemap;

    public static StyleMap makedefault()
    {
        StyleMap astylemap = new StyleMap();
        astylemap.put("default", NodeStyle.makeDefaultNodeStyle());
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

    public static StyleMap makeStyles()
    {
        StyleMap astylemap = new StyleMap();
        NodeStyle docstyle = new NodeStyle();
        docstyle.put("fill", Boolean.FALSE);
        docstyle.put("fillColor", Color.green);
        docstyle.put("lineColor", Color.black);
        astylemap.put("document", docstyle);
        NodeStyle anodestyle = new NodeStyle();
        anodestyle.put("textattop", Boolean.FALSE);
        anodestyle.put("rheight", 40.0D);
        astylemap.put("event", anodestyle);
        NodeStyle anodestyle2 = new NodeStyle();
        anodestyle2.put("fill", Boolean.FALSE);
        anodestyle2.put("fillColor", Color.yellow);
        anodestyle2.put("textattop", Boolean.TRUE);
        anodestyle2.put("underlinetitle", Boolean.TRUE);
        astylemap.put("entity", anodestyle2);
        NodeStyle rolestyle = new NodeStyle();
        rolestyle.put("fill", Boolean.TRUE);
        rolestyle.put("fillColor", Color.pink);
        rolestyle.put("textattop", Boolean.TRUE);
        rolestyle.put("underlinetitle", Boolean.TRUE);
        astylemap.put("role", rolestyle);
        NodeStyle rolegstyle = new NodeStyle();
        rolegstyle.put("fill", Boolean.TRUE);
        rolegstyle.put("fillColor", Color.green);
        rolegstyle.put("textattop", Boolean.TRUE);
        rolegstyle.put("underlinetitle", Boolean.TRUE);
        astylemap.put("rolegroup", rolegstyle);
        NodeStyle defstyle = NodeStyle.makeDefaultNodeStyle();
        astylemap.put("default", defstyle);
        return astylemap;
    }
}