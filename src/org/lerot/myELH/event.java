  package org.lerot.myELH;
  

  public class event  extends elhnode
  {
      static String[]   childtypes = { "event", "role" };
    static String[]  childgroupingtypes = { "sequence", "option", "repetition", "rolegroup" };;

    public event(String aname, elhnode aparent) {
      super(aname, aparent);
      this.childgrouptype = "sequence";

    }

    public void insert(String string)
    {
      System.out.println(" inserting Event");
      event newevent = new event("newevent", this);
      addChild(newevent);
    }



  }
