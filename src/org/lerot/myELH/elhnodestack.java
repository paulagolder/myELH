package org.lerot.myELH;

import java.awt.*;
import java.util.HashMap;
import java.util.Vector;

import static org.lerot.myELH.elhDrawNode.sizeString;

class elhnoderow
{
    Vector<elhDrawNode> members;
    double rowtop =0.0;
    double rowheight =0.0;
    double rowwidth=0.0;
    int row=0;

    elhnoderow()
    {
        members= new Vector<elhDrawNode>();
    }

    void addNode(elhDrawNode adnode)
    {
         members.add(adnode);

 /*       double w = adnode.nodestylemap.getDouble( "rwidth");
        double h = adnode.nodestylemap.getDouble( "rheight");
        g2d.setFont(view.docfont);
        String[] ftext = elhDrawNode.formatLabel(text, g2d, w);
        FontMetrics fm = g2d.getFontMetrics();
        Dimension r = sizeString(ftext, g2d, view.docfont);
        if (adnode.nodestylemap.getBoolean("canexpand") && r.getWidth() > w)
        {
            w = r.getWidth() + 8.0D;
        }*/
    }

    public void add(elhDrawNode acnode)
    {
        members.add(acnode);
    }
}


public class elhnodestack
{
    private final svgdoc view;
    String layoutstyle;
    HashMap<Integer,elhnoderow>  elhtree = new HashMap<>();
    Vector<elhDrawNode> sibs = new Vector();
    elhDrawNode topnode = null;
    int currentsib = 0;
    elhDrawNode currentnode= null;

    public elhnodestack(elhDrawNode atopnode,svgdoc asvgdoc,String layout)
        {
            view = asvgdoc;
            layoutstyle = layout;
         currentnode = null;
        topnode = atopnode;
        int row = currentnode.row+1;
        for(elhDrawNode acnode : currentnode.getChildren())
        {
            elhtree.get(row).add(acnode);
        }
    }

    public elhDrawNode getnextnode()
    {
        if(currentnode == null)
        {
            return topnode;
        }
        if(currentsib >= sibs.size())
        {
            //exhausted current level so  down one level from

        }
return currentnode;
    }
}

