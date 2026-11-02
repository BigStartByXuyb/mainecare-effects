#!/usr/bin/env bash
# Verify the 0.1.0 GitHub Packages artifacts. Requires a token with read:packages.
set -euo pipefail

: "${GITHUB_ACTOR:?set GITHUB_ACTOR to your GitHub username}"
: "${GITHUB_TOKEN:?set GITHUB_TOKEN to a token with read:packages}"

OWNER="${GITHUB_REPOSITORY_OWNER:-BigStartByXuyb}"
BASE="https://maven.pkg.github.com/${OWNER}/mainecare-effects/com/mainecare/effects"

for artifact in motion glass deck; do
  for file in \
    "${artifact}-0.1.0.pom" \
    "${artifact}-0.1.0.aar" \
    "${artifact}-0.1.0.module" \
    "${artifact}-0.1.0-sources.jar" \
    "${artifact}-0.1.0-license.txt" \
    "${artifact}-0.1.0-notice.txt" \
    "${artifact}-0.1.0-third-party-notices.md"; do
    url="${BASE}/${artifact}/0.1.0/${file}"
    code=$(curl -sS -o /dev/null -w '%{http_code}' \
      -u "${GITHUB_ACTOR}:${GITHUB_TOKEN}" "${url}")
    printf '%-6s %-45s %s\n' "$artifact" "$file" "$code"
    test "$code" = "200"
  done
done
