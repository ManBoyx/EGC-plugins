#!/usr/bin/env bash
# Télécharge un serveur Paper ou Folia de test dans .smoke/jars/ (vérifie la somme SHA-256).
# Usage : scripts/fetch-server.sh <paper|folia> <version>      ex. scripts/fetch-server.sh paper 1.20.4
set -euo pipefail

project="${1:?projet : paper ou folia}"
version="${2:?version du jeu, ex. 1.20.4}"
root="$(cd "$(dirname "$0")/.." && pwd)"
dest="$root/.smoke/jars"
mkdir -p "$dest"

jar="$dest/$project-$version.jar"
if [ -s "$jar" ]; then
  echo "déjà présent : $jar"
  exit 0
fi

ua="minecraft-plugins-hub-smoke/0.1"
info="$(curl -fsSL -A "$ua" "https://fill.papermc.io/v3/projects/$project/versions/$version/builds/latest")"
read -r url sha < <(printf '%s' "$info" | python3 -c '
import json, sys
d = json.load(sys.stdin)["downloads"]["server:default"]
print(d["url"], d["checksums"]["sha256"])')

echo "téléchargement de $project $version…"
curl -fsSL -A "$ua" -o "$jar.part" "$url"
echo "$sha  $jar.part" | sha256sum -c --quiet -
mv "$jar.part" "$jar"
echo "ok : $jar"
