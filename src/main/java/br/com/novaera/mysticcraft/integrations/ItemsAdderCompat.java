package br.com.novaera.mysticcraft.integrations;

import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomFurniture;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public final class ItemsAdderCompat {
    private static final boolean ENABLED = Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");

    private ItemsAdderCompat() {}

    public static boolean isEnabled() { return ENABLED; }

    /* ===================== ITEMSTACK (mão) ===================== */

    /** ID IA do item na mão (ex.: "namespace:id"), ou null se não for IA. */
    public static @Nullable String idOf(@Nullable ItemStack stack) {
        if (!ENABLED || stack == null) return null;
        try {
            CustomStack cs = CustomStack.byItemStack(stack);
            return cs != null ? lower(cs.getNamespacedID()) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /* ===================== BLOCO/FURNITURE NO MUNDO ===================== */

    /** Nome IA do que estiver “naquela posição”: tenta CustomBlock e depois Furniture por perto. */
    public static @Nullable String idOf(Block block) {
        if (!ENABLED || block == null) return null;

        // 1) CustomBlock IA diretamente no voxel
        try {
            CustomBlock cb = CustomBlock.byAlreadyPlaced(block);
            if (cb != null) return lower(cb.getNamespacedID());
        } catch (Throwable ignored) {}

        // 2) Furniture próxima — checa centro do bloco, vizinhos e bloco abaixo (alguns pivôs ancoram assim)
        Location c = block.getLocation().add(0.5, 0.0, 0.5);
        String id =
                furnitureIdNear(c, 1.6) != null ? furnitureIdNear(c, 1.6) :
                        furnitureIdNear(c.clone().add( 1, 0,  0), 1.2) != null ? furnitureIdNear(c.clone().add( 1, 0,  0), 1.2) :
                                furnitureIdNear(c.clone().add(-1, 0,  0), 1.2) != null ? furnitureIdNear(c.clone().add(-1, 0,  0), 1.2) :
                                        furnitureIdNear(c.clone().add( 0, 0,  1), 1.2) != null ? furnitureIdNear(c.clone().add( 0, 0,  1), 1.2) :
                                                furnitureIdNear(c.clone().add( 0,-0.5,0), 1.2); // levemente abaixo
        return id;
    }

    /** True se houver CustomBlock OU Furniture IA com o namespacedId naquela posição. */
    public static boolean matchesBlockOrFurniture(Block block, @Nullable String namespacedId) {
        if (!ENABLED || block == null || namespacedId == null || namespacedId.isBlank()) return false;
        String got = idOf(block);
        return got != null && got.equalsIgnoreCase(namespacedId);
    }

    /** Apenas verifica se existe ALGUM CustomBlock OU Furniture IA no local. */
    public static boolean isAnyIaAt(Block block) {
        if (!ENABLED || block == null) return false;
        if (idOf(block) != null) return true;
        return false;
    }

    /* ===================== Helpers ===================== */

    @Nullable
    private static String furnitureIdNear(Location center, double r) {
        if (center == null || center.getWorld() == null) return null;

        BoundingBox box = BoundingBox.of(center, r, r, r);
        for (Entity e : center.getWorld().getNearbyEntities(box)) {
            // IA costuma usar ArmorStand/ItemFrame/Interaction/*Display para furniture
            if (!(e instanceof ArmorStand || e instanceof ItemFrame || e instanceof Interaction || isDisplayLike(e))) continue;
            try {
                if (!ItemsAdder.isFurniture(e)) continue; // evita PLAYER etc.
                CustomFurniture cf = CustomFurniture.byAlreadySpawned(e);
                if (cf != null) {
                    String id = lower(cf.getNamespacedID());
                    if (id != null) return id;
                }
            } catch (Throwable ignored) {
                // versões diferentes do IA podem lançar erro se entidade não for suportada; ignoramos
            }
        }
        return null;
    }

    private static boolean isDisplayLike(Entity e) {
        // sem depender de classes, usa o nome do tipo (ITEM_DISPLAY, BLOCK_DISPLAY, TEXT_DISPLAY)
        String t = e.getType().name();
        return t.endsWith("DISPLAY");
    }

    private static String lower(String s) {
        return (s == null) ? null : s.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
