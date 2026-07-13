---
description: Ban debug artifacts from committed code
globs:
  - "src/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "lib/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "app/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "server/**/*.{ts,tsx,js,jsx,py,go,rs}"
---

# No Debug Artifacts

## Must Remove Before Committing
- `console.log()`, `console.debug()`, `console.dir()`, `console.trace()`
  - `console.warn()` and `console.error()` are OK for intentional logging
- `debugger` statements
- `alert()`, `confirm()`, `prompt()`
- Python `print()` debug statements (use logging module)
- `binding.pry`, `byebug`, `pdb.set_trace()`, `breakpoint()` (Ruby/Python)

## Commented-Out Code
- Delete it — git has the history
- Never commit blocks of commented-out code

## TODOs
- Bare `TODO` / `FIXME` without a ticket reference is not allowed
- Format: `// TODO(#123): description` or `// TODO(@user): description`
