# Architectury Multi-Module Migration Plan

**Goal:** Collapse `main` (NeoForge 1.21) and `origin/fabric-1.21.1` (Fabric 1.21.1) into a single Architectury multi-module Gradle project that builds both loader targets from one codebase, with all tests verifying both still work.

---

## Branching Strategy

All work lands on the feature branch **`architectury-multimodule`**, never directly on `main`.

Each phase below is implemented on its own short-lived branch and merged into `architectury-multimodule` via a PR (pending approval). The PR branch naming convention is:

```
arch/<phase-number>-<short-description>
```

For example:
- `arch/1-gradle-build-system`
- `arch/2-directory-scaffolding`
- `arch/3-service-interfaces`
- ...and so on through Phase 9

Once all phases are merged and the full suite passes on `architectury-multimodule`, a final PR merges the feature branch into `main`.

---

## Resulting Module Layout

```
redstonepen/
├── common/    ← all game logic, blocks, items, libmc utilities, unit tests, GameTest logic
├── neoforge/  ← NeoForge entry point, event bus wiring, NeoForge GameTest wrappers, JaCoCo
└── fabric/    ← Fabric ModInitializer, client initializer, mixins, Fabric GameTest registration
```

All three modules use `dev.architectury.loom` as their Gradle plugin. The `common` module has no NeoForge or Fabric runtime dependency — only `fabric-loader` is on the common *compile* classpath so `@Environment`/`EnvType` annotations are available.

---

## Platform Service Abstraction (4 Seams)

Loader-specific behavior is isolated behind Java `ServiceLoader` interfaces defined in `common/`. Each platform module provides one implementation per interface, registered via `META-INF/services/<interface-FQN>` files.

| Interface | NeoForge impl | Fabric impl | What it abstracts |
|---|---|---|---|
| `libmc/INetworkingPlatform` | `NetworkingPlatformNeoForge` | `NetworkingPlatformFabric` | Payload registration, server→client sends |
| `libmc/INetworkingClientPlatform` | `NetworkingClientPlatformNeoForge` | `NetworkingClientPlatformFabric` | Client receiver registration, sendToServer |
| `libmc/IRenderingPlatform` | `RenderingPlatformNeoForge` | `RenderingPlatformFabric` | Block render layers, additional model registration |
| `libmc/IPlatformHelper` | `PlatformHelperNeoForge` | `PlatformHelperFabric` | `getGameDirectory`, `isModLoaded`, `getFakePlayer` |

`PlatformServices.java` in `common/libmc/` uses `ServiceLoader` to load each; `NETWORKING_CLIENT` and `RENDERING` are loaded lazily (first access only) to prevent client-only implementations from being instantiated on dedicated servers.

---

## Phase 1 — Rebuild the Gradle Build System

### 1.1 Replace `settings.gradle`
```groovy
pluginManagement {
  repositories {
    mavenLocal(); gradlePluginPortal()
    maven { url = 'https://maven.neoforged.net/releases' }
    maven { url = 'https://maven.architectury.dev/' }
    maven { url = 'https://maven.fabricmc.net/' }
  }
}
plugins { id 'org.gradle.toolchains.foojay-resolver-convention' version '0.8.0' }
rootProject.name = 'redstonepen'
include('common', 'neoforge', 'fabric')
```

### 1.2 Replace root `build.gradle`
Apply `architectury-plugin` (v3.4.161) and `dev.architectury.loom` (v1.7.415, `apply false`) at root. Configure Java 21 toolchain and shared Maven repositories in `subprojects {}`.

### 1.3 Update `gradle.properties`
Add:
```properties
fabric_loader_version=0.16.10
fabric_api_version=0.114.0+1.21.1
architectury_version=13.0.8
parchment_mc_version=1.20.6
parchment_mappings_version=2024.05.01
```
Remove `neogradle.*` subsystem entries (only valid inside `neoforge/build.gradle`).

### 1.4 Create `common/build.gradle`
- `architectury { common() }`
- Dependencies: `minecraft`, `officialMojangMappings()`, `modCompileOnly fabric-loader` (for `@Environment`/`EnvType`), JUnit 5
- `test { useJUnitPlatform() }`, jacoco exec → `build/jacoco/test.exec`

### 1.5 Create `neoforge/build.gradle`
- `architectury { neoForge() }`
- Dependencies: NeoForge, `common(project(':common', 'namedElements'))`, `shadowCommon(project(':common', 'transformProductionNeoForge'))`, JUnit 5
- `runs {}` block with client/server/gameTestServer configs
- JaCoCo offline instrumentation (copy from existing `build.gradle` lines 85–147, update paths)
- `shadowJar` + `remapJar` producing fat JAR (embeds common classes)
- `coverage` task: `dependsOn ':common:test', 'runGameTestServer'`
- `jacocoTestReport` merges `neoforge/build/jacoco/*.exec` + `common/build/jacoco/test.exec`

### 1.6 Create `fabric/build.gradle`
- `architectury { fabric() }`
- Dependencies: Fabric loader, Fabric API, `fabric-gametest-api-v1`, `common(:common namedElements)`, `shadowCommon(:common transformProductionFabric)`
- `shadowJar` + `remapJar` for fat JAR
- `prepareGameTestStructures` task copies from `rootProject.file('common/gameteststructures')`

**Checkpoint:** `./gradlew projects` lists all three modules.

---

## Phase 2 — Create Directory Scaffolding

```
common/src/main/java/wile/{redstonepen,api/rca}/
common/src/main/resources/META-INF/services/
common/src/main/java/wile/redstonepen/gametest/    ← shared GameTest logic
common/src/test/java/wile/redstonepen/             ← unit tests
common/gameteststructures/                          ← shared NBT structures (moved from root)

neoforge/src/main/java/wile/redstonepen/{libmc,detail,gametest,commands}/
neoforge/src/main/resources/META-INF/{services,}/

fabric/src/main/java/wile/redstonepen/{libmc,detail,mixin}/
fabric/src/main/resources/META-INF/services/
```

---

## Phase 3 — Define Platform Service Interfaces in `common/libmc/`

**`INetworkingPlatform.java`**
```java
public interface INetworkingPlatform {
  void registerPayloads();
  void sendToPlayer(ServerPlayer player, Networking.UnifiedPayload payload);
  void sendToAllPlayers(ServerLevel world, Networking.UnifiedPayload payload);
}
```

**`INetworkingClientPlatform.java`** (annotate `@Environment(EnvType.CLIENT)`)
```java
public interface INetworkingClientPlatform {
  void registerClientReceiver();
  void sendToServer(Networking.UnifiedPayload payload);
}
```

**`IRenderingPlatform.java`** (annotate `@Environment(EnvType.CLIENT)`)
```java
public interface IRenderingPlatform {
  void setRenderLayer(Block block, RenderType renderType);
  void registerAdditionalModels(List<ResourceLocation> models);
}
```

**`IPlatformHelper.java`**
```java
public interface IPlatformHelper {
  Path getGameDirectory();
  boolean isModLoaded(String modId);
  Optional<? extends Player> getFakePlayer(ServerLevel world);
}
```

**`PlatformServices.java`**
```java
public class PlatformServices {
  public static final INetworkingPlatform NETWORKING = load(INetworkingPlatform.class);
  public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
  // Lazy to avoid instantiating @OnlyIn(CLIENT) impls on the server
  private static INetworkingClientPlatform networkingClient;
  public static INetworkingClientPlatform getNetworkingClient() {
    if (networkingClient == null) networkingClient = load(INetworkingClientPlatform.class);
    return networkingClient;
  }
  private static <T> T load(Class<T> clazz) {
    return ServiceLoader.load(clazz).findFirst()
      .orElseThrow(() -> new RuntimeException("No service impl: " + clazz.getName()));
  }
}
```

---

## Phase 4 — Move and Refactor Common Sources

### Files moved to `common/` unchanged (pure Minecraft/Java)
`ModContent.java`, `ModConstants.java`, `blocks/` (all 6 files), `items/` (both files), `api/rca/` (both files), `libmc/Registries.java`, `RsSignals.java`, `Inventories.java`, `StandardEntityBlocks.java`, `ExtendedShapelessRecipe.java`, `Guis.java`, `GuiTextEditing.java`, `TooltipDisplay.java`, `commands/DemoBuilder.java`, `commands/DemoSections.java`, `detail/RcaSync.java`

### Files requiring edits on the way to `common/`

**`libmc/Auxiliaries.java`**
- `getGameDirectory()` body → `return PlatformServices.PLATFORM.getGameDirectory();`
- `isModLoaded()` body → `return PlatformServices.PLATFORM.isModLoaded(registry_name);`
- `getFakePlayer()` return type → `Optional<? extends Player>`, body → `return PlatformServices.PLATFORM.getFakePlayer((ServerLevel)world);`
- All `@OnlyIn(Dist.CLIENT)` → `@Environment(EnvType.CLIENT)`; swap NeoForge dist imports for Fabric api imports

**`libmc/Networking.java`**
- Extract `static void handleServerReceive(UnifiedPayload, ServerPlayer)` and `static void handleClientReceive(UnifiedPayload, LocalPlayer)` as package-private helpers from the existing switch bodies
- Remove `init(PayloadRegistrar)` → add `public static void init() { PlatformServices.NETWORKING.registerPayloads(); }`
- `sendToClient()` → delegates to `PlatformServices.NETWORKING.sendToPlayer(...)`
- `sendToClients()` → delegates to `PlatformServices.NETWORKING.sendToAllPlayers(...)`
- Remove all `PacketDistributor` imports
- Make `UnifiedPayload.STREAM_CODEC` `public static final`
- Annotate `handleClientReceive` with `@Environment(EnvType.CLIENT)`

**`libmc/NetworkingClient.java`**
- Remove all NeoForge imports; add `@Environment(EnvType.CLIENT)` at class level
- `clientInit()` becomes no-arg → `PlatformServices.getNetworkingClient().registerClientReceiver()`
- `send()` → `PlatformServices.getNetworkingClient().sendToServer(...)`

**`libmc/Overlay.java`**, **`libmc/StandardBlocks.java`**, **`libmc/StandardItems.java`**
- `@OnlyIn(Dist.CLIENT)` → `@Environment(EnvType.CLIENT)` only

**`commands/DemoCommand.java`**
- Remove `onRegisterCommands(RegisterCommandsEvent)` and its NeoForge import
- Keep `register(CommandDispatcher<CommandSourceStack>)` and `runDemo(...)` as public/private statics

**`detail/ModRenderers.java`**
- Remove NeoForge client event imports; keep `TrackTer` with `registerModels()` returning `List<ResourceLocation>`
- `@OnlyIn(Dist.CLIENT)` → `@Environment(EnvType.CLIENT)`

**All blocks/* with `@OnlyIn`** — `BasicButton`, `BasicLever`, `CircuitComponents`, `ControlBox`, `RedstoneTrack`: same annotation swap

**GameTests** — Move all 10 files to `common/src/main/java/wile/redstonepen/gametest/` with `@GameTestHolder` and `@PrefixGameTestTemplate` removed. The `@GameTest` annotation on each method is vanilla and stays.

### Unit tests
Copy all 11 files from `src/test/java/` → `common/src/test/java/` unchanged.

### Resources
- `src/main/resources/assets/` and `data/` → `common/src/main/resources/` (identical between branches)
- `gameteststructures/` → `common/gameteststructures/`

**Checkpoint:** `./gradlew :common:compileJava` clean; `./gradlew :common:test` — 11 unit tests green.

---

## Phase 5 — Implement NeoForge Platform Services

**`neoforge/src/main/java/wile/redstonepen/libmc/`**

**`NetworkingPlatformNeoForge.java`** — Holds a static `PayloadRegistrar` injected by `ModRedstonePen` during `RegisterPayloadHandlersEvent`. `registerPayloads()` calls `registrar.playBidirectional(...)` dispatching to `Networking.handleServerReceive` / `handleClientReceive`. `sendToPlayer()` via `PacketDistributor.sendToPlayer`. `sendToAllPlayers()` iterates `world.players()`.

**`NetworkingClientPlatformNeoForge.java`** (`@OnlyIn(Dist.CLIENT)`) — `registerClientReceiver()` is a no-op (NeoForge bidirectional handler covers both directions). `sendToServer()` via `PacketDistributor.sendToServer(payload)`.

**`RenderingPlatformNeoForge.java`** (`@OnlyIn(Dist.CLIENT)`) — `setRenderLayer()` is a no-op (NeoForge uses block's built-in render type hint). `registerAdditionalModels()` is a no-op (models registered inline from `ModelEvent.RegisterAdditional` in `ModRedstonePen`).

**`PlatformHelperNeoForge.java`** — `getGameDirectory()` via `FMLLoader.getGamePath()`; `isModLoaded()` via `ModList.get().isLoaded()`; `getFakePlayer()` via `FakePlayerFactory.getMinecraft(world)`.

**`neoforge/src/main/java/wile/redstonepen/ModRedstonePen.java`** — Refactored from current `ModRedstonePen.java`:
- `onRegisterNetwork`: calls `NetworkingPlatformNeoForge.setRegistrar(registrar)` then `Networking.init()`
- `onClientSetup`: calls `NetworkingClient.clientInit()` (no-arg)
- `onRegisterModels`: calls `ModRenderers.TrackTer.registerModels().forEach(event::register)`
- `onRegisterCommands`: calls `DemoCommand.register(event.getDispatcher())`

**NeoForge GameTest wrappers** — 10 empty final subclasses in `neoforge/src/main/java/wile/redstonepen/gametest/` that add `@GameTestHolder` and `@PrefixGameTestTemplate(false)`. NeoForge's scanner uses `getMethods()` (which includes inherited methods) so the test methods from the common parent class are discovered:
```java
@GameTestHolder(ModConstants.MODID)
@PrefixGameTestTemplate(false)
public final class RelayGameTests
    extends wile.redstonepen.gametest.RelayGameTests {}
```
One file per each of the 10 GameTest classes.

**NeoForge resources** — `META-INF/neoforge.mods.toml`, `redstonepen.mixins.json`, `pack.mcmeta` in `neoforge/src/main/resources/`. 4 `META-INF/services/` files pointing to NeoForge impl classes.

**`prepareGameTestStructures`** task copies from `rootProject.file('common/gameteststructures')`.

**Checkpoint:** `./gradlew :neoforge:compileJava` and `./gradlew :neoforge:build` succeed.

---

## Phase 6 — Implement Fabric Platform Services

**`fabric/src/main/java/wile/redstonepen/libmc/`**

**`NetworkingPlatformFabric.java`** — `registerPayloads()` registers both C2S and S2C via `PayloadTypeRegistry` and a `ServerPlayNetworking.GlobalReceiver` dispatching to `Networking.handleServerReceive`. `sendToPlayer()` via `ServerPlayNetworking.send`. `sendToAllPlayers()` iterates `world.players()`.

**`NetworkingClientPlatformFabric.java`** (`@Environment(EnvType.CLIENT)`) — `registerClientReceiver()` via `ClientPlayNetworking.registerGlobalReceiver`, dispatching to `Networking.handleClientReceive`. `sendToServer()` via `ClientPlayNetworking.send(payload)`.

**`RenderingPlatformFabric.java`** (`@Environment(EnvType.CLIENT)`) — `setRenderLayer()` via `BlockRenderLayerMap.INSTANCE.putBlock(block, renderType)`.

**`PlatformHelperFabric.java`** — `getGameDirectory()` via `FabricLoader.getInstance().getGameDir()`; `isModLoaded()` via `FabricLoader.getInstance().isModLoaded()`; `getFakePlayer()` via Fabric's `FakePlayer.get(world)`.

**`fabric/ModRedstonePen.java`** — Implements `ModInitializer`. Calls `Registries.init()`, `Networking.init()`, `ModContent.init()`, registers creative tab via `FabricItemGroup`, registers commands via `CommandRegistrationCallback`.

**`fabric/ModRedstonePenClient.java`** — Implements `ClientModInitializer`. Calls `NetworkingClient.clientInit()`, `PlatformServices.RENDERING.setRenderLayer(...)` for translucent/cutout blocks, registers `ModelLoadingPlugin` for TESR models, `WorldRenderEvents` and `ClientTickEvents` for overlay/RCA.

**`fabric/mixin/GuiRenderingMixin.java`** — Copied from fabric branch unchanged.

**Fabric GameTests** — The 10 common GameTest classes are registered directly as Fabric GameTest entry points in `fabric.mod.json`:
```json
"entrypoints": {
  "fabric-gametest": [
    "wile.redstonepen.gametest.BasicLeverButtonGameTests",
    "wile.redstonepen.gametest.TrackGameTests",
    "wile.redstonepen.gametest.ControlBoxGameTests",
    "wile.redstonepen.gametest.PenItemGameTests",
    "wile.redstonepen.gametest.RelayGameTests",
    "wile.redstonepen.gametest.RemoteItemGameTests",
    "wile.redstonepen.gametest.DemoGameTests",
    "wile.redstonepen.gametest.InventoriesGameTests",
    "wile.redstonepen.gametest.RecipeGameTests",
    "wile.redstonepen.gametest.AuxiliariesGameTests"
  ]
}
```
No wrapper classes needed — Fabric discovers tests via entry points, not `@GameTestHolder`. `fabric-gametest-api-v1` added to `fabric/build.gradle` dependencies.

**Fabric resources** — `fabric.mod.json`, `redstonepen.mixins.json`, `pack.mcmeta` from fabric branch; 4 `META-INF/services/` files pointing to Fabric impl classes.

**Checkpoint:** `./gradlew :fabric:compileJava` and `./gradlew :fabric:build` succeed.

---

## Phase 7 — Delete Old `src/` Tree

Once all three modules compile and tests pass, delete `src/` from root. The `gameteststructures/` directory was already moved to `common/` in Phase 4.

---

## Phase 8 — JaCoCo Coverage

`neoforge/build.gradle` `coverage` task: `dependsOn ':common:test', 'runGameTestServer'`.

`jacocoTestReport`:
- `executionData` merges `neoforge/build/jacoco/*.exec` + `project(':common').layout.buildDirectory.file('jacoco/test.exec')`
- `classDirectories` includes classes from both `:neoforge` and `:common`
- Exclusion list stays the same; add NeoForge service impl glue classes

---

## Phase 9 — Update CI

`.github/workflows/ci.yml`:
```yaml
- name: Unit tests (common)
  run: ./gradlew :common:test

- name: Build all modules
  run: ./gradlew build

- name: NeoForge GameTests
  run: ./gradlew :neoforge:runGameTestServer

- name: Fabric GameTests
  run: ./gradlew :fabric:runGameTestServer
```

---

## Files Modified Summary

| File | Change |
|---|---|
| `settings.gradle` | Add `include` + Architectury/Fabric plugin repos |
| `build.gradle` | Replace with `architectury-plugin` multi-module orchestrator |
| `gradle.properties` | Add Fabric/Architectury versions; remove `neogradle.*` subsystem entries |
| `libmc/Networking.java` | Extract receive helpers; delegate sends to `PlatformServices.NETWORKING`; no-arg `init()` |
| `libmc/Auxiliaries.java` | 3 methods delegate to `IPlatformHelper`; `@OnlyIn` → `@Environment` annotation swap |
| `libmc/NetworkingClient.java` | No-arg `clientInit()`; delegate `send()` to `INetworkingClientPlatform` |
| `libmc/Overlay.java`, `StandardBlocks.java`, `StandardItems.java` | Annotation swap only |
| `blocks/*.java` (5 files with `@OnlyIn`) | Annotation swap only |
| `commands/DemoCommand.java` | Remove NeoForge event registration method |
| `detail/ModRenderers.java` | Remove NeoForge event imports; annotation swap |
| `gametest/*.java` (10 files) | Remove `@GameTestHolder` and `@PrefixGameTestTemplate` |
| `.github/workflows/ci.yml` | Add `:common:test`, `:neoforge:runGameTestServer`, `:fabric:runGameTestServer` |

---

## Known Challenges

1. **Client-side service loading on server**: `PlatformServices.getNetworkingClient()` loads lazily to prevent `@OnlyIn(CLIENT)` implementations from being class-loaded on a dedicated server.

2. **`ModelResourceLocation` vs `ResourceLocation`** in `ModRenderers.TrackTer`: Architectury Loom common compilation uses Mojang mappings where only `ResourceLocation` overloads are available. Verify `:common:compileJava` succeeds. If `ModelResourceLocation` is required on NeoForge, expose `IRenderingPlatform.getModel(ModelManager, ResourceLocation)` to wrap it.

3. **`getFakePlayer` return type**: changes from `Optional<FakePlayer>` (NeoForge-specific) to `Optional<? extends Player>`. Existing GameTest assertions only call `.isPresent()` — no site changes needed.

4. **`UnifiedPayload.STREAM_CODEC`** must be `public static final` so NeoForge and Fabric platform implementations can reference it from outside the package.

5. **GameTest structure path format**: NeoForge (with `@PrefixGameTestTemplate(false)`) and Fabric both resolve test templates as `namespace:path`. The `@GameTest(templateNamespace = NS, template = ...)` values already encode both parts, so shared NBT files in `common/gameteststructures/` work for both loaders without modification.

---

## Verification Sequence

| Phase | Command | Expected |
|---|---|---|
| After Phase 1 | `./gradlew projects` | Lists common, neoforge, fabric |
| After Phase 4 | `./gradlew :common:compileJava` | Clean |
| After Phase 4 | `./gradlew :common:test` | 11 unit tests pass |
| After Phase 5 | `./gradlew :neoforge:build` | JAR produced |
| After Phase 6 | `./gradlew :fabric:build` | JAR produced |
| After Phase 7 | `./gradlew build` | All 3 JARs produced |
| After Phase 8 | `./gradlew :neoforge:runGameTestServer` | All 10 GameTest classes pass |
| After Phase 8 | `./gradlew :fabric:runGameTestServer` | All 10 GameTest classes pass |
| After Phase 8 | `./gradlew :neoforge:jacocoTestReport` | Report generated, ≥90% line coverage |
