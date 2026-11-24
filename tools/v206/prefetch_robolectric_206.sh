#!/usr/bin/env bash
# Provision the exact Robolectric SDKs before test workers start.
# Check Maven Central's published checksum and ZIP validity even on cache hits.
set -euo pipefail
dir="${1:?Robolectric dependency directory required}"
mkdir -p "$dir"
versions=(14-robolectric-10818077-i7 15-robolectric-12650502-i7)
repos=(https://repo.maven.apache.org/maven2 https://repo1.maven.org/maven2 https://maven-central.storage-download.googleapis.com/maven2)
valid() {
  local jar="$1" checksum="$2" digest
  test -s "$jar" && test -s "$checksum" || return 1
  digest="$(awk 'NR==1 {print $1}' "$checksum")"
  [[ "$digest" =~ ^[a-fA-F0-9]{40}$ ]] || return 1
  printf '%s  %s\n' "$digest" "$jar" | sha1sum -c --status || return 1
  unzip -tq "$jar" >/dev/null || return 1
}
for version in "${versions[@]}"; do
  name="android-all-instrumented-$version.jar"
  jar="$dir/$name"
  if valid "$jar" "$jar.sha1"; then
    echo "PASS cached verified SDK $version"
    continue
  fi
  rm -f "$jar" "$jar.sha1" "$jar.part" "$jar.sha1.part"
  path="org/robolectric/android-all-instrumented/$version/$name"
  ok=false
  for repo in "${repos[@]}"; do
    echo "Fetching Robolectric $version from $repo"
    if curl -fLsS --retry 3 --retry-all-errors --retry-delay 3 --connect-timeout 20 --max-time 120 "$repo/$path.sha1" -o "$jar.sha1.part" &&
       curl -fLsS --retry 3 --retry-all-errors --retry-delay 3 --connect-timeout 20 --max-time 240 "$repo/$path" -o "$jar.part" &&
       valid "$jar.part" "$jar.sha1.part"; then
      mv "$jar.part" "$jar"
      mv "$jar.sha1.part" "$jar.sha1"
      ok=true
      break
    fi
    rm -f "$jar.part" "$jar.sha1.part"
  done
  if [ "$ok" != true ]; then
    echo "::error::Unable to obtain verified Robolectric SDK $version; tests cannot safely run."
    exit 1
  fi
  echo "PASS downloaded and verified SDK $version"
done
echo "PASS all required Robolectric Android SDK jars verified before tests"
