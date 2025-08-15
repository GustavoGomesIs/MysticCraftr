package br.com.novaera.mysticcraft.core;

import br.com.novaera.mysticcraft.config.StructureConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

public final class StructureService {
    private final StructureManager sm;
    private final StructureStorage storage;

    public java.util.Set<org.bukkit.Location> allCores() {
        return storage.all();
    }

    public StructureService(StructureManager sm, StructureStorage storage) {
        this.sm = sm;
        this.storage = storage;
    }

    // valida se TODOS os altares estão com o material correto em volta do core
    public boolean isIntact(Block core){
        if (!sm.matchesCoreBlock(core)) return false;
        for (int[] off : sm.cfg().altarOffsets){
            Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
            if (!sm.matchesAltarBlock(altar)) return false;
        }
        return true;
    }

    // Monta de fato: marca, spawna stands e salva no storage
    public boolean assemble(Block core, Player pOpt) {
        if (!isIntact(core)) {
            if (pOpt != null) pOpt.sendMessage("§cEstrutura incompleta. Coloque todos os altares.");
            return false;
        }
        // marca
        sm.markCore(core);
        for (int[] off : sm.cfg().altarOffsets) {
            Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
            sm.markAltar(altar);
            Location standLoc = expectedStandLocation(core, altar);
            ArmorStand found = sm.findStandNear(standLoc);
            if (found == null) sm.spawnAltarStand(standLoc);
            else found.teleport(standLoc); // garante alinhamento fino
        }
        // salva
        storage.add(core.getLocation());
        playFx(cfg().fxAssemble, core.getLocation().add(0.5, 1.0, 0.5));
        if (pOpt != null) pOpt.sendMessage("§aMesa MysticCraft montada com sucesso!");
        return true;
    }

    // Desmonta: remove stands/itens, desmarca e remove do storage
    public void disassemble(Block core, Player pOpt) {
        for (int[] off : sm.cfg().altarOffsets) {
            Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
            Location standLoc = expectedStandLocation(core, altar);
            ArmorStand as = sm.findStandNear(standLoc);
            if (as != null) {
                sm.dropHelmet(as);
                as.remove();
            }
            if (sm.isAltar(altar)) sm.unmarkAltar(altar);
        }
        if (sm.isCore(core)) sm.unmarkCore(core);
        storage.remove(core.getLocation());
        playFx(cfg().fxDisassemble, core.getLocation().add(0.5, 1.0, 0.5));
        if (pOpt != null) pOpt.sendMessage("§cMesa MysticCraft desmontada (bloco removido).");
    }

    // Reconstrói todas as mesas salvas no restart
    public void rebuildAllOnEnable() {
        for (org.bukkit.Location loc : storage.all()) {
            org.bukkit.block.Block core = loc.getBlock();

            if (isIntact(core)) {
                // re-marcar núcleo
                sm.markCore(core);

                // garantir/alinhar stands e repor itens salvos
                for (int[] off : sm.cfg().altarOffsets) {
                    org.bukkit.block.Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
                    sm.markAltar(altar);

                    org.bukkit.Location standLoc = expectedStandLocation(core, altar);
                    org.bukkit.entity.ArmorStand as = sm.findStandNear(standLoc);
                    if (as == null) {
                        as = sm.spawnAltarStand(standLoc);
                    } else {
                        as.teleport(standLoc);
                    }

                    // repor item salvo (se houver)
                    org.bukkit.inventory.ItemStack saved = storage.getAltarItem(core.getLocation(), off);
                    if (saved != null && saved.getType() != org.bukkit.Material.AIR) {
                        as.getEquipment().setHelmet(saved.clone());
                    }
                }

            } else {
                // estrutura não existe mais: remove registro (e itens) do storage
                storage.remove(loc); // o remove já limpa os itens desse core
            }
        }
    }

    public StructureConfig cfg() {
        return sm.cfg();
    }

    // ajuda: detectar se "este altar" pertence a algum core salvo (e qual)
    public Block findOwningCoreOfAltar(Block altar) {
        Location base = altar.getLocation();
        for (int[] off : sm.cfg().altarOffsets) {
            Location coreLoc = base.clone().add(-off[0], -off[1], -off[2]);
            Block candidateCore = coreLoc.getBlock();
            if (storage.contains(candidateCore.getLocation())) {
                return candidateCore;
            }
        }
        return null;
    }

    // posição esperada do stand: centro do altar + y_offset + empurrão radial, olhando pro núcleo
    public Location expectedStandLocation(Block core, Block altar) {
        Location center = altar.getLocation().add(0.5, cfg().standYOffset, 0.5);

        double vx = altar.getX() - core.getX();
        double vz = altar.getZ() - core.getZ();
        double len = Math.hypot(vx, vz);
        if (len > 0.0001) {
            vx /= len;
            vz /= len;
            center.add(vx * cfg().standBackOffset, 0, vz * cfg().standBackOffset);

            double dirX = core.getX() + 0.5 - center.getX();
            double dirZ = core.getZ() + 0.5 - center.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(-dirX, dirZ));
            center.setYaw(yaw);
            center.setPitch(0f);
        }
        return center;
    }

    // Reposiciona/garante todos os stands após /mystic reload (sem derrubar itens)
    public void retuneAllStandsAfterConfigChange() {
        for (Location coreLoc : storage.all()) {
            Block core = coreLoc.getBlock();
            if (!isIntact(core)) continue;
            for (int[] off : sm.cfg().altarOffsets) {
                Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
                Location expected = expectedStandLocation(core, altar);
                ArmorStand as = sm.findStandNear(expected);
                if (as == null) sm.spawnAltarStand(expected);
                else as.teleport(expected);
            }
        }
    }

    // Quais offsets de altares estão faltando ao redor deste núcleo
    public java.util.List<int[]> missingAltarsForCore(Block core) {
        java.util.List<int[]> miss = new java.util.ArrayList<>();
        if (core == null || core.getType() != sm.cfg().coreMat) return miss;
        for (int[] off : sm.cfg().altarOffsets) {
            Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
            if (altar.getType() != sm.cfg().altarMat) miss.add(off);
        }
        return miss;
    }

    public int[] offsetBetween(Block core, Block altar) {
        return new int[]{altar.getX() - core.getX(), altar.getY() - core.getY(), altar.getZ() - core.getZ()};
    }

    public void saveAltarItem(Block core, Block altar, org.bukkit.inventory.ItemStack item) {
        storage.saveAltarItem(core.getLocation(), offsetBetween(core, altar), item);
    }

    public org.bukkit.inventory.ItemStack getSavedAltarItem(Block core, Block altar) {
        return storage.getAltarItem(core.getLocation(), offsetBetween(core, altar));
    }

    public void playFx(br.com.novaera.mysticcraft.config.StructureConfig.EffectSpec fx, org.bukkit.Location loc) {
        if (fx == null || !cfg().effectsEnabled || loc == null || loc.getWorld() == null) return;
        var w = loc.getWorld();
        w.spawnParticle(fx.particle, loc.getX(), loc.getY(), loc.getZ(), fx.count, fx.offset, fx.offset, fx.offset, fx.speed);
        w.playSound(loc, fx.sound, fx.volume, fx.pitch);
    }

    public java.util.Set<org.bukkit.Location> coresInChunk(org.bukkit.Chunk c){
        java.util.Set<org.bukkit.Location> out = new java.util.HashSet<>();
        for (org.bukkit.Location loc : storage.all()){
            if (loc.getWorld() == null) continue;
            if (!loc.getWorld().equals(c.getWorld())) continue;
            if ((loc.getBlockX() >> 4) == c.getX() && (loc.getBlockZ() >> 4) == c.getZ()){
                out.add(loc);
            }
        }
        return out;
    }

    public void repairCoreAt(org.bukkit.Location coreLoc){
        org.bukkit.block.Block core = coreLoc.getBlock();
        if (!isIntact(core)) return;
        sm.markCore(core);
        for (int[] off : sm.cfg().altarOffsets){
            org.bukkit.block.Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
            sm.markAltar(altar);
            org.bukkit.Location standLoc = expectedStandLocation(core, altar);
            org.bukkit.entity.ArmorStand as = sm.findStandNear(standLoc);
            if (as == null) as = sm.spawnAltarStand(standLoc);
            else as.teleport(standLoc);
            var saved = storage.getAltarItem(core.getLocation(), off);
            if (saved != null && saved.getType() != org.bukkit.Material.AIR){
                as.getEquipment().setHelmet(saved.clone());
            }
        }
    }


}
