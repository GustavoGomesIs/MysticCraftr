package br.com.novaera.mysticcraft.integrations;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

public final class ItemsAdderCompat {
    private ItemsAdderCompat(){}

    public static boolean isPresent(){
        return Bukkit.getPluginManager().getPlugin("ItemsAdder") != null;
    }

    // ==== já existia (para itens) ====
    public static String idOf(org.bukkit.inventory.ItemStack stack){
        if (!isPresent() || stack == null) return null;
        try {
            dev.lone.itemsadder.api.CustomStack cs = dev.lone.itemsadder.api.CustomStack.byItemStack(stack);
            return (cs != null) ? cs.getNamespacedID() : null;
        } catch (NoClassDefFoundError err){
            return null;
        }
    }

    // ==== NOVO: para blocos IA ====
    public static String customBlockIdOf(Block b){
        if (!isPresent() || b == null) return null;
        try {
            dev.lone.itemsadder.api.CustomBlock cb = dev.lone.itemsadder.api.CustomBlock.byAlreadyPlaced(b);
            return (cb != null) ? cb.getNamespacedID() : null; // "namespace:id"
        } catch (NoClassDefFoundError err){
            return null;
        }
    }

    public static boolean isCustomBlock(Block b, String nsId){
        String id = customBlockIdOf(b);
        return id != null && id.equalsIgnoreCase(nsId);
    }
}
