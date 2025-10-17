package org.lerot.myELH;

import java.awt.*;
import java.util.HashMap;

public class styleMaps extends HashMap<String, styleMap>
{
    private static final long serialVersionUID = 1L;



    static public styleMap getScreenStyles()
    {
        styleMap sm = styleMap.makedefault();
        sm.include(makeScreenStyles());
        return sm;
    }

    static public styleMap getPrintStyles()
    {
        styleMap sm = styleMap.makedefault();
        sm.include(makePrintStyles());
        return sm;
    }

    public styleMap getStyleMap(String stylename)
    {
        styleMap stylemap = this.get(stylename);
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

    public static styleMap makeScreenStyles()
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

    public static styleMap makePrintStyles()
    {
        styleMap astylemap = new styleMap();
        nodeStyle docstyle = new nodeStyle();
        docstyle.put("fill", Boolean.FALSE);
        docstyle.put("fillColor", Color.pink);
        docstyle.put("lineColor", Color.black);
        astylemap.put("document", docstyle);
        nodeStyle anodestyle = new nodeStyle();
        anodestyle.put("textattop", Boolean.FALSE);
        astylemap.put("event", anodestyle);
        nodeStyle anodestyle2 = new nodeStyle();
        anodestyle2.put("textattop", Boolean.TRUE);
        anodestyle2.put("underlinetitle", Boolean.TRUE);
        astylemap.put("entity", anodestyle2);
        nodeStyle rolestyle = new nodeStyle();
        rolestyle.put("textattop", Boolean.TRUE);
        rolestyle.put("underlinetitle", Boolean.TRUE);
        astylemap.put("role", rolestyle);
        nodeStyle rolegstyle = new nodeStyle();
        rolegstyle.put("textattop", Boolean.TRUE);
        rolegstyle.put("underlinetitle", Boolean.TRUE);
        astylemap.put("rolegroup", rolegstyle);
        nodeStyle defstyle = nodeStyle.makeDefaultNodeStyle();
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

