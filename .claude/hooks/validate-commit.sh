#!/bin/bash
#
# PreToolUse hook: validates git commit messages.
# Blocks commits that contain banned patterns.
# Exit 0 = allow, Exit 2 = block.
#

INPUT=$(cat)
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // empty' 2>/dev/null)

# Only check git commit commands
if ! echo "$COMMAND" | grep -qE '^git commit'; then
  exit 0
fi

# --- Banned Pattern: Co-Authored-By ---
if echo "$COMMAND" | grep -qi 'Co-Authored-By'; then
  echo "BLOCKED: 'Co-Authored-By' is not allowed in commit messages." >&2
  echo "Remove the Co-Authored-By line and try again." >&2
  exit 2
fi

# --- Banned Pattern: AI attribution ---
if echo "$COMMAND" | grep -qiE '(claude\.ai|Claude Code|Anthropic|noreply@anthropic|AI generated|Generated with)'; then
  echo "BLOCKED: AI attribution is not allowed in commit messages." >&2
  exit 2
fi

# --- Banned Pattern: Iteration/audit/pass references ---
if echo "$COMMAND" | grep -qiE '(iteration [0-9]|[0-9]+(st|nd|rd|th) (iteration|pass|attempt|audit|review)|first pass|second pass|third pass|found in.*(review|audit)|addressed in.*(review|audit|pass)|round [0-9])'; then
  echo "BLOCKED: Commit message contains iteration/audit/review-process references." >&2
  echo "Commit messages should describe WHAT changed and WHY, not which review pass found it." >&2
  exit 2
fi

# --- Banned Pattern: Vague messages ---
if echo "$COMMAND" | grep -qxiE "git commit -m ['\"]?(fix bugs?|updates?|changes?|misc|wip|stuff|things|cleanup)['\"]?"; then
  echo "BLOCKED: Commit message is too vague." >&2
  echo "Use format: '<imperative verb> <what changed>' — e.g., 'Fix TTS timeout with retry backoff'" >&2
  exit 2
fi

# All checks passed
exit 0
