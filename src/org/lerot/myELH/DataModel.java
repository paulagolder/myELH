package org.lerot.myELH;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// The project's data: every entity and role, with the attributes that can
// be named for it, written Role.attribute ("Member.name", "Library
// Member.membership number"). A role has its own attributes and those of
// the entity (and any role) it belongs to; each also has its state
// indicator, named "state". An entity outside the information system has
// no attributes or state, and passes none on: its roles are entities in
// their own right. Used to check the references in what happens
// inside the information system: activities marked ">" and actions'
// conditions and results.
public final class DataModel
{
    // the name every entity and role's state indicator goes by
    static final String STATE = "state";

    // an entity or a role, with every attribute that can be named for it
    public record Owner(String name, ElhNode node, List<Attribute> attributes)
    {
        // the attribute of this name, in any case, or null
        public Attribute find(String attributename)
        {
            return this.attributes.stream().filter(a -> a.name().equalsIgnoreCase(attributename)).findFirst()
                    .orElse(null);
        }
    }

    // a reference found in some text
    public record Reference(Owner owner, Attribute attribute)
    {
    }

    // by name in lower case, in the order found
    private final Map<String, Owner> owners = new LinkedHashMap<>();
    // the names, in lower case, of entities outside the information system
    private final java.util.Set<String> outside = new java.util.HashSet<>();

    private DataModel()
    {
    }

    public static DataModel of(Project project)
    {
        return of(project.lifeHistories().stream().map(Project.LifeHistory::top).toList());
    }

    public static DataModel of(List<ElhNode> tops)
    {
        DataModel model = new DataModel();
        for (ElhNode top : tops)
        {
            model.collect(top, List.of(), true);
        }
        return model;
    }

    private void collect(ElhNode node, List<Attribute> inherited, boolean top)
    {
        if (node.isOutside())
        {
            this.outside.add(node.getName().toLowerCase(Locale.ROOT));
            for (ElhNode child : node.getChildren())
            {
                collect(child, List.of(), false);
            }
            return;
        }
        List<Attribute> all = inherited;
        if (top || node.is(NodeType.ENTITY) || node.is(NodeType.ROLE))
        {
            all = new ArrayList<>(inherited);
            for (Attribute own : node.getAttributes())
            {
                all.removeIf(a -> a.name().equalsIgnoreCase(own.name()));
                all.add(own);
            }
            List<Attribute> named = new ArrayList<>(all);
            if (named.stream().noneMatch(a -> a.name().equalsIgnoreCase(STATE)))
            {
                named.add(new Attribute(STATE, Attribute.Type.NUMBER, null, false, "its state indicator"));
            }
            this.owners.putIfAbsent(node.getName().toLowerCase(Locale.ROOT),
                    new Owner(node.getName(), node, List.copyOf(named)));
        }
        for (ElhNode child : node.getChildren())
        {
            collect(child, all, false);
        }
    }

    // the entity or role of this name, in any case, or null
    public Owner find(String name)
    {
        return name == null ? null : this.owners.get(name.strip().toLowerCase(Locale.ROOT));
    }

    public List<Owner> owners()
    {
        return List.copyOf(this.owners.values());
    }

    // the references to attributes in the text, and what is wrong with any
    // that do not resolve. A reference is a known entity or role name, a
    // dot, and one of its attribute names; where names contain spaces the
    // longest known name is taken. A capitalised word before a dot that is
    // not a known name is reported; anything else (e.g. "e.g.") is not a
    // reference.
    public List<String> check(String text, List<Reference> found)
    {
        List<String> problems = new ArrayList<>();
        String lower = text.toLowerCase(Locale.ROOT);
        for (int dot = text.indexOf('.'); dot >= 0; dot = text.indexOf('.', dot + 1))
        {
            if (dot + 1 >= text.length() || !Character.isLetter(text.charAt(dot + 1)))
            {
                continue;
            }
            Owner owner = ownerEndingAt(lower, dot);
            String outsidename = outsideEndingAt(lower, dot);
            if (outsidename != null && (owner == null || outsidename.length() > owner.name().length()))
            {
                problems.add(text.substring(dot - outsidename.length(), dot)
                        + " is outside the information system: it has no attributes");
                continue;
            }
            if (owner == null)
            {
                String word = lastWord(text.substring(0, dot));
                if (!word.isEmpty() && Character.isUpperCase(word.charAt(0)))
                {
                    problems.add("no entity or role called " + word);
                }
                continue;
            }
            Attribute attribute = attributeStartingAt(owner, lower, dot + 1);
            if (attribute == null)
            {
                problems.add(owner.name() + " has no attribute \"" + firstWord(text.substring(dot + 1)) + "\"");
            } else if (found != null)
            {
                found.add(new Reference(owner, attribute));
            }
        }
        return problems;
    }

    public List<String> check(String text)
    {
        return check(text, null);
    }

    // the longest known name that the text ends with just before the dot
    private Owner ownerEndingAt(String lower, int dot)
    {
        Owner best = null;
        for (Map.Entry<String, Owner> entry : this.owners.entrySet())
        {
            String name = entry.getKey();
            int start = dot - name.length();
            if (start >= 0 && lower.startsWith(name, start)
                    && (start == 0 || !Character.isLetterOrDigit(lower.charAt(start - 1)))
                    && (best == null || name.length() > best.name().length()))
            {
                best = entry.getValue();
            }
        }
        return best;
    }

    // the longest name of an entity outside the system that the text ends with just before the dot
    private String outsideEndingAt(String lower, int dot)
    {
        String best = null;
        for (String name : this.outside)
        {
            int start = dot - name.length();
            if (start >= 0 && lower.startsWith(name, start)
                    && (start == 0 || !Character.isLetterOrDigit(lower.charAt(start - 1)))
                    && (best == null || name.length() > best.length()))
            {
                best = name;
            }
        }
        return best;
    }

    // the longest of the owner's attribute names that the text starts with at from
    private static Attribute attributeStartingAt(Owner owner, String lower, int from)
    {
        Attribute best = null;
        for (Attribute attribute : owner.attributes())
        {
            String name = attribute.name().toLowerCase(Locale.ROOT);
            int end = from + name.length();
            if (!name.isEmpty() && lower.startsWith(name, from)
                    && (end == lower.length() || !Character.isLetterOrDigit(lower.charAt(end)))
                    && (best == null || name.length() > best.name().length()))
            {
                best = attribute;
            }
        }
        return best;
    }

    private static String lastWord(String text)
    {
        int i = text.length();
        while (i > 0 && Character.isLetterOrDigit(text.charAt(i - 1))) i--;
        return text.substring(i);
    }

    private static String firstWord(String text)
    {
        int i = 0;
        while (i < text.length() && Character.isLetterOrDigit(text.charAt(i))) i++;
        return text.substring(0, i);
    }

    // what is wrong in what happens inside the system in a process: each
    // activity marked ">" must be by a known entity or role, and name only
    // attributes there are; each problem says its line
    public List<String> checkProcess(EventProcess process)
    {
        List<String> problems = new ArrayList<>();
        for (EventProcess.Activity activity : process.activities())
        {
            if (!activity.internal())
            {
                continue;
            }
            if (activity.role() != null && this.outside.contains(activity.role().strip().toLowerCase(Locale.ROOT)))
            {
                problems.add("line " + activity.line() + ": " + activity.role()
                        + " is outside the information system: name one of its roles");
            } else if (activity.role() != null && find(activity.role()) == null)
            {
                problems.add("line " + activity.line() + ": no entity or role called " + activity.role());
            }
            for (String problem : check(activity.text()))
            {
                problems.add("line " + activity.line() + ": " + problem);
            }
        }
        return problems;
    }
}
