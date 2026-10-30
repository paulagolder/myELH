package org.lerot.myELH;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

// A node in an entity life history tree. The tree only holds the data: after
// changing it, the caller updates the screen.
public final class ElhNode
{
    private NodeType type;
    protected ChildGrouping childgrouptype;
    private List<ElhNode> children;
    private String name;
    private ElhNode parent;
    // the id of the project event a leaf action is part of, or null
    private String eventref;
    // for a leaf action, the object's own part in the event (its CORD Action):
    // what must be true of it beforehand, and what the event does to it
    private String condition = "";
    private String result = "";
    // for an action, what it is, in the user's words
    private String description = "";
    // for an entity or a role, the data held about it
    private List<Attribute> attributes = new ArrayList<>();

    // the element holding a node's attributes, among its child nodes
    static final String ATTRIBUTES = "attributes";

    public ElhNode(NodeType type, String aname, ElhNode aparent)
    {
        this.type = type;
        this.childgrouptype = type.defaultgrouping;
        this.name = aname;
        this.parent = aparent;
        this.children = new ArrayList<>();
    }

    public static String getDefaultChildtype()
    {
        return NodeType.ENTITY.xmlName();
    }

    // a new node of the given type, or null if the type is not known
    public static ElhNode makeNode(String typename, String aname, ElhNode parent)
    {
        NodeType type = NodeType.fromXml(typename);
        if (type == null)
        {
            System.out.println(" Unknown type " + typename);
            return null;
        }
        return new ElhNode(type, aname, parent);
    }

    public NodeType getNodeType()
    {
        return this.type;
    }

    public void setNodeType(NodeType type)
    {
        this.type = type;
    }

    public boolean is(NodeType atype)
    {
        return this.type == atype;
    }

    // the edit panel's Save Edits. When the type changes, a structure left
    // as it was follows the new type: an action made a rolegroup gets the
    // parallel roles structure. How a node is drawn follows from its type.
    public void applyEdits(String aname, NodeType newtype, ChildGrouping grouping)
    {
        if (newtype != null && newtype != this.type)
        {
            if (grouping == this.childgrouptype)
            {
                grouping = newtype.defaultgrouping;
            }
            this.type = newtype;
        }
        this.name = aname;
        this.childgrouptype = grouping;
    }

    // a node for the user to fill in, named after its type
    public static ElhNode makeNode(String typename, ElhNode parent)
    {
        return makeNode(typename, "new " + typename, parent);
    }

    // puts a new action node between this node and its children
    public void insertabovechildren(String aname)
    {
        ElhNode newaction = new ElhNode(NodeType.ACTION, aname, this);
        newaction.moveChildrenFrom(this);
        addChild(newaction);
    }

    public String toLongString()
    {
        return this.name + ":" + this.getType() + ":" + this.childgrouptype;
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
            deleteChild(delnode);
        }
    }

    // removes the child and everything below it
    public void deleteChild(ElhNode delnode)
    {
        delnode.deleteChildren();
        removeChild(delnode);
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

    // the new node, or null if the type is not known
    public ElhNode addChild(String type, String aname)
    {
        ElhNode newnode = makeNode(type, aname, this);
        if (newnode != null) addChild(newnode);
        return newnode;
    }

    public void addChild(ElhNode e)
    {
        this.children.add(e);
    }

    // the new action, or null if this node is the top and so has no siblings
    public ElhNode addSibling(String aname)
    {
        ElhNode parent = getParent();
        if (parent == null) return null;
        ElhNode sibling = new ElhNode(NodeType.ACTION, aname, parent);
        parent.addChild(sibling);
        return sibling;
    }

    // a leaf action node is an action, an entity's part in an event; action
    // nodes with children only group the actions below them
    public boolean isLeafAction()
    {
        return is(NodeType.ACTION) && this.children.isEmpty();
    }

    public String getCondition()
    {
        return this.condition;
    }

    public void setCondition(String condition)
    {
        this.condition = condition == null ? "" : condition.trim();
    }

    public String getResult()
    {
        return this.result;
    }

    public void setResult(String result)
    {
        this.result = result == null ? "" : result.trim();
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description == null ? "" : description.strip();
    }

    public List<Attribute> getAttributes()
    {
        return List.copyOf(this.attributes);
    }

    public void setAttributes(List<Attribute> attributes)
    {
        this.attributes = new ArrayList<>(attributes);
    }

    public String getEventref()
    {
        return this.eventref;
    }

    public void setEventref(String eventref)
    {
        this.eventref = eventref == null || eventref.isEmpty() ? null : eventref;
    }

    // every node in this tree linked to a project event: event id -> the
    // names the nodes go by here
    public void collectEventrefs(Map<String, List<String>> found)
    {
        if (this.eventref != null)
        {
            found.computeIfAbsent(this.eventref, k -> new ArrayList<>()).add(this.name);
        }
        for (ElhNode achild : this.children)
        {
            achild.collectEventrefs(found);
        }
    }

    // the nodes in this tree linked to the project event with this id
    public void findLinked(String ref, List<ElhNode> found)
    {
        if (ref.equals(this.eventref))
        {
            found.add(this);
        }
        for (ElhNode achild : this.children)
        {
            achild.findLinked(ref, found);
        }
    }

    // where this node is in its tree: the child index at each level down from the top
    public List<Integer> indexPath()
    {
        List<Integer> path = new ArrayList<>();
        for (ElhNode anode = this; anode.parent != null; anode = anode.parent)
        {
            path.addFirst(anode.parent.children.indexOf(anode));
        }
        return path;
    }

    // the node at an indexPath() below this one, or null if the tree has no such node
    public ElhNode nodeAtIndexPath(List<Integer> path)
    {
        ElhNode anode = this;
        for (int index : path)
        {
            if (index < 0 || index >= anode.children.size()) return null;
            anode = anode.children.get(index);
        }
        return anode;
    }

    // every action in this tree, in order
    public void collectActions(List<ElhNode> found)
    {
        if (isLeafAction())
        {
            found.add(this);
        }
        for (ElhNode achild : this.children)
        {
            achild.collectActions(found);
        }
    }

    // this node and everything below it, as saved in an .elh file
    public Element toElement()
    {
        Element element = new Element(getType());
        element.setAttribute("name", getName() == null ? "" : getName());
        if (!this.attributes.isEmpty())
        {
            Element list = new Element(ATTRIBUTES);
            this.attributes.forEach(a -> list.addContent(a.toElement()));
            element.addContent(list);
        }
        if (getChildgrouptype() != null)
        {
            element.setAttribute("childgrouptype", getChildgrouptype().xmlName());
        }
        if (this.eventref != null)
        {
            element.setAttribute("eventref", this.eventref);
        }
        if (!this.condition.isEmpty())
        {
            element.setAttribute("condition", this.condition);
        }
        if (!this.result.isEmpty())
        {
            element.setAttribute("result", this.result);
        }
        if (!this.description.isEmpty())
        {
            element.setAttribute("description", this.description);
        }
        for (ElhNode achild : getChildren())
        {
            element.addContent(achild.toElement());
        }
        return element;
    }

    // the shape of the tree below this node, ignoring names and other text:
    // it changes when nodes are added, removed or moved
    public String structure()
    {
        StringBuilder sb = new StringBuilder(getType());
        if (!this.children.isEmpty())
        {
            sb.append('(');
            for (ElhNode achild : this.children)
            {
                sb.append(achild.structure()).append(' ');
            }
            sb.append(')');
        }
        return sb.toString();
    }

    public String xmlout()
    {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(toElement());
    }

    // the node type as written in .elh files ("entity", "rolegroup", ...);
    // also used to look up the node's drawing style
    public String getType()
    {
        return this.type.xmlName();
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
        if (selnode != null) duplicateChild(selnode);
    }

    // only the children of events, entities and elhs can be duplicated
    public boolean canDuplicateChildren()
    {
        return is(NodeType.ACTION) || is(NodeType.ENTITY) || is(NodeType.ELH);
    }

    // adds a copy of the child, and everything below it, as a further child
    public void duplicateChild(ElhNode selnode)
    {
        if (!canDuplicateChildren()) return;
        ElhNode newnode = selnode.copy(this);
        if (newnode == null) return;
        newnode.setName("Copyof:" + newnode.getName());
        addChild(newnode);
    }

    // a copy of this node and everything below it; elh nodes are not copied
    ElhNode copy(ElhNode newparent)
    {
        if (is(NodeType.ELH)) return null;
        ElhNode newnode = new ElhNode(this.type, getName(), newparent);
        newnode.setChildgrouptype(getChildgrouptype());
        newnode.setAttributes(this.attributes);
        newnode.setEventref(this.eventref);
        newnode.setCondition(this.condition);
        newnode.setResult(this.result);
        newnode.setDescription(this.description);
        for (ElhNode child : this.children)
        {
            ElhNode childcopy = child.copy(newnode);
            if (childcopy != null) newnode.addChild(childcopy);
        }
        return newnode;
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
            if (nodetype.equals(ATTRIBUTES))
            {
                this.attributes = new ArrayList<>(at.getChildren("attribute").stream().map(Attribute::fromElement).toList());
                continue;
            }
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
        setName(ct.getAttributeValue("name", getName()));
        setEventref(ct.getAttributeValue("eventref"));
        setCondition(ct.getAttributeValue("condition"));
        setResult(ct.getAttributeValue("result"));
        setDescription(ct.getAttributeValue("description"));
        // a stylename attribute in older files is ignored: a node is drawn by its type
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
}
