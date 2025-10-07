package org.lerot.myELH;

import java.awt.event.ActionListener;

public class editviewevent extends editview implements ActionListener
{
    private static final long serialVersionUID = 1L;

    public void setup(event anode)
    {
        super.setup(anode);
        this.namefield.setText(anode.getName());
        this.typefield.setList(anode.getChildtypes());
        this.typefield.setSelected(anode.getType());
        this.grouptypefield.setList(anode.getChildgroupingtypes());
        this.grouptypefield.setSelected(anode.getChildgrouptype());
        this.stylefield.setText(anode.getStylename());
        this.childpanel.removeAll();
        makeChildPanel();
        makebuttonbar();
    }
}


