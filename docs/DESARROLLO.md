# Guía técnica de Dragon Block

Cómo está organizado el código y qué tocar para añadir cosas. Todo está en `src/main/java/com/dragonblock`.

## Flujo general

```
Cliente (teclas)  --C2SInputStatePacket / C2SActionPacket-->  Servidor (valida y simula)
Servidor          --S2CSyncDBCDataPacket (a ti y a quien te ve)-->  Clientes (render, HUD)
Servidor          --S2CAnimPacket (ráfaga, onda, impulso)-->  Clientes (animaciones de un golpe)
```

El servidor es la única autoridad: el cliente solo dice qué teclas mantiene. Costes de Ki, cooldowns, requisitos y daño se comprueban en el servidor.

## Datos del jugador — `common/data`

| Archivo | Qué contiene |
| :--- | :--- |
| `PlayerDBCData` | Raza, atributos, Ki, **cuerpo (vida)**, TP, forma activa/seleccionada, maestría por forma, estado de teclas. Se guarda en el NBT persistente del jugador. |
| `Race` | Multiplicadores por raza y su rasgo visual (cola, antenas, cuernos, tentáculo). |
| `Transformation` | Todas las formas: padre, razas, nivel mínimo, multiplicador, consumo, color de aura, peinado, color de pelo, estilo de aura, pelaje. |

**Ojo:** en servidor `PlayerDBCData.get()` devuelve un objeto cacheado; tras modificarlo llama a `saveToPlayer()` y, si el cambio se tiene que ver, a `ModMessages.syncToTracking()`.

### Vida (cuerpo)
La vida real es `body / getMaxBody()`. La vida vanilla solo la refleja (`DragonBlockEvents.syncBodyWithVanilla`):
- `LivingDamageEvent` (prioridad más baja) resta el daño del cuerpo y ajusta el daño vanilla para que los corazones muestren el porcentaje.
- Cada tick, cualquier cambio vanilla que no hicimos nosotros (comida, pociones, `/kill`) se suma o resta al cuerpo.
- Al transformarte cambia `maxBody`; se conserva el porcentaje.

## Añadir una transformación
1. Añade una entrada en `Transformation` con su padre, razas y valores.
2. Si cambia de tamaño, añádela a `getScale()`; si tiene pelaje, dale `furColor`.
3. Listo: aparece en la pestaña Formas, en `/dev form` y en el render.

## Esencia de cada forma
Todo lo que distingue a una forma vive en `Transformation` (visual) y `FormEffects` (mecánica):
- `Transformation`: `getSequence()` (cómo se transforma), `getParticles()`, `getLightningColor()`, `getEyeColor()`, `getBulk()` (músculo), `getAuraWidth/Height()`, `getBaseTransformSeconds()`, `canFly()`, `getScale()`.
- `common/combat/FormEffects`: multiplicadores de golpe/ki/coste/daño recibido/velocidad, esquiva de Instinto Superior, furia de Ultra Ego, críticos de Bestia, regeneración de las formas divinas.

## Lógica de servidor — `common/combat`
- `FormController`: mantener `G` carga la forma seleccionada, consumo de Ki con maestría, drenaje de vida del Kaio-ken, Ozaru con luna llena / reversión de día.
- `KiAttacks`: técnicas (carga mientras se mantiene `X`, disparo al soltar o en ráfaga si es instantánea, recargas por técnica) e impulso. Cada disparo envía `S2CAnimPacket.TECHNIQUE` con el ordinal de la técnica para que todos vean la pose de soltar.
- `common/event/DragonBlockEvents`: tick del jugador, daño, Zenkai, vuelo, atributos (escala, velocidad).

## Render — `client`
| Archivo | Qué hace |
| :--- | :--- |
| `model/DBModels` | Geometría en código: 6 peinados, colas, antenas, cuernos, tentáculo, pelaje. |
| `renderer/DBPlayerLayer` | Dibuja rasgos de raza, pelo, pelaje y aura sobre cada jugador. |
| `renderer/EnergyRender` | Geometría aditiva: aura (capas, lenguas, chispas, anillo), rayos, esferas, haces. `auraSize()` define cómo crece con el poder. |
| `renderer/KiBlastRenderer` | Dibuja cada proyectil según su técnica: esfera, onda unida a las manos (`KiBlastEntity.origin`), espiral, disco, esfera colosal. |
| `renderer/RaceSkins` | Compone la textura del cuerpo de raza (capas de `textures/skin` teñidas con la apariencia) y la pone en la entrada de la lista de jugadores (`PlayerInfo`), así todos los renders la usan. |
| `fx/TechniqueFx` | Energía de carga en las manos según la postura y efectos de soltar que no son proyectiles (Kiai, Taiyoken). Lo usan la tercera y la primera persona. |
| `fx/LightningStrikes` | Rayos que caen del cielo al transformarse en las formas altas (`tier()` decide cuántos). |
| `fx/CinematicCamera` | Cámara que orbita al jugador local durante las transformaciones fuertes (`worthy()`), moviendo `Camera` en `ComputeCameraAngles`; franjas de cine en `DBCKiOverlay`. |
| `overlay/TechniqueBar` | Barra de técnicas de la derecha (Ctrl + rueda), carga y recarga. |
| `anim/AnimState` | Decide la pose de cada jugador a partir de los datos sincronizados y de los `S2CAnimPacket`. |
| `anim/DBPlayerModel` | Modelo de jugador que aplica las poses después de la animación vanilla. Se instala en todos los `PlayerRenderer` al arrancar (`ClientModEvents.swapModel`). |
| `ClientRenderEvents` | Inclinación del cuerpo al volar, corrección del FOV, brazos en primera persona. |
| `overlay/DBCKiOverlay` | HUD superior y barra de vida sobre los corazones. |
| `gui/DBCCharacterScreen` | Creador de personaje y ficha: atributos, formas, técnicas, apariencia. |
| `gui/UiTheme` | Paleta (azul noche, naranja, dorado) y encabezado con la esfera de 4 estrellas. |

### Añadir una animación
1. Si es de un golpe: añade un valor a `S2CAnimPacket.Anim` y llama a `ModMessages.broadcastAnim()` desde el servidor.
2. Añade la pose en `AnimState.Pose` y su prioridad en `AnimState.pose()`.
3. Tercera y primera persona: un `case` en `DBPlayerModel.applyPose` (con `firstPerson` para la variante de primera persona). Las poses se mezclan solas: `setupAnim` guarda los ángulos suavizados de cada jugador y un peso que entra y sale sobre la animación vanilla, así que no hay que interpolar a mano.

### Añadir una técnica
1. Un valor en `common/data/Technique` (tipo, postura, color, carga, coste, daño, TP, nivel, recarga, duración de la pose de soltar).
2. Si el tipo es nuevo, su efecto en `KiAttacks.fire` y su dibujo en `KiBlastRenderer` (o en `TechniqueFx.renderRelease` si no viaja).
3. Si la postura es nueva, su pose de carga en `DBPlayerModel.charge`, la de soltar en `release`, y dónde se reúne la energía en `TechniqueFx.renderCharge`.
4. Su icono en `tools/gen_technique_icons.py` (mismo orden que el enum) y regenera `textures/gui/techniques.png`.

## Pelo (JSON + vista previa en Blender)
Los peinados están en `src/main/resources/assets/dragonblock/hair/<estilo>.json` y los genera `tools/gen_hair_json.py` (los campos están explicados ahí). `DBModels` los lee al arrancar. Para verlos sin abrir el juego:
```
python tools/gen_hair_json.py src/main/resources/assets/dragonblock/hair
blender -b --factory-startup --python tools/preview_hair.py -- src/main/resources/assets/dragonblock/hair previews
python tools/hair_sheet.py previews
```
`preview_hair.py` monta los mechones igual que el juego (misma rotación ZYX y escalones), así que lo que ves es lo que sale.

## Cabeza del Ozaru
`src/main/resources/assets/dragonblock/ape_head.json` define la cabeza de simio por grupos de cajas (`fur`, `skin`, `dark`, `fangs`, `eyes`), cada uno teñido aparte. Vista previa:
```
blender -b --factory-startup --python tools/preview_ape.py -- src/main/resources/assets/dragonblock/ape_head.json previews
```

## Escombros de transformación — `client/fx/Debris`
Rocas redondeadas (octaedro subdividido con radios aleatorios, textura del bloque del suelo) que flotan alrededor de quien se transforma y salen despedidos al completar (`Debris.blast`, llamado al recibir el `S2CAnimPacket.TRANSFORM`). Son solo visuales: cada cliente los genera a partir del estado sincronizado. Se dibujan en `AFTER_PARTICLES` con un `PoseStack` vacío.

> **Ojo con `RenderLevelStageEvent`**: la rotación de la cámara ya está en la pila model-view de `RenderSystem`. En `AFTER_ENTITIES` Forge además pasa esa misma rotación en `getPoseStack()`; aplicarla otra vez gira todo dos veces y lo dibujado solo se ve mirando hacia un lado (era el bug de los escombros). Dibuja en coordenadas relativas a la cámara con un `PoseStack` nuevo.

## Brazos en primera persona
Forge no tiene un evento para cambiar la animación de los brazos, así que `ClientRenderEvents.onRenderHand` cancela las manos vanilla y dibuja los brazos de nuestro `DBPlayerModel` con `firstPerson = true`. Como un brazo de Minecraft no tiene codo, desde el hombro real nunca entraría en la vista: los hombros se colocan delante de la cámara (`FP_SHOULDER_*`, como la mano vanilla). Las poses de primera persona se ajustaron con `tools/preview_poses.py`, que renderiza la vista desde los ojos con FOV 70.

## Efectos de cliente — `client/fx/TransformFx`
Potencia de aura suavizada (se hincha al cargar, se apaga al soltar), partículas por forma, temblor de cámara de las transformaciones grandes. El estallido y la columna de luz se dibujan en `DBPlayerLayer.renderEnergy`; el destello de pantalla en `DBCKiOverlay`. La tecla `J` cambia `ClientConfig.AURA` (config de cliente `dragonblock-client.toml`).

## Texturas
Las texturas de `textures/entity` son en escala de grises y se tiñen en el juego (pelo, pelaje, rasgos, aura, ki). Para cambiar el color de una forma edita `Transformation`, no la textura.

## Cuerpos de raza
`tools/gen_race_skins.py` pinta, por raza, capas de 64x64 con el formato de skin de Minecraft: `base` (piel, gris que se multiplica por el tono), `accent` (shorts, gi, manchas, gemas), `detail` (colores fijos), `brows` y `scalp` (color de pelo) y `eyes.png` (iris). `RaceSkins` las compone con los colores de `PlayerDBCData` (paletas en `common/data/Appearance`) igual que la vista previa del script. Para verlas en 3D en Blender: `tools/preview_skin_blender.py` (define `SKIN` con la ruta de un PNG compuesto, por ejemplo los `sample_*.png` que genera el script con un segundo argumento).

Los peinados base (*warrior*, *flame*, *scruffy*, *short*, *long*) están en `gen_hair_json.py` junto a los de forma. Los ojos de forma de `DBPlayerLayer.renderEyes` están alineados a los iris de esas caras (fila 5, columnas 2 y 5).

## Escala de poder y choques
`Technique.powerScale(data)` (1 en base, ~2,3 en las formas más altas) multiplica el tamaño de proyectiles, explosiones (tope 8) y la energía de carga. `KiBlastEntity` sincroniza `HALO` (color del aura del tirador) y `CLASH` (ticks de choque): dos técnicas que se tocan se congelan `CLASH_TICKS`, los dueños que mantienen la tecla suman `clashBoost`, y la de id menor resuelve (`resolveClash`). El oscurecimiento del cielo (`TransformFx.darkness`) es una cúpula translúcida dibujada después del mundo con test de profundidad más el color de la niebla.

## Accesorios
`common/item/AccessoryItem` (casilla + texto del efecto), `common/menu/AccessoryMenu` (4 casillas respaldadas por `PlayerDBCData.accessories`, que guarda ids de objeto y se sincroniza a todos) y `client/gui/AccessoryScreen`. Los efectos se aplican donde se calcula cada cosa (`DragonBlockEvents`: TP, regeneración de ki, velocidad). El scouter: `client/overlay/ScouterHud` (lente, lectura, flecha) y `ScouterItem.tickWorn` en el servidor decide la sobrecarga (`S2CAnimPacket.SCOUTER_BREAK`). Los modelos que se ven puestos están en `DBModels` (`SCOUTER`, `WRISTBANDS`, `BELT`) y se dibujan en `DBPlayerLayer.renderAccessories`.

## Esferas y Dragón Eterno
- `common/block/DragonBallBlock` + `DragonBallTracker` (SavedData con la posición de cada esfera colocada, para el radar).
- `common/entity/ShenlongEntity`: estados `RISING` → `WAITING` → `LEAVING` con sus tiempos, deseos (`Wish`), dispersión de esferas (una por tick, genera el chunk si hace falta). No se guarda en el mundo.
- `client/renderer/ShenlongRenderer`: el ritual en pantalla. El cuerpo sigue `path()` (hélice); cada segmento se orienta hacia el siguiente; la cabeza mira al invocador. `path`, `SEGMENTS` y `SCALE` deben coincidir con `tools/preview_dragon_blender.py`.
- `client/gui/WishScreen` + `S2COpenWishPacket` / `C2SWishPacket` (el servidor valida quién pide y en qué estado).
- Cielo oscuro y rayos del ritual en `TransformFx` (`LightningStrikes.strikeAt`).

## Prueba automática del ritual
`client/DevAutotest` solo se activa con `-Ddragonblock.autotest=dragon`: abre (o crea) el mundo `autotest`, invoca al dragón de noche, pide un deseo y guarda capturas de cada fase en `run/screenshots/autotest_*.png`, y cierra el juego. Desde bash:
```
JAVA_TOOL_OPTIONS="-Ddragonblock.autotest=dragon" ./gradlew.bat runClient
```
Ojo: los `ModelPart` ya dividen sus medidas entre 16; la escala del `PoseStack` va en bloques por unidad de modelo (por eso `ShenlongRenderer.SCALE` es 5 y no 5/16).

## NPCs, diálogos y misiones
`common/entity/NpcEntity` + `common/npc` (`NpcKind`, `Dialogues` con sesiones validadas en el servidor, `NpcDialogues` con el contenido). Cada opción es una acción de servidor que **vuelve a comprobar el estado** al ejecutarse. Skins y detalles 3D: `tools/gen_npc_assets.py` → `textures/entity/npc` y `npc_extras.json` (render en `NpcRenderer`).

## Mundos
`common/world/Worlds` construye en código los lugares la primera vez que se visita cada mundo (bandera en SavedData): casa del maestro y laboratorio junto al spawn, plaza del juez, planeta celeste. Dimensiones en `data/dragonblock/dimension*` y `worldgen/biome`; Namek usa el ruido del overworld con bioma fijo y árbol propio (`worldgen/configured_feature/namek_tree.json`). `WorldEvents`: muerte → Otro Mundo, gravedad del planeta celeste, conteo de la prueba. `Planet` lista los destinos de la nave y dónde aterrizar.

## Nave
`SpaceshipEntity` (IDLE → LIFTOFF → salto entre dimensiones creando una nave nueva en el destino → LANDING), `TravelScreen`, `WarpOverlay`, modelo `ship.json` (`tools/gen_ship_json.py`, vista previa con `tools/preview_box_model.py`). Los pasajeros nunca acumulan caída.

## Enemigos
`DBEnemy` + `EnemyKind` (stats), `EnemyRenderer` (modelo humanoide, skins y equipo de `tools/gen_enemy_assets.py`). Los objetivos de IA se añaden al final del constructor: `Mob` llama a `registerGoals()` antes de que la subclase guarde su tipo. Apariciones: `data/dragonblock/forge/biome_modifier/bandits.json` y `spawners` del bioma de Namek.

## Pruebas automáticas
`DevAutotest` con `-Ddragonblock.autotest=<modo>`: `dragon` (ritual), `world` (maestro, muerte, juez, planeta celeste, volver a la vida), `ship` (viaje a otro planeta), `enemies` (arena en el cielo, combate y transformación del emperador). Guarda capturas en `run/screenshots/autotest_*.png`.

## Probar
```
./gradlew.bat runClient
/dev max
/dev mastery 100
/dev form <forma>
```
