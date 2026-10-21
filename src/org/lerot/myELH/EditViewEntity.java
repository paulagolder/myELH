package org.lerot.myELH;

import java.awt.event.ActionListener;

public class EditViewEntity extends EditView implements ActionListener
{
    private static final long serialVersionUID = 1L;

    EditViewEntity(MyElh app)
    {
        super(app);
    }

    public void setup(ElhNode anode)
    {
        super.setup(anode);
        this.typefield.setList(anode.getChildtypes());
        this.typefield.setSelected(anode.getType());
        this.grouptypefield.setList(ChildGrouping.choiceNames());
        this.grouptypefield.setSelected(anode.getChildgrouptype().xmlName());
        this.makeChildPanel();
        makebuttonbar();
    }
}
