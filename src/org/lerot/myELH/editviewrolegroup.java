  package org.lerot.myELH;
  
  import java.awt.event.ActionEvent;
  import java.awt.event.ActionListener;

  public class editviewrolegroup     extends editview     implements ActionListener
  {
    private static final long serialVersionUID = 1L;
    
    public void setup(elhnode anode) {
      super.setup(anode);
      this.typefield.setList(rolegroup.getChildtypes());
      this.typefield.setSelected(anode.getType());
      this.grouptypefield.setList(rolegroup.getChildgroupingtypes());
      this.grouptypefield.setSelected(anode.getChildgrouptype());
      makeChildPanel();
        makebuttonbar();
    }
  
    
    public void actionPerformed(ActionEvent ae) {
      String comStr = ae.getActionCommand();
      actions(comStr, this.activenode);
    }
  
    

  }

