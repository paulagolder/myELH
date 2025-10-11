  package org.lerot.myELH;
  

  public class event  extends elhnode
  {


    public event(String aname, elhnode aparent) {
      super(aname, aparent);
      this.childgrouptype = "sequence";

    }

    public void insert(String string)
    {
      System.out.println(" inserting Event");
      event newelhnode  = new event("newevent", this);
      addChild(newelhnode);
    }



  }
