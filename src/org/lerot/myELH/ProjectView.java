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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

// The project screen: the project's details, then two tabs. ELHs lists the
// ELHs in its folder, and each button acts on the latest version file of the
// selected one. Events lists the project's events against the ELHs whose
// latest version uses them (the entity-event matrix).
public class ProjectView extends JPanel
{
    private static final long serialVersionUID = 1L;
    private final JLabel title = new JLabel();
    private final JLabel author = new JLabel();
    private final JLabel folder = new JLabel();
    private final DefaultTableModel model = new ReadOnlyModel(new Object[]{"ELH", "Latest version", "Versions"});
    private final JTable table = new JTable(this.model);
    // the latest version file of each row
    private final List<File> latestfiles = new ArrayList<>();
    private final DefaultTableModel eventmodel = new ReadOnlyModel(new Object[0]);
    private final JTable eventtable = new JTable(this.eventmodel);
    // the event of each row of the events table
    private final List<ProjectEvent> rowevents = new ArrayList<>();
    // the project's ELHs, the one being edited as it is in memory
    private final Supplier<Collection<ProjectElh>> projectelhs;
    private Project project;

    public ProjectView(Consumer<File> openlatest, Consumer<File> deletelatest, Consumer<File> deleteall,
            Runnable collectevents, Supplier<Collection<ProjectElh>> projectelhs)
    {
        this.projectelhs = projectelhs;
        setLayout(new BorderLayout());
        setBorder(jswStyle.makeLineBorder());
        JPanel details = new JPanel(new GridLayout(3, 1));
        details.setBackground(Color.decode(jswTheme.SELECTAREA));
        this.title.setFont(this.title.getFont().deriveFont(Font.BOLD, this.title.getFont().getSize2D() * 1.4f));
        details.add(this.title);
        details.add(this.author);
        details.add(this.folder);
        add(details, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("ELHs", makeElhTab(openlatest, deletelatest, deleteall));
        tabs.addTab("Events", makeEventTab(collectevents));
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel makeElhTab(Consumer<File> openlatest, Consumer<File> deletelatest, Consumer<File> deleteall)
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
                    actOnSelected(openlatest);
                }
            }
        });
        tab.add(new JScrollPane(this.table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton(buttons, "Open latest version", openlatest);
        addButton(buttons, "Delete latest version", deletelatest);
        addButton(buttons, "Delete all versions", deleteall);
        tab.add(buttons, BorderLayout.SOUTH);
        return tab;
    }

    private JPanel makeEventTab(Runnable collectevents)
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
        addEventButton(buttons, "Add event", this::addEvent);
        addEventButton(buttons, "Rename", this::renameEvent);
        addEventButton(buttons, "Action", this::editAction);
        addEventButton(buttons, "Delete event", this::deleteEvent);
        addEventButton(buttons, "Collect events from ELHs", collectevents);
        tab.add(buttons, BorderLayout.SOUTH);
        return tab;
    }

    private void addButton(JPanel buttons, String label, Consumer<File> action)
    {
        JButton button = new JButton(label);
        button.addActionListener(e -> actOnSelected(action));
        buttons.add(button);
    }

    private static void addEventButton(JPanel buttons, String label, Runnable action)
    {
        JButton button = new JButton(label);
        button.addActionListener(e -> action.run());
        buttons.add(button);
    }

    // passes the selected ELH's latest version file to the action
    private void actOnSelected(Consumer<File> action)
    {
        int row = this.table.getSelectedRow();
        if (row >= 0)
        {
            action.accept(this.latestfiles.get(row));
        }
    }

    // fills the screen from the project and its folder as they now are
    public void show(Project project)
    {
        this.project = project;
        this.title.setText(project.getName());
        this.author.setText("Author: " + (project.getAuthor() == null ? "" : project.getAuthor()));
        this.folder.setText("Folder: " + project.getFolder());
        this.model.setRowCount(0);
        this.latestfiles.clear();
        for (Map.Entry<String, File> entry : ElhVersions.latestVersions(project.getFolder()).entrySet())
        {
            File latest = entry.getValue();
            this.model.addRow(new Object[]{entry.getKey(), latest.getName(),
                    ElhVersions.countVersions(project.getFolder(), entry.getKey())});
            this.latestfiles.add(latest);
        }
        showEvents();
    }

    // one row per project event, one column per ELH. A cell is ticked if the
    // ELH uses the event, or shows the ELH's own wording where that differs
    // from the event's name.
    private void showEvents()
    {
        List<String> elhnames = new ArrayList<>();
        List<Map<String, List<String>>> uses = new ArrayList<>();
        for (ProjectElh elh : this.projectelhs.get())
        {
            Map<String, List<String>> found = new HashMap<>();
            elh.top().collectEventrefs(found);
            elhnames.add(elh.name());
            uses.add(found);
        }
        List<Object> columns = new ArrayList<>(List.of("Event", "Used in"));
        columns.addAll(elhnames);
        this.eventmodel.setColumnIdentifiers(columns.toArray());
        this.eventmodel.setRowCount(0);
        this.rowevents.clear();
        for (ProjectEvent event : this.project.getEvents())
        {
            List<Object> row = new ArrayList<>();
            row.add(event.getName());
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
            row.set(1, usedin);
            this.eventmodel.addRow(row.toArray());
            this.rowevents.add(event);
        }
        this.eventtable.getColumnModel().getColumn(0).setPreferredWidth(160);
        for (int c = 1; c < this.eventtable.getColumnCount(); c++)
        {
            this.eventtable.getColumnModel().getColumn(c).setPreferredWidth(c == 1 ? 60 : 110);
        }
    }

    private ProjectEvent selectedEvent()
    {
        int row = this.eventtable.getSelectedRow();
        return row < 0 ? null : this.rowevents.get(row);
    }

    private void addEvent()
    {
        if (EventActions.addEvent(this, this.project) != null) showEvents();
    }

    private void renameEvent()
    {
        ProjectEvent event = selectedEvent();
        if (event != null && EventActions.renameEvent(this, this.project, event)) showEvents();
    }

    private void editAction()
    {
        ProjectEvent event = selectedEvent();
        if (event != null && EventActions.editAction(this, this.project, event)) showEvents();
    }

    private void deleteEvent()
    {
        int row = this.eventtable.getSelectedRow();
        if (row < 0) return;
        int usedin = (Integer) this.eventmodel.getValueAt(row, 1);
        if (EventActions.deleteEvent(this, this.project, this.rowevents.get(row), usedin)) showEvents();
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
