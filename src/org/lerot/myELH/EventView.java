package org.lerot.myELH;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// The events screen: the project's events on the left and the selected
// event's edit panel beside them. The participants are not stored with the event but found in
// the project's ELHs: every node linked to it.
public class EventView extends JPanel
{
    private static final long serialVersionUID = 1L;
    private final MyElh app;
    private final DefaultListModel<ProjectEvent> listmodel = new DefaultListModel<>();
    private final JList<ProjectEvent> eventlist = new JList<>(this.listmodel);
    private final JLabel filterlabel = new JLabel();
    private final JButton showallbutton = new JButton("Show all");
    // the ids of the events listed, or null to list them all
    private Set<String> filter;
    private final JTextField namefield = new JTextField(20);
    private final JTextArea descriptionfield = new JTextArea(4, 30);
    private final DefaultTableModel participantmodel = new DefaultTableModel(
            new Object[]{"ELH", "Wording", "Where"}, 0)
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    };
    private final JTable participanttable = new JTable(this.participantmodel);
    private final List<Participant> participants = new ArrayList<>();
    private final JPanel editpanel = new JPanel(new BorderLayout(4, 4));

    // a node linked to the event, in one of the project's ELHs
    private record Participant(ProjectElh elh, ElhNode node)
    {
    }

    public EventView(MyElh app)
    {
        this.app = app;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(makeListPanel());
        add(makeEditPanel());
    }

    private JPanel makeListPanel()
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(jswStyle.makeLineBorder());
        panel.setBackground(Color.decode(jswTheme.SELECTAREA));
        JPanel filterbar = new JPanel(new BorderLayout());
        filterbar.setOpaque(false);
        filterbar.add(this.filterlabel, BorderLayout.CENTER);
        this.showallbutton.addActionListener(e -> showAll());
        filterbar.add(this.showallbutton, BorderLayout.EAST);
        panel.add(filterbar, BorderLayout.NORTH);
        this.eventlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.eventlist.setBackground(Color.decode(jswTheme.SELECTAREA));
        this.eventlist.addListSelectionListener(e ->
        {
            if (!e.getValueIsAdjusting()) showSelected();
        });
        JScrollPane scroll = new JScrollPane(this.eventlist);
        scroll.setPreferredSize(new Dimension(180, 200));
        panel.add(scroll, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(260, Integer.MAX_VALUE));
        return panel;
    }

    private JPanel makeEditPanel()
    {
        this.editpanel.setBorder(BorderFactory.createCompoundBorder(jswStyle.makeLineBorder(),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        this.editpanel.setBackground(Color.decode(jswTheme.EDITAREA));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("NAME:"), c);
        c.gridy = 1;
        form.add(new JLabel("DESCRIPTION:"), c);
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
        this.editpanel.add(form, BorderLayout.NORTH);
        JPanel participantpanel = new JPanel(new BorderLayout());
        participantpanel.setOpaque(false);
        participantpanel.add(new JLabel("PARTICIPANTS (the ELH nodes linked to this event):"), BorderLayout.NORTH);
        this.participanttable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.participanttable.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2) goToNode();
            }
        });
        JScrollPane scroll = new JScrollPane(this.participanttable);
        scroll.setPreferredSize(new Dimension(300, 120));
        participantpanel.add(scroll, BorderLayout.CENTER);
        this.editpanel.add(participantpanel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        JButton savebutton = new JButton("Save Edits");
        savebutton.addActionListener(e -> saveEdits());
        buttons.add(savebutton);
        JButton gotobutton = new JButton("Go to node");
        gotobutton.addActionListener(e -> goToNode());
        buttons.add(gotobutton);
        this.editpanel.add(buttons, BorderLayout.SOUTH);
        return this.editpanel;
    }

    public ProjectEvent selectedEvent()
    {
        return this.eventlist.getSelectedValue();
    }

    // relists the project's events, keeping the filter and the selection
    public void refresh()
    {
        Project project = this.app.getProject();
        ProjectEvent selected = selectedEvent();
        String selectedid = selected == null ? null : selected.getId();
        this.listmodel.clear();
        if (project != null)
        {
            for (ProjectEvent event : project.getEvents())
            {
                if (this.filter == null || this.filter.contains(event.getId()))
                {
                    this.listmodel.addElement(event);
                }
            }
        }
        this.showallbutton.setVisible(this.filter != null);
        if (this.filter == null)
        {
            this.filterlabel.setText(" All events");
        }
        select(selectedid);
        if (selectedEvent() == null && !this.listmodel.isEmpty())
        {
            this.eventlist.setSelectedIndex(0);
        }
        showSelected();
    }

    private void select(String id)
    {
        for (int i = 0; i < this.listmodel.size(); i++)
        {
            if (this.listmodel.get(i).getId().equals(id))
            {
                this.eventlist.setSelectedIndex(i);
                this.eventlist.ensureIndexIsVisible(i);
                return;
            }
        }
        this.eventlist.clearSelection();
    }

    // linked browsing from the entities screen: a linked leaf event shows its
    // event; any other node narrows the list to the events in its part of the
    // life history; no node lists every event
    public void focusOn(ElhNode node)
    {
        Project project = this.app.getProject();
        String selectid = null;
        if (node == null || project == null)
        {
            this.filter = null;
        } else if (node.getEventref() != null && project.findEvent(node.getEventref()) != null)
        {
            this.filter = null;
            selectid = node.getEventref();
        } else
        {
            Map<String, List<String>> found = new HashMap<>();
            node.collectEventrefs(found);
            this.filter = found.keySet();
            this.filterlabel.setText(" Events in " + node.getName());
        }
        refresh();
        if (selectid != null)
        {
            select(selectid);
        }
    }

    public void showAll()
    {
        this.filter = null;
        refresh();
    }

    private void showSelected()
    {
        ProjectEvent event = selectedEvent();
        this.participantmodel.setRowCount(0);
        this.participants.clear();
        this.namefield.setText(event == null ? "" : event.getName());
        this.descriptionfield.setText(event == null ? "" : event.getDescription());
        this.namefield.setEnabled(event != null);
        this.descriptionfield.setEnabled(event != null);
        this.app.eventSelected(event);
        if (event == null)
        {
            return;
        }
        for (ProjectElh elh : this.app.projectElhs())
        {
            List<ElhNode> linked = new ArrayList<>();
            elh.top().findLinked(event.getId(), linked);
            for (ElhNode node : linked)
            {
                this.participants.add(new Participant(elh, node));
                this.participantmodel.addRow(new Object[]{elh.name(), node.getName(), node.getPath()});
            }
        }
    }

    private void saveEdits()
    {
        ProjectEvent event = selectedEvent();
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
        EventActions.save(this, project);
        this.eventlist.repaint();
        showSelected();
    }

    private void goToNode()
    {
        int row = this.participanttable.getSelectedRow();
        if (row < 0 && this.participants.size() == 1)
        {
            row = 0;
        }
        if (row >= 0)
        {
            Participant participant = this.participants.get(row);
            this.app.goToNode(participant.elh(), participant.node());
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
            this.filter = null;
            refresh();
            select(event.getId());
        }
    }

    public void deleteEvent()
    {
        ProjectEvent event = selectedEvent();
        Project project = this.app.getProject();
        if (event == null || project == null) return;
        long usedin = this.participants.stream().map(p -> p.elh().name()).distinct().count();
        if (EventActions.deleteEvent(this, project, event, (int) usedin))
        {
            refresh();
        }
    }
}
