package org.lerot.myELH;

public final class Event extends ElhNode
{
    public Event(String aname, ElhNode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = ChildGrouping.SEQUENCE;
    }
}
