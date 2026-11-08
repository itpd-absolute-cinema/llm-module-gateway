# Week 6 Customer Meeting Script

**Target:** In one meeting, get the customer's verdict on the Week 5 open questions (plugin failure policy, cleanup and diagnostics, acceptance of the extension workflow), present the Docker Compose packaging and the DevOps-oriented demo, and record every decision as a `DEC-nnn`.

## Roles

- **Facilitator:** @b4lmor
- **Presenter:** @egozhuk
- **Note-taker:** @FallenChromium
- **Timekeeper:** @m1staken

## Agenda

| Time | Item | Owner |
| --- | --- | --- |
| 0:00–0:03 | Open, confirm the target and the agenda | @b4lmor |
| 0:03–0:10 | Show the Docker Compose packaging: one command starts the Gateway, Plugin Manager, and CLI; the version is explicit | @egozhuk |
| 0:10–0:20 | Show a DevOps pain point: plugin validation and failure isolation — upload a plugin that fails, show that the Gateway stays up and the request follows the configured policy | @egozhuk |
| 0:20–0:28 | Walk the plugin loading and execution path from the Gateway source with a one-slide map | @FallenChromium |
| 0:28–0:38 | Ask the open questions and record the customer's answers as `DEC-nnn` | @b4lmor |
| 0:38–0:43 | Ask for the customer's verdict on the stories shown today | @b4lmor |
| 0:43–0:45 | Close, confirm the next sprint's direction and action points | @b4lmor |

## Questions

1. **Plugin failure policy.** When a required plugin fails or times out, should the Gateway reject the request, skip the plugin, or use a fallback?
   *What it changes:* request recovery policy, the QRT for QR-02, and the plugin interface.

2. **Cleanup and diagnostics.** Should outer plugins receive exceptions for cleanup, and which diagnostics may reach the client?
   *What it changes:* cleanup behavior in the Plugin Manager, the client-visible error shape, and the observability SLI.

3. **Extension workflow acceptance.** Do the current examples meet the earlier minimum, and can the extension workflow be accepted against its criteria?
   *What it changes:* whether the plugin developer guide and the examples are marked accepted or need rework.

4. **DevOps value.** Which DevOps pain point should the next demo lead with: plugin validation, failure isolation, or deployment?
   *What it changes:* the order of the next sprint's stories and the features page.

## Working software to show

- Docker Compose up, one command, explicit version.
- A plugin upload and enable through the CLI.
- A request through the Gateway with a failing plugin, showing the configured failure policy and the Gateway still serving.
- The plugin loading and execution path in the Gateway source.

## Permission questions

- May we record this meeting?
- May we publish the sanitized transcript in the repository?
- May we share the transcript privately with the instructors if publication is refused?

## Close

- Confirm the `DEC-nnn` recorded for each answer.
- Confirm the action points with owners and due weeks.
- Agree the direction for SPRINT-07.