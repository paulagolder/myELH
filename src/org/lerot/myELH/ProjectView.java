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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// The project screen: the project's details and the ELHs in its folder.
// Each button acts on the latest version file of the selected ELH.
public class ProjectView extends JPanel
{
    private static final long serialVersionUID = 1L;
    private final JLabel title = new JLabel();
    private final JLabel author = new JLabel();
    private final JLabel folder = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ELH", "Latest version", "Versions"}, 0)
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    };
    private final JTable table = new JTable(this.model);
    // the latest version file of each row
    private final List<File> latestfiles = new ArrayList<>();

    public ProjectView(Consumer<File> openlatest, Consumer<File> deletelatest, Consumer<File> deleteall)
    {
        setLayout(new BorderLayout());
        setBorder(jswStyle.makeLineBorder());
        JPanel details = new JPanel(new GridLayout(3, 1));
        details.setBackground(Color.decode(jswTheme.SELECTAREA));
        this.title.setFont(this.title.getFont().deriveFont(Font.BOLD, this.title.getFont().getSize2D() * 1.4f));
        details.add(this.title);
        details.add(this.author);
        details.add(this.folder);
        add(details, BorderLayout.NORTH);
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
        add(new JScrollPane(this.table), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton(buttons, "Open latest version", openlatest);
        addButton(buttons, "Delete latest version", deletelatest);
        addButton(buttons, "Delete all versions", deleteall);
        add(buttons, BorderLayout.SOUTH);
    }

    private void addButton(JPanel buttons, String label, Consumer<File> action)
    {
        JButton button = new JButton(label);
        button.addActionListener(e -> actOnSelected(action));
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

    // fills the screen from the project's folder as it now is
    public void show(Project project)
    {
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
    }
}
