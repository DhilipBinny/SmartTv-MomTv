---
description: Security rules enforced on all source code files
globs:
  - "src/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "lib/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "app/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "server/**/*.{ts,tsx,js,jsx,py,go,rs}"
  - "api/**/*.{ts,tsx,js,jsx,py,go,rs}"
---

# Security Rules

## Hardcoded Secrets
- NEVER hardcode API keys, tokens, passwords, or credentials in source code
- Use environment variables: `process.env.API_KEY`, `os.environ["API_KEY"]`
- If you spot an existing hardcoded secret, flag it immediately

## SQL Injection
- ALWAYS use parameterized queries with placeholders (`?`, `$1`, `:param`)
- NEVER use string interpolation in SQL:
  - BANNED: `` `SELECT * FROM users WHERE id = ${id}` ``
  - CORRECT: `db.query("SELECT * FROM users WHERE id = ?", [id])`

## Command Injection
- NEVER use `eval()`, `new Function()`, or `exec()` with dynamic input
- In Python: no `subprocess.run(shell=True)` with user data — pass args as a list
- In Node: no `child_process.exec()` with user input — use `execFile()` with args array

## Input Validation
- Validate at system boundaries: user input, API bodies, URL params, file uploads
- Allowlist approach: define what IS valid, reject everything else
- Never trust client-side validation alone

## XSS Prevention
- Sanitize user content before rendering in HTML
- Never insert raw user input into `innerHTML`, `document.write()`, or script tags
- In React: avoid `dangerouslySetInnerHTML` unless content is sanitized with DOMPurify

## Cryptography
- NEVER use MD5 or SHA1 for password hashing — use bcrypt, argon2, or PBKDF2
- Use TLS (https) for all external API calls
- Don't implement custom crypto — use well-tested libraries

## Error Exposure
- NEVER expose stack traces, internal paths, or DB errors to end users
- Log full details internally, return generic messages externally
