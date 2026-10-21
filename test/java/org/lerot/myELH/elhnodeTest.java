package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

// Tests of the ELH tree model and its file format; none of them open a window.
class elhnodeTest
{
    @TempDir
    Path tempdir;

    // an elh with one entity holding events a, b and c
    private elh makeTree()
    {
        elh top = new elh("top");
        entity person = new entity("Person", top);
        top.addChild(person);
        for (String name : new String[]{"a", "b", "c"})
        {
            person.addChild(new event(name, person));
        }
        return top;
    }

    private elh saveAndLoad(elh top) throws IOException
    {
        Path file = tempdir.resolve("test.elh");
        top.makeElh(file.toString());
        elh loaded = new elh("loaded");
        loaded.importELH(file.toString());
        return loaded;
    }

    @Test
    void saveAndLoadKeepsTheTree() throws IOException
    {
        elh loaded = saveAndLoad(makeTree());
        assertEquals("top", loaded.getName());
        elhnode person = loaded.findChildnode("Person");
        assertInstanceOf(entity.class, person);
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
        elh loaded = new elh("loaded");
        loaded.importELH(file.toString());
        assertEquals("Library", loaded.getName());
        assertInstanceOf(entity.class, loaded.findChildnode("Person"));
    }

    @Test
    void openingAFileThatIsNotXmlFails() throws IOException
    {
        Path file = tempdir.resolve("broken.elh");
        Files.writeString(file, "<elh name=\"unfinished\"");
        assertThrows(IOException.class, () -> new elh("loaded").importELH(file.toString()));
    }

    @Test
    void anElhInsideAnElhSurvivesSaving() throws IOException
    {
        elh top = new elh("outer");
        top.setChildgrouptype(ChildGrouping.SEQUENCE);
        top.setStylename("elh");
        elh inner = new elh("new elh", top);
        inner.setChildgrouptype(ChildGrouping.SEQUENCE);
        inner.setStylename("elh");
        top.addChild(inner);
        elh loaded = saveAndLoad(top);
        assertEquals("outer", loaded.getName());
        assertInstanceOf(elh.class, loaded.findChildnode("new elh"));
    }

    @Test
    void namesWithXmlCharactersSurviveSaving() throws IOException
    {
        elh top = makeTree();
        String awkward = "Tom & \"Jerry\" <cat>";
        top.findChildnode("Person").setName(awkward);
        elh loaded = saveAndLoad(top);
        assertNotNull(loaded.findChildnode(awkward));
    }

    @Test
    void entitygroupsSurviveSaving() throws IOException
    {
        elh top = makeTree();
        top.addChild(new entitygroup("group", top));
        elh loaded = saveAndLoad(top);
        assertInstanceOf(entitygroup.class, loaded.findChildnode("group"));
    }

    @Test
    void moverightOnTheLastChildDoesNothing()
    {
        elhnode person = makeTree().findChildnode("Person");
        person.findChildnode("c").moveright();
        assertEquals("c", person.getChildren().get(2).getName());
    }

    @Test
    void moverightSwapsWithTheNextChild()
    {
        elhnode person = makeTree().findChildnode("Person");
        person.findChildnode("a").moveright();
        assertEquals("b", person.getChildren().get(0).getName());
        assertEquals("a", person.getChildren().get(1).getName());
    }

    @Test
    void moveupWithNoGrandparentDoesNothing()
    {
        elh top = makeTree();
        top.findChildnode("Person").moveup();
        assertEquals(1, top.countChildren());
    }

    @Test
    void addSiblingAddsToTheSameParent()
    {
        elhnode person = makeTree().findChildnode("Person");
        person.findChildnode("a").addSibling();
        assertEquals(4, person.countChildren());
        assertSame(person, person.getChildren().get(3).getParent());
    }

    @Test
    void moveChildrenFromReparentsTheChildren()
    {
        elh top = makeTree();
        elhnode person = top.findChildnode("Person");
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
        elhnode person = makeTree().findChildnode("Person");
        person.deleteChildren();
        assertEquals(0, person.countChildren());
        assertDoesNotThrow(person::xmlout);
    }

    @Test
    void insertAboveChildrenPutsANewEventBetween()
    {
        elhnode person = makeTree().findChildnode("Person");
        person.insertabovechildren();
        assertEquals(1, person.countChildren());
        elhnode newevent = person.getChildren().getFirst();
        assertInstanceOf(event.class, newevent);
        assertSame(person, newevent.getParent());
        assertEquals(3, newevent.countChildren());
        assertSame(newevent, newevent.findChildnode("a").getParent());
    }

    @Test
    void duplicateAddsACopyOfTheChild()
    {
        elhnode person = makeTree().findChildnode("Person");
        person.findChildnode("b").addChild(new event("b1", person.findChildnode("b")));
        person.duplicate("b");
        assertEquals(4, person.countChildren());
        elhnode copy = person.findChildnode("Copyof:b");
        assertNotSame(person.findChildnode("b"), copy);
        assertSame(person, copy.getParent());
        assertEquals("b1", copy.getChildren().getFirst().getName());
    }

    @Test
    void movingTheTopNodeDoesNothing()
    {
        elh top = makeTree();
        assertDoesNotThrow(top::moveleft);
        assertDoesNotThrow(top::moveright);
        assertDoesNotThrow(top::moveup);
    }

    @Test
    void copyKeepsTypeAndGivesChildrenTheCopyAsParent()
    {
        elh top = makeTree();
        role arole = new role("r", top);
        arole.setStylename("special");
        arole.addChild(new event("e", arole));
        elhnode copy = arole.copy(top);
        assertInstanceOf(role.class, copy);
        assertEquals("special", copy.getStylename());
        assertSame(top, copy.getParent());
        assertSame(copy, copy.getChildren().getFirst().getParent());
    }
}
