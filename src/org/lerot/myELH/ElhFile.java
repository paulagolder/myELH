package org.lerot.myELH;

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

// Reads and writes .elh files: the XML of an ELH's top node and everything below it.
public final class ElhFile
{
    private ElhFile()
    {
    }

    // Files saved before this wrapped the whole tree in an extra
    // <elh name="new elh"> with no other attributes; real elh nodes are
    // always saved with childgrouptype (and, in those files, stylename).
    static boolean isOldWrapper(Element root)
    {
        return root.getName().equals("elh")
                && "new elh".equals(root.getAttributeValue("name"))
                && root.getAttribute("childgrouptype") == null
                && root.getAttribute("stylename") == null
                && root.getChildren().size() == 1
                && root.getChildren().getFirst().getName().equals("elh");
    }

    public static void write(ElhNode top, Path file) throws IOException
    {
        Document doc = new Document();
        doc.addContent(new ProcessingInstruction("xml-stylesheet",
                "type='text/xsl' href='./Stylesheets/elh.xsl'"));
        doc.setRootElement(top.toElement());
        // Files.newBufferedWriter writes UTF-8, which is also the outputter's default
        try (Writer out = Files.newBufferedWriter(file))
        {
            new XMLOutputter(Format.getPrettyFormat()).output(doc, out);
        }
    }

    // the top node is always an elh, whatever its element is called in
    // older files (some have <entities>); it is named after the file unless
    // the file gives it a name
    public static ElhNode read(Path file) throws IOException
    {
        Document doc;
        try
        {
            doc = new SAXBuilder().build(file.toFile());
        } catch (JDOMException e)
        {
            throw new IOException(e.getMessage(), e);
        }
        Element root = doc.getRootElement();
        if (isOldWrapper(root))
        {
            root = root.getChildren().getFirst();
        }
        ElhNode top = new ElhNode(NodeType.ELH, file.getFileName().toString(), null);
        top.loadXML(root);
        return top;
    }
}
