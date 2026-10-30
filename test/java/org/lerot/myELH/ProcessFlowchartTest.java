package org.lerot.myELH;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// The layout of an event's flowchart; nothing is drawn on screen.
class ProcessFlowchartTest
{
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
            """;

    private static ProcessFlowchart.Node node(ProcessFlowchart chart, String text)
    {
        return chart.nodes().stream().filter(n -> String.join(" ", n.lines).equals(text)).findFirst().orElseThrow();
    }

    private static List<ProcessFlowchart.Edge> from(ProcessFlowchart chart, ProcessFlowchart.Node node)
    {
        return chart.edges().stream().filter(e -> e.from() == node).toList();
    }

    @Test
    void eachStepGoesInItsRolesLaneFromStartToEnd()
    {
        ProcessFlowchart chart = new ProcessFlowchart("Joins", EventProcess.read(JOINS));
        Map<String, String> shown = chart.describe();
        assertEquals("START in Member", shown.get("Start"));
        assertEquals("ACTIVITY in Member", shown.get("fill in the application form"));
        assertEquals("ACTIVITY in CounterStaff", shown.get("check the customer's id"));
        assertEquals("DECISION in CounterStaff", shown.get("id not valid?"));
        assertEquals("ACTIVITY in CounterStaff (system)", shown.get("create Member record"));
        assertEquals("END in CounterStaff", shown.get("End"));
    }

    @Test
    void aDecisionsYesGoesOnDownAndItsNoGoesRoundTheSide()
    {
        ProcessFlowchart chart = new ProcessFlowchart("Joins", EventProcess.read(JOINS));
        List<ProcessFlowchart.Edge> out = from(chart, node(chart, "id not valid?"));
        assertEquals(2, out.size());
        ProcessFlowchart.Edge yes = out.stream().filter(e -> "yes".equals(e.label())).findFirst().orElseThrow();
        ProcessFlowchart.Edge no = out.stream().filter(e -> "no".equals(e.label())).findFirst().orElseThrow();
        assertEquals("refuse the application", String.join(" ", yes.to().lines));
        assertFalse(yes.side());
        assertEquals("create Member record", String.join(" ", no.to().lines));
        assertTrue(no.side());
    }

    @Test
    void stopEndsThatBranch()
    {
        ProcessFlowchart chart = new ProcessFlowchart("Joins", EventProcess.read(JOINS));
        assertTrue(from(chart, node(chart, "Stop")).isEmpty());
    }

    @Test
    void aLoopHasAPathBackToItsStart()
    {
        ProcessFlowchart chart = new ProcessFlowchart("Leaves", EventProcess.read("""
                CounterStaff:
                    REPEAT for each book out
                        > record the book returned
                        > clear the loan
                    END
                    > close the membership
                """));
        ProcessFlowchart.Edge back = chart.edges().stream().filter(ProcessFlowchart.Edge::back).findFirst().orElseThrow();
        assertEquals("clear the loan", String.join(" ", back.from().lines));
        assertEquals("record the book returned", String.join(" ", back.to().lines));
        assertEquals("repeat for each book out", back.label());
    }

    @Test
    void stepsAreLaidOutTopToBottomInTheirLanes()
    {
        ProcessFlowchart chart = new ProcessFlowchart("Joins", EventProcess.read(JOINS));
        List<ProcessFlowchart.Node> nodes = chart.nodes();
        for (int i = 1; i < nodes.size(); i++)
        {
            assertTrue(nodes.get(i).y > nodes.get(i - 1).bottom(), "each step is below the one before");
        }
        assertTrue(node(chart, "check the customer's id").x > node(chart, "fill in the application form").x,
                "CounterStaff's lane is to the right of Member's");
        assertTrue(chart.getWidth() > 0 && chart.getHeight() > nodes.getLast().bottom());
    }
}
