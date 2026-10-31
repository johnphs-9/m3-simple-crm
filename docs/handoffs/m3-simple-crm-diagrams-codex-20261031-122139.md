# Repository diagram handoff

Created from fresh inspection of the current repository on 31 October 2026 (Asia/Singapore). Existing diagrams were not used as architectural evidence.

## Deliverables

Seven diagrams are saved in `docs/design/`, each as a native `.drawio` source and a `.drawio.svg` export with embedded editable data.

- [m3-simple-crm-class-entities-codex-20261031-122139](../design/m3-simple-crm-class-entities-codex-20261031-122139.drawio.svg)
- [m3-simple-crm-sequence-customer-changes-codex-20261031-122139](../design/m3-simple-crm-sequence-customer-changes-codex-20261031-122139.drawio.svg)
- [m3-simple-crm-sequence-customer-queries-codex-20261031-122139](../design/m3-simple-crm-sequence-customer-queries-codex-20261031-122139.drawio.svg)
- [m3-simple-crm-sequence-interaction-create-codex-20261031-122139](../design/m3-simple-crm-sequence-interaction-create-codex-20261031-122139.drawio.svg)
- [m3-simple-crm-sequence-login-codex-20261031-122139](../design/m3-simple-crm-sequence-login-codex-20261031-122139.drawio.svg)
- [m3-simple-crm-sequence-protected-access-codex-20261031-122139](../design/m3-simple-crm-sequence-protected-access-codex-20261031-122139.drawio.svg)
- [m3-simple-crm-technical-architecture-codex-20261031-122139](../design/m3-simple-crm-technical-architecture-codex-20261031-122139.drawio.svg)

## Source evidence

| View | Executable evidence |
|---|---|
| Architecture | `pom.xml`, `M3SimpleCrmApplication`, `SecurityConfig`, controllers, service/repository implementations and `application*.properties` |
| Login | `auth/AuthController.java:41`, `LoginRequest`, `TokenResponse`, `security/JwtService.java` |
| Protected access | `security/JwtAuthFilter.java`, `config/SecurityConfig.java:48`, `HelloController` and `HelloApiSecurityTest` |
| Customer queries | `controller/CustomerController.java:82`, `:95`, `:152`, `:158`, `CustomerServiceImpl`, `CustomerRepository` |
| Customer mutations | `controller/CustomerController.java:70`, `:111`, `:124`, `service/CustomerServiceImpl.java:46`, `:82`, `:101` |
| Interaction creation | `controller/CustomerController.java:139`, `service/CustomerServiceImpl.java:109` |
| Entity model | `model/Customer.java`, `Interaction.java:34`, `Student.java:54`, `Course.java` |
| Error behavior and startup | `GlobalExceptionHandler`, `DataLoader.java:21`, `application-dev.properties:19` |

Source paths above are relative to `m3-simple-crm/src/main/java/sg/edu/ntu/m3_simple_crm/`, except Maven, resource and test files. Noncompiled `.old` files and stale source comments were not treated as implementation.

## Decisions

- Keep views high-level. Collapse Spring Data JPA, Hibernate and database access into repository participants in sequences.
- Show Basic and JWT authentication as complementary mechanisms. Configured login credentials are separate from the in-memory BCrypt user store.
- Document 401/403 security failures separately from MVC ErrorResponse mapping.
- Preserve repository return types, including Optional<Customer> before service-level not-found handling.
- Show entity associations and multiplicities without inventing composition or cascade deletion.
- Include Student/Course as implemented entities while noting the absence of their REST APIs.
- Include dev startup table recreation and reseeding. No deployment architecture is inferred.

## Validation

- All seven source files and SVGs parsed as XML.
- Draw.io Desktop 32.6.1 exported every SVG, then reopened each SVG and exported uncompressed native XML.
- Verified native cell IDs, label values, edge source and target IDs match the source, embedded SVG model and reopened model for all seven diagrams.
- Ran the repository SVG skill checker on every SVG. All passed.
- Rendered all SVGs with Sharp and inspected all complete previews. Corrected clipped notes, label/lifeline intersections and a connector crossing.
- Final architecture preview was inspected after its last routing change.
- Source model text is at least 14px in Helvetica Neue. Technical palette follows the repository SVG skill with blue headers, cream notes, rose caveats and paper backgrounds.
- Added SVG titles and descriptions. Removed the exporter viewer-warning footer. Embedded editable diagram data was preserved.
- SVGs are self-contained. Draw.io supplies embedded raster text fallbacks for renderers without foreignObject support.
- Inspected relevant existing tests as corroborating evidence. Application tests were not run, since this work changes documentation only.

## Scope and remaining work

Application source and existing staged changes were preserved. No implementation work or required diagram checks remain. If runtime behavior changes, re-trace the relevant source before revising these diagrams.
