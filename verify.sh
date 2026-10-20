#!/usr/bin/env bash
set -euo pipefail
repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$repository_root"
exec java tools/src/main/java/learning/verification/Verify.java "$@"
