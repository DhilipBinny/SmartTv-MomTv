#!/bin/bash
#
# PreToolUse hook: scan staged files for secrets before git commit.
# Uses gitleaks to detect API keys, tokens, passwords, private keys.
# Exit 0 = allow, Exit 2 = block.
#
# Supports all major languages: JS/TS, Python, Go, Rust, Java, C#, Ruby, PHP, etc.
# Requires: gitleaks installed (brew install gitleaks / go install github.com/gitleaks/gitleaks/v8@latest)
#

set -euo pipefail

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty' 2>/dev/null)

# Only check git commit commands
if ! echo "$COMMAND" | grep -qE '^git commit'; then
  exit 0
fi

# Block --no-verify attempts (bypasses git hooks)
if echo "$COMMAND" | grep -qE -- '--no-verify'; then
  echo "BLOCKED: --no-verify is not allowed. Pre-commit hooks must run." >&2
  exit 2
fi

# Check if gitleaks is installed
if ! command -v gitleaks &>/dev/null; then
  echo "WARNING: gitleaks is not installed. Install with: brew install gitleaks" >&2
  echo "Skipping secret scan." >&2
  exit 0
fi

# Check if there are staged changes to scan
if git diff --cached --quiet 2>/dev/null; then
  exit 0
fi

# Run gitleaks on staged changes (v8.19+ syntax)
GITLEAKS_OUTPUT=$(gitleaks git --pre-commit --staged --no-banner --redact 2>&1) || {
  GITLEAKS_EXIT=$?
  if [ "$GITLEAKS_EXIT" -eq 1 ]; then
    echo "BLOCKED: Gitleaks detected secrets in staged files. Remove secrets before committing." >&2
    echo "" >&2
    echo "$GITLEAKS_OUTPUT" >&2
    echo "" >&2
    echo "To fix: remove the secret from code and use environment variables instead." >&2
    echo "For false positives: add '# gitleaks:allow' inline or add fingerprint to .gitleaksignore" >&2
    exit 2
  fi
  echo "WARNING: Gitleaks scan failed (exit $GITLEAKS_EXIT). Blocking commit as a precaution." >&2
  echo "$GITLEAKS_OUTPUT" >&2
  exit 2
}

exit 0
