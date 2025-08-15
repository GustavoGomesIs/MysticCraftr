package br.com.novaera.mysticcraft.util;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public final class PdcBlocks {
    private static JavaPlugin plugin;

    public static void init(JavaPlugin pl) { plugin = pl; }

    public static void setString(Block block, NamespacedKey key, String val) {
        String k = key.getKey();
        if (val == null) block.removeMetadata(k, plugin);
        else block.setMetadata(k, new FixedMetadataValue(plugin, val));
    }

    public static boolean has(Block block, NamespacedKey key) {
        return block.hasMetadata(key.getKey());
    }

    private PdcBlocks() {}
}
