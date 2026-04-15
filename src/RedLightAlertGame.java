import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

/**
 * RedLightAlertGame - Hommage stratégique inspiré de Red Alert.
 *
 * Assets visuels :
 * - Emojis pour unités/structures (intelligemment choisis par rôle)
 * - Formes géométriques pour terrain, portée, sélection et interface tactique.
 */
public class RedLightAlertGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameConfig config = GameConfig.defaultConfig();
            GameState state = GameState.bootstrap(config);
            GameFrame frame = new GameFrame(state);
            frame.setVisible(true);
        });
    }

    enum Faction {
        ALLIES("Alliés", new Color(64, 156, 255), "🛡️"),
        SOVIETS("Soviétiques", new Color(226, 78, 78), "🔨");

        final String label;
        final Color color;
        final String emblem;

        Faction(String label, Color color, String emblem) {
            this.label = label;
            this.color = color;
            this.emblem = emblem;
        }
    }

    enum Dlc {
        BASE("Base Game", "Campagne Europe + guerre tactique standard"),
        AFTERMATH("Aftermath", "Technologies expérimentales: Tesla Storm, chrono-raid"),
        COUNTERSTRIKE("Counterstrike", "Opérations de sabotage, rail-gun mobile"),
        RETALIATION("Retaliation", "Escarmouches avancées, doctrines asymétriques");

        final String title;
        final String details;

        Dlc(String title, String details) {
            this.title = title;
            this.details = details;
        }
    }

    enum UnitClass {
        INFANTRY("Infanterie", 80),
        VEHICLE("Véhicule", 120),
        AIR("Aérien", 160),
        NAVAL("Naval", 170),
        SUPPORT("Support", 100);

        final String label;
        final int cost;

        UnitClass(String label, int cost) {
            this.label = label;
            this.cost = cost;
        }
    }

    record UnitType(
            String id,
            String name,
            String emoji,
            UnitClass unitClass,
            int maxHp,
            int attack,
            int range,
            int speed,
            Set<Dlc> dlcRequirements
    ) {}

    record BuildingType(
            String id,
            String name,
            String emoji,
            int maxHp,
            int armor,
            int income,
            Set<Dlc> dlcRequirements
    ) {}

    static final class ContentRegistry {
        static final List<UnitType> UNIT_TYPES = List.of(
                new UnitType("rifleman", "Fusilier", "🪖", UnitClass.INFANTRY, 100, 17, 2, 3, Set.of(Dlc.BASE)),
                new UnitType("rocket", "Lance-roquettes", "🚀", UnitClass.INFANTRY, 75, 26, 4, 2, Set.of(Dlc.BASE)),
                new UnitType("light_tank", "Char léger", "🚓", UnitClass.VEHICLE, 180, 35, 3, 3, Set.of(Dlc.BASE)),
                new UnitType("mammoth", "Char Mammouth", "🦣", UnitClass.VEHICLE, 350, 60, 4, 2, Set.of(Dlc.BASE)),
                new UnitType("yak", "Yak d'attaque", "✈️", UnitClass.AIR, 150, 42, 5, 5, Set.of(Dlc.BASE)),
                new UnitType("submarine", "Sous-marin", "🛥️", UnitClass.NAVAL, 210, 45, 4, 3, Set.of(Dlc.BASE)),
                new UnitType("tesla_storm", "Tesla Storm Walker", "⚡", UnitClass.SUPPORT, 230, 48, 5, 2, Set.of(Dlc.AFTERMATH)),
                new UnitType("chrono_commando", "Commando Chrono", "⏱️", UnitClass.INFANTRY, 130, 80, 1, 6, Set.of(Dlc.AFTERMATH)),
                new UnitType("railgun", "Railgun Mobile", "🧲", UnitClass.VEHICLE, 220, 72, 6, 2, Set.of(Dlc.COUNTERSTRIKE)),
                new UnitType("drone_swarm", "Nuée de Drones", "🛰️", UnitClass.AIR, 160, 44, 3, 5, Set.of(Dlc.RETALIATION))
        );

        static final List<BuildingType> BUILDING_TYPES = List.of(
                new BuildingType("hq", "QG", "🏛️", 700, 12, 80, Set.of(Dlc.BASE)),
                new BuildingType("refinery", "Raffinerie", "⛽", 430, 8, 120, Set.of(Dlc.BASE)),
                new BuildingType("barracks", "Caserne", "🏕️", 300, 6, 25, Set.of(Dlc.BASE)),
                new BuildingType("war_factory", "Usine de guerre", "🏭", 520, 10, 35, Set.of(Dlc.BASE)),
                new BuildingType("tesla_lab", "Laboratoire Tesla", "🔬", 360, 7, 45, Set.of(Dlc.AFTERMATH)),
                new BuildingType("counter_ops", "Bureau Counter Ops", "🧭", 340, 7, 40, Set.of(Dlc.COUNTERSTRIKE)),
                new BuildingType("retaliation_hub", "Hub Retaliation", "🧠", 370, 8, 50, Set.of(Dlc.RETALIATION))
        );

        static List<UnitType> availableUnits(Set<Dlc> enabled) {
            return UNIT_TYPES.stream().filter(u -> enabled.containsAll(u.dlcRequirements)).toList();
        }

        static List<BuildingType> availableBuildings(Set<Dlc> enabled) {
            return BUILDING_TYPES.stream().filter(b -> enabled.containsAll(b.dlcRequirements)).toList();
        }
    }

    static final class GameConfig {
        final int mapCols;
        final int mapRows;
        final Set<Dlc> enabledDlcs;

        GameConfig(int mapCols, int mapRows, Set<Dlc> enabledDlcs) {
            this.mapCols = mapCols;
            this.mapRows = mapRows;
            this.enabledDlcs = enabledDlcs;
        }

        static GameConfig defaultConfig() {
            return new GameConfig(16, 10, EnumSet.of(Dlc.BASE, Dlc.AFTERMATH, Dlc.COUNTERSTRIKE, Dlc.RETALIATION));
        }
    }

    static final class Unit {
        final UUID id = UUID.randomUUID();
        final UnitType type;
        final Faction faction;
        int hp;
        Point tile;

        Unit(UnitType type, Faction faction, Point tile) {
            this.type = type;
            this.faction = faction;
            this.hp = type.maxHp;
            this.tile = tile;
        }
    }

    static final class Building {
        final UUID id = UUID.randomUUID();
        final BuildingType type;
        final Faction faction;
        int hp;
        Point tile;

        Building(BuildingType type, Faction faction, Point tile) {
            this.type = type;
            this.faction = faction;
            this.hp = type.maxHp;
            this.tile = tile;
        }
    }

    static final class GameState {
        final GameConfig config;
        final List<Unit> units = new ArrayList<>();
        final List<Building> buildings = new ArrayList<>();
        final Map<Faction, Integer> credits = new EnumMap<>(Faction.class);
        int turn = 1;
        Faction activeFaction = Faction.ALLIES;
        String ticker = "Bienvenue Commandant: sécurisez les nœuds énergétiques ⚡";

        GameState(GameConfig config) {
            this.config = config;
            credits.put(Faction.ALLIES, 2500);
            credits.put(Faction.SOVIETS, 2500);
        }

        static GameState bootstrap(GameConfig config) {
            GameState s = new GameState(config);
            List<UnitType> available = ContentRegistry.availableUnits(config.enabledDlcs);
            List<BuildingType> bAvailable = ContentRegistry.availableBuildings(config.enabledDlcs);

            s.units.add(new Unit(findUnit(available, "rifleman"), Faction.ALLIES, new Point(2, 2)));
            s.units.add(new Unit(findUnit(available, "light_tank"), Faction.ALLIES, new Point(4, 2)));
            s.units.add(new Unit(findUnit(available, "mammoth"), Faction.ALLIES, new Point(3, 4)));
            s.units.add(new Unit(findUnit(available, "rocket"), Faction.SOVIETS, new Point(11, 6)));
            s.units.add(new Unit(findUnit(available, "yak"), Faction.SOVIETS, new Point(12, 4)));
            s.units.add(new Unit(findUnit(available, "railgun"), Faction.SOVIETS, new Point(13, 7)));

            s.buildings.add(new Building(findBuilding(bAvailable, "hq"), Faction.ALLIES, new Point(1, 1)));
            s.buildings.add(new Building(findBuilding(bAvailable, "war_factory"), Faction.ALLIES, new Point(2, 6)));
            s.buildings.add(new Building(findBuilding(bAvailable, "hq"), Faction.SOVIETS, new Point(14, 8)));
            s.buildings.add(new Building(findBuilding(bAvailable, "tesla_lab"), Faction.SOVIETS, new Point(12, 8)));

            return s;
        }

        void nextTurn() {
            activeFaction = activeFaction == Faction.ALLIES ? Faction.SOVIETS : Faction.ALLIES;
            if (activeFaction == Faction.ALLIES) {
                turn++;
            }
            applyEconomy();
            botSkirmishTick();
        }

        private void applyEconomy() {
            for (Faction f : Faction.values()) {
                int income = buildings.stream().filter(b -> b.faction == f).mapToInt(b -> b.type.income).sum();
                credits.computeIfPresent(f, (k, v) -> v + income);
            }
        }

        private void botSkirmishTick() {
            Random r = new Random();
            Unit enemy = units.stream().filter(u -> u.faction != activeFaction).findAny().orElse(null);
            Unit ally = units.stream().filter(u -> u.faction == activeFaction).findAny().orElse(null);
            if (enemy != null && ally != null) {
                int dist = Math.abs(enemy.tile.x - ally.tile.x) + Math.abs(enemy.tile.y - ally.tile.y);
                if (dist <= ally.type.range) {
                    enemy.hp -= Math.max(5, ally.type.attack - 4);
                    ticker = activeFaction.emblem + " " + activeFaction.label + " frappe " + enemy.type.name + " (-" + ally.type.attack + " PV)";
                    units.removeIf(u -> u.hp <= 0);
                } else {
                    ally.tile = new Point(
                            clamp(ally.tile.x + Integer.compare(enemy.tile.x, ally.tile.x), 0, config.mapCols - 1),
                            clamp(ally.tile.y + Integer.compare(enemy.tile.y, ally.tile.y), 0, config.mapRows - 1)
                    );
                    ticker = activeFaction.emblem + " " + activeFaction.label + " manœuvre " + ally.type.emoji + " vers la ligne de front.";
                }
            }
        }

        private static int clamp(int val, int min, int max) {
            return Math.max(min, Math.min(max, val));
        }

        private static UnitType findUnit(List<UnitType> src, String id) {
            return src.stream().filter(u -> u.id.equals(id)).findFirst().orElse(src.get(0));
        }

        private static BuildingType findBuilding(List<BuildingType> src, String id) {
            return src.stream().filter(b -> b.id.equals(id)).findFirst().orElse(src.get(0));
        }
    }

    static final class GameFrame extends JFrame {
        GameFrame(GameState state) {
            super("Red Light Alert ⚠️ - Edition DLC Complète");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1260, 820);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            GamePanel panel = new GamePanel(state);
            InfoPanel info = new InfoPanel(state, panel);

            add(panel, BorderLayout.CENTER);
            add(info, BorderLayout.EAST);

            javax.swing.Timer timer = new javax.swing.Timer(1700, (ActionEvent e) -> {
                state.nextTurn();
                panel.repaint();
                info.refresh();
            });
            timer.start();
        }
    }

    static final class GamePanel extends JPanel {
        private static final int TILE = 56;
        private final GameState state;

        GamePanel(GameState state) {
            this.state = state;
            setPreferredSize(new Dimension(state.config.mapCols * TILE + 20, state.config.mapRows * TILE + 20));
            setBackground(new Color(19, 24, 28));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            drawTerrain(g2);
            drawBuildings(g2);
            drawUnits(g2);
            drawHUDOverlay(g2);
        }

        private void drawTerrain(Graphics2D g2) {
            for (int y = 0; y < state.config.mapRows; y++) {
                for (int x = 0; x < state.config.mapCols; x++) {
                    int px = x * TILE + 10;
                    int py = y * TILE + 10;

                    // Formes géométriques comme tuiles de terrain
                    Polygon hexHint = hexagon(px + TILE / 2, py + TILE / 2, TILE / 2 - 4);
                    g2.setColor(((x + y) % 2 == 0) ? new Color(42, 66, 49) : new Color(45, 74, 61));
                    g2.fillRect(px, py, TILE - 2, TILE - 2);
                    g2.setColor(new Color(73, 104, 86, 120));
                    g2.drawPolygon(hexHint);
                }
            }
        }

        private void drawBuildings(Graphics2D g2) {
            for (Building b : state.buildings) {
                int px = b.tile.x * TILE + 10;
                int py = b.tile.y * TILE + 10;

                g2.setColor(alpha(b.faction.color, 120));
                g2.fillRoundRect(px + 4, py + 4, TILE - 10, TILE - 10, 12, 12);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
                g2.drawString(b.type.emoji, px + 14, py + 34);

                hpBar(g2, px + 4, py + TILE - 10, TILE - 10, b.hp, b.type.maxHp);
            }
        }

        private void drawUnits(Graphics2D g2) {
            for (Unit u : state.units) {
                int px = u.tile.x * TILE + 10;
                int py = u.tile.y * TILE + 10;

                g2.setColor(alpha(u.faction.color, 220));
                g2.fillOval(px + 12, py + 8, TILE - 24, TILE - 24);

                g2.setColor(Color.BLACK);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
                g2.drawString(u.type.emoji, px + 16, py + 30);

                // Cercle de portée (forme géométrique)
                int r = u.type.range * 8;
                g2.setColor(alpha(u.faction.color, 65));
                g2.drawOval(px + TILE / 2 - r / 2, py + TILE / 2 - r / 2, r, r);

                hpBar(g2, px + 8, py + TILE - 14, TILE - 16, u.hp, u.type.maxHp);
            }
        }

        private void drawHUDOverlay(Graphics2D g2) {
            int w = state.config.mapCols * TILE;
            g2.setColor(new Color(5, 11, 16, 190));
            g2.fillRoundRect(16, 16, w - 24, 40, 12, 12);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Tour " + state.turn + " • Phase: " + state.activeFaction.label + " " + state.activeFaction.emblem, 24, 40);
            g2.drawString(state.ticker, 320, 40);
        }

        private static Polygon hexagon(int cx, int cy, int r) {
            int[] xs = new int[6];
            int[] ys = new int[6];
            for (int i = 0; i < 6; i++) {
                double a = Math.toRadians(60 * i - 30);
                xs[i] = (int) (cx + r * Math.cos(a));
                ys[i] = (int) (cy + r * Math.sin(a));
            }
            return new Polygon(xs, ys, 6);
        }

        private static void hpBar(Graphics2D g2, int x, int y, int w, int hp, int maxHp) {
            g2.setColor(new Color(22, 22, 22, 220));
            g2.fillRect(x, y, w, 5);
            float ratio = Math.max(0f, Math.min(1f, hp / (float) maxHp));
            g2.setColor(ratio > 0.5f ? new Color(74, 201, 86) : ratio > 0.25f ? new Color(241, 181, 66) : new Color(227, 66, 66));
            g2.fillRect(x, y, Math.round(w * ratio), 5);
        }

        private static Color alpha(Color c, int alpha) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
        }
    }

    static final class InfoPanel extends JPanel {
        private final GameState state;
        private final JTextArea details = new JTextArea();

        InfoPanel(GameState state, GamePanel panel) {
            this.state = state;
            setPreferredSize(new Dimension(380, 800));
            setBackground(new Color(13, 17, 21));
            setLayout(new BorderLayout());

            JLabel title = new JLabel("⚠️ Red Light Alert — Dossier Opérationnel", SwingConstants.CENTER);
            title.setForeground(Color.WHITE);
            title.setFont(new Font("SansSerif", Font.BOLD, 16));
            title.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 8));

            details.setEditable(false);
            details.setLineWrap(true);
            details.setWrapStyleWord(true);
            details.setBackground(new Color(13, 17, 21));
            details.setForeground(new Color(218, 223, 231));
            details.setFont(new Font("Monospaced", Font.PLAIN, 12));
            details.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

            JButton forceTick = new JButton("▶ Tour Suivant");
            forceTick.addActionListener(e -> {
                state.nextTurn();
                panel.repaint();
                refresh();
            });

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setBackground(new Color(13, 17, 21));
            bottom.setBorder(BorderFactory.createEmptyBorder(8, 12, 16, 12));
            bottom.add(forceTick, BorderLayout.CENTER);

            add(title, BorderLayout.NORTH);
            add(new JScrollPane(details), BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);

            refresh();
        }

        void refresh() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== CONFIGURATION ===\n");
            sb.append("Carte: ").append(state.config.mapCols).append("x").append(state.config.mapRows).append(" cases\n");
            sb.append("DLC actifs:\n");
            for (Dlc dlc : state.config.enabledDlcs) {
                sb.append(" • ").append(dlc.title).append(" -> ").append(dlc.details).append("\n");
            }

            sb.append("\n=== ÉCONOMIE ===\n");
            for (Faction faction : Faction.values()) {
                sb.append(faction.emblem).append(" ").append(faction.label)
                        .append(": ").append(state.credits.get(faction)).append(" crédits\n");
            }

            sb.append("\n=== UNITÉS DISPONIBLES (selon DLC) ===\n");
            for (UnitType u : ContentRegistry.availableUnits(state.config.enabledDlcs)) {
                sb.append(u.emoji).append(" ").append(u.name)
                        .append(" [").append(u.unitClass.label).append("]")
                        .append(" HP:").append(u.maxHp)
                        .append(" ATK:").append(u.attack)
                        .append(" RNG:").append(u.range)
                        .append(" VIT:").append(u.speed)
                        .append(" COUT:").append(u.unitClass.cost)
                        .append("\n");
            }

            sb.append("\n=== BÂTIMENTS DISPONIBLES ===\n");
            for (BuildingType b : ContentRegistry.availableBuildings(state.config.enabledDlcs)) {
                sb.append(b.emoji).append(" ").append(b.name)
                        .append(" HP:").append(b.maxHp)
                        .append(" ARM:").append(b.armor)
                        .append(" +").append(b.income).append("/tour")
                        .append("\n");
            }

            sb.append("\n=== RÈGLES CLÉS ===\n")
                    .append("1) Chaque tour applique les revenus de bâtiments.\n")
                    .append("2) L'IA escarmouche avance ou attaque selon la portée.\n")
                    .append("3) Une unité à 0 PV est retirée du théâtre d'opérations.\n")
                    .append("4) Les DLC débloquent unités/structures spécifiques.\n")
                    .append("5) Les emoji sont les assets principaux; les formes pilotent la lisibilité tactique.\n");

            details.setText(sb.toString());
            details.setCaretPosition(0);
        }
    }
}
