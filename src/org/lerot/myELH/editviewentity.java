  package org.lerot.myELH;
  
  import java.awt.event.ActionListener;

  public class editviewentity  extends editview   implements ActionListener
  {
    private static final long serialVersionUID = 1L;
    
    public void setup(elhnode anode)
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

