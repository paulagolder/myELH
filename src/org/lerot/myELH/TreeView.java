package org.lerot.myELH;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.border.LineBorder;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswTheme;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class TreeView extends JPanel implements ActionListener, MouseListener
{
    private static final long serialVersionUID = 1L;
    // selection highlights: branches blue-grey, leaves rose
    static final Color BRANCHSELECTED = Color.decode("#BFC9D4");
    static final Color LEAFSELECTED = Color.decode("#E3CBC6");
    private final MyElh app;
    MyTree thistree;
    Color bg;
    private JPopupMenu popup;

    public TreeView(MyElh app)
    {
        this.app = app;
        this.bg = Color.decode(jswTheme.SELECTAREA);
        this.thistree = emptytree();
        add(this.thistree);
        setBackground(this.bg);
        this.thistree.setBackground(this.bg);
        setCellRenderer(this.thistree);
        setSize(200, 200);
        setBorder(jswStyle.makeLineBorder());
        setVisible(true);
    }

    public void maketree(Elh anelh)
    {
        if (anelh == null)
        {
            return;
        }
        String name = anelh.getName();
        DefaultMutableTreeNode parent = new DefaultMutableTreeNode(name);
        parent.setUserObject(anelh);
        List<ElhNode> entities = anelh.getChildren();
        for (ElhNode e : entities)
        {
            name = e.getName();
            DefaultMutableTreeNode anode = new DefaultMutableTreeNode(name);
            addnodes(anode, e);
            parent.add(anode);
        }
        remove(this.thistree);
        this.thistree = new MyTree(parent, true);
        this.thistree.addMouseListener(this);
        this.thistree.addTreeSelectionListener(new MyTreeListener());
        this.thistree.setBackground(this.bg);
        setCellRenderer(this.thistree);
        add(this.thistree);
    }

    private MyTree emptytree()
    {
        MyTree empty = new MyTree(new DefaultMutableTreeNode("No ELH OPEN"), false);
        empty.addTreeSelectionListener(new MyTreeListener());
        empty.setBackground(this.bg);
        return empty;
    }

    // shows the "No ELH OPEN" tree again
    public void cleartree()
    {
        remove(this.thistree);
        this.thistree = emptytree();
        setCellRenderer(this.thistree);
        add(this.thistree);
        revalidate();
        repaint();
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
                setBackgroundNonSelectionColor(TreeView.this.bg);
                if (node.isRoot())
                {
                    setBackgroundSelectionColor(BRANCHSELECTED);
                } else if (node.getChildCount() > 0)
                {
                    setBackgroundSelectionColor(BRANCHSELECTED);
                } else if (pIsLeaf)
                {
                    setBackgroundSelectionColor(LEAFSELECTED);
                }
                return this;
            }
        });
    }

    private void addnodes(DefaultMutableTreeNode topevnode, ElhNode topev)
    {
        List<ElhNode> events = topev.getChildren();
        if (events != null)
        {
            for (ElhNode e : events)
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
        actions(comStr, app.activenode);
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent me)
    {
        if (me.isPopupTrigger())
        {
            TreePath path = this.thistree.getSelectionPath();
            if(path == null) return;
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

    // carries out a command from the tree's popup menu or the edit panel
    public void actions(String comStr, ElhNode selectednode)
    {
        if (selectednode == null)
        {
            return;
        }
        app.activenode = selectednode;
        if (comStr.startsWith("VIEW"))
        {
            // VIEW PARENT: show the parent of the selected node
            if (selectednode.getParent() == null)
            {
                return;
            }
            setselections(selectednode.getParent());
            app.refresh();
            return;
        } else if (comStr.startsWith("UP"))
        {
            selectednode.moveup();
        } else if (comStr.startsWith("MARK"))
        {
            selectednode.mark();
        } else if (comStr.startsWith("LEFT"))
        {
            selectednode.moveleft();
        } else if (comStr.startsWith("RIGHT"))
        {
            selectednode.moveright();
        } else if (comStr.startsWith("NEWCHILD"))
        {
            selectednode.addChild(ElhNode.getDefaultChildtype());
        } else if (comStr.startsWith("NEWSIBLING"))
        {
            selectednode.addSibling();
        } else if (comStr.startsWith("DELETE"))
        {
            if (selectednode.getParent() == null)
            {
                // deleting the top node: if it only holds another elh, that becomes the top
                if (selectednode.countChildren() == 1 && selectednode.getChildren().getFirst() instanceof Elh inner)
                {
                    inner.setParent(null);
                    app.currentelh = inner;
                    app.activenode = inner;
                }
            } else
            {
                ElhNode parentnode = selectednode.getParent();
                parentnode.moveChildrenFrom(selectednode);
                parentnode.removeChild(selectednode);
                app.activenode = parentnode;
            }
        } else if (comStr.startsWith("DUPLICATE:"))
        {
            selectednode.duplicate(comStr.substring("DUPLICATE:".length()));
        } else
        {
            return;
        }
        app.maketree();
        app.refresh();
    }

    void setselections(ElhNode selectednode)
    {
        app.activenode = selectednode;
        String path = ElhNode.makepath(selectednode);
        DefaultMutableTreeNode node = this.thistree.findActiveTreenode(path);
        app.activetreenode = node;
        if (node == null)
        {
            return;
        }
        TreePath treepath = new TreePath(node.getPath());
        this.thistree.setSelectionPath(treepath);
        this.thistree.scrollPathToVisible(treepath);
    }

    class MyTreeListener implements TreeSelectionListener
    {
        public void valueChanged(TreeSelectionEvent e)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) TreeView.this.thistree.getLastSelectedPathComponent();
            if (node == null)
            {
                return;
            }
            app.activetreenode = node;
            String treepath = TreeView.this.makepath(node);
            if (app.currentelh != null)
            {
                app.activenode = app.currentelh.findActiveEvent(treepath);
            }
            app.updateview();
        }
    }
}

