package org.lerot.myELH;

import java.util.Arrays;

import org.jdom2.Element;

// An attribute of an entity or a role: a piece of data the information
// system holds about it. Saved inside its owner's element in the entity's
// file as <attribute name="..." type="..." refers="..." key="true" note="..."/>.
public record Attribute(String name, Type type, String refers, boolean key, String note)
{
    // what kind of value an attribute holds; a reference holds another entity
    public enum Type
    {
        TEXT, NUMBER, DATE, YESNO, REFERENCE;

        public String xmlName()
        {
            return name().toLowerCase();
        }

        public String toString()
        {
            return this == YESNO ? "yes/no" : xmlName();
        }

        static Type fromXml(String text)
        {
            return Arrays.stream(values()).filter(t -> t.xmlName().equalsIgnoreCase(text)).findFirst().orElse(TEXT);
        }
    }

    public Attribute
    {
        name = name == null ? "" : name.strip();
        type = type == null ? Type.TEXT : type;
        refers = type == Type.REFERENCE && refers != null && !refers.isBlank() ? refers.strip() : null;
        note = note == null ? "" : note.strip();
    }

    public Element toElement()
    {
        Element element = new Element("attribute").setAttribute("name", this.name)
                .setAttribute("type", this.type.xmlName());
        if (this.refers != null) element.setAttribute("refers", this.refers);
        if (this.key) element.setAttribute("key", "true");
        if (!this.note.isEmpty()) element.setAttribute("note", this.note);
        return element;
    }

    public static Attribute fromElement(Element element)
    {
        return new Attribute(element.getAttributeValue("name"), Type.fromXml(element.getAttributeValue("type")),
                element.getAttributeValue("refers"), "true".equals(element.getAttributeValue("key")),
                element.getAttributeValue("note"));
    }
}
