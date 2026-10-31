# myELH: related formalisms and the plan from here

A review, made in October 2026, of whether myELH reinvents existing
notations, which of them have engines for checking or validation, and what
to build next.

## The model so far

- A **project** manages **entities** and **events**.
- An entity has a **life history**: a tree of nodes, each with a
  **structure** (in sequence, one of (selection), repeated (iteration),
  parallel roles). Its leaf nodes are **actions**; an action has a
  description, and, as its part in an event, a condition and a result.
- An **event** happens in the world and triggers an action in each of two
  or more entities. It has a description and a **process**: how it happens,
  as **activities** written under the **role** that does them. An activity
  marked `>` happens inside the information system; any other happens
  outside it, in the world.

  ```
  Member:
      fill in the application form
  CounterStaff:
      check the customer's id
      IF id not valid
          refuse the application
          STOP
      END
      > create Member record
      > register name, age and address
      > issue membership number and card
  ```

  `IF ... / ELSE / END`, `REPEAT ... / END` and `STOP` give the flow; `--`
  starts a comment. Every activity's role and every `Role.attribute`
  reference, including those in IF and REPEAT conditions, is checked; an
  entity outside the system can act in the world but not inside it.

## How it maps onto existing formalisms

| myELH | SSADM | UML / BPMN | Formal methods |
|---|---|---|---|
| Entity life history (sequence / selection / iteration / parallel roles) | Entity Life History (Jackson notation) | State machine | CSP process; regular expression / finite automaton |
| Event triggering actions in several entities | Event and its effects | Signal received by several objects | Shared, synchronised event (CSP) |
| Event / entity matrix | Entity-Event Matrix | | |
| An event with its actions in each entity | Effect Correspondence Diagram | Sequence / collaboration diagram | Parallel composition |
| An action's condition and result | Effect operations; valid prior states | OCL pre- and postconditions | Event-B guards and actions |
| Event process: roles, activities, IF, REPEAT | Update process model | Activity diagram / BPMN with swimlanes | |
| Activity inside (`>`) or outside the system | System boundary on a DFD | BPMN user/service task vs manual task | |
| CORD SysStatus, the last action suffered | State indicator | The object's current state | State variable |

The notation is essentially SSADM / Jackson (JSD). The modern formalisms
each cover a part of it; none covers the whole.

## Formalisms with engines

- **CSP, with FDR or ProB.** Each life history is a CSP process; an event
  shared by several entities is a synchronised event. The checker finds
  deadlocks (an event that can never happen because a participant is never
  in a state to accept it), unreachable actions and inconsistent orderings
  across entities. The best fit for checking life histories together.
- **Event-B, with Rodin and ProB.** State variables (attributes, state
  indicators) and events with guards (conditions) and actions (results);
  Rodin proves that events preserve invariants, ProB animates and model
  checks. The best fit once attributes exist; the heaviest to learn.
- **BPMN, with Camunda, Flowable or bpmnlint.** Lanes are roles, gateways are
  IFs, and task types already separate manual (outside) from user or service
  (inside) tasks. Checks or runs the process, but not against the life
  histories.
- **UML and OCL, with the USE tool.** Classes, attributes, and OCL pre- and
  postconditions on operations, checked.
- **Alloy** and **TLA+** (TLC): lightweight model checkers, further from the
  notation.
- Event Storming and Event Modeling are close in spirit but informal, with
  no checking engines.

## Decision

Keep myELH's own notation, which is SSADM / JSD in form and easier to write
than any of the above, but adopt their concepts and vocabulary on purpose.
Make the cheap, deterministic checks in myELH itself; for deep checking,
export to an existing engine rather than write one.

## Plan

Steps 1 to 3 were done in October 2026.

1. **State indicators, derived from the life histories (SSADM style).**
   Each entity has a state indicator. Each action gets a *valid prior* set
   of values (where the entity must be before it) and a *set to* value
   (where it leaves the entity), worked out from its place in the
   sequence / selection / iteration structure. This gives CORD's SysStatus,
   and the first real check: whether an event is possible, given the state
   each participant must be in.
2. **The event flowchart.** The process drawn in the Events window's
   diagram view: a swimlane per role, decisions as diamonds, activities
   inside the system in their own style; exported as SVG.
3. **Attributes.** A list of attributes per entity (its data model);
   conditions, results and activities inside the system can name them, and
   the app checks that they exist. As built: entities and roles each have
   attributes (name, type, key, note; a reference names another entity),
   a role can also name its entity's, and each has its state indicator as
   `state`. They are named `Role.attribute` (`Library Member.membership
   number`); unknown roles or attributes in an event's process and in
   conditions and results are flagged, with the nearest known name
   suggested, and attributes a process names that are missing can be added
   from the Events window ("Add Missing Attributes").

Later:

4. **Export to CSP** (life histories as processes, events synchronised) for
   deadlock and reachability checks with FDR or ProB; possibly Event-B once
   attributes exist, and BPMN or Mermaid for showing processes in other
   tools.
