package br.com.novaera.mysticcraft.config;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class StructureConfig {
    public final Material coreMat;
    public final Material altarMat;
    public final double standYOffset;
    public final boolean standSmall;
    public final boolean standMarker;
    public final List<int[]> altarOffsets;   // {dx, dy, dz}
    public final double standBackOffset;
    public final Set<Material> allowedItems;

    // Integrações (ItemsAdder)
    public final boolean itemsadderEnabled;
    public final Set<String> allowedCustomIds;   // IA itens permitidos (namespace:id)
    public final String coreCustomBlockId;       // IA bloco do núcleo (namespace:id) ou null
    public final String altarCustomBlockId;      // IA bloco do altar (namespace:id) ou null

    // spin
    public final boolean spinEnabled;
    public final double  spinDegPerStep;
    public final int     spinPeriodTicks;
    public final String  spinMode;

    // effects
    public final boolean effectsEnabled;
    public final EffectSpec fxAssemble, fxDisassemble, fxPut, fxTake;

    public StructureConfig(Material coreMat, Material altarMat, double standYOffset,
                           boolean standSmall, boolean standMarker,
                           List<int[]> offsets,
                           double standBackOffset,
                           Set<Material> allowedItems,
                           boolean itemsadderEnabled, Set<String> allowedCustomIds,
                           String coreCustomBlockId, String altarCustomBlockId,
                           boolean spinEnabled, double spinDegPerStep, int spinPeriodTicks, String spinMode,
                           boolean effectsEnabled, EffectSpec fxAssemble, EffectSpec fxDisassemble, EffectSpec fxPut, EffectSpec fxTake) {
        this.coreMat = coreMat;
        this.altarMat = altarMat;
        this.standYOffset = standYOffset;
        this.standSmall = standSmall;
        this.standMarker = standMarker;
        this.altarOffsets = offsets;
        this.standBackOffset = standBackOffset;
        this.allowedItems = allowedItems;

        this.itemsadderEnabled = itemsadderEnabled;
        this.allowedCustomIds  = allowedCustomIds;
        this.coreCustomBlockId = coreCustomBlockId;
        this.altarCustomBlockId = altarCustomBlockId;

        this.spinEnabled = spinEnabled;
        this.spinDegPerStep = spinDegPerStep;
        this.spinPeriodTicks = spinPeriodTicks;
        this.spinMode = spinMode;

        this.effectsEnabled = effectsEnabled;
        this.fxAssemble = fxAssemble;
        this.fxDisassemble = fxDisassemble;
        this.fxPut = fxPut;
        this.fxTake = fxTake;
    }

    public static StructureConfig load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();

        ConfigurationSection root = plugin.getConfig().getConfigurationSection("mysticcraft");
        if (root == null) throw new IllegalStateException("Config 'mysticcraft' faltando.");

        ConfigurationSection coreSec  = root.getConfigurationSection("core");
        ConfigurationSection altarSec = root.getConfigurationSection("altar");
        ConfigurationSection standSec = (altarSec != null) ? altarSec.getConfigurationSection("stand") : null;

        // integrações
        ConfigurationSection intSec = root.getConfigurationSection("integrations");
        ConfigurationSection iaSec  = (intSec != null) ? intSec.getConfigurationSection("itemsadder") : null;
        boolean iaEnabled = (iaSec == null) || iaSec.getBoolean("enabled", true);

        // materiais (fallback vanilla)
        Material core  = matchOrDefault(coreSec  != null ? coreSec.getString("material")  : null, Material.ENCHANTING_TABLE);
        Material altar = matchOrDefault(altarSec != null ? altarSec.getString("material") : null, Material.POLISHED_BLACKSTONE);

        // stand
        boolean small   = standSec != null && standSec.getBoolean("small",  true);
        boolean marker  = standSec != null && standSec.getBoolean("marker", true);
        double  yOff    = (standSec != null) ? standSec.getDouble("y_offset", 0.62) : 0.62;
        double  backOff = (standSec != null) ? standSec.getDouble("radial_back_offset", 0.08) : 0.08;

        // IA: custom blocks para core/altar
        String coreIaId  = (coreSec  != null) ? coreSec.getString("custom_block", "")  : "";
        String altarIaId = (altarSec != null) ? altarSec.getString("custom_block", "") : "";
        coreIaId  = (coreIaId  != null && !coreIaId.isBlank())  ? coreIaId.trim().toLowerCase(Locale.ROOT)  : null;
        altarIaId = (altarIaId != null && !altarIaId.isBlank()) ? altarIaId.trim().toLowerCase(Locale.ROOT) : null;

        // whitelist vanilla
        Set<Material> allow = new HashSet<>();
        if (altarSec != null) {
            for (String s : altarSec.getStringList("allowed_items")) {
                if (s == null || s.isBlank()) continue;
                Material m = Material.matchMaterial(s.trim());
                if (m != null) allow.add(m);
            }
        }

        // whitelist IA (itens)
        Set<String> allowIa = new HashSet<>();
        if (altarSec != null) {
            for (String s : altarSec.getStringList("allowed_custom")) {
                if (s != null && !s.isBlank()) allowIa.add(s.trim().toLowerCase(Locale.ROOT));
            }
        }

        // spin (default ligado se seção ausente)
        ConfigurationSection spinSec = (standSec != null) ? standSec.getConfigurationSection("spin") : null;
        boolean spinEnabled = (spinSec == null) || spinSec.getBoolean("enabled", true);
        double  spinStep    = (spinSec != null) ? spinSec.getDouble("deg_per_step", 2.0) : 2.0;
        int     spinPeriod  = (spinSec != null) ? spinSec.getInt("period_ticks", 2) : 2;
        String  spinMode    = (spinSec != null) ? spinSec.getString("mode", "HEAD_POSE") : "HEAD_POSE";

        // effects
        ConfigurationSection fxSec = root.getConfigurationSection("effects");
        boolean effectsEnabled = fxSec == null || fxSec.getBoolean("enabled", true);

        EffectSpec defAssemble    = defEffect("ENCHANTMENT_TABLE", 60, 0.50, 0.01, "BLOCK_ENCHANTMENT_TABLE_USE", 1.0f, 1.2f);
        EffectSpec defDisassemble = defEffect("CLOUD",               40, 0.40, 0.00, "BLOCK_GLASS_BREAK",          0.9f, 1.0f);
        EffectSpec defPut         = defEffect("END_ROD",             12, 0.05, 0.00, "ENTITY_EXPERIENCE_ORB_PICKUP", 0.6f, 1.5f);
        EffectSpec defTake        = defEffect("CLOUD",               10, 0.10, 0.00, "UI_BUTTON_CLICK",            0.5f, 1.8f);

        EffectSpec fxAssemble    = readEffect(fxSec, "assemble",    defAssemble);
        EffectSpec fxDisassemble = readEffect(fxSec, "disassemble", defDisassemble);
        EffectSpec fxPut         = readEffect(fxSec, "put",         defPut);
        EffectSpec fxTake        = readEffect(fxSec, "take",        defTake);

        List<int[]> offs = new ArrayList<>();

        // 1) GRADE (lista de strings V/P/A)
        List<String> grid = root.getStringList("altars");
        if (!grid.isEmpty()) {
            int rows = grid.size();
            int cols = -1;
            for (String row : grid) {
                if (cols == -1) cols = row.length();
                else if (row.length() != cols)
                    throw new IllegalStateException("Todas as linhas da grade 'altars' precisam ter o MESMO comprimento.");
            }

            int aRow = -1, aCol = -1, countA = 0;
            for (int r = 0; r < rows; r++) {
                String row = grid.get(r);
                for (int c = 0; c < cols; c++) {
                    char ch = row.charAt(c);
                    if (ch == 'A') { aRow = r; aCol = c; countA++; }
                }
            }
            if (countA == 0) throw new IllegalStateException("A grade 'altars' precisa ter 1 'A' (núcleo).");
            if (countA > 1) throw new IllegalStateException("A grade 'altars' deve ter APENAS 1 'A'.");

            for (int r = 0; r < rows; r++) {
                String row = grid.get(r);
                for (int c = 0; c < cols; c++) {
                    if (row.charAt(c) == 'P') {
                        int dx = c - aCol;
                        int dz = r - aRow;
                        offs.add(new int[]{dx, 0, dz});
                    }
                }
            }

            return new StructureConfig(core, altar, yOff, small, marker, offs, backOff, allow,
                    iaEnabled, allowIa,
                    coreIaId, altarIaId,
                    spinEnabled, spinStep, spinPeriod, spinMode,
                    effectsEnabled, fxAssemble, fxDisassemble, fxPut, fxTake);
        } // <<< FECHA o if da grade

        // 2) Fallback: lista de mapas {dx,dy,dz}
        List<Map<?, ?>> raw = root.getMapList("altars");
        for (Map<?, ?> m0 : raw) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) (Map<?, ?>) m0;
            int dx = toInt(m.get("dx"), 0);
            int dy = toInt(m.get("dy"), 0);
            int dz = toInt(m.get("dz"), 0);
            offs.add(new int[]{dx, dy, dz});
        }

        return new StructureConfig(core, altar, yOff, small, marker, offs, backOff, allow,
                iaEnabled, allowIa,
                coreIaId, altarIaId,
                spinEnabled, spinStep, spinPeriod, spinMode,
                effectsEnabled, fxAssemble, fxDisassemble, fxPut, fxTake);
    }

    /* ================= helpers ================= */

    private static Material matchOrDefault(String name, Material def) {
        if (name == null) return def;
        Material m = Material.matchMaterial(name);
        return (m != null) ? m : def;
    }

    private static int toInt(Object val, int def) {
        if (val == null) return def;
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }

    private static EffectSpec readEffect(ConfigurationSection root, String path, EffectSpec def) {
        if (root == null) return def;
        ConfigurationSection s = root.getConfigurationSection(path);
        if (s == null) return def;

        Particle p = parseParticle(s.getString("particle"), def.particle);
        int count = s.getInt("count", def.count);
        double offset = s.getDouble("offset", def.offset);
        double speed = s.getDouble("speed", def.speed);
        Sound   sound = parseSound(s.getString("sound"), def.sound);
        float volume = (float) s.getDouble("volume", def.volume);
        float pitch  = (float) s.getDouble("pitch",  def.pitch);

        return new EffectSpec(p, count, offset, speed, sound, volume, pitch);
    }

    private static EffectSpec defEffect(String particleName, int count, double offset, double speed,
                                        String soundName, float volume, float pitch) {
        Particle p = parseParticle(particleName, Particle.CLOUD);
        Sound   s = parseSound(soundName, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
        return new EffectSpec(p, count, offset, speed, s, volume, pitch);
    }

    private static Particle parseParticle(String name, Particle def){
        if (name == null || name.isBlank()) return def;
        try { return Particle.valueOf(name.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex){ return def; }
    }

    private static Sound parseSound(String name, Sound def){
        if (name == null || name.isBlank()) return def;
        try { return Sound.valueOf(name.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex){ return def; }
    }

    private static <E extends Enum<E>> E matchEnum(Class<E> cls, String name, E def) {
        if (name == null || name.isBlank()) return def;
        try { return Enum.valueOf(cls, name.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return def; }
    }

    /* ===== efeito ===== */
    public static final class EffectSpec {
        public final Particle particle;
        public final int count;
        public final double offset;
        public final double speed;
        public final Sound sound;
        public final float volume;
        public final float pitch;

        public EffectSpec(Particle particle, int count, double offset, double speed,
                          Sound sound, float volume, float pitch){
            this.particle = particle;
            this.count = count;
            this.offset = offset;
            this.speed = speed;
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
