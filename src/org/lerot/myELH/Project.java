package org.lerot.myELH;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

// A project is a folder holding a project.xml; while one is active every
// ELH is saved into its folder.
public final class Project
{
    static final String PROJECTFILE = "project.xml";
    private final File folder;
    private String name;
    private String author;
    // the project's event catalogue, in the order the events were added
    private final List<ProjectEvent> events = new ArrayList<>();
    // the number in the next event id (e1, e2, ...); saved, so ids are never reused
    private int nexteventid = 1;

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
        Element eventlist = root.getChild("events");
        if (eventlist != null)
        {
            for (Element event : eventlist.getChildren("event"))
            {
                String id = event.getAttributeValue("id");
                String eventname = event.getAttributeValue("name");
                if (id != null && eventname != null)
                {
                    project.events.add(new ProjectEvent(id, eventname, event.getChildTextTrim("description")));
                    if (id.matches("e\\d+"))
                    {
                        project.nexteventid = Math.max(project.nexteventid, Integer.parseInt(id.substring(1)) + 1);
                    }
                }
            }
            String nextid = eventlist.getAttributeValue("nextid");
            if (nextid != null && nextid.matches("\\d+"))
            {
                project.nexteventid = Math.max(project.nexteventid, Integer.parseInt(nextid));
            }
        }
        return project;
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

    public void save() throws IOException
    {
        Element root = new Element("project");
        root.addContent(new Element("name").setText(this.name));
        root.addContent(new Element("author").setText(this.author == null ? "" : this.author));
        Element eventlist = new Element("events");
        eventlist.setAttribute("nextid", String.valueOf(this.nexteventid));
        for (ProjectEvent event : this.events)
        {
            Element element = new Element("event");
            element.setAttribute("id", event.getId());
            element.setAttribute("name", event.getName());
            if (!event.getDescription().isEmpty())
            {
                element.addContent(new Element("description").setText(event.getDescription()));
            }
            eventlist.addContent(element);
        }
        root.addContent(eventlist);
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

    // adds a new event to the catalogue, or returns the one already of that name;
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

    // links to a deleted event are left broken: its id is never given to another
    public void removeEvent(ProjectEvent event)
    {
        this.events.remove(event);
    }
}
