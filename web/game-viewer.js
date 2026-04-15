const TILE = 56;
const COLS = 16;
const ROWS = 10;

const factions = {
  ALLIES: { label: 'Alliés', emblem: '🛡️', color: '#409cff' },
  SOVIETS: { label: 'Soviétiques', emblem: '🔨', color: '#e24e4e' }
};

const dlcs = [
  ['Base Game', 'Campagne Europe + guerre tactique standard'],
  ['Aftermath', 'Technologies expérimentales: Tesla Storm, chrono-raid'],
  ['Counterstrike', 'Opérations de sabotage, rail-gun mobile'],
  ['Retaliation', 'Escarmouches avancées, doctrines asymétriques']
];

const unitTypes = {
  rifleman: { name: 'Fusilier', emoji: '🪖', maxHp: 100, attack: 17, range: 2, speed: 3, class: 'Infanterie', cost: 80 },
  rocket: { name: 'Lance-roquettes', emoji: '🚀', maxHp: 75, attack: 26, range: 4, speed: 2, class: 'Infanterie', cost: 80 },
  light_tank: { name: 'Char léger', emoji: '🚓', maxHp: 180, attack: 35, range: 3, speed: 3, class: 'Véhicule', cost: 120 },
  mammoth: { name: 'Char Mammouth', emoji: '🦣', maxHp: 350, attack: 60, range: 4, speed: 2, class: 'Véhicule', cost: 120 },
  yak: { name: "Yak d'attaque", emoji: '✈️', maxHp: 150, attack: 42, range: 5, speed: 5, class: 'Aérien', cost: 160 },
  railgun: { name: 'Railgun Mobile', emoji: '🧲', maxHp: 220, attack: 72, range: 6, speed: 2, class: 'Véhicule', cost: 120 },
  tesla_storm: { name: 'Tesla Storm Walker', emoji: '⚡', maxHp: 230, attack: 48, range: 5, speed: 2, class: 'Support', cost: 100 },
  chrono_commando: { name: 'Commando Chrono', emoji: '⏱️', maxHp: 130, attack: 80, range: 1, speed: 6, class: 'Infanterie', cost: 80 },
  drone_swarm: { name: 'Nuée de Drones', emoji: '🛰️', maxHp: 160, attack: 44, range: 3, speed: 5, class: 'Aérien', cost: 160 }
};

const buildingTypes = {
  hq: { name: 'QG', emoji: '🏛️', maxHp: 700, income: 80, armor: 12 },
  war_factory: { name: 'Usine de guerre', emoji: '🏭', maxHp: 520, income: 35, armor: 10 },
  tesla_lab: { name: 'Laboratoire Tesla', emoji: '🔬', maxHp: 360, income: 45, armor: 7 }
};

const state = {
  turn: 1,
  activeFaction: 'ALLIES',
  ticker: 'Bienvenue Commandant: sécurisez les nœuds énergétiques ⚡',
  credits: { ALLIES: 2500, SOVIETS: 2500 },
  units: [
    { type: 'rifleman', faction: 'ALLIES', x: 2, y: 2, hp: unitTypes.rifleman.maxHp },
    { type: 'light_tank', faction: 'ALLIES', x: 4, y: 2, hp: unitTypes.light_tank.maxHp },
    { type: 'mammoth', faction: 'ALLIES', x: 3, y: 4, hp: unitTypes.mammoth.maxHp },
    { type: 'rocket', faction: 'SOVIETS', x: 11, y: 6, hp: unitTypes.rocket.maxHp },
    { type: 'yak', faction: 'SOVIETS', x: 12, y: 4, hp: unitTypes.yak.maxHp },
    { type: 'railgun', faction: 'SOVIETS', x: 13, y: 7, hp: unitTypes.railgun.maxHp }
  ],
  buildings: [
    { type: 'hq', faction: 'ALLIES', x: 1, y: 1, hp: buildingTypes.hq.maxHp },
    { type: 'war_factory', faction: 'ALLIES', x: 2, y: 6, hp: buildingTypes.war_factory.maxHp },
    { type: 'hq', faction: 'SOVIETS', x: 14, y: 8, hp: buildingTypes.hq.maxHp },
    { type: 'tesla_lab', faction: 'SOVIETS', x: 12, y: 8, hp: buildingTypes.tesla_lab.maxHp }
  ]
};

const canvas = document.getElementById('battleCanvas');
const ctx = canvas.getContext('2d');
const phaseTag = document.getElementById('phase');
const intelTag = document.getElementById('intelText');
const nextTurnBtn = document.getElementById('nextTurn');
const autoBtn = document.getElementById('toggleAuto');

let timer = null;

function alpha(hex, a) {
  const c = hex.replace('#', '');
  const r = parseInt(c.slice(0, 2), 16);
  const g = parseInt(c.slice(2, 4), 16);
  const b = parseInt(c.slice(4, 6), 16);
  return `rgba(${r}, ${g}, ${b}, ${a})`;
}

function drawHexHint(cx, cy, r) {
  ctx.beginPath();
  for (let i = 0; i < 6; i++) {
    const a = ((60 * i - 30) * Math.PI) / 180;
    const x = cx + r * Math.cos(a);
    const y = cy + r * Math.sin(a);
    if (i === 0) ctx.moveTo(x, y);
    else ctx.lineTo(x, y);
  }
  ctx.closePath();
}

function hpBar(x, y, w, hp, maxHp) {
  const ratio = Math.max(0, Math.min(1, hp / maxHp));
  ctx.fillStyle = 'rgba(22,22,22,.9)';
  ctx.fillRect(x, y, w, 5);
  ctx.fillStyle = ratio > 0.5 ? '#4ac956' : ratio > 0.25 ? '#f1b542' : '#e34242';
  ctx.fillRect(x, y, Math.round(w * ratio), 5);
}

function drawTerrain() {
  for (let y = 0; y < ROWS; y++) {
    for (let x = 0; x < COLS; x++) {
      const px = x * TILE + 10;
      const py = y * TILE + 10;
      ctx.fillStyle = (x + y) % 2 === 0 ? '#2a4231' : '#2d4a3d';
      ctx.fillRect(px, py, TILE - 2, TILE - 2);
      ctx.strokeStyle = 'rgba(73,104,86,.5)';
      drawHexHint(px + TILE / 2, py + TILE / 2, TILE / 2 - 4);
      ctx.stroke();
    }
  }
}

function drawBuildings() {
  for (const b of state.buildings) {
    const t = buildingTypes[b.type];
    const faction = factions[b.faction];
    const px = b.x * TILE + 10;
    const py = b.y * TILE + 10;

    ctx.fillStyle = alpha(faction.color, 0.45);
    ctx.beginPath();
    ctx.roundRect(px + 4, py + 4, TILE - 10, TILE - 10, 12);
    ctx.fill();

    ctx.font = '24px "Segoe UI Emoji"';
    ctx.fillStyle = '#fff';
    ctx.fillText(t.emoji, px + 14, py + 34);
    hpBar(px + 4, py + TILE - 10, TILE - 10, b.hp, t.maxHp);
  }
}

function drawUnits() {
  for (const u of state.units) {
    const t = unitTypes[u.type];
    const faction = factions[u.faction];
    const px = u.x * TILE + 10;
    const py = u.y * TILE + 10;

    ctx.fillStyle = alpha(faction.color, 0.85);
    ctx.beginPath();
    ctx.ellipse(px + TILE / 2, py + TILE / 2, (TILE - 24) / 2, (TILE - 24) / 2, 0, 0, Math.PI * 2);
    ctx.fill();

    const r = t.range * 8;
    ctx.strokeStyle = alpha(faction.color, 0.35);
    ctx.beginPath();
    ctx.ellipse(px + TILE / 2, py + TILE / 2, r / 2, r / 2, 0, 0, Math.PI * 2);
    ctx.stroke();

    ctx.font = '22px "Segoe UI Emoji"';
    ctx.fillStyle = '#111';
    ctx.fillText(t.emoji, px + 16, py + 30);

    hpBar(px + 8, py + TILE - 14, TILE - 16, u.hp, t.maxHp);
  }
}

function drawHud() {
  const width = COLS * TILE;
  ctx.fillStyle = 'rgba(5,11,16,.75)';
  ctx.beginPath();
  ctx.roundRect(16, 16, width - 24, 40, 12);
  ctx.fill();

  const fact = factions[state.activeFaction];
  ctx.fillStyle = '#fff';
  ctx.font = 'bold 14px sans-serif';
  ctx.fillText(`Tour ${state.turn} • Phase: ${fact.label} ${fact.emblem}`, 24, 40);
  ctx.fillText(state.ticker, 320, 40);
}

function render() {
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  drawTerrain();
  drawBuildings();
  drawUnits();
  drawHud();

  const f = factions[state.activeFaction];
  phaseTag.textContent = `Tour ${state.turn} — ${f.label} ${f.emblem}`;
  refreshIntel();
}

function clamp(v, min, max) {
  return Math.max(min, Math.min(max, v));
}

function applyEconomy() {
  const byFaction = { ALLIES: 0, SOVIETS: 0 };
  state.buildings.forEach((b) => {
    byFaction[b.faction] += buildingTypes[b.type].income;
  });
  state.credits.ALLIES += byFaction.ALLIES;
  state.credits.SOVIETS += byFaction.SOVIETS;
}

function skirmishTick() {
  const ally = state.units.find((u) => u.faction === state.activeFaction);
  const enemy = state.units.find((u) => u.faction !== state.activeFaction);
  if (!ally || !enemy) return;

  const allyType = unitTypes[ally.type];
  const dist = Math.abs(enemy.x - ally.x) + Math.abs(enemy.y - ally.y);

  if (dist <= allyType.range) {
    enemy.hp -= Math.max(5, allyType.attack - 4);
    state.ticker = `${factions[state.activeFaction].emblem} ${factions[state.activeFaction].label} frappe ${unitTypes[enemy.type].name} (-${allyType.attack} PV)`;
    state.units = state.units.filter((u) => u.hp > 0);
  } else {
    ally.x = clamp(ally.x + Math.sign(enemy.x - ally.x), 0, COLS - 1);
    ally.y = clamp(ally.y + Math.sign(enemy.y - ally.y), 0, ROWS - 1);
    state.ticker = `${factions[state.activeFaction].emblem} ${factions[state.activeFaction].label} manœuvre ${allyType.emoji} vers la ligne de front.`;
  }
}

function nextTurn() {
  state.activeFaction = state.activeFaction === 'ALLIES' ? 'SOVIETS' : 'ALLIES';
  if (state.activeFaction === 'ALLIES') state.turn += 1;
  applyEconomy();
  skirmishTick();
  render();
}

function refreshIntel() {
  const allUnits = Object.values(unitTypes)
    .map((u) => `${u.emoji} ${u.name} [${u.class}] HP:${u.maxHp} ATK:${u.attack} RNG:${u.range} VIT:${u.speed} COUT:${u.cost}`)
    .join('\n');

  const allBuildings = Object.values(buildingTypes)
    .map((b) => `${b.emoji} ${b.name} HP:${b.maxHp} ARM:${b.armor} +${b.income}/tour`)
    .join('\n');

  const dlcText = dlcs.map((d) => ` • ${d[0]} -> ${d[1]}`).join('\n');

  intelTag.textContent =
`=== CONFIGURATION ===
Carte: ${COLS}x${ROWS} cases
DLC actifs:
${dlcText}

=== ÉCONOMIE ===
🛡️ Alliés: ${state.credits.ALLIES} crédits
🔨 Soviétiques: ${state.credits.SOVIETS} crédits

=== UNITÉS DISPONIBLES ===
${allUnits}

=== BÂTIMENTS DISPONIBLES ===
${allBuildings}

=== RÈGLES CLÉS ===
1) Chaque tour applique les revenus de bâtiments.
2) L'IA escarmouche avance ou attaque selon la portée.
3) Une unité à 0 PV est retirée du théâtre d'opérations.
4) Les DLC débloquent unités/structures spécifiques.
5) Les emoji sont les assets principaux; les formes pilotent la lisibilité tactique.`;
}

nextTurnBtn.addEventListener('click', nextTurn);
autoBtn.addEventListener('click', () => {
  if (timer) {
    clearInterval(timer);
    timer = null;
    autoBtn.textContent = '⏯ Auto';
  } else {
    timer = setInterval(nextTurn, 1700);
    autoBtn.textContent = '⏸ Pause';
  }
});

render();
