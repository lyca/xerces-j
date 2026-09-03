<!-- SPDX-License-Identifier: Apache-2.0
      https://www.apache.org/licenses/LICENSE-2.0 -->

# Agent Rules: Xerces-J

**Core & Tech Stack**
- **Java 8 STRICT**: No `var`, records, text blocks (`source=1.8`, `target=1.8`).
- **Tests**: Use JUnit 5 (JUnit Jupiter) exclusively.
- **Architecture**: `xerces-impl/` implements W3C DOM/SAX. Do NOT break public API contracts.

**Build (`./mvnw` ONLY - no global `mvn`)**
- All tests: `./mvnw clean test`
- Single test: `./mvnw -Dtest=<Class> -pl xerces-impl test`
- Fast build (no tests): `./mvnw clean install -DskipTests`

**Structure**
- `xerces-impl/`: Core logic (DOM, SAX)
- `xerces-samples/`: Example code
- `xerces-tools/`: XML/Schema utilities
- `xerces-benchmarks/`: Performance testing

**PRs, Commits & Governance**
- **License**: Insert ASF 2.0 header in ALL new `.java`/`.xml` files.
- **Attribution**: Autonomous agent PRs MUST append footer: `\n---\nGenerated-by: <Agent>`
- **Authorship**: NO AI in `Co-Authored-By`. Humans act as authors.
- **Commits**: USE Conventional Commits (`feat:`, `fix:`, etc.). Keep messages COMPACT. Imperative mood. Explain *why*.
- **Security**: NO secrets. NO new external dependencies outside `test` scope without user consent.