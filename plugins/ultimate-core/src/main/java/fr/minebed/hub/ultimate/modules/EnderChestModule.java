package fr.minebed.hub.ultimate.modules;

import fr.minebed.hub.ultimate.AbstractModule;
import fr.minebed.hub.ultimate.BaseCommand;
import fr.minebed.hub.ultimate.Ctx;
import fr.minebed.hub.ultimate.Msg;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /ec (/enderchest) : ouvre son coffre de l'Ender, ou celui d'un joueur en ligne avec la permission « .others ».
 * L'appel {@code openInventory} ignore sa valeur de retour : il ne touche donc pas à {@code InventoryView}, dont la forme a changé en 1.21.
 * Sur Folia, l'inventaire d'un autre joueur appartient à la région de ce joueur : l'ouvrir depuis la nôtre est dangereux, c'est refusé.
 */
public final class EnderChestModule extends AbstractModule {

    public EnderChestModule(Ctx ctx) {
        super(ctx);
    }

    @Override
    public String id() {
        return "enderchest";
    }

    @Override
    public void enable() {
        command("enderchest", new BaseCommand(ctx, "ultimatecore.ec") {
            @Override
            protected boolean execute(CommandSender sender, String label, String[] args) {
                if (args.length > 1) {
                    return false;
                }
                Player me = requirePlayer(sender);
                if (me == null) {
                    return true;
                }
                if (args.length == 0) {
                    me.openInventory(me.getEnderChest());
                    return true;
                }
                if (!me.hasPermission("ultimatecore.ec.others")) {
                    ctx.messages.send(me, Msg.NO_PERMISSION);
                    return true;
                }
                if (ctx.platform.isFolia()) {
                    ctx.messages.send(me, Msg.EC_OTHERS_UNSUPPORTED);
                    return true;
                }
                Player target = requireOnline(me, args[0]);
                if (target != null) {
                    me.openInventory(target.getEnderChest());
                }
                return true;
            }

            @Override
            protected List<String> complete(CommandSender sender, String[] args) {
                return args.length == 1 && sender.hasPermission("ultimatecore.ec.others") ? onlineNames(args[0]) : Collections.<String>emptyList();
            }
        });
    }
}
