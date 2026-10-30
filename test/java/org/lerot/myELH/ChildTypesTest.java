package org.lerot.myELH;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChildTypesTest
{
    // Person with parallel roles Library Member, whose life is actions, and Employee
    private static ElhNode person()
    {
        ElhNode person = new ElhNode(NodeType.ENTITY, "Person", null);
        person.setChildgrouptype(ChildGrouping.ROLEGROUP);
        ElhNode member = new ElhNode(NodeType.ROLE, "Library Member", person);
        person.addChild(member);
        member.addChild(new ElhNode(NodeType.ACTION, "Joins", member));
        member.addChild(new ElhNode(NodeType.ACTION, "Leaves", member));
        person.addChild(new ElhNode(NodeType.ROLE, "Employee", person));
        return person;
    }

    @Test
    void parallelRolesHoldOnlyRoles()
    {
        assertEquals(List.of(NodeType.ROLE), ChildTypes.allowed(person()));
        ElhNode role = new ElhNode(NodeType.ROLE, "r", null);
        role.setChildgrouptype(ChildGrouping.ROLEGROUP);
        assertEquals(List.of(NodeType.ROLE), ChildTypes.allowed(role));
    }

    @Test
    void entitiesAndRolesOtherwiseHoldActionsFirstThenRoles()
    {
        for (NodeType type : List.of(NodeType.ENTITY, NodeType.ROLE))
        {
            for (ChildGrouping structure : List.of(ChildGrouping.SEQUENCE, ChildGrouping.OPTION,
                    ChildGrouping.REPETITION))
            {
                assertEquals(List.of(NodeType.ACTION, NodeType.ROLE), ChildTypes.allowed(type, structure));
            }
        }
    }

    @Test
    void actionsHoldOnlyActions()
    {
        for (ChildGrouping structure : ChildGrouping.values())
        {
            assertEquals(List.of(NodeType.ACTION), ChildTypes.allowed(NodeType.ACTION, structure));
        }
    }

    @Test
    void aSiblingIsOfferedTheNodesOwnTypeFirst()
    {
        ElhNode member = person().findChildnode("Library Member");
        assertEquals(List.of(NodeType.ROLE), ChildTypes.forSibling(member));
        assertEquals(List.of(NodeType.ACTION, NodeType.ROLE), ChildTypes.forSibling(member.findChildnode("Joins")));
        ElhNode book = new ElhNode(NodeType.ENTITY, "Book", null);
        ElhNode lending = new ElhNode(NodeType.ROLE, "Lending", book);
        book.addChild(lending);
        assertEquals(List.of(NodeType.ROLE, NodeType.ACTION), ChildTypes.forSibling(lending));
        assertEquals(List.of(), ChildTypes.forSibling(book), "the top has no siblings");
    }

    @Test
    void onlyATypeThatCanHoldTheChildrenIsInsertedAbove()
    {
        ElhNode person = person();
        assertEquals(List.of(NodeType.ROLE), ChildTypes.forInsertAbove(person), "roles can only be held by a role");
        ElhNode member = person.findChildnode("Library Member");
        assertEquals(List.of(NodeType.ACTION, NodeType.ROLE), ChildTypes.forInsertAbove(member));
        assertEquals(List.of(), ChildTypes.forInsertAbove(member.findChildnode("Joins")), "nothing to insert above");
    }

    @Test
    void insertingAboveRolesKeepsThemParallel()
    {
        ElhNode person = person();
        ElhNode lives = person.insertabovechildren(NodeType.ROLE, "Lives");
        assertEquals(1, person.countChildren());
        assertEquals(ChildGrouping.ROLEGROUP, lives.getChildgrouptype());
        assertEquals(2, lives.countChildren());
        assertSame(lives, lives.findChildnode("Employee").getParent());
        assertEquals(List.of(NodeType.ACTION, NodeType.ROLE), ChildTypes.allowed(person));
    }
}
