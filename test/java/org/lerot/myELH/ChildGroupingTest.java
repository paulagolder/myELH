package org.lerot.myELH;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChildGroupingTest
{
    @Test
    void readsTheWordsSavedInFilesInAnyCase()
    {
        assertEquals(ChildGrouping.REPETITION, ChildGrouping.fromXml("repetition"));
        assertEquals(ChildGrouping.OPTION, ChildGrouping.fromXml("Option"));
        assertEquals(ChildGrouping.ENTITYGROUP, ChildGrouping.fromXml("entitygroup"));
    }

    @Test
    void unknownWordsAreTreatedAsASequence()
    {
        assertEquals(ChildGrouping.SEQUENCE, ChildGrouping.fromXml("Iteration"));
        assertEquals(ChildGrouping.SEQUENCE, ChildGrouping.fromXml(null));
    }

    @Test
    void writesLowerCaseWords()
    {
        assertEquals("rolegroup", ChildGrouping.ROLEGROUP.xmlName());
    }

    @Test
    void theEditPanelOffersTheFourGroupings()
    {
        assertArrayEquals(new String[]{"sequence", "option", "repetition", "rolegroup"},
                ChildGrouping.choiceNames());
    }
}
