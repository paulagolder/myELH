package org.lerot.myELH;

public final class RoleGroup extends ElhNode
{
    public RoleGroup(String aname, ElhNode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = ChildGrouping.ROLEGROUP;
    }
}
