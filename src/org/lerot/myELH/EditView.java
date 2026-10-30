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
// below. An action also has a description. Which event an action is part
// of, and its condition and result there, are dealt with in the Events
// window.
public class EditView extends JPanel
{
    private static final long serialVersionUID = 1L;
    // the child selected when the panel was last made, selected again when
    // it is remade after a change (moving a child up, say)
    private static ElhNode lastselectedchild;
    protected final MyElh app;
    ElhNode activenode;
    private final JLabel title = new JLabel();
    private final JLabel subtitle = new JLabel();
    private final JTextField namefield = new JTextField(20);
    private final JComboBox<String> typefield = new JComboBox<>();
    private final JComboBox<ChildGrouping> structurefield = new JComboBox<>();
    // only there for an action
    private JTextArea descriptionfield;
    private final DefaultTableModel childmodel = new DefaultTableModel(
            new Object[]{"Child", "Type", "Structure"}, 0)
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
    // only there for an entity or a role: its attributes, one row each
    private DefaultTableModel attributemodel;
    private JTable attributetable;
    private static final int NAMECOL = 0;
    private static final int TYPECOL = 1;
    private static final int REFERSCOL = 2;
    private static final int KEYCOL = 3;
    private static final int NOTECOL = 4;

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
        if (anode.getParent() == null || anode.is(NodeType.ENTITY) || anode.is(NodeType.ROLE))
        {
            top.add(makeAttributePanel(anode, project), BorderLayout.SOUTH);
        }
        add(top, BorderLayout.NORTH);
        add(makeChildPanel(anode), BorderLayout.CENTER);
        add(makeButtons(anode), BorderLayout.SOUTH);
    }

    // an entity's or role's attributes, editable in place: name, type, the
    // entity a reference refers to, whether it is a key, and a note
    private JPanel makeAttributePanel(ElhNode anode, Project project)
    {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        JLabel heading = new JLabel("ATTRIBUTES:");
        // a role can also name those of the entity and roles it belongs to
        List<String> inherited = new ArrayList<>();
        for (ElhNode up = anode.getParent(); up != null; up = up.getParent())
        {
            for (Attribute attribute : up.getAttributes())
            {
                inherited.add(attribute.name() + " (" + up.getName() + ")");
            }
        }
        if (!inherited.isEmpty())
        {
            heading.setText("ATTRIBUTES:   also has " + String.join(", ", inherited));
        }
        panel.add(heading, BorderLayout.NORTH);
        this.attributemodel = new DefaultTableModel(new Object[]{"Name", "Type", "Refers to", "Key", "Note"}, 0)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<?> getColumnClass(int column)
            {
                return column == KEYCOL ? Boolean.class : column == TYPECOL ? Attribute.Type.class : String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column)
            {
                // only a reference refers to another entity
                return column != REFERSCOL || getValueAt(row, TYPECOL) == Attribute.Type.REFERENCE;
            }
        };
        for (Attribute attribute : anode.getAttributes())
        {
            this.attributemodel.addRow(new Object[]{attribute.name(), attribute.type(),
                    attribute.refers() == null ? "" : attribute.refers(), attribute.key(), attribute.note()});
        }
        this.attributetable = new JTable(this.attributemodel);
        this.attributetable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.attributetable.setFillsViewportHeight(true);
        this.attributetable.setDefaultEditor(Attribute.Type.class,
                new DefaultCellEditor(new JComboBox<>(Attribute.Type.values())));
        JComboBox<String> entities = new JComboBox<>();
        if (project != null)
        {
            project.getEntities().forEach(e -> entities.addItem(e.getName()));
        }
        entities.setEditable(true);
        this.attributetable.getColumnModel().getColumn(REFERSCOL).setCellEditor(new DefaultCellEditor(entities));
        this.attributetable.getColumnModel().getColumn(KEYCOL).setMaxWidth(50);
        JScrollPane scroll = new JScrollPane(this.attributetable);
        scroll.setPreferredSize(new Dimension(300, 100));
        panel.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        buttons.setOpaque(false);
        addButton(buttons, "Add Attribute", () ->
        {
            this.attributemodel.addRow(new Object[]{"", Attribute.Type.TEXT, "", false, ""});
            int row = this.attributemodel.getRowCount() - 1;
            this.attributetable.setRowSelectionInterval(row, row);
            this.attributetable.editCellAt(row, NAMECOL);
            this.attributetable.getEditorComponent().requestFocusInWindow();
        });
        addButton(buttons, "Remove Attribute", () ->
        {
            int row = this.attributetable.getSelectedRow();
            if (row >= 0)
            {
                if (this.attributetable.isEditing()) this.attributetable.getCellEditor().cancelCellEditing();
                this.attributemodel.removeRow(row);
            }
        });
        buttons.add(new JLabel("  (saved with Save Edits)"));
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // the attributes as the table now has them, or null, after saying why,
    // if two have the same name
    private List<Attribute> attributesFromTable()
    {
        if (this.attributetable.isEditing())
        {
            this.attributetable.getCellEditor().stopCellEditing();
        }
        List<Attribute> attributes = new ArrayList<>();
        for (int row = 0; row < this.attributemodel.getRowCount(); row++)
        {
            String name = String.valueOf(this.attributemodel.getValueAt(row, NAMECOL)).strip();
            if (name.isEmpty())
            {
                continue;
            }
            if (attributes.stream().anyMatch(a -> a.name().equalsIgnoreCase(name)))
            {
                JOptionPane.showMessageDialog(this, "There are two attributes called " + name, "Attributes",
                        JOptionPane.ERROR_MESSAGE);
                return null;
            }
            attributes.add(new Attribute(name, (Attribute.Type) this.attributemodel.getValueAt(row, TYPECOL),
                    String.valueOf(this.attributemodel.getValueAt(row, REFERSCOL)),
                    Boolean.TRUE.equals(this.attributemodel.getValueAt(row, KEYCOL)),
                    String.valueOf(this.attributemodel.getValueAt(row, NOTECOL))));
        }
        return attributes;
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
        for (ChildGrouping structure : structureChoices(anode.getChildgrouptype()))
        {
            this.structurefield.addItem(structure);
        }
        this.structurefield.setSelectedItem(anode.getChildgrouptype());
        this.structurefield.setRenderer(new DefaultListCellRenderer()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus)
            {
                Object shown = value instanceof ChildGrouping structure ? structure.label : value;
                return super.getListCellRendererComponent(list, shown, index, isSelected, cellHasFocus);
            }
        });
        this.structurefield.setToolTipText("How this node's children relate: in sequence, one of them, "
                + "repeated, or as roles in parallel");
        addRow(form, row++, "STRUCTURE:", this.structurefield);
        if (anode.is(NodeType.ACTION))
        {
            this.descriptionfield = textArea(anode.getDescription());
            this.descriptionfield.setRows(4);
            addRow(form, row++, "DESCRIPTION:", new JScrollPane(this.descriptionfield));
        }
        if (anode.isLeafAction())
        {
            addRow(form, row++, "STATE INDICATOR:", new JLabel(describeState(anode)));
        }
        return form;
    }

    // the action's state indicator in words, worked out from its life history
    private static String describeState(ElhNode action)
    {
        ElhNode top = action;
        while (top.getParent() != null)
        {
            top = top.getParent();
        }
        StateIndicators.ActionState state = StateIndicators.of(top).stateOf(action);
        if (state == null)
        {
            return "";
        }
        String prior = state.validprior().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(", "));
        String before = state.canstart() ? (prior.isEmpty() ? "- (not yet begun)" : "- (not yet begun), " + prior) : prior;
        return state.indicator() + ":  valid before " + (before.isEmpty() ? "none" : before)
                + ";  sets it to " + state.setsto() + (state.canend() ? ";  can end the life" : "");
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

    // the usual choices, plus the node's own structure if it is not one of them (entitygroup)
    private static List<ChildGrouping> structureChoices(ChildGrouping current)
    {
        List<ChildGrouping> choices = new ArrayList<>(ChildGrouping.CHOICES);
        if (!choices.contains(current))
        {
            choices.add(current);
        }
        return choices;
    }

    // the node's children, and what can be done to the one selected
    private JPanel makeChildPanel(ElhNode anode)
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
            this.childmodel.addRow(new Object[]{child.getName(), child.getType(), child.getChildgrouptype().label});
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
        List<Attribute> attributes = this.attributemodel == null ? null : attributesFromTable();
        if (this.attributemodel != null && attributes == null)
        {
            return;
        }
        if (attributes != null)
        {
            node.setAttributes(attributes);
        }
        node.applyEdits(this.namefield.getText().trim(),
                NodeType.fromXml((String) this.typefield.getSelectedItem()),
                (ChildGrouping) this.structurefield.getSelectedItem());
        if (this.descriptionfield != null)
        {
            node.setDescription(this.descriptionfield.getText());
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
