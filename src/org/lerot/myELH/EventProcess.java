package org.lerot.myELH;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// How an event happens, step by step, read from a few lines of text:
//
//   Member:
//       fill in the application form
//   CounterStaff:
//       check the customer's id
//       IF id not valid
//           refuse the application
//           STOP
//       END
//       > create Member record
//       > register name, age and address
//
// A line "Role:" starts the activities of that role (a member of an entity
// or a role in its life history); "Role: activity" is a single activity.
// An activity marked ">" happens inside the information system; any other
// happens outside it, in the world. The roles and Role.attribute references
// of both are checked (see DataModel.findProblems).
// IF ... / ELSE / END makes a decision, REPEAT ... / END a loop, STOP ends
// the process. Keywords may be in any case; indentation is only for the
// reader; a line starting "--" is a comment.
public final class EventProcess
{
    // a step of the process
    public sealed interface Step permits Activity, Decision, Loop, Stop
    {
    }

    // something a role does, inside the information system or outside it
    public record Activity(String role, String text, boolean internal, int line) implements Step
    {
    }

    // IF condition: the steps if it holds, and (after ELSE) if it does not
    public record Decision(String condition, List<Step> yes, List<Step> no, int line) implements Step
    {
    }

    // REPEAT condition: the steps repeated
    public record Loop(String condition, List<Step> body, int line) implements Step
    {
    }

    // STOP: the process ends here
    public record Stop(int line) implements Step
    {
    }

    private static final Pattern ROLEHEADING = Pattern.compile("([A-Za-z][\\w \\-]*?)\\s*:\\s*");
    private static final Pattern ROLEACTIVITY = Pattern.compile("(>\\s*)?([A-Za-z][\\w \\-]*?)\\s*:\\s*(.+)");

    private final List<Step> steps;
    private final List<String> problems;

    private EventProcess(List<Step> steps, List<String> problems)
    {
        this.steps = steps;
        this.problems = problems;
    }

    public List<Step> steps()
    {
        return this.steps;
    }

    // the lines that could not be read, each saying why
    public List<String> problems()
    {
        return this.problems;
    }

    // what is being built at one level: the steps so far, and what opened it
    private static final class Block
    {
        final List<Step> steps = new ArrayList<>();
        final String keyword;
        final String condition;
        final int line;
        // for an IF, its steps before ELSE once ELSE has been read
        List<Step> yes;

        Block(String keyword, String condition, int line)
        {
            this.keyword = keyword;
            this.condition = condition;
            this.line = line;
        }
    }

    public static EventProcess read(String text)
    {
        List<String> problems = new ArrayList<>();
        Deque<Block> open = new ArrayDeque<>();
        open.push(new Block(null, null, 0));
        String role = null;
        String[] lines = text == null ? new String[0] : text.split("\n", -1);
        for (int n = 1; n <= lines.length; n++)
        {
            String line = lines[n - 1].strip();
            if (line.isEmpty() || line.startsWith("--"))
            {
                continue;
            }
            String word = line.split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
            String rest = line.contains(" ") ? line.substring(line.indexOf(' ')).strip() : "";
            Block current = open.peek();
            switch (word)
            {
                case "IF", "REPEAT" ->
                {
                    if (rest.isEmpty())
                    {
                        problems.add("line " + n + ": " + word + " needs a condition");
                    }
                    open.push(new Block(word, rest, n));
                }
                case "ELSE" ->
                {
                    if (!"IF".equals(current.keyword) || current.yes != null)
                    {
                        problems.add("line " + n + ": ELSE without an IF");
                    } else
                    {
                        current.yes = List.copyOf(current.steps);
                        current.steps.clear();
                    }
                }
                case "END" ->
                {
                    if (current.keyword == null)
                    {
                        problems.add("line " + n + ": END without an IF or REPEAT");
                    } else
                    {
                        open.pop();
                        open.peek().steps.add(close(current));
                    }
                }
                case "STOP" -> current.steps.add(new Stop(n));
                default ->
                {
                    Matcher heading = ROLEHEADING.matcher(line);
                    Matcher single = ROLEACTIVITY.matcher(line);
                    if (heading.matches())
                    {
                        role = heading.group(1).strip();
                    } else if (single.matches())
                    {
                        current.steps.add(new Activity(single.group(2).strip(), single.group(3).strip(),
                                single.group(1) != null, n));
                    } else
                    {
                        boolean internal = line.startsWith(">");
                        String activity = internal ? line.substring(1).strip() : line;
                        if (role == null)
                        {
                            problems.add("line " + n + ": whose activity is \"" + activity
                                    + "\"? Start with a role, as \"Member:\"");
                        }
                        if (activity.isEmpty())
                        {
                            problems.add("line " + n + ": > needs an activity after it");
                        } else
                        {
                            current.steps.add(new Activity(role, activity, internal, n));
                        }
                    }
                }
            }
        }
        while (open.size() > 1)
        {
            Block unclosed = open.pop();
            problems.add("line " + unclosed.line + ": " + unclosed.keyword + " has no END");
            open.peek().steps.add(close(unclosed));
        }
        return new EventProcess(List.copyOf(open.pop().steps), problems);
    }

    private static Step close(Block block)
    {
        if ("REPEAT".equals(block.keyword))
        {
            return new Loop(block.condition, List.copyOf(block.steps), block.line);
        }
        return block.yes == null
                ? new Decision(block.condition, List.copyOf(block.steps), List.of(), block.line)
                : new Decision(block.condition, block.yes, List.copyOf(block.steps), block.line);
    }

    // every activity, in order, however deep
    public List<Activity> activities()
    {
        List<Activity> found = new ArrayList<>();
        collect(this.steps, found);
        return found;
    }

    private static void collect(List<Step> steps, List<Activity> found)
    {
        for (Step step : steps)
        {
            switch (step)
            {
                case Activity activity -> found.add(activity);
                case Decision decision ->
                {
                    collect(decision.yes(), found);
                    collect(decision.no(), found);
                }
                case Loop loop -> collect(loop.body(), found);
                case Stop stop ->
                {
                }
            }
        }
    }

    // the roles taking part, in the order they first appear
    public Set<String> roles()
    {
        Set<String> found = new LinkedHashSet<>();
        for (Activity activity : activities())
        {
            if (activity.role() != null) found.add(activity.role());
        }
        return found;
    }

    // a line saying what was read, or the first problem
    public String summary()
    {
        if (!this.problems.isEmpty())
        {
            return this.problems.size() == 1 ? this.problems.getFirst()
                    : this.problems.getFirst() + "  (and " + (this.problems.size() - 1) + " more)";
        }
        List<Activity> activities = activities();
        if (activities.isEmpty())
        {
            return "No activities yet";
        }
        long internal = activities.stream().filter(Activity::internal).count();
        return activities.size() + (activities.size() == 1 ? " activity" : " activities") + " ("
                + internal + " inside the system) by " + String.join(", ", roles());
    }
}
