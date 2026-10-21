package org.lerot.myELH;

import java.awt.event.ActionListener;

public class EditViewEvent extends EditView implements ActionListener
{
    private static final long serialVersionUID = 1L;

    EditViewEvent(MyElh app)
    {
        super(app);
    }

    public void setup(Event anode)
    {
        super.setup(anode);
        this.namefield.setText(anode.getName());
        this.typefield.setList(anode.getChildtypes());
        this.typefield.setSelected(anode.getType());
        this.grouptypefield.setList(ChildGrouping.choiceNames());
        this.grouptypefield.setSelected(anode.getChildgrouptype().xmlName());
        this.stylefield.setText(anode.getStylename());
        this.childpanel.removeAll();
        makeChildPanel();
        makebuttonbar();
    }
}


