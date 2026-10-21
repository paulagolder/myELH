package org.lerot.myELH;

import java.awt.event.ActionListener;

public class editviewelh extends editview implements ActionListener
{
    private static final long serialVersionUID = 1L;

    editviewelh(myELHgui app)
    {
        super(app);
    }

    public void setup(elhnode anode)
    {
        super.setup(anode);
        String[] t = {"ELH"};
        this.typefield.setList(t);
        this.typefield.setSelected(anode.getType());
        this.grouptypefield.setList(ChildGrouping.choiceNames());
        this.grouptypefield.setSelected(anode.getChildgrouptype().xmlName());
        this.childpanel.removeAll();
        makeChildPanel();
        makebuttonbar();
    }
}
