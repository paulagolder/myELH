package org.lerot.myELH;

import org.lerot.mywidgets.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public abstract class editview extends jswVerticalPanel implements ActionListener, MouseListener
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
    elhnode activenode;
    private jswButton insertbutton;
    private jswButton editbutton;
    private jswButton insertabovebutton;
    private JPopupMenu popup;

    public editview()
    {
        super("editview", true, true);
        // setInsets(10, 10, 10, 10);
        setBorder(new LineBorder(Color.GREEN, 2));
        jswHorizontalPanel namebar = new jswHorizontalPanel();
        this.namepanel = new jswTable(this, "namepanel", myELHgui.tablestyles());
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
        this.childpanel = new jswTable(this, "childtable", myELHgui.tablestyles2());
        this.childpanel.setBackground(Color.green);
        childbar.add(" MIDDLE ", this.childpanel);
        add(childbar);
        this.bottompanel = new jswHorizontalPanel();
        add(" BOTTOM ", this.bottompanel);
    }

    public void setup(elhnode anode)
    {
        System.out.println(" in set up  " + anode.toLongString());
        this.activenode = myELHgui.mframe.activenode;
        this.namefield.setText(anode.getName());
        this.stylefield.setText(anode.getStylename());
        // this.grouptypefield.setSelected(anode.getGrouping());
        //  this.typefield.setSelected(anode.gettype());
    }

    public void makebuttonbar()
    {
        jswHorizontalPanel buttonbar = new jswHorizontalPanel();
        this.insertbutton = new jswButton(this, "NEW CHILD", "NEWCHILD");
        //this.insertbutton.setParentnode(this.activenode);
        //  this.insertbutton.addMouseListener(this);
        buttonbar.add(this.insertbutton);
        this.insertabovebutton = new jswButton(this, "INSERT ABOVE CHILDREN");
        this.viewbutton = new jswButton(this, "VIEW PARENT");
        buttonbar.add(this.viewbutton);
        this.editbutton = new jswButton(this, "Save Edits");
        buttonbar.add(this.editbutton);
        buttonbar.setMinimumSize(new Dimension(600, 30));
        buttonbar.setPreferredSize(new Dimension(500, 30));
        this.bottompanel.add(" MIDDLE  ", buttonbar);
        if (this.activenode.getParent() == null)
        {
            this.insertabovebutton.setEnabled(false);
            this.viewbutton.setEnabled(false);
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
        for (elhnode asubevent : activenode.getChildren())
        {
            addChildrow(asubevent, n, count);
            n++;
        }
        this.childpanel.setVisible(true);
    }

    public void addChildrow(elhnode child, int n, int count)
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

    public void actions(String comStr, elhnode activenode)
    {
        System.out.println(comStr + " action in edit view");
        if (comStr.equalsIgnoreCase("save edits"))
        {
            if (!(this.typefield.getSelectedValue().equalsIgnoreCase(activenode.getType())))
            {
                elhnode newnode = elhnode.makeNode(this.typefield.getSelectedValue(), activenode.getParent());
                newnode.moveChildrenFrom(activenode);
                newnode.setName(this.namefield.getText().trim()+"-new");
                newnode.setChildgrouptype(this.grouptypefield.getSelectedValue());
                newnode.setStylename(this.stylefield.getText().trim());
                activenode.getParent().addChild(newnode);
                System.out.println(" edited +" + newnode.toLongString());
            }else
            {
                elhnode selectedelhnode = (elhnode) myELHgui.mframe.activetreenode.getUserObject();
                selectedelhnode.setName(this.namefield.getText().trim());
                selectedelhnode.setChildgrouptype(this.grouptypefield.getSelectedValue());
                selectedelhnode.setStylename(this.stylefield.getText().trim());
                System.out.println(" edited -" + activenode.toLongString());
            }
            myELHgui.mframe.maketree();
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("VIEW:"))
        {
            System.out.println(comStr + " action");
            String path = comStr.substring(5);
            elhnode targetnode = activenode.findChildnode(path);
            System.out.println(comStr + " action " + path + " " + targetnode);
            this.activenode = targetnode;
            myELHgui.mframe.currentview.setselections(targetnode);
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("UP:"))
        {
            System.out.println(comStr + " action");
            String path = comStr.substring(3);
            elhnode targetnode = activenode.findChildnode(path);
            myELHgui.mframe.currentview.actions("LEFT", targetnode);
        } else if (comStr.startsWith("DELETE:"))
        {
            System.out.println(comStr + " action");
            String path = comStr.substring(7);
            this.activenode.deleteChild(path);
            myELHgui.mframe.maketree();
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("DOWN:"))
        {
            System.out.println(comStr + " action");
            String path = comStr.substring(5);
            elhnode targetnode = activenode.findChildnode(path);
            myELHgui.mframe.currentview.actions("RIGHT", targetnode);
        } else if (comStr.startsWith("NEWCHILD"))
        {
            this.popup = makePopup(this.activenode);
            this.popup.show(this, 50, 50);
        } else if (comStr.startsWith("NEWROLE"))
        {
            activenode.addChild("role");
        } else if (comStr.startsWith("NEWEVENT"))
        {
            activenode.addChild("event");
        } else if (comStr.startsWith("NEWENTITY"))
        {
            activenode.addChild("entity");
        } else
        {
            myELHgui.mframe.currentview.actions(comStr, activenode);
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
        System.out.println(" preparing popup 44 ");
        if (me.isPopupTrigger())
        {
            this.popup = makePopup(this.activenode);
            this.popup.show((JComponent) me.getSource(), me.getX(), me.getY());
        }
    }

    JPopupMenu makePopup(elhnode currentnode)
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