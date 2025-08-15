package br.com.novaera.mysticcraft.config;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

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

    // Offsets dos pilares prontos para uso (Vector dx,dy,dz)
    private List<Vector> pillarOffsets = Collections.emptyList();
    public List<Vector> getPillarOffsets() { return pillarOffsets; }

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

        // ===== Materiais OU IA (unificado) =====
        String coreMatRaw  = (coreSec  != null) ? coreSec.getString("material")  : null;
        String altarMatRaw = (altarSec != null) ? altarSec.getString("material") : null;

        // Se "material" parecer IA e o IA estiver habilitado → trata como IA; Material vira AIR (placeholder)
        Material core  = (looksLikeIaId(coreMatRaw)  && iaEnabled) ? Material.AIR : matchOrDefault(coreMatRaw,  Material.ENCHANTING_TABLE);
        Material altar = (looksLikeIaId(altarMatRaw) && iaEnabled) ? Material.AIR : matchOrDefault(altarMatRaw, Material.POLISHED_BLACKSTONE);

        // Derivados do material (caso ele seja IA)
        String coreIaFromMat  = (iaEnabled && looksLikeIaId(coreMatRaw))  ? normalizeIaId(coreMatRaw)  : null;
        String altarIaFromMat = (iaEnabled && looksLikeIaId(altarMatRaw)) ? normalizeIaId(altarMatRaw) : null;

        // Back-compat: custom_block tem prioridade sobre material
        String coreIaId  = normalizeIaId((coreSec  != null) ? coreSec.getString("custom_block", "")  : "");
        String altarIaId = normalizeIaId((altarSec != null) ? altarSec.getString("custom_block", "") : "");
        if (coreIaId == null)  coreIaId  = coreIaFromMat;
        if (altarIaId == null) altarIaId = altarIaFromMat;

        // stand
        boolean small   = standSec != null && standSec.getBoolean("small",  true);
        boolean marker  = standSec != null && standSec.getBoolean("marker", true);
        double  yOff    = (standSec != null) ? standSec.getDouble("y_offset", 0.62) : 0.62;
        double  backOff = (standSec != null) ? standSec.getDouble("radial_back_offset", 0.08) : 0.08;

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

        // ---- Offsets em int[] (relativos ao núcleo) ----
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
        } else {
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
        }

        // Monta a config final
        StructureConfig cfg = new StructureConfig(
                core, altar, yOff, small, marker,
                offs, backOff, allow,
                iaEnabled, allowIa,
                coreIaId, altarIaId,
                spinEnabled, spinStep, spinPeriod, spinMode,
                effectsEnabled, fxAssemble, fxDisassemble, fxPut, fxTake
        );

        // Offsets de pilares como Vector:
        // - se veio grade, calcula pela grade (A como referência)
        // - se não veio grade, usa os próprios offsets dx,dy,dz
        if (!grid.isEmpty()) {
            cfg.pillarOffsets = computePillarOffsets(grid);
        } else {
            List<Vector> vs = new ArrayList<>(offs.size());
            for (int[] o : offs) vs.add(new Vector(o[0], o[1], o[2]));
            cfg.pillarOffsets = Collections.unmodifiableList(vs);
        }

        return cfg;
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

    // Converte grade de 7x7 (ou N x M) em offsets Vector relativos ao 'A'
    private static List<Vector> computePillarOffsets(List<String> lines) {
        if (lines == null || lines.isEmpty()) return Collections.emptyList();

        // normaliza: remove espaços e mede o maior comprimento
        List<String> grid = new ArrayList<>(lines.size());
        int maxLen = 0;
        for (String raw : lines) {
            String s = raw == null ? "" : raw.replace(" ", "");
            grid.add(s);
            if (s.length() > maxLen) maxLen = s.length();
        }
        if (maxLen == 0) return Collections.emptyList();

        // encontra o núcleo (A); se não achar, usa o centro da grade
        int coreRow = -1, coreCol = -1;
        for (int r = 0; r < grid.size(); r++) {
            String row = grid.get(r);
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                if (ch == 'A' || ch == 'a') { coreRow = r; coreCol = c; break; }
            }
            if (coreRow != -1) break;
        }
        if (coreRow == -1 || coreCol == -1) {
            coreRow = grid.size() / 2;
            coreCol = maxLen / 2;
        }

        // Mapeia 'P' → Vector(dx, 0, dz). Convenções: linhas N→S, colunas W→E; Bukkit: +X=E, +Z=S
        List<Vector> out = new ArrayList<>();
        for (int r = 0; r < grid.size(); r++) {
            String row = grid.get(r);
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                if (ch == 'P' || ch == 'p') {
                    int dx = c - coreCol;
                    int dz = r - coreRow;
                    out.add(new Vector(dx, 0, dz));
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static boolean looksLikeIaId(String s) {
        return s != null && s.contains(":");
    }

    private static String normalizeIaId(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toLowerCase(Locale.ROOT);
    }
}
