package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jdom2.Element;

import static org.junit.jupiter.api.Assertions.*;

// Tests of a project's entities and events: where they are kept, when they
// are read, and how an older project is converted.
class ProjectEntitiesTest
{
    @TempDir
    Path elhfolder;

    private Project newProject() throws IOException
    {
        return Project.create(elhfolder.toFile(), "Library", "Paul");
    }

    private Project reload(Project project) throws IOException
    {
        return Project.load(project.getFolder());
    }

    private Path folder(Project project)
    {
        return project.getFolder().toPath();
    }

    @Test
    void aNewEntityIsKeptInlineInProjectXml() throws IOException
    {
        Project project = newProject();
        ProjectEntity person = project.addEntity("Person");
        person.getTop().addChild("action", "Joins");
        project.saveEntity(person);
        String xml = Files.readString(folder(project).resolve(Project.PROJECTFILE));
        assertTrue(xml.contains("<entity name=\"Person\" />"), "listed without a file");
        assertTrue(xml.contains("<myelh>"), "its life history is in the inline section");
        Project loaded = reload(project);
        ProjectEntity again = loaded.findEntity("Person");
        assertTrue(again.isInline());
        assertFalse(again.isLoaded(), "not read until needed");
        assertEquals("Joins", loaded.loadEntity(again).getChildren().getFirst().getName());
    }

    @Test
    void unsavedEditsToAnInlineEntityAreNotWrittenWithOtherChanges() throws IOException
    {
        Project project = newProject();
        ProjectEntity person = project.addEntity("Person");
        person.getTop().addChild("action", "Joins");
        project.addEvent("Borrow");
        project.save();
        assertEquals(0, reload(project).loadEntity(reload(project).findEntity("Person")).countChildren());
    }

    @Test
    void anEntityMovedToItsOwnFileIsWrappedInMyelh() throws IOException
    {
        Project project = newProject();
        ProjectEntity person = project.addEntity("Person");
        person.getTop().addChild("action", "Joins");
        File file = project.moveEntityToFile(person);
        assertEquals("Person_v1.elh", file.getName());
        Element root = MyelhFile.readRoot(file.toPath());
        assertEquals("myelh", root.getName());
        assertEquals(1, root.getChildren().size());
        assertEquals("Person", root.getChildren().getFirst().getAttributeValue("name"));
        Project loaded = reload(project);
        ProjectEntity again = loaded.findEntity("Person");
        assertEquals("Person", again.getFile());
        assertFalse(again.isLoaded());
        assertEquals("Joins", loaded.loadEntity(again).getChildren().getFirst().getName());
    }

    @Test
    void savingAFileEntityMakesANewVersionOnlyWhenItsShapeChanges() throws IOException
    {
        Project project = newProject();
        ProjectEntity person = project.addEntity("Person");
        project.moveEntityToFile(person);
        person.getTop().setName("Human");
        assertEquals("Person_v1.elh", project.saveEntity(person).getName(), "a rename overwrites");
        person.getTop().addChild("action", "Joins");
        assertEquals("Person_v2.elh", project.saveEntity(person).getName(), "a new node makes a new version");
        assertEquals("Person_v3.elh", project.saveEntityAsNewVersion(person).getName());
        assertEquals("Human", reload(project).getEntities().getFirst().getName());
    }

    @Test
    void anEntityMovedInlineLeavesItsFilesBehind() throws IOException
    {
        Project project = newProject();
        ProjectEntity person = project.addEntity("Person");
        File file = project.moveEntityToFile(person);
        project.moveEntityInline(person);
        assertTrue(file.exists());
        ProjectEntity again = reload(project).findEntity("Person");
        assertTrue(again.isInline());
    }

    @Test
    void anEntityWhoseFileIsMissingCannotBeLoaded() throws IOException
    {
        Project project = newProject();
        File file = project.moveEntityToFile(project.addEntity("Person"));
        Files.delete(file.toPath());
        Project loaded = reload(project);
        assertThrows(IOException.class, () -> loaded.loadEntity(loaded.findEntity("Person")));
    }

    @Test
    void twoEntitiesCannotShareAName() throws IOException
    {
        Project project = newProject();
        project.addEntity("Person");
        assertThrows(IOException.class, () -> project.addEntity("PERSON"));
    }

    @Test
    void anEventMovedToItsOwnFileIsReadWhenNeeded() throws IOException
    {
        Project project = newProject();
        ProjectEvent borrow = project.addEvent("Borrow");
        borrow.setDescription("A member takes a book away");
        project.save();
        File file = project.moveEventToFile(borrow);
        assertEquals("Borrow_v1.event", file.getName());
        assertEquals("myelh", MyelhFile.readRoot(file.toPath()).getName());
        Project loaded = reload(project);
        ProjectEvent again = loaded.findEvent(borrow.getId());
        assertFalse(again.isLoaded());
        assertEquals("", again.getDescription(), "not read yet");
        loaded.loadEvent(again);
        assertEquals("A member takes a book away", again.getDescription());
        again.setDescription("Takes a book home");
        loaded.saveEvent(again);
        Project third = reload(project);
        ProjectEvent third_borrow = third.findEvent(borrow.getId());
        third.loadEvent(third_borrow);
        assertEquals("Takes a book home", third_borrow.getDescription());
    }

    @Test
    void inlineEventDetailsAreKeptInTheMyelhSection() throws IOException
    {
        Project project = newProject();
        project.addEvent("Borrow").setDescription("takes a book");
        project.save();
        String xml = Files.readString(folder(project).resolve(Project.PROJECTFILE));
        assertTrue(xml.contains("<myelh>") && xml.contains("<description>takes a book</description>"));
        assertEquals("takes a book", reload(project).findEventByName("Borrow").getDescription());
    }

    @Test
    void anEventDescriptionSavedAsAnActionIsStillRead() throws IOException
    {
        Path folder = elhfolder.resolve("Mid");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(Project.PROJECTFILE), """
                <project><name>Mid</name><author>Paul</author><entities />
                <events nextid="3"><event id="e1" name="Borrow" /><event id="e2" name="Return" file="Return" /></events>
                <myelh><event id="e1" name="Borrow"><action>takes a book</action></event></myelh>
                </project>
                """);
        Files.writeString(folder.resolve("Return_v1.event"),
                "<myelh><event id=\"e2\" name=\"Return\"><action>brings it back</action></event></myelh>");
        Project project = Project.load(folder.toFile());
        assertEquals("takes a book", project.findEvent("e1").getDescription());
        ProjectEvent returned = project.findEvent("e2");
        project.loadEvent(returned);
        assertEquals("brings it back", returned.getDescription());
    }

    @Test
    void actionsAreFoundInEveryEntityAndFlaggedUntilPartOfAnEvent() throws IOException
    {
        Project project = newProject();
        ProjectEvent borrow = project.addEvent("Borrow");
        ProjectEvent lost = project.addEvent("Lost");
        ProjectEntity member = project.addEntity("Member");
        member.getTop().addChild("action", "Borrows").setEventref(borrow.getId());
        member.getTop().addChild("action", "Joins");
        project.saveEntity(member);
        ProjectEntity book = project.addEntity("Book");
        book.getTop().addChild("action", "Is borrowed").setEventref(borrow.getId());
        // linked to an event that is then deleted
        book.getTop().addChild("action", "Goes missing").setEventref(lost.getId());
        project.saveEntity(book);
        project.removeEvent(lost);
        project.save();
        Project loaded = reload(project);
        List<EntityAction> all = loaded.allActions();
        assertEquals(4, all.size());
        assertTrue(loaded.getEntities().stream().noneMatch(ProjectEntity::isLoaded), "read without opening");
        ProjectEvent again = loaded.findEvent(borrow.getId());
        assertEquals(List.of("Member: Borrows", "Book: Is borrowed"),
                loaded.actionsOf(again, all).stream().map(EntityAction::toString).toList());
        assertFalse(loaded.isLonely(again, all), "Borrow triggers two actions");
        assertEquals(List.of("Member: Joins", "Book: Goes missing"),
                loaded.unlinkedActions(all).stream().map(EntityAction::toString).toList());
    }

    @Test
    void anEventWithFewerThanTwoActionsIsLonely() throws IOException
    {
        Project project = newProject();
        ProjectEvent joins = project.addEvent("Joins");
        ProjectEntity member = project.addEntity("Member");
        member.getTop().addChild("action", "Joins").setEventref(joins.getId());
        project.saveEntity(member);
        assertTrue(project.isLonely(joins, project.allActions()));
        assertTrue(project.isLonely(project.addEvent("Unused"), project.allActions()));
    }

    // an old project: a project.xml with no entity list, and ELH files
    private Path oldProject() throws IOException
    {
        Path folder = elhfolder.resolve("Old");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(Project.PROJECTFILE), """
                <project><name>Old</name><author>Paul</author>
                <events nextid="2"><event id="e1" name="Joins"><action>signs up</action></event></events>
                </project>
                """);
        Files.writeString(folder.resolve("library_v1.elh"), """
                <elh name="library"><entity name="Person"><event name="Was" /></entity></elh>
                """);
        Files.writeString(folder.resolve("library_v2.elh"), """
                <elh name="library" childgrouptype="sequence" stylename="elh">
                  <entity name="Person" childgrouptype="sequence" stylename="entity">
                    <event name="Joins" eventref="e1" />
                  </entity>
                  <entity name="Book" childgrouptype="sequence" stylename="entity" />
                </elh>
                """);
        Files.writeString(folder.resolve("empty_v1.elh"), "<elh name=\"empty\" />");
        return folder;
    }

    @Test
    void anOldProjectIsConvertedWhenOpened() throws IOException
    {
        Path folder = oldProject();
        String oldv2 = Files.readString(folder.resolve("library_v2.elh"));
        Project project = Project.load(folder.toFile());
        assertEquals(3, project.getEntities().size(), "the entities of each ELH's latest version");
        ProjectEntity person = project.findEntity("Person");
        assertEquals("Person", person.getFile());
        assertTrue(MyelhFile.isMyelh(folder.resolve("Person_v1.elh").toFile()));
        assertEquals("Joins", project.loadEntity(person).getChildren().getFirst().getName(), "from the latest version");
        assertEquals("e1", person.getTop().getChildren().getFirst().getEventref());
        assertEquals(oldv2, Files.readString(folder.resolve("library_v2.elh")), "old files are left alone");
        assertTrue(Files.exists(folder.resolve("project.xml.bak")));
        assertNotNull(project.findEntity("empty"), "an empty ELH becomes an empty entity of its name");
        assertEquals("signs up", project.findEvent("e1").getDescription(), "events are kept");
    }

    @Test
    void aConvertedProjectIsNotConvertedAgain() throws IOException
    {
        Path folder = oldProject();
        Project.load(folder.toFile());
        Project again = Project.load(folder.toFile());
        assertTrue(again.getConversionNotes().isEmpty());
        assertEquals(3, again.getEntities().size());
        assertFalse(Files.exists(folder.resolve("Person_v2.elh")));
    }

    @Test
    void anOldElhOfAnEntitysNameIsNumberedPastAndNotOverwritten() throws IOException
    {
        Path folder = oldProject();
        // an old ELH that happens to be called person
        Files.writeString(folder.resolve("person_v1.elh"), "<elh name=\"person\" />");
        Project project = Project.load(folder.toFile());
        ProjectEntity person = project.findEntity("Person");
        assertEquals("<elh name=\"person\" />", Files.readString(folder.resolve("person_v1.elh")));
        assertEquals("Person_v2.elh", project.entityVersions(person).getLast().getName());
        assertEquals(1, project.entityVersions(person).size());
        assertEquals("Joins", project.loadEntity(person).getChildren().getFirst().getName());
    }

    @Test
    void anOldElhFileIsNeverTakenForOneOfAnEntitysVersions() throws IOException
    {
        Path folder = oldProject();
        Project project = Project.load(folder.toFile());
        // an old-style file appears later with the entity's file name
        Files.writeString(folder.resolve("Person_v7.elh"), "<elh name=\"Person\" />");
        ProjectEntity person = Project.load(folder.toFile()).findEntity("Person");
        assertEquals(1, project.entityVersions(person).size());
        assertEquals("Person_v1.elh", project.entityVersions(person).getLast().getName());
    }
}
