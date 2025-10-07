package org.lerot.myELH;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.theme.DesertBluer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.lerot.mywidgets.jswStyle;
import org.lerot.mywidgets.jswStyles;
import org.lerot.mywidgets.jswVerticalPanel;

public class myELHgui extends JFrame implements ActionListener, ComponentListener
{
    private static final long serialVersionUID = 1L;
    private static final String version = "2.0";
    public static myELHgui mframe;
    static JLabel activepath;
    private final String userdir;
    private final String user;
    private final String osversion;
    private final String os;
    private final String dotelh;
    private final JLabel sourcefile;
    private final JPanel svgpanel;
    private final JMenuItem stylej;
    private final JMenuItem styled;
    private final String userhome;
    private final String propsfile;
    private final Object props;
    private final String elhfolder;
    public stylemaps mystylemaps;
    public DefaultMutableTreeNode activetreenode;
    public elhnode activenode;
    public Font promptfont;
    treeview currentview;
    elh currentelh;
    jswVerticalPanel editpanel;
    boolean jacksonstyle;
    JPopupMenu popup;
    private ImageIcon jstatIcon;
    private File currentfile;
    private File svgfile;
    private elhdrawnode topdrawnode;
    private Dimension closeButtonSize;
    private String view;
    private editview evpanel;
    private editview evpanel2;
    private editview evpanel3;

    public myELHgui(int w, int h)
    {
        super("ELH 0.2");
        // jswStyles.initiateStyles();
        this.jacksonstyle = false;
        this.userdir = System.getProperty("user.dir");
        this.userhome = System.getProperty("user.home");
        this.user = System.getProperty("user.name");
        this.osversion = System.getProperty("os.version");
        this.os = System.getProperty("os.name");
        this.mystylemaps = new stylemaps();
        System.out.println("user :" + this.user);
        System.out.println("user directory :" + this.userdir);
        System.out.println("operating system :" + this.os + "(" + this.osversion + ")");
        if (this.os.startsWith("Windows"))
        {
            this.dotelh = "C:/Users/" + this.user + "/.myelh/";
            this.elhfolder = "C:/Users/" + this.user + "/Documents/elh";
        } else
        {
            this.dotelh = "/home/" + this.user + "/.myelh/";
            this.elhfolder = "/home/" + this.user + "/Documents/elh";
        }
        URL jstatIconURL = myELHgui.class.getResource("/resources/elh.png");
        if (jstatIconURL != null)
        {
            this.jstatIcon = new ImageIcon(jstatIconURL);
            Image jstatIconImage = this.jstatIcon.getImage();
            setIconImage(jstatIconImage);
            this.closeButtonSize = new Dimension(this.jstatIcon.getIconWidth() + 2, this.jstatIcon.getIconHeight() + 2);
        } else
        {
            System.out.println("no icon");
        }
        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
        JMenuBar jmb = new JMenuBar();
        JMenu jmFile = new JMenu("File");
        JMenuItem jmiOpen = new JMenuItem("Open");
        JMenuItem jmiNew = new JMenuItem("New");
        JMenuItem jmiClose = new JMenuItem("Close");
        JMenuItem jmiSave = new JMenuItem("Save As");
        JMenuItem jmiSaveAs = new JMenuItem("Save");
        JMenuItem jmiExit = new JMenuItem("Exit");
        jmFile.add(jmiOpen);
        jmFile.add(jmiNew);
        jmFile.add(jmiSaveAs);
        jmFile.add(jmiSave);
        jmFile.add(jmiClose);
        jmFile.addSeparator();
        jmFile.add(jmiExit);
        jmb.add(jmFile);
        JMenu jmView = new JMenu("View");
        JMenuItem medit = new JMenuItem("View EDIT");
        JMenuItem svgdraw = new JMenuItem("View SVG");
        jmView.add(medit);
        jmView.add(svgdraw);
        jmb.add(jmView);
        JMenu jmExport = new JMenu("Export");
        JMenuItem svgn = new JMenuItem("SVG New");
        JMenuItem svg = new JMenuItem("SVG test");
        JMenuItem odf = new JMenuItem("ODF");
        jmExport.add(svgn);
        jmExport.add(svg);
        jmExport.add(odf);
        jmb.add(jmExport);
        JMenu jmOptions = new JMenu("Options");
        this.stylej = new JMenuItem("Jackson Style");
        this.styled = new JMenuItem("Default Style");
        jmOptions.add(this.stylej);
        jmOptions.add(this.styled);
        jmb.add(jmOptions);
        JMenu jmHelp = new JMenu("Help");
        JMenuItem jmiAbout = new JMenuItem("About");
        jmHelp.add(jmiAbout);
        jmb.add(jmHelp);
        jmiOpen.addActionListener(this);
        jmiNew.addActionListener(this);
        jmiClose.addActionListener(this);
        jmiSaveAs.addActionListener(this);
        jmiSave.addActionListener(this);
        jmiExit.addActionListener(this);
        medit.addActionListener(this);
        svgdraw.addActionListener(this);
        svgn.addActionListener(this);
        this.stylej.addActionListener(this);
        this.styled.addActionListener(this);
        jmiAbout.addActionListener(this);
        setJMenuBar(jmb);
        setVisible(true);
        setSize(w, h);
        getContentPane().setLayout(new BoxLayout(getContentPane(), 1));
        JPanel statusbar = new JPanel();
        statusbar.setLayout(new BoxLayout(statusbar, 0));
        statusbar.add(new JLabel("ACTIVE FILE:"));
        statusbar.add(this.sourcefile = new JLabel());
        statusbar.add(Box.createHorizontalGlue());
        statusbar.add(new JLabel("ACTIVE ENTITY:"));
        statusbar.add(activepath = new JLabel());
        add(statusbar);
        this.currentview = new treeview();
        this.editpanel = new jswVerticalPanel("editpanel", false, false);
        this.editpanel.setBorder(new LineBorder(Color.PINK, 2));
        this.editpanel.setLayout(new BorderLayout());
        showEditView(this.editpanel, this.activenode);
        (this.svgpanel = new JPanel()).setBorder(new LineBorder(Color.BLUE, 2));
        this.svgpanel.setLayout(new BorderLayout());
        this.svgpanel.addComponentListener(this);
        JPanel mainpanels = new JPanel();
        mainpanels.setLayout(new BoxLayout(mainpanels, 0));
        mainpanels.add(this.currentview);
        mainpanels.add(this.editpanel);
        mainpanels.add(this.svgpanel);
        this.editpanel.setVisible(false);
        this.svgpanel.setVisible(true);
        add(mainpanels);
        repaint();
        getContentPane().validate();
        setDefaultCloseOperation(3);
        this.propsfile = this.dotelh + "properties.xml";
        this.props = readProperties(this.propsfile);
        this.view = "EDIT";
        this.editpanel.setVisible(true);
        this.svgpanel.setVisible(false);
    }

    public static void main(String[] args)
    {
        try
        {
            PlasticLookAndFeel.setPlasticTheme(new DesertBluer());
            UIManager.setLookAndFeel((LookAndFeel) new Plastic3DLookAndFeel());
        } catch (Exception exception)
        {
        }
        UIManager.put("FileChooser.readOnly", Boolean.TRUE);
        (mframe = new myELHgui(800, 400)).addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
        mframe.setLocation(50, 50);
        mframe.setMinimumSize(new Dimension(800, 400));
        mframe.pack();
        mframe.setVisible(true);
    }

    public static jswStyles tablestyles()
    {
        jswStyles tablestyles = new jswStyles();
        tablestyles.name = "defaulttable";
        jswStyle tablestyle = tablestyles.makeStyle("table");
        tablestyle.putAttribute("borderwidth", 1);
        tablestyle.putAttribute("bordercolor", "gray");
        jswStyle rowstyle0 = tablestyles.makeStyle("row_0");
        rowstyle0.putAttribute("backgroundcolor", "gray");
        rowstyle0.putAttribute("fontsize", 16);
        rowstyle0.putAttribute("fontStyle", Font.BOLD + Font.ITALIC);
        rowstyle0.putAttribute("foregroundColor", "#0e56f2");
        jswStyle colstyle0 = tablestyles.makeStyle("col_0");
        colstyle0.putAttribute("backgroundcolor", "blue");
        colstyle0.putAttribute("horizontalAlignment", "RIGHT");
        colstyle0.putAttribute("minwidth", 30);
        colstyle0.putAttribute("fontsize", "16");
        colstyle0.putAttribute("foregroundcolor", "gray");
        jswStyle colstyle = tablestyles.makeStyle("col");
        colstyle.putAttribute("horizontalAlignment", "RIGHT");
        colstyle.putAttribute("width", 30);
        jswStyle cellstyle = tablestyles.makeStyle("cell");
        cellstyle.putAttribute("foregroundcolor", "black");
        cellstyle.putAttribute("borderWidth", "1");
        cellstyle.putAttribute("borderColor", "black");
        cellstyle.putAttribute("fontsize", 16);
        return tablestyles;
    }

    public static jswStyles tablestyles2()
    {
        jswStyles tablestyles = new jswStyles();
        tablestyles.name = "defaulttable";
        jswStyle tablestyle = tablestyles.makeStyle("table");
        tablestyle.putAttribute("borderwidth", 1);
        tablestyle.putAttribute("bordercolor", "gray");
        jswStyle rowstyle0 = tablestyles.makeStyle("row_0");
        rowstyle0.putAttribute("backgroundcolor", "gray");
        rowstyle0.putAttribute("fontsize", 16);
        // rowstyle0.putAttribute("fontStyle", Font.BOLD + Font.ITALIC);
        //  rowstyle0.putAttribute("foregroundColor", "#0e56f2");
        jswStyle colstyle0 = tablestyles.makeStyle("col_0");
        colstyle0.putAttribute("backgroundcolor", "blue");
        colstyle0.putAttribute("horizontalAlignment", "RIGHT");
        colstyle0.putAttribute("minwidth", 30);
        colstyle0.putAttribute("fontsize", "16");
        colstyle0.putAttribute("foregroundcolor", "gray");
        jswStyle colstyle = tablestyles.makeStyle("col");
        colstyle.putAttribute("horizontalAlignment", "RIGHT");
        colstyle.putAttribute("width", 30);
        jswStyle cellstyle = tablestyles.makeStyle("cell");
        cellstyle.putAttribute("foregroundcolor", "black");
        cellstyle.putAttribute("borderWidth", "1");
        cellstyle.putAttribute("borderColor", "black");
        cellstyle.putAttribute("fontsize", 16);
        return tablestyles;
    }

    public void actionPerformed(ActionEvent ae)
    {
        String comStr = ae.getActionCommand();
        if (comStr.equalsIgnoreCase("Jackson Style"))
        {
            this.jacksonstyle = true;
            this.stylej.setEnabled(false);
            this.styled.setEnabled(true);
        } else if (comStr.equalsIgnoreCase("Default Style"))
        {
            this.jacksonstyle = false;
            this.stylej.setEnabled(true);
            this.styled.setEnabled(false);
        } else if (comStr.equalsIgnoreCase("svg"))
        {
            svgexport(true);
        } else if (comStr.equalsIgnoreCase("svg new"))
        {
            svgexport(true);
        } else if (comStr.equalsIgnoreCase("VIEW SVG"))
        {
            this.view = "SVG";
            this.editpanel.setVisible(false);
            this.svgpanel.setVisible(true);
        } else if (comStr.equalsIgnoreCase("VIEW EDIT"))
        {
            this.view = "EDIT";
            this.editpanel.setVisible(true);
            this.svgpanel.setVisible(false);
        } else if (comStr.startsWith("VIEW:"))
        {
            String path = comStr.substring(5);
            activepath.setText(path);
            showEditView();
            repaint();
            getContentPane().validate();
        } else if (comStr.equalsIgnoreCase("INSERT ABOVE CHILDREN"))
        {
            if (this.activenode == null)
            {
                return;
            }
            if (this.activenode instanceof event)
            {
                this.activenode.insertabovechildren();
            } else if (this.activenode instanceof entity)
            {
                this.activenode.insertabovechildren();
            }
            this.currentview.maketree(this.currentelh);
            showEditView();
            repaint();
            getContentPane().validate();
        } else if (comStr.equalsIgnoreCase("insert child"))
        {
            if (this.activenode == null)
            {
                return;
            }
            if (this.activenode instanceof event)
            {
                ((event) this.activenode).insert("newevent");
            } else if (this.activenode instanceof entity)
            {
                ((entity) this.activenode).insert("newevent");
            } else if (this.activenode instanceof elh)
            {
                ((elh) this.activenode).insert("newevent");
            }
            this.currentview.maketree(this.currentelh);
            showEditView();
            repaint();
            getContentPane().validate();
        } else if (comStr.equalsIgnoreCase("insert sibling"))
        {
            if (this.activenode != null && this.activenode instanceof event)
            {
                ((event) this.activenode).insert("newevent");
            }
            this.currentview.maketree(this.currentelh);
        } else if (comStr.equalsIgnoreCase("open"))
        {
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(new File(this.elhfolder));
            fc.setDialogTitle("Open ELH file");
            FileNameExtensionFilter filter = new FileNameExtensionFilter("ELH", "xml", "elh");
            fc.setFileFilter(filter);
            int returnVal = fc.showOpenDialog(this);
            if (returnVal == 0)
            {
                File fileToLoad = fc.getSelectedFile();
                this.currentfile = fileToLoad;
                this.sourcefile.setText(this.currentfile.getName());
                this.currentelh = new elh(this.currentfile.getName());
                this.currentelh.importELH(this.currentfile.getAbsolutePath());
                this.currentview.maketree(this.currentelh);
                this.activenode = null;
                this.currentview.repaint();
                getContentPane().validate();
            } else
            {
                System.out.println("Open command cancelled by user.");
            }
        } else if (comStr.equalsIgnoreCase("new"))
        {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Specify a new ELH file");
            FileNameExtensionFilter filter = new FileNameExtensionFilter("ELH", "xml", "elh");
            fc.setFileFilter(filter);
            int returnVal = fc.showSaveDialog(this);
            if (returnVal == 0)
            {
                File fileToSave = fc.getSelectedFile();
                this.currentfile = fileToSave;
            } else
            {
                System.out.println("Open command cancelled by user.");
            }
            this.currentelh = new elh(this.currentfile.getName());
            this.currentelh.makeElh(this.currentfile.getAbsolutePath());
            this.currentview.maketree(this.currentelh);
        } else if (comStr.equalsIgnoreCase("save as"))
        {
            JFileChooser fc = new JFileChooser();
            fc.setCurrentDirectory(new File(this.elhfolder));
            int returnVal = fc.showOpenDialog(this);
            fc.setDialogTitle("Save to new ELH file");
            FileNameExtensionFilter filter = new FileNameExtensionFilter("ELH", "xml", "elh");
            fc.setFileFilter(filter);
            returnVal = fc.showSaveDialog(this);
            if (returnVal == 0)
            {
                File fileToSave = fc.getSelectedFile();
                this.currentfile = fileToSave;
            } else
            {
                System.out.println("Open command cancelled by user.");
            }
            this.currentelh.makeElh(this.currentfile.getAbsolutePath());
        } else if (comStr.equalsIgnoreCase("save"))
        {
            this.currentelh.makeElh(this.currentfile.getAbsolutePath());
        } else
        {
            System.out.println("command " + comStr + " unrecognised ");
        }
    }

    public void componentHidden(ComponentEvent arg0)
    {
    }

    public void componentMoved(ComponentEvent arg0)
    {
    }

    public void componentResized(ComponentEvent arg0)
    {
        if (mframe != null)
        {
            refresh();
        }
    }

    public void componentShown(ComponentEvent arg0)
    {
    }

    public void refresh()
    {
        System.out.println(" in refresh " + this.activetreenode);
        if (this.activetreenode != null)
        {
            TreePath treepath = new TreePath(mframe.activetreenode.getPath());
            mframe.currentview.thistree.setSelectionPath(treepath);
            mframe.currentview.thistree.scrollPathToVisible(treepath);
        }
        mframe.showEditView();
        // mframe.showSVGView();
        mframe.repaint();
        getContentPane().validate();
        if (this.activetreenode != null)
        {
            TreePath treepath = new TreePath(mframe.activetreenode.getPath());
            mframe.currentview.thistree.setSelectionPath(treepath);
            mframe.currentview.thistree.scrollPathToVisible(treepath);
        }
    }

    public void showEditView()
    {
        showEditView(this.editpanel, this.activenode);
    }

    public void showEditView(JPanel apanel, Object activenode2)
    {
        apanel.removeAll();
        if (this.activenode == null)
        {
            return;
        }
        if (this.activenode instanceof event)
        {
            this.evpanel = new editviewevent();
            ((editviewevent) this.evpanel).setup((event) this.activenode);
            apanel.add(this.evpanel, "Center");
        } else if (this.activenode instanceof entity)
        {
            this.evpanel2 = new editviewentity();
            this.evpanel2.setup(this.activenode);
            apanel.add(this.evpanel2, "Center");
        } else if (this.activenode instanceof rolegroup)
        {
            this.evpanel2 = new editviewrolegroup();
            this.evpanel2.setup(this.activenode);
            apanel.add(this.evpanel2, "Center");
        } else if (this.activenode instanceof role)
        {
            this.evpanel2 = new editviewrole();
            this.evpanel2.setup(this.activenode);
            apanel.add(this.evpanel2, "Center");
        } else if (this.activenode instanceof elh)
        {
            this.evpanel3 = new editviewelh();
            this.evpanel3.setup(this.activenode);
            apanel.add(this.evpanel3, "Center");
        }
        repaint();
        getContentPane().validate();
    }

    public void showSVGView()
    {
        showSVGView(this.svgpanel, this.topdrawnode);
    }

    private void showSVGView(JPanel apanel, elhdrawnode activenode)
    {
        apanel.removeAll();
        apanel.setBackground(Color.pink);
        Dimension cas = apanel.getSize();
        if (activenode == null)
        {
            return;
        }
        if (activenode.elhtype.equalsIgnoreCase("event"))
        {
            svgdoc adoc = new svgdoc();
            adoc.setup(this.topdrawnode, cas.width, cas.height);
            svgview svgpanel = new svgview();
            svgpanel.setLayout(new BorderLayout());
            svgpanel.setBackground(Color.cyan);
            svgpanel.showview(adoc);
            apanel.add(svgpanel, "Center");
            apanel.repaint();
        } else if (activenode.elhtype.equalsIgnoreCase("entity"))
        {
            svgdoc adoc = new svgdoc();
            adoc.setup(this.topdrawnode, cas.width, cas.height);
            svgview svgpanel = new svgview();
            svgpanel.setLayout(new BorderLayout());
            svgpanel.setBackground(Color.green);
            svgpanel.showview(adoc);
            apanel.add(svgpanel, "Center");
            apanel.repaint();
        } else if (activenode.elhtype.equalsIgnoreCase("rolegroup"))
        {
            svgdoc adoc = new svgdoc();
            adoc.setup(this.topdrawnode, cas.width, cas.height);
            svgview svgpanel = new svgview();
            svgpanel.setLayout(new BorderLayout());
            svgpanel.setBackground(Color.green);
            svgpanel.showview(adoc);
            apanel.add(svgpanel, "Center");
            apanel.repaint();
        } else if (activenode.elhtype.equalsIgnoreCase("role"))
        {
            svgdoc adoc = new svgdoc();
            adoc.setup(this.topdrawnode, cas.width, cas.height);
            svgview svgpanel = new svgview();
            svgpanel.setLayout(new BorderLayout());
            svgpanel.setBackground(Color.green);
            svgpanel.showview(adoc);
            apanel.add(svgpanel, "Center");
            apanel.repaint();
        } else
        {
            activenode.elhtype.equalsIgnoreCase("elh");
        }
        repaint();
        getContentPane().validate();
    }

    private void svgexport(boolean saveas)
    {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save SVG file");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("svg", "svg");
        fc.setFileFilter(filter);
        int returnVal = fc.showSaveDialog(this);
        if (returnVal != 0)
        {
            System.out.println("Open command cancelled by user.");
            return;
        }
        File fileToSave = fc.getSelectedFile();
        this.svgfile = fileToSave;
        if (this.topdrawnode == null)
        {
            return;
        }
        if (this.topdrawnode.elhtype.equalsIgnoreCase("event"))
        {
            this.topdrawnode.updatetree();
            svgdoc adoc = new svgdoc();
            dimensionp dim = adoc.getExportSize(this.topdrawnode);
            adoc.setup(this.topdrawnode, dim.width, dim.height);
            adoc.output(this.svgfile);
        } else if (this.topdrawnode.elhtype.equalsIgnoreCase("entity"))
        {
            this.topdrawnode.updatetree();
            svgdoc adoc = new svgdoc();
            dimensionp dim = adoc.getExportSize(this.topdrawnode);
            adoc.setup(this.topdrawnode, dim.width, dim.height);
            adoc.output(this.svgfile);
        }
    }

    public void updateview(String treepath)
    {
        activepath.setText(treepath);
        if (mframe.currentelh != null)
        {
            this.activenode = mframe.currentelh.findActiveEvent(treepath);
            this.activetreenode = mframe.currentview.thistree.findActiveTreenode(treepath);
            this.topdrawnode = new elhdrawnode(this.activenode, 0, 5);
            mframe.currentview.expandPath(this.activetreenode);
            mframe.showEditView();
            //mframe.showSVGView();
        }
        mframe.repaint();
        getContentPane().validate();
    }

    public void updateview(elhnode activenode)
    {
        String treepath = elhnode.makepath(activenode);
        activepath.setText(treepath);
        if (mframe.currentelh != null)
        {
            activenode = mframe.currentelh.findActiveEvent(treepath);
            this.activetreenode = mframe.currentview.thistree.findActiveTreenode(treepath);
            this.topdrawnode = new elhdrawnode(activenode, 0, 5);
            mframe.currentview.expandPath(this.activetreenode);
            mframe.showEditView();
            //mframe.showSVGView();
        }
        mframe.repaint();
        getContentPane().validate();
    }

    public void updateview()
    {
        updateview(this.activenode);
    }

    public Properties readProperties(String propsfile)
    {
        Properties prop = new Properties();
        try
        {
            prop.loadFromXML(new FileInputStream(propsfile));
            return prop;
        } catch (InvalidPropertiesFormatException e)
        {
            e.printStackTrace();
        } catch (FileNotFoundException fileNotFoundException)
        {
        } catch (IOException iOException)
        {
        }
        return null;
    }

    public void maketree()
    {
        this.currentview.maketree(this.currentelh);
    }
}


