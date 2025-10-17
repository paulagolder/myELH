package org.lerot.myELH;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

public class ShapeI
{
    int x=0;
    int y=0;
    int width=0;
    int height = 0;

    public ShapeI()
    {
        x=0;
        y=0;
        width=0;
        height= 0;
    }

    public ShapeI(int ax, int ay, int aw, int ah)
    {
        x=ax;
        y=ay;
        width=aw;
        height= ah;
    }




    public Dimension getDimension()
    {
        return new Dimension(width,height);
    }

    public Rectangle2D.Double getBounds()
    {
        return new Rectangle2D.Double(x,y,width,height);
    }

    public Point2D.Double getPosition()
    {
        return new Point2D.Double(x,y);
    }




    boolean containsPoint(double px, double py)
    {
        return px < x + width && px > x && py < y + height && py > y;
    }

}
