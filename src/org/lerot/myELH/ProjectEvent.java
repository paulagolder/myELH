package org.lerot.myELH;

// An event in a project's catalogue. Several entities can share it: their
// leaf event nodes refer to it by id, each in its own wording if need be (a
// Member "Borrows", a Book "Is borrowed"). Its id and name are always in
// project.xml; its details are kept either inline there or in its own
// versioned files (<file>_v<N>.event), read only when first needed.
public final class ProjectEvent
{
    private final String id;
    private String name;
    // what happens in the world when the event occurs
    private String action;
    // the base name of its files, or null if it is kept inline
    private String file;
    // false while its details are still in its file, unread
    private boolean loaded;

    ProjectEvent(String id, String name, String action)
    {
        this(id, name, action, null);
    }

    // an event kept in its own file is not loaded until its details are read
    ProjectEvent(String id, String name, String action, String file)
    {
        this.id = id;
        this.name = name;
        this.action = action == null ? "" : action;
        this.file = file;
        this.loaded = file == null;
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

    // its action; empty until its details are loaded (Project.loadEvent)
    public String getAction()
    {
        return this.action;
    }

    void setAction(String action)
    {
        this.action = action == null ? "" : action;
    }

    public String getFile()
    {
        return this.file;
    }

    public boolean isInline()
    {
        return this.file == null;
    }

    void setFile(String file)
    {
        this.file = file;
    }

    public boolean isLoaded()
    {
        return this.loaded;
    }

    void setLoaded()
    {
        this.loaded = true;
    }

    public String toString()
    {
        return this.name;
    }
}
