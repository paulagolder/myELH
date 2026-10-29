package org.lerot.myELH;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

// The edit panel for the selected node of a life history, whatever its
// type: its details above, its children in a table, and what can be done
// below. An action (a leaf action node) also has the event it is part of and
// its condition and result.
public class EditView extends JPanel
{
    private static final long serialVersionUID = 1L;
    private static final String NOEVENT = "(not yet in an event)";
    // the child selected when the panel was last made, selected again when
    // it is remade after a change (moving a child up, say)
    private static ElhNode lastselectedchild;
    protected final MyElh app;
    ElhNode activenode;
    private final JLabel title = new JLabel();
    private final JLabel subtitle = new JLabel();
    private final JTextField namefield = new JTextField(20);
    private final JComboBox<String> typefield = new JComboBox<>();
    private final JComboBox<String> groupingfield = new JComboBox<>();
    private final JTextField stylefield = new JTextField(20);
    // only there for an action
    private JComboBox<Object> eventfield;
    private JTextArea conditionfield;
    private JTextArea resultfield;
    private final DefaultTableModel childmodel = new DefaultTableModel(
            new Object[]{"Child", "Type", "Grouping", "Event"}, 0)
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    };
    private final JTable childtable = new JTable(this.childmodel);
    private final List<JButton> childbuttons = new ArrayList<>();
    private JButton upbutton;
    private JButton downbutton;
    private JButton duplicatebutton;

    public EditView(MyElh app)
    {
        super(new BorderLayout(4, 6));
        this.app = app;
        setBorder(BorderFactory.createCompoundBorder(jswStyle.makeLineBorder(),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        setBackground(Color.decode(jswTheme.EDITAREA));
    }

    public void setup(ElhNode anode)
    {
        this.activenode = anode;
        Project project = this.app.getProject();
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(makeHeading(anode, project), BorderLayout.NORTH);
        top.add(makeForm(anode, project), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(makeChildPanel(anode, project), BorderLayout.CENTER);
        add(makeButtons(anode), BorderLayout.SOUTH);
    }

    // the node's name, with what it is and where it is
    private JPanel makeHeading(ElhNode anode, Project project)
    {
        JPanel heading = new JPanel(new GridLayout(2, 1));
        heading.setOpaque(false);
        this.title.setText(anode.getName());
        this.title.setFont(this.title.getFont().deriveFont(Font.BOLD, this.title.getFont().getSize2D() * 1.3f));
        String kind = anode.getType();
        ElhNode parent = anode.getParent();
        String where = parent == null ? "the top of its life history" : "in " + parent.getPath();
        this.subtitle.setText(kind + ", " + where);
        this.subtitle.setForeground(Color.GRAY);
        heading.add(this.title);
        heading.add(this.subtitle);
        if (project != null && project.isUnlinked(anode))
        {
            this.subtitle.setText(this.subtitle.getText() + "   " + TreeView.FLAG.strip() + " not yet part of an event");
            this.subtitle.setForeground(TreeView.FLAGGED);
        }
        return heading;
    }

    private JPanel makeForm(ElhNode anode, Project project)
    {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        int row = 0;
        this.namefield.setText(anode.getName());
        addRow(form, row++, "NAME:", this.namefield);
        // the top elh stays an elh; any other node can become any other type
        for (String type : anode.is(NodeType.ELH) ? new String[]{anode.getType()} : NodeType.choiceNames())
        {
            this.typefield.addItem(type);
        }
        this.typefield.setSelectedItem(anode.getType());
        addRow(form, row++, "TYPE:", this.typefield);
        for (String grouping : groupingChoices(anode.getChildgrouptype()))
        {
            this.groupingfield.addItem(grouping);
        }
        this.groupingfield.setSelectedItem(anode.getChildgrouptype().xmlName());
        addRow(form, row++, "CHILD GROUPING:", this.groupingfield);
        this.stylefield.setText(anode.getStylename());
        addRow(form, row++, "STYLE:", this.stylefield);
        if (anode.isLeafAction())
        {
            if (project != null)
            {
                this.eventfield = new JComboBox<>();
                this.eventfield.addItem(NOEVENT);
                project.getEvents().forEach(this.eventfield::addItem);
                ProjectEvent linked = anode.getEventref() == null ? null : project.findEvent(anode.getEventref());
                this.eventfield.setSelectedItem(linked == null ? NOEVENT : linked);
                addRow(form, row++, "PART OF EVENT:", this.eventfield);
            }
            this.conditionfield = textArea(anode.getCondition());
            addRow(form, row++, "CONDITION:", new JScrollPane(this.conditionfield));
            this.resultfield = textArea(anode.getResult());
            addRow(form, row++, "RESULT:", new JScrollPane(this.resultfield));
        }
        return form;
    }

    private static JTextArea textArea(String text)
    {
        JTextArea area = new JTextArea(text, 2, 30);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private static void addRow(JPanel form, int row, String label, JComponent field)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 6);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = row;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(field, c);
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

    // the node's children, and what can be done to the one selected
    private JPanel makeChildPanel(ElhNode anode, Project project)
    {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        panel.add(new JLabel(anode.countChildren() == 0 ? "CHILDREN: none" : "CHILDREN:"), BorderLayout.NORTH);
        if (anode.countChildren() == 0)
        {
            return panel;
        }
        for (ElhNode child : anode.getChildren())
        {
            String event = "";
            if (child.isLeafAction() && project != null)
            {
                ProjectEvent linked = child.getEventref() == null ? null : project.findEvent(child.getEventref());
                event = linked == null ? TreeView.FLAG.strip() + " none yet" : linked.getName();
            }
            this.childmodel.addRow(new Object[]{child.getName(), child.getType(),
                    child.getChildgrouptype().xmlName(), event});
        }
        this.childtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.childtable.setFillsViewportHeight(true);
        this.childtable.getSelectionModel().addListSelectionListener(e -> enableChildButtons());
        this.childtable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2) openChild();
            }
        });
        JScrollPane scroll = new JScrollPane(this.childtable);
        scroll.setPreferredSize(new Dimension(300, 110));
        panel.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttons.setOpaque(false);
        this.childbuttons.add(addButton(buttons, "Open", this::openChild));
        this.upbutton = addButton(buttons, "Move Up", () -> moveChild(true));
        this.downbutton = addButton(buttons, "Move Down", () -> moveChild(false));
        this.duplicatebutton = addButton(buttons, "Duplicate", this::duplicateChild);
        this.childbuttons.add(addButton(buttons, "Delete", this::deleteChild));
        panel.add(buttons, BorderLayout.SOUTH);
        int row = anode.getChildren().indexOf(lastselectedchild);
        if (row >= 0)
        {
            this.childtable.setRowSelectionInterval(row, row);
        }
        enableChildButtons();
        return panel;
    }

    private JPanel makeButtons(ElhNode anode)
    {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttons.setOpaque(false);
        JButton save = addButton(buttons, "Save Edits", this::saveEdits);
        save.setFont(save.getFont().deriveFont(Font.BOLD));
        JButton newchild = addButton(buttons, "New Child ▾", null);
        newchild.addActionListener(e -> makeNewChildMenu().show(newchild, 0, newchild.getHeight()));
        JButton insertabove = addButton(buttons, "Insert Above Children", this::insertAbove);
        // a new action can only be put above the children of an action or entity
        insertabove.setEnabled(anode.is(NodeType.ACTION) || anode.is(NodeType.ENTITY));
        JButton viewparent = addButton(buttons, "View Parent", () -> this.app.currenttreeview.actions("VIEW PARENT", anode));
        viewparent.setEnabled(anode.getParent() != null);
        return buttons;
    }

    private static JButton addButton(JPanel buttons, String label, Runnable action)
    {
        JButton button = new JButton(label);
        if (action != null)
        {
            button.addActionListener(e -> action.run());
        }
        buttons.add(button);
        return button;
    }

    private ElhNode selectedChild()
    {
        int row = this.childtable.getSelectedRow();
        return row < 0 ? null : this.activenode.getChildren().get(row);
    }

    private void enableChildButtons()
    {
        int row = this.childtable.getSelectedRow();
        boolean selected = row >= 0;
        this.childbuttons.forEach(b -> b.setEnabled(selected));
        this.upbutton.setEnabled(selected && row > 0);
        this.downbutton.setEnabled(selected && row < this.childmodel.getRowCount() - 1);
        this.duplicatebutton.setEnabled(selected && this.activenode.canDuplicateChildren());
        lastselectedchild = selectedChild();
    }

    // the tree and the panels are remade after a change
    private void changed()
    {
        this.app.maketree();
        this.app.refresh();
    }

    private void openChild()
    {
        ElhNode child = selectedChild();
        if (child == null) return;
        this.app.currenttreeview.setselections(child);
        this.app.refresh();
    }

    private void moveChild(boolean up)
    {
        ElhNode child = selectedChild();
        if (child == null) return;
        if (up)
        {
            child.moveleft();
        } else
        {
            child.moveright();
        }
        changed();
    }

    private void duplicateChild()
    {
        ElhNode child = selectedChild();
        if (child == null) return;
        this.activenode.duplicateChild(child);
        changed();
    }

    private void deleteChild()
    {
        ElhNode child = selectedChild();
        if (child == null) return;
        if (child.countChildren() > 0 && JOptionPane.showConfirmDialog(this, "Delete " + child.getName()
                        + " and everything below it?", "Delete", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
        {
            return;
        }
        this.activenode.deleteChild(child);
        lastselectedchild = null;
        changed();
    }

    private void saveEdits()
    {
        ElhNode node = this.activenode;
        node.applyEdits(this.namefield.getText().trim(),
                NodeType.fromXml((String) this.typefield.getSelectedItem()),
                ChildGrouping.fromXml((String) this.groupingfield.getSelectedItem()),
                this.stylefield.getText().trim());
        if (this.eventfield != null)
        {
            node.setEventref(this.eventfield.getSelectedItem() instanceof ProjectEvent event ? event.getId() : null);
        }
        if (this.conditionfield != null)
        {
            node.setCondition(this.conditionfield.getText());
            node.setResult(this.resultfield.getText());
        }
        // only actions can be part of an event
        if (!node.is(NodeType.ACTION))
        {
            node.setEventref(null);
        }
        this.app.maketree();
        this.app.currenttreeview.setselections(node);
        this.app.refresh();
    }

    private JPopupMenu makeNewChildMenu()
    {
        JPopupMenu menu = new JPopupMenu();
        addNewChildItem(menu, "Entity", NodeType.ENTITY);
        addNewChildItem(menu, "Role", NodeType.ROLE);
        addNewChildItem(menu, "Action", NodeType.ACTION);
        return menu;
    }

    private void addNewChildItem(JPopupMenu menu, String label, NodeType type)
    {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> newChild(type));
        menu.add(item);
    }

    private void newChild(NodeType type)
    {
        ElhNode node = this.activenode;
        if (type == NodeType.ACTION)
        {
            if (this.app.addAction(name -> node.addChild(type.xmlName(), name)) == null) return;
        } else
        {
            String name = this.app.askNodeName(type.xmlName());
            if (name == null) return;
            node.addChild(type.xmlName(), name);
        }
        changed();
    }

    private void insertAbove()
    {
        String name = this.app.askNodeName("action");
        if (name == null) return;
        this.activenode.insertabovechildren(name);
        changed();
    }
}
