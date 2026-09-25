#!/usr/bin/env bash
# Lance le test de fumée sur plusieurs serveurs, l'un après l'autre, et résume le tout dans .smoke/results.md.
# Usage : HUB_ACCEPT_EULA=true scripts/smoke-matrix.sh [projet:version ...]
#   sans argument : la liste par défaut ci-dessous.
set -uo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
targets=("$@")
if [ "${#targets[@]}" -eq 0 ]; then
  targets=(paper:1.8.8 paper:1.12.2 paper:1.16.5 paper:1.18.2 paper:1.20.4 paper:1.21.11 paper:26.1.2 folia:1.21.11 folia:26.1.2)
fi
out="$root/.smoke/results.md"
mkdir -p "$root/.smoke"
{
  echo "| Serveur | Java | Résultat | Durée |"
  echo "| --- | --- | --- | --- |"
} > "$out"
status=0
for t in "${targets[@]}"; do
  project="${t%%:*}"; version="${t##*:}"
  start=$(date +%s)
  log="$root/.smoke/$project-$version.summary.log"
  "$root/scripts/smoke-test.sh" "$project" "$version" > "$log" 2>&1
  code=$?
  elapsed=$(( $(date +%s) - start ))
  java="$(sed -n 's/^== .* avec //p' "$log" | head -1)"
  if [ "$code" -eq 0 ]; then res="réussi"; else res="**ÉCHEC**"; status=1; fi
  echo "| $project $version | ${java:-?} | $res | ${elapsed} s |" >> "$out"
  echo "$project $version : $res (${elapsed} s)"
done
echo; cat "$out"
exit $status
