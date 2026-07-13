---
description: Enforce API verification before writing integration code
globs:
  - "src/**/*.{ts,tsx,js,jsx}"
  - "lib/**/*.{ts,tsx,js,jsx}"
  - "app/**/*.{ts,tsx,js,jsx}"
  - "server/**/*.{ts,tsx,js,jsx}"
  - "api/**/*.{ts,tsx,js,jsx}"
---

# API & SDK Verification

When writing code that calls any external API or uses any SDK:

1. STOP before writing the call
2. WebSearch or WebFetch the official documentation
3. Verify:
   - Authentication method (Bearer token, API key header, OAuth, etc.)
   - Exact endpoint URL and HTTP method
   - Required vs optional parameters
   - Request body format (JSON, form-data, etc.)
   - Response schema and status codes
   - Known constraints (e.g., Anthropic: thinking blocks cannot include temperature)
4. Only then write the code

Common mistakes to avoid:
- Using outdated API versions from training data
- Wrong auth header names (Authorization vs X-Api-Key vs api-key)
- Assuming request/response shapes without checking
- Missing required parameters that were added after training cutoff
