package br.com.novaera.mysticcraft.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Keys {
    public static NamespacedKey CORE;        // tag do bloco núcleo S
    public static NamespacedKey ALTAR;       // tag do bloco altar X
    public static NamespacedKey ALTAR_STAND; // tag do ArmorStand do altar

    public static void init(JavaPlugin plugin){
        CORE        = new NamespacedKey(plugin, "core");
        ALTAR       = new NamespacedKey(plugin, "altar");
        ALTAR_STAND = new NamespacedKey(plugin, "altar_stand");
    }

    private Keys(){}
}
