#!/usr/bin/env bash
set -euo pipefail

# One pinned formatter for production and test Java in both executable services.
mode="${1:---check}"
if [[ "$#" -gt 1 || ("$mode" != "--check" && "$mode" != "--write") ]]; then
    echo "Usage: bash scripts/format-java.sh [--check|--write]" >&2
    exit 2
fi

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
formatter_version="1.35.0"
formatter_sha256="bfb7f9ead6cd328389bc2da53860443bc0e805dfd08cc889bfdf43b26cb2a6e8"
formatter_cache="${JAVA_FORMAT_CACHE:-${XDG_CACHE_HOME:-$repository_root/.cache}/engineering-learning/java-format}"
formatter_jar="$formatter_cache/google-java-format-$formatter_version.jar"

if [[ ! -f "$formatter_jar" ]]; then
    mkdir -p "$formatter_cache"
    download="$(mktemp "$formatter_cache/download.XXXXXX")"
    trap 'rm -f -- "$download"' EXIT
    curl --fail --location --silent --show-error \
        "https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/$formatter_version/google-java-format-$formatter_version-all-deps.jar" \
        --output "$download"
    printf '%s  %s\n' "$formatter_sha256" "$download" | shasum -a 256 --check --status
    mv "$download" "$formatter_jar"
fi
printf '%s  %s\n' "$formatter_sha256" "$formatter_jar" | shasum -a 256 --check --status

cd "$repository_root"
sources=()
while IFS= read -r -d '' source; do
    sources+=("$source")
done < <(find ai-engineering tools/src -type f -name '*.java' -not -path '*/build/*' -not -path '*/node_modules/*' -not -path '*/.gradle/*' -not -path '*/source-notes/*' -print0)

formatter_options=(--aosp --skip-reflowing-long-strings)
if [[ "$mode" == "--check" ]]; then
    formatter_options+=(--dry-run --set-exit-if-changed)
else
    formatter_options+=(--replace)
fi
java -jar "$formatter_jar" "${formatter_options[@]}" "${sources[@]}"
