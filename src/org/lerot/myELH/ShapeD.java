package org.lerot.myELH;

import java.awt.*;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

public class ShapeD
{
    Double x=0.0;
    Double y=0.0;
    Double width=0.0;
    Double height = 0.0;

    public ShapeD()
    {
        x=0.0;
        y=0.0;
        width=0.0;
        height= 0.0;
    }

    public ShapeD(Double ax, Double ay, Double aw, Double ah)
    {
        x=ax;
        y=ay;
        width=aw;
        height= ah;
    }

    public void setShape(Double ax, Double ay, Double aw, Double ah)
    {
        x=ax;
        y=ay;
        width=aw;
        height= ah;
    }


    public HashMap<String, Double> getDimension()
    {
        HashMap<String,Double> dimension = new HashMap<>();
        dimension.put("width",width);
        dimension.put("height",height);
        return dimension;
    }

    public Rectangle2D.Double getBounds()
    {
        return new Rectangle2D.Double(x,y,width,height);
    }

    public Point2D.Double getPosition()
    {
        return new Point2D.Double(x,y);
    }

    public void setDimension(Double w, Double h)
    {
        width = w;
        height = h;
    }


    boolean containsPoint(double px, double py)
    {
        return px < x + width && px > x && py < y + height && py > y;
    }

    ShapeI getShapeI()
    {
        return new ShapeI((int) Math.round(x), (int) Math.round(y), (int) Math.round(width), (int) Math.round(height));
    }

    public void fillRect(Graphics2D g2d)
    {
        ShapeI shapei = getShapeI();
        g2d.fillRect(shapei.x,shapei.y,shapei.width,shapei.height);
    }

    public void drawRect(Graphics2D g2d)
    {
        ShapeI shapei = getShapeI();
        g2d.drawRect(shapei.x,shapei.y,shapei.width,shapei.height);
    }
}
