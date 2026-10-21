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
    private Elh makeTree()
    {
        Elh top = new Elh("top");
        Entity person = new Entity("Person", top);
        top.addChild(person);
        for (String name : new String[]{"a", "b", "c"})
        {
            person.addChild(new Event(name, person));
        }
        return top;
    }

    private Elh saveAndLoad(Elh top) throws IOException
    {
        Path file = tempdir.resolve("test.elh");
        top.makeElh(file.toString());
        Elh loaded = new Elh("loaded");
        loaded.importELH(file.toString());
        return loaded;
    }

    @Test
    void saveAndLoadKeepsTheTree() throws IOException
    {
        Elh loaded = saveAndLoad(makeTree());
        assertEquals("top", loaded.getName());
        ElhNode person = loaded.findChildnode("Person");
        assertInstanceOf(Entity.class, person);
        assertEquals(3, person.countChildren());
        assertEquals("c", person.getChildren().get(2).getName());
    }

    @Test
    void saveWritesTheTreeWithoutAnExtraWrapper() throws IOException
    {
        Path file = tempdir.resolve("test.elh");
        makeTree().makeElh(file.toString());
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
        Elh loaded = new Elh("loaded");
        loaded.importELH(file.toString());
        assertEquals("Library", loaded.getName());
        assertInstanceOf(Entity.class, loaded.findChildnode("Person"));
    }

    @Test
    void openingAFileThatIsNotXmlFails() throws IOException
    {
        Path file = tempdir.resolve("broken.elh");
        Files.writeString(file, "<elh name=\"unfinished\"");
        assertThrows(IOException.class, () -> new Elh("loaded").importELH(file.toString()));
    }

    @Test
    void anElhInsideAnElhSurvivesSaving() throws IOException
    {
        Elh top = new Elh("outer");
        top.setChildgrouptype(ChildGrouping.SEQUENCE);
        top.setStylename("elh");
        Elh inner = new Elh("new elh", top);
        inner.setChildgrouptype(ChildGrouping.SEQUENCE);
        inner.setStylename("elh");
        top.addChild(inner);
        Elh loaded = saveAndLoad(top);
        assertEquals("outer", loaded.getName());
        assertInstanceOf(Elh.class, loaded.findChildnode("new elh"));
    }

    @Test
    void namesWithXmlCharactersSurviveSaving() throws IOException
    {
        Elh top = makeTree();
        String awkward = "Tom & \"Jerry\" <cat>";
        top.findChildnode("Person").setName(awkward);
        Elh loaded = saveAndLoad(top);
        assertNotNull(loaded.findChildnode(awkward));
    }

    @Test
    void entitygroupsSurviveSaving() throws IOException
    {
        Elh top = makeTree();
        top.addChild(new EntityGroup("group", top));
        Elh loaded = saveAndLoad(top);
        assertInstanceOf(EntityGroup.class, loaded.findChildnode("group"));
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
        Elh top = makeTree();
        top.findChildnode("Person").moveup();
        assertEquals(1, top.countChildren());
    }

    @Test
    void addSiblingAddsToTheSameParent()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.findChildnode("a").addSibling();
        assertEquals(4, person.countChildren());
        assertSame(person, person.getChildren().get(3).getParent());
    }

    @Test
    void moveChildrenFromReparentsTheChildren()
    {
        Elh top = makeTree();
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
        person.insertabovechildren();
        assertEquals(1, person.countChildren());
        ElhNode newevent = person.getChildren().getFirst();
        assertInstanceOf(Event.class, newevent);
        assertSame(person, newevent.getParent());
        assertEquals(3, newevent.countChildren());
        assertSame(newevent, newevent.findChildnode("a").getParent());
    }

    @Test
    void duplicateAddsACopyOfTheChild()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.findChildnode("b").addChild(new Event("b1", person.findChildnode("b")));
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
        Elh top = makeTree();
        assertDoesNotThrow(top::moveleft);
        assertDoesNotThrow(top::moveright);
        assertDoesNotThrow(top::moveup);
    }

    @Test
    void copyKeepsTypeAndGivesChildrenTheCopyAsParent()
    {
        Elh top = makeTree();
        Role arole = new Role("r", top);
        arole.setStylename("special");
        arole.addChild(new Event("e", arole));
        ElhNode copy = arole.copy(top);
        assertInstanceOf(Role.class, copy);
        assertEquals("special", copy.getStylename());
        assertSame(top, copy.getParent());
        assertSame(copy, copy.getChildren().getFirst().getParent());
    }
}
