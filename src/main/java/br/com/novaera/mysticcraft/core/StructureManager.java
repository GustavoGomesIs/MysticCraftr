package br.com.novaera.mysticcraft.core;

import br.com.novaera.mysticcraft.config.StructureConfig;
import br.com.novaera.mysticcraft.integrations.ItemsAdderCompat;
import br.com.novaera.mysticcraft.util.Keys;
import br.com.novaera.mysticcraft.util.PdcBlocks;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import java.util.List;

public final class StructureManager {
    // cfg mutável para permitir /mystic reload
    private StructureConfig cfg;
    private List<Vector> pillarOffsets = Collections.emptyList();

    public StructureManager(StructureConfig cfg){ this.cfg = cfg; }

    public void updateConfig(StructureConfig newCfg){
        this.cfg = newCfg;
    }

    // ---------- marcações ----------
    public void markCore(Block core){ PdcBlocks.setString(core, Keys.CORE, "1"); }
    public void unmarkCore(Block core){ PdcBlocks.setString(core, Keys.CORE, null); }

    public void markAltar(Block altar){ PdcBlocks.setString(altar, Keys.ALTAR, "1"); }
    public void unmarkAltar(Block altar){ PdcBlocks.setString(altar, Keys.ALTAR, null); }

    // ---------- detecção por IA OU Material ----------
    public boolean matchesCoreBlock(Block b){
        if (cfg.coreCustomBlockId != null && !cfg.coreCustomBlockId.isBlank() && ItemsAdderCompat.isEnabled()) {
            if (ItemsAdderCompat.matchesBlockOrFurniture(b, cfg.coreCustomBlockId)) return true;
            return false; // se configurou IA, fica estrito
        }
        return b.getType() == cfg.coreMat;
    }

    public boolean matchesAltarBlock(Block b){
        if (cfg.altarCustomBlockId != null && !cfg.altarCustomBlockId.isBlank() && ItemsAdderCompat.isEnabled()) {
            if (ItemsAdderCompat.matchesBlockOrFurniture(b, cfg.altarCustomBlockId)) return true;
            return false;
        }
        return b.getType() == cfg.altarMat;
    }

    // Estes são os ÚNICOS isCore/isAltar da classe
    public boolean isCore(Block b){
        return matchesCoreBlock(b) && PdcBlocks.has(b, Keys.CORE);
    }

    public boolean isAltar(Block b){
        return matchesAltarBlock(b) && PdcBlocks.has(b, Keys.ALTAR);
    }

    // ---------- stands ----------
    public ArmorStand findStandNear(Location base){
        double r = 0.8;
        for (Entity e : base.getWorld().getNearbyEntities(base, r, r, r)){
            if (e instanceof ArmorStand as && as.getPersistentDataContainer().has(Keys.ALTAR_STAND)){
                return as;
            }
        }
        return null;
    }

    public ArmorStand spawnAltarStand(Location loc){
        return loc.getWorld().spawn(loc, ArmorStand.class, s -> {
            s.setInvisible(true);
            s.setGravity(false);
            s.setInvulnerable(true);

            s.setSmall(cfg.standSmall);
            s.setMarker(cfg.standMarker);

            s.setCustomNameVisible(false);
            s.setSilent(true);

            s.getPersistentDataContainer().set(
                    Keys.ALTAR_STAND,
                    org.bukkit.persistence.PersistentDataType.STRING,
                    "1"
            );
        });
    }

    public void dropHelmet(ArmorStand as){
        ItemStack helm = as.getEquipment().getHelmet();
        if (helm != null && helm.getType() != Material.AIR){
            as.getWorld().dropItemNaturally(as.getLocation(), helm);
            as.getEquipment().setHelmet(null);
        }
    }

    /** Retorna true se TODOS os pilares esperados estão presentes (IA ou vanilla). */
    public boolean hasAllPillars(Block coreBlock) {
        List<Vector> pillarOffsets = cfg.getPillarOffsets(); // deve ser populado a partir da grade 7x7
        if (pillarOffsets == null || pillarOffsets.isEmpty()) {
            // Se não há pilares definidos, consideramos "intacto" para não bloquear interação por engano
            return true;
        }
        for (Vector off : pillarOffsets) {
            Block b = coreBlock.getRelative(off.getBlockX(), off.getBlockY(), off.getBlockZ());
            if (!matchesAltarBlock(b)) {
                return false;
            }
        }
        return true;
    }

    public List<Vector> getPillarOffsets() {
        return pillarOffsets; // será preenchido no load/construtor
    }

    public StructureConfig cfg(){ return cfg; }
}
