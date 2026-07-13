---
description: Generate a professional README.md for the current project following best practices
---

# /readme — Generate Professional README

Read `.claude/rules/readme-best-practices.md` for the full reference. Then:

1. **Scan the project** — read the source tree, package.json/build.gradle/Cargo.toml/pyproject.toml, existing README if any
2. **Identify** — project name, language, framework, what it does, who it's for
3. **Capture screenshots** if it's a UI project and a dev server or device is running
4. **Write README.md** following the exact structure from the best practices rule:
   - Hero section (screenshot + pitch + badges)
   - Why (motivation)
   - Screenshots (grid)
   - Features (categorized)
   - Requirements
   - Installation (copy-paste ready)
   - Usage / Quick Start
   - Tech Stack
   - Project Structure
   - Contributing
   - License
5. **Create LICENSE** file if missing (default MIT unless user specifies otherwise)
6. **Update repo description** via `gh repo edit` if it's a GitHub repo

## Arguments

- No args: generate README for current project
- `update`: update existing README with current project state
- `audit`: check existing README against best practices and report gaps
