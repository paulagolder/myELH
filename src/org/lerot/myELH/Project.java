package org.lerot.myELH;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

// A project is a folder holding a project.xml. The project manages the
// model's two kinds of object, its entities and its events:
//
//   <project>
//     <name>Libproj</name> <author>Paul</author>
//     <entities>
//       <entity name="Person" file="Person"/>   kept in Person_v<N>.elh
//       <entity name="Book"/>                   kept inline, below
//     </entities>
//     <events nextid="8">
//       <event id="e2" name="Borrow" file="Borrow"/>   kept in Borrow_v<N>.event
//       <event id="e1" name="Joins"/>                  kept inline, below
//     </events>
//     <myelh> the inline entities' life histories and events' details </myelh>
//   </project>
//
// An entity's or event's own file wraps it in <myelh> too. Files are only
// read when the object is first needed.
public final class Project
{
    static final String PROJECTFILE = "project.xml";
    static final String ENTITYEXTENSION = ".elh";
    static final String EVENTEXTENSION = ".event";
    private final File folder;
    private String name;
    private String author;
    private final List<ProjectEntity> entities = new ArrayList<>();
    // the project's event catalogue, in the order the events were added
    private final List<ProjectEvent> events = new ArrayList<>();
    // the number in the next event id (e1, e2, ...); saved, so ids are never reused
    private int nexteventid = 1;
    // what converting an older project did, for the user to see
    private final List<String> conversionnotes = new ArrayList<>();

    private Project(File folder, String name, String author)
    {
        this.folder = folder;
        this.name = name;
        this.author = author;
    }

    // makes the project's folder inside parentfolder and writes its project.xml
    public static Project create(File parentfolder, String name, String author) throws IOException
    {
        File folder = new File(parentfolder, name);
        if (folder.exists())
        {
            throw new IOException(folder + " already exists");
        }
        Files.createDirectories(folder.toPath());
        Project project = new Project(folder, name, author);
        project.save();
        return project;
    }

    // An older project.xml has no entity list: its entities are then found in
    // the latest version of each ELH file in the folder and each is written to
    // its own file. The old files are left as they are, and the old
    // project.xml is kept as project.xml.bak.
    public static Project load(File folder) throws IOException
    {
        Document doc;
        try
        {
            doc = new SAXBuilder().build(new File(folder, PROJECTFILE));
        } catch (JDOMException e)
        {
            throw new IOException(e.getMessage(), e);
        }
        Element root = doc.getRootElement();
        String name = root.getChildTextTrim("name");
        Project project = new Project(folder, name == null || name.isEmpty() ? folder.getName() : name,
                root.getChildTextTrim("author"));
        Element inline = root.getChild(MyelhFile.WRAPPER);
        project.loadEvents(root.getChild("events"), inline);
        Element entitylist = root.getChild("entities");
        if (entitylist == null)
        {
            project.convertOldElhs();
        } else
        {
            project.loadEntities(entitylist, inline);
        }
        return project;
    }

    private void loadEntities(Element entitylist, Element inline)
    {
        for (Element entry : entitylist.getChildren("entity"))
        {
            String entityname = entry.getAttributeValue("name");
            if (entityname == null)
            {
                continue;
            }
            String file = entry.getAttributeValue("file");
            Element details = null;
            if (file == null)
            {
                details = inline == null ? null : findInlineEntity(inline, entityname);
                if (details == null)
                {
                    // listed but missing: an entity with an empty life history
                    details = new Element(NodeType.ENTITY.xmlName()).setAttribute("name", entityname);
                }
            }
            this.entities.add(new ProjectEntity(entityname, file, details == null ? null : details.clone()));
        }
    }

    private static Element findInlineEntity(Element inline, String entityname)
    {
        for (Element element : MyelhFile.entityElements(inline))
        {
            if (entityname.equals(element.getAttributeValue("name")))
            {
                return element;
            }
        }
        return null;
    }

    private void loadEvents(Element eventlist, Element inline)
    {
        if (eventlist == null)
        {
            return;
        }
        for (Element entry : eventlist.getChildren("event"))
        {
            String id = entry.getAttributeValue("id");
            String eventname = entry.getAttributeValue("name");
            if (id == null || eventname == null)
            {
                continue;
            }
            String file = entry.getAttributeValue("file");
            if (file != null)
            {
                this.events.add(new ProjectEvent(id, eventname, null, file));
            } else
            {
                // inline details are in <myelh>; projects saved before that kept
                // the action, or earlier a description, in the list itself
                Element details = inline == null ? null : MyelhFile.findEventElement(inline, id);
                String action = details != null ? details.getChildTextTrim("action")
                        : entry.getChildTextTrim("action") != null ? entry.getChildTextTrim("action")
                        : entry.getChildTextTrim("description");
                this.events.add(new ProjectEvent(id, eventname, action));
            }
            if (id.matches("e\\d+"))
            {
                this.nexteventid = Math.max(this.nexteventid, Integer.parseInt(id.substring(1)) + 1);
            }
        }
        String nextid = eventlist.getAttributeValue("nextid");
        if (nextid != null && nextid.matches("\\d+"))
        {
            this.nexteventid = Math.max(this.nexteventid, Integer.parseInt(nextid));
        }
    }

    private void convertOldElhs() throws IOException
    {
        File projectfile = new File(this.folder, PROJECTFILE);
        File backup = new File(this.folder, PROJECTFILE + ".bak");
        if (!backup.exists())
        {
            Files.copy(projectfile.toPath(), backup.toPath());
        }
        for (Map.Entry<String, File> latest : ElhVersions.latestVersions(this.folder).entrySet())
        {
            File oldfile = latest.getValue();
            if (MyelhFile.isMyelh(oldfile))
            {
                continue;
            }
            ElhNode top;
            try
            {
                top = ElhFile.read(oldfile.toPath());
            } catch (IOException e)
            {
                this.conversionnotes.add(oldfile.getName() + " could not be read: " + e.getMessage());
                continue;
            }
            // an ELH with nothing in it was started for an entity of its name
            List<ElhNode> found = top.getChildren().isEmpty()
                    ? List.of(new ElhNode(NodeType.ENTITY, ElhVersions.baseName(oldfile), null))
                    : List.copyOf(top.getChildren());
            for (ElhNode child : found)
            {
                if (findEntity(child.getName()) != null)
                {
                    this.conversionnotes.add(child.getName() + " in " + oldfile.getName()
                            + " was left out: the project already has an entity of that name");
                    continue;
                }
                child.setParent(null);
                String base = newFileBase(child.getName(), ENTITYEXTENSION);
                File target = ElhVersions.nextVersion(this.folder, base, ENTITYEXTENSION);
                MyelhFile.writeEntity(child, target.toPath());
                this.entities.add(new ProjectEntity(child.getName(), base, null));
                this.conversionnotes.add(child.getName() + " from " + oldfile.getName() + " is now in " + target.getName());
            }
        }
        save();
    }

    // what converting an older project did; empty if it was not converted
    public List<String> getConversionNotes()
    {
        return List.copyOf(this.conversionnotes);
    }

    // the folders directly inside parentfolder that hold a project.xml
    public static List<File> findProjects(File parentfolder)
    {
        List<File> found = new ArrayList<>();
        File[] subfolders = parentfolder.listFiles(File::isDirectory);
        if (subfolders != null)
        {
            for (File subfolder : subfolders)
            {
                if (new File(subfolder, PROJECTFILE).isFile())
                {
                    found.add(subfolder);
                }
            }
        }
        found.sort(null);
        return found;
    }

    // writes project.xml: the lists, and the inline objects as last saved
    public void save() throws IOException
    {
        Element root = new Element("project");
        root.addContent(new Element("name").setText(this.name));
        root.addContent(new Element("author").setText(this.author == null ? "" : this.author));
        Element inline = new Element(MyelhFile.WRAPPER);
        Element entitylist = new Element("entities");
        for (ProjectEntity entity : this.entities)
        {
            Element entry = new Element("entity").setAttribute("name", entity.getSavedName());
            if (entity.isInline())
            {
                inline.addContent(entity.getInline().clone());
            } else
            {
                entry.setAttribute("file", entity.getFile());
            }
            entitylist.addContent(entry);
        }
        root.addContent(entitylist);
        Element eventlist = new Element("events");
        eventlist.setAttribute("nextid", String.valueOf(this.nexteventid));
        for (ProjectEvent event : this.events)
        {
            Element entry = new Element("event");
            entry.setAttribute("id", event.getId());
            entry.setAttribute("name", event.getName());
            if (event.isInline())
            {
                // an event with nothing more than its id and name needs no details
                if (!event.getAction().isEmpty())
                {
                    inline.addContent(MyelhFile.eventElement(event));
                }
            } else
            {
                entry.setAttribute("file", event.getFile());
            }
            eventlist.addContent(entry);
        }
        root.addContent(eventlist);
        root.addContent(inline);
        try (Writer out = Files.newBufferedWriter(new File(this.folder, PROJECTFILE).toPath()))
        {
            new XMLOutputter(Format.getPrettyFormat()).output(new Document(root), out);
        }
    }

    public File getFolder()
    {
        return this.folder;
    }

    public String getName()
    {
        return this.name;
    }

    public String getAuthor()
    {
        return this.author;
    }

    public String toString()
    {
        return this.name;
    }

    // a base name for a new object's files that no other object uses. Files
    // already in the folder under that name (old ELH files, or those of a
    // removed object) are never overwritten: new versions are numbered after
    // them, and only <myelh> files count as an entity's versions.
    private String newFileBase(String objectname, String extension)
    {
        String base = ElhVersions.fileBaseName(objectname);
        String tried = base;
        for (int n = 2; usesFileBase(tried, extension); n++)
        {
            tried = base + "_" + n;
        }
        return tried;
    }

    private boolean usesFileBase(String base, String extension)
    {
        return extension.equals(ENTITYEXTENSION)
                ? this.entities.stream().anyMatch(e -> base.equalsIgnoreCase(e.getFile()))
                : this.events.stream().anyMatch(e -> base.equalsIgnoreCase(e.getFile()));
    }

    // ---- entities ----

    public List<ProjectEntity> getEntities()
    {
        return List.copyOf(this.entities);
    }

    // the entity with this name, in any case, or null
    public ProjectEntity findEntity(String entityname)
    {
        return this.entities.stream().filter(e -> e.getName().equalsIgnoreCase(entityname)).findFirst().orElse(null);
    }

    // the entity whose life history this is, or null
    public ProjectEntity entityOf(ElhNode top)
    {
        return this.entities.stream().filter(e -> e.getTop() == top).findFirst().orElse(null);
    }

    // a new entity, kept inline, with an empty life history; saved straight away
    public ProjectEntity addEntity(String entityname) throws IOException
    {
        return addEntity(new ElhNode(NodeType.ENTITY, entityname, null));
    }

    // adds a life history from elsewhere as a new entity, kept inline
    public ProjectEntity addEntity(ElhNode top) throws IOException
    {
        if (findEntity(top.getName()) != null)
        {
            throw new IOException("The project already has an entity called " + top.getName());
        }
        top.setParent(null);
        ProjectEntity entity = new ProjectEntity(top.getName(), null, top.toElement());
        entity.loaded(top, null);
        this.entities.add(entity);
        save();
        return entity;
    }

    // its files are left in the folder
    public void removeEntity(ProjectEntity entity) throws IOException
    {
        this.entities.remove(entity);
        save();
    }

    // the entity's files, lowest version first; old ELH files of the same
    // name in the folder are not its versions
    public List<File> entityVersions(ProjectEntity entity)
    {
        if (entity.isInline())
        {
            return List.of();
        }
        return ElhVersions.versions(this.folder, entity.getFile(), ENTITYEXTENSION).stream()
                .filter(MyelhFile::isMyelh).toList();
    }

    // reads the entity's life history if it has not been read yet
    public ElhNode loadEntity(ProjectEntity entity) throws IOException
    {
        if (entity.isLoaded())
        {
            return entity.getTop();
        }
        ElhNode top = readEntity(entity);
        entity.loaded(top, entity.isInline() ? null : entityVersions(entity).getLast());
        return top;
    }

    // an entity's life history, for looking at only
    public record LifeHistory(ProjectEntity entity, ElhNode top)
    {
    }

    // The life history of every entity, for the screens that look across
    // them all. An entity already loaded gives its tree in memory; any other
    // is read afresh but left unloaded, so it is still only opened when it is
    // selected. One that cannot be read is left out.
    public List<LifeHistory> lifeHistories()
    {
        List<LifeHistory> found = new ArrayList<>();
        for (ProjectEntity entity : this.entities)
        {
            try
            {
                found.add(new LifeHistory(entity, entity.isLoaded() ? entity.getTop() : readEntity(entity)));
            } catch (IOException e)
            {
                // left out
            }
        }
        return found;
    }

    private ElhNode readEntity(ProjectEntity entity) throws IOException
    {
        if (entity.isInline())
        {
            return MyelhFile.entityFromElement(entity.getInline());
        }
        List<File> versions = entityVersions(entity);
        if (versions.isEmpty())
        {
            throw new IOException("There is no file for " + entity.getName() + " ("
                    + entity.getFile() + "_v<N>" + ENTITYEXTENSION + ") in " + this.folder);
        }
        return MyelhFile.readEntity(versions.getLast().toPath(), entity.getSavedName());
    }

    // Saves the entity's life history: inline, into project.xml; otherwise
    // over its latest version, or as a new version if nodes were added,
    // removed or moved since it was loaded. Returns the file written, or null.
    public File saveEntity(ProjectEntity entity) throws IOException
    {
        if (!entity.isLoaded())
        {
            return null;
        }
        ElhNode top = entity.getTop();
        if (entity.isInline())
        {
            entity.setInline(top.toElement());
            entity.loaded(top, null);
            save();
            return null;
        }
        List<File> versions = entityVersions(entity);
        File target = versions.isEmpty() || entity.isReshaped()
                ? ElhVersions.nextVersion(this.folder, entity.getFile(), ENTITYEXTENSION)
                : versions.getLast();
        return writeEntityFile(entity, target);
    }

    // saves the entity as a new version whatever has changed
    public File saveEntityAsNewVersion(ProjectEntity entity) throws IOException
    {
        if (!entity.isLoaded() || entity.isInline())
        {
            return saveEntity(entity);
        }
        return writeEntityFile(entity, ElhVersions.nextVersion(this.folder, entity.getFile(), ENTITYEXTENSION));
    }

    private File writeEntityFile(ProjectEntity entity, File target) throws IOException
    {
        ElhNode top = entity.getTop();
        MyelhFile.writeEntity(top, target.toPath());
        entity.loaded(top, target);
        // its name may have changed
        save();
        return target;
    }

    // moves an inline entity, as it now is, into a file of its own
    public File moveEntityToFile(ProjectEntity entity) throws IOException
    {
        loadEntity(entity);
        if (!entity.isInline())
        {
            return entity.getLoadedFrom();
        }
        String base = newFileBase(entity.getName(), ENTITYEXTENSION);
        entity.setFile(base);
        entity.setInline(null);
        return writeEntityFile(entity, ElhVersions.nextVersion(this.folder, base, ENTITYEXTENSION));
    }

    // moves an entity, as it now is, into project.xml; its files are left in the folder
    public void moveEntityInline(ProjectEntity entity) throws IOException
    {
        ElhNode top = loadEntity(entity);
        entity.setFile(null);
        entity.setInline(top.toElement());
        entity.loaded(top, null);
        save();
    }

    // ---- events ----

    public List<ProjectEvent> getEvents()
    {
        return List.copyOf(this.events);
    }

    // the catalogue event with this id, or null
    public ProjectEvent findEvent(String id)
    {
        return this.events.stream().filter(e -> e.getId().equals(id)).findFirst().orElse(null);
    }

    // the catalogue event with this name, in any case, or null
    public ProjectEvent findEventByName(String eventname)
    {
        return this.events.stream().filter(e -> e.getName().equalsIgnoreCase(eventname)).findFirst().orElse(null);
    }

    // adds a new event, kept inline, or returns the one already of that name;
    // the caller saves the project
    public ProjectEvent addEvent(String eventname)
    {
        ProjectEvent existing = findEventByName(eventname);
        if (existing != null)
        {
            return existing;
        }
        ProjectEvent event = new ProjectEvent("e" + this.nexteventid++, eventname, "");
        this.events.add(event);
        return event;
    }

    // links to a deleted event are left broken: its id is never given to
    // another. Its files are left in the folder.
    public void removeEvent(ProjectEvent event)
    {
        this.events.remove(event);
    }

    public List<File> eventVersions(ProjectEvent event)
    {
        return event.isInline() ? List.of() : ElhVersions.versions(this.folder, event.getFile(), EVENTEXTENSION);
    }

    // reads the event's details from its file if they have not been read yet
    public void loadEvent(ProjectEvent event) throws IOException
    {
        if (event.isLoaded())
        {
            return;
        }
        List<File> versions = eventVersions(event);
        if (versions.isEmpty())
        {
            throw new IOException("There is no file for event " + event.getName() + " ("
                    + event.getFile() + "_v<N>" + EVENTEXTENSION + ") in " + this.folder);
        }
        Element root = MyelhFile.readRoot(versions.getLast().toPath());
        Element details = MyelhFile.findEventElement(root, event.getId());
        if (details == null && !root.getChildren(MyelhFile.EVENT).isEmpty())
        {
            details = root.getChildren(MyelhFile.EVENT).getFirst();
        }
        event.setAction(details == null ? "" : details.getChildTextTrim("action"));
        event.setLoaded();
    }

    // saves the event's name and details: inline, into project.xml;
    // otherwise over the latest version of its file
    public void saveEvent(ProjectEvent event) throws IOException
    {
        if (!event.isInline())
        {
            loadEvent(event);
            List<File> versions = eventVersions(event);
            File target = versions.isEmpty()
                    ? ElhVersions.versionFile(this.folder, event.getFile(), 1, EVENTEXTENSION) : versions.getLast();
            MyelhFile.write(target.toPath(), List.of(MyelhFile.eventElement(event)));
        }
        save();
    }

    // moves an inline event's details into a file of its own
    public File moveEventToFile(ProjectEvent event) throws IOException
    {
        if (!event.isInline())
        {
            List<File> versions = eventVersions(event);
            return versions.isEmpty() ? null : versions.getLast();
        }
        String base = newFileBase(event.getName(), EVENTEXTENSION);
        File target = ElhVersions.nextVersion(this.folder, base, EVENTEXTENSION);
        MyelhFile.write(target.toPath(), List.of(MyelhFile.eventElement(event)));
        event.setFile(base);
        save();
        return target;
    }

    // moves an event's details into project.xml; its files are left in the folder
    public void moveEventInline(ProjectEvent event) throws IOException
    {
        loadEvent(event);
        event.setFile(null);
        save();
    }
}
