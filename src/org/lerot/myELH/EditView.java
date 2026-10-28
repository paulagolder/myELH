package org.lerot.myELH;

import org.lerot.mywidgets.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

// The edit panel for the selected node, whatever its type
public class EditView extends jswVerticalPanel implements ActionListener, MouseListener
{
    private static final long serialVersionUID = 1L;
    private final jswTable namepanel;
    protected jswTextBox namefield;
    protected jswTable childpanel;
    protected jswDropDownBox typefield;
    protected jswTextBox stylefield;
    protected jswButton viewbutton;
    protected jswDropDownBox grouptypefield;
    protected JPanel bottompanel;
    // the project event a leaf event stands for; only there for a leaf event
    // while a project is open
    private jswDropDownBox eventfield;
    private static final String NOEVENT = "(none)";
    ElhNode activenode;
    private jswButton insertbutton;
    private jswButton editbutton;
    private jswButton insertabovebutton;
    private JPopupMenu popup;
    protected final MyElh app;

    public EditView(MyElh app)
    {
        super("editview", true, true);
        this.app = app;
        // setInsets(10, 10, 10, 10);
        setBorder(jswStyle.makeLineBorder());
        jswTheme.tint(this, jswTheme.EDITAREA);
        jswHorizontalPanel namebar = new jswHorizontalPanel();
        this.namepanel = new jswTable(this, "namepanel", MyElh.tablestyles());
        this.namefield = new jswTextBox(this, "namefield", 200, "namefield");
        jswLabel namelabel = new jswLabel("NAME:");
        this.namepanel.addCell(namelabel, 0, 0);
        this.namepanel.addCell((jswPanel) this.namefield, 0, 1);
        jswLabel typelabel = new jswLabel("TYPE:");
        String[] typelist = {"entitygroup"};
        this.typefield = new jswDropDownBox(null, "type");
        this.typefield.setEnabled(false);
        this.typefield.setList(typelist);
        this.typefield.setEnabled(true);
        this.typefield.setSelected(typelist[0]);
        this.namepanel.addCell(typelabel, 1, 0);
        this.namepanel.addCell((jswPanel) this.typefield, 1, 1);
        jswLabel grouptypelabel = new jswLabel("CHILDTYPE:");
        String[] grouptypeStrings = {"Group"};
        this.grouptypefield = new jswDropDownBox(null, "grouptype");
        this.grouptypefield.setEnabled(false);
        this.grouptypefield.setList(grouptypeStrings);
        //   this.grouptypefield.setSelected(0);
        this.grouptypefield.setEnabled(true);
        this.namepanel.addCell(grouptypelabel, 2, 0);
        this.namepanel.addCell((jswPanel) this.grouptypefield, 2, 1);
        jswLabel stylelabel = new jswLabel("STYLE:");
        this.stylefield = new jswTextBox(this, "stylefield", 200, "stylefield");
        this.namepanel.addCell(stylelabel, 3, 0);
        this.namepanel.addCell((jswPanel) this.stylefield, 3, 1);
        namebar.add(" MIDDLE", this.namepanel);
        add(namebar);
        jswHorizontalPanel childbar = new jswHorizontalPanel();
        this.childpanel = new jswTable(this, "childtable", MyElh.tablestyles2());
        childbar.add(" MIDDLE ", this.childpanel);
        add(childbar);
        this.bottompanel = new jswHorizontalPanel();
        add(" BOTTOM ", this.bottompanel);
    }

    public void setup(ElhNode anode)
    {
        this.activenode = app.activenode;
        this.namefield.setText(anode.getName());
        this.stylefield.setText(anode.getStylename());
        // the top elh stays an elh; any other node can become any other type
        this.typefield.setList(anode.is(NodeType.ELH) ? new String[]{anode.getType()} : NodeType.choiceNames());
        this.typefield.setSelected(anode.getType());
        this.grouptypefield.setList(groupingChoices(anode.getChildgrouptype()));
        this.grouptypefield.setSelected(anode.getChildgrouptype().xmlName());
        Project project = app.getProject();
        if (project != null && anode.isLeafEvent())
        {
            java.util.List<String> names = new java.util.ArrayList<>(java.util.List.of(NOEVENT));
            project.getEvents().forEach(e -> names.add(e.getName()));
            this.eventfield = new jswDropDownBox(null, "event");
            this.eventfield.setList(names.toArray(String[]::new));
            ProjectEvent linked = anode.getEventref() == null ? null : project.findEvent(anode.getEventref());
            this.eventfield.setSelected(linked == null ? NOEVENT : linked.getName());
            this.namepanel.addCell(new jswLabel("PROJECT EVENT:"), 4, 0);
            this.namepanel.addCell((jswPanel) this.eventfield, 4, 1);
        }
        makeChildPanel();
        makebuttonbar();
    }

    // the usual choices, plus the node's own grouping if it is not one of them (entitygroup)
    private static String[] groupingChoices(ChildGrouping current)
    {
        String[] choices = ChildGrouping.choiceNames();
        if (ChildGrouping.CHOICES.contains(current))
        {
            return choices;
        }
        String[] withcurrent = java.util.Arrays.copyOf(choices, choices.length + 1);
        withcurrent[choices.length] = current.xmlName();
        return withcurrent;
    }

    public void makebuttonbar()
    {
        jswHorizontalPanel buttonbar = new jswHorizontalPanel();
        this.insertbutton = new jswButton(this, "NEW CHILD", "NEWCHILD");
        //this.insertbutton.setParentnode(this.activenode);
        //  this.insertbutton.addMouseListener(this);
        buttonbar.add(this.insertbutton);
        this.insertabovebutton = new jswButton(this, "INSERT ABOVE CHILDREN");
        buttonbar.add(this.insertabovebutton);
        this.viewbutton = new jswButton(this, "VIEW PARENT");
        buttonbar.add(this.viewbutton);
        this.editbutton = new jswButton(this, "Save Edits");
        buttonbar.add(this.editbutton);
        buttonbar.setMinimumSize(new Dimension(600, 30));
        buttonbar.setPreferredSize(new Dimension(500, 30));
        this.bottompanel.add(" MIDDLE  ", buttonbar);
        if (this.activenode.getParent() == null)
        {
            this.viewbutton.setEnabled(false);
        }
        // a new event can only be put above the children of an event or entity
        if (!(this.activenode.is(NodeType.EVENT) || this.activenode.is(NodeType.ENTITY)))
        {
            this.insertabovebutton.setEnabled(false);
        }
    }

    public void actionPerformed(ActionEvent ae)
    {
        String comStr = ae.getActionCommand();
        actions(comStr, this.activenode);
    }

    public void makeChildPanel()
    {
        this.childpanel.setVisible(false);
        this.childpanel.removeAll();
        int count = activenode.countChildren();
        if (count < 1) return;
        int n = 0;
        for (ElhNode asubevent : activenode.getChildren())
        {
            addChildrow(asubevent, n, count);
            n++;
        }
        this.childpanel.setVisible(true);
    }

    public void addChildrow(ElhNode child, int n, int count)
    {
        String childname = child.getName();
        jswLabel sname = new jswLabel(childname);
        jswLabel stype = new jswLabel(child.getType());
        jswButton up = new jswButton(this, "UP");
        if (n == 0)
        {
            up.setEnabled(false);
        }
        jswButton down = new jswButton(this, "DOWN");
        if (n >= count - 1)
        {
            down.setEnabled(false);
        }
        jswButton delete = new jswButton(this, "DELETE");
        jswButton view = new jswButton(this, "VIEW");
        jswButton duplicate = new jswButton(this, "DUPLICATE");
        delete.setActionCommand("DELETE:" + childname);
        up.setActionCommand("UP:" + childname);
        down.setActionCommand("DOWN:" + childname);
        view.setActionCommand("VIEW:" + childname);
        duplicate.setActionCommand("DUPLICATE:" + childname);
        this.childpanel.addCell(sname, n, 1);
        this.childpanel.addCell(stype, n, 2);
        this.childpanel.addCell((jswPanel) up, n, 3);
        this.childpanel.addCell((jswPanel) down, n, 4);
        this.childpanel.addCell((jswPanel) view, n, 5);
        this.childpanel.addCell((jswPanel) duplicate, n, 6);
        this.childpanel.addCell((jswPanel) delete, n, 7);
    }

    public void actions(String comStr, ElhNode activenode)
    {
        if (comStr.equalsIgnoreCase("save edits"))
        {
            activenode.applyEdits(this.namefield.getText().trim(),
                    NodeType.fromXml(this.typefield.getSelectedValue()),
                    ChildGrouping.fromXml(this.grouptypefield.getSelectedValue()),
                    this.stylefield.getText().trim());
            if (this.eventfield != null)
            {
                ProjectEvent chosen = app.getProject().findEventByName(this.eventfield.getSelectedValue());
                activenode.setEventref(chosen == null ? null : chosen.getId());
            }
            // only events stand for project events
            if (!activenode.is(NodeType.EVENT))
            {
                activenode.setEventref(null);
            }
            app.maketree();
            app.currenttreeview.setselections(activenode);
            app.refresh();
        } else if (comStr.startsWith("VIEW:"))
        {
            String path = comStr.substring(5);
            ElhNode targetnode = activenode.findChildnode(path);
            this.activenode = targetnode;
            app.currenttreeview.setselections(targetnode);
            app.refresh();
        } else if (comStr.startsWith("UP:"))
        {
            String path = comStr.substring(3);
            ElhNode targetnode = activenode.findChildnode(path);
            app.currenttreeview.actions("LEFT", targetnode);
        } else if (comStr.startsWith("DELETE:"))
        {
            String path = comStr.substring(7);
            this.activenode.deleteChild(path);
            app.maketree();
            app.refresh();
        } else if (comStr.startsWith("DOWN:"))
        {
            String path = comStr.substring(5);
            ElhNode targetnode = activenode.findChildnode(path);
            app.currenttreeview.actions("RIGHT", targetnode);
        } else if (comStr.startsWith("NEWCHILD"))
        {
            this.popup = makePopup(this.activenode);
            this.popup.show(this, 50, 50);
        } else if (comStr.equalsIgnoreCase("NEWROLE") || comStr.equalsIgnoreCase("NEWEVENT")
                || comStr.equalsIgnoreCase("NEWENTITY"))
        {
            String type = comStr.substring(3).toLowerCase();
            if (type.equals(NodeType.EVENT.xmlName()))
            {
                if (app.addLeafEvent(name -> activenode.addChild(type, name)) == null) return;
            } else
            {
                String name = app.askNodeName(type);
                if (name == null) return;
                activenode.addChild(type, name);
            }
            app.maketree();
            app.refresh();
        } else if (comStr.equalsIgnoreCase("INSERT ABOVE CHILDREN"))
        {
            String name = app.askNodeName("event");
            if (name == null) return;
            activenode.insertabovechildren(name);
            app.maketree();
            app.refresh();
        } else
        {
            app.currenttreeview.actions(comStr, activenode);
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent)
    {
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent)
    {
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent)
    {
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent)
    {
    }

    public void mousePressed(MouseEvent me)
    {
        if (me.isPopupTrigger())
        {
            this.popup = makePopup(this.activenode);
            this.popup.show((JComponent) me.getSource(), me.getX(), me.getY());
        }
    }

    JPopupMenu makePopup(ElhNode currentnode)
    {
        JPopupMenu popup = new JPopupMenu();
        jswLabel ml = new jswLabel("Create new node");
        popup.add(ml);
        JMenuItem mi = new JMenuItem("Entity");
        mi.addActionListener(this);
        mi.setActionCommand("newentity");
        popup.add(mi);
        mi = new JMenuItem("Role");
        mi.addActionListener(this);
        mi.setActionCommand("newrole");
        popup.add(mi);
        mi = new JMenuItem("Event");
        mi.addActionListener(this);
        mi.setActionCommand("newevent");
        popup.add(mi);
        return popup;
    }
}