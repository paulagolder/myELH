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
        assertEquals(List.of("Library Member has no attribute \"phone\"",
                        "no entity or role called Member: did you mean Library Member?"),
                model.check("record Library Member.phone and Member.name"));
    }

    @Test
    void ordinaryDotsAreNotReferences()
    {
        DataModel model = DataModel.of(List.of(person()));
        assertTrue(model.check("check the id, e.g. a passport. Then go on. Costs 2.50").isEmpty());
    }

    // Person outside the information system: its roles are entities in their own right
    private static ElhNode outsidePerson()
    {
        ElhNode person = person();
        person.setOutside(true);
        person.setAttributes(List.of());
        ElhNode member = person.findChildnode("Library Member");
        member.setAttributes(List.of(text("name"), text("membership number")));
        return person;
    }

    @Test
    void anEntityOutsideTheSystemPassesNothingOnAndCannotBeNamed()
    {
        DataModel model = DataModel.of(List.of(outsidePerson()));
        assertNull(model.find("Person"), "Person is not held");
        assertEquals(List.of("name", "membership number", "state"),
                model.find("Library Member").attributes().stream().map(Attribute::name).toList());
        assertEquals(List.of("state"), model.find("CounterStaff").attributes().stream().map(Attribute::name).toList(),
                "CounterStaff has nothing from Person");
        assertEquals(List.of("Person is outside the information system: it has no attributes"),
                model.check("record Person.name"));
        assertTrue(model.check("record Library Member.name").isEmpty());
        EventProcess process = EventProcess.read("Person:\n    > fill in the form");
        assertEquals(List.of("line 2: Person is outside the information system: name one of its roles"),
                model.checkProcess(process));
    }

    @Test
    void theOutsideMarkIsSaved() throws IOException
    {
        Project project = Project.create(elhfolder.toFile(), "Library", "Paul");
        project.moveEntityToFile(project.addEntity(outsidePerson()));
        Project loaded = Project.load(project.getFolder());
        assertTrue(loaded.loadEntity(loaded.findEntity("Person")).isOutside());
    }

    @Test
    void everyActivityAndConditionIsChecked()
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
                IF Library Member.colour is red
                    CounterStaff: refuse
                END
                """);
        assertEquals(List.of("line 2: no entity or role called Visitor",
                "line 5: Library Member has no attribute \"card\"",
                "line 7: no entity or role called Clerk",
                "line 8: Library Member has no attribute \"colour\""), model.checkProcess(process));
    }

    @Test
    void anEntityOutsideTheSystemCanActInTheWorld()
    {
        DataModel model = DataModel.of(List.of(outsidePerson()));
        EventProcess process = EventProcess.read("""
                person : presents themselves at reception
                counterstaff : checks persons.id
                IF person.id is satisfactory
                    counterstaff: creates Library Member record
                END
                """);
        assertEquals(List.of("line 2: no entity or role called persons: did you mean Person?",
                "line 3: person is outside the information system: it has no attributes"),
                model.checkProcess(process));
    }

    @Test
    void aNearNameIsSuggested()
    {
        DataModel model = DataModel.of(List.of(person()));
        assertEquals(List.of("line 1: no entity or role called Member: did you mean Library Member?",
                "line 2: no entity or role called Counterstaf: did you mean CounterStaff?"),
                model.checkProcess(EventProcess.read("Member: brings a book\nCounterstaf: scans it")));
    }

    @Test
    void anUnknownAttributeSaysWhoseItIs()
    {
        DataModel model = DataModel.of(List.of(person()));
        List<DataModel.Problem> problems = model.findProblems(EventProcess.read(
                "CounterStaff: enters Library Member.phone and Member.name"));
        assertEquals(2, problems.size());
        assertEquals("Library Member", problems.getFirst().owner().name());
        assertEquals("phone", problems.getFirst().attribute());
        assertNull(problems.get(1).attribute(), "an unknown role has no attribute to add");
    }

    @Test
    void attributesCanBeListedInBrackets()
    {
        DataModel model = DataModel.of(List.of(person()));
        List<DataModel.Reference> found = new ArrayList<>();
        List<String> problems = model.check("enter Library Member(name, membership number , age) "
                + "and CounterStaff (state)", found);
        assertTrue(problems.isEmpty(), problems.toString());
        assertEquals(List.of("name", "membership number", "age", "state"),
                found.stream().map(r -> r.attribute().name()).toList());
    }

    @Test
    void anUnknownAttributeInBracketsIsTakenWhole()
    {
        DataModel model = DataModel.of(List.of(person()));
        List<DataModel.Problem> problems = model.findProblems(EventProcess.read(
                "CounterStaff: > enters Library Member(name, phone number)"));
        assertEquals(List.of("line 1: Library Member has no attribute \"phone number\""),
                problems.stream().map(DataModel.Problem::toString).toList());
        assertEquals("phone number", problems.getFirst().attribute());
    }

    @Test
    void bracketsThatAreNotAListAreLeftAlone()
    {
        DataModel model = DataModel.of(List.of(outsidePerson()));
        assertTrue(model.check("refuse the application (politely)").isEmpty());
        assertEquals(List.of("no entity or role called Membr: did you mean Library Member?",
                        "Person is outside the information system: it has no attributes"),
                model.check("record Membr(name) and Person(name)"));
    }
}
