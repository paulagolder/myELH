package org.lerot.myELH;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// The project window: the project tree, with both its entities and its
// events, on the left; the project's details on the right, above the
// entities table (when Entities is selected) or the event/entity matrix
// (when Events is). Selecting an entity or an event goes to the Entities or
// Events window with it selected there.
public class ProjectView extends JPanel
{
    private static final long serialVersionUID = 1L;
    static final int ENTITIESTAB = 0;
    static final int EVENTSTAB = 1;
    private static final String ENTITIESCARD = "entities";
    private static final String EVENTSCARD = "events";
    private final MyElh app;
    private final TreeView tree;
    private final JLabel title = new JLabel();
    private final JLabel author = new JLabel();
    private final JLabel folder = new JLabel();
    private final CardLayout cards = new CardLayout();
    private final JPanel cardpanel = new JPanel(this.cards);
    private final DefaultTableModel model = new ReadOnlyModel(new Object[]{"Entity", "Kept in", "Versions"});
    private final JTable table = new JTable(this.model);
    // the entity of each row of the entities table
    private final List<ProjectEntity> rowentities = new ArrayList<>();
    private final DefaultTableModel eventmodel = new ReadOnlyModel(new Object[0]);
    private final JTable eventtable = new JTable(this.eventmodel);
    // the event of each row of the events table
    private final List<ProjectEvent> rowevents = new ArrayList<>();
    private static final int USEDINCOLUMN = 2;
    private Project project;

    public ProjectView(MyElh app)
    {
        this.app = app;
        this.tree = new TreeView(app, TreeView.Mode.PROJECT, new TreeView.Listener()
        {
            public void selected(Object shown)
            {
                switch (shown)
                {
                    case ProjectEntity entity -> app.goToEntity(entity);
                    case ProjectEvent event -> app.showEventInTab(event);
                    case TreeView.Heading heading when heading == TreeView.Heading.EVENTS ->
                            cards.show(cardpanel, EVENTSCARD);
                    case null, default -> cards.show(cardpanel, ENTITIESCARD);
                }
            }

            public void popup(Object shown, JPopupMenu menu)
            {
                if (shown == TreeView.Heading.EVENTS)
                {
                    TreeView.addItem(menu, "New Event", e -> app.newEvent());
                } else if (shown == TreeView.Heading.ENTITIES)
                {
                    TreeView.addItem(menu, "New Entity", e -> app.newEntity());
                    TreeView.addItem(menu, "Import Entity...", e -> app.importEntity());
                }
            }
        });
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        JScrollPane treescroll = new JScrollPane(this.tree);
        treescroll.setPreferredSize(new Dimension(240, 200));
        // wide enough for the flags, however wide the rest of the window wants to be
        treescroll.setMinimumSize(new Dimension(240, 100));
        treescroll.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));
        add(treescroll);
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(jswStyle.makeLineBorder());
        JPanel details = new JPanel(new GridLayout(3, 1));
        details.setBackground(Color.decode(jswTheme.SELECTAREA));
        this.title.setFont(this.title.getFont().deriveFont(Font.BOLD, this.title.getFont().getSize2D() * 1.4f));
        details.add(this.title);
        details.add(this.author);
        details.add(this.folder);
        right.add(details, BorderLayout.NORTH);
        this.cardpanel.add(makeEntityCard(), ENTITIESCARD);
        this.cardpanel.add(makeEventCard(), EVENTSCARD);
        right.add(this.cardpanel, BorderLayout.CENTER);
        add(right);
    }

    // shows the entities or the events, selecting that heading in the tree
    public void showTab(int tab)
    {
        this.tree.select(tab == EVENTSTAB ? TreeView.Heading.EVENTS : TreeView.Heading.ENTITIES);
        this.cards.show(this.cardpanel, tab == EVENTSTAB ? EVENTSCARD : ENTITIESCARD);
    }

    private JPanel makeEntityCard()
    {
        JPanel card = new JPanel(new BorderLayout());
        this.table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.table.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {
                    actOnEntity(app::goToEntity);
                }
            }
        });
        card.add(new JScrollPane(this.table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton(buttons, "Open", () -> actOnEntity(app::goToEntity));
        addButton(buttons, "New Entity", app::newEntity);
        addButton(buttons, "Import Entity...", app::importEntity);
        addButton(buttons, "Move to Own File / into project.xml", () -> actOnEntity(app::moveEntity));
        addButton(buttons, "Delete Latest Version", () -> actOnEntity(app::deleteLatestVersion));
        addButton(buttons, "Remove from Project", () -> actOnEntity(app::removeEntity));
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    private JPanel makeEventCard()
    {
        JPanel card = new JPanel(new BorderLayout());
        this.eventtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.eventtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.eventtable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {
                    ProjectEvent event = selectedEvent();
                    if (event != null) app.showEventInTab(event);
                }
            }
        });
        card.add(new JScrollPane(this.eventtable), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton(buttons, "Open", () ->
        {
            ProjectEvent event = selectedEvent();
            if (event != null) app.showEventInTab(event);
        });
        addButton(buttons, "New Event", this::addEvent);
        addButton(buttons, "Rename", this::renameEvent);
        addButton(buttons, "Description", this::editDescription);
        addButton(buttons, "Move to Own File / into project.xml", this::moveEvent);
        addButton(buttons, "Delete Event", this::deleteEvent);
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    private static void addButton(JPanel buttons, String label, Runnable action)
    {
        JButton button = new JButton(label);
        button.addActionListener(e -> action.run());
        buttons.add(button);
    }

    private void actOnEntity(Consumer<ProjectEntity> action)
    {
        int row = this.table.getSelectedRow();
        if (row >= 0)
        {
            action.accept(this.rowentities.get(row));
        }
    }

    // fills the window from the project as it now is
    public void show(Project project)
    {
        this.project = project;
        List<Object> selected = this.tree.selectedObjects();
        this.tree.maketree(project);
        if (selected.size() == 1 && selected.getFirst() instanceof TreeView.Heading heading)
        {
            this.tree.select(heading);
        }
        this.title.setText(project.getName());
        this.author.setText("Author: " + (project.getAuthor() == null ? "" : project.getAuthor()));
        this.folder.setText("Folder: " + project.getFolder());
        ProjectEntity selectedentity = selectedEntity();
        this.model.setRowCount(0);
        this.rowentities.clear();
        for (ProjectEntity entity : project.getEntities())
        {
            List<File> versions = project.entityVersions(entity);
            String keptin = entity.isInline() ? Project.PROJECTFILE
                    : versions.isEmpty() ? entity.getFile() + " (no file)" : versions.getLast().getName();
            this.model.addRow(new Object[]{entity.getName() + (entity.isModified() ? " *" : ""), keptin,
                    entity.isInline() ? "" : versions.size()});
            this.rowentities.add(entity);
        }
        int row = this.rowentities.indexOf(selectedentity);
        if (row >= 0)
        {
            this.table.setRowSelectionInterval(row, row);
        }
        showEvents();
    }

    private ProjectEntity selectedEntity()
    {
        int row = this.table.getSelectedRow();
        return row < 0 || row >= this.rowentities.size() ? null : this.rowentities.get(row);
    }

    // one row per project event, one column per entity. A cell is ticked if
    // the entity has an action in the event, or shows the action's own
    // wording where that differs from the event's name. An event with fewer
    // than two actions is flagged.
    private void showEvents()
    {
        List<Project.LifeHistory> histories = this.app.lifeHistories();
        List<Map<String, List<String>>> uses = new ArrayList<>();
        for (Project.LifeHistory history : histories)
        {
            Map<String, List<String>> found = new HashMap<>();
            history.top().collectEventrefs(found);
            uses.add(found);
        }
        List<Object> columns = new ArrayList<>(List.of("Event", "Kept in", "Actions"));
        histories.forEach(h -> columns.add(h.entity().getName()));
        this.eventmodel.setColumnIdentifiers(columns.toArray());
        this.eventmodel.setRowCount(0);
        this.rowevents.clear();
        for (ProjectEvent event : this.project.getEvents())
        {
            List<Object> row = new ArrayList<>();
            row.add(event.getName());
            if (event.isInline())
            {
                row.add(Project.PROJECTFILE);
            } else
            {
                List<File> versions = this.project.eventVersions(event);
                row.add(versions.isEmpty() ? event.getFile() + " (no file)" : versions.getLast().getName());
            }
            row.add(null);
            int actions = 0;
            for (Map<String, List<String>> found : uses)
            {
                List<String> localnames = found.get(event.getId());
                if (localnames == null)
                {
                    row.add("");
                    continue;
                }
                actions += localnames.size();
                List<String> different = localnames.stream()
                        .filter(n -> !n.equalsIgnoreCase(event.getName())).distinct().toList();
                row.add(different.isEmpty() ? "✓" : String.join(", ", different));
            }
            row.set(USEDINCOLUMN, actions < 2 ? actions + TreeView.FLAG : String.valueOf(actions));
            this.eventmodel.addRow(row.toArray());
            this.rowevents.add(event);
        }
        this.eventtable.getColumnModel().getColumn(0).setPreferredWidth(160);
        this.eventtable.getColumnModel().getColumn(1).setPreferredWidth(130);
        for (int c = USEDINCOLUMN; c < this.eventtable.getColumnCount(); c++)
        {
            this.eventtable.getColumnModel().getColumn(c).setPreferredWidth(c == USEDINCOLUMN ? 60 : 110);
        }
    }

    private ProjectEvent selectedEvent()
    {
        int row = this.eventtable.getSelectedRow();
        return row < 0 ? null : this.rowevents.get(row);
    }

    private void addEvent()
    {
        if (EventActions.addEvent(this, this.project) != null) this.app.eventsChanged();
    }

    private void renameEvent()
    {
        ProjectEvent event = selectedEvent();
        if (event != null && EventActions.renameEvent(this, this.project, event)) this.app.eventsChanged();
    }

    private void editDescription()
    {
        ProjectEvent event = selectedEvent();
        if (event != null && EventActions.editDescription(this, this.project, event)) this.app.eventsChanged();
    }

    private void moveEvent()
    {
        ProjectEvent event = selectedEvent();
        if (event != null) this.app.moveEvent(event);
    }

    private void deleteEvent()
    {
        ProjectEvent event = selectedEvent();
        if (event == null) return;
        long usedin = this.project.actionsOf(event, this.project.allActions()).stream()
                .map(EntityAction::entity).distinct().count();
        if (EventActions.deleteEvent(this, this.project, event, (int) usedin)) this.app.eventsChanged();
    }

    private static class ReadOnlyModel extends DefaultTableModel
    {
        private static final long serialVersionUID = 1L;

        ReadOnlyModel(Object[] columns)
        {
            super(columns, 0);
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    }
}
