package org.lerot.myELH;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

// The state indicators of an entity, worked out from its life history as
// SSADM does. The actions are numbered in order; each action, when it
// happens, sets the entity's state indicator to its own number, and is only
// valid when the indicator holds the number of an action that can come
// directly before it, or, if it can come first, "-" (the entity does not
// exist yet).
//
// What can come before what follows from the structure of each node: in
// sequence, each child follows the one before; one of (selection), any
// child can come; repeated (iteration), the children in sequence, any
// number of times, so the end can be followed by the start again, or they
// can be skipped. Parallel roles (and entity groups) each have a state
// indicator of their own, named after the role, as the entity lives them
// at the same time; the life around them is not changed by their actions.
// An entity outside the information system has no state indicator of its
// own; its roles still have theirs.
public final class StateIndicators
{
    // what an action needs and does to its state indicator
    public record ActionState(String indicator, int setsto, Set<Integer> validprior, boolean canstart,
            boolean canend)
    {
        // the valid prior values, "-" first if the action can come first
        public String priorText()
        {
            List<String> values = new ArrayList<>();
            if (this.canstart) values.add("-");
            this.validprior.forEach(n -> values.add(String.valueOf(n)));
            return values.isEmpty() ? "none" : String.join(",", values);
        }

        // as written under the action's box: valid prior / sets to
        public String label()
        {
            return priorText() + "/" + this.setsto;
        }
    }

    private final Map<ElhNode, ActionState> states = new IdentityHashMap<>();
    private final List<String> indicators = new ArrayList<>();

    private StateIndicators()
    {
    }

    public static StateIndicators of(ElhNode top)
    {
        StateIndicators result = new StateIndicators();
        result.analyseLife(top.getName(), top, !top.isOutside());
        return result;
    }

    // the state of an action of the life history, or null if it is not an action
    public ActionState stateOf(ElhNode node)
    {
        return this.states.get(node);
    }

    // the names of the entity's state indicators: one for its life as a
    // whole, if that has actions of its own, and one for each parallel role
    public List<String> indicators()
    {
        return Collections.unmodifiableList(this.indicators);
    }

    // one life with its own state indicator: its actions are numbered from 1
    private static final class Life
    {
        final String name;
        final List<ElhNode> actions = new ArrayList<>();
        final Map<ElhNode, Set<ElhNode>> before = new IdentityHashMap<>();

        Life(String name)
        {
            this.name = name;
        }
    }

    // what a part of a life history can begin and end with, and whether it
    // can happen without any action at all
    private record Part(boolean skippable, Set<ElhNode> first, Set<ElhNode> last)
    {
        static final Part EMPTY = new Part(true, Set.of(), Set.of());
    }

    private void analyseLife(String name, ElhNode root)
    {
        analyseLife(name, root, true);
    }

    // held is false for a life outside the information system: what is in
    // it is still worked out (its parallel roles), but it has no indicator
    private void analyseLife(String name, ElhNode root, boolean held)
    {
        Life life = new Life(name);
        Part whole = analyse(root, life);
        if (life.actions.isEmpty() || !held)
        {
            return;
        }
        this.indicators.add(name);
        for (int i = 0; i < life.actions.size(); i++)
        {
            ElhNode action = life.actions.get(i);
            Set<Integer> prior = new TreeSet<>();
            for (ElhNode previous : life.before.getOrDefault(action, Set.of()))
            {
                prior.add(life.actions.indexOf(previous) + 1);
            }
            this.states.put(action, new ActionState(name, i + 1, Collections.unmodifiableSet(prior),
                    whole.first().contains(action), whole.last().contains(action)));
        }
    }

    private Part analyse(ElhNode node, Life life)
    {
        if (node.isLeafAction())
        {
            life.actions.add(node);
            Set<ElhNode> only = newSet(node);
            return new Part(false, only, only);
        }
        List<ElhNode> children = node.getChildren();
        if (children.isEmpty())
        {
            return Part.EMPTY;
        }
        return switch (node.getChildgrouptype())
        {
            case ROLEGROUP, ENTITYGROUP ->
            {
                // each role is lived at the same time, with its own indicator
                for (ElhNode child : children)
                {
                    analyseLife(child.getName(), child);
                }
                yield Part.EMPTY;
            }
            case OPTION ->
            {
                boolean skippable = false;
                Set<ElhNode> first = newSet();
                Set<ElhNode> last = newSet();
                for (ElhNode child : children)
                {
                    Part part = analyse(child, life);
                    skippable |= part.skippable();
                    first.addAll(part.first());
                    last.addAll(part.last());
                }
                yield new Part(skippable, first, last);
            }
            case REPETITION ->
            {
                Part body = sequence(children, life);
                // the end of one time round can be followed by the start of the next
                link(body.last(), body.first(), life);
                yield new Part(true, body.first(), body.last());
            }
            case SEQUENCE -> sequence(children, life);
        };
    }

    private Part sequence(List<ElhNode> children, Life life)
    {
        Part sofar = Part.EMPTY;
        for (ElhNode child : children)
        {
            Part next = analyse(child, life);
            link(sofar.last(), next.first(), life);
            Set<ElhNode> first = newSet(sofar.first());
            if (sofar.skippable()) first.addAll(next.first());
            Set<ElhNode> last = newSet(next.last());
            if (next.skippable()) last.addAll(sofar.last());
            sofar = new Part(sofar.skippable() && next.skippable(), first, last);
        }
        return sofar;
    }

    // each of the actions in to can come directly after any of those in from
    private static void link(Set<ElhNode> from, Set<ElhNode> to, Life life)
    {
        for (ElhNode action : to)
        {
            life.before.computeIfAbsent(action, k -> newSet()).addAll(from);
        }
    }

    private static Set<ElhNode> newSet(ElhNode... nodes)
    {
        Set<ElhNode> set = Collections.newSetFromMap(new IdentityHashMap<>());
        Collections.addAll(set, nodes);
        return set;
    }

    private static Set<ElhNode> newSet(Set<ElhNode> nodes)
    {
        Set<ElhNode> set = newSet();
        set.addAll(nodes);
        return set;
    }

    // the state of every action, for showing, as "indicator: prior/set-to"
    public String describe()
    {
        return this.states.values().stream()
                .sorted(java.util.Comparator.comparing(ActionState::indicator).thenComparing(ActionState::setsto))
                .map(s -> s.indicator() + " " + s.label())
                .collect(Collectors.joining("\n"));
    }
}
