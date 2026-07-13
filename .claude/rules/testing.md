---
description: Testing requirements for all code changes
globs:
  - "src/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "lib/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "app/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "test/**/*"
  - "tests/**/*"
  - "**/*.test.*"
  - "**/*.spec.*"
---

# Testing Rules

## When Tests Are Required
- New features and service methods MUST have tests
- Bug fixes MUST include a regression test
- Auth/permission flows ALWAYS need tests

## Test Structure
- Arrange-Act-Assert (AAA) pattern in every test
- One logical behavior per test
- No flow control in tests: no `if`, `for`, `while`, `switch`
- Tests must be independent and deterministic

## Banned in Commits
- `test.only()`, `it.only()`, `describe.only()`, `fdescribe()`, `fit()`
- `test.skip()`, `it.skip()`, `describe.skip()`, `xit()`, `xtest()`
- Remove these before committing — they cause silent CI gaps

## What to Test
- Business logic, validation rules, error conditions
- Edge cases: null, empty, zero, negative, boundary values
- User-facing behavior, NOT implementation details

## What NOT to Test
- Trivial getters/setters with no logic
- Framework internals
- Private methods (test through public API)
