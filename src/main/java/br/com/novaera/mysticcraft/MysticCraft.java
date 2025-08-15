package br.com.novaera.mysticcraft;

import br.com.novaera.mysticcraft.command.MysticCommand;
import br.com.novaera.mysticcraft.config.StructureConfig;
import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import br.com.novaera.mysticcraft.core.StructureStorage;
import br.com.novaera.mysticcraft.i18n.Lang;
import br.com.novaera.mysticcraft.integrations.IaFurnitureListener;
import br.com.novaera.mysticcraft.integrations.ItemsAdderCompat;
import br.com.novaera.mysticcraft.listener.*;
import br.com.novaera.mysticcraft.task.SpinTask;
import br.com.novaera.mysticcraft.util.Keys;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class MysticCraft extends JavaPlugin {
    private StructureManager sm;
    private StructureService service;
    private SpinTask spinTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Keys.init(this);
        br.com.novaera.mysticcraft.util.PdcBlocks.init(this);
        Lang.init(this);

        StructureConfig cfg = StructureConfig.load(this);
        sm = new StructureManager(cfg);

        StructureStorage storage = new StructureStorage(this);
        service = new StructureService(sm, storage);

        // Listeners principais
        Bukkit.getPluginManager().registerEvents(new CorePlaceListener(sm, service), this);
        Bukkit.getPluginManager().registerEvents(new AltarPlaceListener(sm, service, this), this);
        Bukkit.getPluginManager().registerEvents(new AltarBreakListener(service), this);
        Bukkit.getPluginManager().registerEvents(new AltarInteractListener(sm, service, this), this);
        Bukkit.getPluginManager().registerEvents(new CoreBreakListener(service), this);
        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this, sm, service), this);
        Bukkit.getPluginManager().registerEvents(new ChunkLoadListener(service, this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(service, this), this);

        if (ItemsAdderCompat.isEnabled()) {
            getServer().getPluginManager().registerEvents(new IaFurnitureListener(service, this), this);
        }

        // Reconstrução atrasada (não remova entradas no rebuildAllOnEnable!)
        getServer().getScheduler().runTaskLater(this, () -> service.rebuildAllOnEnable(), 60L);   // ~3s
        getServer().getScheduler().runTaskLater(this, () -> service.rebuildAllOnEnable(), 200L); // ~10s

        // NOVO: reinicia o spin logo após as ondas de rebuild
        getServer().getScheduler().runTaskLater(this, this::restartSpin, 70L);
        getServer().getScheduler().runTaskLater(this, this::restartSpin, 210L);

        // Ondas de "poke" seguro (só repara, nunca desmonta)
        getServer().getScheduler().runTaskLater(this, () -> {
            for (var loc : service.allCores()) service.pokeRepairOnly(loc.getBlock());
        }, 60L);
        getServer().getScheduler().runTaskLater(this, () -> {
            for (var loc : service.allCores()) service.pokeRepairOnly(loc.getBlock());
        }, 200L);
        getServer().getScheduler().runTaskLater(this, () -> {
            for (var loc : service.allCores()) service.pokeRepairOnly(loc.getBlock());
        }, 600L);


        // NOVO: restart após cada onda de poke
        getServer().getScheduler().runTaskLater(this, this::restartSpin, 75L);
        getServer().getScheduler().runTaskLater(this, this::restartSpin, 220L);
        getServer().getScheduler().runTaskLater(this, this::restartSpin, 610L);

        // Comando /mystic
        MysticCommand mc = new MysticCommand(this, sm, service);
        var cmd = getCommand("mystic");
        if (cmd == null) {
            getLogger().severe("Comando 'mystic' não encontrado no plugin.yml!");
        } else {
            cmd.setExecutor(mc);
            cmd.setTabCompleter(mc);
        }

        // Spin
        spinTask = new SpinTask(this, sm, service);
        spinTask.start();

        getLogger().info("MysticCraft ligado. Mesas persistentes habilitadas.");
    }

    @Override
    public void onDisable() {
        if (spinTask != null) spinTask.stop();
        getLogger().info("MysticCraft desligado.");
    }

    // usado pelo /mystic reload
    public void restartSpin() {
        if (spinTask != null) spinTask.stop();
        spinTask = new SpinTask(this, sm, service);
        spinTask.start();
    }
}
