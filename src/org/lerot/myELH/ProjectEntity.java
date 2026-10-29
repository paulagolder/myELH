package org.lerot.myELH;

import java.io.File;

import org.jdom2.Element;

// An entity of a project. Its life history is kept either in its own
// versioned files (<file>_v<N>.elh, the highest version being the current
// one) or inline in project.xml. It is only read when first needed.
public final class ProjectEntity
{
    private String name;
    // the base name of its files, or null if it is kept inline
    private String file;
    // while inline: its element as last saved in project.xml
    private Element inline;
    // its life history, or null until it is loaded
    private ElhNode top;
    // the version file it was read from or last saved to
    private File loadedfrom;
    // its XML and shape when last loaded or saved, to tell whether it has
    // changed and whether a save needs a new version
    private String savedxml;
    private String savedstructure;

    ProjectEntity(String name, String file, Element inline)
    {
        this.name = name;
        this.file = file;
        this.inline = inline;
    }

    public String getName()
    {
        return this.top == null ? this.name : this.top.getName();
    }

    // its name as last loaded or saved, which is how project.xml lists it
    String getSavedName()
    {
        return this.name;
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

    Element getInline()
    {
        return this.inline;
    }

    void setInline(Element inline)
    {
        this.inline = inline;
    }

    public boolean isLoaded()
    {
        return this.top != null;
    }

    // its life history; null until it is loaded
    public ElhNode getTop()
    {
        return this.top;
    }

    public File getLoadedFrom()
    {
        return this.loadedfrom;
    }

    // records the life history as just loaded or saved
    void loaded(ElhNode top, File from)
    {
        this.top = top;
        this.name = top.getName();
        this.loadedfrom = from;
        this.savedxml = top.xmlout();
        this.savedstructure = top.structure();
    }

    // forgets its life history, so that it is read again when next needed
    void unload()
    {
        this.top = null;
        this.loadedfrom = null;
        this.savedxml = null;
        this.savedstructure = null;
    }

    public boolean isModified()
    {
        return this.top != null && !this.top.xmlout().equals(this.savedxml);
    }

    // nodes added, removed or moved since it was loaded or saved
    boolean isReshaped()
    {
        return this.top != null && !this.top.structure().equals(this.savedstructure);
    }

    // where it is kept, for the project screen
    public String describeStorage()
    {
        if (isInline())
        {
            return "inline in project.xml";
        }
        return this.loadedfrom != null ? this.loadedfrom.getName() : this.file + "_v?.elh";
    }

    public String toString()
    {
        return getName();
    }
}
