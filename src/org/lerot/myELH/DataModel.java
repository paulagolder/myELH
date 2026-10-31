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
// their own right. Used to check the roles and references in an event's
// process, and the references in actions' conditions and results.
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

    // something wrong in a process, on its line; where it is an attribute
    // the entity or role does not have, the owner and the name given, so
    // that the attribute can be added
    public record Problem(int line, String message, Owner owner, String attribute)
    {
        public String toString()
        {
            return "line " + this.line + ": " + this.message;
        }
    }

    // by name in lower case, in the order found
    private final Map<String, Owner> owners = new LinkedHashMap<>();
    // the names of entities outside the information system, by name in lower case
    private final Map<String, String> outside = new LinkedHashMap<>();

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
            this.outside.putIfAbsent(node.getName().toLowerCase(Locale.ROOT), node.getName());
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
    // longest known name is taken. A capitalised word, or one of three or
    // more letters, before a dot that is not a known name is reported;
    // anything else (e.g. "e.g.") is not a reference.
    public List<String> check(String text, List<Reference> found)
    {
        return checkText(text, 0, found).stream().map(Problem::message).toList();
    }

    public List<String> check(String text)
    {
        return check(text, null);
    }

    private List<Problem> checkText(String text, int line, List<Reference> found)
    {
        List<Problem> problems = new ArrayList<>();
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
                problems.add(new Problem(line, text.substring(dot - outsidename.length(), dot)
                        + " is outside the information system: it has no attributes", null, null));
                continue;
            }
            if (owner == null)
            {
                String word = lastWord(text.substring(0, dot));
                if (!word.isEmpty() && (Character.isUpperCase(word.charAt(0)) || word.length() >= 3))
                {
                    problems.add(new Problem(line, unknown(word), null, null));
                }
                continue;
            }
            Attribute attribute = attributeStartingAt(owner, lower, dot + 1);
            if (attribute == null)
            {
                String name = firstWord(text.substring(dot + 1));
                problems.add(new Problem(line, owner.name() + " has no attribute \"" + name + "\"", owner, name));
            } else if (found != null)
            {
                found.add(new Reference(owner, attribute));
            }
        }
        return problems;
    }

    // "no entity or role called ...", with the nearest known name if there is one
    private String unknown(String name)
    {
        String near = nearest(name);
        return "no entity or role called " + name + (near == null ? "" : ": did you mean " + near + "?");
    }

    // the entity or role whose name has the name as one of its words
    // ("Member" for "Library Member"), or differs from it by a letter or two
    private String nearest(String name)
    {
        String lower = name.strip().toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>(this.owners.values().stream().map(Owner::name).toList());
        names.addAll(this.outside.values());
        for (String known : names)
        {
            if (List.of(known.toLowerCase(Locale.ROOT).split("\\s+")).contains(lower)) return known;
        }
        for (String known : names)
        {
            if (lower.length() >= 4 && distance(lower, known.toLowerCase(Locale.ROOT)) <= 2) return known;
        }
        return null;
    }

    // the number of letters to add, remove or change to make one word the other
    private static int distance(String a, String b)
    {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++)
        {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++)
            {
                int change = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + change);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
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
        for (String name : this.outside.keySet())
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

    // what is wrong in a process, each problem saying its line: every
    // activity must be by a known entity or role, one inside the system
    // (marked ">") by one held in it; and every activity and condition may
    // name only attributes there are
    public List<Problem> findProblems(EventProcess process)
    {
        List<Problem> problems = new ArrayList<>();
        findProblems(process.steps(), problems);
        // a name wrong as both the role and in a reference is said once
        return problems.stream().distinct().toList();
    }

    public List<String> checkProcess(EventProcess process)
    {
        return findProblems(process).stream().map(Problem::toString).toList();
    }

    private void findProblems(List<EventProcess.Step> steps, List<Problem> problems)
    {
        for (EventProcess.Step step : steps)
        {
            switch (step)
            {
                case EventProcess.Activity activity ->
                {
                    String role = activity.role() == null ? null : activity.role().strip();
                    boolean isoutside = role != null && this.outside.containsKey(role.toLowerCase(Locale.ROOT));
                    if (isoutside && activity.internal())
                    {
                        problems.add(new Problem(activity.line(), role
                                + " is outside the information system: name one of its roles", null, null));
                    } else if (role != null && !isoutside && find(role) == null)
                    {
                        problems.add(new Problem(activity.line(), unknown(role), null, null));
                    }
                    problems.addAll(checkText(activity.text(), activity.line(), null));
                }
                case EventProcess.Decision decision ->
                {
                    problems.addAll(checkText(decision.condition(), decision.line(), null));
                    findProblems(decision.yes(), problems);
                    findProblems(decision.no(), problems);
                }
                case EventProcess.Loop loop ->
                {
                    problems.addAll(checkText(loop.condition(), loop.line(), null));
                    findProblems(loop.body(), problems);
                }
                case EventProcess.Stop stop ->
                {
                }
            }
        }
    }
}
