# Quality Requirements

Priority matrix of active quality requirements. Sorted by: active first, then Business Importance (High → Medium → Low), then Technical Risk (High → Medium → Low).

`Active` means in scope, not verified. Prototype offline API checks do not establish the planned CLI integration, performance, or configurable plugin-failure policy targets.

| ID | Characteristic / Sub-characteristic | Business Importance | Technical Risk | Outcome (short) | Status | File |
| --- | --- | --- | --- | --- | --- | --- |
| QR-02 | Reliability / Fault Tolerance | High | High | Plugin failure completes per policy; Gateway stays up | Active | [QR-02](qr/QR-02.md) |
| QR-01 | Functional Suitability / Functional Completeness | Medium | Medium | 100/100 requests hit all enabled plugins once per path; 0/100 hit disabled | Active | [QR-01](qr/QR-01.md) |
| QR-03 | Maintainability / Testability | High | Medium | Fast plugin development | Active | [QR-03](qr/QR-03.md) |
| QR-04 | Security / Authenticity | High | Medium | Unauthorized administration is rejected without saved-state changes | Active | [QR-04](qr/QR-04.md) |
| QR-05 | Reliability / Fault Tolerance | Medium | High | Admin updates preserve runtime; recoverable storage failures preserve saved state | Active | [QR-05](qr/QR-05.md) |

Test status for each quality requirement is tracked in the [Quality Requirement Testing Plan](../project-plan.md#quality-requirement-testing-plan).