package org.lerot.myELH;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EditViewRoleGroup extends EditView implements ActionListener
{
    private static final long serialVersionUID = 1L;

    EditViewRoleGroup(MyElh app)
    {
        super(app);
    }

    public void setup(ElhNode anode)
    {
        super.setup(anode);
        this.typefield.setList(RoleGroup.getChildtypes());
        this.typefield.setSelected(anode.getType());
        this.grouptypefield.setList(ChildGrouping.choiceNames());
        this.grouptypefield.setSelected(anode.getChildgrouptype().xmlName());
        makeChildPanel();
        makebuttonbar();
    }

    public void actionPerformed(ActionEvent ae)
    {
        String comStr = ae.getActionCommand();
        actions(comStr, this.activenode);
    }
}
