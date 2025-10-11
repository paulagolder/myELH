  package org.lerot.myELH;
  

  public class entity  extends elhnode
  {


    public entity(String aname, elhnode aparent)
    {
      super(aname, aparent);
      this.childgrouptype = "sequence";
    }


    public void insert(String string)
    {
      System.out.println(" inserting entity ");
      entity newelhnode = new entity("newentity", this);
      addChild(newelhnode);
    }



  }

