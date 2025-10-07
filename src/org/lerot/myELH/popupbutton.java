package org.lerot.myELH;

import org.lerot.mywidgets.jswButton;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class popupbutton  extends jswButton
{
    private JPopupMenu popup;
    private elhnode parentnode;

    public popupbutton(ActionListener al, String name, String actioncommand)
    {
        super(al,name,actioncommand);

    }








    public elhnode getParentnode()
    {
        return parentnode;
    }

    public void setParentnode(elhnode parentnode)
    {
        this.parentnode = parentnode;
    }
}
