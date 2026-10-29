package org.lerot.myELH;

import java.awt.*;
import java.util.HashMap;

public class StyleMaps extends HashMap<String, StyleMap>
{
    private static final long serialVersionUID = 1L;

    static public StyleMap getScreenStyles()
    {
        StyleMap sm = StyleMap.makedefault();
        sm.include(makeScreenStyles());
        return sm;
    }

    static public StyleMap getPrintStyles()
    {
        StyleMap sm = StyleMap.makedefault();
        sm.include(makePrintStyles());
        return sm;
    }

    public StyleMap getStyleMap(String stylename)
    {
        StyleMap stylemap = this.get(stylename);
        if(stylemap == null)
        {
            System.out.println( "Not found styles "+stylename);
            stylemap = this.get("default");
        }
        if(stylemap == null)
        {
            System.out.println( "Not found default styles ");
        }
        return stylemap;
    }

    public static StyleMap makeScreenStyles()
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
        anodestyle.put("rectanglestyle", "hard");
        astylemap.put("action", anodestyle);
        NodeStyle anodestyle2 = new NodeStyle();
        anodestyle2.put("fill", Boolean.FALSE);
        anodestyle2.put("fillColor", Color.yellow);
        anodestyle2.put("textattop", Boolean.TRUE);
        anodestyle2.put("underlinetitle", Boolean.TRUE);
        anodestyle2.put("rectanglestyle", "hard");
        astylemap.put("entity", anodestyle2);
        NodeStyle rolestyle = new NodeStyle();
        rolestyle.put("fill", Boolean.TRUE);
        rolestyle.put("fillColor", Color.pink);
        rolestyle.put("textattop", Boolean.TRUE);
        rolestyle.put("underlinetitle", Boolean.TRUE);
        rolestyle.put("rectanglestyle", "soft");
        astylemap.put("role", rolestyle);
        NodeStyle rolegstyle = new NodeStyle();
        rolegstyle.put("fill", Boolean.TRUE);
        rolegstyle.put("fillColor", Color.green);
        rolegstyle.put("textattop", Boolean.TRUE);
        rolegstyle.put("underlinetitle", Boolean.TRUE);
        rolegstyle.put("rectanglestyle", "soft");
        astylemap.put("rolegroup", rolegstyle);
        NodeStyle defstyle = NodeStyle.makeDefaultNodeStyle();
        astylemap.put("default", defstyle);
        return astylemap;
    }

    public static StyleMap makePrintStyles()
    {
        StyleMap astylemap = new StyleMap();
        NodeStyle docstyle = new NodeStyle();
        docstyle.put("fill", Boolean.FALSE);
        docstyle.put("fillColor", Color.pink);
        docstyle.put("lineColor", Color.black);
        astylemap.put("document", docstyle);
        NodeStyle anodestyle = new NodeStyle();
        anodestyle.put("textattop", Boolean.FALSE);
        astylemap.put("action", anodestyle);
        NodeStyle anodestyle2 = new NodeStyle();
        anodestyle2.put("textattop", Boolean.TRUE);
        anodestyle2.put("underlinetitle", Boolean.TRUE);
        astylemap.put("entity", anodestyle2);
        NodeStyle rolestyle = new NodeStyle();
        rolestyle.put("textattop", Boolean.TRUE);
        rolestyle.put("underlinetitle", Boolean.TRUE);
        astylemap.put("role", rolestyle);
        NodeStyle rolegstyle = new NodeStyle();
        rolegstyle.put("textattop", Boolean.TRUE);
        rolegstyle.put("underlinetitle", Boolean.TRUE);
        astylemap.put("rolegroup", rolegstyle);
        NodeStyle defstyle = NodeStyle.makeDefaultNodeStyle();
        defstyle.put("textattop", Boolean.FALSE);
        defstyle.put("borderColor", Color.black);
        defstyle.put("borderWidth", 4.0D);
        defstyle.put("rheight", 40.0D);
        defstyle.put("fill", Boolean.FALSE);
        defstyle.put("fillColor", Color.green);
        astylemap.put("default", defstyle);

        return astylemap;
    }
}

