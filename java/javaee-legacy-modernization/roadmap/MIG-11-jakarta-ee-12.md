---
id: MIG-11
title: Move to Jakarta EE 12 once it is released
priority: blocked
depends_on: [MIG-08]
runs_on: 25
estimate_bobcoins: "?"
cap_bobcoins: "?"
---

## Why

Jakarta EE 12 is not released yet (target announced for February 2027). Open Liberty only has an internal
placeholder feature and OpenRewrite has no recipe yet. Once it ships, the jump is `jakarta` to `jakarta`: a version
bump, not a namespace change.

## Acceptance

Not defined until the platform, the Liberty feature and the recipe exist. No checks file on purpose.
