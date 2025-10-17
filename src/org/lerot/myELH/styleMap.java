package org.lerot.myELH;

import java.awt.*;
import java.util.HashMap;

public class styleMap extends HashMap<String, nodeStyle>
{
    private static final long serialVersionUID = 1L;
    //public styleMap docstylemap;

    public static styleMap makedefault()
    {
        styleMap astylemap = new styleMap();
        astylemap.put("default", nodeStyle.makeDefaultNodeStyle());
        return astylemap;
    }


    public nodeStyle getStyleMap(String stylename)
    {
        nodeStyle stylemap = this.get(stylename);
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

    public void include(styleMap instylemap)
    {
        for(Entry<String, nodeStyle> amap : instylemap.entrySet())
        {
            String key = amap.getKey();
            nodeStyle anodestyle = amap.getValue();
            this.put(key,anodestyle);
        }
    }

    public nodeStyle getNodeStyle(String key)
    {
        nodeStyle anodestyle = new nodeStyle();
        anodestyle.include(get("default"))  ;
        anodestyle.include(get(key));
        return anodestyle;
    }

    public static styleMap makeStyles()
    {
        styleMap astylemap = new styleMap();
        nodeStyle docstyle = new nodeStyle();
        docstyle.put("fill", Boolean.FALSE);
        docstyle.put("fillColor", Color.green);
        docstyle.put("lineColor", Color.black);
        astylemap.put("document", docstyle);
        nodeStyle anodestyle = new nodeStyle();
        anodestyle.put("textattop", Boolean.FALSE);
        anodestyle.put("rheight", 40.0D);
        astylemap.put("event", anodestyle);
        nodeStyle anodestyle2 = new nodeStyle();
        anodestyle2.put("fill", Boolean.FALSE);
        anodestyle2.put("fillColor", Color.yellow);
        anodestyle2.put("textattop", Boolean.TRUE);
        anodestyle2.put("underlinetitle", Boolean.TRUE);
        astylemap.put("entity", anodestyle2);
        nodeStyle rolestyle = new nodeStyle();
        rolestyle.put("fill", Boolean.TRUE);
        rolestyle.put("fillColor", Color.pink);
        rolestyle.put("textattop", Boolean.TRUE);
        rolestyle.put("underlinetitle", Boolean.TRUE);
        astylemap.put("role", rolestyle);
        nodeStyle rolegstyle = new nodeStyle();
        rolegstyle.put("fill", Boolean.TRUE);
        rolegstyle.put("fillColor", Color.green);
        rolegstyle.put("textattop", Boolean.TRUE);
        rolegstyle.put("underlinetitle", Boolean.TRUE);
        astylemap.put("rolegroup", rolegstyle);
        nodeStyle defstyle = nodeStyle.makeDefaultNodeStyle();
        astylemap.put("default", defstyle);
        return astylemap;
    }
}