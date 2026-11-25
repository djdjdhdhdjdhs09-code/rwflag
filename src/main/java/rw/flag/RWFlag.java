package rw.flag;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.PatternReplacementResult;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Pattern;

public final class RWFlag extends JavaPlugin implements Listener, TabExecutor {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern TOKEN = Pattern.compile(":([a-z0-9_]+):");
    private final Map<String, Component> emojis = new TreeMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        load();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("rwflag").setExecutor(this);
        getCommand("rwflag").setTabCompleter(this);
        getLogger().info("RWFlag loaded " + emojis.size() + " shortcodes.");
    }

    private void load() {
        reloadConfig();
        emojis.clear();
        ConfigurationSection sec = getConfig().getConfigurationSection("emojis");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String raw = sec.getString(key);
            if (raw == null || raw.isBlank()) continue;
            try {
                emojis.put(key.toLowerCase(Locale.ROOT), MM.deserialize(raw));
            } catch (RuntimeException ex) {
                getLogger().warning("Bad emoji '" + key + "': " + ex.getMessage());
            }
        }
    }

    private Component parse(Component in) {
        return in.replaceText(TextReplacementConfig.builder()
                .match(TOKEN)
                .condition((m, count, replaced) -> emojis.containsKey(m.group(1))
                        ? PatternReplacementResult.REPLACE : PatternReplacementResult.CONTINUE)
                .replacement((m, b) -> emojis.get(m.group(1)))
                .build());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        if (!getConfig().getBoolean("chat", true) || !e.getPlayer().hasPermission("rwflag.use")) return;
        e.message(parse(e.message()));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSign(SignChangeEvent e) {
        if (!getConfig().getBoolean("signs", true) || !e.getPlayer().hasPermission("rwflag.use")) return;
        for (int i = 0; i < 4; i++) {
            Component line = e.line(i);
            if (line != null) e.line(i, parse(line));
        }
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        String sub = a.length == 0 ? "list" : a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            if (!s.hasPermission("rwflag.admin")) {
                s.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                return true;
            }
            load();
            s.sendMessage(Component.text("[RWFlag] Reloaded " + emojis.size() + " shortcodes.", NamedTextColor.GREEN));
            return true;
        }
        s.sendMessage(Component.text("[RWFlag] Type the code in chat or on a sign. Click one to copy:", NamedTextColor.GOLD));
        for (Map.Entry<String, Component> en : emojis.entrySet()) {
            String code = ":" + en.getKey() + ":";
            s.sendMessage(Component.text("  " + code + "  ", NamedTextColor.YELLOW)
                    .append(en.getValue())
                    .clickEvent(ClickEvent.copyToClipboard(code))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to copy " + code, NamedTextColor.GRAY))));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) {
        if (a.length == 1) {
            List<String> r = new ArrayList<>(List.of("list"));
            if (s.hasPermission("rwflag.admin")) r.add("reload");
            r.removeIf(x -> !x.startsWith(a[0].toLowerCase(Locale.ROOT)));
            return r;
        }
        return List.of();
    }
              }
