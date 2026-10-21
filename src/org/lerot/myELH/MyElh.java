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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

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
    private final JLabel sourcefile = new JLabel();
    private final JLabel activepath = new JLabel();
    private final JPanel svgpanel;
    public DefaultMutableTreeNode activetreenode;
    public ElhNode activenode;
    TreeView currenttreeview;
    Elh currentelh;
    jswVerticalPanel editpanel;
    private File currentfile;
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
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setJMenuBar(makeMenuBar());
        setSize(w, h);
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        JPanel statusbar = new JPanel();
        statusbar.setLayout(new BoxLayout(statusbar, BoxLayout.X_AXIS));
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
        add(mainpanels);
        showEdit();
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
        JMenu jmFile = new JMenu("File");
        addMenuItem(jmFile, "Open", this::openElh);
        addMenuItem(jmFile, "New", this::newElh);
        addMenuItem(jmFile, "Save", this::saveElh);
        addMenuItem(jmFile, "Save As", this::saveElhAs);
        addMenuItem(jmFile, "Close", this::closeElh);
        jmFile.addSeparator();
        addMenuItem(jmFile, "Exit", () -> System.exit(0));
        jmb.add(jmFile);
        JMenu jmView = new JMenu("View");
        addMenuItem(jmView, "View EDIT", this::showEdit);
        addMenuItem(jmView, "View SVG", this::showSvg);
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

    private JFileChooser elhChooser(String title)
    {
        JFileChooser fc = new JFileChooser();
        fc.setCurrentDirectory(this.elhfolder);
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
        File openfile = fc.getSelectedFile();
        Elh openelh = new Elh(openfile.getName());
        try
        {
            openelh.importELH(openfile.getAbsolutePath());
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not open " + openfile + "\n" + e.getMessage(),
                    "Open failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        this.currentfile = openfile;
        this.sourcefile.setText(this.currentfile.getName());
        this.currentelh = openelh;
        this.currenttreeview.maketree(this.currentelh);
        this.activenode = null;
        this.activetreenode = null;
        this.currenttreeview.repaint();
        showDiagram();
        getContentPane().validate();
    }

    private void newElh()
    {
        JFileChooser fc = elhChooser("Specify a new ELH file");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        this.currentfile = fc.getSelectedFile();
        this.currentelh = new Elh(this.currentfile.getName());
        writeElh();
        this.currenttreeview.maketree(this.currentelh);
    }

    private void saveElh()
    {
        if (this.currentfile == null)
        {
            saveElhAs();
        } else
        {
            writeElh();
        }
    }

    private void saveElhAs()
    {
        if (this.currentelh == null)
        {
            return;
        }
        JFileChooser fc = elhChooser("Save to new ELH file");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
        {
            return;
        }
        this.currentfile = fc.getSelectedFile();
        writeElh();
    }

    private void writeElh()
    {
        if (this.currentelh == null)
        {
            return;
        }
        try
        {
            this.currentelh.makeElh(this.currentfile.getAbsolutePath());
            this.sourcefile.setText(this.currentfile.getName());
        } catch (IOException e)
        {
            JOptionPane.showMessageDialog(this, "Could not save " + this.currentfile + "\n" + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void closeElh()
    {
        this.currentelh = null;
        this.currentfile = null;
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
        EditView panel = switch (this.activenode)
        {
            case Event anevent ->
            {
                EditViewEvent eventpanel = new EditViewEvent(this);
                eventpanel.setup(anevent);
                yield eventpanel;
            }
            case Entity anentity -> setupEditView(new EditViewEntity(this), anentity);
            case RoleGroup arolegroup -> setupEditView(new EditViewRoleGroup(this), arolegroup);
            case Role arole -> setupEditView(new EditViewRole(this), arole);
            case Elh anelh -> setupEditView(new EditViewElh(this), anelh);
            // entity groups have no edit panel
            case EntityGroup anentitygroup -> null;
        };
        if (panel != null)
        {
            this.editpanel.add(panel, "Center");
        }
        repaint();
        getContentPane().validate();
    }

    private static EditView setupEditView(EditView panel, ElhNode anode)
    {
        panel.setup(anode);
        return panel;
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
        fc.setCurrentDirectory(this.elhfolder);
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
