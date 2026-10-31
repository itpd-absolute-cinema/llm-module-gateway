# Sprint NN Retrospective

## What Went Well

| What went well |
| --- |
| The team developed the CLI, establishing a command-line interface for working with the system. |
| The Gateway and plugin manager were implemented, delivering the main integration components of the system. |
| The team tested the system, expanded the test suite, and added CI to run the tests automatically. |
| The team validated the solution with the customer and deployed it to a university virtual machine. |

## What Did Not

Actually, this sprint was good, couldn't say that something went wrong.

## Actions

| Action | Owner | Impact | Reason | Carried |
| --- | --- | --- | --- | --- |
| Add end-to-end CI tests covering the main flow through the CLI, Gateway, and plugin manager. | `b4lmor, fallenchromium` | High | Component tests do not by themselves verify that the integrated system works as a whole. | Yes |
| Document the university VM deployment steps and add a post-deployment smoke-test checklist. | `b4lmor, fallenchromium` | Medium | The deployment has been verified on one VM, but its assumptions and repeatability should be made explicit. | Yes |
| Turn customer validation feedback into explicit acceptance checks and include those checks in the test suite where practical. | `b4lmor, fallenchromium` | High | Customer validation is most useful when its outcomes become repeatable checks for future changes. | Yes |

## Carried Actions

None: this is the first sprint retrospective, so there is no previous retrospective to carry actions from.
