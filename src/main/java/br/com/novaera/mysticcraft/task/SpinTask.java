package br.com.novaera.mysticcraft.task;

import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.EulerAngle;

public final class SpinTask {
    private final JavaPlugin plugin;
    private final StructureManager sm;
    private final StructureService service;
    private org.bukkit.scheduler.BukkitTask handle;

    public SpinTask(JavaPlugin plugin, StructureManager sm, StructureService service){
        this.plugin = plugin;
        this.sm = sm;
        this.service = service;
    }

    public void start(){
        stop();
        if (!sm.cfg().spinEnabled) return; // não agenda se está desligado
        int period = Math.max(1, sm.cfg().spinPeriodTicks);
        handle = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop(){
        if (handle != null){ handle.cancel(); handle = null; }
    }

    private void tick(){
        var cfg = sm.cfg();
        double stepDeg = cfg.spinDegPerStep;
        boolean useHead = "HEAD_POSE".equalsIgnoreCase(cfg.spinMode);

        boolean rotatedSomething = false; // << escopo correto

        for (Location coreLoc : service.allCores()){
            var core = coreLoc.getBlock();
            if (!service.isIntact(core)) continue;

            for (int[] off : cfg.altarOffsets){
                var altar = core.getLocation().add(off[0], off[1], off[2]).getBlock();
                Location expected = service.expectedStandLocation(core, altar);
                ArmorStand as = sm.findStandNear(expected);
                if (as == null) continue; // evita NPE

                ItemStack helm = as.getEquipment().getHelmet();
                if (helm == null || helm.getType() == Material.AIR) continue; // só gira com item

                if (useHead){
                    EulerAngle pose = as.getHeadPose();
                    as.setHeadPose(new EulerAngle(
                            pose.getX(),
                            pose.getY() + Math.toRadians(stepDeg),
                            pose.getZ()
                    ));
                } else {
                    Location l = as.getLocation();
                    l.setYaw(l.getYaw() + (float) stepDeg);
                    as.teleport(l);
                }
                rotatedSomething = true;
            }
        }

        if (!rotatedSomething) {
            stop(); // pausa a task até alguém colocar um item
        }
    }
}
