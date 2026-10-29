package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ElhVersionsTest
{
    @TempDir
    Path folder;

    private void touch(String name) throws IOException
    {
        Files.writeString(folder.resolve(name), "<elh/>");
    }

    @Test
    void splitsVersionedNames()
    {
        File file = new File("library_v12.elh");
        assertEquals("library", ElhVersions.baseName(file));
        assertEquals(12, ElhVersions.version(file));
    }

    @Test
    void otherNamesAreVersionZeroOfTheirName()
    {
        File file = new File("library25b.xml");
        assertEquals("library25b", ElhVersions.baseName(file));
        assertEquals(0, ElhVersions.version(file));
        assertFalse(ElhVersions.isVersioned(file));
    }

    @Test
    void picksTheHighestVersionOfEachElh() throws IOException
    {
        touch("library_v2.elh");
        touch("library_v10.elh");
        touch("library_v9.elh");
        touch("roles.xml");
        touch(Project.PROJECTFILE);
        Map<String, File> latest = ElhVersions.latestVersions(folder.toFile());
        assertEquals(2, latest.size());
        assertEquals("library_v10.elh", latest.get("library").getName());
        assertEquals("roles.xml", latest.get("roles").getName());
        assertEquals(3, ElhVersions.countVersions(folder.toFile(), "library"));
    }

    @Test
    void theNextVersionFollowsTheHighest() throws IOException
    {
        assertEquals("library_v1.elh", ElhVersions.nextVersion(folder.toFile(), "library").getName());
        touch("library_v3.elh");
        assertEquals("library_v4.elh", ElhVersions.nextVersion(folder.toFile(), "library").getName());
        // an unversioned file becomes version 1 when it is first saved as a version
        touch("roles.xml");
        assertEquals("roles_v1.elh", ElhVersions.nextVersion(folder.toFile(), "roles").getName());
    }

    @Test
    void structureChangesWithNodesButNotNames()
    {
        ElhNode elh = new ElhNode(NodeType.ELH, "library", null);
        elh.addChild(new ElhNode(NodeType.ENTITY, "Member", elh));
        String before = elh.structure();
        elh.getChildren().getFirst().setName("Borrower");
        assertEquals(before, elh.structure());
        elh.getChildren().getFirst().addChild("action");
        assertNotEquals(before, elh.structure());
    }
}
