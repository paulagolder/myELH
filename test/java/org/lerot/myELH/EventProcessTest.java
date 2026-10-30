package org.lerot.myELH;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventProcessTest
{
    @TempDir
    Path elhfolder;

    private static final String JOINS = """
            Member:
                fill in the application form
            CounterStaff:
                check the customer's id
                IF id not valid
                    refuse the application
                    STOP
                END
                > create Member record
                > register name, age and address
                > issue membership number and card
            """;

    @Test
    void readsRolesActivitiesAndADecision()
    {
        EventProcess process = EventProcess.read(JOINS);
        assertTrue(process.problems().isEmpty(), process.problems().toString());
        List<EventProcess.Step> steps = process.steps();
        assertEquals(6, steps.size(), "2 activities, the decision, 3 more activities");
        assertEquals(new EventProcess.Activity("Member", "fill in the application form", false, 2), steps.get(0));
        EventProcess.Decision decision = (EventProcess.Decision) steps.get(2);
        assertEquals("id not valid", decision.condition());
        assertEquals(2, decision.yes().size());
        assertInstanceOf(EventProcess.Stop.class, decision.yes().get(1));
        assertTrue(decision.no().isEmpty());
        assertEquals(List.of("Member", "CounterStaff"), List.copyOf(process.roles()));
    }

    @Test
    void anActivityMarkedWithAnArrowIsInsideTheSystem()
    {
        List<EventProcess.Activity> activities = EventProcess.read(JOINS).activities();
        assertEquals(List.of(false, false, false, true, true, true),
                activities.stream().map(EventProcess.Activity::internal).toList());
        assertEquals("create Member record", activities.get(3).text());
        assertEquals("CounterStaff", activities.get(3).role());
    }

    @Test
    void aSummarySaysWhatWasRead()
    {
        assertEquals("6 activities (3 inside the system) by Member, CounterStaff", EventProcess.read(JOINS).summary());
        assertEquals("No activities yet", EventProcess.read("").summary());
    }

    @Test
    void aRoleCanBeGivenOnTheActivitysOwnLine()
    {
        EventProcess process = EventProcess.read("""
                Member: fill in the form
                > CounterStaff: register the member
                """);
        assertEquals(List.of(new EventProcess.Activity("Member", "fill in the form", false, 1),
                new EventProcess.Activity("CounterStaff", "register the member", true, 2)), process.activities());
    }

    @Test
    void elseAndRepeatAndCommentsAndAnyCase()
    {
        EventProcess process = EventProcess.read("""
                -- a comment
                CounterStaff:
                    if the member has books out
                        refuse
                    else
                        > close the membership
                    end
                    Repeat for each card
                        > cancel the card
                    End
                """);
        assertTrue(process.problems().isEmpty(), process.problems().toString());
        EventProcess.Decision decision = (EventProcess.Decision) process.steps().get(0);
        assertEquals(1, decision.yes().size());
        assertEquals(1, decision.no().size());
        EventProcess.Loop loop = (EventProcess.Loop) process.steps().get(1);
        assertEquals("for each card", loop.condition());
        assertEquals("cancel the card", ((EventProcess.Activity) loop.body().getFirst()).text());
    }

    @Test
    void mistakesAreReportedWithTheirLine()
    {
        EventProcess process = EventProcess.read("""
                fill in the form
                Member:
                    END
                    IF
                    ELSE
                """);
        List<String> problems = process.problems();
        assertTrue(problems.get(0).startsWith("line 1: whose activity"), problems.toString());
        assertTrue(problems.contains("line 3: END without an IF or REPEAT"), problems.toString());
        assertTrue(problems.contains("line 4: IF needs a condition"), problems.toString());
        assertTrue(problems.contains("line 4: IF has no END"), problems.toString());
        assertTrue(process.summary().startsWith("line 1:"));
    }

    @Test
    void aProcessIsSavedAsWrittenInlineAndInItsOwnFile() throws IOException
    {
        Project project = Project.create(elhfolder.toFile(), "Library", "Paul");
        ProjectEvent joins = project.addEvent("Joins");
        joins.setProcess(JOINS);
        project.save();
        Project loaded = Project.load(project.getFolder());
        assertEquals(JOINS.stripTrailing(), loaded.findEvent(joins.getId()).getProcess(), "kept inline, indentation and all");
        project.moveEventToFile(joins);
        Project fromfile = Project.load(project.getFolder());
        ProjectEvent again = fromfile.findEvent(joins.getId());
        fromfile.loadEvent(again);
        assertEquals(JOINS.stripTrailing(), again.getProcess(), "kept in its own file, indentation and all");
    }
}
