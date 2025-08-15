package br.com.novaera.mysticcraft.command;

import br.com.novaera.mysticcraft.MysticCraft;
import br.com.novaera.mysticcraft.config.StructureConfig;
import br.com.novaera.mysticcraft.core.StructureManager;
import br.com.novaera.mysticcraft.core.StructureService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class MysticCommand implements CommandExecutor, TabCompleter {

    private final MysticCraft plugin;
    private final StructureManager sm;
    private final StructureService service;

    public MysticCommand(MysticCraft plugin, StructureManager sm, StructureService service){
        this.plugin = plugin;
        this.sm = sm;
        this.service = service;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§eUso: /" + label + " reload §7ou§e /" + label + " debug [list]");
            return true;
        }

        // ===== /mystic reload =====
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mystic.admin")) {
                sender.sendMessage("§cVocê não tem permissão para isso.");
                return true;
            }
            long t0 = System.currentTimeMillis();

            plugin.reloadConfig();
            br.com.novaera.mysticcraft.i18n.Lang.reload();
            StructureConfig newCfg = StructureConfig.load(plugin);
            sm.updateConfig(newCfg);
            service.retuneAllStandsAfterConfigChange();
            plugin.restartSpin();

            long dt = System.currentTimeMillis() - t0;
            sender.sendMessage("§aMysticCraft recarregado em " + dt + " ms.");
            return true;
        }

        // ===== /mystic debug [list] =====
        if (args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("mystic.admin")) {
                sender.sendMessage("§cVocê não tem permissão para isso.");
                return true;
            }

            // /mystic debug list  -> lista todos os núcleos salvos
            if (args.length >= 2 && args[1].equalsIgnoreCase("list")) {
                Set<Location> cores = service.allCores();
                if (cores.isEmpty()){
                    sender.sendMessage("§eNenhuma mesa salva.");
                    return true;
                }
                sender.sendMessage("§6Mesas salvas:");
                for (Location loc : cores) {
                    boolean intact = service.isIntact(loc.getBlock());
                    sender.sendMessage(" §7- §f" + fmtLoc(loc) + " §8(íntegra: " + (intact ? "§aSIM" : "§cNÃO§8") + "§8)");
                }
                return true;
            }

            // Interativo: mira num CORE ou ALTAR a até 6 blocos
            if (!(sender instanceof Player p)) {
                sender.sendMessage("§cUse /mystic debug in-game mirando num núcleo ou pilar.");
                return true;
            }

            Block target = p.getTargetBlockExact(6);
            if (target == null) {
                sender.sendMessage("§eMire num núcleo ou pilar a até 6 blocos, ou use §7/mystic debug list§e.");
                return true;
            }

            Block core = null;
            if (target.getType() == sm.cfg().coreMat) {
                core = target;
            } else if (target.getType() == sm.cfg().altarMat) {
                core = service.findOwningCoreOfAltar(target);
            }

            if (core == null) {
                sender.sendMessage("§eEsse bloco não é núcleo nem pilar conhecido. Mire num núcleo ou pilar, ou use §7/mystic debug list§e.");
                return true;
            }

            // Relatório
            reportForCore(sender, core);
            return true;
        }

        sender.sendMessage("§eUso: /" + label + " reload §7ou§e /" + label + " debug [list]");
        return true;
    }

    private void reportForCore(CommandSender sender, Block core){
        var cfg = sm.cfg();
        boolean intact = service.isIntact(core);
        List<int[]> missing = service.missingAltarsForCore(core);

        sender.sendMessage("§6== MysticCraft :: DEBUG ==");
        sender.sendMessage("§7Core: §f" + fmtLoc(core.getLocation()));
        sender.sendMessage("§7Materiais: §fcore=" + cfg.coreMat + " §7| §faltar=" + cfg.altarMat);
        sender.sendMessage("§7Stand: §fy_offset=" + cfg.standYOffset + " §7| §fback=" + cfg.standBackOffset + " §7| §fsmall=" + cfg.standSmall + " §7| §fmarker=" + cfg.standMarker);
        sender.sendMessage("§7Spin: §fenabled=" + cfg.spinEnabled + " §7| §fdeg/step=" + cfg.spinDegPerStep + " §7| §fperiod=" + cfg.spinPeriodTicks + "t §7| §fmode=" + cfg.spinMode);

        if (intact) {
            sender.sendMessage("§aEstrutura íntegra. Todos os pilares estão presentes.");
        } else {
            sender.sendMessage("§cEstrutura incompleta. Faltam estes offsets (dx,dy,dz):");
            StringBuilder sb = new StringBuilder(" §7");
            for (int i = 0; i < missing.size(); i++){
                int[] o = missing.get(i);
                sb.append("[").append(o[0]).append(",").append(o[1]).append(",").append(o[2]).append("]");
                if (i < missing.size()-1) sb.append(", ");
            }
            sender.sendMessage(sb.toString());
        }

        // Dica: mostrar um exemplo de um altar esperado (primeiro offset)
        if (!cfg.altarOffsets.isEmpty()){
            int[] o = cfg.altarOffsets.get(0);
            Location expectedAltar = core.getLocation().add(o[0], o[1], o[2]);
            sender.sendMessage("§7Exemplo de altar (primeiro offset): §f" + fmtLoc(expectedAltar));
        }
    }

    private String fmtLoc(Location l){
        return l.getWorld().getName() + " @ " + l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            String part = args[0].toLowerCase();
            List<String> opts = Arrays.asList("reload", "debug");
            List<String> out = new ArrayList<>();
            for (String o : opts) if (o.startsWith(part)) out.add(o);
            return out;
        } else if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            if ("list".startsWith(args[1].toLowerCase())) return Collections.singletonList("list");
        }
        return Collections.emptyList();
    }
}
