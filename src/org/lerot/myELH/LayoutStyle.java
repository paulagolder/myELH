package org.lerot.myELH;

// How the diagram is drawn, chosen from the Options menu
public enum LayoutStyle
{
    DEFAULT("Default Style"),
    JACKSON("Jackson Style"),
    SIMPLE("Plain Style");

    final String menuLabel;

    LayoutStyle(String menuLabel)
    {
        this.menuLabel = menuLabel;
    }
}
