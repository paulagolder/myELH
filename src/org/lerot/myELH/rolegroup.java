  package org.lerot.myELH;
  

  public class rolegroup  extends elhnode
  {

      static String[]   childtypes = { "event", "role" };
      static String[]  childgroupingtypes = { "sequence", "option", "repetition", "rolegroup" };;

      public rolegroup(String aname, elhnode aparent)
    {
      super(aname, aparent);

      this.childgrouptype = "group";

    }
  
  
  

    
    public void insert(String string)
    {
      System.out.println(" inserting Rolegroup ");
      event newevent = new event("newrolegroup", this);
      addChild(newevent);
    }


  }

