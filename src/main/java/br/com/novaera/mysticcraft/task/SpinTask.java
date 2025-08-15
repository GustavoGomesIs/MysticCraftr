package br.com.novaera.mysticcraft.task;

import br.com.novaera.mysticcraft.MysticCraft;
import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import br.com.novaera.mysticcraft.config.StructureConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;
import org.bukkit.Material;

public final class SpinTask {
    private final MysticCraft plugin;
    private final StructureManager sm;
    private final StructureService service;

    private BukkitTask task;

    public SpinTask(MysticCraft plugin, StructureManager sm, StructureService service){
        this.plugin   = plugin;
        this.sm       = sm;
        this.service  = service;
    }

    public void start(){
        stop();
        long period = Math.max(1L, sm.cfg().spinPeriodTicks); // garante >=1
        // roda SEMPRE; ele mesmo se auto-“noopa” quando desabilitado
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop(){
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick(){
        StructureConfig cfg = sm.cfg();
        if (!cfg.spinEnabled) return;

        final boolean useHeadPose = "HEAD_POSE".equalsIgnoreCase(cfg.spinMode);
        final double stepDeg = cfg.spinDegPerStep;
        final double stepRad = Math.toRadians(stepDeg);

        // NADA de cache: varre as mesas salvas e encontra o stand atual de cada altar
        for (Location coreLoc : service.allCores()){
            Block core = coreLoc.getBlock();

            for (int[] off : cfg.altarOffsets){
                Block altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();

                // Só anima se esse altar ainda for um altar válido/esperado
                if (!sm.matchesAltarBlock(altar)) continue;

                Location expected = service.expectedStandLocation(core, altar);
                ArmorStand as = sm.findStandNear(expected);
                if (as == null) continue;

                // >>> só anima se tiver item no “capacete”
                var equip = as.getEquipment();
                if (equip == null) continue;
                var helm = equip.getHelmet();
                if (helm == null || helm.getType() == Material.AIR) continue;
                // <<< fim da checagem

                if (useHeadPose) {
                    var pose = as.getHeadPose();
                    as.setHeadPose(new EulerAngle(pose.getX(), pose.getY() + stepRad, pose.getZ()));
                } else {
                    var l = as.getLocation();
                    l.setYaw(l.getYaw() + (float) stepDeg);
                    as.teleport(l);
                }

            }
        }
    }
}
