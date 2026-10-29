package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectTest
{
    @TempDir
    Path elhfolder;

    @Test
    void aNewProjectIsAFolderWithAProjectFile() throws IOException
    {
        Project.create(elhfolder.toFile(), "Library", "Paul");
        Project loaded = Project.load(elhfolder.resolve("Library").toFile());
        assertEquals("Library", loaded.getName());
        assertEquals("Paul", loaded.getAuthor());
    }

    @Test
    void theEventCatalogueIsSavedWithTheProject() throws IOException
    {
        Project project = Project.create(elhfolder.toFile(), "Library", "Paul");
        ProjectEvent borrow = project.addEvent("Borrow");
        borrow.setDescription("A member takes a book away");
        project.addEvent("Return");
        project.save();
        Project loaded = Project.load(elhfolder.resolve("Library").toFile());
        assertEquals(2, loaded.getEvents().size());
        assertEquals("Borrow", loaded.findEvent(borrow.getId()).getName());
        assertEquals("A member takes a book away", loaded.findEventByName("borrow").getDescription());
    }

    @Test
    void anOlderEventDescriptionBecomesItsAction() throws IOException
    {
        Path folder = elhfolder.resolve("Old");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve(Project.PROJECTFILE), """
                <project><name>Old</name><author>Paul</author>
                <events nextid="2"><event id="e1" name="Borrow"><description>takes a book</description></event></events>
                </project>
                """);
        assertEquals("takes a book", Project.load(folder.toFile()).findEvent("e1").getDescription());
    }

    @Test
    void addingAnEventTwiceGivesTheSameEvent() throws IOException
    {
        Project project = Project.create(elhfolder.toFile(), "Library", "Paul");
        assertSame(project.addEvent("Borrow"), project.addEvent("BORROW"));
        assertEquals(1, project.getEvents().size());
    }

    @Test
    void theIdOfADeletedEventIsNeverReused() throws IOException
    {
        Project project = Project.create(elhfolder.toFile(), "Library", "Paul");
        project.addEvent("Borrow");
        ProjectEvent returned = project.addEvent("Return");
        project.removeEvent(returned);
        project.save();
        Project loaded = Project.load(elhfolder.resolve("Library").toFile());
        assertNotEquals(returned.getId(), loaded.addEvent("Renew").getId());
    }

    @Test
    void cannotCreateAProjectOverAnExistingFolder() throws IOException
    {
        Project.create(elhfolder.toFile(), "Library", "Paul");
        assertThrows(IOException.class, () -> Project.create(elhfolder.toFile(), "Library", "Paul"));
    }

    @Test
    void findsOnlyFoldersHoldingAProjectFile() throws IOException
    {
        Project.create(elhfolder.toFile(), "Library", "Paul");
        assertTrue(elhfolder.resolve("notaproject").toFile().mkdir());
        List<File> found = Project.findProjects(elhfolder.toFile());
        assertEquals(List.of(elhfolder.resolve("Library").toFile()), found);
    }
}
