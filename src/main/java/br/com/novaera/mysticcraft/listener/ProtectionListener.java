package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.MysticCraft;
import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import br.com.novaera.mysticcraft.i18n.Lang;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public final class ProtectionListener implements Listener {
    private static final double STAND_ZONE_RADIUS = 1.2;
    private static final double STAND_ZONE_R2 = STAND_ZONE_RADIUS * STAND_ZONE_RADIUS;

    private final MysticCraft plugin;
    private final StructureManager sm;
    private final StructureService service;

    public ProtectionListener(MysticCraft plugin, StructureManager sm, StructureService service){
        this.plugin = plugin;
        this.sm = sm;
        this.service = service;
    }

    private boolean protEnabled(){
        return plugin.getConfig().getBoolean("mysticcraft.protections.enabled", true);
    }
    private boolean requirePerm(){
        return plugin.getConfig().getBoolean("mysticcraft.protections.require_permission", false);
    }
    private boolean blockPistons(){
        return plugin.getConfig().getBoolean("mysticcraft.protections.block.pistons", true);
    }
    private boolean blockExplosions(){
        return plugin.getConfig().getBoolean("mysticcraft.protections.block.explosions", true);
    }
    private boolean blockFluids(){
        return plugin.getConfig().getBoolean("mysticcraft.protections.block.fluids", true);
    }
    private boolean blockArmorStandDirect(){
        return plugin.getConfig().getBoolean("mysticcraft.protections.block.armorstand_direct", true);
    }

    // =================== Colocar/Quebrar: perm simples ===================
    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e){
        if (!protEnabled()) return;
        Block b = e.getBlockPlaced();
        boolean isCore  = b.getType() == sm.cfg().coreMat;
        boolean isAltar = b.getType() == sm.cfg().altarMat;
        if (!isCore && !isAltar) return;

        if (requirePerm() && !e.getPlayer().hasPermission("mystic.use") && !e.getPlayer().hasPermission("mystic.admin")){
            e.setCancelled(true);
            e.getPlayer().sendMessage(Lang.t("no_permission"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e){
        if (!protEnabled()) return;
        Block b = e.getBlock();
        boolean isCore  = sm.isCore(b) || b.getType() == sm.cfg().coreMat;
        boolean isAltar = sm.isAltar(b) || b.getType() == sm.cfg().altarMat;
        if (!isCore && !isAltar) return;

        if (requirePerm() && !e.getPlayer().hasPermission("mystic.use") && !e.getPlayer().hasPermission("mystic.admin")){
            e.setCancelled(true);
            e.getPlayer().sendMessage(Lang.t("no_permission"));
        }
    }

    // =================== Pistões ===================
    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e){
        if (!protEnabled() || !blockPistons()) return;
        for (Block b : e.getBlocks()){
            if (b.getType() == sm.cfg().coreMat || b.getType() == sm.cfg().altarMat){
                e.setCancelled(true);
                return;
            }
        }
    }
    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e){
        if (!protEnabled() || !blockPistons()) return;
        for (Block b : e.getBlocks()){
            if (b.getType() == sm.cfg().coreMat || b.getType() == sm.cfg().altarMat){
                e.setCancelled(true);
                return;
            }
        }
    }

    // =================== Explosões ===================
    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e){
        if (!protEnabled() || !blockExplosions()) return;
        e.blockList().removeIf(b -> b.getType() == sm.cfg().coreMat || b.getType() == sm.cfg().altarMat);
    }
    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e){
        if (!protEnabled() || !blockExplosions()) return;
        e.blockList().removeIf(b -> b.getType() == sm.cfg().coreMat || b.getType() == sm.cfg().altarMat);
    }

    // =================== Fluidos ===================
    @EventHandler(ignoreCancelled = true)
    public void onFluid(BlockFromToEvent e){
        if (!protEnabled() || !blockFluids()) return;
        Block to = e.getToBlock();
        if (to.getType() == sm.cfg().coreMat || to.getType() == sm.cfg().altarMat){
            e.setCancelled(true);
        }
    }

    // =================== ArmorStand: bloquear interação direta na zona do altar ===================
    @EventHandler(ignoreCancelled = true)
    public void onArmorStandManip(PlayerArmorStandManipulateEvent e){
        if (!protEnabled() || !blockArmorStandDirect()) return;

        // admins podem burlar
        if (e.getPlayer().hasPermission("mystic.protect.bypass") || e.getPlayer().hasPermission("mystic.admin")) return;

        // bloqueia se for nosso stand marcado OU se estiver na zona do stand do altar
        var as = e.getRightClicked();
        var pdc = as.getPersistentDataContainer();
        if (pdc.has(br.com.novaera.mysticcraft.util.Keys.ALTAR_STAND) || isInAltarStandZone(as.getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent e){
        if (!protEnabled() || !blockArmorStandDirect()) return;
        if (!(e.getRightClicked() instanceof org.bukkit.entity.ArmorStand as)) return;

        // admins podem burlar
        if (e.getPlayer().hasPermission("mystic.protect.bypass") || e.getPlayer().hasPermission("mystic.admin")) return;

        // bloqueia se for nosso stand marcado OU se estiver na zona do stand do altar
        var pdc = as.getPersistentDataContainer();
        if (pdc.has(br.com.novaera.mysticcraft.util.Keys.ALTAR_STAND) || isInAltarStandZone(as.getLocation())) {
            e.setCancelled(true);
        }
    }

    // Zona do stand = pontos esperados (expectedStandLocation) para todos os altares de todos os núcleos salvos
    private boolean isInAltarStandZone(Location loc) {
        var w = loc.getWorld();
        if (w == null) return false;

        for (Location coreLoc : service.allCores()) {
            if (!coreLoc.getWorld().equals(w)) continue;
            var core = coreLoc.getBlock();
            for (int[] off : sm.cfg().altarOffsets) {
                var altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
                var expected = service.expectedStandLocation(core, altar);
                if (!expected.getWorld().equals(w)) continue;
                if (expected.distanceSquared(loc) <= STAND_ZONE_R2) {
                    return true;
                }
            }
        }
        return false;
    }
}
