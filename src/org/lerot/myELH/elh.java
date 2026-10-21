package org.lerot.myELH;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.ProcessingInstruction;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

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
                && root.getChildren().getFirst().getName().equals("elh");
    }

    public void makeElh(String filename) throws IOException
    {
        Document doc = new Document();
        doc.addContent(new ProcessingInstruction("xml-stylesheet",
                "type='text/xsl' href='./Stylesheets/elh.xsl'"));
        doc.setRootElement(toElement());
        // Files.newBufferedWriter writes UTF-8, which is also the outputter's default
        try (Writer out = Files.newBufferedWriter(Path.of(filename)))
        {
            new XMLOutputter(Format.getPrettyFormat()).output(doc, out);
        }
    }

    public void importELH(String importfilename) throws IOException
    {
        SAXBuilder builder = new SAXBuilder();
        File xmlFile = new File(importfilename);
        Document doc;
        try
        {
            doc = builder.build(xmlFile);
        } catch (JDOMException e)
        {
            throw new IOException(e.getMessage(), e);
        }
        Element rootNode = doc.getRootElement();
        if (isOldWrapper(rootNode))
        {
            rootNode = rootNode.getChildren().getFirst();
        }
        loadXML(rootNode);
    }

    public void insert(String string)
    {
        System.out.println(" inserting  entity");
        entity newentity = new entity("newentity", this);
        addChild(newentity);
    }
}


