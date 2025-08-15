[README.md](https://github.com/user-attachments/files/21798476/README.md)
# MysticCraft

Mesa de magia **multibloco** para servidores Paper.  
Suporta itens vanilla e **ItemsAdder** (itens e *custom blocks*), com animações, efeitos, proteção da estrutura e persistência entre reinícios.

> Feito à moda antiga: simples, robusto, fácil de manter. 🪄

---

## Sumário
- [Requisitos](#requisitos)
- [Instalação](#instalação)
- [Compilação](#compilação)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Como Funciona](#como-funciona)
- [Configuração](#configuração)
- [Comandos & Permissões](#comandos--permissões)
- [Integrações](#integrações)
- [Recursos](#recursos)
- [Solução de Problemas](#solução-de-problemas)
- [Roadmap](#roadmap)
- [Contribuindo](#contribuindo)
- [Licença](#licença)

---

## Requisitos
- **Servidor**: Paper **1.21.8**
- **Java**: **17**
- **Build**: Maven
- **Opcional**: ItemsAdder, PlaceholderAPI, Vault, MythicMobs, ModelEngine

---

## Instalação
1. Baixe/compile `mysticcraft-1.0.0-SNAPSHOT.jar`.
2. Coloque em `server/plugins/`.
3. Inicie o servidor para gerar `config.yml`.
4. Ajuste `config.yml` (especialmente se for usar ItemsAdder).
5. Use `/mystic reload` para aplicar mudanças.

---

## Compilação
```bash
# dentro do diretório do projeto
mvn clean package
# artefato gerado em:
# target/mysticcraft-1.0.0-SNAPSHOT.jar
```

**Dica IntelliJ**  
Marque `src/main/resources` como *Resources Root* (para embutir `plugin.yml`/`config.yml` no jar).

---

## Estrutura do Projeto
```
src/main/java/br/com/novaera/mysticraft/
  MysticCraft.java                     # plugin principal

  config/
    StructureConfig.java               # carrega config.yml (grade, whitelist, spin, efeitos, IA)

  core/
    StructureManager.java              # detecção/marcação (PDC), spawn de stands, helpers
    StructureService.java              # montar/desmontar, persistência, rebuild, posição dos stands
    StructureStorage.java              # armazenamento dos núcleos e itens por altar

  integrations/
    ItemsAdderCompat.java              # CustomStack (itens) e CustomBlock (blocos) do IA

  util/
    Keys.java                          # NamespacedKeys (CORE, ALTAR, ALTAR_STAND)
    PdcBlocks.java                     # utilitários PDC em blocos

  listener/
    CorePlaceListener.java             # coloca núcleo → tenta montar
    CoreBreakListener.java             # quebra núcleo → desmonta
    AltarPlaceListener.java            # coloca pilar → tenta completar e montar
    AltarBreakListener.java            # quebra pilar → desmonta mesa do núcleo dono
    AltarInteractListener.java         # coloca/retira item (vanilla/IA), whitelist, efeitos
    ProtectionListener.java            # cancela explosões/pistões/fluidos na estrutura

  command/
    MysticCommand.java                 # /mystic reload

  task/
    SpinTask.java                      # anima a rotação do item (HEAD_POSE ou YAW)

  i18n/
    Lang.java                          # mensagens (CHAT/ACTION_BAR/OFF)
```
---

## Como Funciona
- A mesa é uma **estrutura multibloco** definida por uma **grade** no `config.yml`.
- `A` = **núcleo** (ex.: ENCHANTING_TABLE ou Custom Block IA)  
  `P` = **altares** (pilares)  
  `V` = vazio  
- Ao **colocar** o núcleo/altar, o plugin tenta **montar** a mesa (só funciona 100% montada).
- Clicar com **item** no altar coloca 1 unidade como “capacete” do ArmorStand do altar.  
  Mão **vazia** ou **shift** retira o item.
- **Persistência**: tudo volta após restart (estrutura e itens sobre os altares).
- **Proteções**: TNT, pistões e fluidos não afetam a mesa.

---

## Configuração

### `plugin.yml` (embutido)
```yaml
name: MysticCraft
version: 1.0.0
main: br.com.novaera.mysticraft.MysticCraft
api-version: '1.21'
softdepend: [PlaceholderAPI, Vault, MythicMobs, ModelEngine, ItemsAdder]
commands:
  mystic:
    description: Comandos do MysticCraft
    usage: /mystic reload
    permission: mystic.admin
    aliases: [mysticcraft]

permissions:
  mystic.admin:
    default: op
  mystic.use:
    default: true
```

### `config.yml` (exemplo)
```yaml
mysticcraft:
  messages:
    # CHAT | ACTION_BAR | OFF
    put_take: ACTION_BAR

  effects:
    enabled: true
    assemble:    { particle: ENCHANTMENT_TABLE, count: 60, offset: 0.50, speed: 0.01, sound: BLOCK_ENCHANTMENT_TABLE_USE, volume: 1.0, pitch: 1.2 }
    disassemble: { particle: CLOUD,              count: 40, offset: 0.40, speed: 0.00, sound: BLOCK_GLASS_BREAK,           volume: 0.9, pitch: 1.0 }
    put:         { particle: END_ROD,            count: 12, offset: 0.05, speed: 0.00, sound: ENTITY_EXPERIENCE_ORB_PICKUP, volume: 0.6, pitch: 1.5 }
    take:        { particle: CLOUD,              count: 10, offset: 0.10, speed: 0.00, sound: UI_BUTTON_CLICK,             volume: 0.5, pitch: 1.8 }

  integrations:
    itemsadder:
      enabled: true

  core:
    material: ENCHANTING_TABLE          # fallback vanilla
    custom_block: ''                    # ex.: mypack:core_block (vazio = não usar IA)

  altar:
    material: POLISHED_BLACKSTONE       # fallback vanilla
    custom_block: ''                    # ex.: mypack:altar_pillar (IA)
    # whitelist de itens (vanilla). vazio = todos
    allowed_items:
      - BLAZE_ROD
      - DIAMOND
    # whitelist de itens IA (namespace:id). vazio = todos
    allowed_custom:
      - mypack:orb_arcano

    stand:
      small: true
      marker: true
      y_offset: 0.30
      radial_back_offset: 0.08  # empurra o stand para centralizar o item 3D
      spin:
        enabled: true
        deg_per_step: 2.0
        period_ticks: 1
        mode: HEAD_POSE          # HEAD_POSE (recomendado) ou YAW

  area:
    size: { width: 7, length: 7, height: 1 }

  # Grade 7x7 (N→S linhas, O→L colunas)
  altars:
    - "VVVPVVV"
    - "VPVVVPV"
    - "VVVVVVV"
    - "PVVAVVP"
    - "VVVVVVV"
    - "VPVVVPV"
    - "VVVPVVV"
```

---

## Comandos & Permissões

**Comandos**
- `/mystic reload` — recarrega `config.yml`, revalida mesas, reposiciona stands e reinicia animação.

**Permissões**
- `mystic.admin` — acesso ao `/mystic reload` (padrão: OP).
- `mystic.use` — uso geral (padrão: true).

---

## Integrações
- **ItemsAdder**
  - **Itens** no altar: whitelist por `allowed_custom` (`namespace:id`).
  - **Blocos** do núcleo/altar: `core.custom_block` e `altar.custom_block` (fallback para `material`).
- **PlaceholderAPI / Vault / MythicMobs / ModelEngine**
  - Listadas em `softdepend`, preparadas para futura integração (economia, mobs, modelos, placeholders).

---

## Recursos
- Estrutura **multibloco** configurável por **grade**.
- **Persistência** completa (estrutura + itens).
- **Proteção** contra explosões, pistões e fluidos.
- **Animação** do item do altar (gira suave).
- **Mensagens** customizáveis (CHAT / ACTION_BAR / OFF).
- **Whitelist** de itens (vanilla e IA).
- Compatível com **Custom Blocks** do ItemsAdder.

---

## Solução de Problemas

- **“illegal start of expression”**  
  Quase sempre é **chave não fechada** no `StructureConfig.java` (em especial no `if (!grid.isEmpty()) { … }`).

- **Método duplicado (`isCore`/`isAltar`)**  
  Garanta que só existem os métodos que usam `matchesCoreBlock/AltarBlock`.

- **`/mystic` não funciona**  
  Confira `plugin.yml` (comando e permissões) e registro do `CommandExecutor`.

- **Paper não deleta `.paper-remapped`**  
  Provável servidor/console duplicado. Feche instâncias antes de trocar o `.jar`.

- **ItemsAdder ausente**  
  A integração desliga sozinha; comportamento volta ao **vanilla**.

---

## Roadmap
- [ ] **MythicMobs / ModelEngine**: efeitos e entidades ao concluir rituais.
- [ ] **GUI** da mesa (inventário custom para receitas).
- [ ] **Múltiplas mesas** por mundo com estados independentes.
- [ ] **Permissões finas** (`mystic.use.place`, `mystic.use.take`, etc.).
- [ ] **Economia (Vault)**: custo de colocar/retirar item.

---

## Contribuindo
1) Faça um fork  
2) Crie sua branch: `git checkout -b feat/minha-feature`  
3) Commit: `git commit -m "feat: minha feature"`  
4) Push: `git push origin feat/minha-feature`  
5) Abra um Pull Request

Sugestão de `.gitignore`:
```
target/
.idea/
*.iml
*.log
.DS_Store
```

---

## Licença
Escolha a licença que preferir (ex.: **MIT**).
