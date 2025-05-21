// ✅ Version Complète et Optimisée - GameManager.java

package fr.crafity.dropper.game;

import fr.crafity.dropper.Dropper;
import fr.crafity.dropper.data.LevelData;
import fr.crafity.dropper.data.PlayerData;
import fr.crafity.dropper.listeners.DropEventListener;
import fr.crafity.dropper.util.Animations;
import fr.crafity.dropper.util.LocationUtil;
import fr.crafity.dropper.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockType;
import org.bukkit.boss.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GameManager {

    private final Dropper plugin;
    private final List<UUID> rotation = new ArrayList<>();
    private static final Map<UUID, PlayerData> players = new HashMap<>();

    private boolean isRunning = false;
    private boolean isPaused = false;
    private int currentIndex = 0;
    private int PerfectDropsInThisRotation = 0;
    private String currentLevel = null;
    public final Set<UUID> activeTurn = new HashSet<>();
    private UUID forcedPlayer = null;
    public static final Set<UUID> blockedPlayers = new HashSet<>();

    int minimumPlayersToWin = 1; //1;

    public static Map<UUID, PlayerData> getPlayers() {
        return players;
    }

    public final String blockStr = "§5WHO PUT THIS IN MY HEAD ?!";
    public final String colorBossBar = "§6";
    public final String colorMentionedPlayer = "§3";
    public final String colorMessage = "§e";
    public final String colorSuccess = "§a";
    public final String colorError = "§4";
    public final String colorFailed = "§c";

    public BossBar bossBar = Bukkit.createBossBar(colorBossBar + "Chargement en cours...", BarColor.YELLOW, BarStyle.SOLID);

    private void updateBossBar(Player player, String title, BarColor color, double progress) {

        BossBar currentBossBar = bossBar;

        if (bossBar != null) {
            Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
        } else {
            currentBossBar = Bukkit.createBossBar(colorBossBar + "Chargement en cours...", BarColor.YELLOW, BarStyle.SOLID);
        }

        currentBossBar.setTitle(colorBossBar + "Tour de " + colorMentionedPlayer + player.getName() + colorBossBar + " - " + colorMessage + title);
        currentBossBar.setColor(color);
        currentBossBar.setProgress(progress);

//        bossBar.removeAll();
//        Bukkit.getOnlinePlayers().forEach(bossBar::addPlayer);
    }

    private BukkitRunnable turnTimer;
    private BukkitRunnable preTurnDelay;
    private BukkitRunnable scoreboardUpdater;
    private final Set<Location> placedBlocks = new HashSet<>();
    public Set<UUID> disconnectedPlayers = new HashSet<>();
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private final Map<UUID, List<String>> dynamicKeys = new HashMap<>();
    private final Map<UUID, Objective> objectives = new HashMap<>();

    public final String StrPerfectDrop = "§6§kiii§r§6§lPerfect Drop§r§6§kiii§r";
    public final String StrSuperJump = "§d§kiii§r§d§lSuper Jump§r§d§kiii§r";
    public final String ExplainSuperJump = "§7§oPetit rappel pour le §r§d§oSuper Jump§7§o, s'il réussit, il posera 5 blocs sinon, il perdra 2 vies !";

    private final String carac1 = "§3[";
    private final String carac2 = "§3] ";

    public final String sss(Integer i) {
        return i <= 1 ? "" : "s";
    }

    public final String format_lives = "§c♥";
    public final String format_jumps = "§e⇪";
    public final String format_perfectDrops = "§6★"; //"§6★⇪";
    public final String format_systeme = "§8⚙";

    public final String prefix_lives = carac1 + format_lives + carac2;
    public final String prefix_jumps = carac1 + format_jumps + carac2;
    public final String prefix_perfectDrops = carac1 + format_perfectDrops + carac2;
    public final String prefix_systeme = carac1 + format_systeme + carac2;

    private Scoreboard scoreboard;
    private final Map<UUID, List<String>> scoreboardCache = new HashMap<>();

    public GameManager(Dropper plugin) {
        this.plugin = plugin;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            scoreboard = manager.getNewScoreboard();
        } else {
            scoreboard = null;
        }
    }

    // ➤ Ajout et Suppression de Joueurs
    public void addPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (rotation.add(uuid)) {
            players.put(uuid, new PlayerData(player));
            updateVisualsInfos();
        }
    }

    public void removePlayer(Player player) {
        UUID uuid = player.getUniqueId();
        if (rotation.remove(uuid)) {
            players.remove(uuid);
            updateVisualsInfos();
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return players.get(uuid);
    }

    public List<UUID> getRotation() {
        return rotation;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public boolean isInRotation(Player player) {
        return rotation.contains(player.getUniqueId());
    }

    public void clearAllPlayers() {
        rotation.clear();
        players.clear();
        Bukkit.getOnlinePlayers().forEach(p -> p.getInventory().setHelmet(null));
    }

    // ➤ Gestion de l'État du Jeu
    public void pauseGame() {
        isPaused = true;
        cancelTurnTimer();
        updateVisualsInfos();
        resetNameTagVisibility();
        clearAllBossBars();
        stopScoreboardUpdater();
    }

    public void resumeGame() {
        isPaused = false;
        updateVisualsInfos();
        startScoreboardUpdater();
        nextTurn();
    }

    /**
     * Stoppe proprement la partie.
     *
     * @param playersTraitement Si true, téléporte les joueurs au spawn et réinitialise leurs états.
     *                          À mettre sur false lors d'un arrêt serveur ou d'un rechargement.
     */
    public void stopGame(boolean playersTraitement) {
        isRunning = false;
        isPaused = false;

        PerfectDropsInThisRotation = 0;
        currentIndex = 0;

        currentLevel = null;
        forcedPlayer = null;

        cancelTurnTimer();
        clearAllBossBars();
        resetPlacedBlocks();
        updateVisualsInfos();
        stopScoreboardUpdater();
        resetNameTagVisibility();

        blockedPlayers.clear();
        playerScoreboards.clear();
        objectives.clear();
        scoreboardCache.clear();

        if (playersTraitement) {
            Location spawn = LocationUtil.fromConfig(plugin.getConfig().getConfigurationSection("spawn"));

            Bukkit.getOnlinePlayers().forEach(p -> p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard()));

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (spawn != null) player.teleport(spawn);
                player.setGameMode(GameMode.ADVENTURE);
                player.getInventory().setHelmet(null);
            }
        }
    }

    private void cancelTurnTimer() {
        if (turnTimer != null) {
            turnTimer.cancel();
            turnTimer = null;
        }

        if (preTurnDelay != null) {
            preTurnDelay.cancel();
            preTurnDelay = null;
        }
    }

    public LevelData loadLevel(String levelName) {
        File file = new File(plugin.getDataFolder(), "levels/" + levelName + ".yml");
        return LevelData.fromFile(file);
    }

    public Set<String> getAvailableLevels() {
        File levelsDir = new File(plugin.getDataFolder(), "levels");
        if (!levelsDir.exists() || !levelsDir.isDirectory()) return Collections.emptySet();

        Set<String> levelNames = new HashSet<>();
        File[] files = levelsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                levelNames.add(file.getName().replace(".yml", ""));
            }
        }
        return levelNames;
    }

    public void reloadLevels() {

        File levelsDir = new File(plugin.getDataFolder(), "levels");
        if (!levelsDir.exists() || !levelsDir.isDirectory()) return;

        Set<String> reloadedLevels = new HashSet<>();
        File[] files = levelsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String levelName = file.getName().replace(".yml", "");
                reloadedLevels.add(levelName);
            }
        }
    }

    public void cancelCurrentTurn() {
        cancelTurnTimer();
        activeTurn.clear();
        clearBossBar();
    }

    public boolean isPlayerInTurn(Player player) {
        return activeTurn.contains(player.getUniqueId());
    }
    public boolean isPlayerInTurn(OfflinePlayer player) {
        return activeTurn.contains(player.getUniqueId());
    }

    public Player getCurrentPlayerInTurn() {
        return activeTurn.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    // ➤ BossBar et Effets Visuels
    public void clearAllBossBars() {
        plugin.getServer().getBossBars().forEachRemaining(bar -> {
            Bukkit.getOnlinePlayers().forEach(bar::removePlayer);
        });
        clearBossBar();
    }

    private void clearBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    private void resetPlayerState(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.removePotionEffect(PotionEffectType.GLOWING);
        if (bossBar != null) bossBar.removePlayer(player);
        activeTurn.remove(player.getUniqueId());
        updateVisualsInfos();
    }

    public void updateVisualsInfos() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            updateTabList(player);
            updateScoreboard(player);
        });
    }

    public void updateTabList(Player player) {
        if (!plugin.getConfig().getBoolean("tab_list", true)) return;

        UUID uuid = player.getUniqueId();
        PlayerData data = players.get(uuid);

        boolean inRotation = rotation.contains(uuid);
        boolean isCurrent = activeTurn.contains(uuid);
        boolean isEliminated = data != null && data.isEliminated();

        String color = isEliminated ? "§7§m" : (isCurrent ? "§2§l→ " : colorMentionedPlayer);
        String name = player.getName();
        String separator = " §7| ";
        String eyeIcon = !inRotation ? "§7👁 " : "";

        String orderPrefix = inRotation ? (isCurrent ? "§0§0§0§0§0" : "§1§1§1§1§1") : "§2§2§2§2§2";

        int lives = data != null ? data.getLives() : 0;
        int jumps = data != null ? data.getJumpsThisTurn() : 0;
        int perfectDrops = data != null ? data.getPerfectDropsThisTurn() : 0;

        String finalName = orderPrefix + eyeIcon + color + name;

        if (inRotation) {
            finalName += isRunning ? "§r" + separator + colorFailed + lives + format_lives + " " + colorMessage + jumps + format_jumps + " §6" + perfectDrops + format_perfectDrops : separator + "§3waiting...";
        }

        updateNameTagVisibility();

        if (!player.getPlayerListName().equals(finalName)) {
            player.setPlayerListName(finalName);
        }

    }

    private void updateNameTagVisibility() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        final Team hiddenTeam = scoreboard.getTeam("hidden_names");

        if (hiddenTeam == null) {
            Team newTeam = scoreboard.registerNewTeam("hidden_names");
            newTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            newTeam.setCanSeeFriendlyInvisibles(false);
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            UUID uuid = player.getUniqueId();
            boolean isCurrent = activeTurn.contains(uuid);

            if (player.getGameMode() == GameMode.SPECTATOR && !activeTurn.contains(player.getUniqueId())) {
                scoreboard.getTeam("hidden_names").addEntry(player.getName());
            } else {
                scoreboard.getTeam("hidden_names").removeEntry(player.getName());
            }
        });
    }

    public void updateScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        UUID uuid = player.getUniqueId();
        PlayerData data = players.get(uuid);
        if (data == null) return;

        Objective obj = objectives.get(uuid);
        Scoreboard sb = playerScoreboards.get(uuid);

        if (obj == null || sb == null) {
            initScoreboard(player);
            obj = objectives.get(uuid);
            sb = playerScoreboards.get(uuid);
            if (obj == null || sb == null) return;
        }

        List<String> rawLines = plugin.getConfig().getStringList("scoreboard.lines");
        List<String> newLines = new ArrayList<>();

        for (int i = 0; i < rawLines.size(); i++) {
            String parsed = parseScoreboardLine(rawLines.get(i), player);
            // Caractère invisible pour rendre chaque ligne unique
            String uniqueLine = parsed + ChatColor.COLOR_CHAR + ChatColor.values()[i % ChatColor.values().length].getChar();
            newLines.add(uniqueLine);
        }

        List<String> oldLines = scoreboardCache.getOrDefault(uuid, Collections.emptyList());

        if (newLines.equals(oldLines)) return; // ✅ Pas de changement → on skip

        // On met à jour le cache
        scoreboardCache.put(uuid, newLines);

        // On supprime les anciennes lignes qui ne sont plus utilisées
        Set<String> currentEntries = sb.getEntries();
        for (String entry : currentEntries) {
            if (!newLines.contains(entry)) sb.resetScores(entry);
        }

        // On ajoute les nouvelles lignes avec des scores décroissants
        int score = newLines.size();
        for (String line : newLines) {
            obj.getScore(line).setScore(score--); // score nécessaire mais caché
        }
    }

    public void initScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) return;

        UUID uuid = player.getUniqueId();

        // Si le joueur a déjà un scoreboard géré, on ne fait rien
        if (playerScoreboards.containsKey(uuid) && objectives.containsKey(uuid)) return;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        // Nouveau scoreboard indépendant
        Scoreboard sb = manager.getNewScoreboard();

        // Nom unique pour éviter les conflits
        String objectiveName = "dropper_" + uuid.toString().substring(0, 8);

        Objective obj = sb.registerNewObjective(objectiveName, "dummy",
                plugin.getConfig().getString("scoreboard.title", "§6§l▶ §e§lDROPPER §6§l◀"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        player.setScoreboard(sb); // ⬅️ assignation une seule fois ici
        playerScoreboards.put(uuid, sb);
        objectives.put(uuid, obj);
    }

    private String parseScoreboardLine(String line, Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = getPlayerData(uuid);
        if (data == null) return "";

        int index = rotation.indexOf(uuid);
        int position = index + 1;
        int etaPlayers = 0;
        int etaTime = 0;

        if (isRunning && index != -1 && !activeTurn.contains(uuid)) {
            int diff = index - currentIndex;
            if (diff < 0) diff += rotation.size(); // rotation circulaire
            etaPlayers = diff;
            LevelData level = loadLevel(getCurrentLevel());
            int jumpTime = (level != null) ? level.getJumpTime() : plugin.getConfig().getInt("default_jump_time", 10);
            etaTime = etaPlayers * jumpTime;
        }

        boolean isInRotation = rotation.contains(uuid);
        boolean isEliminated = data.isEliminated();
        boolean isCurrentTurn = activeTurn.contains(uuid);
        boolean hasSuperJump = data.hasPendingSuperJump();
        boolean isSpectator = player.getGameMode() == GameMode.SPECTATOR;

        if (!isInRotation) {
            position = -1;
        } else if (isRunning && !isCurrentTurn) {
            LevelData level = loadLevel(getCurrentLevel());
            int jumpTime = (level != null) ? level.getJumpTime() : plugin.getConfig().getInt("default_jump_time", 10);
            etaPlayers = position - 1;
            etaTime = etaPlayers * jumpTime;
        }

        String playerStatus;
        if (isCurrentTurn) {
            playerStatus = hasSuperJump ? "§dSuper Jump ✦" : "§aC'est ton tour !";
        } else if (isSpectator && !isCurrentTurn) {
            playerStatus = "§7Spectateur";
        } else if (isEliminated) {
            playerStatus = colorError + "Éliminé";
        } else {
            playerStatus = colorMessage + "En lice...";
        }

        // Remplacement standard
        line = line
                .replace("%level%", getCurrentLevel() != null ? getCurrentLevel() : "Aucun")
                .replace("%player%", player.getName())
                .replace("%lives%", String.valueOf(data.getLives()))
                .replace("%jumps%", String.valueOf(data.getJumpsThisTurn()))
                .replace("%perfect%", String.valueOf(data.getPerfectDropsThisTurn()))
                .replace("%position%", position == -1 ? "?" : String.valueOf(position))
                .replace("%eta%", etaTime <= 0 ? "0" : String.valueOf(etaTime))
                .replace("%etaPlayers%", etaPlayers <= 0 ? "0" : String.valueOf(etaPlayers))
                .replace("%superJump%", hasSuperJump ? "§a✔" : "§c✘")
                .replace("%playerStatus%", playerStatus)
                .replace("%inRotation%", isInRotation ? "§a✔" : "§c✘")
                .replace("%cF%", colorFailed)
                .replace("%cMP%", colorMentionedPlayer)
                .replace("%fL%", format_lives)
                .replace("%fJ%", format_jumps)
                .replace("%fPD%", format_perfectDrops);
        //.replace("%fSJ%", format_superJumps);

        // Remplacement dynamique des %s:var:singulier:pluriel%
        Pattern smartPattern = Pattern.compile("%s:([a-zA-Z0-9_]+):([^:%]+):([^%]+)%");
        Matcher matcher = smartPattern.matcher(line);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);     // ex: etaPlayers
            String singular = matcher.group(2);    // ex: joueur
            String plural = matcher.group(3);      // ex: joueurs
            int value = switch (varName) {
                case "etaPlayers" -> etaPlayers;
                case "eta" -> etaTime;
                case "lives" -> data.getLives();
                case "perfect" -> data.getPerfectDropsThisTurn();
                case "jumps" -> data.getJumpsThisTurn();
                default -> -1;
            };

            String replacement = value >= 0 ? value + " " + (value <= 1 ? singular : plural) : "?";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    public void startScoreboardUpdater() {
        if (scoreboardUpdater != null) scoreboardUpdater.cancel();

        scoreboardUpdater = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    cancel();
                    return;
                }

                for (UUID uuid : rotation) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player == null || !player.isOnline()) continue;

                    updateScoreboard(player);
                }
            }
        };

        scoreboardUpdater.runTaskTimer(plugin, 0L, 20L); // mise à jour toutes les secondes
    }

    public void stopScoreboardUpdater() {
        if (scoreboardUpdater != null) {
            scoreboardUpdater.cancel();
            scoreboardUpdater = null;
        }
    }

    public void resetScoreboardCache() {
        scoreboardCache.clear();
        Bukkit.getOnlinePlayers().forEach(this::updateScoreboard);
    }

    private void resetNameTagVisibility() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team hiddenTeam = scoreboard.getTeam("hidden_names");

        if (hiddenTeam != null) {
            hiddenTeam.getEntries().forEach(hiddenTeam::removeEntry);
        }
    }

    public void nextTurn() {
        if (!isRunning || isPaused || rotation.isEmpty()) return;

        if (rotation.isEmpty()) {
            checkWinCondition();
            return;
        }

        if (currentIndex >= rotation.size()) currentIndex = 0;

        if (forcedPlayer != null) {
            Player p = Bukkit.getPlayer(forcedPlayer);
            forcedPlayer = null;
            if (p != null && !players.get(p.getUniqueId()).isEliminated()) {
                MessageUtil.broadcast(colorMentionedPlayer + p.getName() + " " + colorSuccess + "reprend son tour après le forçage.");
                cancelTurnTimer();
                startPlayerTurn(p);
                return;
            }
        }

        checkWinCondition();

        cancelTurnTimer();

        int attempts = 0;
        while (attempts < rotation.size()) {
            UUID uuid = rotation.get(currentIndex);
            Player player = Bukkit.getPlayer(uuid);

            if (player != null && !players.get(uuid).isEliminated()) {
                startPlayerTurn(player);
                return;
            }

            currentIndex = (currentIndex + 1) % rotation.size();
            attempts++;

            // ✅ Vérification ici quand on revient à 0 ➔ Début d'une nouvelle rotation
            if (currentIndex == 0) {
                handleRotationComplete();
                // Vérifie la condition de victoire après traitement
                if (rotation.isEmpty()) {
                    checkWinCondition();
                    return;
                }
            }
        }

    }

    private void handleRotationComplete() {
        for (UUID uuid : rotation) {
            if (disconnectedPlayers.contains(uuid)) {
                PlayerData data = players.get(uuid);
                if (data == null) continue;

                int max_missed_turn = plugin.getConfig().getInt("max_missed_turn", 3);

                if (max_missed_turn == -1) return;

                data.incrementMissedTurns();

                if (data.getMissedTurns() >= max_missed_turn) {
                    data.setEliminated(true);
                    data.setPendingSuperJump(false);
                    disconnectedPlayers.remove(uuid);
                    MessageUtil.broadcast(prefix_systeme + colorMentionedPlayer + Bukkit.getOfflinePlayer(uuid).getName() + " " + colorError + "été éliminé après avoir passé " + colorMessage + "3 tours" + colorError + ".");
                } else {
                    int remaining = 3 - data.getMissedTurns();

                    int give_super_jump_after_missed_turns = plugin.getConfig().getInt("give_super_jump_after_missed_turns", 1);
                    boolean super_jump_on_reconnection = plugin.getConfig().getBoolean("super_jump_on_reconnection", true);

                    if (give_super_jump_after_missed_turns != -1 || !super_jump_on_reconnection) {
                        if (data.getMissedTurns() >= give_super_jump_after_missed_turns) {
                            data.setPendingSuperJump(super_jump_on_reconnection);
                        }
                    }

                    MessageUtil.broadcast(prefix_systeme + colorMentionedPlayer + Bukkit.getOfflinePlayer(uuid).getName() + " " + colorFailed + "est toujours déconnecté. Élimination dans " + colorMessage + remaining + " tour" + sss(remaining) + colorFailed + ".");
                }
            }
        }
    }

    public void startGame(String levelName) {
        LevelData levelData = loadLevel(levelName);
        if (levelData == null) {
            MessageUtil.broadcast(prefix_systeme + colorFailed + "Erreur : Le niveau " + colorMessage + levelName + " " + colorFailed + "n'existe pas ou est corrompu.");
            return;
        }

        currentLevel = levelName;
        isRunning = true;
        currentIndex = 0;

        BossBar loadBar = Bukkit.createBossBar(colorBossBar + "Chargement du niveau... (" + colorSuccess + "0§6%)", BarColor.YELLOW, BarStyle.SEGMENTED_10);
        Bukkit.getOnlinePlayers().forEach(loadBar::addPlayer);

        new BukkitRunnable() {
            final List<Runnable> tasks = new ArrayList<>(List.of(
                    () -> {
                        if (levelData.getJumpPoint() == null) {
                            MessageUtil.broadcast(prefix_systeme + colorFailed + "Erreur : Point de saut non défini pour " + colorMessage + levelName + colorFailed + ".");
                            throw new RuntimeException("Chargement annulé.");
                        }
                    },
                    () -> {
                        if (levelData.getLives() <= 0 || levelData.getJumpTime() <= 0) {
                            MessageUtil.broadcast(prefix_systeme + colorFailed + "Erreur : Paramètres de vies ou temps invalides pour " + colorMessage + levelName + colorFailed + ".");
                            throw new RuntimeException("Chargement annulé.");
                        }
                    },
                    () -> {
                        if (levelData.getLimit1() == null || levelData.getLimit2() == null) {
                            MessageUtil.broadcast(prefix_systeme + colorFailed + "Erreur : Les limites ne sont pas correctement définies pour " + colorMessage + levelName + colorFailed + ".");
                            throw new RuntimeException("Chargement annulé.");
                        }
                    },
                    () -> {
                        List<String> blocks = plugin.getConfig().getStringList("blocks");
                        boolean block_on_head = plugin.getConfig().getBoolean("block_on_head", true);

                        blockedPlayers.clear();

                        for (Player player : Bukkit.getOnlinePlayers()) {
                            UUID uuid = player.getUniqueId();
                            PlayerData data = players.get(uuid);
                            boolean inRotation = rotation.contains(uuid);

                            player.setGameMode(GameMode.SPECTATOR);
                            data.resetJumpsThisTurn();
                            data.resetPerfectDropsThisTurn();
                            resetScoreboardCache();

                            if (inRotation) {
                                data.reset(levelData.getLives());
                                player.removePotionEffect(PotionEffectType.GLOWING);

                                initScoreboard(player);
                                updateScoreboard(player);

                                if (block_on_head) {
                                    Material mat = Material.valueOf(blocks.get(rotation.indexOf(uuid) % blocks.size()));
                                    ItemStack block = new ItemStack(mat);
                                    ItemMeta meta = block.getItemMeta();
                                    meta.setDisplayName(blockStr);
                                    block.setItemMeta(meta);
                                    player.getInventory().setHelmet(block);
                                }
                            }
                        }

                    },
                    () -> {
                        Location jump = levelData.getJumpPoint();
                        Bukkit.getOnlinePlayers().forEach(player -> player.teleport(jump));

                        StringBuilder order = new StringBuilder(prefix_systeme + "§7Ordre de passage : ");
                        for (int i = 0; i < rotation.size(); i++) {
                            UUID uuid = rotation.get(i);
                            Player p = Bukkit.getPlayer(uuid);
                            if (p != null) {
                                order.append(colorMentionedPlayer).append(p.getName());
                                if (i < rotation.size() - 1) order.append("§7, ");

                                // ✅ Ajout ici : init + update
                                initScoreboard(p);
                                updateScoreboard(p);
                            }
                        }

                        updateVisualsInfos();
                        MessageUtil.broadcast(order.toString());
                        if (plugin.getConfig().getBoolean("scoreboard.enabled", true)) startScoreboardUpdater();
                    }

            ));

            int totalSteps = tasks.size();
            int currentStep = 0;

            @Override
            public void run() {
                if (currentStep >= totalSteps) {
                    loadBar.setTitle(colorBossBar + "Chargement du niveau... (" + colorSuccess + "100§6%)");
                    loadBar.setProgress(1.0);
                    loadBar.removeAll();
                    MessageUtil.broadcast(prefix_systeme + colorSuccess + "Chargement terminé ! Le jeu commence maintenant.");
                    nextTurn();
                    cancel();
                    return;
                }

                try {
                    tasks.get(currentStep).run();
                } catch (RuntimeException e) {
                    loadBar.removeAll();
                    cancel();
                    return;
                }

                currentStep++;
                double progress = (double) currentStep / totalSteps;
                int percent = (int) (progress * 100);
                loadBar.setTitle(colorBossBar + "Chargement du niveau... (" + colorSuccess + percent + "§6%)");
                loadBar.setProgress(progress);
            }
        }.runTaskTimer(plugin, 0L, 1L); // Exécute chaque action immédiatement, la barre avance en temps réel
    }

    private void startPlayerTurn(Player player) {
        cancelTurnTimer();
        UUID uuid = player.getUniqueId();

        activeTurn.clear();
        activeTurn.add(uuid);

        updateVisualsInfos();

        LevelData level = loadLevel(currentLevel);
        if (level == null && isRunning || level.getJumpPoint() == null && isRunning) {
            MessageUtil.broadcast(prefix_systeme + colorFailed + "Erreur : Le niveau actuel est invalide ou non chargé !");
            stopGame(true);
            return;
        }


        int timeLeft = (level != null) ? level.getJumpTime() : plugin.getConfig().getInt("default_jump_time", 10);
        currentIndex = rotation.indexOf(uuid);

        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(level.getJumpPoint());
        player.setVelocity(new Vector(0, 0, 0));
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 0.2, 0), 15, 0.2, 0.1, 0.2, 0.01);

        blockedPlayers.add(uuid);
        player.sendTitle(colorFailed + "On bouge plus !", "", 0, 40, 10);

        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(colorBossBar + "Chargement en cours...", BarColor.YELLOW, BarStyle.SOLID);
        }

        updateBossBar(player, colorBossBar + "Chargement en cours...", BarColor.YELLOW, 1.0);

        handleSuperJumpChance(player);

        if (disconnectedPlayers.contains(uuid)) {
            unblockPlayerPending(player);
            return;
        }

        preTurnDelay = new BukkitRunnable() {
            @Override
            public void run() {
                preTurnDelay = null; // nettoie la référence
                blockedPlayers.remove(uuid);

                if (disconnectedPlayers.contains(uuid)) {
                    unblockPlayerPending(player);
                    cancel();
                    return;
                }

                player.sendTitle(colorSuccess + "Go !", "", 0, 20, 10);
                updateBossBar(player, colorBossBar + "Chargement terminé !", BarColor.YELLOW, 1.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * timeLeft, 1, false, false));

                player.getWorld().spawnParticle(
                        Particle.WITCH,
                        player.getLocation().add(0, 1, 0),
                        30, 0.5, 0.5, 0.5, 0.05
                );

                PlayerData data = getPlayerData(uuid);
                String jumpMessage = data.hasPendingSuperJump()
                        ? "C'est au tour de " + colorMentionedPlayer + player.getName() + colorMessage + " d'essayer de faire son " + StrSuperJump + " " + colorMessage + "! " + ExplainSuperJump
                        : "C'est au tour de " + colorMentionedPlayer + player.getName() + colorMessage + " de sauter !";

                MessageUtil.broadcast(prefix_jumps + colorMessage + jumpMessage);
                MessageUtil.actionBar(player, "§7C'est ton tour ! Saute dans les " + timeLeft + " secondes.");

                startTurnTimer(player, timeLeft);
            }
        };
        preTurnDelay.runTaskLater(plugin, 40L);
    }

    private void unblockPlayerPending(Player player) {
        blockedPlayers.remove(player.getUniqueId());
        updateBossBar(player, colorError + "Pending action...", BarColor.RED, 0.0);
    }

    private void handleSuperJumpChance(Player player) {
        double chancePercent = plugin.getConfig().getDouble("super_jump_chance", 10.0);
        if (chancePercent <= 0) return;

        double chance = chancePercent / 100.0;
        if (Math.random() < chance) {
            PlayerData data = getPlayerData(player.getUniqueId());
            if (data != null && !data.hasPendingSuperJump()) {
                data.setPendingSuperJump(true);
                MessageUtil.broadcast(prefix_jumps + colorError + "§lPlus de piquant !!! §r" + colorMentionedPlayer + player.getName() + " " + colorMessage + "a débloqué un " + StrSuperJump + " " + colorMessage + "pour ce tour !");
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_ON_FIRE, 1f, 0.8f));
            }
        }
    }

    private void startTurnTimer(Player player, int timeLeft) {
        UUID uuid = player.getUniqueId();
        final int[] timer = {timeLeft};

        turnTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isRunning || isPaused) {
                    cancel();
                    return;
                }

                if (disconnectedPlayers.contains(uuid)) {
                    unblockPlayerPending(player);
                    playerLanded(player, false);
                    cancel();
                    return;
                }

                boolean be_kind_with_players = plugin.getConfig().getBoolean("be_kind_with_players", true);
                int timerEnd = -1;

                if (!be_kind_with_players) timerEnd = 0;

                if (timer[0] <= timerEnd) {
                    // Vérifie s'il est en chute libre (=> il a sauté juste à temps)
                    if (player.getVelocity().getY() < -0.1 && !player.isOnGround()) {
                        // ⏳ Buffer de 1 seconde pour laisser le temps de tomber
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (DropEventListener.fallingPlayers.contains(player.getUniqueId()))
                                return; // saut détecté entre-temps

                            updateBossBar(player, colorFailed + "Temps écoulé " + colorBossBar + "!", BarColor.RED, 0.0);
                            playerLanded(player, false);
                        }, 20L); // 1 seconde = 20 ticks
                    } else {
                        updateBossBar(player, colorFailed + "Temps écoulé " + colorBossBar + "!", BarColor.RED, 0.0);
                        playerLanded(player, false);
                    }

                    cancel();
                    return;
                }

                updateBossBar(player, colorMentionedPlayer + timer[0] + colorBossBar + "s restante" + sss(timer[0]), BarColor.YELLOW, (double) timer[0] / timeLeft);

                Sound sound = (timer[0] <= 3) ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_HAT;
                float pitch = (timer[0] <= 3) ? 2.0f : 1.2f;
                player.playSound(player.getLocation(), sound, 1f, pitch);

                timer[0]--;
            }
        };
        turnTimer.runTaskTimer(plugin, 0L, 20L);
    }


    public void playerLanded(Player player, boolean inWater) {
        UUID uuid = player.getUniqueId();
        if (!rotation.contains(uuid) || !isRunning) return;

        Location loc = player.getLocation().clone();
        cancelTurnTimer();

        if (!activeTurn.remove(uuid)) return; // Protection anti double-trigger

        updateBossBar(player, colorBossBar + "A sauté...", BarColor.YELLOW, 0.0);

        PlayerData data = players.get(uuid);

        if (data != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    handleLanding(player, data, loc, inWater);
                    player.setGameMode(GameMode.SPECTATOR);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            bossBar.removeAll();
                            player.removePotionEffect(PotionEffectType.GLOWING);
                            updateVisualsInfos();
                            currentIndex = (currentIndex + 1) % rotation.size();
                            nextTurn();

                        }
                    }.runTaskLater(plugin, 40L);
                }
            }.runTaskLater(plugin, 1L);
        }
    }

    public void onPlayerJump(Player player) {
        UUID uuid = player.getUniqueId();

        if (!isRunning || !rotation.contains(uuid) || !activeTurn.contains(uuid)) return;

        cancelTurnTimer();

        PlayerData data = players.get(uuid);
        if (data != null) {
            data.incrementJumpsThisTurn();
        }

        if (bossBar != null) {
            updateBossBar(player, colorBossBar + "A sauté...", BarColor.YELLOW, 0.0);
        }
    }

    private void handleLanding(Player player, PlayerData data, Location loc, boolean inWater) {
        UUID uuid = player.getUniqueId();
        List<String> blocks = plugin.getConfig().getStringList("blocks");

        data.resetMissedTurns();

        if (inWater) {
            Location blockLoc = loc.getBlock().getLocation();
            blockLoc.getBlock().setType(Material.AIR);
            int index = rotation.indexOf(uuid);

            if (index >= 0 && index < blocks.size()) {
                if (data.hasPendingSuperJump()) {
                    applySuperJump(loc, player);
                } else {
                    Material mat = Material.valueOf(blocks.get(index % blocks.size()));
                    loc.getBlock().setType(mat);
                    placedBlocks.add(loc.clone());
                }
            }

            boolean perfect_drops = plugin.getConfig().getBoolean("perfect_drops", true);
            boolean give_life_after_perfect_drop = plugin.getConfig().getBoolean("give_life_after_perfect_drop", true);
            int max_perfect_drop = plugin.getConfig().getInt("max_perfect_drop", 5);

            if (perfect_drops &&
                    isPerfectDrop(loc) &&
                    (max_perfect_drop == -1 || PerfectDropsInThisRotation < max_perfect_drop) &&
                    !data.hasPendingSuperJump()) {
                PerfectDropsInThisRotation++;
                data.incrementPerfectDropsThisTurn();

                if (give_life_after_perfect_drop) data.addLife();

                String addLife = give_life_after_perfect_drop ? colorFailed + " +1" + format_lives : "";
                String perfectDropsCount = max_perfect_drop != -1 ? colorMentionedPlayer + " (" + PerfectDropsInThisRotation + "/" + max_perfect_drop + ")" : "";
                String notifyMaxPerfectDropsReach =
                        (max_perfect_drop != -1 && PerfectDropsInThisRotation >= max_perfect_drop)
                                ? "\n§7§oLe maximum de §6§oPerfect Jump §7§oa été atteint, GG !"
                                : "";

                MessageUtil.broadcast(prefix_perfectDrops + colorMentionedPlayer + player.getName() + " " + colorMessage + "a réalisé un " + StrPerfectDrop + " " + colorMessage + "!" + perfectDropsCount + addLife + notifyMaxPerfectDropsReach);

                Animations.startPerfectDrop(blockLoc.clone().add(0, 0.8, 0), Particle.FLAME);
                Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f));

                updateBossBar(player, StrPerfectDrop + " " + colorBossBar + "!", BarColor.GREEN, 0.0);

            } else {
                handleSuccessfulJump(player, data, blockLoc);
            }
        } else {
            handleFailedJump(player, data);
        }
    }

    private boolean isPerfectDrop(Location loc) {
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : directions) {
            Material adjacent = loc.clone().add(dir[0], 0, dir[1]).getBlock().getType();
            if (adjacent == Material.WATER || adjacent == Material.AIR) return false;
        }
        return true;
    }

    private void handleSuccessfulJump(Player player, PlayerData data, Location loc) {
        if (data.hasPendingSuperJump()) {
            data.setPendingSuperJump(false);
            Animations.startSuperJump(loc.clone().add(0, 0.8, 0), Particle.FLAME);
            MessageUtil.broadcast(prefix_jumps + colorMentionedPlayer + player.getName() + " " + colorMessage + "a réussi son " + StrSuperJump + " " + colorMessage + "!");
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.3f));
            updateBossBar(player, StrSuperJump + " " + colorBossBar + "!", BarColor.GREEN, 0.0);
        } else {
            MessageUtil.broadcast(prefix_jumps + colorMentionedPlayer + player.getName() + " " + colorSuccess + "a réussi son saut !");
            spawnParticles(loc, Particle.HAPPY_VILLAGER);
            Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f));
            updateBossBar(player, colorSuccess + "Réussi " + colorBossBar + "!", BarColor.GREEN, 0.0);
        }
    }

    private void handleFailedJump(Player player, PlayerData data) {
        UUID playerUUID = player.getUniqueId();

        if (!disconnectedPlayers.contains(playerUUID)) {
            if (data.hasPendingSuperJump()) {
                data.setPendingSuperJump(false);
                data.loseLife();
                data.loseLife();
            } else {
                data.loseLife();
            }

            if (data.isEliminated()) {
                MessageUtil.broadcast(prefix_lives + colorMentionedPlayer + player.getName() + colorError + " est éliminé !");
            } else {
                MessageUtil.broadcast(prefix_lives + colorMentionedPlayer + player.getName() + colorFailed + " a échoué ! " + data.getLives() + format_lives + " restante" + sss(data.getLives()));
            }

            spawnParticles(player.getLocation(), Particle.SMOKE);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 1f, 0.5f);
            updateBossBar(player, colorFailed + "Échoué " + colorBossBar + "!", BarColor.RED, 0.0);
        } else {
            updateBossBar(player, colorFailed + "Pending action...", BarColor.RED, 0.0);
        }
    }


    private void spawnParticles(Location loc, Particle particle) {
        loc.getWorld().spawnParticle(particle, loc.clone().add(0.5, 1, 0.5), 30, 0.3, 0.5, 0.3, 0.05);
    }

    private void checkWinCondition() {
        if (!isRunning) return;

        long alives = players.values().stream().filter(p -> !p.isEliminated()).count();
        if (alives > minimumPlayersToWin) return;

        isRunning = false;
        MessageUtil.broadcast(prefix_jumps + colorMessage + "Fin du jeu !");

        // 🟢 Chercher le gagnant non éliminé
        Optional<UUID> winnerUUIDOpt = players.entrySet().stream()
                .filter(e -> !e.getValue().isEliminated())
                .map(Map.Entry::getKey)
                .findFirst();

        if (winnerUUIDOpt.isPresent()) {
            UUID winnerUUID = winnerUUIDOpt.get();
            OfflinePlayer winner = Bukkit.getOfflinePlayer(winnerUUID);

            if (winner.isOnline()) {
                MessageUtil.broadcast(prefix_jumps + colorMessage + "Gagnant : " + colorMentionedPlayer + winner.getName());
            } else {
                Optional<UUID> fallbackUUID = players.entrySet().stream()
                        .filter(e -> e.getValue().isEliminated())
                        .map(Map.Entry::getKey)
                        .filter(uuid -> Bukkit.getPlayer(uuid) != null)
                        .findFirst();

                if (fallbackUUID.isPresent()) {
                    Player fallbackPlayer = Bukkit.getPlayer(fallbackUUID.get());
                    MessageUtil.broadcast(prefix_jumps + colorMessage + "Gagnant : " + colorSuccess + fallbackPlayer.getName());
                    MessageUtil.broadcast(prefix_jumps + colorFailed + winner.getName() + " était le dernier survivant, mais comme il est déco... "
                            + "§l" + colorMentionedPlayer + fallbackPlayer.getName() + colorFailed + " gagne à sa place !!!");
                } else {
                    MessageUtil.broadcast(prefix_jumps + colorMessage + "Gagnant : §5Personne :) (Tout le monde est déco !)");
                }
            }
        } else {
            // Aucun survivant valide
            MessageUtil.broadcast(prefix_jumps + colorMessage + "Gagnant : §5Personne :) (Bug ou tous éliminés)");
        }

        stopGame(true);
    }


    private void resetPlacedBlocks() {
        placedBlocks.forEach(loc -> loc.getBlock().setType(Material.WATER));
        placedBlocks.clear();
    }

    public enum SkipResult {
        SKIPPED_CURRENT,
        MOVED_IN_ROTATION,
        NOT_IN_ROTATION,
        NO_PLAYER_IN_TURN
    }

    public SkipResult skipPlayer(Player target) {
        if (!isRunning) return SkipResult.NO_PLAYER_IN_TURN;

        // ➤ Aucun joueur donné → skip du joueur en cours
        if (target == null) {
            Player current = getCurrentPlayerInTurn();
            if (current == null) return SkipResult.NO_PLAYER_IN_TURN;

            cancelTurnTimer();
            resetPlayerState(current);
            blockedPlayers.remove(current.getUniqueId());

            LevelData level = loadLevel(getCurrentLevel());
            if (level != null && level.getJumpPoint() != null) {
                current.teleport(level.getJumpPoint());
            }

            currentIndex = (currentIndex + 1) % rotation.size();
            nextTurn();
            return SkipResult.SKIPPED_CURRENT;
        }

        UUID uuid = target.getUniqueId();

        if (!rotation.contains(uuid)) {
            return SkipResult.NOT_IN_ROTATION;
        }

        if (activeTurn.contains(uuid)) {
            cancelTurnTimer();
            resetPlayerState(target);
            blockedPlayers.remove(uuid);

            LevelData level = loadLevel(getCurrentLevel());
            if (level != null && level.getJumpPoint() != null) {
                target.teleport(level.getJumpPoint());
            }

            currentIndex = (currentIndex + 1) % rotation.size();
            nextTurn();
            return SkipResult.SKIPPED_CURRENT;
        }

        rotation.remove(uuid);
        rotation.add(uuid);
        return SkipResult.MOVED_IN_ROTATION;
    }

    public enum ForceTurnResult {
        SUCCESS,
        NOT_IN_ROTATION,
        ALREADY_TURN,
        NO_GAME_RUNNING
    }

    public ForceTurnResult forceTurn(Player player) {
        if (!isRunning || player == null) return ForceTurnResult.NO_GAME_RUNNING;

        UUID targetUUID = player.getUniqueId();

        if (!rotation.contains(targetUUID)) return ForceTurnResult.NOT_IN_ROTATION;
        if (activeTurn.contains(targetUUID)) return ForceTurnResult.ALREADY_TURN;

        // Annulation du tour en cours proprement
        if (!activeTurn.isEmpty()) {
            UUID currentUUID = activeTurn.iterator().next();
            Player currentPlayer = Bukkit.getPlayer(currentUUID);

            cancelTurnTimer();
            blockedPlayers.remove(currentUUID);
            activeTurn.remove(currentUUID);

            if (currentPlayer != null) {
                resetPlayerState(currentPlayer);
                currentPlayer.setGameMode(GameMode.SPECTATOR);
                LevelData level = loadLevel(getCurrentLevel());
                if (level != null && level.getJumpPoint() != null) {
                    currentPlayer.teleport(level.getJumpPoint());
                }
            }
        }

        // Préparation du nouveau tour
        currentIndex = rotation.indexOf(targetUUID);
        forcedPlayer = targetUUID;

        nextTurn(); // Démarre le tour forcé
        return ForceTurnResult.SUCCESS;
    }


    private void applySuperJump(Location center, Player player) {
        World world = center.getWorld();
        List<String> blocks = plugin.getConfig().getStringList("blocks");
        Material mat = Material.valueOf(blocks.get(rotation.indexOf(player.getUniqueId()) % blocks.size()));

        // Centre
        center.getBlock().setType(Material.AIR);
        center.getBlock().setType(mat);
        placedBlocks.add(center.clone());

        // Diagonales
        int[][] diagonals = {{1, 1}, {-1, 1}, {1, -1}, {-1, -1}};
        for (int[] d : diagonals) {
            Location diag = center.clone().add(d[0], 0, d[1]);
            if (diag.getBlock().getType() == Material.WATER) {
                diag.getBlock().setType(mat);
                placedBlocks.add(diag.clone());
            }
        }
    }
}
