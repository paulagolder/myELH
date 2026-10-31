package org.lerot.myELH;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Tests of how an action names the entity and roles it belongs to.
class EntityActionTest
{
    private static ElhNode add(ElhNode parent, NodeType type, String name)
    {
        ElhNode child = new ElhNode(type, name, parent);
        parent.addChild(child);
        return child;
    }

    @Test
    void actionDirectlyInEntityShowsEntity()
    {
        ElhNode book = new ElhNode(NodeType.ENTITY, "Book", null);
        ElhNode body = add(book, NodeType.ACTION, "body");
        ElhNode borrow = add(body, NodeType.ACTION, "Borrow");
        ProjectEntity entity = new ProjectEntity("Book", "book", null);
        assertEquals("Book", new EntityAction(entity, borrow).owner());
    }

    @Test
    void actionInRolesShowsEntityAndRoles()
    {
        ElhNode person = new ElhNode(NodeType.ENTITY, "Person", null);
        ElhNode member = add(person, NodeType.ROLE, "Library Member");
        ElhNode joins = add(member, NodeType.ACTION, "Joins");
        ElhNode employee = add(person, NodeType.ROLEGROUP, "Employee");
        ElhNode staff = add(employee, NodeType.ROLE, "CounterStaff");
        ElhNode operation = add(staff, NodeType.ACTION, "CounterOperation");
        ElhNode issue = add(operation, NodeType.ACTION, "IssueBook");
        ProjectEntity entity = new ProjectEntity("Person", "Person", null);
        assertEquals("Person / Library Member", new EntityAction(entity, joins).owner());
        assertEquals("Person / Employee / CounterStaff", new EntityAction(entity, issue).owner());
        assertEquals("Person / Employee / CounterStaff: IssueBook", new EntityAction(entity, issue).toString());
    }
}
