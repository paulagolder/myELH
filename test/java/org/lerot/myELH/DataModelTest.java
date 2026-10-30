package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataModelTest
{
    @TempDir
    Path elhfolder;

    private static Attribute text(String name)
    {
        return new Attribute(name, Attribute.Type.TEXT, null, false, null);
    }

    // Person with name, age and address; its roles Library Member (with a
    // membership number) and CounterStaff
    private static ElhNode person()
    {
        ElhNode person = new ElhNode(NodeType.ENTITY, "Person", null);
        person.setChildgrouptype(ChildGrouping.ROLEGROUP);
        person.setAttributes(List.of(text("name"), new Attribute("age", Attribute.Type.NUMBER, null, false, null),
                text("address")));
        ElhNode member = new ElhNode(NodeType.ROLE, "Library Member", person);
        member.setAttributes(List.of(new Attribute("membership number", Attribute.Type.TEXT, null, true, "on the card")));
        person.addChild(member);
        member.addChild(new ElhNode(NodeType.ACTION, "Joins", member));
        person.addChild(new ElhNode(NodeType.ROLE, "CounterStaff", person));
        return person;
    }

    @Test
    void attributesAreSavedWithTheirNode() throws IOException
    {
        Project project = Project.create(elhfolder.toFile(), "Library", "Paul");
        ProjectEntity entity = project.addEntity(person());
        project.moveEntityToFile(entity);
        Project loaded = Project.load(project.getFolder());
        ElhNode top = loaded.loadEntity(loaded.findEntity("Person"));
        assertEquals(List.of("name", "age", "address"), top.getAttributes().stream().map(Attribute::name).toList());
        assertEquals(Attribute.Type.NUMBER, top.getAttributes().get(1).type());
        Attribute number = top.findChildnode("Library Member").getAttributes().getFirst();
        assertEquals(new Attribute("membership number", Attribute.Type.TEXT, null, true, "on the card"), number);
        assertEquals(1, top.findChildnode("Library Member").countChildren(), "the attributes are not taken for a node");
    }

    @Test
    void aRoleHasItsOwnAttributesItsEntitysAndItsState()
    {
        DataModel model = DataModel.of(List.of(person()));
        DataModel.Owner member = model.find("library member");
        assertEquals(List.of("name", "age", "address", "membership number", "state"),
                member.attributes().stream().map(Attribute::name).toList());
        assertEquals(List.of("name", "age", "address", "state"),
                model.find("Person").attributes().stream().map(Attribute::name).toList());
    }

    @Test
    void referencesWithSpacesInTheirNamesAreFound()
    {
        DataModel model = DataModel.of(List.of(person()));
        List<DataModel.Reference> found = new ArrayList<>();
        List<String> problems = model.check("register Library Member.name and Library Member.membership number, "
                + "then CounterStaff.state", found);
        assertTrue(problems.isEmpty(), problems.toString());
        assertEquals(List.of("name", "membership number", "state"), found.stream().map(r -> r.attribute().name()).toList());
        assertEquals("Library Member", found.getFirst().owner().name());
    }

    @Test
    void unknownNamesAreReported()
    {
        DataModel model = DataModel.of(List.of(person()));
        assertEquals(List.of("Library Member has no attribute \"phone\"", "no entity or role called Member"),
                model.check("record Library Member.phone and Member.name"));
    }

    @Test
    void ordinaryDotsAreNotReferences()
    {
        DataModel model = DataModel.of(List.of(person()));
        assertTrue(model.check("check the id, e.g. a passport. Then go on. Costs 2.50").isEmpty());
    }

    @Test
    void onlyActivitiesInsideTheSystemAreChecked()
    {
        DataModel model = DataModel.of(List.of(person()));
        EventProcess process = EventProcess.read("""
                Visitor:
                    write down Visitor.anything
                CounterStaff:
                    > register Library Member.name and Library Member.age
                    > issue Library Member.card number
                Clerk:
                    > file the form
                """);
        assertEquals(List.of("line 5: Library Member has no attribute \"card\"",
                "line 7: no entity or role called Clerk"), model.checkProcess(process));
    }
}
