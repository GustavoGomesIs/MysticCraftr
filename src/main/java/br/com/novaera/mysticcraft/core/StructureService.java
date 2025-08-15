package br.com.novaera.mysticcraft.core;

import br.com.novaera.mysticcraft.config.StructureConfig;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.*;

public final class StructureService {
    private final StructureManager sm;
    private final StructureStorage storage;

    public java.util.Set<Location> allCores() {
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

    // Monta de fato: marca, garante 1 stand/altar e salva no storage
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

            // garante exatamente 1 stand por altar (só stands MARCADOS)
            ArmorStand as = ensureSingleStand(core, altar);

            // (em geral vazio aqui, mas se tiver item salvo, repõe)
            var saved = storage.getAltarItem(core.getLocation(), off);
            if (saved != null && saved.getType() != Material.AIR) {
                var cur = as.getEquipment().getHelmet();
                if (cur == null || cur.getType() == Material.AIR) {
                    as.getEquipment().setHelmet(saved.clone());
                }
            }
        }
        // salva + fx + aviso
        storage.add(core.getLocation());
        playFx(cfg().fxAssemble, core.getLocation().add(0.5, 1.0, 0.5));
        notifyAssembleNear(core.getLocation(), pOpt);
        return true;
    }

    // Desmonta: remove TODOS os stands MARCADOS, desmarca e remove do storage
    public void disassemble(Block core, Player pOpt) {
        for (int[] off : sm.cfg().altarOffsets) {
            Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
            Location standLoc = expectedStandLocation(core, altar);
            for (ArmorStand as : findTaggedStandsNear(standLoc, STAND_CANDIDATE_RADIUS)) {
                sm.dropHelmet(as);
                as.remove();
            }
            sm.unmarkAltar(altar);
        }
        if (sm.isCore(core)) sm.unmarkCore(core);
        storage.remove(core.getLocation());
        playFx(cfg().fxDisassemble, core.getLocation().add(0.5, 1.0, 0.5));
        notifyDisassembleNear(core.getLocation(), pOpt);
    }

    // Reconstrói todas as mesas salvas no restart (sem criar stands no boot!)
    public void rebuildAllOnEnable() {
        for (Location loc : storage.all()) {
            Block core = loc.getBlock();
            if (!isIntact(core)) {
                // se estiver incompleta no boot, não desmonta nem cria nada aqui.
                // Os listeners (join/chunk) e os pokes agendados vão reparar depois.
                continue;
            }

            // Só remarcar núcleo/altares agora. Nada de spawnar stand ou repor capacete aqui!
            sm.markCore(core);
            for (int[] off : sm.cfg().altarOffsets) {
                Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
                sm.markAltar(altar);
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
            if (storage.contains(coreLoc)) {
                return coreLoc.getBlock();
            }
        }
        return null;
    }

    // posição esperada do stand: centro do altar + y_offset + empurrão radial (pra trás, em direção ao núcleo) e yaw voltado para o núcleo
    public Location expectedStandLocation(Block core, Block altar) {
        Location center = altar.getLocation().add(0.5, cfg().standYOffset, 0.5);

        double vx = altar.getX() - core.getX();
        double vz = altar.getZ() - core.getZ();
        double len = Math.hypot(vx, vz);
        if (len > 0.0001) {
            // normaliza vetor e empurra PRA TRÁS (em direção ao núcleo)
            vx /= len; vz /= len;
            center.add(-vx * cfg().standBackOffset, 0, -vz * cfg().standBackOffset);

            // olhar para o núcleo
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
                ensureSingleStand(core, altar);
            }
        }
    }

    // Quais offsets de altares estão faltando ao redor deste núcleo
    public java.util.List<int[]> missingAltarsForCore(Block core) {
        java.util.List<int[]> miss = new java.util.ArrayList<>();
        if (core == null || !sm.matchesCoreBlock(core)) return miss;
        for (int[] off : sm.cfg().altarOffsets) {
            Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
            if (!sm.matchesAltarBlock(altar)) miss.add(off);
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

    public void playFx(StructureConfig.EffectSpec fx, Location loc) {
        if (fx == null || !cfg().effectsEnabled || loc == null || loc.getWorld() == null) return;
        var w = loc.getWorld();
        w.spawnParticle(fx.particle, loc.getX(), loc.getY(), loc.getZ(), fx.count, fx.offset, fx.offset, fx.offset, fx.speed);
        w.playSound(loc, fx.sound, fx.volume, fx.pitch);
    }

    public java.util.Set<Location> coresInChunk(Chunk c){
        java.util.Set<Location> out = new java.util.HashSet<>();
        for (Location loc : storage.all()){
            if (loc.getWorld() == null) continue;
            if (!loc.getWorld().equals(c.getWorld())) continue;
            if ((loc.getBlockX() >> 4) == c.getX() && (loc.getBlockZ() >> 4) == c.getZ()){
                out.add(loc);
            }
        }
        return out;
    }

    public void repairCoreAt(Location coreLoc){
        Block core = coreLoc.getBlock();
        if (!isIntact(core)) return;
        sm.markCore(core);
        for (int[] off : sm.cfg().altarOffsets){
            Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
            sm.markAltar(altar);

            ArmorStand as = ensureSingleStand(core, altar);

            var saved = storage.getAltarItem(core.getLocation(), off);
            if (saved != null && saved.getType() != Material.AIR){
                as.getEquipment().setHelmet(saved.clone());
            }
        }
    }

    /** acha o núcleo salvo (ou provável) mais perto de uma posição. */
    public Block findNearestCore(Location around) {
        if (around == null) return null;

        Location base = around.getBlock().getLocation();

        // 1) Se já está salvo, é ele
        if (storage.contains(base)) return base.getBlock();

        // 2) Offsets a partir de um ALTAR próximo (salvo ou "parece core")
        for (int[] off : sm.cfg().altarOffsets) {
            Location coreLoc = base.clone().add(-off[0], -off[1], -off[2]);
            if (storage.contains(coreLoc)) return coreLoc.getBlock();
            Block coreCand = coreLoc.getBlock();
            if (sm.matchesCoreBlock(coreCand)) return coreCand;
        }

        // 3) Varredura curta por um core plausível
        int scan = 4;
        Block best = null;
        double bestD = Double.MAX_VALUE;
        for (int dx = -scan; dx <= scan; dx++) {
            for (int dz = -scan; dz <= scan; dz++) {
                Block b = base.clone().add(dx, 0, dz).getBlock();
                if (sm.matchesCoreBlock(b)) {
                    double d = b.getLocation().distanceSquared(base);
                    if (d < bestD) { bestD = d; best = b; }
                }
            }
        }
        return best;
    }

    // Envia mensagem só para quem montou/desmontou (sem broadcast)
    private void notifyAssembleNear(Location coreLoc, Player pOpt) {
        if (pOpt != null) pOpt.sendMessage("§aMesa MysticCraft montada com sucesso!");
    }
    private void notifyDisassembleNear(Location coreLoc, Player pOpt) {
        if (pOpt != null) pOpt.sendMessage("§cMesa MysticCraft desmontada.");
    }

    // Garante integridade; monta se completou, desmonta se quebrou,
    // e REPARA marcas/stands se já estiver salvo e completo (pós-reboot).
    public void ensureIntactOrDisassemble(Block around, Player actor) {
        if (around == null) return;

        Block core = findNearestCore(around.getLocation());
        if (core == null) return;

        Location coreLoc = core.getLocation();
        boolean isSaved  = storage.contains(coreLoc);
        boolean complete = sm.matchesCoreBlock(core) && sm.hasAllPillars(core);

        if (isSaved) {
            if (!complete) {
                disassemble(core, actor);
            } else {
                boolean needsRepair = !sm.isCore(core);
                if (!needsRepair) {
                    for (int[] off : sm.cfg().altarOffsets) {
                        Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
                        if (!sm.isAltar(altar)) { needsRepair = true; break; }
                    }
                }
                if (needsRepair) {
                    repairCoreAt(coreLoc);
                }
            }
            return;
        }

        if (complete) {
            assemble(core, actor);
        }
    }

    // compat antigo (sem actor)
    public void ensureIntactOrDisassemble(Block around) {
        ensureIntactOrDisassemble(around, null);
    }

    /** Repara marcas/stands/itens se o núcleo já estiver salvo e a estrutura estiver completa.
     * Nunca monta e NUNCA desmonta. Seguro para pós-boot/join/chunk. */
    public void pokeRepairOnly(Block around) {
        if (around == null) return;
        Block core = findNearestCore(around.getLocation());
        if (core == null) return;

        Location coreLoc = core.getLocation();
        if (!storage.contains(coreLoc)) return; // só repara mesas já salvas

        boolean complete = sm.matchesCoreBlock(core) && sm.hasAllPillars(core);
        if (!complete) return; // nada de desmontar no boot

        boolean needsRepair = !sm.isCore(core);
        if (!needsRepair) {
            for (int[] off : sm.cfg().altarOffsets) {
                Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
                if (!sm.isAltar(altar)) { needsRepair = true; break; }
            }
        }
        if (needsRepair) {
            repairCoreAt(coreLoc);
        }
    }

    /* ===================== helpers de stand (somente os MARCADOS) ===================== */

    // raio maior para ENCONTRAR candidato (mesmo torto)
    private static final double STAND_CANDIDATE_RADIUS = 1.6;
    // raio pequeno para LIMPAR duplicatas grudadas no expected
    private static final double STAND_CLEAN_RADIUS = 1.05;

    private java.util.List<ArmorStand> findTaggedStandsNear(Location base, double r) {
        java.util.List<ArmorStand> out = new java.util.ArrayList<>();
        var w = base.getWorld(); if (w == null) return out;
        for (var e : w.getNearbyEntities(base, r, r, r)) {
            if (e instanceof ArmorStand as
                    && as.getPersistentDataContainer().has(br.com.novaera.mysticcraft.util.Keys.ALTAR_STAND)) {
                out.add(as);
            }
        }
        return out;
    }

    private ArmorStand nearest(java.util.List<ArmorStand> list, Location target) {
        ArmorStand best = null; double bestD = Double.MAX_VALUE;
        for (ArmorStand as : list) {
            double d = as.getLocation().distanceSquared(target);
            if (d < bestD) { bestD = d; best = as; }
        }
        return best;
    }

    /**
     * Garante exatamente 1 stand MARCADO para este altar:
     *  - Procura candidatos num raio MAIOR (1.6) pra achar o stand “torto”
     *  - Teleporta o melhor pro expected
     *  - Limpa apenas duplicatas muito próximas (<= 1.05) pra não afetar altares vizinhos
     *  - NUNCA mexe em stands não-marcados (furniture do IA)
     */
    private ArmorStand ensureSingleStand(Block core, Block altar) {
        Location expected = expectedStandLocation(core, altar);

        // 1) acha qualquer stand marcado por perto (raio MAIOR)
        java.util.List<ArmorStand> candidates = findTaggedStandsNear(expected, STAND_CANDIDATE_RADIUS);

        ArmorStand keep;
        if (candidates.isEmpty()) {
            // nada marcado por perto -> cria
            keep = sm.spawnAltarStand(expected);
        } else {
            // escolhe o mais perto e alinha
            keep = nearest(candidates, expected);
            if (keep != null) keep.teleport(expected);
        }

        // 2) remove duplicatas apenas se estiverem MUITO próximas
        java.util.List<ArmorStand> nearExpected = findTaggedStandsNear(expected, STAND_CLEAN_RADIUS);
        for (ArmorStand as : nearExpected) {
            if (as == keep) continue;
            sm.dropHelmet(as);
            as.remove();
        }

        // 3) fallback
        if (keep == null) {
            keep = sm.spawnAltarStand(expected);
        }
        return keep;
    }
}