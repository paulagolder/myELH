package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
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

    @Test
    void elhsFromElsewhereAreSavedInTheProjectFolder() throws IOException
    {
        Project project = Project.create(elhfolder.toFile(), "Library", "Paul");
        File saved = project.fileFor(new File("/somewhere/else/book.xml"));
        assertEquals(elhfolder.resolve("Library").resolve("book.xml").toFile(), saved);
    }
}
