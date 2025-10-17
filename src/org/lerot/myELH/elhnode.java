package org.lerot.myELH;

import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import org.jdom.Element;

public abstract class elhnode
{
   // static String[]   childtypes = { "event", "role" };
   // static String[]  childgroupingtypes = { "sequence", "option", "repetition", "rolegroup" };;
    protected static String[] childtypes = {"entity", "event", "role"};
    protected static String[] childgroupingtypes = {"sequence", "option", "repetition", "rolegroup"};
    protected String childgrouptype;
    private Vector<elhnode> children;
    private String name;
    private elhnode parent;
    private String stylename;
    private int id=0;

    public elhnode()
    {
        this.children = new Vector<>();
    }

    public elhnode(String aname, elhnode aparent)
    {
        this.name = aname;
        this.parent = aparent;
        this.children = new Vector<>();
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

    public void insertabovechildren()
    {
        myELHgui.mframe.currenttreeview.suspendtreeview();
        event newevent = new event("newevent", this);
        Vector<elhnode> oldchildren = getChildren();
        this.children = null;
        newevent.setChildren(oldchildren);
        this.children = new Vector<>();
        addChild(newevent);
        myELHgui.mframe.refresh();
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

    public elhnode findChildEvent(String sname)
    {
        for (elhnode anevent : this.children)
        {
            if (anevent.getName().equalsIgnoreCase(sname))
            {
                return anevent;
            }
        }
        return null;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String aname)
    {
        this.name = aname;
    }

    public Vector<elhnode> getChildren()
    {
        return this.children;
    }

    protected void setChildren(Vector<elhnode> somechildren)
    {
        this.children = somechildren;
    }

    public int countChildren()
    {
        if (this.children == null) return 0;
        return this.children.size();
    }

    public void moveup()
    {
        elhnode event = this;
        elhnode parent = event.getParent();
        elhnode grandparent = parent.getParent();
        Vector<elhnode> oldsiblings = parent.getChildren();
        Vector<elhnode> newsiblings = grandparent.getChildren();
        event.setParent(grandparent);
        newsiblings.add(event);
        grandparent.setChildren(newsiblings);
        oldsiblings.remove(event);
        parent.setChildren(oldsiblings);
    }

    public void moveleft()
    {
        elhnode event = this;
        elhnode parent = event.getParent();
        Vector<elhnode> siblings = parent.getChildren();
        int index = siblings.indexOf(event);
        if (index == 0) return;
        Vector<elhnode> newsiblings = new Vector<>(siblings);
        elhnode previous = siblings.get(index - 1);
        newsiblings.set(index, previous);
        newsiblings.set(index - 1, event);
        parent.setChildren(newsiblings);
    }

    public void moveright()
    {
        elhnode event = this;
        elhnode parent = event.getParent();
        Vector<elhnode> siblings = parent.getChildren();
        int index = siblings.indexOf(event);
        if (index == siblings.size()) return;
        Vector<elhnode> newsiblings = new Vector<>(siblings);
        elhnode next = siblings.get(index + 1);
        newsiblings.set(index, next);
        newsiblings.set(index + 1, event);
        parent.setChildren(newsiblings);
    }

    public void deleteChildren()
    {
        if (countChildren() > 0)
        {
            Vector<elhnode> entities = getChildren();
            for (elhnode anentity : entities)
            {
                anentity.deleteChildren();
                anentity = null;
            }
        }
        setChildren(null);
        System.out.println("deleted children " + this);
    }

    public void deleteChild(String childname)
    {
        elhnode delnode = findChildEvent(childname);
        if (delnode != null)
        {
            delnode.deleteChildren();
            removeChild(delnode);
        }
    }

    void removeChild(elhnode delnode)
    {
        this.children.remove(delnode);
        delnode = null;
    }

    public void movedown(String path)
    {
        Vector<elhnode> entities = getChildren();
        int count = entities.size();
        elhnode subevent = null;
        for (elhnode anentity : entities)
        {
            if (anentity.getName().equalsIgnoreCase(path))
            {
                subevent = anentity;
                break;
            }
        }
        if (subevent != null)
        {
            int i1 = entities.indexOf(subevent);
            if (i1 < count - 1)
            {
                elhnode next = entities.get(i1 + 1);
                entities.setElementAt(subevent, i1 + 1);
                entities.setElementAt(next, i1);
            }
        }
    }

    public void addChild(String type)
    {
        elhnode newnode = makeNode(type,this);
        Vector<elhnode> siblings = getChildren();
        siblings.add(newnode);
        setChildren(siblings);
    }

    public static  elhnode makeNode(String typename, elhnode parent)
    {
        elhnode newnode = null;
        switch (typename)
        {
            case "entity":
                newnode = new entity("new entity", parent);
                break;
            case "role":
                newnode = new role("new role", parent);
                break;
            case "event":
                newnode = new event("new event", parent);
                break;
            case "rolegroup":
                newnode = new rolegroup("new rolegroup", parent);
                break;
            case "entitygroup":
                newnode = new entitygroup("new entitygroup", parent);
                break;
            default:
                System.out.println(" Unknown type " + typename);
                return null;
        }
        return newnode;
    }

    public void addChild(elhnode e)
    {
        this.children.add(e);
    }

    public void addSibling()
    {
        elhnode parent = getParent();
        elhnode newnode = new event("newnode", this);
        Vector<elhnode> siblings = parent.getChildren();
        siblings.add(newnode);
        parent.setChildren(siblings);
    }

    public String xmlout() {
        StringBuilder out = new StringBuilder();
        String nodeclass= getType();
        out.append("<").append(nodeclass).append(" name=\"").append(getName()).append("\"");
        if (getChildgrouptype() != null) {
            out.append(" childgrouptype=\"").append(getChildgrouptype()).append("\"");
        }
        if (getStylename() != null) {
            out.append(" stylename=\"").append(getStylename()).append("\"");
        }
        if(!getChildren().isEmpty())
        {
            out.append(" > \n");
            for (elhnode achild : getChildren())
            {
                out.append(achild.xmlout());
            }
            out.append("</").append(nodeclass).append(">\n");
        }else
        {
            out.append(" /> \n");
        }

        return out.toString();
    }

    public String getType()
    {
        return this.getClass().getSimpleName();
    }

    public elhnode findActiveEvent(String path)
    {
        String str = path;
        StringTokenizer st = new StringTokenizer(str, "/");
        if (st != null && st.hasMoreElements())
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

    public void duplicate(String path)
    {
        elhnode selnode = null;
        Vector<elhnode> entities = getChildren();
        for (elhnode anentity : entities)
        {
            if (anentity.getName().equalsIgnoreCase(path))
            {
                selnode = anentity;
                break;
            }
        }
        if (selnode != null)
        {
            elhnode newnode = selnode.copy();
            newnode.setName("Copyof:" + newnode.getName());
            if (this instanceof event)
            {
                this.addChild(newnode);
            } else if (this instanceof entity)
            {
                this.addChild(newnode);
            } else if (this instanceof elh)
            {
                this.addChild(newnode);
            }
            myELHgui.mframe.refresh();
        }
    }

    private elhnode copy()
    {
        elhnode newnode = null;
        if (this instanceof event)
        {
            newnode = new event(getName(), this.parent);
        } else if (this instanceof entity)
        {
            newnode = new entity(getName(), this.parent);
        } else if (newnode instanceof elh)
        {
            return null;
        }
        for (elhnode child : this.children)
        {
            elhnode childcopy = child.copy();
            newnode.addChild(childcopy);
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
        if (nl != null && !nl.isEmpty())
        {
            for (int i = 0; i < nl.size(); i++)
            {
                Element at = nl.get(i);
                String nodetype = at.getName();
                //  System.out.println(" nodename =" + nodetype);
                String str;
                switch (nodetype)
                {
                    case "elh":
                        elh anelh = new elh("elh " + i, this);
                        anelh.loadXML(at);
                        addChild(anelh);
                        break;
                    case "entity":
                        entity anentity = new entity("entity " + i, this);
                        anentity.loadXML(at);
                        addChild(anentity);
                        break;
                    case "role":
                        role arole = new role("role " + i, this);
                        arole.loadXML(at);
                        addChild(arole);
                        break;
                    case "event":
                        event anevent = new event("event " + i, this);
                        anevent.loadXML(at);
                        addChild(anevent);
                        break;
                    case "rolegroup":
                        rolegroup arolegroup = new rolegroup("rolegroup " + i, this);
                        arolegroup.loadXML(at);
                        addChild(arolegroup);
                        break;
                    default:
                        System.out.println(" unknown nodename =" + nodetype);
                        break;
                }
            }
        }
    }

    public void loadXML(Element ct)
    {
        String nodetype = ct.getName();
        setName(ct.getAttribute("name").getValue());
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

    public void moveChildrenFrom(elhnode anode)
    {
        for(elhnode acnode : anode.getChildren())
        {
            this.addChild(acnode);
        }
        anode.children=null;
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

