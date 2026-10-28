package org.lerot.myELH;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswStyles;
import org.lerot.mywidgets.jswTheme;
import org.lerot.mywidgets.jswVerticalPanel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.prefs.Preferences;

import static org.lerot.myELH.StyleMaps.getPrintStyles;
import static org.lerot.myELH.StyleMaps.getScreenStyles;

// The main window: the tree of the open ELH on the left, and either the
// edit panel for the selected node or the diagram on the right.
public class MyElh extends JFrame
{
    private static final long serialVersionUID = 1L;
    // set in build.gradle.kts and written into the jar's manifest
    private static final String version = Objects.requireNonNullElse(
            MyElh.class.getPackage().getImplementationVersion(), "(development)");
    final StyleMaps stylemaps = new StyleMaps();
    // where the file choosers start
    private final File elhfolder = Path.of(System.getProperty("user.home"), "Documents", "elh").toFile();
    private final JLabel projectname = new JLabel();
    private final JLabel sourcefile = new JLabel();
    private final JLabel activepath = new JLabel();
    // while a project is active every ELH is saved into its folder
    private Project currentproject;
    // remembers the active project between runs (~/.java/.userPrefs on Linux)
    private static final Preferences prefs = Preferences.userNodeForPackage(MyElh.class);
    private static final String LASTPROJECT = "lastproject";
    private final JPanel svgpanel;
    public DefaultMutableTreeNode activetreenode;
    public ElhNode activenode;
    TreeView currenttreeview;
    // the top node of the open ELH
    ElhNode currentelh;
    jswVerticalPanel editpanel;
    private File currentfile;
    // the open ELH's shape when it was last loaded or saved
    private String savedstructure;
    // the ELH screen (tree, edit panel, diagram) or the project screen
    private static final String ELHSCREEN = "elh";
    private static final String PROJECTSCREEN = "project";
    private final CardLayout screens = new CardLayout();
    private final JPanel screenpanel = new JPanel(screens);
    private final ProjectView projectview;
    private final DiagramPanel diagrampanel;
    private LayoutStyle layoutstyle = LayoutStyle.DEFAULT;

    public MyElh(int w, int h)
    {
        super("ELH " + version);
        stylemaps.put("screen", getScreenStyles());
        stylemaps.put("print", getPrintStyles());
        URL iconURL = MyElh.class.getResource("/elh.png");
        if (iconURL != null)
        {
            setIconImage(new ImageIcon(iconURL).getImage());
        }
        // closing the window exits the same way as Project > Exit
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                exitApp();
            }
        });
        setJMenuBar(makeMenuBar());
        setSize(w, h);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        JPanel statusbar = new JPanel();
        statusbar.setLayout(new BoxLayout(statusbar, BoxLayout.X_AXIS));
        statusbar.add(new JLabel("CURRENT PROJECT:"));
        statusbar.add(this.projectname);
        statusbar.add(Box.createHorizontalGlue());
        statusbar.add(new JLabel("ACTIVE FILE:"));
        statusbar.add(this.sourcefile);
        statusbar.add(Box.createHorizontalGlue());
        statusbar.add(new JLabel("ACTIVE ENTITY:"));
        statusbar.add(this.activepath);
        statusbar.setBackground(Color.decode(jswTheme.NAVAREA));
        add(statusbar);
        this.currenttreeview = new TreeView(this);
        this.editpanel = new jswVerticalPanel("editpanel", false, false);
        this.editpanel.setBorder(jswStyle.makeLineBorder());
        this.editpanel.setLayout(new BorderLayout());
        this.svgpanel = new JPanel();
        this.svgpanel.setBorder(jswStyle.makeLineBorder());
        this.svgpanel.setLayout(new BorderLayout());
        this.diagrampanel = new DiagramPanel(this::selectNode);
        JScrollPane diagramscroll = new JScrollPane(this.diagrampanel);
        // small, so the diagram scrolls rather than squeezing the tree
        diagramscroll.setPreferredSize(new Dimension(200, 200));
        this.svgpanel.add(diagramscroll, BorderLayout.CENTER);
        JPanel mainpanels = new JPanel();
        mainpanels.setLayout(new BoxLayout(mainpanels, BoxLayout.X_AXIS));
        mainpanels.add(this.currenttreeview);
        mainpanels.add(this.editpanel);
        mainpanels.add(this.svgpanel);
        this.projectview = new ProjectView(this::openLatest, this::deleteLatestVersion, this::deleteAllVersions);
        this.screenpanel.add(mainpanels, ELHSCREEN);
        this.screenpanel.add(this.projectview, PROJECTSCREEN);
        add(this.screenpanel);
        showEdit();
        reopenLastProject();
    }

    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() ->
        {
            jswTheme.install();
            UIManager.put("FileChooser.readOnly", Boolean.TRUE);
            MyElh frame = new MyElh(800, 400);
            frame.setLocation(50, 50);
            frame.setMinimumSize(new Dimension(800, 400));
            frame.pack();
            frame.setVisible(true);
        });
    }

    private JMenuBar makeMenuBar()
    {
        JMenuBar jmb = new JMenuBar();
        JMenu jmProject = new JMenu("Project");
        addMenuItem(jmProject, "New Project", this::newProject);
        addMenuItem(jmProject, "Open Project", this::openProject);
        addMenuItem(jmProject, "Project View", this::showProjectView);
        addMenuItem(jmProject, "Close Project", this::closeProject);
        jmProject.addSeparator();
        addMenuItem(jmProject, "Exit", this::exitApp);
        jmb.add(jmProject);
        JMenu jmFile = new JMenu("File");
        addMenuItem(jmFile, "Open", this::openElh);
        addMenuItem(jmFile, "New", this::newElh);
        addMenuItem(jmFile, "Save", this::saveElh);
        addMenuItem(jmFile, "Save As", this::saveElhAs);
        addMenuItem(jmFile, "Save As New Version", this::saveNewVersion);
        addMenuItem(jmFile, "Close", this::closeElh);
        jmb.add(jmFile);
        JMenu jmView = new JMenu("View");
        addMenuItem(jmView, "View EDIT", () -> { showElhScreen(); showEdit(); });
        addMenuItem(jmView, "View SVG", () -> { showElhScreen(); showSvg(); });
        jmView.addSeparator();
        addMenuItem(jmView, "Zoom In", KeyEvent.VK_EQUALS, () -> diagrampanel.setZoom(diagrampanel.getZoom() * 1.25));
        addMenuItem(jmView, "Zoom Out", KeyEvent.VK_MINUS, () -> diagrampanel.setZoom(diagrampanel.getZoom() / 1.25));
        addMenuItem(jmView, "Actual Size", KeyEvent.VK_0, () -> diagrampanel.setZoom(1.0));
        jmb.add(jmView);
        JMenu jmExport = new JMenu("Export");
        addMenuItem(jmExport, "Print SVG", this::svgexport);
        jmb.add(jmExport);
        JMenu jmOptions = new JMenu("Options");
        ButtonGroup stylegroup = new ButtonGroup();
        for (LayoutStyle style : new LayoutStyle[]{LayoutStyle.JACKSON, LayoutStyle.SIMPLE, LayoutStyle.DEFAULT})
        {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(style.menuLabel, style == this.layoutstyle);
            item.addActionListener(e -> setLayoutStyle(style));
            stylegroup.add(item);
            jmOptions.add(item);
        }
        jmb.add(jmOptions);
        JMenu jmHelp = new JMenu("Help");
        addMenuItem(jmHelp, "About", () -> JOptionPane.showMessageDialog(this,
                "myELH " + version + "\nEntity life history editor", "About myELH",
                JOptionPane.INFORMATION_MESSAGE));
        jmb.add(jmHelp);
        return jmb;
    }

    private static void addMenuItem(JMenu menu, String label, Runnable action)
    {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    // a menu item that can also be chosen with Ctrl + key
    private static void addMenuItem(JMenu menu, String label, int key, Runnable action)
    {
        JMenuItem item = new JMenuItem(label);
        item.setAccelerator(KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    public static jswStyles tablestyles()
    {
        jswStyles tablestyles = new jswStyles();
        tablestyles.name = "defaulttable";
        jswStyle tablestyle = tablestyles.makeStyle("table");
        tablestyle.putAttribute("borderwidth", 1);
        tablestyle.putAttribute("bordercolor", jswTheme.BORDER);
        jswStyle rowstyle0 = tablestyles.makeStyle("row_0");
        rowstyle0.putAttribute("fontsize", jswTheme.HEADING);
        rowstyle0.putAttribute("fontStyle", Font.BOLD + Font.ITALIC);
        rowstyle0.putAttribute("foregroundColor", "#0e56f2");
        jswStyle colstyle0 = tablestyles.makeStyle("col_0");
        colstyle0.putAttribute("horizontalAlignment", "RIGHT");
        colstyle0.putAttribute("minwidth", 30);
        colstyle0.putAttribute("fontsize", jswTheme.HEADING);
        colstyle0.putAttribute("foregroundcolor", "gray");
        jswStyle colstyle = tablestyles.makeStyle("col");
        colstyle.putAttribute("horizontalAlignment", "RIGHT");
        colstyle.putAttribute("width", 30);
        jswStyle cellstyle = tablestyles.makeStyle("cell");
        cellstyle.putAttribute("foregroundcolor", "black");
        cellstyle.putAttribute("borderWidth", "1");
        cellstyle.putAttribute("borderColor", jswTheme.CELLBORDER);
        cellstyle.putAttribute("fontsize", jswTheme.HEADING);
        return tablestyles;
    }

    public static jswStyles tablestyles2()
    {
        jswStyles tablestyles = new jswStyles();
        tablestyles.name = "defaulttable";
        jswStyle tablestyle = tablestyles.makeStyle("table");
        tablestyle.putAttribute("borderwidth", 1);
        tablestyle.putAttribute("bordercolor", jswTheme.BORDER);
        jswStyle rowstyle0 = tablestyles.makeStyle("row_0");
        rowstyle0.putAttribute("fontsize", jswTheme.HEADING);
        jswStyle colstyle0 = tablestyles.makeStyle("col_0");
        colstyle0.putAttribute("horizontalAlignment", "RIGHT");
        colstyle0.putAttribute("minwidth", 30);
        colstyle0.putAttribute("fontsize", jswTheme.HEADING);
        colstyle0.putAttribute("foregroundcolor", "gray");
        jswStyle colstyle = tablestyles.makeStyle("col");
        colstyle.putAttribute("horizontalAlignment", "RIGHT");
        colstyle.putAttribute("width", 30);
        jswStyle cellstyle = tablestyles.makeStyle("cell");
        cellstyle.putAttribute("foregroundcolor", "black");
        cellstyle.putAttribute("borderWidth", "1");
        cellstyle.putAttribute("borderColor", jswTheme.CELLBORDER);
        cellstyle.putAttribute("fontsize", jswTheme.HEADING);
        return tablestyles;
    }

    private void newProject()
    {
        JTextField namefield = new JTextField(20);
        JTextField authorfield = new JTextField(20);
        JPanel form = new JPanel(new GridLayout(2, 2, 4, 4));
        form.add(new JLabel("Project name:"));
        form.add(namefield);
        form.add(new JLabel("Author:"));
        form.add(authorfield);
        if (JOptionPane.showConfirmDialog(this, form, "New Project", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION)
        {
            return;
        }
        String name = namefield.getText().trim();
        if (name.isEmpty() || !closeProject())
        {
            return;
        }
        try
        {
            setProject(Project.create(this.elhfolder, name, authorfield.getText().trim()));
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not create project " + name + "\n" + e.getMessage(),
                    "New Project failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openProject()
    {
        java.util.List<File> folders = Project.findProjects(this.elhfolder);
        if (folders.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "There are no projects in " + this.elhfolder,
                    "Open Project", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] names = folders.stream().map(File::getName).toArray(String[]::new);
        Object chosen = JOptionPane.showInputDialog(this, "Project:", "Open Project",
                JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
        if (chosen == null || !closeProject())
        {
            return;
        }
        File folder = new File(this.elhfolder, (String) chosen);
        try
        {
            setProject(Project.load(folder));
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not open project " + folder + "\n" + e.getMessage(),
                    "Open Project failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // reopens the project that was active when the app last closed, if it is still there
    private void reopenLastProject()
    {
        String lastfolder = prefs.get(LASTPROJECT, null);
        if (lastfolder != null && new File(lastfolder, Project.PROJECTFILE).isFile())
        {
            try
            {
                setProject(Project.load(new File(lastfolder)));
            } catch (IOException e)
            {
                // a damaged project.xml: start without a project
            }
        }
    }

    // an open ELH with no project moves into the new one: from now on it is
    // saved into its folder. A project opens on its project view.
    private void setProject(Project project)
    {
        this.currentproject = project;
        this.projectname.setText(project == null ? "" : project.getName());
        if (project == null)
        {
            prefs.remove(LASTPROJECT);
            showElhScreen();
        } else
        {
            prefs.put(LASTPROJECT, project.getFolder().getAbsolutePath());
        }
        if (project != null && this.currentfile != null)
        {
            setCurrentFile(this.currentfile);
        }
        if (project != null)
        {
            showProjectView();
        }
    }

    // saves and closes the open ELH, then closes the project; false if the
    // ELH could not be saved, leaving everything open
    private boolean closeProject()
    {
        if (this.currentproject == null)
        {
            return true;
        }
        if (this.currentelh != null)
        {
            if (!saveElh())
            {
                return false;
            }
            closeElh();
        }
        setProject(null);
        return true;
    }

    // in a project the open ELH is saved first; elsewhere it is left unsaved, as before
    private void exitApp()
    {
        if (this.currentproject != null && this.currentelh != null && !saveElh()
                && JOptionPane.showConfirmDialog(this, "Exit without saving?", "Exit",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
        {
            return;
        }
        System.exit(0);
    }

    private void showProjectView()
    {
        if (this.currentproject == null)
        {
            JOptionPane.showMessageDialog(this, "No project is open", "Project View",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        this.projectview.show(this.currentproject);
        this.screens.show(this.screenpanel, PROJECTSCREEN);
    }

    private void showElhScreen()
    {
        this.screens.show(this.screenpanel, ELHSCREEN);
    }

    // chosen in the project view: the open ELH is saved before the other is opened
    private void openLatest(File latest)
    {
        if (this.currentelh != null && !saveElh())
        {
            return;
        }
        if (loadElh(latest))
        {
            showElhScreen();
        }
    }

    // chosen in the project view; if that version is open it is closed unsaved
    private void deleteLatestVersion(File latest)
    {
        if (JOptionPane.showConfirmDialog(this, "Delete " + latest.getName() + "?", "Delete latest version",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
        {
            return;
        }
        if (this.currentfile != null && this.currentfile.getAbsoluteFile().equals(latest.getAbsoluteFile()))
        {
            closeElh();
        }
        deleteFiles(java.util.List.of(latest));
    }

    // chosen in the project view; if that ELH is open it is closed unsaved
    private void deleteAllVersions(File latest)
    {
        String basename = ElhVersions.baseName(latest);
        java.util.List<File> versions = ElhVersions.allVersions(this.currentproject.getFolder(), basename);
        if (JOptionPane.showConfirmDialog(this, "Delete all " + versions.size() + " versions of " + basename + "?",
                "Delete all versions", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
                != JOptionPane.OK_OPTION)
        {
            return;
        }
        if (this.currentfile != null && ElhVersions.baseName(this.currentfile).equalsIgnoreCase(basename))
        {
            closeElh();
        }
        deleteFiles(versions);
    }

    private void deleteFiles(java.util.List<File> files)
    {
        for (File file : files)
        {
            try
            {
                Files.delete(file.toPath());
            } catch (IOException e)
            {
                JOptionPane.showMessageDialog(this, "Could not delete " + file + "\n" + e.getMessage(),
                        "Delete failed", JOptionPane.ERROR_MESSAGE);
            }
        }
        this.projectview.show(this.currentproject);
    }

    // where the ELH choosers start
    private File startFolder()
    {
        return this.currentproject == null ? this.elhfolder : this.currentproject.getFolder();
    }

    // the file the open ELH is saved to. In a project that is one of the
    // project's versioned files: a file from anywhere else, or not yet
    // versioned, becomes the next version of its name in the project folder.
    private void setCurrentFile(File file)
    {
        if (this.currentproject == null)
        {
            this.currentfile = file;
        } else
        {
            File folder = this.currentproject.getFolder();
            boolean inproject = folder.getAbsoluteFile().equals(file.getAbsoluteFile().getParentFile());
            this.currentfile = inproject && ElhVersions.isVersioned(file) ? file
                    : ElhVersions.nextVersion(folder, ElhVersions.baseName(file));
        }
        this.sourcefile.setText(this.currentfile.getName());
    }

    private JFileChooser elhChooser(String title)
    {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(startFolder());
        fc.setDialogTitle(title);
        fc.setFileFilter(new FileNameExtensionFilter("ELH", "xml", "elh"));
        return fc;
    }

    private void openElh()
    {
        JFileChooser fc = elhChooser("Open ELH file");
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        loadElh(fc.getSelectedFile());
    }

    private boolean loadElh(File openfile)
    {
        if (openfile.getName().equals(Project.PROJECTFILE))
        {
            JOptionPane.showMessageDialog(this, openfile + " is a project file, not an ELH",
                    "Open failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        ElhNode openelh;
        try
        {
            openelh = ElhFile.read(openfile.toPath());
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not open " + openfile + "\n" + e.getMessage(),
                    "Open failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        setCurrentFile(openfile);
        this.currentelh = openelh;
        this.savedstructure = openelh.structure();
        this.currenttreeview.maketree(this.currentelh);
        this.activenode = null;
        this.activetreenode = null;
        this.currenttreeview.repaint();
        showDiagram();
        getContentPane().validate();
        return true;
    }

    private void newElh()
    {
        JFileChooser fc = elhChooser("Specify a new ELH file");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        setCurrentFile(fc.getSelectedFile());
        this.currentelh = new ElhNode(NodeType.ELH, ElhVersions.baseName(this.currentfile), null);
        writeElh(false);
        this.currenttreeview.maketree(this.currentelh);
    }

    // a new version if nodes were added, removed or moved since the last
    // save; otherwise the current version is overwritten
    private boolean saveElh()
    {
        if (this.currentfile == null)
        {
            return saveElhAs();
        }
        return writeElh(true);
    }

    private boolean saveElhAs()
    {
        if (this.currentelh == null)
        {
            return false;
        }
        JFileChooser fc = elhChooser("Save to new ELH file");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return false;
        }
        setCurrentFile(fc.getSelectedFile());
        return writeElh(false);
    }

    private void saveNewVersion()
    {
        if (this.currentelh == null)
        {
            return;
        }
        if (this.currentfile == null)
        {
            saveElhAs();
            return;
        }
        this.currentfile = ElhVersions.nextVersion(this.currentfile.getAbsoluteFile().getParentFile(),
                ElhVersions.baseName(this.currentfile));
        writeElh(false);
    }

    private boolean writeElh(boolean newversionifreshaped)
    {
        if (this.currentelh == null)
        {
            return false;
        }
        String structure = this.currentelh.structure();
        if (newversionifreshaped && this.currentfile.exists() && !structure.equals(this.savedstructure))
        {
            this.currentfile = ElhVersions.nextVersion(this.currentfile.getAbsoluteFile().getParentFile(),
                    ElhVersions.baseName(this.currentfile));
        }
        try
        {
            ElhFile.write(this.currentelh, this.currentfile.toPath());
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not save " + this.currentfile + "\n" + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        this.savedstructure = structure;
        this.sourcefile.setText(this.currentfile.getName());
        if (this.currentproject != null)
        {
            this.projectview.show(this.currentproject);
        }
        return true;
    }

    private void closeElh()
    {
        this.currentelh = null;
        this.currentfile = null;
        this.savedstructure = null;
        this.activenode = null;
        this.activetreenode = null;
        this.sourcefile.setText("");
        this.activepath.setText("");
        this.currenttreeview.cleartree();
        refresh();
    }

    private void showEdit()
    {
        this.editpanel.setVisible(true);
        this.svgpanel.setVisible(false);
    }

    private void showSvg()
    {
        this.editpanel.setVisible(false);
        this.svgpanel.setVisible(true);
    }

    private void setLayoutStyle(LayoutStyle style)
    {
        this.layoutstyle = style;
        refresh();
    }

    public void refresh()
    {
        selectActiveTreenode();
        showEditView();
        showDiagram();
        repaint();
        getContentPane().validate();
        selectActiveTreenode();
    }

    private void selectActiveTreenode()
    {
        if (this.activetreenode != null)
        {
            TreePath treepath = new TreePath(this.activetreenode.getPath());
            this.currenttreeview.thistree.setSelectionPath(treepath);
            this.currenttreeview.thistree.scrollPathToVisible(treepath);
        }
    }

    public void showEditView()
    {
        this.editpanel.removeAll();
        this.activepath.setText(this.activenode == null ? "" : this.activenode.getPath());
        if (this.activenode == null)
        {
            return;
        }
        EditView panel = new EditView(this);
        panel.setup(this.activenode);
        this.editpanel.add(panel, "Center");
        repaint();
        getContentPane().validate();
    }

    // the name for a node about to be added, or null if the user cancels
    public String askNodeName(String type)
    {
        String name = (String) JOptionPane.showInputDialog(this, "Name of the new " + type + ":",
                "New " + type, JOptionPane.PLAIN_MESSAGE, null, null, "");
        return name == null || name.isBlank() ? null : name.trim();
    }

    // redraws the diagram of the whole ELH, outlining the active node
    private void showDiagram()
    {
        ElhDiagram diagram = this.currentelh == null ? null
                : new ElhDiagram(this.currentelh, stylemaps.getStyleMap("screen"), layoutstyle);
        this.diagrampanel.setDiagram(diagram, this.activenode);
    }

    // a box clicked in the diagram: select its node in the tree
    private void selectNode(ElhNode anode)
    {
        this.currenttreeview.setselections(anode);
    }

    private void svgexport()
    {
        if (this.currentelh == null)
        {
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save SVG file");
        fc.setCurrentDirectory(startFolder());
        fc.setFileFilter(new FileNameExtensionFilter("svg", "svg"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        try
        {
            // exported with the screen styles, as it always has been; the
            // "print" style map is kept but not used
            SvgExport.write(this.currentelh, stylemaps.getStyleMap("screen"), layoutstyle, fc.getSelectedFile().toPath());
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not save " + fc.getSelectedFile() + "\n" + e.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // after the tree selection changes
    public void updateview()
    {
        if (this.currentelh != null)
        {
            this.currenttreeview.expandPath(this.activetreenode);
            showEditView();
            showDiagram();
        }
        repaint();
        getContentPane().validate();
    }

    public void maketree()
    {
        this.currenttreeview.maketree(this.currentelh);
    }
}
