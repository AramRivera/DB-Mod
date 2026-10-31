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
- `KiAttacks`: ráfaga, onda cargada, impulso. Cada técnica envía su `S2CAnimPacket`.
- `common/event/DragonBlockEvents`: tick del jugador, daño, Zenkai, vuelo, atributos (escala, velocidad).

## Render — `client`
| Archivo | Qué hace |
| :--- | :--- |
| `model/DBModels` | Geometría en código: 6 peinados, colas, antenas, cuernos, tentáculo, pelaje. |
| `renderer/DBPlayerLayer` | Dibuja rasgos de raza, pelo, pelaje y aura sobre cada jugador. |
| `renderer/EnergyRender` | Geometría aditiva: aura (capas, lenguas, chispas, anillo), rayos, esferas, haces. `auraSize()` define cómo crece con el poder. |
| `renderer/KiBlastRenderer` | Esfera y haz de los proyectiles. |
| `anim/AnimState` | Decide la pose de cada jugador a partir de los datos sincronizados y de los `S2CAnimPacket`. |
| `anim/DBPlayerModel` | Modelo de jugador que aplica las poses después de la animación vanilla. Se instala en todos los `PlayerRenderer` al arrancar (`ClientModEvents.swapModel`). |
| `ClientRenderEvents` | Inclinación del cuerpo al volar, corrección del FOV, brazos en primera persona. |
| `overlay/DBCKiOverlay` | HUD superior y barra de vida sobre los corazones. |
| `gui/DBCCharacterScreen` | Ficha: selección de raza, atributos, formas. |

### Añadir una animación
1. Si es de un golpe: añade un valor a `S2CAnimPacket.Anim` y llama a `ModMessages.broadcastAnim()` desde el servidor.
2. Añade la pose en `AnimState.Pose` y su prioridad en `AnimState.pose()`.
3. Tercera persona: un `case` en `DBPlayerModel.setupAnim`. Primera persona: un `case` en `ClientRenderEvents.onRenderHand`.

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
Trozos del bloque del suelo que flotan alrededor de quien se transforma y salen despedidos al completar (`Debris.blast`, llamado al recibir el `S2CAnimPacket.TRANSFORM`). Son solo visuales: cada cliente los genera a partir del estado sincronizado. Se dibujan en `RenderLevelStageEvent.AFTER_ENTITIES`.

## Brazos en primera persona
Forge no tiene un evento para cambiar la animación de los brazos, así que `ClientRenderEvents.onRenderHand` cancela las manos vanilla y dibuja los brazos de nuestro `DBPlayerModel` con `firstPerson = true`. Como un brazo de Minecraft no tiene codo, desde el hombro real nunca entraría en la vista: los hombros se colocan delante de la cámara (`FP_SHOULDER_*`, como la mano vanilla). Las poses de primera persona se ajustaron con `tools/preview_poses.py`, que renderiza la vista desde los ojos con FOV 70.

## Efectos de cliente — `client/fx/TransformFx`
Potencia de aura suavizada (se hincha al cargar, se apaga al soltar), partículas por forma, temblor de cámara de las transformaciones grandes. El estallido y la columna de luz se dibujan en `DBPlayerLayer.renderEnergy`; el destello de pantalla en `DBCKiOverlay`. La tecla `J` cambia `ClientConfig.AURA` (config de cliente `dragonblock-client.toml`).

## Texturas
Las texturas de `textures/entity` son en escala de grises y se tiñen en el juego (pelo, pelaje, rasgos, aura, ki). Para cambiar el color de una forma edita `Transformation`, no la textura.

## Probar
```
./gradlew.bat runClient
/dev max
/dev mastery 100
/dev form <forma>
```
