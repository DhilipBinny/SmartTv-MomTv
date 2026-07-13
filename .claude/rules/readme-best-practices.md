---
description: README writing standards for public repositories
globs:
  - "README.md"
  - "readme.md"
---

# README Best Practices for Public Repositories

Every public repo README must follow this structure. The goal is: a stranger lands on the repo and knows what it does, why it exists, how to use it, and how to contribute — in under 2 minutes.

## Structure (in order)

### 1. Hero Section (first screenful — most important)
- **Hero image or screenshot** — centered, max width 720px. Show the product, not a logo.
- **Project name** — `<h1>` centered
- **One-line pitch** — bold, explains what it does in one sentence. No jargon.
- **Three-word value props** — separated by `·` or `<br>`. Example: "Zero ads. Zero tracking. Zero config."
- **Badge row** — shields.io badges for: language/framework version, license, build status, min platform version

```markdown
<p align="center">
  <img src="screenshots/hero.png" alt="App Name" width="720">
</p>
<h1 align="center">Project Name</h1>
<p align="center">
  <strong>One sentence explaining what this does.</strong>
  <br>Short technical descriptor. Short value prop.
</p>
<p align="center">
  <img src="https://img.shields.io/badge/..." alt="badge">
</p>
```

### 2. Why (motivation — 3-5 sentences max)
- What problem does this solve?
- Who is it for?
- Why should anyone care?
- Write like a human, not a spec. Story > feature list.

### 3. Screenshots (2-4, grid layout)
- Use an HTML `<table>` with 2 columns for side-by-side
- Each screenshot gets a `<sub>` caption below it
- Show the product in use, not configuration screens
- Landscape screenshots for TV/desktop apps, portrait for mobile
- Max width 400px per cell

```markdown
<table>
  <tr>
    <td><img src="screenshots/feature1.png" width="400"><br><sub>Caption</sub></td>
    <td><img src="screenshots/feature2.png" width="400"><br><sub>Caption</sub></td>
  </tr>
</table>
```

### 4. Features (categorized bullet list)
- Group by theme (Core, UX, System, API, etc.)
- Each bullet: **Bold feature name** — one-line description
- Include a "What It Doesn't Have (By Design)" section if the project is intentionally minimal
- Use `###` subheadings for categories, not numbered lists

### 5. Requirements / Prerequisites
- Table format: Component | What You Need
- Include tested devices/platforms/versions
- Be specific about hardware if applicable

### 6. Installation (copy-paste ready)
- Every command in a fenced code block with language tag
- Start with the quickest path (download binary, npm install, etc.)
- Then "Build from Source" with exact steps
- Include environment variables needed
- Platform-specific instructions in separate subsections

### 7. Usage / Quick Start
- The minimum steps to go from installed to working
- Include a table for keyboard shortcuts / CLI commands / API endpoints
- Show real examples, not abstract ones

### 8. Tech Stack (short list)
- Language, framework, key libraries — one line each
- Architecture pattern in one line
- Min/target platform versions

### 9. Project Structure (tree view)
- Only show the important files, not everything
- Add a one-line comment for each file explaining its purpose
- Use a fenced code block with no language tag

```
src/
├── main.kt          # Entry point
├── ui/              # All UI components
└── data/            # Data layer
```

### 10. Contributing
- Fork → branch → commit → PR (4 steps max)
- Link to CONTRIBUTING.md for details if needed
- Don't over-engineer this section for small projects

### 11. License
- One line: "MIT License. See [LICENSE](LICENSE) for details."
- Include the actual LICENSE file in the repo root

### 12. Footer (optional, tasteful)
- One centered line that adds personality
- No emoji spam, no "made with love", no self-promotion
- Example: "Built with care for moms everywhere who just want to watch TV."

## Rules

### DO
- **Write for scanners** — headers, bullets, tables. Nobody reads paragraphs on GitHub.
- **Front-load value** — hero screenshot + pitch must fit in one screen without scrolling
- **Make every command copy-paste ready** — no "replace X with your value" without an example
- **Test your screenshots in both GitHub light and dark mode**
- **Use relative paths** for images (`screenshots/home.png` not absolute URLs)
- **Keep badges under 6** — more than that is noise
- **Update screenshots when UI changes** — stale screenshots are worse than none

### DON'T
- Don't start with a logo. Start with a screenshot of the actual product.
- Don't write "Table of Contents" for READMEs under 300 lines — GitHub auto-generates one.
- Don't use emoji as section headers (📦 Installation) — it looks dated.
- Don't explain how Git works in Contributing — your audience uses GitHub.
- Don't list every single file in Project Structure — only the ones that matter.
- Don't put deployment/production instructions in README — use a separate DEPLOYMENT.md.
- Don't write "This project is..." — show, don't tell. The screenshot and features list tell.

## Badge Reference (shields.io)

```markdown
<!-- Language/Framework -->
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5.0-3178C6?logo=typescript&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.12-3776AB?logo=python&logoColor=white)
![Next.js](https://img.shields.io/badge/Next.js-15-000000?logo=nextdotjs&logoColor=white)

<!-- Platform -->
![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-16+-000000?logo=apple&logoColor=white)
![Node](https://img.shields.io/badge/Node-20+-339933?logo=nodedotjs&logoColor=white)

<!-- License -->
![MIT](https://img.shields.io/badge/License-MIT-blue)
![Apache](https://img.shields.io/badge/License-Apache_2.0-blue)

<!-- Build -->
![Build](https://img.shields.io/github/actions/workflow/status/USER/REPO/ci.yml?label=build)
![Release](https://img.shields.io/github/v/release/USER/REPO)
```

## Quality Checklist

Before merging a README:

- [ ] Hero screenshot is current and shows the product working
- [ ] A stranger can understand what this does in 10 seconds
- [ ] All code blocks are copy-paste ready (no broken commands)
- [ ] All links work (especially LICENSE, screenshots)
- [ ] Renders correctly on GitHub (check both light/dark mode)
- [ ] No stale version numbers or dates
- [ ] Installation instructions tested from a clean clone
