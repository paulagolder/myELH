package org.lerot.myELH;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswTheme;

// The project tree:
//
//   Libproj
//   ├─ Entities
//   │  ├─ Person        its life history appears below it once it is selected
//   │  └─ Book
//   └─ Events
//      ├─ Joins
//      └─ ...
//
// Each tree node holds the object it shows (a ProjectEntity, an ElhNode of a
// life history, a ProjectEvent or a Heading), so a selection is found by
// the object itself and never by name.
public class TreeView extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    // selection highlights: branches blue-grey, leaves rose
    static final Color BRANCHSELECTED = Color.decode("#BFC9D4");
    static final Color LEAFSELECTED = Color.decode("#E3CBC6");

    // the two main subheadings of the tree
    enum Heading
    {
        ENTITIES("Entities"), EVENTS("Events");

        private final String label;

        Heading(String label)
        {
            this.label = label;
        }

        public String toString()
        {
            return this.label;
        }
    }

    private final MyElh app;
    JTree thistree;
    Color bg;
    // true while the tree is being rebuilt, when selections are not the user's
    private boolean rebuilding;

    public TreeView(MyElh app)
    {
        this.app = app;
        this.bg = Color.decode(jswTheme.SELECTAREA);
        setBackground(this.bg);
        setBorder(jswStyle.makeLineBorder());
        this.thistree = newTree(new DefaultMutableTreeNode("No project open"));
        add(this.thistree);
    }

    private JTree newTree(DefaultMutableTreeNode root)
    {
        JTree tree = new JTree(root, true);
        tree.setBackground(this.bg);
        setCellRenderer(tree);
        tree.addTreeSelectionListener(e ->
        {
            if (!this.rebuilding) selected(tree.getLastSelectedPathComponent());
        });
        // expanding an entity not yet read selects it, which reads it
        tree.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener()
        {
            public void treeWillExpand(javax.swing.event.TreeExpansionEvent e)
            {
                if (!rebuilding && ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject()
                        instanceof ProjectEntity entity && !entity.isLoaded())
                {
                    tree.setSelectionPath(e.getPath());
                }
            }

            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent e)
            {
            }
        });
        tree.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent me)
            {
                showPopup(me);
            }

            public void mouseReleased(MouseEvent me)
            {
                showPopup(me);
            }
        });
        return tree;
    }

    // rebuilds the tree from the project, keeping what was expanded
    public void maketree(Project project)
    {
        Set<Object> expanded = expandedObjects();
        // a project just opened starts with its headings expanded
        boolean firsttime = ((DefaultMutableTreeNode) this.thistree.getModel().getRoot()).getUserObject() != project;
        DefaultMutableTreeNode root;
        if (project == null)
        {
            root = new DefaultMutableTreeNode("No project open");
        } else
        {
            root = new DefaultMutableTreeNode(project);
            DefaultMutableTreeNode entities = new DefaultMutableTreeNode(Heading.ENTITIES);
            for (ProjectEntity entity : project.getEntities())
            {
                DefaultMutableTreeNode entitynode = new DefaultMutableTreeNode(entity);
                // its life history is only shown once it has been read
                if (entity.isLoaded())
                {
                    addnodes(entitynode, entity.getTop());
                }
                entities.add(entitynode);
            }
            root.add(entities);
            DefaultMutableTreeNode events = new DefaultMutableTreeNode(Heading.EVENTS);
            for (ProjectEvent event : project.getEvents())
            {
                events.add(new DefaultMutableTreeNode(event, false));
            }
            root.add(events);
        }
        this.rebuilding = true;
        try
        {
            remove(this.thistree);
            this.thistree = newTree(root);
            add(this.thistree);
            if (firsttime && project != null)
            {
                expanded.add(project);
                expanded.add(Heading.ENTITIES);
                expanded.add(Heading.EVENTS);
            }
            expand(root, expanded);
        } finally
        {
            this.rebuilding = false;
        }
        revalidate();
        repaint();
    }

    // the objects of the tree nodes now expanded
    private Set<Object> expandedObjects()
    {
        Set<Object> found = Collections.newSetFromMap(new IdentityHashMap<>());
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.thistree.getModel().getRoot();
        Enumeration<TreePath> paths = this.thistree.getExpandedDescendants(new TreePath(root));
        while (paths != null && paths.hasMoreElements())
        {
            found.add(((DefaultMutableTreeNode) paths.nextElement().getLastPathComponent()).getUserObject());
        }
        return found;
    }

    private void expand(DefaultMutableTreeNode node, Set<Object> expanded)
    {
        if (expanded.contains(node.getUserObject()))
        {
            this.thistree.expandPath(new TreePath(node.getPath()));
        }
        for (Enumeration<TreeNode> children = node.children(); children.hasMoreElements(); )
        {
            expand((DefaultMutableTreeNode) children.nextElement(), expanded);
        }
    }

    private void addnodes(DefaultMutableTreeNode treenode, ElhNode node)
    {
        for (ElhNode child : node.getChildren())
        {
            DefaultMutableTreeNode childnode = new DefaultMutableTreeNode(child);
            addnodes(childnode, child);
            treenode.add(childnode);
        }
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
                if (node.getChildCount() > 0 || node.isRoot() || node.getUserObject() instanceof Heading)
                {
                    setBackgroundSelectionColor(BRANCHSELECTED);
                } else
                {
                    setBackgroundSelectionColor(LEAFSELECTED);
                }
                return this;
            }
        });
    }

    // the tree node showing the object, or null; an entity's top node is
    // shown by the entity's own tree node
    private DefaultMutableTreeNode find(Object target)
    {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.thistree.getModel().getRoot();
        for (Enumeration<TreeNode> e = root.depthFirstEnumeration(); e.hasMoreElements(); )
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            Object shown = node.getUserObject();
            if (shown == target || shown instanceof ProjectEntity entity && entity.getTop() != null
                    && entity.getTop() == target)
            {
                return node;
            }
        }
        return null;
    }

    // selects the object's tree node, if it has one, without acting on it
    public void select(Object target)
    {
        DefaultMutableTreeNode node = find(target);
        if (node == null)
        {
            return;
        }
        TreePath path = new TreePath(node.getPath());
        this.rebuilding = true;
        try
        {
            this.thistree.setSelectionPath(path);
        } finally
        {
            this.rebuilding = false;
        }
        this.thistree.scrollPathToVisible(path);
    }

    public void expand(Object target)
    {
        DefaultMutableTreeNode node = find(target);
        if (node != null)
        {
            this.thistree.expandPath(new TreePath(node.getPath()));
        }
    }

    // the entity whose life history a tree node is part of, or null
    private static ProjectEntity owner(DefaultMutableTreeNode node)
    {
        for (TreeNode n = node; n != null; n = n.getParent())
        {
            if (((DefaultMutableTreeNode) n).getUserObject() instanceof ProjectEntity entity)
            {
                return entity;
            }
        }
        return null;
    }

    // what the user selected
    private void selected(Object component)
    {
        if (!(component instanceof DefaultMutableTreeNode node))
        {
            return;
        }
        Object shown = node.getUserObject();
        // the app may rebuild the tree in answer, so not while JTree is still notifying
        SwingUtilities.invokeLater(() ->
        {
            switch (shown)
            {
                case ProjectEntity entity -> app.entitySelected(entity);
                case ElhNode elhnode -> app.nodeSelected(elhnode, owner(node));
                case ProjectEvent event -> app.eventSelectedInTree(event);
                default -> app.nothingSelected();
            }
        });
    }

    // right-click: selects the row under the mouse and offers what can be done to it
    private void showPopup(MouseEvent me)
    {
        if (!me.isPopupTrigger())
        {
            return;
        }
        TreePath path = this.thistree.getPathForLocation(me.getX(), me.getY());
        if (path == null)
        {
            return;
        }
        this.thistree.setSelectionPath(path);
        Object shown = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        JPopupMenu popup = new JPopupMenu();
        switch (shown)
        {
            case Heading heading when heading == Heading.ENTITIES ->
            {
                addItem(popup, "New Entity", e -> app.newEntity());
                addItem(popup, "Import Entity...", e -> app.importEntity());
            }
            case Heading heading -> addItem(popup, "New Event", e -> app.newEvent());
            case ProjectEntity entity ->
            {
                addItem(popup, "Save Entity", e -> app.saveEntity(entity));
                addItem(popup, entity.isInline() ? "Move to Own File" : "Move into project.xml",
                        e -> app.moveEntity(entity));
                addItem(popup, "Remove from Project", e -> app.removeEntity(entity));
            }
            case ProjectEvent event ->
            {
                addItem(popup, event.isInline() ? "Move to Own File" : "Move into project.xml",
                        e -> app.moveEvent(event));
                addItem(popup, "Show in Events Tab", e -> app.showEventInTab(event));
            }
            case ElhNode node ->
            {
                addCommand(popup, "UP", "UP");
                addCommand(popup, "LEFT", "LEFT");
                addCommand(popup, "MARK", "MARK");
                addCommand(popup, "Add new sibling", "NEWSIBLING");
                addCommand(popup, "Add new child", "NEWCHILD");
                addCommand(popup, "Remove this node", "DELETE");
            }
            default ->
            {
                return;
            }
        }
        popup.show(this.thistree, me.getX(), me.getY());
    }

    private static void addItem(JPopupMenu popup, String label, ActionListener action)
    {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(action);
        popup.add(item);
    }

    private void addCommand(JPopupMenu popup, String label, String command)
    {
        JMenuItem item = new JMenuItem(label);
        item.setActionCommand(command);
        item.addActionListener(this);
        popup.add(item);
    }

    public void actionPerformed(ActionEvent ae)
    {
        actions(ae.getActionCommand(), app.activenode);
    }

    // carries out a command from the tree's popup menu or the edit panel on a
    // node of the active entity's life history
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
            String name = app.askNodeName(ElhNode.getDefaultChildtype());
            if (name == null) return;
            selectednode.addChild(ElhNode.getDefaultChildtype(), name);
        } else if (comStr.startsWith("NEWSIBLING"))
        {
            if (selectednode.getParent() == null) return;
            if (app.addLeafEvent(selectednode::addSibling) == null) return;
        } else if (comStr.startsWith("DELETE"))
        {
            // an entity's top node is removed with Remove from Project instead
            if (selectednode.getParent() == null)
            {
                return;
            }
            ElhNode parentnode = selectednode.getParent();
            parentnode.moveChildrenFrom(selectednode);
            parentnode.removeChild(selectednode);
            app.activenode = parentnode;
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

    // makes the node the active one and selects it in the tree
    void setselections(ElhNode selectednode)
    {
        app.activenode = selectednode;
        select(selectednode);
    }
}
