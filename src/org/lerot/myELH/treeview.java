package org.lerot.myELH;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class treeview extends JPanel implements ActionListener, MouseListener
{
    private static final long serialVersionUID = 1L;
    myTree thistree;
    Color bg;
    private JPopupMenu popup;

    public treeview()
    {
        this.bg = Color.lightGray;
        this.thistree = new myTree(new DefaultMutableTreeNode("No ELH OPEN"), false);
        this.thistree.removeAll();
        this.thistree.addTreeSelectionListener(new mytreelistener());
        add(this.thistree);
        setBackground(this.bg);
        this.thistree.setBackground(this.bg);
        setCellRenderer(this.thistree);
        setSize(200, 200);
        setBorder(new LineBorder(Color.RED, 2));
        setVisible(true);
    }

    public void maketree(elh anelh)
    {
        if (anelh == null)
        {
            return;
        }
        String name = anelh.getName();
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode(name);
        parent.setUserObject(anelh);
        Vector<elhnode> entities = anelh.getChildren();
        for (elhnode e : entities)
        {
            name = e.getName();
            DefaultMutableTreeNode anode = new DefaultMutableTreeNode(name);
            addnodes(anode, e);
            parent.add(anode);
        }
        remove(this.thistree);
        this.thistree = new myTree(parent, true);
        this.thistree.addMouseListener(this);
        this.thistree.addTreeSelectionListener(new mytreelistener());
        this.thistree.setBackground(this.bg);
        setCellRenderer(this.thistree);
        add(this.thistree);
    }

    public void setCellRenderer(JTree atree)
    {
        atree.setCellRenderer(new DefaultTreeCellRenderer()
        {
            private static final long serialVersionUID = 1L;

            public DefaultTreeCellRenderer getTreeCellRendererComponent(JTree pTree, Object pValue, boolean pIsSelected, boolean pIsExpanded, boolean pIsLeaf, int pRow, boolean pHasFocus)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) pValue;
                super.getTreeCellRendererComponent(pTree, pValue, pIsSelected, pIsExpanded, pIsLeaf, pRow, pHasFocus);
                setBackgroundNonSelectionColor(treeview.this.bg);
                if (node.isRoot())
                {
                    setBackgroundSelectionColor(Color.darkGray);
                } else if (node.getChildCount() > 0)
                {
                    setBackgroundSelectionColor(Color.darkGray);
                } else if (pIsLeaf)
                {
                    setBackgroundSelectionColor(Color.pink);
                }
                return this;
            }
        });
    }

    private void addnodes(DefaultMutableTreeNode topevnode, elhnode topev)
    {
        Vector<elhnode> events = topev.getChildren();
        if (events != null)
        {
            for (elhnode e : events)
            {
                String name = e.getName();
                DefaultMutableTreeNode evnode = new DefaultMutableTreeNode(name);
                evnode.setUserObject(e);
                addnodes(evnode, e);
                topevnode.add(evnode);
            }
        }
    }

    public String makepath(TreeNode node)
    {
        String treepath = "";
        for (TreeNode pnode = node; pnode != null; pnode = pnode.getParent())
        {
            Object nodeInfo = ((DefaultMutableTreeNode) pnode).getUserObject();
            treepath = nodeInfo + "/" + treepath;
        }
        return treepath;
    }

    public void expandPath(DefaultMutableTreeNode activetreenode)
    {
        if (activetreenode != null)
        {
            TreeNode[] apath = activetreenode.getPath();
            this.thistree.expandPath(new TreePath(apath));
        }
    }

    public void suspendtreeview()
    {
        TreeSelectionListener alistener = this.thistree.getTreeSelectionListeners()[0];
        this.thistree.removeTreeSelectionListener(alistener);
    }

    public void activatetreeview()
    {
        this.thistree.addTreeSelectionListener(new mytreelistener());
    }

    JPopupMenu makeTreePopup(String path)
    {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem mi = new JMenuItem("UP");
        mi.addActionListener(this);
        mi.setActionCommand("UP");
        popup.add(mi);
        mi = new JMenuItem("LEFT");
        mi.addActionListener(this);
        mi.setActionCommand("LEFT");
        popup.add(mi);
        mi = new JMenuItem("MARK");
        mi.addActionListener(this);
        mi.setActionCommand("MARK");
        popup.add(mi);
        mi = new JMenuItem("Add new sibling");
        mi.addActionListener(this);
        mi.setActionCommand("NEWSIBLING");
        popup.add(mi);
        mi = new JMenuItem("Add new child");
        mi.addActionListener(this);
        mi.setActionCommand("NEWCHILD");
        popup.add(mi);
        mi = new JMenuItem("Remove this node");
        mi.addActionListener(this);
        mi.setActionCommand("DELETE");
        popup.add(mi);
        return popup;
    }

    public void actionPerformed(ActionEvent ae)
    {
        String comStr = ae.getActionCommand();
        elhnode activenode = myELHgui.mframe.activenode;
        actions(comStr, activenode);
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent me)
    {
        if (me.isPopupTrigger())
        {
            TreePath path = this.thistree.getSelectionPath();
            this.popup = makeTreePopup(path.toString());
            this.popup.show((JComponent) me.getSource(), me.getX(), me.getY());
        }
    }

    public void mouseReleased(MouseEvent me)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void actions(String comStr, elhnode selectednode)
    {
        System.out.println(String.valueOf(comStr) + " " + selectednode + " in treeview ");
        myELHgui.mframe.activenode = selectednode;
        if (comStr.startsWith("VIEW PARENT"))
        {
            myELHgui.mframe.activenode = selectednode.getParent();
            myELHgui.mframe.currenttreeview.setselections(myELHgui.mframe.activenode);
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("VIEW"))
        {
            myELHgui.mframe.activenode = selectednode.getParent();
            myELHgui.mframe.currenttreeview.setselections(myELHgui.mframe.activenode);
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("UP"))
        {
            selectednode.moveup();
            myELHgui.mframe.maketree();
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("MARK"))
        {
            selectednode.mark();
            myELHgui.mframe.maketree();
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("LEFT"))
        {
            selectednode.moveleft();
            myELHgui.mframe.maketree();
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("RIGHT"))
        {
            selectednode.moveright();
            myELHgui.mframe.maketree();
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("NEWCHILD"))
        {
            selectednode.addChild(selectednode.getDefaultChildtype());
            myELHgui.mframe.maketree();
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("NEWSIBLING"))
        {
            String childname = selectednode.getName();
            selectednode.addSibling();
            myELHgui.mframe.maketree();
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("DELETE"))
        {
            if(selectednode.getParent()  == null)
            {
                if(selectednode.getChildren().size()==1)
                {
                    myELHgui.mframe.activenode = selectednode.getChildren().get(0);
                    myELHgui.mframe.currentelh = (elh)myELHgui.mframe.activenode;
                }
            }else
            {
                elhnode targetnode = selectednode;
                elhnode parentnode = selectednode.getParent();
                parentnode.moveChildrenFrom(targetnode);
                parentnode.removeChild(targetnode);
                myELHgui.mframe.activenode = parentnode;
            }
         //   myELHgui.mframe.currentview.setselections(selectednode);
            myELHgui.mframe.maketree();
            myELHgui.mframe.refresh();
        } else if (comStr.startsWith("DUPLICATE"))
        {
            String path = comStr.substring(10);
            selectednode.duplicate(path);
            myELHgui.mframe.refresh();
        }
    }

    void setselections(elhnode selectednode)
    {
        myELHgui.mframe.activenode = selectednode;
        String path = elhnode.makepath(selectednode);
        DefaultMutableTreeNode node = this.thistree.findActiveTreenode(path);
        myELHgui.mframe.activetreenode = node;
        TreePath treepath = new TreePath(node.getPath());
        this.thistree.setSelectionPath(treepath);
        this.thistree.scrollPathToVisible(treepath);
    }

    class mytreelistener implements TreeSelectionListener
    {
        public void valueChanged(TreeSelectionEvent e)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) treeview.this.thistree.getLastSelectedPathComponent();
            if (node == null)
            {
                return;
            }
            myELHgui.mframe.activetreenode = node;
            String treepath = treeview.this.makepath(node);
            if (myELHgui.mframe.currentelh != null)
            {
                myELHgui.mframe.activenode = myELHgui.mframe.currentelh.findActiveEvent(treepath);
            }
            myELHgui.mframe.updateview( myELHgui.mframe.currentelh);
        }
    }
}

