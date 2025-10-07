  package org.lerot.myELH;
  

  public class entity  extends elhnode
  {
      protected static String[] childgroupingtypes = {"sequence", "option", "repetition", "rolegroup"};
      static String[]   childtypes = { "event", "role" };

    public entity(String aname, elhnode aparent)
    {
      super(aname, aparent);
    }



    
    public void insert(String string)
    {
      System.out.println(" inserting event ");
      event newevent = new event("newevent", this);
      addChild(newevent);
    }



  }

