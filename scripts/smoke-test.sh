#!/usr/bin/env bash
# Test de fumée : démarre un vrai serveur Paper ou Folia, y charge EGC-plugins, lance « /uc selftest », recharge,
# relance l'auto-test, exécute des commandes console et cherche toute erreur du plugin dans le journal.
#
# Usage : scripts/smoke-test.sh <paper|folia> <version> [port]
#   ex. : HUB_ACCEPT_EULA=true scripts/smoke-test.sh paper 1.20.4
#
# Prérequis : bash, curl, python3, et le bon Java (voir « Java » ci-dessous). Le jar du plugin doit exister
#   (./gradlew :plugins:ultimate-core:shadowJar).
#
# EULA : les serveurs de Mojang exigent d'accepter https://aka.ms/MinecraftEULA. Ce script n'écrit « eula=true »
#   que si vous exportez HUB_ACCEPT_EULA=true, donc c'est vous qui acceptez, en connaissance de cause.
#
# Java : Java 8 jusqu'à la 1.16, 17 pour 1.17 à 1.20.4, 21 pour 1.20.5 à 1.21.x, 25 pour la numérotation 26.x.
#   Indiquez l'exécutable avec JAVA8, JAVA17, JAVA21 ou JAVA25 ; à défaut, « java » du PATH est utilisé.
#
# Joueurs simulés : avec HUB_BOTS=1 (Node.js 18+ et « npm install » dans scripts/bot), des clients mineflayer se connectent
#   et essaient les commandes (maisons, kits, téléportations, économie, chat…). Ignoré si la version n'est pas gérée.
#
# Sécurité : le serveur n'écoute que sur 127.0.0.1, en mode hors ligne, avec 4 joueurs au plus, puis il est arrêté.
set -uo pipefail

project="${1:?projet : paper ou folia}"
version="${2:?version du jeu, ex. 1.20.4}"
port="${3:-25599}"
root="$(cd "$(dirname "$0")/.." && pwd)"
dir="$root/.smoke/run/$project-$version"
log="$dir/server.log"

if [ "${HUB_ACCEPT_EULA:-}" != "true" ]; then
  echo "Il faut accepter l'EULA de Mojang (https://aka.ms/MinecraftEULA) : relancez avec HUB_ACCEPT_EULA=true." >&2
  exit 2
fi

# --- Mémoire : ne pas étouffer une machine qui héberge autre chose
avail_mb=$(awk '/MemAvailable/ {print int($2/1024)}' /proc/meminfo)
if [ "${avail_mb:-0}" -lt 900 ] && [ "${HUB_FORCE:-}" != "1" ]; then
  echo "Mémoire disponible insuffisante (${avail_mb} Mo < 900). HUB_FORCE=1 pour passer outre." >&2
  exit 2
fi

# --- Java
major_for() {
  local v="$1" first minor patch
  first="${v%%.*}"
  if [ "$first" -ge 26 ] 2>/dev/null; then echo 25; return; fi
  minor="$(echo "$v" | cut -d. -f2)"
  patch="$(echo "$v" | cut -d. -f3)"; patch="${patch:-0}"
  if [ "$minor" -le 16 ]; then echo 8
  elif [ "$minor" -le 19 ] || { [ "$minor" -eq 20 ] && [ "$patch" -le 4 ]; }; then echo 17
  else echo 21; fi
}
jmajor="$(major_for "$version")"
jvar="JAVA$jmajor"
java_bin="${!jvar:-java}"
if ! command -v "$java_bin" >/dev/null 2>&1; then echo "Java introuvable : $java_bin (définissez $jvar)" >&2; exit 2; fi
echo "== $project $version avec $("$java_bin" -version 2>&1 | head -1)"

# --- Fichiers
"$root/scripts/fetch-server.sh" "$project" "$version" || exit 2
plugin_jar="$(ls "$root"/plugins/ultimate-core/build/libs/EGC-plugins-*.jar 2>/dev/null | grep -v -- '-thin' | head -1)"
if [ -z "$plugin_jar" ]; then echo "Jar du plugin introuvable : lancez ./gradlew :plugins:ultimate-core:shadowJar" >&2; exit 2; fi

mkdir -p "$dir/plugins"
rm -f -- "$dir"/plugins/ultimate-core*.jar "$dir"/plugins/EGC-plugins*.jar
rm -rf -- "$dir/plugins/EGC-plugins" "$dir/plugins/UltimateCore"
cp "$plugin_jar" "$dir/plugins/"
if [ "${HUB_BOTS:-}" = "1" ]; then
  # Les joueurs simulés doivent partir d'un état propre (nouveaux joueurs, personne opérateur).
  rm -rf -- "$dir/world" "$dir/world_nether" "$dir/world_the_end"
  rm -f -- "$dir/ops.json" "$dir/usercache.json"
fi
echo "eula=true" > "$dir/eula.txt"

level_type="flat"
minor="$(echo "$version" | cut -d. -f2)"; first="${version%%.*}"
if [ "$first" -ge 26 ] 2>/dev/null || [ "$minor" -ge 19 ]; then level_type='minecraft\:flat'; fi
cat > "$dir/server.properties" <<PROPS
server-ip=127.0.0.1
server-port=$port
online-mode=false
level-type=$level_type
generate-structures=false
view-distance=3
simulation-distance=3
spawn-protection=0
max-players=4
allow-nether=false
spawn-monsters=false
spawn-animals=false
spawn-npcs=false
motd=EGC-plugins smoke test
PROPS

# --- Lancement (l'entrée du serveur est un tube nommé, pour lui envoyer des commandes)
fifo="$dir/console.in"; rm -f -- "$fifo"; mkfifo "$fifo"
: > "$log"
cd "$dir" || exit 2
nice -n 19 "$java_bin" -Xmx640M -XX:+UseSerialGC -Dfile.encoding=UTF-8 -jar "$root/.smoke/jars/$project-$version.jar" nogui < "$fifo" > "$log" 2>&1 &
pid=$!
exec 3> "$fifo"
cleanup() { exec 3>&- 2>/dev/null; if kill -0 "$pid" 2>/dev/null; then kill "$pid" 2>/dev/null; sleep 3; kill -9 "$pid" 2>/dev/null; fi; }
trap cleanup EXIT

wait_for() { # motif, secondes
  local pattern="$1" limit="$2" waited=0
  while [ "$waited" -lt "$limit" ]; do
    grep -qE "$pattern" "$log" && return 0
    kill -0 "$pid" 2>/dev/null || { grep -qE "$pattern" "$log"; return $?; }
    sleep 1; waited=$((waited + 1))
  done
  return 1
}
send() { echo "$1" >&3; }

fail() { echo "ÉCHEC : $1"; echo "--- fin du journal ($log) :"; tail -40 "$log"; exit 1; }

wait_for 'Done \([0-9.,]+s\)!' 420 || fail "le serveur n'a pas fini de démarrer"
grep -qE "Error occurred while enabling|Could not load '.*EGC|Disabling EGC-plugins" "$log" && fail "le plugin n'a pas pu se charger"
grep -q "EGC-plugins .* activé sur" "$log" || fail "message d'activation du plugin absent"
echo "-- démarrage : $(grep -m1 'EGC-plugins .* activé sur' "$log" | sed 's/^.*\] //')"

run_selftest() {
  local before
  before="$(grep -c 'SELFTEST RESULT' "$log")"
  send "uc selftest"
  local waited=0
  while [ "$(grep -c 'SELFTEST RESULT' "$log")" -le "$before" ] && [ "$waited" -lt 60 ]; do sleep 1; waited=$((waited + 1)); done
  grep 'SELFTEST RESULT' "$log" | tail -1 | grep -q 'PASS'
}

run_selftest || { grep -E 'selftest' "$log" | sed 's/^.*\[selftest\]/  [selftest]/'; fail "auto-test en échec (1re passe)"; }
echo "-- auto-test 1 : PASS"
send "uc reload"; wait_for 'rechargés' 30 || fail "rechargement sans réponse"
run_selftest || { grep -E 'selftest' "$log" | tail -25; fail "auto-test en échec après rechargement"; }
echo "-- auto-test 2 (après uc reload) : PASS"

# Joueurs simulés (optionnel) : HUB_BOTS=1 ; demande Node.js 18+ et « npm install » dans scripts/bot.
if [ "${HUB_BOTS:-}" = "1" ]; then
  if ! command -v node >/dev/null 2>&1; then
    echo "-- joueurs simulés : ignorés (Node.js introuvable)"
  else
    echo "-- joueurs simulés :"
    node "$root/scripts/bot/e2e.js" --port "$port" --version "$version" --console "$fifo" --log "$log" 2>&1 | sed 's/^/   /'
    bots_code="${PIPESTATUS[0]}"
    if [ "$bots_code" -eq 3 ]; then echo "-- joueurs simulés : version non gérée, ignorés"
    elif [ "$bots_code" -ne 0 ]; then fail "des joueurs simulés ont échoué"; fi
  fi
fi

# Commandes utilisables depuis la console : aucune ne doit lever d'erreur.
for c in "uc help" "uc info" "uc version" "uc broadcast Bonjour à tous" "uc title * Test|Sous-titre" "uc actionbar * Test" "uc sound * LEVEL_UP" \
         "kits" "kit starter" "kit inconnu" "balance" "eco give Personne 5" "baltop" "warps" "spawn" "info rules" "info" "rules" "clearchat" \
         "gamemode creative" "heal" "ping" "tpa Personne"; do
  send "$c"
done
sleep 4
send "uc selftest"; sleep 4

send "stop"
wait_for 'Stopping server|Closing Server' 30 >/dev/null
for _ in $(seq 1 60); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
kill -0 "$pid" 2>/dev/null && fail "le serveur ne s'arrête pas"

# --- Recherche d'erreurs provenant du plugin
if grep -nE "at fr\.minebed\.hub|Erreur dans la commande|Erreur à l'arrêt|n'a pas pu démarrer|Could not pass event|Le module « " "$log" >/tmp/hub-smoke-errors.$$ 2>&1 && [ -s /tmp/hub-smoke-errors.$$ ]; then
  echo "ÉCHEC : erreurs du plugin dans le journal :"; head -30 /tmp/hub-smoke-errors.$$; rm -f /tmp/hub-smoke-errors.$$; exit 1
fi
rm -f /tmp/hub-smoke-errors.$$
echo "-- avertissements du plugin :"; grep -E "\[EGC-plugins\].*(WARN|WARNING)|WARN.*EGC-plugins" "$log" | sed 's/^/   /' | head -10 || true
echo "RÉSULTAT : $project $version : PASS"
