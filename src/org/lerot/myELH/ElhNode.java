package org.lerot.myELH;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

// A node in an entity life history tree. The tree only holds the data: after
// changing it, the caller updates the screen.
public sealed abstract class ElhNode permits Elh, Entity, EntityGroup, Event, Role, RoleGroup
{
    protected static String[] childtypes = {"entity", "event", "role"};
    protected ChildGrouping childgrouptype;
    private List<ElhNode> children;
    private String name;
    private ElhNode parent;
    private String stylename;

    public ElhNode(String aname, ElhNode aparent)
    {
        this.name = aname;
        this.parent = aparent;
        this.children = new ArrayList<>();
    }

    protected static String[] getChildtypes()
    {
        return childtypes;
    }

    public static String getDefaultChildtype()
    {
        return childtypes[0];
    }

    // a new node of the given type, or null if the type is not known
    public static ElhNode makeNode(String typename, String aname, ElhNode parent)
    {
        return switch (typename)
        {
            case "elh" -> new Elh(aname, parent);
            case "entity" -> new Entity(aname, parent);
            case "role" -> new Role(aname, parent);
            case "event" -> new Event(aname, parent);
            case "rolegroup" -> new RoleGroup(aname, parent);
            case "entitygroup" -> new EntityGroup(aname, parent);
            default ->
            {
                System.out.println(" Unknown type " + typename);
                yield null;
            }
        };
    }

    // a node for the user to fill in, named after its type
    public static ElhNode makeNode(String typename, ElhNode parent)
    {
        return makeNode(typename, "new " + typename, parent);
    }

    // puts a new event between this node and its children
    public void insertabovechildren()
    {
        Event newevent = new Event("newevent", this);
        newevent.moveChildrenFrom(this);
        addChild(newevent);
    }

    public String toLongString()
    {
        return this.name + ":" + this.getType() + ":" + this.childgrouptype + ":" + this.stylename;
    }

    public String toString()
    {
        return this.name;
    }

    public ElhNode getParent()
    {
        return this.parent;
    }

    public void setParent(ElhNode parent)
    {
        this.parent = parent;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String aname)
    {
        this.name = aname;
    }

    public List<ElhNode> getChildren()
    {
        return this.children;
    }

    public int countChildren()
    {
        return this.children.size();
    }

    public void moveup()
    {
        ElhNode parent = getParent();
        if (parent == null || parent.getParent() == null) return;
        ElhNode grandparent = parent.getParent();
        parent.children.remove(this);
        grandparent.children.add(this);
        setParent(grandparent);
    }

    public void moveleft()
    {
        if (getParent() == null) return;
        List<ElhNode> siblings = getParent().children;
        int index = siblings.indexOf(this);
        if (index == 0) return;
        siblings.set(index, siblings.get(index - 1));
        siblings.set(index - 1, this);
    }

    public void moveright()
    {
        if (getParent() == null) return;
        List<ElhNode> siblings = getParent().children;
        int index = siblings.indexOf(this);
        if (index == siblings.size() - 1) return;
        siblings.set(index, siblings.get(index + 1));
        siblings.set(index + 1, this);
    }

    public void deleteChildren()
    {
        for (ElhNode achild : this.children)
        {
            achild.deleteChildren();
        }
        this.children = new ArrayList<>();
    }

    public void deleteChild(String childname)
    {
        ElhNode delnode = findChildnode(childname);
        if (delnode != null)
        {
            delnode.deleteChildren();
            removeChild(delnode);
        }
    }

    void removeChild(ElhNode delnode)
    {
        this.children.remove(delnode);
    }

    public void addChild(String type)
    {
        ElhNode newnode = makeNode(type, this);
        if (newnode != null) addChild(newnode);
    }

    public void addChild(ElhNode e)
    {
        this.children.add(e);
    }

    public void addSibling()
    {
        ElhNode parent = getParent();
        if (parent == null) return;
        parent.addChild(new Event("newnode", parent));
    }

    // this node and everything below it, as saved in an .elh file
    public Element toElement()
    {
        Element element = new Element(getType());
        element.setAttribute("name", getName() == null ? "" : getName());
        if (getChildgrouptype() != null)
        {
            element.setAttribute("childgrouptype", getChildgrouptype().xmlName());
        }
        if (getStylename() != null)
        {
            element.setAttribute("stylename", getStylename());
        }
        for (ElhNode achild : getChildren())
        {
            element.addContent(achild.toElement());
        }
        return element;
    }

    public String xmlout()
    {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(toElement());
    }

    // the node type as written in .elh files ("entity", "rolegroup", ...);
    // also used to look up the node's drawing style
    public String getType()
    {
        return this.getClass().getSimpleName().toLowerCase();
    }

    public ElhNode findActiveEvent(String path)
    {
        StringTokenizer st = new StringTokenizer(path, "/");
        if (st.hasMoreElements())
        {
            String cfilename = (String) st.nextElement();
            if (!cfilename.equalsIgnoreCase(getName()))
            {
                System.out.println(" Error in ELH path " + cfilename + "!=" + getName());
                System.out.println(" Error in ELH path "+path);
                return null;
            }
        }
        if (!st.hasMoreElements())
        {
            return this;
        }
        String centityname = (String) st.nextElement();
        ElhNode centity = findChildnode(centityname);
        ElhNode cevent = null;
        while (centity != null && st.hasMoreElements())
        {
            cevent = centity;
            String token = (String) st.nextElement();
            centity = centity.findChildnode(token);
        }
        if (centity == null)
        {
            return cevent;
        }
        return centity;
    }

    public ElhNode findChildnode(String sname)
    {
        for (ElhNode anentity : getChildren())
        {
            if (anentity.getName().equalsIgnoreCase(sname))
            {
                return anentity;
            }
        }
        return null;
    }

    // copies the named child, adding the copy as a further child
    public void duplicate(String childname)
    {
        ElhNode selnode = findChildnode(childname);
        if (selnode == null) return;
        if (!(this instanceof Event || this instanceof Entity || this instanceof Elh)) return;
        ElhNode newnode = selnode.copy(this);
        if (newnode == null) return;
        newnode.setName("Copyof:" + newnode.getName());
        addChild(newnode);
    }

    // a copy of this node and everything below it; elh nodes are not copied
    ElhNode copy(ElhNode newparent)
    {
        if (this instanceof Elh) return null;
        ElhNode newnode = makeNode(getType(), getName(), newparent);
        newnode.setChildgrouptype(getChildgrouptype());
        newnode.setStylename(getStylename());
        for (ElhNode child : this.children)
        {
            ElhNode childcopy = child.copy(newnode);
            if (childcopy != null) newnode.addChild(childcopy);
        }
        return newnode;
    }

    String getStylename()
    {
        return this.stylename;
    }

    void setStylename(String astylename)
    {
        this.stylename = astylename;
    }

    public void mark()
    {
    }

    public void loadChildren(Element ct)
    {
        List<Element> nl = ct.getChildren();
        for (int i = 0; i < nl.size(); i++)
        {
            Element at = nl.get(i);
            String nodetype = at.getName();
            ElhNode anode = makeNode(nodetype, nodetype + " " + i, this);
            if (anode == null)
            {
                System.out.println(" unknown nodename =" + nodetype);
                continue;
            }
            anode.loadXML(at);
            addChild(anode);
        }
    }

    public void loadXML(Element ct)
    {
        String nodetype = ct.getName();
        setName(ct.getAttributeValue("name", getName()));
        if (ct.getAttribute("stylename") != null)
        {
            setStylename(ct.getAttribute("stylename").getValue());
        } else
        {
            setStylename(nodetype);
        }
        if (ct.getAttribute("childgrouptype") != null)
        {
            setChildgrouptype(ChildGrouping.fromXml(ct.getAttribute("childgrouptype").getValue()));
        } else
        {
            setChildgrouptype(ChildGrouping.SEQUENCE);
        }
        loadChildren(ct);
    }

    protected ChildGrouping getChildgrouptype()
    {
        return this.childgrouptype;
    }

    protected void setChildgrouptype(ChildGrouping childgrouptype)
    {
        this.childgrouptype = childgrouptype;
    }

    public String getPath()
    {
        String path = getName();
        for (ElhNode anode = this; anode.getParent() != null; )
        {
            anode = anode.getParent();
            path = anode.getName() + "/" + path;
        }
        return path;
    }

    // moves all of anode's children to the end of this node's children
    public void moveChildrenFrom(ElhNode anode)
    {
        List<ElhNode> moving = anode.children;
        anode.children = new ArrayList<>();
        for (ElhNode acnode : moving)
        {
            acnode.setParent(this);
            this.addChild(acnode);
        }
    }

    public static String makepath(ElhNode node)
    {
        String treepath = "";
        for (ElhNode pnode = node; pnode != null; pnode = pnode.getParent())
        {
            String nodeInfo = pnode.getName();
            treepath = nodeInfo + "/" + treepath;
        }
        return treepath;
    }
}
