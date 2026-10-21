package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests of laying out and exporting the diagram; none of them open a window.
class ElhDiagramTest
{
    @TempDir
    Path tempdir;

    // an elh with an entity holding events a, b and c, where b has two children
    private Elh makeTree()
    {
        Elh top = new Elh("top");
        Entity person = new Entity("Person", top);
        top.addChild(person);
        for (String name : new String[]{"a", "b", "c"})
        {
            person.addChild(new Event(name, person));
        }
        ElhNode b = person.findChildnode("b");
        b.addChild(new Event("b1", b));
        b.addChild(new Event("b2 with a much longer name", b));
        return top;
    }

    private ElhDiagram screenDiagram(ElhNode root)
    {
        return new ElhDiagram(root, StyleMaps.getScreenStyles(), LayoutStyle.DEFAULT);
    }

    @Test
    void childrenSitBelowTheirParentWithoutOverlapping()
    {
        ElhDiagram diagram = screenDiagram(makeTree());
        ElhDrawNode person = diagram.top.getChildren().getFirst();
        List<ElhDrawNode> events = person.getChildren();
        for (ElhDrawNode event : events)
        {
            assertTrue(event.bounds.y >= person.bounds.y + person.bounds.height, event + " is below Person");
        }
        for (int i = 1; i < events.size(); i++)
        {
            ElhDrawNode left = events.get(i - 1);
            assertTrue(events.get(i).bounds.x >= left.bounds.x + left.bounds.width, events.get(i) + " is right of " + left);
        }
    }

    @Test
    void aParentIsCentredOverItsChildren()
    {
        ElhDrawNode person = screenDiagram(makeTree()).top.getChildren().getFirst();
        ElhDrawNode first = person.getChildren().getFirst();
        ElhDrawNode last = person.getChildren().getLast();
        double childrencentre = (first.bounds.x + last.bounds.x + last.bounds.width) / 2;
        assertEquals(childrencentre, person.bounds.x + person.bounds.width / 2, 0.001);
    }

    @Test
    void aLongNameWithNoSpacesMakesAWiderBox()
    {
        Elh top = makeTree();
        ElhNode a = top.findChildnode("Person").findChildnode("a");
        a.setName("Aquitedeliberatelyunbreakablename");
        ElhDiagram diagram = screenDiagram(top);
        double usual = diagram.findNode(top.findChildnode("Person").findChildnode("c")).bounds.width;
        assertTrue(diagram.findNode(a).bounds.width > usual, "wider than " + usual);
    }

    @Test
    void theDiagramIsBigEnoughForEveryBox()
    {
        ElhDiagram diagram = screenDiagram(makeTree());
        ElhDrawNode deepest = diagram.top.getChildren().getFirst().getChildren().get(1).getChildren().getLast();
        assertTrue(diagram.getWidth() > deepest.bounds.x + deepest.bounds.width);
        assertTrue(diagram.getHeight() > deepest.bounds.y + deepest.bounds.height);
    }

    @Test
    void clickingABoxFindsItsNode()
    {
        Elh top = makeTree();
        ElhDiagram diagram = screenDiagram(top);
        ElhNode person = top.findChildnode("Person");
        ElhDrawNode box = diagram.findNode(person);
        assertNotNull(box);
        ElhDrawNode hit = diagram.nodeAt(box.bounds.x + box.bounds.width / 2, box.bounds.y + box.bounds.height / 2);
        assertSame(person, hit.source);
        assertNull(diagram.nodeAt(1, 1), "the top-left corner is empty");
    }

    @Test
    void onlyTheFirstLevelsAreDrawn()
    {
        Elh top = new Elh("top");
        ElhNode node = top;
        for (int depth = 1; depth <= ElhDiagram.MAXDEPTH + 2; depth++)
        {
            Event child = new Event("level " + depth, node);
            node.addChild(child);
            node = child;
        }
        ElhDiagram diagram = screenDiagram(top);
        assertNotNull(diagram.findNode(top.findActiveEvent(pathTo(ElhDiagram.MAXDEPTH))));
        assertNull(diagram.findNode(top.findActiveEvent(pathTo(ElhDiagram.MAXDEPTH + 1))));
    }

    private static String pathTo(int depth)
    {
        StringBuilder path = new StringBuilder("top");
        for (int d = 1; d <= depth; d++) path.append("/level ").append(d);
        return path.toString();
    }

    @Test
    void exportWritesAnSvgFileInTheStylesItIsGiven() throws IOException
    {
        Path file = tempdir.resolve("out.svg");
        SvgExport.write(makeTree(), StyleMaps.getPrintStyles(), LayoutStyle.JACKSON, file);
        String svg = Files.readString(file);
        assertTrue(svg.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(svg.contains(">Person<"), "the box labels are in the file");
        assertTrue(svg.contains(">b2<"), "long names are split over lines");
        // the print style draws boxes with black borders 4 wide
        assertTrue(svg.contains("stroke-width:4") && svg.contains("stroke:black"));
    }
}
