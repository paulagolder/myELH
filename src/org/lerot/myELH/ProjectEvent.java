package org.lerot.myELH;

// An event in a project's catalogue. Several ELHs can share it: their leaf
// event nodes refer to it by id, each in its own wording if need be (a
// Member "Borrows", a Book "Is borrowed").
public final class ProjectEvent
{
    private final String id;
    private String name;
    // what happens in the world when the event occurs
    private String action;

    ProjectEvent(String id, String name, String action)
    {
        this.id = id;
        this.name = name;
        this.action = action == null ? "" : action;
    }

    public String getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    void setName(String name)
    {
        this.name = name;
    }

    public String getAction()
    {
        return this.action;
    }

    void setAction(String action)
    {
        this.action = action == null ? "" : action;
    }

    public String toString()
    {
        return this.name;
    }
}
