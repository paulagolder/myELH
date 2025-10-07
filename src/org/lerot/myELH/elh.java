package org.lerot.myELH;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

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

    public void makeElh(String filename)
    {
        try
        {
            PrintWriter printWriter = new PrintWriter(filename);
            printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            printWriter.println("<?xml-stylesheet type='text/xsl' href='./Stylesheets/elh.xsl' ?>");
            printWriter.println("<elh name=\"new elh\" >\n");
            printWriter.print(xmlout());
            printWriter.println("</elh>");
            printWriter.close();
        } catch (Exception e)
        {
            e.printStackTrace();
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


