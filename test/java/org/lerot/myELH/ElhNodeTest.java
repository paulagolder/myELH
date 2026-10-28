package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Tests of the ELH tree model and its file format; none of them open a window.
class ElhNodeTest
{
    @TempDir
    Path tempdir;

    // an elh with one entity holding events a, b and c
    private ElhNode makeTree()
    {
        ElhNode top = new ElhNode(NodeType.ELH, "top", null);
        ElhNode person = new ElhNode(NodeType.ENTITY, "Person", top);
        top.addChild(person);
        for (String name : new String[]{"a", "b", "c"})
        {
            person.addChild(new ElhNode(NodeType.EVENT, name, person));
        }
        return top;
    }

    private ElhNode saveAndLoad(ElhNode top) throws IOException
    {
        Path file = tempdir.resolve("test.elh");
        ElhFile.write(top, file);
        return ElhFile.read(file);
    }

    @Test
    void saveAndLoadKeepsTheTree() throws IOException
    {
        ElhNode loaded = saveAndLoad(makeTree());
        assertEquals("top", loaded.getName());
        ElhNode person = loaded.findChildnode("Person");
        assertEquals(NodeType.ENTITY, person.getNodeType());
        assertEquals(3, person.countChildren());
        assertEquals("c", person.getChildren().get(2).getName());
    }

    @Test
    void saveWritesTheTreeWithoutAnExtraWrapper() throws IOException
    {
        Path file = tempdir.resolve("test.elh");
        ElhFile.write(makeTree(), file);
        String xml = Files.readString(file);
        assertFalse(xml.contains("new elh"));
        assertEquals(1, xml.split("<elh ").length - 1);
    }

    @Test
    void oldFilesWithTheExtraWrapperStillOpen() throws IOException
    {
        Path file = tempdir.resolve("old.elh");
        Files.writeString(file, """
                <?xml version="1.0" encoding="UTF-8"?>
                <elh name="new elh" >
                <elh name="Library" childgrouptype="sequence" stylename="elh" >
                <entity name="Person" childgrouptype="sequence" stylename="entity" />
                </elh>
                </elh>
                """);
        ElhNode loaded = ElhFile.read(file);
        assertEquals("Library", loaded.getName());
        assertEquals(NodeType.ENTITY, loaded.findChildnode("Person").getNodeType());
    }

    @Test
    void openingAFileThatIsNotXmlFails() throws IOException
    {
        Path file = tempdir.resolve("broken.elh");
        Files.writeString(file, "<elh name=\"unfinished\"");
        assertThrows(IOException.class, () -> ElhFile.read(file));
    }

    @Test
    void anElhInsideAnElhSurvivesSaving() throws IOException
    {
        ElhNode top = new ElhNode(NodeType.ELH, "outer", null);
        top.setChildgrouptype(ChildGrouping.SEQUENCE);
        top.setStylename("elh");
        ElhNode inner = new ElhNode(NodeType.ELH, "new elh", top);
        inner.setChildgrouptype(ChildGrouping.SEQUENCE);
        inner.setStylename("elh");
        top.addChild(inner);
        ElhNode loaded = saveAndLoad(top);
        assertEquals("outer", loaded.getName());
        assertEquals(NodeType.ELH, loaded.findChildnode("new elh").getNodeType());
    }

    @Test
    void namesWithXmlCharactersSurviveSaving() throws IOException
    {
        ElhNode top = makeTree();
        String awkward = "Tom & \"Jerry\" <cat>";
        top.findChildnode("Person").setName(awkward);
        ElhNode loaded = saveAndLoad(top);
        assertNotNull(loaded.findChildnode(awkward));
    }

    @Test
    void entitygroupsSurviveSaving() throws IOException
    {
        ElhNode top = makeTree();
        top.addChild(new ElhNode(NodeType.ENTITYGROUP, "group", top));
        ElhNode loaded = saveAndLoad(top);
        assertEquals(NodeType.ENTITYGROUP, loaded.findChildnode("group").getNodeType());
    }

    @Test
    void olderFilesWithAnEntitiesTopStillOpenAsAnElh() throws IOException
    {
        Path file = tempdir.resolve("newpaper.xml");
        Files.writeString(file, """
                <entities name="new elh">
                <entity name="Library Member" type="Entity" stylename="" />
                </entities>
                """);
        ElhNode loaded = ElhFile.read(file);
        assertEquals(NodeType.ELH, loaded.getNodeType());
        assertNotNull(loaded.findChildnode("Library Member"));
    }

    @Test
    void savingEditsCanChangeTheType()
    {
        ElhNode b = makeTree().findChildnode("Person").findChildnode("b");
        b.setStylename("event");
        b.applyEdits("Membership", NodeType.ROLEGROUP, b.getChildgrouptype(), "event");
        assertEquals(NodeType.ROLEGROUP, b.getNodeType());
        assertEquals("Membership", b.getName());
        // grouping and style left as they were follow the new type
        assertEquals(ChildGrouping.ROLEGROUP, b.getChildgrouptype());
        assertEquals("rolegroup", b.getStylename());
    }

    @Test
    void groupingAndStyleChosenWithANewTypeAreKept()
    {
        ElhNode b = makeTree().findChildnode("Person").findChildnode("b");
        b.setStylename("event");
        b.applyEdits("b", NodeType.ROLE, ChildGrouping.OPTION, "special");
        assertEquals(NodeType.ROLE, b.getNodeType());
        assertEquals(ChildGrouping.OPTION, b.getChildgrouptype());
        assertEquals("special", b.getStylename());
    }

    @Test
    void moverightOnTheLastChildDoesNothing()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.findChildnode("c").moveright();
        assertEquals("c", person.getChildren().get(2).getName());
    }

    @Test
    void moverightSwapsWithTheNextChild()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.findChildnode("a").moveright();
        assertEquals("b", person.getChildren().get(0).getName());
        assertEquals("a", person.getChildren().get(1).getName());
    }

    @Test
    void moveupWithNoGrandparentDoesNothing()
    {
        ElhNode top = makeTree();
        top.findChildnode("Person").moveup();
        assertEquals(1, top.countChildren());
    }

    @Test
    void addSiblingAddsToTheSameParent()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.findChildnode("a").addSibling("Renews");
        assertEquals(4, person.countChildren());
        assertSame(person, person.getChildren().get(3).getParent());
        assertEquals("Renews", person.getChildren().get(3).getName());
    }

    @Test
    void addChildUsesTheGivenName()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.addChild("role", "Borrower");
        ElhNode added = person.getChildren().getLast();
        assertEquals("Borrower", added.getName());
        assertEquals("role", added.getType());
    }

    @Test
    void moveChildrenFromReparentsTheChildren()
    {
        ElhNode top = makeTree();
        ElhNode person = top.findChildnode("Person");
        top.moveChildrenFrom(person);
        top.removeChild(person);
        assertEquals(3, top.countChildren());
        assertSame(top, top.findChildnode("a").getParent());
        assertEquals(0, person.countChildren());
        assertDoesNotThrow(person::xmlout);
    }

    @Test
    void deletedChildrenLeaveAWritableNode()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.deleteChildren();
        assertEquals(0, person.countChildren());
        assertDoesNotThrow(person::xmlout);
    }

    @Test
    void insertAboveChildrenPutsANewEventBetween()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.insertabovechildren("Lifetime");
        assertEquals(1, person.countChildren());
        ElhNode newevent = person.getChildren().getFirst();
        assertEquals(NodeType.EVENT, newevent.getNodeType());
        assertEquals("Lifetime", newevent.getName());
        assertSame(person, newevent.getParent());
        assertEquals(3, newevent.countChildren());
        assertSame(newevent, newevent.findChildnode("a").getParent());
    }

    @Test
    void duplicateAddsACopyOfTheChild()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.findChildnode("b").addChild(new ElhNode(NodeType.EVENT, "b1", person.findChildnode("b")));
        person.duplicate("b");
        assertEquals(4, person.countChildren());
        ElhNode copy = person.findChildnode("Copyof:b");
        assertNotSame(person.findChildnode("b"), copy);
        assertSame(person, copy.getParent());
        assertEquals("b1", copy.getChildren().getFirst().getName());
    }

    @Test
    void movingTheTopNodeDoesNothing()
    {
        ElhNode top = makeTree();
        assertDoesNotThrow(top::moveleft);
        assertDoesNotThrow(top::moveright);
        assertDoesNotThrow(top::moveup);
    }

    @Test
    void copyKeepsTypeAndGivesChildrenTheCopyAsParent()
    {
        ElhNode top = makeTree();
        ElhNode arole = new ElhNode(NodeType.ROLE, "r", top);
        arole.setStylename("special");
        arole.addChild(new ElhNode(NodeType.EVENT, "e", arole));
        ElhNode copy = arole.copy(top);
        assertEquals(NodeType.ROLE, copy.getNodeType());
        assertEquals("special", copy.getStylename());
        assertSame(top, copy.getParent());
        assertSame(copy, copy.getChildren().getFirst().getParent());
    }
}
