package org.lerot.myELH;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswTheme;
import org.lerot.mywidgets.jswVerticalPanel;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

import org.jdom2.Element;

import static org.lerot.myELH.StyleMaps.getPrintStyles;
import static org.lerot.myELH.StyleMaps.getScreenStyles;

// The main window. A project manages entities and events; the Entities tab
// shows them in the project tree on the left, and on the right either the
// edit panel for what is selected or the diagram of the selected entity's
// life history. An entity's or event's file is only read when it is selected.
public class MyElh extends JFrame
{
    private static final long serialVersionUID = 1L;
    // set in build.gradle.kts and written into the jar's manifest
    private static final String version = Objects.requireNonNullElse(
            MyElh.class.getPackage().getImplementationVersion(), "(development)");
    final StyleMaps stylemaps = new StyleMaps();
    // where projects are kept
    private final File elhfolder = Path.of(System.getProperty("user.home"), "Documents", "elh").toFile();
    private final JLabel projectname = new JLabel();
    private final JLabel sourcefile = new JLabel();
    private final JLabel activepath = new JLabel();
    // "Entity" or "Event"
    private final JLabel activelabel = new JLabel();
    private Project currentproject;
    // remembers the active project between runs (~/.java/.userPrefs on Linux)
    private static final Preferences prefs = Preferences.userNodeForPackage(MyElh.class);
    private static final String LASTPROJECT = "lastproject";
    private final JPanel svgpanel;
    // the entity whose life history is being edited and drawn
    ProjectEntity activeentity;
    // the selected node of that life history, or null
    public ElhNode activenode;
    TreeView currenttreeview;
    jswVerticalPanel editpanel;
    // the two dimensions of the model, entities and events, each with its own
    // tab and menus; the project tab is the way into both
    private static final int PROJECTTAB = 0;
    private static final int ENTITYTAB = 1;
    private static final int EVENTTAB = 2;
    private final JTabbedPane tabs = new JTabbedPane();
    private int currenttab = ENTITYTAB;
    private final ProjectView projectview;
    private final EventView eventview;
    private final DiagramPanel diagrampanel;
    private LayoutStyle layoutstyle = LayoutStyle.DEFAULT;
    // whether the diagram shows each action's state indicator values
    private boolean showstates = true;

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
        setSize(w, h);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        JPanel statusbar = new JPanel();
        statusbar.setLayout(new BoxLayout(statusbar, BoxLayout.X_AXIS));
        // quiet captions, each followed by its value in bold
        statusbar.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        statusbar.add(caption(new JLabel("Project")));
        statusbar.add(value(this.projectname));
        statusbar.add(Box.createHorizontalGlue());
        statusbar.add(caption(this.activelabel));
        statusbar.add(value(this.activepath));
        statusbar.add(Box.createHorizontalGlue());
        statusbar.add(caption(new JLabel("Kept in")));
        statusbar.add(value(this.sourcefile));
        statusbar.setBackground(Color.decode(jswTheme.NAVAREA));
        add(statusbar);
        this.currenttreeview = new TreeView(this, TreeView.Mode.ENTITIES, new TreeView.Listener()
        {
            public void selected(Object shown)
            {
                entityTreeSelected(shown);
            }

            public void popup(Object shown, JPopupMenu menu)
            {
                entityTreePopup(shown, menu);
            }
        });
        this.editpanel = new jswVerticalPanel("editpanel", false, false);
        this.editpanel.setBorder(jswStyle.makeLineBorder());
        this.editpanel.setLayout(new BorderLayout());
        this.svgpanel = new JPanel();
        this.svgpanel.setBorder(jswStyle.makeLineBorder());
        this.svgpanel.setLayout(new BorderLayout());
        this.diagrampanel = new DiagramPanel(this::selectNode, this::openNode);
        JScrollPane diagramscroll = new JScrollPane(this.diagrampanel);
        // small, so the diagram scrolls rather than squeezing the tree
        diagramscroll.setPreferredSize(new Dimension(200, 200));
        this.svgpanel.add(diagramscroll, BorderLayout.CENTER);
        JPanel mainpanels = new JPanel();
        mainpanels.setLayout(new BoxLayout(mainpanels, BoxLayout.X_AXIS));
        mainpanels.add(this.currenttreeview);
        mainpanels.add(this.editpanel);
        mainpanels.add(this.svgpanel);
        this.projectview = new ProjectView(this);
        this.eventview = new EventView(this);
        this.tabs.addTab("Project", this.projectview);
        this.tabs.addTab("Entities", mainpanels);
        this.tabs.addTab("Events", this.eventview);
        this.tabs.setSelectedIndex(ENTITYTAB);
        this.tabs.addChangeListener(e -> tabChanged());
        add(this.tabs);
        showEdit();
        enableProjectTabs();
        reopenLastProject();
        setJMenuBar(makeMenuBar(this.currenttab));
        showStatus();
    }

    private static JLabel caption(JLabel label)
    {
        label.setForeground(Color.DARK_GRAY);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        return label;
    }

    private static JLabel value(JLabel label)
    {
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    // linked browsing: the tab switched to follows what was active in the other
    private void tabChanged()
    {
        int previoustab = this.currenttab;
        this.currenttab = this.tabs.getSelectedIndex();
        if (this.currenttab == PROJECTTAB && this.currentproject != null)
        {
            this.projectview.show(this.currentproject);
        } else if (this.currenttab == EVENTTAB)
        {
            // the active action, under its event or among those not yet in one
            if (previoustab == ENTITYTAB && this.activeentity != null && this.activenode != null
                    && this.activenode.isLeafAction())
            {
                this.eventview.selectAction(new EntityAction(this.activeentity, this.activenode));
            } else
            {
                this.eventview.refresh();
            }
        } else if (this.currenttab == ENTITYTAB && previoustab == EVENTTAB)
        {
            selectLinkedNode(this.eventview.selectedEvent());
        }
        setJMenuBar(makeMenuBar(this.currenttab));
        showStatus();
        getRootPane().revalidate();
    }

    // selects a node linked to the event, in the active entity if it has
    // one, unless the active node already is one
    private void selectLinkedNode(ProjectEvent event)
    {
        if (event == null || this.activenode != null && event.getId().equals(this.activenode.getEventref()))
        {
            return;
        }
        List<ProjectEntity> lookin = new ArrayList<>();
        if (this.activeentity != null)
        {
            lookin.add(this.activeentity);
        }
        if (this.currentproject != null)
        {
            this.currentproject.getEntities().stream()
                    .filter(e -> e.isLoaded() && e != this.activeentity).forEach(lookin::add);
        }
        for (ProjectEntity entity : lookin)
        {
            List<ElhNode> linked = new ArrayList<>();
            entity.getTop().findLinked(event.getId(), linked);
            if (!linked.isEmpty())
            {
                showNode(entity, linked.getFirst());
                return;
            }
        }
    }

    private void showStatus()
    {
        if (this.currenttab == EVENTTAB)
        {
            ProjectEvent event = this.eventview.selectedEvent();
            this.activelabel.setText("Event");
            this.activepath.setText(event == null ? "" : event.getName());
            this.sourcefile.setText(event == null ? "" : keptIn(event));
        } else
        {
            this.activelabel.setText("Entity");
            this.activepath.setText(this.activenode == null ? "" : this.activenode.getPath());
            this.sourcefile.setText(this.activeentity == null ? ""
                    : this.activeentity.isInline() ? Project.PROJECTFILE : this.activeentity.describeStorage());
        }
    }

    private String keptIn(ProjectEvent event)
    {
        if (event.isInline())
        {
            return Project.PROJECTFILE;
        }
        List<File> versions = this.currentproject.eventVersions(event);
        return versions.isEmpty() ? event.getFile() + " (no file)" : versions.getLast().getName();
    }

    // from the events screen
    void eventSelected(ProjectEvent event)
    {
        if (this.currenttab == EVENTTAB)
        {
            showStatus();
        }
    }

    // the life history of every entity, for the screens that look across them all
    List<Project.LifeHistory> lifeHistories()
    {
        return this.currentproject == null ? List.of() : this.currentproject.lifeHistories();
    }

    // Opens the entity, reading it if need be, and gives the node at the
    // same place in its life history as node, which may be from a copy read
    // only to be looked at. Null, after saying why, if it cannot be read.
    ElhNode openAt(ProjectEntity entity, ElhNode node)
    {
        ElhNode top = node;
        while (top.getParent() != null)
        {
            top = top.getParent();
        }
        if (entity.isLoaded() && top == entity.getTop())
        {
            return node;
        }
        List<Integer> where = node.indexPath();
        try
        {
            return this.currentproject.loadEntity(entity).nodeAtIndexPath(where);
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not read entity " + entity.getName() + "\n" + e.getMessage(),
                    "Entity", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    // from the events screen: selects the node on the entities tab
    void goToNode(ProjectEntity entity, ElhNode node)
    {
        ElhNode opened = openAt(entity, node);
        if (opened == null)
        {
            return;
        }
        this.tabs.setSelectedIndex(ENTITYTAB);
        showNode(entity, opened);
    }

    // makes the node of the entity's life history the active one
    private void showNode(ProjectEntity entity, ElhNode node)
    {
        this.activeentity = entity;
        this.activenode = node;
        maketree();
        this.currenttreeview.expand(entity);
        refresh();
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

    // the Project and Help menus are always there; between them, the menus of the tab
    private JMenuBar makeMenuBar(int tab)
    {
        JMenuBar jmb = new JMenuBar();
        JMenu jmProject = new JMenu("Project");
        addMenuItem(jmProject, "New Project", this::newProject);
        addMenuItem(jmProject, "Open Project", this::openProject);
        addMenuItem(jmProject, "Close Project", this::closeProject);
        jmProject.addSeparator();
        addMenuItem(jmProject, "Entities", () -> showProjectView(ProjectView.ENTITIESTAB));
        addMenuItem(jmProject, "Events", () -> showProjectView(ProjectView.EVENTSTAB));
        jmProject.addSeparator();
        addMenuItem(jmProject, "Exit", this::exitApp);
        jmb.add(jmProject);
        if (tab == ENTITYTAB)
        {
            addEntityMenus(jmb);
        } else if (tab == EVENTTAB)
        {
            JMenu jmEvent = new JMenu("Event");
            addMenuItem(jmEvent, "New Event", this.eventview::newEvent);
            addMenuItem(jmEvent, "New Event from Selected Actions", this.eventview::newEventFromSelected);
            addMenuItem(jmEvent, "Delete Event", this.eventview::deleteEvent);
            addMenuItem(jmEvent, "Move to Own File / into project.xml", () ->
            {
                ProjectEvent event = this.eventview.selectedEvent();
                if (event != null) moveEvent(event);
            });
            jmb.add(jmEvent);
            // to be filled once the events screen has a diagram
            jmb.add(new JMenu("View"));
            jmb.add(new JMenu("Export"));
            jmb.add(new JMenu("Options"));
        }
        JMenu jmHelp = new JMenu("Help");
        addMenuItem(jmHelp, "About", () -> JOptionPane.showMessageDialog(this,
                "myELH " + version + "\nEntity life history editor", "About myELH",
                JOptionPane.INFORMATION_MESSAGE));
        jmb.add(jmHelp);
        return jmb;
    }

    private void addEntityMenus(JMenuBar jmb)
    {
        JMenu jmEntity = new JMenu("Entity");
        addMenuItem(jmEntity, "New Entity", this::newEntity);
        addMenuItem(jmEntity, "Import Entity...", this::importEntity);
        jmEntity.addSeparator();
        addMenuItem(jmEntity, "Save", KeyEvent.VK_S, () -> saveEntity(this.activeentity));
        addMenuItem(jmEntity, "Save As New Version", this::saveNewVersion);
        addMenuItem(jmEntity, "Save All", this::saveAll);
        jmEntity.addSeparator();
        addMenuItem(jmEntity, "Move to Own File / into project.xml", () -> moveEntity(this.activeentity));
        addMenuItem(jmEntity, "Remove from Project", () -> removeEntity(this.activeentity));
        jmb.add(jmEntity);
        JMenu jmView = new JMenu("View");
        addMenuItem(jmView, "View EDIT", this::showEdit);
        addMenuItem(jmView, "View SVG", this::showSvg);
        jmView.addSeparator();
        addMenuItem(jmView, "Zoom In", KeyEvent.VK_EQUALS, () -> diagrampanel.setZoom(diagrampanel.getZoom() * 1.25));
        addMenuItem(jmView, "Zoom Out", KeyEvent.VK_MINUS, () -> diagrampanel.setZoom(diagrampanel.getZoom() / 1.25));
        addMenuItem(jmView, "Actual Size", KeyEvent.VK_0, () -> diagrampanel.setZoom(1.0));
        jmView.addSeparator();
        JCheckBoxMenuItem statesitem = new JCheckBoxMenuItem("Show State Indicators", this.showstates);
        statesitem.addActionListener(e ->
        {
            this.showstates = statesitem.isSelected();
            refresh();
        });
        jmView.add(statesitem);
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

    // ---- projects ----

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
        List<File> folders = Project.findProjects(this.elhfolder);
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

    // a project opens on its project view
    private void setProject(Project project)
    {
        this.currentproject = project;
        this.activeentity = null;
        this.activenode = null;
        this.projectname.setText(project == null ? "" : project.getName());
        if (project == null)
        {
            prefs.remove(LASTPROJECT);
        } else
        {
            prefs.put(LASTPROJECT, project.getFolder().getAbsolutePath());
        }
        enableProjectTabs();
        maketree();
        this.eventview.refresh();
        refresh();
        if (project == null)
        {
            showElhScreen();
            return;
        }
        showProjectView(ProjectView.ENTITIESTAB);
        if (!project.getConversionNotes().isEmpty())
        {
            // after the window is up, if the project was opened on starting
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "This project was saved by an earlier version of myELH and has been converted:\n\n"
                            + String.join("\n", project.getConversionNotes())
                            + "\n\nThe old ELH files are left as they were, and the old project file is kept as "
                            + Project.PROJECTFILE + ".bak.",
                    "Project converted", JOptionPane.INFORMATION_MESSAGE));
        }
    }

    // the project and events tabs are only there while a project is open
    private void enableProjectTabs()
    {
        this.tabs.setEnabledAt(PROJECTTAB, this.currentproject != null);
        this.tabs.setEnabledAt(EVENTTAB, this.currentproject != null);
    }

    // saves every changed entity and closes the project; false if one could
    // not be saved and the user chose to keep the project open
    private boolean closeProject()
    {
        if (this.currentproject == null)
        {
            return true;
        }
        if (!saveAllOrAsk("Close the project without saving?"))
        {
            return false;
        }
        setProject(null);
        return true;
    }

    private void exitApp()
    {
        if (this.currentproject != null && !saveAllOrAsk("Exit without saving?"))
        {
            return;
        }
        System.exit(0);
    }

    // true if every changed entity was saved, or the user chose to go on without
    private boolean saveAllOrAsk(String question)
    {
        return saveAll() || JOptionPane.showConfirmDialog(this, question, "Save",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    private void showProjectView(int subtab)
    {
        if (this.currentproject == null)
        {
            JOptionPane.showMessageDialog(this, "No project is open", "Project",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        this.projectview.show(this.currentproject);
        this.projectview.showTab(subtab);
        this.tabs.setSelectedIndex(PROJECTTAB);
    }

    private void showElhScreen()
    {
        this.tabs.setSelectedIndex(ENTITYTAB);
    }

    private boolean requireProject()
    {
        if (this.currentproject == null)
        {
            JOptionPane.showMessageDialog(this, "Open or create a project first (Project menu)", "No project",
                    JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        return true;
    }

    public Project getProject()
    {
        return this.currentproject;
    }

    // ---- selections in the project tree ----

    // an entity selected: its life history is read now if it has not been
    void entitySelected(ProjectEntity entity)
    {
        boolean newlyloaded = !entity.isLoaded();
        try
        {
            this.currentproject.loadEntity(entity);
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not read entity " + entity.getName() + "\n" + e.getMessage(),
                    "Entity", JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.activeentity = entity;
        this.activenode = entity.getTop();
        if (newlyloaded)
        {
            maketree();
            this.currenttreeview.expand(entity);
        }
        updateview();
    }

    // from the project screen: shows the entity on the entities tab
    void goToEntity(ProjectEntity entity)
    {
        this.tabs.setSelectedIndex(ENTITYTAB);
        entitySelected(entity);
    }

    // a node of a life history selected
    void nodeSelected(ElhNode node, ProjectEntity entity)
    {
        this.activeentity = entity;
        this.activenode = node;
        updateview();
    }


    // the project or a heading selected
    void nothingSelected()
    {
        this.activenode = null;
        updateview();
    }

    private void entityTreeSelected(Object shown)
    {
        switch (shown)
        {
            case ProjectEntity entity -> entitySelected(entity);
            case ElhNode node -> nodeSelected(node, this.currenttreeview.ownerOf(node));
            case null, default -> nothingSelected();
        }
    }

    private void entityTreePopup(Object shown, JPopupMenu menu)
    {
        switch (shown)
        {
            case ProjectEntity entity ->
            {
                TreeView.addItem(menu, "Save Entity", e -> saveEntity(entity));
                TreeView.addItem(menu, entity.isInline() ? "Move to Own File" : "Move into project.xml",
                        e -> moveEntity(entity));
                TreeView.addItem(menu, "Remove from Project", e -> removeEntity(entity));
            }
            case TreeView.Heading heading ->
            {
                TreeView.addItem(menu, "New Entity", e -> newEntity());
                TreeView.addItem(menu, "Import Entity...", e -> importEntity());
            }
            default ->
            {
            }
        }
    }

    void showEventInTab(ProjectEvent event)
    {
        this.tabs.setSelectedIndex(EVENTTAB);
        this.eventview.selectEvent(event);
    }

    // after events are added, changed or removed, or actions linked to them
    void eventsChanged()
    {
        maketree();
        refresh();
        if (this.currentproject != null)
        {
            this.projectview.show(this.currentproject);
        }
        this.eventview.refresh();
    }

    // ---- entities ----

    void newEntity()
    {
        if (!requireProject()) return;
        String name = askNodeName("entity");
        if (name == null) return;
        try
        {
            ProjectEntity entity = this.currentproject.addEntity(name);
            selectEntity(entity);
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not add entity " + name + "\n" + e.getMessage(),
                    "New Entity", JOptionPane.ERROR_MESSAGE);
        }
    }

    // reads the entities in an .elh file, of this or an earlier version of
    // myELH, and adds them to the project, kept inline
    void importEntity()
    {
        if (!requireProject()) return;
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(this.currentproject.getFolder());
        fc.setDialogTitle("Import entities from an ELH file");
        fc.setFileFilter(new FileNameExtensionFilter("ELH", "xml", "elh"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        File file = fc.getSelectedFile();
        List<ElhNode> found = new ArrayList<>();
        try
        {
            Element root = MyelhFile.readRoot(file.toPath());
            if (root.getName().equals(MyelhFile.WRAPPER))
            {
                MyelhFile.entityElements(root).forEach(e -> found.add(MyelhFile.entityFromElement(e)));
            } else
            {
                ElhNode top = ElhFile.read(file.toPath());
                found.addAll(top.getChildren().isEmpty()
                        ? List.of(new ElhNode(NodeType.ENTITY, ElhVersions.baseName(file), null))
                        : List.copyOf(top.getChildren()));
            }
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not read " + file + "\n" + e.getMessage(),
                    "Import Entity", JOptionPane.ERROR_MESSAGE);
            return;
        }
        List<String> report = new ArrayList<>();
        ProjectEntity first = null;
        for (ElhNode top : found)
        {
            int dropped = keepOwnEventLinks(top);
            try
            {
                ProjectEntity entity = this.currentproject.addEntity(top);
                first = first == null ? entity : first;
                report.add("Added " + entity.getName()
                        + (dropped == 0 ? "" : " (" + dropped + " event link" + (dropped == 1 ? "" : "s")
                        + " to another project's events removed)"));
            } catch (IOException e)
            {
                report.add("Left out " + top.getName() + ": " + e.getMessage());
            }
        }
        if (first != null)
        {
            selectEntity(first);
        }
        JOptionPane.showMessageDialog(this, report.isEmpty() ? file.getName() + " holds no entities"
                : String.join("\n", report), "Import Entity", JOptionPane.INFORMATION_MESSAGE);
    }

    // an imported life history may come from another project: a link is kept
    // only if this project has an event with that id and the node's name.
    // Returns how many were removed; Collect Events can relink them by name.
    private int keepOwnEventLinks(ElhNode node)
    {
        int dropped = 0;
        if (node.getEventref() != null)
        {
            ProjectEvent event = this.currentproject.findEvent(node.getEventref());
            if (event == null || !event.getName().equalsIgnoreCase(node.getName()))
            {
                node.setEventref(null);
                dropped++;
            }
        }
        for (ElhNode child : node.getChildren())
        {
            dropped += keepOwnEventLinks(child);
        }
        return dropped;
    }

    // shows the entity on the entities tab, selected
    private void selectEntity(ProjectEntity entity)
    {
        this.tabs.setSelectedIndex(ENTITYTAB);
        this.activeentity = entity;
        this.activenode = entity.getTop();
        maketree();
        this.currenttreeview.expand(entity);
        refresh();
        this.projectview.show(this.currentproject);
    }

    // saves an entity, inline or as a version of its file; false if it could not be
    boolean saveEntity(ProjectEntity entity)
    {
        if (entity == null || this.currentproject == null)
        {
            return true;
        }
        try
        {
            this.currentproject.saveEntity(entity);
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not save " + entity.getName() + "\n" + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        afterEntityChange();
        return true;
    }

    private void saveNewVersion()
    {
        if (this.activeentity == null) return;
        try
        {
            this.currentproject.saveEntityAsNewVersion(this.activeentity);
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not save " + this.activeentity.getName() + "\n" + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
        afterEntityChange();
    }

    // saves every entity changed since it was read or saved; false if one could not be
    boolean saveAll()
    {
        boolean saved = true;
        if (this.currentproject != null)
        {
            for (ProjectEntity entity : this.currentproject.getEntities())
            {
                if (entity.isModified())
                {
                    saved &= saveEntity(entity);
                }
            }
        }
        return saved;
    }

    // between its own file and project.xml
    void moveEntity(ProjectEntity entity)
    {
        if (entity == null || this.currentproject == null) return;
        String question = entity.isInline()
                ? "Move " + entity.getName() + " into a file of its own?\nIts life history is saved as it now is."
                : "Move " + entity.getName() + " into " + Project.PROJECTFILE
                + "?\nIts life history is saved as it now is; its files are left in the project folder.";
        if (JOptionPane.showConfirmDialog(this, question, "Move entity", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION)
        {
            return;
        }
        try
        {
            if (entity.isInline())
            {
                this.currentproject.moveEntityToFile(entity);
            } else
            {
                this.currentproject.moveEntityInline(entity);
            }
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not move " + entity.getName() + "\n" + e.getMessage(),
                    "Move entity", JOptionPane.ERROR_MESSAGE);
        }
        afterEntityChange();
    }

    // its files are left in the project folder
    void removeEntity(ProjectEntity entity)
    {
        if (entity == null || this.currentproject == null) return;
        String question = "Remove " + entity.getName() + " from the project?"
                + (entity.isModified() ? "\nIts unsaved changes will be lost." : "")
                + (entity.isInline() ? "" : "\nIts files are left in the project folder.");
        if (JOptionPane.showConfirmDialog(this, question, "Remove entity", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.OK_OPTION)
        {
            return;
        }
        try
        {
            this.currentproject.removeEntity(entity);
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not save the project\n" + e.getMessage(),
                    "Remove entity", JOptionPane.ERROR_MESSAGE);
        }
        if (this.activeentity == entity)
        {
            this.activeentity = null;
            this.activenode = null;
        }
        afterEntityChange();
    }

    // deletes the latest version of a file entity; it is read again from the
    // version before, losing any unsaved changes
    void deleteLatestVersion(ProjectEntity entity)
    {
        List<File> versions = this.currentproject.entityVersions(entity);
        if (versions.size() < 2)
        {
            JOptionPane.showMessageDialog(this, entity.getName() + " has no earlier version to go back to.\n"
                    + "Use Remove from Project to take it out of the project.", "Delete latest version",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        File latest = versions.getLast();
        if (JOptionPane.showConfirmDialog(this, "Delete " + latest.getName() + "?"
                        + (entity.isModified() ? "\nThe unsaved changes to " + entity.getName() + " will be lost." : ""),
                "Delete latest version", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
                != JOptionPane.OK_OPTION)
        {
            return;
        }
        try
        {
            Files.delete(latest.toPath());
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not delete " + latest + "\n" + e.getMessage(),
                    "Delete failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        entity.unload();
        if (this.activeentity == entity)
        {
            this.activeentity = null;
            this.activenode = null;
        }
        afterEntityChange();
    }

    private void afterEntityChange()
    {
        maketree();
        refresh();
        if (this.currentproject != null)
        {
            this.projectview.show(this.currentproject);
        }
    }

    // ---- events ----

    void newEvent()
    {
        if (!requireProject()) return;
        this.tabs.setSelectedIndex(EVENTTAB);
        this.eventview.newEvent();
    }

    // between its own file and project.xml
    void moveEvent(ProjectEvent event)
    {
        if (this.currentproject != null && EventActions.moveEvent(this, this.currentproject, event))
        {
            eventsChanged();
        }
    }

    // Makes each action part of the event, or of none if event is null.
    // The entities are opened if need be, and saved. False if one could not
    // be read or saved.
    boolean linkActions(List<EntityAction> actions, ProjectEvent event)
    {
        java.util.Set<ProjectEntity> changed = new java.util.LinkedHashSet<>();
        boolean done = true;
        for (EntityAction action : actions)
        {
            ElhNode opened = openAt(action.entity(), action.node());
            if (opened == null)
            {
                done = false;
                continue;
            }
            opened.setEventref(event == null ? null : event.getId());
            changed.add(action.entity());
        }
        for (ProjectEntity entity : changed)
        {
            done &= saveEntity(entity);
        }
        eventsChanged();
        return done;
    }

    // Asks for the name of a new action and adds it with adder. Which event
    // it is part of is settled in the Events window. The new node, or null
    // if the user cancels.
    public ElhNode addAction(java.util.function.Function<String, ElhNode> adder)
    {
        String name = askNodeName("action");
        return name == null ? null : adder.apply(name);
    }

    // ---- the right-hand side ----

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
        showEditView();
        showDiagram();
        selectActive();
        showStatus();
        repaint();
        getContentPane().validate();
    }

    // selects what is active in the tree
    private void selectActive()
    {
        if (this.activenode != null)
        {
            this.currenttreeview.select(this.activenode);
        }
    }

    public void showEditView()
    {
        this.editpanel.removeAll();
        if (this.activenode != null)
        {
            EditView panel = new EditView(this);
            panel.setup(this.activenode);
            this.editpanel.add(panel, "Center");
        }
        this.editpanel.revalidate();
        this.editpanel.repaint();
    }

    // the name for a node about to be added, or null if the user cancels
    public String askNodeName(String type)
    {
        String name = (String) JOptionPane.showInputDialog(this, "Name of the new " + type + ":",
                "New " + type, JOptionPane.PLAIN_MESSAGE, null, null, "");
        return name == null || name.isBlank() ? null : name.trim();
    }

    // redraws the diagram of the active entity's life history, outlining the active node
    private void showDiagram()
    {
        ElhDiagram diagram = this.activeentity == null || !this.activeentity.isLoaded() ? null
                : new ElhDiagram(this.activeentity.getTop(), stylemaps.getStyleMap("screen"), layoutstyle, showstates);
        // actions not yet part of an event are flagged
        Project project = this.currentproject;
        this.diagrampanel.setDiagram(diagram, this.activenode, n -> project != null && project.isUnlinked(n));
    }

    // a box clicked in the diagram: select its node in the tree
    private void selectNode(ElhNode anode)
    {
        this.activenode = anode;
        updateview();
    }

    // a box double-clicked in the diagram: select its node and switch to its edit panel
    private void openNode(ElhNode anode)
    {
        selectNode(anode);
        showEdit();
    }

    private void svgexport()
    {
        if (this.activeentity == null || !this.activeentity.isLoaded())
        {
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save SVG file");
        fc.setCurrentDirectory(this.currentproject == null ? this.elhfolder : this.currentproject.getFolder());
        fc.setFileFilter(new FileNameExtensionFilter("svg", "svg"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        try
        {
            // exported with the screen styles, as it always has been; the
            // "print" style map is kept but not used
            SvgExport.write(this.activeentity.getTop(), stylemaps.getStyleMap("screen"), layoutstyle, showstates,
                    fc.getSelectedFile().toPath());
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not save " + fc.getSelectedFile() + "\n" + e.getMessage(),
                    "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // after the selection changes
    public void updateview()
    {
        refresh();
    }

    // rebuilds the project tree, keeping what is active selected
    public void maketree()
    {
        this.currenttreeview.maketree(this.currentproject);
        selectActive();
    }
}
