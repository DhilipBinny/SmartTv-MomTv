# Project Development Standards

## 1. NEVER Assume — Always Read Code First

- ALWAYS read existing source files before making any change
- NEVER guess function signatures, API endpoints, request/response formats, or config shapes
- If unsure about any external API, look it up online using WebSearch/WebFetch BEFORE writing code
- Verify: Anthropic API constraints, third-party SDK signatures, internal project code

## 2. Git Commit Discipline

Format: `<imperative verb> <what changed>` — subject ≤72 chars, one logical change per commit.

**BANNED:** "Co-Authored-By", AI attribution, iteration/audit refs ("2nd pass", "found in review"), vague messages ("fix bugs", "updates", "WIP", "misc").

## 3. Verify API Details — Never Rely on Memory

When using ANY external API or SDK: WebSearch the official docs FIRST → confirm auth, headers, schema, rate limits, constraints → only then write code.

## 4. Mandatory Self-Review (2 Passes)

**Pass 1 — Correctness:** Does it work? Edge cases? Error paths? Types match reality?
**Pass 2 — Quality:** Modular? Constants extracted? Follows existing patterns? No duplicates? Complexity reasonable?

Only after BOTH passes report the task as complete.

## 5. Design Pattern Alignment & Modularity

- READ surrounding codebase before writing new code — follow existing patterns
- Functions under 50 lines, composition over inheritance, no monolithic files
- Extract shared logic into utilities, reusable components over one-off implementations

## 6. Constants & Configuration

- No magic strings/numbers in business logic — use constants files (per-module + global)
- Environment-specific values in env files, NOT hardcoded
- Enum-like values use actual enums or const objects

## 7. Documentation Updates

Update docs when touching: README (new features), API docs (endpoints), JSDoc (signatures), config docs (env vars), CLAUDE.md (constraints). If docs are stale for the area you touched, update them.

## 8. PR / Code Review Checklist

Every change must be self-reviewed against ALL dimensions:

- [ ] **CORRECTNESS** — Does it work? Edge cases? Error handling?
- [ ] **STABILITY** — Can it crash? Race conditions? Null safety?
- [ ] **MODULARITY** — Small focused units? Proper separation of concerns?
- [ ] **CONSTANTS** — No magic values? Config externalized?
- [ ] **DESIGN ALIGNMENT** — Follows existing patterns? No paradigm drift?
- [ ] **COMPLEXITY** — Efficient? No O(n^2) where O(n) works?
- [ ] **NO DUPLICATES** — Shared utilities used? No copy-paste?
- [ ] **SECURITY** — No hardcoded secrets? Input validated? No injection? No eval()?
- [ ] **TESTS** — New feature/fix has tests? No .only/.skip? Edge cases covered?
- [ ] **NO DEBUG ARTIFACTS** — No console.log/debugger? No commented-out code?
- [ ] **DEPENDENCIES** — Package justified? Stdlib checked? Bundle size OK?
- [ ] **ERROR HANDLING** — Boundaries covered? User errors helpful? No stack traces?
- [ ] **SECRETS SCAN** — Gitleaks passes on staged files?
- [ ] **DOCS** — README, API docs, comments reflect changes?

## 9. CI Must Be Green Before Merging

Check CI status before reporting PR as ready. If checks fail, fix and re-verify. NEVER mark ready while CI is red.

## 10. Security Basics

No hardcoded secrets — use env vars. Parameterized SQL queries only. No eval/exec with dynamic input. Validate input at system boundaries (allowlist approach). Sanitize output (XSS). Never expose stack traces to users. No MD5/SHA1 for passwords — use bcrypt/argon2/PBKDF2. See `.claude/rules/security.md` for full details.

## 11. Testing Requirements

New features MUST have tests. Bug fixes MUST include regression tests. Use AAA pattern (Arrange-Act-Assert), one behavior per test, no flow control in tests. Ban test.only/skip in commits. See `.claude/rules/testing.md` for full details.

## 12. No Debug Artifacts

Remove console.log/debug/dir/trace, debugger statements, commented-out code before committing. TODOs must have ticket refs: `// TODO(#123): description`. See `.claude/rules/no-debug-artifacts.md` for full details.

## 13. Dependency Discipline

Check stdlib first. Verify package exists (AI hallucinates names). Check health: last release, age, maintenance, vulnerabilities, license. Commit lock files, never edit manually. One lock file per project.

## 14. Error Handling at Boundaries

User-facing: helpful messages, no internals. Consistent API error format. Graceful degradation. Retry with backoff for transient failures (max 3, no retry on 4xx).

## 15. Secret Leak Prevention (Gitleaks)

Gitleaks scans staged files automatically before every commit (222 patterns). Blocked commit → remove secret, use env vars. False positives: `// gitleaks:allow` inline or `.gitleaksignore`. Config: `.gitleaks.toml` at project root.
