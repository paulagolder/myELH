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

// The project screen: the project's details, then two tabs. Entities lists
// the project's entities and where each is kept. Events lists the project's
// events against the entities that use them (the entity-event matrix); to
// fill it every entity is read.
public class ProjectView extends JPanel
{
    private static final long serialVersionUID = 1L;
    static final int ENTITIESTAB = 0;
    static final int EVENTSTAB = 1;
    private final MyElh app;
    private final JLabel title = new JLabel();
    private final JLabel author = new JLabel();
    private final JLabel folder = new JLabel();
    private final JTabbedPane tabs = new JTabbedPane();
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
        setLayout(new BorderLayout());
        setBorder(jswStyle.makeLineBorder());
        JPanel details = new JPanel(new GridLayout(3, 1));
        details.setBackground(Color.decode(jswTheme.SELECTAREA));
        this.title.setFont(this.title.getFont().deriveFont(Font.BOLD, this.title.getFont().getSize2D() * 1.4f));
        details.add(this.title);
        details.add(this.author);
        details.add(this.folder);
        add(details, BorderLayout.NORTH);
        this.tabs.addTab("Entities", makeEntityTab());
        this.tabs.addTab("Events", makeEventTab());
        add(this.tabs, BorderLayout.CENTER);
    }

    public void showTab(int tab)
    {
        this.tabs.setSelectedIndex(tab);
    }

    private JPanel makeEntityTab()
    {
        JPanel tab = new JPanel(new BorderLayout());
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
        tab.add(new JScrollPane(this.table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton(buttons, "Open", () -> actOnEntity(app::goToEntity));
        addButton(buttons, "New Entity", app::newEntity);
        addButton(buttons, "Import Entity...", app::importEntity);
        addButton(buttons, "Move to Own File / into project.xml", () -> actOnEntity(app::moveEntity));
        addButton(buttons, "Delete Latest Version", () -> actOnEntity(app::deleteLatestVersion));
        addButton(buttons, "Remove from Project", () -> actOnEntity(app::removeEntity));
        tab.add(buttons, BorderLayout.SOUTH);
        return tab;
    }

    private JPanel makeEventTab()
    {
        JPanel tab = new JPanel(new BorderLayout());
        this.eventtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.eventtable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.eventtable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {
                    renameEvent();
                }
            }
        });
        tab.add(new JScrollPane(this.eventtable), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton(buttons, "Add event", this::addEvent);
        addButton(buttons, "Rename", this::renameEvent);
        addButton(buttons, "Action", this::editAction);
        addButton(buttons, "Move to Own File / into project.xml", this::moveEvent);
        addButton(buttons, "Delete event", this::deleteEvent);
        addButton(buttons, "Collect events from entities", app::collectEvents);
        tab.add(buttons, BorderLayout.SOUTH);
        return tab;
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

    // fills the screen from the project as it now is
    public void show(Project project)
    {
        this.project = project;
        this.title.setText(project.getName());
        this.author.setText("Author: " + (project.getAuthor() == null ? "" : project.getAuthor()));
        this.folder.setText("Folder: " + project.getFolder());
        ProjectEntity selected = selectedEntity();
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
        int row = this.rowentities.indexOf(selected);
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
    // the entity uses the event, or shows the entity's own wording where that
    // differs from the event's name.
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
        List<Object> columns = new ArrayList<>(List.of("Event", "Kept in", "Used in"));
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
            int usedin = 0;
            for (Map<String, List<String>> found : uses)
            {
                List<String> localnames = found.get(event.getId());
                if (localnames == null)
                {
                    row.add("");
                    continue;
                }
                usedin++;
                List<String> different = localnames.stream()
                        .filter(n -> !n.equalsIgnoreCase(event.getName())).distinct().toList();
                row.add(different.isEmpty() ? "✓" : String.join(", ", different));
            }
            row.set(USEDINCOLUMN, usedin);
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
        if (EventActions.addEvent(this, this.project) != null) afterEventChange();
    }

    private void renameEvent()
    {
        ProjectEvent event = selectedEvent();
        if (event != null && EventActions.renameEvent(this, this.project, event)) afterEventChange();
    }

    private void editAction()
    {
        ProjectEvent event = selectedEvent();
        if (event != null && EventActions.editAction(this, this.project, event)) afterEventChange();
    }

    private void moveEvent()
    {
        ProjectEvent event = selectedEvent();
        if (event != null) this.app.moveEvent(event);
    }

    private void deleteEvent()
    {
        int row = this.eventtable.getSelectedRow();
        if (row < 0) return;
        int usedin = (Integer) this.eventmodel.getValueAt(row, USEDINCOLUMN);
        if (EventActions.deleteEvent(this, this.project, this.rowevents.get(row), usedin)) afterEventChange();
    }

    private void afterEventChange()
    {
        show(this.project);
        this.app.maketree();
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
