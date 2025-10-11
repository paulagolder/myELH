package org.lerot.myELH;

import java.awt.*;
import java.util.HashMap;

public class styleMaps extends HashMap<String, styleMap>
{
    private static final long serialVersionUID = 1L;

    public nodeStyle get(String mapkey, String elhtype)
    {
        styleMap astylemap = get(mapkey);
        if (astylemap == null)
        {
            astylemap = get("default");
        }
        nodeStyle anodestyle = astylemap.get(elhtype);
        if (anodestyle == null)
        {
            anodestyle = astylemap.get("default");
        }
        return anodestyle;
    }

    public void makeNodeStyles()
    {
        put("default",styleMap.getdefault());
        styleMap sm = this.get("default");
        sm.include(makeStyles());
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

