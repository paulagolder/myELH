package org.lerot.myELH;

import java.util.Arrays;
import java.util.List;

// A node's structure: how its children relate to each other, in Jackson's
// terms a sequence, a selection or an iteration, or roles in parallel.
// Saved in .elh files as the childgrouptype attribute, in lower case; shown
// on screen as a short phrase saying what the children are.
public enum ChildGrouping
{
    SEQUENCE("in sequence"),
    OPTION("one of (selection)"),
    REPETITION("repeated (iteration)"),
    ROLEGROUP("parallel roles"),
    ENTITYGROUP("entity group");

    // what the edit panel shows
    final String label;

    ChildGrouping(String label)
    {
        this.label = label;
    }

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
