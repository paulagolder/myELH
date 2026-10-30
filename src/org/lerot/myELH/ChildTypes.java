package org.lerot.myELH;

import java.util.List;

// Which types of node may be put below a node, by the node's type and its
// structure. The first type listed is the one a new child is given unless
// the user picks another. Edit the table to change what is allowed: the
// first rule that matches is used, and a null structure matches any.
public final class ChildTypes
{
    private record Rule(NodeType parent, ChildGrouping structure, List<NodeType> children)
    {
        boolean matches(NodeType type, ChildGrouping grouping)
        {
            return this.parent == type && (this.structure == null || this.structure == grouping);
        }
    }

    private static final List<Rule> RULES = List.of(
            // an entity's or a role's parallel roles are roles
            new Rule(NodeType.ENTITY, ChildGrouping.ROLEGROUP, List.of(NodeType.ROLE)),
            new Rule(NodeType.ROLE, ChildGrouping.ROLEGROUP, List.of(NodeType.ROLE)),
            // otherwise entities and roles have actions, or roles within them
            new Rule(NodeType.ENTITY, null, List.of(NodeType.ACTION, NodeType.ROLE)),
            new Rule(NodeType.ROLE, null, List.of(NodeType.ACTION, NodeType.ROLE)),
            // an action can only have actions
            new Rule(NodeType.ACTION, null, List.of(NodeType.ACTION)),
            // types kept for older files
            new Rule(NodeType.ROLEGROUP, null, List.of(NodeType.ROLE)),
            new Rule(NodeType.ENTITYGROUP, null, List.of(NodeType.ENTITY)),
            new Rule(NodeType.ELH, null, List.of(NodeType.ENTITY, NodeType.ACTION, NodeType.ROLE)));

    private ChildTypes()
    {
    }

    // the types a child of a node of this type and structure may have, the usual one first
    public static List<NodeType> allowed(NodeType type, ChildGrouping structure)
    {
        return RULES.stream().filter(rule -> rule.matches(type, structure)).findFirst()
                .map(Rule::children).orElse(List.of(NodeType.ACTION));
    }

    public static List<NodeType> allowed(ElhNode parent)
    {
        return allowed(parent.getNodeType(), parent.getChildgrouptype());
    }

    // the types a new sibling of anode may have, anode's own type first if
    // it is allowed; empty for the top, which has no siblings
    public static List<NodeType> forSibling(ElhNode anode)
    {
        if (anode.getParent() == null) return List.of();
        List<NodeType> types = allowed(anode.getParent());
        if (!types.contains(anode.getNodeType())) return types;
        List<NodeType> ordered = new java.util.ArrayList<>(types);
        ordered.remove(anode.getNodeType());
        ordered.addFirst(anode.getNodeType());
        return ordered;
    }

    // the types a node put between anode and its children may have: it must
    // be allowed below anode, and be able to hold the children with the
    // structure it takes over from anode
    public static List<NodeType> forInsertAbove(ElhNode anode)
    {
        if (anode.countChildren() == 0) return List.of();
        ChildGrouping structure = anode.getChildgrouptype();
        return allowed(anode.getNodeType(), ChildGrouping.SEQUENCE).stream()
                .filter(type -> anode.getChildren().stream()
                        .allMatch(child -> allowed(type, structure).contains(child.getNodeType())))
                .toList();
    }
}
