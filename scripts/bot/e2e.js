'use strict';
// Test de bout en bout d'EGC-plugins avec de vrais joueurs (simulés par mineflayer) sur un serveur de test local.
// Lancé par scripts/smoke-test.sh quand HUB_BOTS=1 ; se lance aussi seul :
//   node scripts/bot/e2e.js --port 25599 --version 1.20.4 --console .smoke/run/paper-1.20.4/console.in
//
// Code de sortie : 0 = tout réussi, 1 = échec, 3 = version non gérée par mineflayer (test ignoré).
const fs = require('fs');
const path = require('path');

const args = {};
for (let i = 2; i < process.argv.length; i += 2) args[process.argv[i].replace(/^--/, '')] = process.argv[i + 1];
const PORT = Number(args.port || 25599);
const VERSION = args.version;
const CONSOLE = args.console; // tube nommé de la console du serveur
const LOG = args.log; // journal du serveur (pour ce que le client ne voit pas tel quel)

let mineflayer;
try {
  mineflayer = require('mineflayer');
} catch (e) {
  console.log('mineflayer n\'est pas installé (npm install dans scripts/bot) : test ignoré');
  process.exit(3);
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const results = [];

function consoleCommand(cmd) {
  const fd = fs.openSync(CONSOLE, 'w');
  fs.writeSync(fd, cmd + '\n');
  fs.closeSync(fd);
}

function connect(name) {
  return new Promise((resolve, reject) => {
    let bot;
    try {
      bot = mineflayer.createBot({ host: '127.0.0.1', port: PORT, username: name, version: VERSION, auth: 'offline', hideErrors: false });
    } catch (e) {
      return reject(e);
    }
    bot.chatLog = [];
    bot.titles = [];
    bot.actionBars = [];
    bot.on('messagestr', (text) => bot.chatLog.push(text));
    // Selon la version, le texte arrive en chaîne ou en objet (composant) : on le met à plat pour chercher dedans.
    const flat = (t) => (typeof t === 'string' ? t : JSON.stringify(t));
    bot.on('title', (text, type) => bot.titles.push({ type, text: flat(text) }));
    bot.on('actionBar', (msg) => bot.actionBars.push(flat(msg)));
    const timer = setTimeout(() => reject(new Error('connexion trop longue : ' + name)), 60000);
    bot.once('spawn', () => {
      clearTimeout(timer);
      resolve(bot);
    });
    bot.once('error', (e) => { clearTimeout(timer); reject(e); });
    bot.once('kicked', (reason) => { clearTimeout(timer); reject(new Error('expulsé : ' + reason)); });
  });
}

/** Attend qu'un message correspondant à `re` arrive (à partir de `since`, index dans bot.chatLog). */
async function expectChat(bot, re, ms, since) {
  const deadline = Date.now() + (ms || 10000);
  let from = since === undefined ? 0 : since;
  while (Date.now() < deadline) {
    for (let i = from; i < bot.chatLog.length; i++) if (re.test(bot.chatLog[i])) return bot.chatLog[i];
    await sleep(150);
  }
  throw new Error('message attendu ' + re + ' ; reçu : ' + JSON.stringify(bot.chatLog.slice(from).slice(-8)));
}

/**
 * Envoie un message en respectant le compteur anti-spam de Minecraft (chaque message ajoute 20, il retombe de 1 par 50 ms,
 * expulsion au-dessus de 200) : sans cela, le test se fait expulser dès que la machine va un peu vite.
 */
async function send(bot, text) {
  const now = Date.now();
  bot.spamScore = Math.max(0, (bot.spamScore || 0) - (now - (bot.spamAt || now)) / 50);
  while (bot.spamScore + 20 > 100) {
    await sleep(100);
    const t = Date.now();
    bot.spamScore = Math.max(0, bot.spamScore - (t - bot.spamAt) / 50);
    bot.spamAt = t;
  }
  bot.spamScore += 20;
  bot.spamAt = Date.now();
  bot.chat(text);
}

/** Envoie une commande et attend la réponse. */
async function say(bot, text, re, ms) {
  const since = bot.chatLog.length;
  await send(bot, text);
  return expectChat(bot, re, ms, since);
}

const logSize = () => (LOG && fs.existsSync(LOG) ? fs.statSync(LOG).size : 0);

/** Attend une ligne du journal du serveur écrite après `offset` (taille du fichier avant l'action). */
async function expectLog(re, ms, offset) {
  const deadline = Date.now() + (ms || 8000);
  while (Date.now() < deadline) {
    if (LOG && fs.existsSync(LOG)) {
      const text = fs.readFileSync(LOG).slice(offset).toString('utf8'); // offset en octets (le journal contient des accents)
      const line = text.split('\n').find((l) => re.test(l));
      if (line) return line;
    }
    await sleep(200);
  }
  throw new Error('ligne de journal attendue ' + re);
}

async function waitFor(fn, ms, label) {
  const deadline = Date.now() + ms;
  while (Date.now() < deadline) {
    if (fn()) return;
    await sleep(150);
  }
  throw new Error('condition non remplie : ' + label);
}

async function step(name, fn) {
  try {
    await fn();
    results.push({ name, ok: true });
    console.log('OK     ' + name);
  } catch (e) {
    results.push({ name, ok: false, error: e.message });
    console.log('ÉCHEC  ' + name + '\n         ' + String(e.message).split('\n')[0].slice(0, 400));
  }
}

const near = (a, b, d) => Math.abs(a.x - b.x) <= d && Math.abs(a.z - b.z) <= d;
/** Téléporte un joueur par la console, à sa hauteur actuelle (le sol des mondes plats varie selon la version). */
function moveBy(bot, name, dx, dz) {
  const p = bot.entity.position;
  consoleCommand(`tp ${name} ${Math.floor(p.x + dx)} ${Math.ceil(p.y)} ${Math.floor(p.z + dz)}`);
}

async function main() {
  let a;
  try {
    a = await connect('BotA');
  } catch (e) {
    if (/unsupported|not supported|version/i.test(String(e.message)) && !/expuls|connexion/i.test(String(e.message))) {
      console.log('version non gérée par mineflayer : ' + e.message);
      process.exit(3);
    }
    throw e;
  }
  await sleep(1500);
  let home;

  await step('message du jour à la connexion', () => expectChat(a, /Tapez \/info/, 12000));
  await step('solde de départ (100)', () => say(a, '/balance', /Solde : 100,00 \$/));
  await step('/sethome', async () => {
    await sleep(500);
    home = { ...a.entity.position };
    await say(a, '/sethome maison', /Maison maison enregistrée/);
  });
  await step('/homes liste la maison', () => say(a, '/homes', /maison/));
  await step('/home avec attente puis arrivée', async () => {
    moveBy(a, 'BotA', 40, 40);
    await waitFor(() => a.entity.position.x > home.x + 30, 10000, 'téléporté par la console');
    await sleep(800);
    const since = a.chatLog.length;
    await send(a, '/home maison');
    await expectChat(a, /Téléportation dans 3 secondes/, 6000, since);
    await expectChat(a, /Bienvenue à la maison maison/, 10000, since);
    await waitFor(() => near(a.entity.position, home, 4), 6000, 'retour près de la maison');
  });
  await step('/home immédiatement refusé (temps de recharge)', () => say(a, '/home maison', /Patientez encore/, 6000));
  await step('/kit starter reçu avec une épée', async () => {
    await say(a, '/kit starter', /Kit starter reçu/);
    await waitFor(() => a.inventory.items().some((i) => /sword/.test(i.name)), 8000, 'une épée dans l\'inventaire');
  });
  await step('/kit starter refusé (recharge)', () => say(a, '/kit starter', /disponible dans/));
  await step('/kit inexistant', () => say(a, '/kit nexistepas', /Kit introuvable/));
  await step('/sethome au-delà de la limite', async () => {
    await say(a, '/sethome deux', /enregistrée/);
    await say(a, '/sethome trois', /enregistrée/);
    await say(a, '/sethome quatre', /limite de 3/);
  });
  await step('/delhome', () => say(a, '/delhome trois', /supprimée/));
  await step('chat : majuscules abaissées (vu dans le journal du serveur)', async () => {
    // Depuis la 1.19 le client lit le corps signé d'origine du message : seul le journal montre le texte publié.
    const offset = logSize();
    await send(a, 'ARRETEZ DE FAIRE CA');
    await expectLog(/<BotA> arretez de faire ca/, 6000, offset);
  });
  await step('chat : répétition refusée', async () => {
    await send(a, 'achetez mon super grade');
    await sleep(1200);
    await say(a, 'achetez mon super grade', /répéter/);
  });
  await step('chat : anti-spam', async () => {
    await sleep(6000);
    const since = a.chatLog.length;
    // Le plugin coupe à 4 messages par 5 s ; Minecraft expulse pour spam bien plus tard (rafale trop grosse) : 6 messages espacés.
    for (let i = 0; i < 6; i++) {
      a.chat('message numero ' + i + ' assez different ' + 'xyz'.repeat(i));
      await sleep(150);
    }
    await expectChat(a, /Doucement/, 6000, since);
    await sleep(6000); // laisse retomber le compteur de spam de Minecraft
  });

  let b;
  await step('second joueur connecté', async () => {
    b = await connect('BotB');
    await sleep(1500);
  });
  await step('/tpa puis /tpaccept : le demandeur rejoint', async () => {
    const bStart = b.entity.position.clone();
    moveBy(b, 'BotB', -30, -30);
    await waitFor(() => b.entity.position.x < bStart.x - 20, 10000, 'BotB déplacé');
    await sleep(6000); // fin du temps de recharge de BotA
    await say(a, '/tpa BotB', /Demande envoyée à BotB/);
    await expectChat(b, /BotA.*demande à se téléporter vers vous/, 6000);
    const since = a.chatLog.length;
    await send(b, '/tpaccept');
    await expectChat(a, /accepté votre demande/, 6000, since);
    await expectChat(a, /Téléportation dans 3 secondes/, 6000, since);
    await waitFor(() => near(a.entity.position, b.entity.position, 4), 25000, 'BotA arrivé près de BotB');
  });
  await step('/pay et /balance', async () => {
    await say(a, '/pay BotB 10', /Vous avez envoyé 10,00 \$ à BotB/);
    await expectChat(b, /BotA vous a envoyé 10,00 \$/, 6000);
    await say(a, '/balance', /Solde : 90,00 \$/);
    await say(b, '/balance', /Solde : 110,00 \$/);
  });
  await step('/pay refusé (fonds, soi-même, montant)', async () => {
    await say(a, '/pay BotB 100000', /Solde insuffisant/);
    await say(a, '/pay BotA 5', /vous payer vous-même/);
    await say(a, '/pay BotB -5', /Montant invalide/);
  });
  await step('console : eco give puis solde', async () => {
    const since = a.chatLog.length;
    consoleCommand('eco give BotA 5');
    await sleep(800);
    await say(a, '/balance', /Solde : 95,00 \$/);
  });
  await step('console : kit donné à un joueur', async () => {
    consoleCommand('kit starter BotB');
    await waitFor(() => b.inventory.items().some((i) => /sword/.test(i.name)), 8000, 'épée reçue par BotB');
  });
  await step('/tpa refusé : joueur inconnu', () => say(a, '/tpa Fantome', /Joueur introuvable/));
  await step('/tpdeny : la demande est refusée', async () => {
    await say(a, '/tpa BotB', /Demande envoyée/);
    await expectChat(b, /demande à se téléporter vers vous/, 6000);
    await say(b, '/tpdeny', /refusée/);
    await expectChat(a, /refusé votre demande/, 6000);
  });
  await step('/tpcancel : annule ses demandes', async () => {
    await say(a, '/tpa BotB', /Demande envoyée/);
    await say(a, '/tpcancel', /1 demande\(s\) annulée/);
    await say(b, '/tpaccept', /Aucune demande en attente/);
  });
  await step('/kits et /baltop', async () => {
    await say(a, '/kits', /starter/);
    await say(a, '/baltop', /Les plus riches/);
  });
  await step('commande réservée refusée à un joueur', () => say(a, '/heal', /pas la permission/));

  consoleCommand('op BotA');
  await sleep(1200);
  await step('opérateur : /heal /feed', async () => {
    await say(a, '/heal', /soigné/);
    await say(a, '/feed', /faim/);
  });
  await step('opérateur : /fly puis retour', async () => {
    await say(a, '/fly', /Vol activé/);
    await say(a, '/fly', /Vol désactivé/);
  });
  await step('opérateur : /gamemode creative', () => say(a, '/gamemode creative', /Mode de jeu : creative/));
  await step('opérateur : /speed', () => say(a, '/speed 3', /Vitesse : 3/));
  await step('/ping (valeur ou indisponible)', () => say(a, '/ping', /ping|indisponible/i));
  await step('opérateur : /setspawn puis /spawn', async () => {
    await say(a, '/setspawn', /Spawn défini/);
    const before = a.entity.position.x;
    moveBy(a, 'BotA', 50, 50);
    await waitFor(() => a.entity.position.x > before + 40, 10000, 'déplacé');
    await say(a, '/spawn', /Vous voilà au spawn/);
    await waitFor(() => a.entity.position.x < before + 10, 8000, 'revenu au spawn');
  });
  await step('opérateur : /back après /spawn', async () => {
    const spawnX = a.entity.position.x;
    await say(a, '/back', /Retour à votre position précédente/);
    await waitFor(() => a.entity.position.x > spawnX + 30, 8000, `retourné à l'endroit quitté (x avant ${spawnX}, x après ${a.entity.position.x})`);
  });
  await step('titre et sous-titre reçus par le joueur', async () => {
    consoleCommand('uc title BotA Bonjour|Sous-titre');
    await waitFor(() => a.titles.some((t) => /Bonjour/.test(t.text)) && a.titles.some((t) => /Sous-titre/.test(t.text)), 8000, 'titre et sous-titre (reçu : ' + JSON.stringify(a.titles) + ')');
  });
  await step('barre d\'action reçue par le joueur', async () => {
    consoleCommand('uc actionbar BotA MessageBarre');
    await waitFor(() => a.actionBars.some((m) => /MessageBarre/.test(m)), 8000, 'barre d\'action (reçu : ' + JSON.stringify(a.actionBars) + ')');
  });
  await step('points de passage : création, liste, téléportation, suppression', async () => {
    await say(a, '/setwarp arene', /Point de passage arene créé/);
    await say(a, '/warps', /arene/);
    const here = a.entity.position.x;
    moveBy(a, 'BotA', 60, 0);
    await waitFor(() => a.entity.position.x > here + 50, 10000, 'déplacé');
    await say(a, '/warp arene', /Téléporté vers arene/);
    await waitFor(() => a.entity.position.x < here + 10, 8000, 'arrivé au point de passage');
    await say(a, '/warp nexistepas', /introuvable/);
    await say(a, '/delwarp arene', /supprimé/);
    await say(a, '/warp arene', /introuvable/);
  });
  await step('/tpahere : le destinataire rejoint le demandeur', async () => {
    moveBy(b, 'BotB', 40, 40);
    await waitFor(() => !near(b.entity.position, a.entity.position, 10), 10000, 'BotB éloigné de BotA');
    await sleep(800);
    await say(a, '/tpahere BotB', /Demande envoyée/);
    await expectChat(b, /vous demande de vous téléporter à sa position/, 6000);
    const since = b.chatLog.length;
    await send(b, '/tpaccept');
    await expectChat(b, /Téléportation dans 3 secondes/, 6000, since);
    await waitFor(() => near(b.entity.position, a.entity.position, 4), 12000, 'BotB arrivé près de BotA');
  });
  await step('/rtp : envoi loin puis temps de recharge', async () => {
    await sleep(6000); // fin du temps de recharge de téléportation de BotB
    const before = b.entity.position.clone();
    // Si la recharge de téléportation (5 s) n'est pas finie, le plugin le dit : on patiente et on réessaie.
    for (let essai = 0; ; essai++) {
      const since = b.chatLog.length;
      await send(b, '/rtp');
      await expectChat(b, /Recherche d'un endroit sûr/, 8000, since);
      const deadline = Date.now() + 40000;
      let attente = false;
      while (Date.now() < deadline) {
        const log = b.chatLog.slice(since);
        if (log.some((l) => /Téléporté en X/.test(l))) break;
        if (log.some((l) => /Patientez encore/.test(l))) { attente = true; break; }
        await sleep(200);
      }
      if (!attente) break;
      if (essai >= 2) throw new Error('recharge de téléportation toujours active après 3 essais');
      await sleep(6000);
    }
    await expectChat(b, /Téléporté en X/, 40000, 0);
    await waitFor(() => Math.hypot(b.entity.position.x - before.x, b.entity.position.z - before.z) > 150, 20000, 'BotB loin de son point de départ');
    await say(b, '/rtp', /Prochain téléport aléatoire/);
  });
  await step('console : rtp JOUEUR (sans attente ni recharge)', async () => {
    const before = a.entity.position.clone();
    const since = a.chatLog.length;
    consoleCommand('rtp BotA');
    await expectChat(a, /Téléporté en X/, 40000, since);
    await waitFor(() => Math.hypot(a.entity.position.x - before.x, a.entity.position.z - before.z) > 150, 20000, 'BotA loin de son point de départ');
  });
  await step('/ec ouvre le coffre de l\'Ender', async () => {
    await send(a, '/ec');
    await waitFor(() => a.currentWindow, 8000, 'fenêtre ouverte');
    a.closeWindow(a.currentWindow);
    await sleep(300);
  });
  await step('/freeze : titre, message, immobilisation, commande bloquée, dégel', async () => {
    const t0 = b.titles.length;
    await say(a, '/freeze BotB Contrôle de triche', /BotB est gelé/);
    await expectChat(a, /\[Staff\].*a gelé/, 6000);
    await waitFor(() => b.titles.slice(t0).some((t) => /GELÉ/.test(t.text)), 8000, 'titre de gel reçu (reçu : ' + JSON.stringify(b.titles.slice(t0)) + ')');
    await expectChat(b, /Contrôle de triche/, 6000);
    const p0 = b.entity.position.clone();
    b.setControlState('forward', true);
    await sleep(1500);
    b.setControlState('forward', false);
    await sleep(700);
    const drift = Math.hypot(b.entity.position.x - p0.x, b.entity.position.z - p0.z);
    if (drift > 1.0) throw new Error('le joueur gelé a bougé de ' + drift.toFixed(2) + ' blocs');
    await say(b, '/home', /commandes sont bloquées/);
    await say(a, '/freeze BotB', /n'est plus gelé/);
    await expectChat(b, /plus gelé/, 6000);
    const p1 = b.entity.position.clone();
    b.setControlState('forward', true);
    await sleep(1200);
    b.setControlState('forward', false);
    const moved = Math.hypot(b.entity.position.x - p1.x, b.entity.position.z - p1.z);
    if (moved < 2.5) throw new Error('le joueur dégelé n\'avance pas (' + moved.toFixed(2) + ' blocs)');
  });
  await step('/freeze refusé sur un opérateur', () => say(a, '/freeze BotA', /ne peut pas être gelé/));
  await step('/alert : message, titre', async () => {
    const t0 = b.titles.length;
    await say(a, '/alert BotB Serveur bientôt redémarré', /Envoyé à 1 joueur/);
    await expectChat(b, /\[Alerte\] Serveur bientôt redémarré/, 6000);
    await waitFor(() => b.titles.slice(t0).some((t) => /ALERTE/.test(t.text)), 8000, 'titre d\'alerte reçu');
  });
  await step('/warn puis /warns', async () => {
    const t0 = b.titles.length;
    await say(a, '/warn BotB Insultes répétées', /Avertissement n°1 envoyé à BotB/);
    await expectChat(b, /Avertissement n°1 : Insultes répétées/, 6000);
    await waitFor(() => b.titles.slice(t0).some((t) => /AVERTISSEMENT/.test(t.text)), 8000, 'titre d\'avertissement reçu');
    await say(a, '/warns BotB', /BotB.* 1 avertissement/);
    await say(a, '/warn BotB Deuxième fois', /n°2/);
    await say(a, '/warns BotB', /BotB.* 2 avertissement/);
  });
  await step('/mute : chat et messages privés bloqués, puis /unmute', async () => {
    await say(a, '/mute BotB 1m Spam', /BotB est muet/);
    await expectChat(b, /Vous avez été rendu muet/, 6000);
    await say(b, 'bonjour tout le monde', /Vous êtes muet/);
    await say(b, '/msg BotA salut', /pas utiliser cette commande/);
    await say(a, '/unmute BotB', /peut de nouveau parler/);
    await expectChat(b, /pouvez de nouveau parler/, 6000);
    const offset = logSize();
    await send(b, 'me revoilà');
    await expectLog(/<BotB> me revoilà/, 6000, offset);
  });
  await step('/mute refusé sur un opérateur, /unmute sur un joueur non muet', async () => {
    await say(a, '/mute BotA', /ne peut pas être rendu muet/);
    await say(a, '/unmute BotB', /n'est pas muet/);
  });
  await step('/tphere : le joueur visé est amené auprès du staff', async () => {
    moveBy(b, 'BotB', 150, 0);
    await waitFor(() => !near(a.entity.position, b.entity.position, 30), 15000, 'BotB éloigné de BotA');
    await sleep(800);
    const since = b.chatLog.length;
    await say(a, '/tphere BotB', /BotB a été téléporté auprès de vous/);
    await expectChat(b, /téléporté auprès de BotA/, 8000, since);
    await waitFor(() => near(a.entity.position, b.entity.position, 3), 20000, 'BotB arrivé auprès de BotA');
  });
  await step('/tphere refusé : soi-même, sans permission', async () => {
    await say(a, '/tphere BotA', /vous cibler vous-même/);
    await say(b, '/tphere BotA', /pas la permission/);
  });
  await step('/vanish : invisible pour les autres, puis visible de nouveau', async () => {
    const voitA = () => b.players['BotA'] && b.players['BotA'].entity;
    await waitFor(voitA, 15000, 'BotB voit BotA avant le vanish');
    await say(a, '/vanish', /invisible pour les autres joueurs/);
    await waitFor(() => !voitA(), 15000, 'BotB ne voit plus BotA');
    await say(a, '/vanish', /de nouveau visible/);
    await waitFor(voitA, 15000, 'BotB revoit BotA');
  });
  await step('/vanish refusé à un joueur sans permission', () => say(b, '/vanish', /pas la permission/));
  await step('/uc info', () => say(a, '/uc info', /Plateforme/));
  await step('/info et raccourci /rules', async () => {
    await say(a, '/info rules', /Règles du serveur/);
    await say(a, '/rules', /Règles du serveur/);
  });

  a.quit();
  if (b) b.quit();
  await sleep(500);
  const failed = results.filter((r) => !r.ok);
  console.log(`\nJOUEURS SIMULÉS : ${results.length - failed.length}/${results.length} réussis`);
  process.exit(failed.length ? 1 : 0);
}

main().catch((e) => {
  console.log('ERREUR FATALE du test : ' + (e && e.stack ? e.stack : e));
  process.exit(1);
});
