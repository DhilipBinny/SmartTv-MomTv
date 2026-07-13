---
description: Self-review checklist applied after every code change
globs:
  - "**/*.{ts,tsx,js,jsx,py,go,rs}"
---

# Mandatory Self-Review Checklist

After completing any code change, run through these checks in TWO passes:

## Pass 1 — Correctness
- Does it do exactly what was requested?
- Are edge cases handled (empty arrays, null, undefined, zero, negative)?
- Are error paths covered (network failure, invalid input, timeout)?
- Do types accurately represent the data (no `any`, no wrong generics)?
- Are async operations properly awaited?

## Pass 2 — Quality
- CORRECTNESS: Logic is sound, tested mentally or with actual tests
- STABILITY: No crashes on unexpected input, no race conditions
- MODULARITY: Small focused units, proper separation of concerns
- CONSTANTS: No magic values hardcoded in business logic
- DESIGN ALIGNMENT: Follows codebase patterns, no paradigm drift
- COMPLEXITY: Time/space complexity is reasonable for the data size
- NO DUPLICATES: Shared utilities used, no copy-paste code
- DOCS: README/comments/API docs updated if interface changed

If any check fails, fix it before reporting the task as done.
