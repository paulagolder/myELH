package org.lerot.myELH;

import java.util.Arrays;
import java.util.List;

// How a node's children relate to each other; saved in .elh files as the
// childgrouptype attribute, in lower case.
public enum ChildGrouping
{
    SEQUENCE, OPTION, REPETITION, ROLEGROUP, ENTITYGROUP;

    // the choices offered in the edit panel
    static final List<ChildGrouping> CHOICES = List.of(SEQUENCE, OPTION, REPETITION, ROLEGROUP);

    public String xmlName()
    {
        return name().toLowerCase();
    }

    // reads a saved value, in any case; anything unknown is treated as a sequence
    public static ChildGrouping fromXml(String text)
    {
        return Arrays.stream(values())
                .filter(grouping -> grouping.name().equalsIgnoreCase(text))
                .findFirst()
                .orElseGet(() ->
                {
                    System.out.println(" unknown childgrouptype " + text + ", using sequence");
                    return SEQUENCE;
                });
    }

    static String[] choiceNames()
    {
        return CHOICES.stream().map(ChildGrouping::xmlName).toArray(String[]::new);
    }
}
