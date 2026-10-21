package org.lerot.myELH;

public final class role extends elhnode
{
    public role(String aname, elhnode aparent)
    {
        super(aname, aparent);
        this.childgrouptype = "sequence";
    }
}
