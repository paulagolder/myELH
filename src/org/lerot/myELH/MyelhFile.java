package org.lerot.myELH;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

// The <myelh> wrapper that holds a project's objects: one entity in an
// entity's .elh file, one event in an event's .event file, or any number of
// either in project.xml's inline section. The wrapper itself is never part
// of the tree.
public final class MyelhFile
{
    static final String WRAPPER = "myelh";
    static final String EVENT = "event";

    private MyelhFile()
    {
    }

    // true if the file's top element is <myelh>; old ELH files are not
    public static boolean isMyelh(File file)
    {
        try
        {
            return WRAPPER.equals(new SAXBuilder().build(file).getRootElement().getName());
        } catch (JDOMException | IOException e)
        {
            return false;
        }
    }

    public static Element readRoot(Path file) throws IOException
    {
        try
        {
            return new SAXBuilder().build(file.toFile()).getRootElement();
        } catch (JDOMException e)
        {
            throw new IOException(e.getMessage(), e);
        }
    }

    // writes the elements inside a <myelh> wrapper
    public static void write(Path file, List<Element> contents) throws IOException
    {
        Element wrapper = new Element(WRAPPER);
        for (Element element : contents)
        {
            wrapper.addContent(element.clone());
        }
        try (Writer out = Files.newBufferedWriter(file))
        {
            new XMLOutputter(Format.getPrettyFormat()).output(new Document(wrapper), out);
        }
    }

    public static void writeEntity(ElhNode top, Path file) throws IOException
    {
        write(file, List.of(top.toElement()));
    }

    // the life history elements a wrapper holds: everything but events
    public static List<Element> entityElements(Element wrapper)
    {
        List<Element> found = new ArrayList<>();
        for (Element child : wrapper.getChildren())
        {
            if (!isEventDetails(child))
            {
                found.add(child);
            }
        }
        return found;
    }

    // an <event> directly inside a wrapper is an event's details, not a life
    // history: it has an id, and no child nodes other than its description
    // (called its action in files saved before actions were the entities' part)
    static boolean isEventDetails(Element element)
    {
        return element.getName().equals(EVENT) && element.getAttribute("id") != null
                && element.getChildren().stream().allMatch(c -> c.getName().equals("description")
                || c.getName().equals("process") || c.getName().equals("action"));
    }

    // an event's description from its details, in either form
    static String eventDescription(Element details)
    {
        String text = details.getChildTextTrim("description");
        return text != null ? text : details.getChildTextTrim("action");
    }

    // the life history element with this name, or the only one if none has it
    public static Element entityElement(Element wrapper, String name)
    {
        List<Element> entities = entityElements(wrapper);
        for (Element element : entities)
        {
            if (name.equalsIgnoreCase(element.getAttributeValue("name")))
            {
                return element;
            }
        }
        return entities.size() == 1 ? entities.getFirst() : null;
    }

    // the top node of an entity's life history, built from its element
    public static ElhNode entityFromElement(Element element)
    {
        NodeType type = NodeType.fromXml(element.getName());
        ElhNode top = new ElhNode(type == null ? NodeType.ENTITY : type, element.getAttributeValue("name"), null);
        top.loadXML(element);
        return top;
    }

    // reads the named entity from its file. An old ELH file (an <elh> top
    // rather than <myelh>) is read too: the entity is the child of that name.
    public static ElhNode readEntity(Path file, String name) throws IOException
    {
        Element root = readRoot(file);
        if (!root.getName().equals(WRAPPER))
        {
            ElhNode old = ElhFile.read(file);
            for (ElhNode child : old.getChildren())
            {
                if (child.getName().equalsIgnoreCase(name))
                {
                    child.setParent(null);
                    return child;
                }
            }
            throw new IOException(file.getFileName() + " has no entity called " + name);
        }
        Element element = entityElement(root, name);
        if (element == null)
        {
            throw new IOException(file.getFileName() + " has no entity called " + name);
        }
        return entityFromElement(element);
    }

    // an event's process, as written; empty if it has none
    static String eventProcess(Element details)
    {
        Element process = details.getChild("process");
        return process == null ? "" : process.getText();
    }

    // an event's details: <event id="e2" name="Borrow"><description>...</description>
    // <process>...</process></event>
    public static Element eventElement(ProjectEvent event)
    {
        Element element = new Element(EVENT);
        element.setAttribute("id", event.getId());
        element.setAttribute("name", event.getName());
        if (!event.getDescription().isEmpty())
        {
            element.addContent(new Element("description").setText(event.getDescription()));
        }
        if (!event.getProcess().isEmpty())
        {
            // kept as written: its indentation matters
            element.addContent(new Element("process")
                    .setAttribute("space", "preserve", org.jdom2.Namespace.XML_NAMESPACE)
                    .setText(event.getProcess()));
        }
        return element;
    }

    // the details of the event with this id among a wrapper's elements, or null
    public static Element findEventElement(Element wrapper, String id)
    {
        for (Element child : wrapper.getChildren(EVENT))
        {
            if (id.equals(child.getAttributeValue("id")) && isEventDetails(child))
            {
                return child;
            }
        }
        return null;
    }
}
