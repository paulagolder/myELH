package org.lerot.myELH;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

// Changes to a project's event catalogue, asked for in dialogs and saved
// straight away; shared by the project view and the events screen.
final class EventActions
{
    private EventActions()
    {
    }

    // a new or changed event name, or null if cancelled, blank or taken by another event
    static String askEventName(Component parent, Project project, String prompt, String current,
            ProjectEvent changing)
    {
        String name = (String) JOptionPane.showInputDialog(parent, prompt, "Event",
                JOptionPane.PLAIN_MESSAGE, null, null, current);
        return name == null ? null : checkName(parent, project, name, changing);
    }

    // the trimmed name, or null if it is blank or another event already has it
    static String checkName(Component parent, Project project, String name, ProjectEvent changing)
    {
        if (name.isBlank())
        {
            return null;
        }
        name = name.trim();
        ProjectEvent existing = project.findEventByName(name);
        if (existing != null && existing != changing)
        {
            JOptionPane.showMessageDialog(parent, "The project already has an event called " + existing.getName(),
                    "Event", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return name;
    }

    // the new event, or null if cancelled
    static ProjectEvent addEvent(Component parent, Project project)
    {
        String name = askEventName(parent, project, "Name of the new event:", "", null);
        if (name == null)
        {
            return null;
        }
        ProjectEvent event = project.addEvent(name);
        save(parent, project);
        return event;
    }

    // only the catalogue name changes; each ELH keeps its own wording
    static boolean renameEvent(Component parent, Project project, ProjectEvent event)
    {
        String name = askEventName(parent, project, "New name for " + event.getName() + ":", event.getName(), event);
        if (name == null)
        {
            return false;
        }
        event.setName(name);
        save(parent, project);
        return true;
    }

    static boolean editAction(Component parent, Project project, ProjectEvent event)
    {
        JTextArea text = new JTextArea(event.getAction(), 6, 40);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        if (JOptionPane.showConfirmDialog(parent, new JScrollPane(text), "Action of " + event.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
        {
            return false;
        }
        event.setAction(text.getText().trim());
        save(parent, project);
        return true;
    }

    // ELH nodes linked to a deleted event keep its id, which then links to nothing
    static boolean deleteEvent(Component parent, Project project, ProjectEvent event, int usedin)
    {
        String message = "Delete event " + event.getName() + "?"
                + (usedin == 0 ? "" : "\nIt is used in " + usedin + " ELH" + (usedin == 1 ? "" : "s")
                + "; their nodes will no longer be linked to a project event.");
        if (JOptionPane.showConfirmDialog(parent, message, "Delete event", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
        {
            return false;
        }
        project.removeEvent(event);
        save(parent, project);
        return true;
    }

    static void save(Component parent, Project project)
    {
        try
        {
            project.save();
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(parent, "Could not save the project\n" + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
