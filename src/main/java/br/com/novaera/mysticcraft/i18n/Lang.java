package br.com.novaera.mysticcraft.i18n;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Lang {
    private static JavaPlugin plugin;
    private static File file;
    private static YamlConfiguration yml;
    private static String prefix = "";

    private Lang(){}

    public static void init(JavaPlugin p){
        plugin = p;
        file = new File(p.getDataFolder(), "lang.yml");
        if (!file.exists()) {
            p.saveResource("lang.yml", false);
        }
        reload();
    }

    public static void reload(){
        yml = YamlConfiguration.loadConfiguration(file);
        prefix = color(yml.getString("prefix", ""));
    }

    public static String t(String key){
        String raw = yml.getString("messages."+key, "&7["+key+"]");
        return prefix + color(raw);
    }

    public static String color(String s){
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public static String raw(String key){
        return color(yml.getString("messages."+key, "&7["+key+"]"));
    }
}
