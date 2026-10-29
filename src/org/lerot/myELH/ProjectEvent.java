package org.lerot.myELH;

// An event in a project's catalogue: something that happens in the world
// and triggers an action in each of two or more entities. Each action is a
// leaf event node of an entity's life history that refers to the event by
// id, in its own wording if need be (a Member "Borrows", a Book "Is
// borrowed"). Its id and name are always in project.xml; its details are
// kept either inline there or in its own versioned files
// (<file>_v<N>.event), read only when first needed.
public final class ProjectEvent
{
    private final String id;
    private String name;
    // what happens in the world when the event occurs
    private String description;
    // the base name of its files, or null if it is kept inline
    private String file;
    // false while its details are still in its file, unread
    private boolean loaded;

    ProjectEvent(String id, String name, String description)
    {
        this(id, name, description, null);
    }

    // an event kept in its own file is not loaded until its details are read
    ProjectEvent(String id, String name, String description, String file)
    {
        this.id = id;
        this.name = name;
        this.description = description == null ? "" : description;
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

    // its description; empty until its details are loaded (Project.loadEvent)
    public String getDescription()
    {
        return this.description;
    }

    void setDescription(String description)
    {
        this.description = description == null ? "" : description;
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
