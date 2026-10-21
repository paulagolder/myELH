package org.lerot.myELH;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

// A node in an entity life history tree. The tree only holds the data: after
// changing it, the caller updates the screen.
public sealed abstract class elhnode permits elh, entity, entitygroup, event, role, rolegroup
{
    protected static String[] childtypes = {"entity", "event", "role"};
    protected static String[] childgroupingtypes = {"sequence", "option", "repetition", "rolegroup"};
    protected String childgrouptype;
    private List<elhnode> children;
    private String name;
    private elhnode parent;
    private String stylename;

    public elhnode(String aname, elhnode aparent)
    {
        this.name = aname;
        this.parent = aparent;
        this.children = new ArrayList<>();
    }

    protected static String[] getChildgroupingtypes()
    {
        return childgroupingtypes;
    }

    protected static String getDefaultChildgroupingtype()
    {
        return childgroupingtypes[0];
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
    public static elhnode makeNode(String typename, String aname, elhnode parent)
    {
        return switch (typename)
        {
            case "elh" -> new elh(aname, parent);
            case "entity" -> new entity(aname, parent);
            case "role" -> new role(aname, parent);
            case "event" -> new event(aname, parent);
            case "rolegroup" -> new rolegroup(aname, parent);
            case "entitygroup" -> new entitygroup(aname, parent);
            default ->
            {
                System.out.println(" Unknown type " + typename);
                yield null;
            }
        };
    }

    // a node for the user to fill in, named after its type
    public static elhnode makeNode(String typename, elhnode parent)
    {
        return makeNode(typename, "new " + typename, parent);
    }

    // puts a new event between this node and its children
    public void insertabovechildren()
    {
        event newevent = new event("newevent", this);
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

    public elhnode getParent()
    {
        return this.parent;
    }

    public void setParent(elhnode parent)
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

    public List<elhnode> getChildren()
    {
        return this.children;
    }

    public int countChildren()
    {
        return this.children.size();
    }

    public void moveup()
    {
        elhnode parent = getParent();
        if (parent == null || parent.getParent() == null) return;
        elhnode grandparent = parent.getParent();
        parent.children.remove(this);
        grandparent.children.add(this);
        setParent(grandparent);
    }

    public void moveleft()
    {
        if (getParent() == null) return;
        List<elhnode> siblings = getParent().children;
        int index = siblings.indexOf(this);
        if (index == 0) return;
        siblings.set(index, siblings.get(index - 1));
        siblings.set(index - 1, this);
    }

    public void moveright()
    {
        if (getParent() == null) return;
        List<elhnode> siblings = getParent().children;
        int index = siblings.indexOf(this);
        if (index == siblings.size() - 1) return;
        siblings.set(index, siblings.get(index + 1));
        siblings.set(index + 1, this);
    }

    public void deleteChildren()
    {
        for (elhnode achild : this.children)
        {
            achild.deleteChildren();
        }
        this.children = new ArrayList<>();
        System.out.println("deleted children " + this);
    }

    public void deleteChild(String childname)
    {
        elhnode delnode = findChildnode(childname);
        if (delnode != null)
        {
            delnode.deleteChildren();
            removeChild(delnode);
        }
    }

    void removeChild(elhnode delnode)
    {
        this.children.remove(delnode);
    }

    public void addChild(String type)
    {
        elhnode newnode = makeNode(type, this);
        if (newnode != null) addChild(newnode);
    }

    public void addChild(elhnode e)
    {
        this.children.add(e);
    }

    public void addSibling()
    {
        elhnode parent = getParent();
        if (parent == null) return;
        parent.addChild(new event("newnode", parent));
    }

    // this node and everything below it, as saved in an .elh file
    public Element toElement()
    {
        Element element = new Element(getType());
        element.setAttribute("name", getName() == null ? "" : getName());
        if (getChildgrouptype() != null)
        {
            element.setAttribute("childgrouptype", getChildgrouptype());
        }
        if (getStylename() != null)
        {
            element.setAttribute("stylename", getStylename());
        }
        for (elhnode achild : getChildren())
        {
            element.addContent(achild.toElement());
        }
        return element;
    }

    public String xmlout()
    {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(toElement());
    }

    public String getType()
    {
        return this.getClass().getSimpleName();
    }

    public elhnode findActiveEvent(String path)
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
        elhnode centity = findChildnode(centityname);
        elhnode cevent = null;
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

    public elhnode findChildnode(String sname)
    {
        for (elhnode anentity : getChildren())
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
        elhnode selnode = findChildnode(childname);
        if (selnode == null) return;
        if (!(this instanceof event || this instanceof entity || this instanceof elh)) return;
        elhnode newnode = selnode.copy(this);
        if (newnode == null) return;
        newnode.setName("Copyof:" + newnode.getName());
        addChild(newnode);
    }

    // a copy of this node and everything below it; elh nodes are not copied
    elhnode copy(elhnode newparent)
    {
        if (this instanceof elh) return null;
        elhnode newnode = makeNode(getType(), getName(), newparent);
        newnode.setChildgrouptype(getChildgrouptype());
        newnode.setStylename(getStylename());
        for (elhnode child : this.children)
        {
            elhnode childcopy = child.copy(newnode);
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
            elhnode anode = makeNode(nodetype, nodetype + " " + i, this);
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
            setChildgrouptype(ct.getAttribute("childgrouptype").getValue());
        } else
        {
            setChildgrouptype(getDefaultChildgroupingtype());
        }
        loadChildren(ct);
    }

    protected String getChildgrouptype()
    {
        return this.childgrouptype;
    }

    protected void setChildgrouptype(String childgrouptype)
    {
        this.childgrouptype = childgrouptype;
    }

    public String getPath()
    {
        String path = getName();
        for (elhnode anode = this; anode.getParent() != null; )
        {
            anode = anode.getParent();
            path = anode.getName() + "/" + path;
        }
        return path;
    }

    // moves all of anode's children to the end of this node's children
    public void moveChildrenFrom(elhnode anode)
    {
        List<elhnode> moving = anode.children;
        anode.children = new ArrayList<>();
        for (elhnode acnode : moving)
        {
            acnode.setParent(this);
            this.addChild(acnode);
        }
    }

    public static String makepath(elhnode node)
    {
        String treepath = "";
        for (elhnode pnode = node; pnode != null; pnode = pnode.getParent())
        {
            String nodeInfo = pnode.getName();
            treepath = nodeInfo + "/" + treepath;
        }
        return treepath;
    }
}
