  package org.lerot.myELH;
  
  import java.util.List;
  import org.jdom.Element;
  

  public class role     extends entity
  {
      static String[]   childtypes = { "event", "role" };
      static String[]  childgroupingtypes = { "sequence", "option", "repetition", "rolegroup" };;


      public role(String aname, elhnode aparent)
      {
      super(aname, aparent);
      this.childgrouptype = "sequence";

    }

  

    
    public void insert(String string) {
      System.out.println(" inserting event ");
      event newevent = new event("newevent", this);
      addChild(newevent);
    }
  
    

  
    

  }


