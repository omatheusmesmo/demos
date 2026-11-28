#!/usr/bin/env bash
# Merges scripts/bob-approval.json into the approval block of IBM Bob's global settings, so the demo runs without
# an approval click: builds, checks, git, gh, edits and the usual shell tools, with no deny list. Bob reads approvals
# only from its global settings, never from the project, so they apply to every workspace. The rest of the settings
# file (hooks, migrations) is kept, and a backup is written next to it.
# Close Bob before running it: Bob rewrites the file while it runs.
set -euo pipefail
cd "$(dirname "$0")"

settings="${BOB_SETTINGS:-$HOME/.bob/settings/settings.json}"
mkdir -p "$(dirname "$settings")"
[[ -f "$settings" ]] || echo '{}' > "$settings"

backup="$settings.bak-demo-$(date +%Y%m%d%H%M%S)"
cp "$settings" "$backup"

merged=$(jq --slurpfile demo bob-approval.json '.approval = ((.approval // {}) + $demo[0])' "$settings")
printf '%s\n' "$merged" > "$settings"

echo "Bob approvals updated in $settings"
echo "Backup of the previous settings: $backup"
