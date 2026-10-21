package org.lerot.myELH;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class elh extends elhnode
{
    static String[] childtypes = {"event", "role"};
    static String[] childgroupingtypes = {"sequence", "option", "repetition", "rolegroup"};

    public elh(String aname, elhnode aparent)
    {
        super(aname, aparent);
    }

    public elh(String aname)
    {
        super(aname, null);
    }

    // Files saved before this wrapped the whole tree in an extra
    // <elh name="new elh"> with no other attributes; real elh nodes are
    // always saved with childgrouptype and stylename.
    static boolean isOldWrapper(Element root)
    {
        return root.getName().equals("elh")
                && "new elh".equals(root.getAttributeValue("name"))
                && root.getAttribute("childgrouptype") == null
                && root.getAttribute("stylename") == null
                && root.getChildren().size() == 1
                && ((Element) root.getChildren().getFirst()).getName().equals("elh");
    }

    public void makeElh(String filename) throws IOException
    {
        try (PrintWriter printWriter = new PrintWriter(filename, StandardCharsets.UTF_8))
        {
            printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            printWriter.println("<?xml-stylesheet type='text/xsl' href='./Stylesheets/elh.xsl' ?>");
            printWriter.print(xmlout());
            if (printWriter.checkError()) throw new IOException("Could not write " + filename);
        }
    }

    public void importELH(String importfilename)
    {
        SAXBuilder builder = new SAXBuilder();
        File xmlFile = new File(importfilename);
        try
        {
            Document doc = builder.build(xmlFile);
            Element rootNode = doc.getRootElement();
            if (isOldWrapper(rootNode))
            {
                rootNode = (Element) rootNode.getChildren().getFirst();
            }
            loadXML(rootNode);
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
        } catch (JDOMException e2)
        {
            e2.printStackTrace();
        }
    }

    public void insert(String string)
    {
        System.out.println(" inserting  entity");
        entity newentity = new entity("newentity", this);
        addChild(newentity);
    }
}


