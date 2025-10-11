package org.lerot.myELH;

public class entitygroup extends elhnode
{


    public entitygroup(String aname, elhnode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = "entitygroup";
    }

    public void insert(String string)
    {
        System.out.println(" inserting Rolegroup");
        entitygroup newelhnode = new entitygroup("newrole", this);
        addChild(newelhnode);
    }
}

