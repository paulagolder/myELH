package org.lerot.myELH;

import java.util.Arrays;
import java.util.List;

// What a node in an ELH is; saved in .elh files as the element name, in
// lower case. A node's type can be changed while editing.
public enum NodeType
{
    ELH(ChildGrouping.SEQUENCE),
    ENTITY(ChildGrouping.SEQUENCE),
    ENTITYGROUP(ChildGrouping.ENTITYGROUP),
    ACTION(ChildGrouping.SEQUENCE),
    ROLE(ChildGrouping.SEQUENCE),
    ROLEGROUP(ChildGrouping.ROLEGROUP);

    // the types a node below the top can be given in the edit panel
    static final List<NodeType> CHOICES = List.of(ENTITY, ACTION, ROLE, ROLEGROUP, ENTITYGROUP);

    // the child grouping a new node of this type starts with
    final ChildGrouping defaultgrouping;

    NodeType(ChildGrouping defaultgrouping)
    {
        this.defaultgrouping = defaultgrouping;
    }

    public String xmlName()
    {
        return name().toLowerCase();
    }

    // reads a saved element name, in any case; null if it is not a node type
    public static NodeType fromXml(String text)
    {
        // files saved before actions were so called have <event> nodes
        if ("event".equalsIgnoreCase(text))
        {
            return ACTION;
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(text))
                .findFirst()
                .orElse(null);
    }

    static String[] choiceNames()
    {
        return CHOICES.stream().map(NodeType::xmlName).toArray(String[]::new);
    }
}
