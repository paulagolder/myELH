package org.lerot.myELH;

import java.awt.*;
import java.util.HashMap;

public class styleMap extends HashMap<String, nodeStyle>
{
    private static final long serialVersionUID = 1L;

    public static styleMap getdefault()
    {
        styleMap astylemap = new styleMap();
        astylemap.put("default", nodeStyle.getdefault());
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
        docstyle.put("fill", Boolean.valueOf(false));
        docstyle.put("fillColor", Color.green);
        docstyle.put("lineColor", Color.black);
        astylemap.put("document", docstyle);
        nodeStyle anodestyle = new nodeStyle();
        anodestyle.put("textattop", Boolean.valueOf(false));
        anodestyle.put("rheight", Double.valueOf(40.0D));
        astylemap.put("event", anodestyle);
        nodeStyle anodestyle2 = new nodeStyle();
        anodestyle2.put("fill", Boolean.valueOf(false));
        anodestyle2.put("fillColor", Color.yellow);
        anodestyle2.put("textattop", Boolean.valueOf(true));
        anodestyle2.put("underlinetitle", Boolean.valueOf(true));
        astylemap.put("entity", anodestyle2);
        nodeStyle rolestyle = new nodeStyle();
        rolestyle.put("fill", Boolean.valueOf(true));
        rolestyle.put("fillColor", Color.pink);
        rolestyle.put("textattop", Boolean.valueOf(true));
        rolestyle.put("underlinetitle", Boolean.valueOf(true));
        astylemap.put("role", rolestyle);
        nodeStyle rolegstyle = new nodeStyle();
        rolegstyle.put("fill", Boolean.valueOf(true));
        rolegstyle.put("fillColor", Color.green);
        rolegstyle.put("textattop", Boolean.valueOf(true));
        rolegstyle.put("underlinetitle", Boolean.valueOf(true));
        astylemap.put("rolegroup", rolegstyle);
        nodeStyle defstyle = nodeStyle.getdefault();
        astylemap.put("default", defstyle);
        return astylemap;
    }
}