package org.lerot.myELH;

// An action: a leaf event node of an entity's life history, the entity's
// part in an external event. Its node may be from a copy of the life
// history read only to be looked at (see Project.lifeHistories).
public record EntityAction(ProjectEntity entity, ElhNode node)
{
    public String toString()
    {
        return this.entity.getName() + ": " + this.node.getName();
    }
}
