#!/usr/bin/env bash
# Usage: scripts/with-jdk.sh <major> <command...>   Runs the command with that JDK.
set -euo pipefail
source "$(dirname "$0")/lib.sh"
with_jdk "$@"
