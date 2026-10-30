package org.lerot.myELH;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StateIndicatorsTest
{
    private static ElhNode node(NodeType type, String name, ElhNode parent, ChildGrouping structure)
    {
        ElhNode node = new ElhNode(type, name, parent);
        node.setChildgrouptype(structure);
        if (parent != null) parent.addChild(node);
        return node;
    }

    private static ElhNode action(String name, ElhNode parent)
    {
        return node(NodeType.ACTION, name, parent, ChildGrouping.SEQUENCE);
    }

    // a library member, as in the library example:
    //   Joins, then any number of (Borrow or Return), then Leaves
    private static ElhNode member()
    {
        ElhNode member = node(NodeType.ENTITY, "Member", null, ChildGrouping.SEQUENCE);
        action("Joins", member);
        ElhNode body = node(NodeType.ACTION, "body", member, ChildGrouping.REPETITION);
        ElhNode activity = node(NodeType.ACTION, "activity", body, ChildGrouping.OPTION);
        action("Borrow", activity);
        action("Return", activity);
        action("Leaves", member);
        return member;
    }

    private static String label(StateIndicators states, ElhNode top, String... path)
    {
        ElhNode node = top;
        for (String name : path) node = node.findChildnode(name);
        return states.stateOf(node).label();
    }

    @Test
    void aSequenceSetsEachActionAfterTheOneBefore()
    {
        ElhNode top = node(NodeType.ENTITY, "Book", null, ChildGrouping.SEQUENCE);
        action("Acquired", top);
        action("Catalogued", top);
        action("Withdrawn", top);
        StateIndicators states = StateIndicators.of(top);
        assertEquals("-/1", label(states, top, "Acquired"));
        assertEquals("1/2", label(states, top, "Catalogued"));
        assertEquals("2/3", label(states, top, "Withdrawn"));
        assertTrue(states.stateOf(top.findChildnode("Withdrawn")).canend());
        assertFalse(states.stateOf(top.findChildnode("Catalogued")).canend());
        assertEquals(List.of("Book"), states.indicators());
    }

    @Test
    void aRepeatedSelectionCanFollowItselfOrBeSkipped()
    {
        ElhNode member = member();
        StateIndicators states = StateIndicators.of(member);
        assertEquals("-/1", label(states, member, "Joins"));
        assertEquals("1,2,3/2", label(states, member, "body", "activity", "Borrow"));
        assertEquals("1,2,3/3", label(states, member, "body", "activity", "Return"));
        // Leaves can come straight after Joins, as the loop can be skipped
        assertEquals("1,2,3/4", label(states, member, "Leaves"));
    }

    @Test
    void parallelRolesEachHaveTheirOwnStateIndicator()
    {
        ElhNode person = node(NodeType.ENTITY, "Person", null, ChildGrouping.ROLEGROUP);
        ElhNode member = node(NodeType.ROLE, "Library Member", person, ChildGrouping.SEQUENCE);
        action("Joins", member);
        action("Leaves", member);
        ElhNode employee = node(NodeType.ROLE, "Employee", person, ChildGrouping.SEQUENCE);
        action("Hired", employee);
        StateIndicators states = StateIndicators.of(person);
        assertEquals(List.of("Library Member", "Employee"), states.indicators());
        ElhNode hired = employee.findChildnode("Hired");
        assertEquals("Employee", states.stateOf(hired).indicator());
        assertEquals("-/1", states.stateOf(hired).label(), "each role's actions are numbered from 1");
        assertEquals("1/2", label(states, person, "Library Member", "Leaves"));
    }

    @Test
    void anOptionalPartLetsWhatFollowsComeEarlier()
    {
        ElhNode top = node(NodeType.ENTITY, "Loan", null, ChildGrouping.SEQUENCE);
        action("Starts", top);
        ElhNode maybe = node(NodeType.ACTION, "renewals", top, ChildGrouping.REPETITION);
        action("Renewed", maybe);
        action("Ends", top);
        StateIndicators states = StateIndicators.of(top);
        assertEquals(Set.of(1, 2), states.stateOf(top.findChildnode("Ends")).validprior());
        assertEquals("1,2/2", label(states, top, "renewals", "Renewed"), "a renewal can follow another");
    }

    @Test
    void anEntityWithNoActionsHasNoStateIndicator()
    {
        StateIndicators states = StateIndicators.of(node(NodeType.ENTITY, "Empty", null, ChildGrouping.SEQUENCE));
        assertTrue(states.indicators().isEmpty());
    }
}
