package org.lerot.myELH;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswTheme;

// A tree of the project, in one of three forms:
//
//   PROJECT              ENTITIES                 EVENTS
//   Libproj              Entities                 Events
//   ├─ Entities          ├─ Person                ├─ Borrow
//   │  ├─ Person         │  ├─ Library Member     │  ├─ Person: Borrow
//   │  └─ Book           │  │  └─ ...             │  └─ Book: Is borrowed
//   └─ Events            │  └─ Employee           ├─ Joins ⚑
//      ├─ Borrow         └─ Book                  └─ Not in an event yet
//      └─ Joins ⚑                                    └─ Person: Leaves ⚑
//
// The project window, the Entities window and the Events window each have
// one. An entity's life history appears only once it has been selected (and
// so read). Flags (⚑, in orange) mark an action not yet part of any event,
// and an event that triggers fewer than two actions.
//
// Each tree node holds the object it shows (a ProjectEntity, an ElhNode of a
// life history, a ProjectEvent, an EntityAction or a Heading), so a
// selection is found by the object itself and never by name.
public class TreeView extends JPanel implements ActionListener
{
    private static final long serialVersionUID = 1L;
    // selection highlights: branches blue-grey, leaves rose
    static final Color BRANCHSELECTED = Color.decode("#BFC9D4");
    static final Color LEAFSELECTED = Color.decode("#E3CBC6");
    static final Color FLAGGED = Color.decode("#C0580A");
    static final String FLAG = " ⚑";

    enum Mode
    {
        PROJECT, ENTITIES, EVENTS
    }

    // the headings of the trees
    enum Heading
    {
        ENTITIES("Entities"), EVENTS("Events"), UNLINKED("Not in an event yet");

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

    // what a tree reports to its window
    interface Listener
    {
        // the user selected a tree node showing this object (null for nothing)
        void selected(Object shown);

        // the popup menu items for a tree node showing this object
        default void popup(Object shown, JPopupMenu menu)
        {
        }
    }

    private final MyElh app;
    private final Mode mode;
    private final Listener listener;
    JTree thistree;
    Color bg;
    // true while the tree is being rebuilt, when selections are not the user's
    private boolean rebuilding;
    // the project shown, and its actions, for the flags
    private Project project;
    private List<EntityAction> actions = List.of();

    public TreeView(MyElh app, Mode mode, Listener listener)
    {
        this.app = app;
        this.mode = mode;
        this.listener = listener;
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
        // in the events tree several actions can be chosen at once, to make an event of them
        tree.getSelectionModel().setSelectionMode(this.mode == Mode.EVENTS
                ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION : TreeSelectionModel.SINGLE_TREE_SELECTION);
        setCellRenderer(tree);
        tree.addTreeSelectionListener(e ->
        {
            if (!this.rebuilding) selected(tree.getLastSelectedPathComponent());
        });
        // expanding an entity not yet read selects it, which reads it
        tree.addTreeWillExpandListener(new TreeWillExpandListener()
        {
            public void treeWillExpand(TreeExpansionEvent e)
            {
                if (!rebuilding && ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject()
                        instanceof ProjectEntity entity && !entity.isLoaded())
                {
                    tree.setSelectionPath(e.getPath());
                }
            }

            public void treeWillCollapse(TreeExpansionEvent e)
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
        boolean firsttime = project != this.project;
        this.project = project;
        this.actions = project == null || this.mode == Mode.ENTITIES ? List.of() : project.allActions();
        DefaultMutableTreeNode root = project == null ? new DefaultMutableTreeNode("No project open") : switch (this.mode)
        {
            case PROJECT -> projectTree(project);
            case ENTITIES -> entityTree(project, true);
            case EVENTS -> eventTree(project);
        };
        this.rebuilding = true;
        try
        {
            remove(this.thistree);
            this.thistree = newTree(root);
            add(this.thistree);
            if (firsttime && project != null)
            {
                expanded.add(root.getUserObject());
                expanded.add(Heading.ENTITIES);
                expanded.add(Heading.EVENTS);
                expanded.add(Heading.UNLINKED);
            }
            expand(root, expanded);
        } finally
        {
            this.rebuilding = false;
        }
        revalidate();
        repaint();
    }

    private DefaultMutableTreeNode projectTree(Project project)
    {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(project);
        DefaultMutableTreeNode entities = entityTree(project, false);
        root.add(entities);
        DefaultMutableTreeNode events = new DefaultMutableTreeNode(Heading.EVENTS);
        for (ProjectEvent event : project.getEvents())
        {
            events.add(new DefaultMutableTreeNode(event, false));
        }
        root.add(events);
        return root;
    }

    // the entities, each with its life history if it has been read and
    // withlifehistories is true
    private DefaultMutableTreeNode entityTree(Project project, boolean withlifehistories)
    {
        DefaultMutableTreeNode entities = new DefaultMutableTreeNode(Heading.ENTITIES);
        for (ProjectEntity entity : project.getEntities())
        {
            DefaultMutableTreeNode entitynode = new DefaultMutableTreeNode(entity, withlifehistories);
            if (withlifehistories && entity.isLoaded())
            {
                addnodes(entitynode, entity.getTop());
            }
            entities.add(entitynode);
        }
        return entities;
    }

    private DefaultMutableTreeNode eventTree(Project project)
    {
        DefaultMutableTreeNode events = new DefaultMutableTreeNode(Heading.EVENTS);
        for (ProjectEvent event : project.getEvents())
        {
            DefaultMutableTreeNode eventnode = new DefaultMutableTreeNode(event);
            for (EntityAction action : project.actionsOf(event, this.actions))
            {
                eventnode.add(new DefaultMutableTreeNode(action, false));
            }
            events.add(eventnode);
        }
        DefaultMutableTreeNode unlinked = new DefaultMutableTreeNode(Heading.UNLINKED);
        for (EntityAction action : project.unlinkedActions(this.actions))
        {
            unlinked.add(new DefaultMutableTreeNode(action, false));
        }
        events.add(unlinked);
        return events;
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

    // the label of a tree node, with its flag if it has one; null if it is not flagged
    private String flaggedLabel(Object shown)
    {
        if (this.project == null)
        {
            return null;
        }
        switch (shown)
        {
            case ElhNode node when this.project.isUnlinked(node):
                return node.getName() + FLAG;
            case EntityAction action when this.project.isUnlinked(action.node()):
                return action + FLAG;
            case ProjectEntity entity when entity.isLoaded():
            {
                List<ElhNode> nodes = new ArrayList<>();
                entity.getTop().collectActions(nodes);
                long unlinked = nodes.stream().filter(this.project::isUnlinked).count();
                return unlinked == 0 ? null : entity.getName() + FLAG + " " + unlinked;
            }
            case ProjectEvent event when this.mode != Mode.ENTITIES && this.project.isLonely(event, this.actions):
            {
                int count = this.project.actionsOf(event, this.actions).size();
                return event.getName() + FLAG + " (" + count + (count == 1 ? " action)" : " actions)");
            }
            case Heading heading when heading == Heading.UNLINKED:
            {
                int count = this.project.unlinkedActions(this.actions).size();
                return count == 0 ? null : heading + FLAG + " " + count;
            }
            default:
                return null;
        }
    }

    public void setCellRenderer(JTree atree)
    {
        atree.setCellRenderer(new DefaultTreeCellRenderer()
        {
            private static final long serialVersionUID = 1L;

            public Component getTreeCellRendererComponent(JTree pTree, Object pValue, boolean pIsSelected, boolean pIsExpanded, boolean pIsLeaf, int pRow, boolean pHasFocus)
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
                String flagged = flaggedLabel(node.getUserObject());
                if (flagged != null)
                {
                    setText(flagged);
                    setForeground(FLAGGED);
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
                    && entity.getTop() == target
                    || shown instanceof EntityAction action && target instanceof EntityAction other
                    && action.node() == other.node())
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

    // the objects of every selected tree node
    public List<Object> selectedObjects()
    {
        List<Object> found = new ArrayList<>();
        TreePath[] paths = this.thistree.getSelectionPaths();
        if (paths != null)
        {
            for (TreePath path : paths)
            {
                found.add(((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject());
            }
        }
        return found;
    }

    // the entity whose life history a tree node is part of, or null
    static ProjectEntity owner(DefaultMutableTreeNode node)
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

    // the entity whose life history the node is part of, if the tree shows it
    public ProjectEntity ownerOf(ElhNode node)
    {
        DefaultMutableTreeNode treenode = find(node);
        return treenode == null ? null : owner(treenode);
    }

    // what the user selected
    private void selected(Object component)
    {
        Object shown = component instanceof DefaultMutableTreeNode node ? node.getUserObject() : null;
        // the window may rebuild the tree in answer, so not while JTree is still notifying
        SwingUtilities.invokeLater(() -> this.listener.selected(shown));
    }

    // right-click: selects the row under the mouse, unless it is already
    // among those selected, and offers what can be done to it
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
        if (!this.thistree.isPathSelected(path))
        {
            this.thistree.setSelectionPath(path);
        }
        Object shown = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        JPopupMenu popup = new JPopupMenu();
        if (shown instanceof ElhNode && this.mode == Mode.ENTITIES)
        {
            addCommand(popup, "UP", "UP");
            addCommand(popup, "LEFT", "LEFT");
            addCommand(popup, "MARK", "MARK");
            addCommand(popup, "Add new sibling", "NEWSIBLING");
            addCommand(popup, "Add new child", "NEWCHILD");
            addCommand(popup, "Remove this node", "DELETE");
        } else
        {
            this.listener.popup(shown, popup);
        }
        if (popup.getComponentCount() > 0)
        {
            popup.show(this.thistree, me.getX(), me.getY());
        }
    }

    static void addItem(JPopupMenu popup, String label, Consumer<ActionEvent> action)
    {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(action::accept);
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
            if (app.addAction(selectednode::addSibling) == null) return;
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
