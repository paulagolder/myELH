package org.lerot.myELH;

public final class EntityGroup extends ElhNode
{
    public EntityGroup(String aname, ElhNode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = ChildGrouping.ENTITYGROUP;
    }
}
