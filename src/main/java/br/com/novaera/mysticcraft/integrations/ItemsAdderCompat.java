package br.com.novaera.mysticcraft.integrations;

import dev.lone.itemsadder.api.ItemsAdder;
import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public final class ItemsAdderCompat {
    private static final boolean ENABLED = Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");

    private ItemsAdderCompat() {}

    public static boolean isEnabled() { return ENABLED; }

    /** Confere se o bloco do mundo é um CustomBlock do IA com o ID informado (ex.: "mypack:altar_pillar"). */
    public static boolean matchesCustomBlock(Block block, @Nullable String namespacedId) {
        if (!ENABLED || block == null || namespacedId == null || namespacedId.isBlank()) return false;
        CustomBlock cb = CustomBlock.byAlreadyPlaced(block); // IA v4
        return cb != null && namespacedId.equalsIgnoreCase(cb.getNamespacedID());
    }

    /** Diz apenas se o bloco é algum CustomBlock IA (independente de ID). */
    public static boolean isAnyCustomBlock(Block block) {
        return ENABLED && block != null && CustomBlock.byAlreadyPlaced(block) != null;
    }

    /** Retorna o ID IA do item na mão (ex.: "namespace:id"), ou null se não for IA. */
    public static @Nullable String idOf(@Nullable ItemStack stack) {
        if (!ENABLED || stack == null) return null;
        CustomStack cs = CustomStack.byItemStack(stack);
        return cs != null ? cs.getNamespacedID().toLowerCase(java.util.Locale.ROOT) : null;
    }

    /** Procura uma furniture IA com o ID dado nas proximidades do bloco. */
    public static boolean matchesCustomFurnitureNear(Block center, @Nullable String namespacedId) {
        if (!ENABLED || center == null || namespacedId == null || namespacedId.isBlank()) return false;
        var w = center.getWorld(); if (w == null) return false;

        double r = 0.9;
        BoundingBox box = BoundingBox.of(center).expand(r);

        for (Entity e : w.getNearbyEntities(box)) {
            try {
                if (!ItemsAdder.isFurniture(e)) continue; // evita passar PLAYER e afins
                CustomFurniture f = CustomFurniture.byAlreadySpawned(e);
                if (f != null && namespacedId.equalsIgnoreCase(f.getNamespacedID())) return true;
            } catch (Throwable ignored) {
                // algumas builds do IA podem lançar exceção em entidades não suportadas; ignore
            }
        }
        return false;
    }

    /** Tenta casar como CustomBlock OU como Furniture IA. */
    public static boolean matchesBlockOrFurniture(Block block, @Nullable String namespacedId) {
        if (!ENABLED || block == null || namespacedId == null || namespacedId.isBlank()) return false;
        if (matchesCustomBlock(block, namespacedId)) return true;
        return matchesCustomFurnitureNear(block, namespacedId);
    }
}
