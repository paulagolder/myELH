package org.lerot.myELH;

import java.awt.*;
import java.util.HashMap;

public class nodeStyle extends HashMap<String, Object>
{
    private static final long serialVersionUID = 1L;

    public static nodeStyle makeDefaultNodeStyle()
    {
        nodeStyle anodestyle = new nodeStyle();
        anodestyle.put("rwidth", 80.0D);
        anodestyle.put("rheight", 60.0D);
        anodestyle.put("borderColor", Color.gray);
        anodestyle.put("textColor", Color.black);
        anodestyle.put("textFontSize", 14);
        anodestyle.put("symbolFontSize", 2);
        anodestyle.put("symbolColor", Color.blue);
        anodestyle.put("lineColor", Color.red);
        anodestyle.put("lineWidth", 1.0D);
        anodestyle.put("fillColor",  Color.decode("#b09f9f"));
        anodestyle.put("fill", Boolean.TRUE);
        anodestyle.put("borderWidth", 2.0D);
        anodestyle.put("hspace", 20.0D);
        anodestyle.put("vspace", 30.0D);
        anodestyle.put("canexpand", Boolean.TRUE);
        anodestyle.put("textattop", Boolean.FALSE);
        anodestyle.put("underlinetitle", Boolean.FALSE);
        return anodestyle;
    }

    public double getDouble(String key)
    {
        return ((Double) get(key)).doubleValue();
    }

    public boolean getBoolean(String key)
    {
        return ((Boolean) get(key)).booleanValue();
    }

    public Color getColor(String key)
    {
        return (Color) get(key);
    }

    public Integer getInteger(String key)
    {
        return (Integer) get(key);
    }

    public void include(nodeStyle anodestyle)
    {
        if (anodestyle == null || anodestyle.isEmpty()) return;
        for (Entry<String, Object> amap : anodestyle.entrySet())
        {
            String key = amap.getKey();
            Object anobject = amap.getValue();
            this.put(key, anobject);
        }
    }
}


