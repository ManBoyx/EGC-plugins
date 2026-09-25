package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.common.time.DurationParser;
import fr.minebed.hub.common.util.Cooldowns;
import fr.minebed.hub.nms.Reflect;
import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.TeleportService;
import fr.minebed.hub.ultimate.logic.RandomSpot;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rtp : téléport aléatoire vers un endroit sûr. Le chunk visé est chargé sans bloquer le serveur quand c'est possible
 * (Paper 1.13+ et Folia : {@code getChunkAtAsync}, par réflexion), et le contrôle du sol se fait dans la région du lieu (Folia).
 * « /rtp JOUEUR » (permission « .others », console comprise) envoie quelqu'un ailleurs sans attente ni temps de recharge.
 */
public final class RtpModule extends AbstractModule {

    private static final Method CHUNK_ASYNC = Reflect.findMethod(World.class, "getChunkAtAsync", int.class, int.class);
    private static final Method MIN_HEIGHT = Reflect.findMethod(World.class, "getMinHeight");

    private final TeleportService teleports;
    private final Cooldowns cooldowns = new Cooldowns();
    private final Set<UUID> busy = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Random random = new Random();

    public RtpModule(Ctx ctx, TeleportService teleports) {
        super(ctx);
        this.teleports = teleports;
    }

    @Override
    public String id() {
        return "rtp";
    }

    @Override
    public void enable() {
        command("rtp", new BaseCommand(ctx, "ultimatecore.rtp") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length > 1) {
                    return false;
                }
                if (args.length == 1) {
                    if (!sender.hasPermission("ultimatecore.rtp.others")) {
                        ctx.messages.send(sender, Msg.NO_PERMISSION);
                        return true;
                    }
                    final Player target = requireOnline(sender, args[0]);
                    if (target != null) {
                        ctx.runOnPlayer(target, new Runnable() {
                            @Override
                            public void run() {
                                start(target, false);
                            }
                        });
                        ctx.messages.send(sender, Msg.UTIL_DONE_FOR, "player", target.getName());
                    }
                    return true;
                }
                Player me = requirePlayer(sender);
                if (me != null) {
                    start(me, true);
                }
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 && sender.hasPermission("ultimatecore.rtp.others") ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
    }

    /** Doit être appelée depuis le fil du joueur (sa commande, ou {@code runOnPlayer}). */
    private void start(final Player player, final boolean applyRules) {
        final UUID id = player.getUniqueId();
        final boolean bypass = !applyRules || player.hasPermission("ultimatecore.rtp.bypass");
        if (!bypass) {
            long left = cooldowns.remaining(id);
            if (left > 0) {
                ctx.messages.send(player, Msg.RTP_COOLDOWN, "time", DurationParser.format(left, "j", "h", "min", "s"));
                return;
            }
        }
        final World world = pickWorld(player);
        if (world == null) {
            ctx.messages.send(player, Msg.RTP_WORLD_DISABLED);
            return;
        }
        if (!busy.add(id)) {
            ctx.messages.send(player, Msg.RTP_BUSY);
            return;
        }
        ctx.messages.send(player, Msg.RTP_SEARCHING);
        int min = Math.max(0, ctx.config().getInt("rtp.min-radius", 200));
        int max = Math.max(min + 1, ctx.config().getInt("rtp.max-radius", 3000));
        int attempts = Math.max(1, Math.min(30, ctx.config().getInt("rtp.attempts", 12)));
        int centerX = 0;
        int centerZ = 0;
        if (!"zero".equalsIgnoreCase(ctx.config().getString("rtp.center", "spawn"))) {
            Location spawn = world.getSpawnLocation();
            centerX = spawn.getBlockX();
            centerZ = spawn.getBlockZ();
        }
        search(world, centerX, centerZ, min, max, attempts, new Consumer<Location>() {
            @Override
            public void accept(final Location spot) {
                busy.remove(id);
                if (!player.isOnline()) {
                    return;
                }
                ctx.runOnPlayer(player, new Runnable() {
                    @Override
                    public void run() {
                        if (spot == null) {
                            ctx.messages.send(player, Msg.RTP_FAILED);
                            return;
                        }
                        teleports.teleportThen(player, spot, applyRules, new Runnable() {
                            @Override
                            public void run() {
                                long seconds = ctx.config().getLong("rtp.cooldown-seconds", 300);
                                if (!bypass && seconds > 0) {
                                    cooldowns.start(id, seconds * 1000L);
                                }
                            }
                        }, Msg.RTP_TELEPORTED, "x", String.valueOf(spot.getBlockX()), "z", String.valueOf(spot.getBlockZ()));
                    }
                });
            }
        });
    }

    /** Le monde visé : ceux de {@code rtp.worlds} s'il y en a, sinon celui du joueur ; toujours un monde « normal » (ni Nether, ni End). */
    private World pickWorld(Player player) {
        List<String> allowed = ctx.config().getStringList("rtp.worlds");
        World here = player.getWorld();
        if (allowed.isEmpty()) {
            if (isOverworld(here)) {
                return here;
            }
            for (World w : Bukkit.getWorlds()) {
                if (isOverworld(w)) {
                    return w;
                }
            }
            return null;
        }
        for (String name : allowed) {
            if (name.equalsIgnoreCase(here.getName()) && isOverworld(here)) {
                return here;
            }
        }
        for (String name : allowed) {
            World w = Bukkit.getWorld(name);
            if (w != null && isOverworld(w)) {
                return w;
            }
        }
        return null;
    }

    private static boolean isOverworld(World world) {
        return world.getEnvironment() == World.Environment.NORMAL;
    }

    /** Essaie jusqu'à {@code left} points au hasard ; {@code done} reçoit un endroit sûr ou {@code null}. */
    private void search(final World world, final int cx, final int cz, final int min, final int max, final int left, final Consumer<Location> done) {
        if (left <= 0) {
            done.accept(null);
            return;
        }
        int[] xz = RandomSpot.pick(random, cx, cz, min, max);
        final int x = xz[0];
        final int z = xz[1];
        final Location probe = new Location(world, x + 0.5, 64, z + 0.5);
        final Runnable inspect = new Runnable() {
            @Override
            public void run() {
                Location spot = null;
                try {
                    spot = safeSpot(world, x, z);
                } catch (RuntimeException e) {
                    ctx.plugin.getLogger().fine("rtp : point ignoré (" + e + ")");
                }
                if (spot != null) {
                    done.accept(spot);
                } else {
                    search(world, cx, cz, min, max, left - 1, done);
                }
            }
        };
        if (CHUNK_ASYNC != null) {
            try {
                Object future = CHUNK_ASYNC.invoke(world, x >> 4, z >> 4);
                if (future instanceof CompletableFuture) {
                    ((CompletableFuture<?>) future).whenComplete((chunk, error) -> {
                        if (error != null) {
                            search(world, cx, cz, min, max, left - 1, done);
                        } else {
                            ctx.scheduler.runAtLocation(probe, inspect);
                        }
                    });
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                // repli : chargement classique ci-dessous
            }
        }
        ctx.scheduler.runAtLocation(probe, inspect);
    }

    /**
     * Le premier sol franchissable près de la surface en ({@code x}, {@code z}), ou {@code null}. {@code getHighestBlockYAt} ne
     * renvoie pas la même chose avant et après la 1.13 (bloc libre au-dessus, ou dernier bloc plein) : on part donc d'un cran au-dessus
     * et on descend jusqu'à trouver un sol plein avec deux blocs libres au-dessus.
     */
    private Location safeSpot(World world, int x, int z) {
        org.bukkit.WorldBorder border = world.getWorldBorder();
        Location bc = border.getCenter();
        if (!RandomSpot.insideBorder(x + 0.5, z + 0.5, bc.getX(), bc.getZ(), border.getSize(), 8)) {
            return null;
        }
        int top = world.getHighestBlockYAt(x, z);
        int floor = 0;
        Object min = Reflect.invoke(MIN_HEIGHT, world);
        if (min instanceof Integer) {
            floor = (Integer) min;
        }
        int maxY = world.getMaxHeight() - 3;
        for (int y = Math.min(top + 1, maxY); y >= Math.max(top - 6, floor); y--) {
            Block ground = world.getBlockAt(x, y, z);
            Material type = ground.getType();
            if (!type.isSolid()) {
                continue;
            }
            if (ground.isLiquid() || RandomSpot.isUnsafeGroundName(type.name())) {
                return null;
            }
            Block feet = world.getBlockAt(x, y + 1, z);
            Block head = world.getBlockAt(x, y + 2, z);
            if (feet.isEmpty() && head.isEmpty()) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return null;
    }
}
