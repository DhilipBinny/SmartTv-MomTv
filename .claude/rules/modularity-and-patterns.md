---
description: Enforce modular design and pattern consistency
globs:
  - "src/**/*.{ts,tsx,js,jsx}"
  - "lib/**/*.{ts,tsx,js,jsx}"
  - "app/**/*.{ts,tsx,js,jsx}"
  - "components/**/*.{ts,tsx,js,jsx}"
---

# Modularity & Design Pattern Rules

## Before Writing Code
1. Read surrounding files to identify the design patterns in use
2. Follow those patterns — do NOT introduce a new paradigm without explicit approval
3. If the codebase uses hooks, use hooks. If it uses services, use services. Match the style.

## Code Structure
- Functions/methods: under 50 lines. If longer, split by responsibility.
- One export per file for components. Utilities can have multiple named exports.
- No god files — split by domain/feature, not by type (avoid "utils.ts" dumping grounds)

## Reusability
- Before writing a new helper, search for existing ones that do the same thing
- Extract repeated logic (3+ occurrences) into shared utilities
- Use composition: small focused pieces combined, not large monolithic functions

## Constants
- No magic strings or numbers in logic
- Per-module: `constants.ts` next to the module
- Global/shared: `src/constants/` or `lib/constants/`
- Environment values: `.env` files only, accessed through a config module
