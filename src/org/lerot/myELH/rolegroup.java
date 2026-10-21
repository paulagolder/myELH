package org.lerot.myELH;

public final class rolegroup extends elhnode
{
    public rolegroup(String aname, elhnode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = "rolegroup";
    }
}
