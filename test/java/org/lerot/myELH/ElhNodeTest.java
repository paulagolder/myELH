package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            person.addChild(new ElhNode(NodeType.ACTION, name, person));
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
        ElhNode inner = new ElhNode(NodeType.ELH, "new elh", top);
        inner.setChildgrouptype(ChildGrouping.SEQUENCE);
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
    void eventLinksSurviveSaving() throws IOException
    {
        ElhNode top = makeTree();
        top.findChildnode("Person").findChildnode("a").setEventref("e7");
        ElhNode loaded = saveAndLoad(top);
        assertEquals("e7", loaded.findChildnode("Person").findChildnode("a").getEventref());
        assertNull(loaded.findChildnode("Person").findChildnode("b").getEventref());
    }

    @Test
    void conditionsAndResultsSurviveSavingAndCopying() throws IOException
    {
        ElhNode top = makeTree();
        ElhNode a = top.findChildnode("Person").findChildnode("a");
        a.setCondition("fewer than 6 loans & not barred");
        a.setResult("starts Borrower");
        ElhNode loaded = saveAndLoad(top).findChildnode("Person").findChildnode("a");
        assertEquals("fewer than 6 loans & not barred", loaded.getCondition());
        assertEquals("starts Borrower", loaded.getResult());
        assertEquals("", top.findChildnode("Person").findChildnode("b").getCondition());
        assertEquals("starts Borrower", a.copy(a.getParent()).getResult());
    }

    @Test
    void onlyLeafEventsAreActions()
    {
        ElhNode top = makeTree();
        ElhNode b = top.findChildnode("Person").findChildnode("b");
        b.addChild(new ElhNode(NodeType.ACTION, "b1", b));
        List<ElhNode> actions = new ArrayList<>();
        top.collectActions(actions);
        // a and c are leaves, b now groups b1; Person is an entity
        assertEquals(List.of("a", "b1", "c"), actions.stream().map(ElhNode::getName).toList());
    }

    @Test
    void eachElhCanWordASharedEventItsOwnWay() throws IOException
    {
        Project project = Project.create(tempdir.toFile(), "Library", "Paul");
        String borrow = project.addEvent("Borrow").getId();
        ElhNode member = new ElhNode(NodeType.ELH, "member", null);
        member.addChild(NodeType.ACTION.xmlName(), "Borrows").setEventref(borrow);
        ElhNode book = new ElhNode(NodeType.ELH, "book", null);
        book.addChild(NodeType.ACTION.xmlName(), "Is borrowed").setEventref(borrow);
        java.util.Map<String, java.util.List<String>> found = new java.util.HashMap<>();
        member.collectEventrefs(found);
        book.collectEventrefs(found);
        assertEquals(java.util.List.of("Borrows", "Is borrowed"), found.get(borrow));
    }

    @Test
    void anIndexPathFindsTheSameNodeInAFreshCopy() throws IOException
    {
        ElhNode top = makeTree();
        ElhNode c = top.findChildnode("Person").findChildnode("c");
        ElhNode loaded = saveAndLoad(top);
        ElhNode found = loaded.nodeAtIndexPath(c.indexPath());
        assertEquals("c", found.getName());
        assertEquals(java.util.List.of(0, 2), c.indexPath());
        assertNull(loaded.nodeAtIndexPath(java.util.List.of(0, 9)));
    }

    @Test
    void findLinkedFindsEveryNodeOfAnEvent()
    {
        ElhNode top = makeTree();
        ElhNode person = top.findChildnode("Person");
        person.findChildnode("a").setEventref("e1");
        person.findChildnode("c").setEventref("e1");
        person.findChildnode("b").setEventref("e2");
        java.util.List<ElhNode> found = new java.util.ArrayList<>();
        top.findLinked("e1", found);
        assertEquals(java.util.List.of("a", "c"), found.stream().map(ElhNode::getName).toList());
    }

    @Test
    void savingEditsCanChangeTheType()
    {
        ElhNode b = makeTree().findChildnode("Person").findChildnode("b");
        b.applyEdits("Membership", NodeType.ROLEGROUP, b.getChildgrouptype());
        assertEquals(NodeType.ROLEGROUP, b.getNodeType());
        assertEquals("Membership", b.getName());
        // a grouping left as it was follows the new type
        assertEquals(ChildGrouping.ROLEGROUP, b.getChildgrouptype());
    }

    @Test
    void noStyleNameIsSavedAndAnOldOneIsIgnored() throws IOException
    {
        Path file = tempdir.resolve("old.elh");
        Files.writeString(file, """
                <elh name="Library" childgrouptype="sequence" stylename="entities">
                  <entity name="Person" childgrouptype="sequence" stylename="special" />
                </elh>
                """);
        ElhNode top = ElhFile.read(file);
        assertFalse(top.xmlout().contains("stylename"));
    }

    @Test
    void anOldFilesEventNodesAreReadAsActionsAndSavedAsActions() throws IOException
    {
        Path file = tempdir.resolve("old.elh");
        Files.writeString(file, """
                <elh name="Library"><entity name="Person"><event name="Joins" /></entity></elh>
                """);
        ElhNode top = ElhFile.read(file);
        ElhNode joins = top.findChildnode("Person").findChildnode("Joins");
        assertEquals(NodeType.ACTION, joins.getNodeType());
        assertTrue(joins.isLeafAction());
        assertTrue(top.xmlout().contains("<action name=\"Joins\""));
        assertFalse(top.xmlout().contains("<event"));
    }

    @Test
    void anActionsDescriptionIsSavedAndCopied() throws IOException
    {
        ElhNode top = makeTree();
        ElhNode a = top.findChildnode("Person").findChildnode("a");
        a.setDescription("The member signs the form\nand pays the fee");
        ElhNode loaded = saveAndLoad(top);
        assertEquals("The member signs the form\nand pays the fee",
                loaded.findChildnode("Person").findChildnode("a").getDescription());
        ElhNode copy = a.copy(a.getParent());
        assertEquals(a.getDescription(), copy.getDescription());
    }

    @Test
    void aGroupingChosenWithANewTypeIsKept()
    {
        ElhNode b = makeTree().findChildnode("Person").findChildnode("b");
        b.applyEdits("b", NodeType.ROLE, ChildGrouping.OPTION);
        assertEquals(NodeType.ROLE, b.getNodeType());
        assertEquals(ChildGrouping.OPTION, b.getChildgrouptype());
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
        person.findChildnode("a").addSibling(NodeType.ACTION, "Renews");
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
        person.setChildgrouptype(ChildGrouping.OPTION);
        person.insertabovechildren(NodeType.ACTION, "Lifetime");
        assertEquals(1, person.countChildren());
        ElhNode newevent = person.getChildren().getFirst();
        assertEquals(NodeType.ACTION, newevent.getNodeType());
        assertEquals("Lifetime", newevent.getName());
        assertSame(person, newevent.getParent());
        assertEquals(3, newevent.countChildren());
        assertSame(newevent, newevent.findChildnode("a").getParent());
        assertEquals(ChildGrouping.OPTION, newevent.getChildgrouptype(), "the structure moves down with the children");
        assertEquals(ChildGrouping.SEQUENCE, person.getChildgrouptype());
    }

    @Test
    void duplicateAddsACopyOfTheChild()
    {
        ElhNode person = makeTree().findChildnode("Person");
        person.findChildnode("b").addChild(new ElhNode(NodeType.ACTION, "b1", person.findChildnode("b")));
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
        arole.setChildgrouptype(ChildGrouping.OPTION);
        arole.addChild(new ElhNode(NodeType.ACTION, "e", arole));
        ElhNode copy = arole.copy(top);
        assertEquals(NodeType.ROLE, copy.getNodeType());
        assertEquals(ChildGrouping.OPTION, copy.getChildgrouptype());
        assertSame(top, copy.getParent());
        assertSame(copy, copy.getChildren().getFirst().getParent());
    }
}
