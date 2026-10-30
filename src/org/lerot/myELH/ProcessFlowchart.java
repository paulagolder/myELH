package org.lerot.myELH;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// An event's process drawn as a flowchart, laid out once and ready to be
// painted on screen or into an SVG file. Each role has a swimlane, and the
// process runs down the page, one step to a row:
//
//   - an activity outside the system is a plain box; one inside it (">")
//     is a blue rounded box, and each run of them in a lane is enclosed in a
//     dashed "system" container
//   - a decision (IF) is a diamond: "yes" goes on down, "no" round the side
//   - a loop (REPEAT) is drawn once, with a path from its end back to its
//     start, labelled with its condition
//   - Start, End and STOP are rounded terminals
public final class ProcessFlowchart
{
    private static final double LANEWIDTH = 240;
    // the routes down the side of a lane, between its edge and the boxes:
    // a loop's way back on the left; on the right, a decision's "no", and,
    // just inside it, an arrow that skips past steps to reach a later one
    private static final double BACKROUTE = 16;
    private static final double NOROUTE = 14;
    private static final double SKIPROUTE = 26;
    private static final double HEADERHEIGHT = 34;
    private static final double TOPMARGIN = 16;
    private static final double GAP = 30;
    private static final double BOXWIDTH = 170;
    private static final double DIAMONDWIDTH = 150;
    private static final double TERMINALWIDTH = 80;
    private static final double TERMINALHEIGHT = 26;
    private static final Color LANEHEADER = new Color(0xE6E1D6);
    private static final Color LANESHADE = new Color(0xF7F5F0);
    private static final Color LINE = new Color(0x505050);
    private static final Color INSIDEFILL = new Color(0xDCE9F7);
    private static final Color INSIDEEDGE = new Color(0x3A6EA5);
    private static final Color DECISIONFILL = new Color(0xFFF4CF);
    private static final Color PROBLEM = new Color(0xC0580A);

    enum Kind
    {
        START, END, STOP, ACTIVITY, DECISION
    }

    // a shape on the chart
    static final class Node
    {
        final Kind kind;
        final int lane;
        final List<String> lines;
        final boolean internal;
        double x;
        double y;
        double width;
        double height;

        Node(Kind kind, int lane, List<String> lines, boolean internal)
        {
            this.kind = kind;
            this.lane = lane;
            this.lines = lines;
            this.internal = internal;
        }

        double centreX()
        {
            return this.x + this.width / 2;
        }

        double bottom()
        {
            return this.y + this.height;
        }
    }

    // an arrow: down to the next step, round the side for a decision's
    // "no", or back up for a loop
    record Edge(Node from, Node to, String label, boolean side, boolean back)
    {
    }

    // where the flow leaves a step, to go on to whatever comes next
    private record Exit(Node from, String label, boolean side)
    {
    }

    private final String title;
    private final List<String> lanes = new ArrayList<>();
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final List<String> problems;
    private final Font font;
    private final Font smallfont;
    private final Graphics2D measure;
    private double width;
    private double height;

    public ProcessFlowchart(String title, EventProcess process, Graphics2D measure)
    {
        this.title = title;
        this.problems = process.problems();
        this.measure = measure;
        this.font = new Font("SansSerif", Font.PLAIN, 12);
        this.smallfont = this.font.deriveFont(10f);
        this.lanes.addAll(process.roles());
        if (process.activities().stream().anyMatch(a -> a.role() == null))
        {
            this.lanes.add("?");
        }
        if (this.lanes.isEmpty())
        {
            this.lanes.add("");
        }
        build(process);
        layout();
    }

    public ProcessFlowchart(String title, EventProcess process)
    {
        this(title, process, new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics());
    }

    public double getWidth()
    {
        return this.width;
    }

    public double getHeight()
    {
        return this.height;
    }

    List<Node> nodes()
    {
        return this.nodes;
    }

    List<Edge> edges()
    {
        return this.edges;
    }

    // ---- building the chart from the process ----

    private int laneOf(String role)
    {
        int lane = role == null ? this.lanes.indexOf("?") : this.lanes.indexOf(role);
        return Math.max(lane, 0);
    }

    private Node add(Kind kind, int lane, List<String> lines, boolean internal)
    {
        Node node = new Node(kind, lane, new ArrayList<>(lines), internal);
        this.nodes.add(node);
        return node;
    }

    private void connect(List<Exit> exits, Node to)
    {
        for (Exit exit : exits)
        {
            this.edges.add(new Edge(exit.from(), to, exit.label(), exit.side(), false));
        }
    }

    private void build(EventProcess process)
    {
        List<EventProcess.Activity> activities = process.activities();
        int firstlane = activities.isEmpty() ? 0 : laneOf(activities.getFirst().role());
        Node start = add(Kind.START, firstlane, List.of("Start"), false);
        List<Exit> exits = emit(process.steps(), List.of(new Exit(start, null, false)), firstlane);
        if (!exits.isEmpty())
        {
            Node end = add(Kind.END, exits.getLast().from().lane, List.of("End"), false);
            connect(exits, end);
        }
    }

    // adds the steps in order; the flow comes in from incoming, and the
    // exits returned are where it leaves the last of them
    private List<Exit> emit(List<EventProcess.Step> steps, List<Exit> incoming, int lane)
    {
        List<Exit> exits = incoming;
        for (EventProcess.Step step : steps)
        {
            switch (step)
            {
                case EventProcess.Activity activity ->
                {
                    lane = laneOf(activity.role());
                    Node node = add(Kind.ACTIVITY, lane, List.of(activity.text()), activity.internal());
                    connect(exits, node);
                    exits = List.of(new Exit(node, null, false));
                }
                case EventProcess.Stop stop ->
                {
                    Node node = add(Kind.STOP, lane, List.of("Stop"), false);
                    connect(exits, node);
                    exits = List.of();
                }
                case EventProcess.Decision decision ->
                {
                    // the diamond goes in the lane of whoever acts next
                    int decisionlane = firstLane(decision.yes(), firstLane(decision.no(), lane));
                    Node diamond = add(Kind.DECISION, decisionlane, List.of(decision.condition() + "?"), false);
                    connect(exits, diamond);
                    List<Exit> after = new ArrayList<>(emit(decision.yes(),
                            List.of(new Exit(diamond, "yes", false)), decisionlane));
                    after.addAll(decision.no().isEmpty()
                            ? List.of(new Exit(diamond, "no", true))
                            : emit(decision.no(), List.of(new Exit(diamond, "no", true)), decisionlane));
                    exits = after;
                    lane = decisionlane;
                }
                case EventProcess.Loop loop ->
                {
                    int first = this.nodes.size();
                    List<Exit> after = emit(loop.body(), exits, lane);
                    if (this.nodes.size() > first)
                    {
                        // from the end of the body back to its start
                        Node start = this.nodes.get(first);
                        for (Exit exit : after)
                        {
                            this.edges.add(new Edge(exit.from(), start, "repeat " + loop.condition(), false, true));
                        }
                        exits = after;
                        lane = this.nodes.getLast().lane;
                    }
                }
            }
        }
        return exits;
    }

    private int firstLane(List<EventProcess.Step> steps, int otherwise)
    {
        for (EventProcess.Step step : steps)
        {
            if (step instanceof EventProcess.Activity activity) return laneOf(activity.role());
            if (step instanceof EventProcess.Decision decision)
                return firstLane(decision.yes(), firstLane(decision.no(), otherwise));
            if (step instanceof EventProcess.Loop loop) return firstLane(loop.body(), otherwise);
        }
        return otherwise;
    }

    // ---- laying it out ----

    private void layout()
    {
        double y = TOPMARGIN + titleHeight() + HEADERHEIGHT + GAP;
        for (Node node : this.nodes)
        {
            switch (node.kind)
            {
                case START, END, STOP ->
                {
                    node.width = TERMINALWIDTH;
                    node.height = TERMINALHEIGHT;
                }
                case ACTIVITY ->
                {
                    List<String> wrapped = wrap(node.lines.getFirst(), BOXWIDTH - 16, this.font);
                    node.lines.clear();
                    node.width = BOXWIDTH;
                    node.height = Math.max(36, wrapped.size() * lineHeight() + 14);
                    node.lines.addAll(wrapped);
                }
                case DECISION ->
                {
                    List<String> wrapped = wrap(node.lines.getFirst(), DIAMONDWIDTH * 0.6, this.font);
                    node.lines.clear();
                    node.width = DIAMONDWIDTH;
                    node.height = Math.max(56, wrapped.size() * lineHeight() + 34);
                    node.lines.addAll(wrapped);
                }
            }
            node.x = laneLeft(node.lane) + (LANEWIDTH - node.width) / 2;
            node.y = y;
            y += node.height + GAP;
        }
        this.width = this.lanes.size() * LANEWIDTH + 2;
        this.height = y + TOPMARGIN;
    }

    private double titleHeight()
    {
        return 22 + (this.problems.isEmpty() ? 0 : 16);
    }

    private double laneLeft(int lane)
    {
        return 1 + lane * LANEWIDTH;
    }

    private double lineHeight()
    {
        this.measure.setFont(this.font);
        return this.measure.getFontMetrics().getHeight();
    }

    // the text broken into lines no wider than maxwidth
    private List<String> wrap(String text, double maxwidth, Font afont)
    {
        this.measure.setFont(afont);
        FontMetrics fm = this.measure.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+"))
        {
            String tried = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(tried) > maxwidth && !line.isEmpty())
            {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else
            {
                line = new StringBuilder(tried);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    // ---- drawing ----

    public void draw(Graphics2D g2d)
    {
        drawLanes(g2d);
        drawSystemContainers(g2d);
        for (Edge edge : this.edges)
        {
            drawEdge(g2d, edge);
        }
        for (Node node : this.nodes)
        {
            drawNode(g2d, node);
        }
    }

    private void drawLanes(Graphics2D g2d)
    {
        double top = TOPMARGIN + titleHeight();
        g2d.setFont(this.font.deriveFont(Font.BOLD, 14f));
        g2d.setColor(Color.BLACK);
        g2d.drawString(this.title, (float) laneLeft(0) + 4, (float) (TOPMARGIN + 14));
        if (!this.problems.isEmpty())
        {
            g2d.setFont(this.smallfont);
            g2d.setColor(PROBLEM);
            g2d.drawString(TreeView.FLAG.strip() + " " + this.problems.getFirst()
                    + (this.problems.size() > 1 ? "  (and " + (this.problems.size() - 1) + " more)" : ""),
                    (float) laneLeft(0) + 4, (float) (TOPMARGIN + 30));
        }
        for (int lane = 0; lane < this.lanes.size(); lane++)
        {
            double left = laneLeft(lane);
            g2d.setColor(lane % 2 == 0 ? Color.WHITE : LANESHADE);
            g2d.fill(new Rectangle.Double(left, top, LANEWIDTH, this.height - top - TOPMARGIN / 2));
            g2d.setColor(LANEHEADER);
            g2d.fill(new Rectangle.Double(left, top, LANEWIDTH, HEADERHEIGHT));
            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(new Rectangle.Double(left, top, LANEWIDTH, this.height - top - TOPMARGIN / 2));
            g2d.setFont(this.font.deriveFont(Font.BOLD));
            g2d.setColor(Color.BLACK);
            drawCentred(g2d, this.lanes.get(lane), left + LANEWIDTH / 2, top + HEADERHEIGHT / 2);
        }
    }

    // a dashed "system" container round each run of activities inside the
    // system that follow one another in the same lane
    private void drawSystemContainers(Graphics2D g2d)
    {
        int i = 0;
        while (i < this.nodes.size())
        {
            Node first = this.nodes.get(i);
            if (first.kind != Kind.ACTIVITY || !first.internal)
            {
                i++;
                continue;
            }
            int j = i;
            while (j + 1 < this.nodes.size() && this.nodes.get(j + 1).kind == Kind.ACTIVITY
                    && this.nodes.get(j + 1).internal && this.nodes.get(j + 1).lane == first.lane)
            {
                j++;
            }
            Node last = this.nodes.get(j);
            double left = laneLeft(first.lane) + 8;
            double top = first.y - 18;
            Shape container = new RoundRectangle2D.Double(left, top, LANEWIDTH - 16, last.bottom() + 10 - top, 12, 12);
            g2d.setColor(new Color(0xF2F7FD));
            g2d.fill(container);
            g2d.setColor(INSIDEEDGE);
            g2d.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10,
                    new float[]{6, 4}, 0));
            g2d.draw(container);
            g2d.setFont(this.smallfont.deriveFont(Font.ITALIC));
            g2d.drawString("system", (float) left + 6, (float) top + 11);
            i = j + 1;
        }
    }

    private void drawNode(Graphics2D g2d, Node node)
    {
        g2d.setStroke(new BasicStroke(1.4f));
        switch (node.kind)
        {
            case START, END, STOP ->
            {
                Shape pill = new RoundRectangle2D.Double(node.x, node.y, node.width, node.height, node.height, node.height);
                g2d.setColor(node.kind == Kind.STOP ? new Color(0xF6D6D0) : new Color(0xE4E4E4));
                g2d.fill(pill);
                g2d.setColor(node.kind == Kind.STOP ? new Color(0xA03020) : LINE);
                g2d.draw(pill);
            }
            case ACTIVITY ->
            {
                Shape box = node.internal
                        ? new RoundRectangle2D.Double(node.x, node.y, node.width, node.height, 14, 14)
                        : new Rectangle.Double(node.x, node.y, node.width, node.height);
                g2d.setColor(node.internal ? INSIDEFILL : Color.WHITE);
                g2d.fill(box);
                g2d.setColor(node.internal ? INSIDEEDGE : LINE);
                g2d.draw(box);
            }
            case DECISION ->
            {
                Path2D diamond = new Path2D.Double();
                diamond.moveTo(node.centreX(), node.y);
                diamond.lineTo(node.x + node.width, node.y + node.height / 2);
                diamond.lineTo(node.centreX(), node.bottom());
                diamond.lineTo(node.x, node.y + node.height / 2);
                diamond.closePath();
                g2d.setColor(DECISIONFILL);
                g2d.fill(diamond);
                g2d.setColor(LINE);
                g2d.draw(diamond);
            }
        }
        g2d.setColor(Color.BLACK);
        g2d.setFont(this.font);
        double lh = lineHeight();
        double y = node.y + node.height / 2 - (node.lines.size() - 1) * lh / 2;
        for (String line : node.lines)
        {
            drawCentred(g2d, line, node.centreX(), y);
            y += lh;
        }
    }

    private void drawEdge(Graphics2D g2d, Edge edge)
    {
        Node from = edge.from();
        Node to = edge.to();
        g2d.setColor(LINE);
        g2d.setStroke(new BasicStroke(1.2f));
        Path2D path = new Path2D.Double();
        double endx;
        double endy;
        if (edge.back())
        {
            // down a little, out to the lane's left edge, up, and in to the start of the loop
            double x = Math.min(laneLeft(from.lane), laneLeft(to.lane)) + BACKROUTE;
            double below = from.bottom() + 10;
            double above = to.y - 10;
            path.moveTo(from.centreX(), from.bottom());
            path.lineTo(from.centreX(), below);
            path.lineTo(x, below);
            path.lineTo(x, above);
            path.lineTo(to.centreX(), above);
            path.lineTo(to.centreX(), to.y);
            endx = to.centreX();
            endy = to.y;
            // the condition written up the way back, beside it
            Graphics2D turned = (Graphics2D) g2d.create();
            turned.setFont(this.smallfont.deriveFont(Font.ITALIC));
            FontMetrics fm = turned.getFontMetrics();
            turned.translate(x - 3, (above + below) / 2 + fm.stringWidth(edge.label()) / 2.0);
            turned.rotate(-Math.PI / 2);
            turned.drawString(edge.label(), 0, 0);
            turned.dispose();
        } else if (edge.side())
        {
            // out of the diamond's right corner, down the lane's right edge, in from above
            double x = laneLeft(from.lane) + LANEWIDTH - NOROUTE;
            double above = to.y - 10;
            path.moveTo(from.x + from.width, from.y + from.height / 2);
            path.lineTo(x, from.y + from.height / 2);
            path.lineTo(x, above);
            path.lineTo(to.centreX(), above);
            path.lineTo(to.centreX(), to.y);
            endx = to.centreX();
            endy = to.y;
            label(g2d, edge.label(), from.x + from.width + 4, from.y + from.height / 2 - 4);
        } else if (this.nodes.indexOf(to) > this.nodes.indexOf(from) + 1)
        {
            // past the steps in between: down a little, round the right of
            // them, and in from above
            double x = laneLeft(to.lane) + LANEWIDTH - SKIPROUTE;
            double below = from.bottom() + 10;
            double above = to.y - 10;
            path.moveTo(from.centreX(), from.bottom());
            path.lineTo(from.centreX(), below);
            path.lineTo(x, below);
            path.lineTo(x, above);
            path.lineTo(to.centreX(), above);
            path.lineTo(to.centreX(), to.y);
            endx = to.centreX();
            endy = to.y;
            if (edge.label() != null)
            {
                label(g2d, edge.label(), from.centreX() + 4, from.bottom() + 11);
            }
        } else
        {
            // straight down, or down, across and down again into another lane
            double mid = (from.bottom() + to.y) / 2;
            path.moveTo(from.centreX(), from.bottom());
            if (Math.abs(from.centreX() - to.centreX()) > 1)
            {
                path.lineTo(from.centreX(), mid);
                path.lineTo(to.centreX(), mid);
            }
            path.lineTo(to.centreX(), to.y);
            endx = to.centreX();
            endy = to.y;
            if (edge.label() != null)
            {
                label(g2d, edge.label(), from.centreX() + 4, from.bottom() + 11);
            }
        }
        g2d.draw(path);
        arrowhead(g2d, endx, endy);
    }

    private void label(Graphics2D g2d, String text, double x, double y)
    {
        if (text == null) return;
        g2d.setFont(this.smallfont);
        g2d.drawString(text, (float) x, (float) y);
    }

    // pointing down, its tip at (x, y)
    private static void arrowhead(Graphics2D g2d, double x, double y)
    {
        Path2D head = new Path2D.Double();
        head.moveTo(x, y);
        head.lineTo(x - 4, y - 8);
        head.lineTo(x + 4, y - 8);
        head.closePath();
        g2d.fill(head);
    }

    private static void drawCentred(Graphics2D g2d, String text, double x, double y)
    {
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(text, (float) (x - fm.stringWidth(text) / 2.0),
                (float) (y + (fm.getAscent() - fm.getDescent()) / 2.0));
    }

    // what is on the chart, for tests: each node's kind, lane and text
    Map<String, String> describe()
    {
        Map<String, String> found = new LinkedHashMap<>();
        for (Node node : this.nodes)
        {
            found.put(String.join(" ", node.lines), node.kind + " in " + this.lanes.get(node.lane)
                    + (node.internal ? " (system)" : ""));
        }
        return found;
    }
}
