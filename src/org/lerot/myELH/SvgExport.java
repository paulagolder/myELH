package org.lerot.myELH;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Document;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

// Writes the diagram of an ELH tree to an SVG file
public final class SvgExport
{
    private static final String SVGNS = "http://www.w3.org/2000/svg";

    private SvgExport()
    {
    }

    public static void write(ElhNode root, StyleMap stylemap, LayoutStyle layoutstyle, Path file) throws IOException
    {
        write(root, stylemap, layoutstyle, false, file);
    }

    // an event's process as a flowchart
    public static void writeFlowchart(String title, EventProcess process, Path file) throws IOException
    {
        Document document = GenericDOMImplementation.getDOMImplementation().createDocument(SVGNS, "svg", null);
        SVGGraphics2D svggraphic = new SVGGraphics2D(document);
        ProcessFlowchart chart = new ProcessFlowchart(title, process, svggraphic);
        chart.draw(svggraphic);
        svggraphic.setSVGCanvasSize(new Dimension((int) Math.ceil(chart.getWidth()), (int) Math.ceil(chart.getHeight())));
        try (Writer out = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8))
        {
            svggraphic.stream(out, true);
        }
    }

    // with the actions' state indicator values under their boxes if showstates is true
    public static void write(ElhNode root, StyleMap stylemap, LayoutStyle layoutstyle, boolean showstates, Path file)
            throws IOException
    {
        Document document = GenericDOMImplementation.getDOMImplementation().createDocument(SVGNS, "svg", null);
        SVGGraphics2D svggraphic = new SVGGraphics2D(document);
        ElhDiagram diagram = new ElhDiagram(root, stylemap, layoutstyle, svggraphic, showstates);
        diagram.draw(svggraphic);
        svggraphic.setSVGCanvasSize(new Dimension((int) diagram.getWidth(), (int) diagram.getHeight()));
        // an OutputStreamWriter, so that Batik writes encoding="UTF-8" in the header
        try (Writer out = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8))
        {
            svggraphic.stream(out, true);
        }
    }
}
