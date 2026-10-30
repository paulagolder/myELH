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

// The Events window. On the left, the events, each with its actions, and
// below them a browser of every entity's actions, in which the actions not
// yet part of an event, and the branches leading to them, are flagged. On
// the right, the event selected, or the actions chosen in the browser.
//
// An event happens in the world and triggers an action in each of two or
// more entities; it has a Description. Its actions are not stored with it
// but found in the entities' life histories: the leaf action nodes linked
// to it. Each action has its own Condition and Result (its CORD Action); the
// event can happen when every Condition holds, and then every Result
// happens. Double-clicking an action in the browser adds it to the event
// shown.
public class EventView extends JPanel
{
    private static final long serialVersionUID = 1L;
    private static final String NOTHING = "nothing";
    private static final String EVENT = "event";
    private static final String CHOSEN = "chosen";
    private final MyElh app;
    // the events, each with its actions
    private final TreeView eventtree;
    // every entity's actions
    private final TreeView browser;
    private JButton addselectedbutton;
    private final CardLayout cards = new CardLayout();
    private final JPanel right = new JPanel(this.cards);
    // the event shown
    private ProjectEvent shownevent;
    private final JTextField namefield = new JTextField(20);
    private final JTextArea descriptionfield = new JTextArea(3, 30);
    private final JLabel keptin = new JLabel();
    private final JLabel lonely = new JLabel();
    private static final int CONDITIONCOLUMN = 2;
    private static final int RESULTCOLUMN = 3;
    private final DefaultTableModel actionmodel = new DefaultTableModel(
            new Object[]{"Entity", "Action", "Condition", "Result", "Where"}, 0)
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return column == CONDITIONCOLUMN || column == RESULTCOLUMN;
        }
    };
    private final JTable actiontable = new JTable(this.actionmodel);
    // the action of each row of the table
    private final List<EntityAction> rowactions = new ArrayList<>();
    // the whole transaction: every action's condition and result
    private final JTextArea transactionfield = new JTextArea(6, 30);
    // the actions selected in the browser
    private final List<EntityAction> chosenactions = new ArrayList<>();
    private final JTextArea chosenlist = new JTextArea(6, 30);
    private final JComboBox<ProjectEvent> eventchoice = new JComboBox<>();

    public EventView(MyElh app)
    {
        this.app = app;
        this.eventtree = new TreeView(app, TreeView.Mode.EVENTS, new TreeView.Listener()
        {
            public void selected(Object shown)
            {
                eventTreeSelected();
            }

            public void popup(Object shown, JPopupMenu menu)
            {
                treePopup(shown, menu);
            }
        });
        this.browser = new TreeView(app, TreeView.Mode.ACTIONS, new TreeView.Listener()
        {
            public void selected(Object shown)
            {
                browserSelected();
            }

            public void popup(Object shown, JPopupMenu menu)
            {
                browserPopup(shown, menu);
            }

            public void opened(Object shown)
            {
                if (shown instanceof EntityAction action) addToShownEvent(List.of(action));
            }
        });
        setLayout(new BorderLayout());
        JSplitPane left = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(this.eventtree),
                new JScrollPane(this.browser));
        left.setResizeWeight(0.4);
        left.setContinuousLayout(true);
        left.setPreferredSize(new Dimension(260, 200));
        // wide enough for the flags, however wide the rest of the window wants to be
        left.setMinimumSize(new Dimension(260, 100));
        left.setMaximumSize(new Dimension(340, Integer.MAX_VALUE));
        add(left, BorderLayout.WEST);
        this.right.add(makeNothingPanel(), NOTHING);
        this.right.add(makeEventPanel(), EVENT);
        this.right.add(makeChosenPanel(), CHOSEN);
        add(this.right, BorderLayout.CENTER);
    }

    private JPanel styled(JPanel panel)
    {
        panel.setBorder(BorderFactory.createCompoundBorder(jswStyle.makeLineBorder(),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        panel.setBackground(Color.decode(jswTheme.EDITAREA));
        return panel;
    }

    private JPanel makeNothingPanel()
    {
        JPanel panel = styled(new JPanel(new BorderLayout()));
        JTextArea help = new JTextArea("""
                An event happens in the world and triggers an action in each of
                two or more entities: the leaf action nodes of their life histories.

                Select an event above to see and edit it.

                Below, every entity's actions: those not yet part of an event,
                and the branches leading to them, are flagged. With an event
                shown, double-click an action to add it to the event. Otherwise
                select one or more (Ctrl-click) to make a new event of them or
                add them to one.""");
        help.setEditable(false);
        help.setOpaque(false);
        panel.add(help, BorderLayout.NORTH);
        return panel;
    }

    private JPanel makeEventPanel()
    {
        JPanel panel = styled(new JPanel(new BorderLayout(4, 4)));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("EVENT:"), c);
        c.gridy = 1;
        form.add(new JLabel("DESCRIPTION:"), c);
        c.gridy = 2;
        form.add(new JLabel("KEPT IN:"), c);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        form.add(this.namefield, c);
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        this.descriptionfield.setLineWrap(true);
        this.descriptionfield.setWrapStyleWord(true);
        form.add(new JScrollPane(this.descriptionfield), c);
        c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(this.keptin, c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        this.lonely.setForeground(TreeView.FLAGGED);
        form.add(this.lonely, c);
        panel.add(form, BorderLayout.NORTH);
        JPanel actionpanel = new JPanel(new BorderLayout());
        actionpanel.setOpaque(false);
        actionpanel.add(new JLabel("ACTIONS (the entities' parts in this event):"), BorderLayout.NORTH);
        this.actiontable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.actiontable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                // a double click on condition or result edits it instead
                int column = actiontable.columnAtPoint(e.getPoint());
                if (e.getClickCount() == 2 && column != CONDITIONCOLUMN && column != RESULTCOLUMN) goToAction();
            }
        });
        JScrollPane scroll = new JScrollPane(this.actiontable);
        scroll.setPreferredSize(new Dimension(300, 120));
        actionpanel.add(scroll, BorderLayout.CENTER);
        this.transactionfield.setEditable(false);
        this.transactionfield.setLineWrap(true);
        this.transactionfield.setWrapStyleWord(true);
        JPanel transactionpanel = new JPanel(new BorderLayout());
        transactionpanel.setOpaque(false);
        transactionpanel.add(new JLabel("TRANSACTION:"), BorderLayout.NORTH);
        transactionpanel.add(new JScrollPane(this.transactionfield), BorderLayout.CENTER);
        actionpanel.add(transactionpanel, BorderLayout.SOUTH);
        panel.add(actionpanel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        addButton(buttons, "Save Edits", this::saveEdits);
        addButton(buttons, "Go to Action", this::goToAction);
        addButton(buttons, "Take Action out of Event", this::unlinkAction);
        this.addselectedbutton = new JButton("Add Selected Actions");
        this.addselectedbutton.setToolTipText("Adds the actions selected in the browser below to this event "
                + "(or double-click one there)");
        this.addselectedbutton.addActionListener(e -> addToShownEvent(List.copyOf(this.chosenactions)));
        buttons.add(this.addselectedbutton);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel makeChosenPanel()
    {
        JPanel panel = styled(new JPanel(new BorderLayout(4, 4)));
        panel.add(new JLabel("ACTIONS CHOSEN IN THE BROWSER (Ctrl-click to choose more):"), BorderLayout.NORTH);
        this.chosenlist.setEditable(false);
        panel.add(new JScrollPane(this.chosenlist), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        addButton(buttons, "New Event from These Actions", this::newEventFromChosen);
        buttons.add(new JLabel("  or add them to"));
        buttons.add(this.eventchoice);
        addButton(buttons, "Add", this::addChosenToEvent);
        addButton(buttons, "Go to Action", () ->
        {
            if (!this.chosenactions.isEmpty()) goTo(this.chosenactions.getFirst());
        });
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private static void addButton(JPanel buttons, String label, Runnable action)
    {
        JButton button = new JButton(label);
        button.addActionListener(e -> action.run());
        buttons.add(button);
    }

    // the tree's right-click menu
    private void treePopup(Object shown, JPopupMenu menu)
    {
        Project project = this.app.getProject();
        if (project == null) return;
        switch (shown)
        {
            case ProjectEvent event ->
            {
                TreeView.addItem(menu, event.isInline() ? "Move to Own File" : "Move into project.xml",
                        e -> this.app.moveEvent(event));
                TreeView.addItem(menu, "Delete Event", e -> deleteEvent());
            }
            case EntityAction action ->
            {
                TreeView.addItem(menu, "Take Action out of Event", e -> unlink(action));
                TreeView.addItem(menu, "Go to Action", e -> goTo(action));
            }
            default -> TreeView.addItem(menu, "New Event", e -> newEvent());
        }
    }

    public ProjectEvent selectedEvent()
    {
        return this.shownevent;
    }

    // the browser's right-click menu
    private void browserPopup(Object shown, JPopupMenu menu)
    {
        Project project = this.app.getProject();
        if (project == null || !(shown instanceof EntityAction action)) return;
        if (this.shownevent != null)
        {
            ProjectEvent event = this.shownevent;
            TreeView.addItem(menu, "Add to " + event.getName(), e -> addToShownEvent(List.copyOf(this.chosenactions)));
        }
        TreeView.addItem(menu, "New Event from Selected Actions", e -> newEventFromChosen());
        if (!project.isUnlinked(action.node()))
        {
            TreeView.addItem(menu, "Take Action out of Event", e -> unlink(action));
        }
        TreeView.addItem(menu, "Go to Action", e -> goTo(action));
    }

    // rebuilds both trees, keeping what is selected in each, and shows it
    public void refresh()
    {
        List<Object> events = this.eventtree.selectedObjects();
        List<Object> actions = this.browser.selectedObjects();
        this.eventtree.maketree(this.app.getProject());
        this.browser.maketree(this.app.getProject());
        if (events.size() == 1)
        {
            this.eventtree.select(events.getFirst());
        }
        actions.forEach(this.browser::addToSelection);
        eventTreeSelected();
        browserSelected();
    }

    // selects the event
    public void selectEvent(ProjectEvent event)
    {
        refresh();
        this.eventtree.select(event);
        eventTreeSelected();
    }

    // selects the action in the browser and, if it is part of an event, shows the event
    public void selectAction(EntityAction action)
    {
        refresh();
        Project project = this.app.getProject();
        this.browser.select(action);
        if (project != null && !project.isUnlinked(action.node()))
        {
            this.eventtree.select(action);
            eventTreeSelected();
        } else
        {
            this.eventtree.clearSelection();
            this.shownevent = null;
        }
        browserSelected();
    }

    // shows on the right the event selected in the events tree, or the
    // event of the action selected there
    private void eventTreeSelected()
    {
        Project project = this.app.getProject();
        List<Object> selected = this.eventtree.selectedObjects();
        this.shownevent = null;
        ProjectEvent event = project == null || selected.isEmpty() ? null : switch (selected.getLast())
        {
            case ProjectEvent e -> e;
            case EntityAction a when !project.isUnlinked(a.node()) -> project.findEvent(a.node().getEventref());
            default -> null;
        };
        if (event == null || !EventActions.loadEvent(this, project, event))
        {
            showChosenOrNothing(project);
        } else
        {
            showEvent(project, event, selected.getLast() instanceof EntityAction a ? a : null);
        }
        enableAddSelected();
        this.app.eventSelected(this.shownevent);
    }

    // the actions selected in the browser: with an event shown they can be
    // added to it; with none, a new event can be made of them
    private void browserSelected()
    {
        this.chosenactions.clear();
        for (Object shown : this.browser.selectedObjects())
        {
            if (shown instanceof EntityAction action) this.chosenactions.add(action);
        }
        if (this.shownevent == null)
        {
            showChosenOrNothing(this.app.getProject());
        }
        enableAddSelected();
    }

    private void showChosenOrNothing(Project project)
    {
        if (project == null || this.chosenactions.isEmpty())
        {
            this.cards.show(this.right, NOTHING);
        } else
        {
            showChosen(project);
        }
    }

    private void enableAddSelected()
    {
        boolean any = this.shownevent != null && this.chosenactions.stream()
                .anyMatch(a -> !this.shownevent.getId().equals(a.node().getEventref()));
        this.addselectedbutton.setEnabled(any);
        this.addselectedbutton.setText(this.chosenactions.size() > 1 ? "Add Selected Actions" : "Add Selected Action");
    }

    // adds the actions to the event shown, asking first about any that are
    // part of another event
    private void addToShownEvent(List<EntityAction> actions)
    {
        ProjectEvent event = this.shownevent;
        if (event == null)
        {
            JOptionPane.showMessageDialog(this, "Select an event above first, then add actions to it.",
                    "Add to event", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<EntityAction> adding = actions.stream().filter(a -> !event.getId().equals(a.node().getEventref())).toList();
        if (adding.isEmpty() || !confirmMoves(adding, event)) return;
        if (this.app.linkActions(adding, event))
        {
            selectEvent(event);
        }
    }

    // true if none of the actions is part of another event, or the user
    // agrees to move those that are
    private boolean confirmMoves(List<EntityAction> actions, ProjectEvent target)
    {
        Project project = this.app.getProject();
        List<String> moving = new ArrayList<>();
        for (EntityAction action : actions)
        {
            ProjectEvent current = project.isUnlinked(action.node()) ? null : project.findEvent(action.node().getEventref());
            if (current != null && current != target)
            {
                moving.add(action + " (now in " + current.getName() + ")");
            }
        }
        return moving.isEmpty() || JOptionPane.showConfirmDialog(this, "These are already part of another event:\n"
                        + String.join("\n", moving) + "\nMove them to " + (target == null ? "the new event" : target.getName())
                        + "?", "Move actions", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
                == JOptionPane.OK_OPTION;
    }

    private void showChosen(Project project)
    {
        StringBuilder text = new StringBuilder();
        for (EntityAction action : this.chosenactions)
        {
            text.append(action).append("    (").append(action.node().getPath()).append(")\n");
        }
        this.chosenlist.setText(text.toString().stripTrailing());
        this.eventchoice.removeAllItems();
        project.getEvents().forEach(this.eventchoice::addItem);
        this.cards.show(this.right, CHOSEN);
    }

    private void showEvent(Project project, ProjectEvent event, EntityAction selectedaction)
    {
        this.shownevent = event;
        this.namefield.setText(event.getName());
        this.descriptionfield.setText(event.getDescription());
        if (event.isInline())
        {
            this.keptin.setText(Project.PROJECTFILE);
        } else
        {
            List<java.io.File> versions = project.eventVersions(event);
            this.keptin.setText(versions.isEmpty() ? event.getFile() + " (no file)" : versions.getLast().getName());
        }
        this.actionmodel.setRowCount(0);
        this.rowactions.clear();
        for (EntityAction action : project.actionsOf(event, project.allActions()))
        {
            ElhNode node = action.node();
            this.rowactions.add(action);
            this.actionmodel.addRow(new Object[]{action.entity().getName(), node.getName(), node.getCondition(),
                    node.getResult(), node.getPath()});
            if (selectedaction != null && node == selectedaction.node())
            {
                int row = this.rowactions.size() - 1;
                this.actiontable.setRowSelectionInterval(row, row);
            }
        }
        int count = this.rowactions.size();
        this.lonely.setText(count >= 2 ? " " : TreeView.FLAG.strip() + " An event should trigger actions in two or "
                + "more entities; this one triggers " + count + ".");
        showTransaction(event);
        this.cards.show(this.right, EVENT);
    }

    // every action's condition, all of which must hold, and every action's
    // result, all of which happen together
    private void showTransaction(ProjectEvent event)
    {
        StringBuilder text = new StringBuilder();
        if (this.rowactions.isEmpty())
        {
            text.append("No entity takes part in ").append(event.getName()).append(" yet.");
        } else
        {
            text.append(event.getName()).append(" can happen when all of these hold:\n");
            for (EntityAction action : this.rowactions)
            {
                text.append("   ").append(describe(event, action)).append(": ")
                        .append(orNone(action.node().getCondition())).append('\n');
            }
            text.append("and then all of these happen together:\n");
            for (EntityAction action : this.rowactions)
            {
                text.append("   ").append(describe(event, action)).append(": ")
                        .append(orNone(action.node().getResult())).append('\n');
            }
        }
        this.transactionfield.setText(text.toString().stripTrailing());
        this.transactionfield.setCaretPosition(0);
    }

    // the entity, with its own wording of the event where that differs
    private static String describe(ProjectEvent event, EntityAction action)
    {
        String wording = action.node().getName();
        return action.entity().getName() + (wording.equalsIgnoreCase(event.getName()) ? "" : " (" + wording + ")");
    }

    private static String orNone(String text)
    {
        return text.isEmpty() ? "(nothing stated)" : text;
    }

    private void saveEdits()
    {
        ProjectEvent event = this.shownevent;
        Project project = this.app.getProject();
        if (event == null || project == null)
        {
            return;
        }
        String name = EventActions.checkName(this, project, this.namefield.getText(), event);
        if (name == null)
        {
            this.namefield.setText(event.getName());
            return;
        }
        event.setName(name);
        event.setDescription(this.descriptionfield.getText().trim());
        EventActions.saveEvent(this, project, event);
        saveActions();
        this.app.eventsChanged();
        selectEvent(event);
    }

    // conditions and results changed in the table go onto their nodes, and
    // the entities changed are saved
    private void saveActions()
    {
        if (this.actiontable.isEditing())
        {
            this.actiontable.getCellEditor().stopCellEditing();
        }
        java.util.Set<ProjectEntity> changed = new java.util.LinkedHashSet<>();
        for (int row = 0; row < this.rowactions.size(); row++)
        {
            EntityAction action = this.rowactions.get(row);
            ElhNode node = action.node();
            String condition = String.valueOf(this.actionmodel.getValueAt(row, CONDITIONCOLUMN)).trim();
            String result = String.valueOf(this.actionmodel.getValueAt(row, RESULTCOLUMN)).trim();
            if (!condition.equals(node.getCondition()) || !result.equals(node.getResult()))
            {
                // the change goes on the node in the opened entity
                ElhNode opened = this.app.openAt(action.entity(), node);
                if (opened == null)
                {
                    continue;
                }
                opened.setCondition(condition);
                opened.setResult(result);
                changed.add(action.entity());
            }
        }
        for (ProjectEntity entity : changed)
        {
            this.app.saveEntity(entity);
        }
    }

    private EntityAction selectedRowAction()
    {
        int row = this.actiontable.getSelectedRow();
        if (row < 0 && this.rowactions.size() == 1)
        {
            row = 0;
        }
        return row < 0 ? null : this.rowactions.get(row);
    }

    private void goToAction()
    {
        EntityAction action = selectedRowAction();
        if (action != null) goTo(action);
    }

    private void goTo(EntityAction action)
    {
        this.app.goToNode(action.entity(), action.node());
    }

    private void unlinkAction()
    {
        EntityAction action = selectedRowAction();
        if (action != null) unlink(action);
    }

    // takes the action out of its event; its entity is saved
    private void unlink(EntityAction action)
    {
        ProjectEvent event = this.app.getProject().findEvent(action.node().getEventref());
        if (JOptionPane.showConfirmDialog(this, "Take " + action + " out of event "
                        + (event == null ? "" : event.getName()) + "?\n" + action.entity().getName() + " is saved.",
                "Take action out of event", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
        {
            return;
        }
        if (this.app.linkActions(List.of(action), null) && event != null)
        {
            selectEvent(event);
        }
    }

    // a new event, named by the user, triggering the chosen actions
    private void newEventFromChosen()
    {
        Project project = this.app.getProject();
        if (project == null || this.chosenactions.isEmpty()) return;
        List<EntityAction> chosen = List.copyOf(this.chosenactions);
        if (!confirmMoves(chosen, null)) return;
        String name = EventActions.askEventName(this, project, "Name of the new event triggering\n"
                + String.join("\n", chosen.stream().map(EntityAction::toString).toList()) + ":",
                chosen.getFirst().node().getName(), null);
        if (name == null) return;
        ProjectEvent event = project.addEvent(name);
        EventActions.save(this, project);
        if (this.app.linkActions(chosen, event))
        {
            selectEvent(event);
        }
    }

    private void addChosenToEvent()
    {
        ProjectEvent event = (ProjectEvent) this.eventchoice.getSelectedItem();
        if (event == null || this.chosenactions.isEmpty()) return;
        List<EntityAction> adding = this.chosenactions.stream()
                .filter(a -> !event.getId().equals(a.node().getEventref())).toList();
        if (adding.isEmpty() || !confirmMoves(adding, event)) return;
        if (this.app.linkActions(adding, event))
        {
            selectEvent(event);
        }
    }

    // the event menu
    public void newEvent()
    {
        Project project = this.app.getProject();
        if (project == null) return;
        ProjectEvent event = EventActions.addEvent(this, project);
        if (event != null)
        {
            this.app.eventsChanged();
            selectEvent(event);
        }
    }

    public void newEventFromSelected()
    {
        browserSelected();
        if (this.chosenactions.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "Select one or more actions in the browser below the events "
                    + "first (Ctrl-click for more than one).", "New Event", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        newEventFromChosen();
    }

    public void deleteEvent()
    {
        ProjectEvent event = this.shownevent;
        Project project = this.app.getProject();
        if (event == null || project == null) return;
        long usedin = this.rowactions.stream().map(EntityAction::entity).distinct().count();
        if (EventActions.deleteEvent(this, project, event, (int) usedin))
        {
            this.app.eventsChanged();
            refresh();
        }
    }
}
