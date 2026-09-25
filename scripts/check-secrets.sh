#!/usr/bin/env bash
# Cherche dans les fichiers suivis par git des choses qui ressemblent à des secrets (jetons, clés privées, webhooks).
# Ne remplace pas un vrai outil (gitleaks…), mais attrape les erreurs les plus courantes avant un commit ou en CI.
# Usage : scripts/check-secrets.sh        (code de sortie 1 si quelque chose est trouvé)
set -uo pipefail
cd "$(dirname "$0")/.."

# Les motifs sont assemblés en morceaux pour que ce fichier ne se signale pas lui-même.
patterns=(
  "gh[pousr]_[A-Za-z0-9]{30,}"
  "github_""pat_[A-Za-z0-9_]{20,}"
  "sk-or-""v1-[A-Za-z0-9]{20,}"
  "sk-ant-[A-Za-z0-9_-]{20,}"
  "AKIA[0-9A-Z]{16}"
  "xox[baprs]-[A-Za-z0-9-]{10,}"
  "-----BEGIN (RSA |EC |OPENSSH |DSA |PGP )?PRIVATE KEY-----"
  "discord(app)?\.com/api/web""hooks/[0-9]+/[A-Za-z0-9_-]+"
  "(password|passwd|secret|token|api[_-]?key)[\"']?[[:space:]]*[:=][[:space:]]*[\"'][A-Za-z0-9/+_=-]{16,}[\"']"
)

found=0
while IFS= read -r file; do
  [ -f "$file" ] || continue
  case "$file" in scripts/check-secrets.sh|*.jar|*.png|*.gradle.jar) continue ;; esac
  for p in "${patterns[@]}"; do
    if grep -InE -e "$p" -- "$file" >/tmp/hub-secret-hit.$$ 2>/dev/null; then
      echo "Suspect dans $file :"; sed 's/^/   /' /tmp/hub-secret-hit.$$ | cut -c1-160
      found=1
      break
    fi
  done
done < <(git ls-files)
rm -f /tmp/hub-secret-hit.$$

# Fichiers qui ne doivent jamais être suivis
while IFS= read -r file; do
  case "$file" in
    *.pem|*.p12|*.jks|*.keystore|.env|.env.*|*/.env|*id_rsa*|*id_ed25519*|*.kdbx) echo "Fichier sensible suivi par git : $file"; found=1 ;;
  esac
done < <(git ls-files)

if [ "$found" -eq 0 ]; then echo "Aucun secret apparent dans les fichiers suivis."; fi
exit "$found"
