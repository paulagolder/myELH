package org.lerot.myELH;

public final class entitygroup extends elhnode
{
    public entitygroup(String aname, elhnode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = "entitygroup";
    }
}
