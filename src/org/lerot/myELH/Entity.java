package org.lerot.myELH;

public final class Entity extends ElhNode
{
    public Entity(String aname, ElhNode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = ChildGrouping.SEQUENCE;
    }
}
