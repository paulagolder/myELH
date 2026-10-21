package org.lerot.myELH;

public final class Role extends ElhNode
{
    public Role(String aname, ElhNode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = ChildGrouping.SEQUENCE;
    }
}
