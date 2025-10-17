package org.lerot.myELH;

public class role extends elhnode
{
    public role(String aname, elhnode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = "sequence";
    }

    public void insert(String string)
    {
        System.out.println(" inserting Role");
        role newelhnode = new role("newrole", this);
        addChild(newelhnode);
    }
}


