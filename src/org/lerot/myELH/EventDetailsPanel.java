package org.lerot.myELH;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswTheme;

import javax.swing.*;
import java.awt.*;

// What the entities tab shows for an event selected in the project tree:
// its name, its action and where it is kept. The Events tab has the rest
// (its participants and their conditions and results).
public class EventDetailsPanel extends JPanel
{
    private static final long serialVersionUID = 1L;
    private final JTextField namefield = new JTextField(20);
    private final JTextArea actionfield = new JTextArea(4, 30);
    private final JLabel keptin = new JLabel();

    public EventDetailsPanel(MyElh app, Project project, ProjectEvent event)
    {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createCompoundBorder(jswStyle.makeLineBorder(),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        setBackground(Color.decode(jswTheme.EDITAREA));
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("EVENT:"), c);
        c.gridy = 1;
        form.add(new JLabel("ACTION:"), c);
        c.gridy = 2;
        form.add(new JLabel("KEPT IN:"), c);
        c.gridx = 1;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        this.namefield.setText(event.getName());
        form.add(this.namefield, c);
        c.gridy = 1;
        c.fill = GridBagConstraints.BOTH;
        this.actionfield.setText(event.getAction());
        this.actionfield.setLineWrap(true);
        this.actionfield.setWrapStyleWord(true);
        form.add(new JScrollPane(this.actionfield), c);
        c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        showKeptIn(project, event);
        form.add(this.keptin, c);
        add(form, BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.setOpaque(false);
        JButton save = new JButton("Save Edits");
        save.addActionListener(e ->
        {
            String name = EventActions.checkName(this, project, this.namefield.getText(), event);
            if (name == null)
            {
                this.namefield.setText(event.getName());
                return;
            }
            event.setName(name);
            event.setAction(this.actionfield.getText().trim());
            EventActions.saveEvent(this, project, event);
            app.maketree();
            app.currenttreeview.select(event);
        });
        buttons.add(save);
        JButton show = new JButton("Show in Events Tab");
        show.addActionListener(e -> app.showEventInTab(event));
        buttons.add(show);
        add(buttons, BorderLayout.SOUTH);
    }

    private void showKeptIn(Project project, ProjectEvent event)
    {
        if (event.isInline())
        {
            this.keptin.setText(Project.PROJECTFILE);
        } else
        {
            java.util.List<java.io.File> versions = project.eventVersions(event);
            this.keptin.setText(versions.isEmpty() ? event.getFile() + " (no file)" : versions.getLast().getName());
        }
    }
}
