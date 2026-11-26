#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
expected=bebc061de2c379d921d9132bcd9268dc7a0fc65ff6d1b1d9acf1c7ab3b7647b3
bank=arena-vocabulary-v1.json
if [ -f "$bank" ] && echo "$expected  $bank" | sha256sum -c --status; then
  echo "Verified existing Arena dictionary."
else
  tmp=$(mktemp -d)
  trap 'rm -rf "$tmp"' EXIT
  curl --fail --location --retry 3 --max-time 300 --proto '=https' --proto-redir '=https' \
    'https://github.com/ineklemihribanse-design/WordQuestAI/releases/download/v1.0.148/WordQuestAI.apk' -o "$tmp/published.apk"
  unzip -p "$tmp/published.apk" assets/arena-vocabulary-v1.json > "$tmp/$bank"
  echo "$expected  $tmp/$bank" | sha256sum -c
  mv "$tmp/$bank" "$bank"
fi
node --input-type=module -e 'import fs from "node:fs"; import {loadVocabulary,vocabularyReport} from "./vocabulary_bank.mjs"; const r=vocabularyReport(loadVocabulary(JSON.parse(fs.readFileSync("arena-vocabulary-v1.json","utf8")))); if(r.count!==1575)throw Error("Unexpected bank count"); console.log(JSON.stringify(r));'
