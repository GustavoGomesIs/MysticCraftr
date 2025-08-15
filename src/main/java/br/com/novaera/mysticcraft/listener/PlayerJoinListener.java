package br.com.novaera.mysticcraft.listener;

import br.com.novaera.mysticcraft.MysticCraft;
import br.com.novaera.mysticcraft.core.StructureService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {
    private final StructureService service;
    private final MysticCraft plugin; // << agora é MysticCraft

    public PlayerJoinListener(StructureService service, MysticCraft plugin) {
        this.service = service;
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (var loc : service.allCores()) {
                service.pokeRepairOnly(loc.getBlock());
            }
            plugin.getServer().getScheduler().runTaskLater(plugin, plugin::restartSpin, 1L);
        }, 20L);
    }
}
