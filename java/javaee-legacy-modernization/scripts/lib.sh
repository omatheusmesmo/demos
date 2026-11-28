#!/usr/bin/env bash
# Shared helpers for the demo scripts. Source it, do not run it.

DEMO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# The JDK major version the project declares in .sdkmanrc (8, 25, ...).
declared_jdk() {
    sed -n 's/^java=\([0-9]*\).*/\1/p' "$DEMO_DIR/.sdkmanrc"
}

# JAVA_HOME for a major version: $JDK<major>_HOME if set, else the newest SDKMAN install of that major.
jdk_home() {
    local major="$1" var="JDK${1}_HOME"
    if [[ -n "${!var:-}" ]]; then
        echo "${!var}"
        return
    fi
    local candidate
    candidate=$(ls -d "${SDKMAN_DIR:-$HOME/.sdkman}/candidates/java/${major}."* 2>/dev/null | grep -v -e mandrel -e graal | sort -V | tail -1)
    if [[ -z "$candidate" ]]; then
        echo "No JDK $major found. Install one (sdk install java <$major version>) or set $var." >&2
        return 1
    fi
    echo "$candidate"
}

# Run a command with the JDK of the given major version.
with_jdk() {
    local home
    home=$(jdk_home "$1") || return 1
    shift
    JAVA_HOME="$home" PATH="$home/bin:$PATH" "$@"
}

# Removes what an earlier build left in target/, without the Liberty runtime. Needed when moving between card
# branches: classes of deleted sources would stay in the war (and old test classes would still run), and a Liberty
# workarea built for other features (jakartaee-11.0 after javaee-8.0, or back) can start without security.
clean_build_outputs() {
    rm -rf "$DEMO_DIR"/target/classes "$DEMO_DIR"/target/test-classes "$DEMO_DIR"/target/invoicing \
        "$DEMO_DIR"/target/invoicing.war "$DEMO_DIR"/target/liberty/wlp/usr/servers/invoicing/workarea \
        "$DEMO_DIR"/target/liberty/wlp/usr/servers/invoicing/apps
}
