package me.purplertp.plugin.commands;

import java.util.List;
import me.purplertp.plugin.PurpleRTP;
import me.purplertp.plugin.gui.RTPMenu;
import me.purplertp.plugin.gui.RTPMenuListener;
import me.purplertp.plugin.utils.MessageUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RTPCommand
implements CommandExecutor {
    private final PurpleRTP plugin;
    private final RTPMenu menu;
    private final RTPMenuListener listener;

    public RTPCommand(PurpleRTP plugin, RTPMenuListener listener) {
        this.plugin = plugin;
        this.menu = new RTPMenu(plugin);
        this.listener = listener;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("havocrtp.use")) {
            player.sendMessage(MessageUtils.format("&cYou don't have permission to use RTP."));
            return true;
        }
        if (!this.plugin.getConfig().getBoolean("ENABLED", true)) {
            String msg = this.plugin.getConfig().getString("MESSAGES.DISABLED", "&cRTP is disabled.");
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, (BaseComponent)new TextComponent(MessageUtils.format(msg)));
            return true;
        }
        List denied = this.plugin.getConfig().getStringList("DENIED-WORLDS");
        if (player.getWorld() != null && denied.contains(player.getWorld().getName())) {
            player.sendMessage(MessageUtils.format("&cRTP is not allowed in this world."));
            return true;
        }
        this.menu.open(player, this.listener);
        return true;
    }
}

