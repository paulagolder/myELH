package org.lerot.myELH;

import java.util.ArrayList;
import java.util.List;

// An action: a leaf action node of an entity's life history, the entity's
// part in an event. Its node may be from a copy of the life
// history read only to be looked at (see Project.lifeHistories).
public record EntityAction(ProjectEntity entity, ElhNode node)
{
    // the entity and the roles the action is done in, as
    // "Person / Employee / CounterStaff"; the structure in between is left out
    public String owner()
    {
        List<String> names = new ArrayList<>();
        for (ElhNode anode = this.node.getParent(); anode != null; anode = anode.getParent())
        {
            if (anode.is(NodeType.ENTITY) || anode.is(NodeType.ROLE) || anode.is(NodeType.ROLEGROUP))
            {
                names.addFirst(anode.getName());
            }
        }
        if (names.isEmpty() || !names.getFirst().equals(this.entity.getName()))
        {
            names.addFirst(this.entity.getName());
        }
        return String.join(" / ", names);
    }

    public String toString()
    {
        return owner() + ": " + this.node.getName();
    }
}
