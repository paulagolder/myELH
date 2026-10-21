package org.lerot.myELH;

public final class event extends elhnode
{
    public event(String aname, elhnode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = ChildGrouping.SEQUENCE;
    }
}
