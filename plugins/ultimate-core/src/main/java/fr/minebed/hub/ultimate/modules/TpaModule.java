package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import fr.minebed.hub.ultimate.TeleportService;
import fr.minebed.hub.ultimate.logic.TpaRequests;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/** Demandes de téléportation entre joueurs : /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel. */
public final class TpaModule extends AbstractModule implements Listener {

    private final TeleportService teleports;
    private TpaRequests requests;

    public TpaModule(Ctx ctx, TeleportService teleports) {
        super(ctx);
        this.teleports = teleports;
    }

    @Override
    public String id() {
        return "tpa";
    }

    @Override
    public void enable() {
        long lifetime = Math.max(5, ctx.config().getLong("teleport.request-expire-seconds", 60)) * 1000L;
        requests = new TpaRequests(lifetime, System::currentTimeMillis);
        command("tpa", new Ask(TpaRequests.Direction.REQUESTER_TO_TARGET));
        command("tpahere", new Ask(TpaRequests.Direction.TARGET_TO_REQUESTER));
        command("tpaccept", new BaseCommand(ctx, "ultimatecore.tpa") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player me = requirePlayer(sender);
                return me == null || answer(me, args, true);
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("tpdeny", new BaseCommand(ctx, "ultimatecore.tpa") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player me = requirePlayer(sender);
                return me == null || answer(me, args, false);
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
        command("tpcancel", new BaseCommand(ctx, "ultimatecore.tpa") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                Player me = requirePlayer(sender);
                if (me != null) {
                    ctx.messages.send(me, Msg.TPA_CANCELLED, "count", String.valueOf(requests.cancelFrom(me.getUniqueId())));
                }
                return true;
            }
        });
        listen(this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        requests.forget(event.getPlayer().getUniqueId());
    }

    private final class Ask extends BaseCommand {
        private final TpaRequests.Direction direction;

        Ask(TpaRequests.Direction direction) {
            super(TpaModule.this.ctx, "ultimatecore.tpa");
            this.direction = direction;
        }

        @Override
        protected boolean execute(CommandSender sender, String label, String[] args) {
            Player me = requirePlayer(sender);
            if (me == null) {
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            Player target = requireOnline(me, args[0]);
            if (target == null) {
                return true;
            }
            if (target.getUniqueId().equals(me.getUniqueId())) {
                ctx.messages.send(me, Msg.TPA_SELF);
                return true;
            }
            requests.create(me.getUniqueId(), target.getUniqueId(), direction);
            ctx.messages.send(me, Msg.TPA_SENT, "player", target.getName());
            ctx.messages.send(target, direction == TpaRequests.Direction.REQUESTER_TO_TARGET ? Msg.TPA_RECEIVED : Msg.TPA_RECEIVED_HERE, "player", me.getName());
            return true;
        }

        @Override
        protected List<String> complete(CommandSender sender, String[] args) {
            return args.length == 1 ? onlineNames(args[0]) : Collections.<String>emptyList();
        }
    }

    private boolean answer(final Player me, String[] args, boolean accept) {
        if (args.length > 1) {
            return false;
        }
        java.util.UUID wanted = null;
        if (args.length == 1) {
            Player from = ctx.findOnline(args[0]);
            if (from == null) {
                ctx.messages.send(me, Msg.PLAYER_NOT_FOUND, "name", args[0]);
                return true;
            }
            wanted = from.getUniqueId();
        }
        final TpaRequests.Request request = requests.take(me.getUniqueId(), wanted);
        if (request == null) {
            ctx.messages.send(me, Msg.TPA_NONE);
            return true;
        }
        final Player requester = Bukkit.getPlayer(request.from());
        if (requester == null || !requester.isOnline()) {
            ctx.messages.send(me, Msg.TPA_OFFLINE);
            return true;
        }
        if (!accept) {
            ctx.messages.send(me, Msg.TPA_DENIED, "player", requester.getName());
            ctx.messages.send(requester, Msg.TPA_DENIED_NOTICE, "player", me.getName());
            return true;
        }
        ctx.messages.send(me, Msg.TPA_ACCEPTED, "player", requester.getName());
        ctx.messages.send(requester, Msg.TPA_ACCEPTED_NOTICE, "player", me.getName());
        if (request.direction() == TpaRequests.Direction.REQUESTER_TO_TARGET) {
            final Location destination = me.getLocation();
            ctx.runOnPlayer(requester, new Runnable() {
                @Override
                public void run() {
                    teleports.teleport(requester, destination, true, null);
                }
            });
        } else {
            // « tpahere » : c'est celui qui accepte qui se déplace, vers la position de celui qui a demandé.
            ctx.runOnPlayer(requester, new Runnable() {
                @Override
                public void run() {
                    final Location destination = requester.getLocation();
                    ctx.runOnPlayer(me, new Runnable() {
                        @Override
                        public void run() {
                            teleports.teleport(me, destination, true, null);
                        }
                    });
                }
            });
        }
        return true;
    }
}
