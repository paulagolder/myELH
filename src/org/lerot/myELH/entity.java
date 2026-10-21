package org.lerot.myELH;

public final class entity extends elhnode
{
    public entity(String aname, elhnode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = "sequence";
    }
}
