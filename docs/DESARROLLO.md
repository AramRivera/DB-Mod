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

## Probar
```
./gradlew.bat runClient
/dev max
/dev mastery 100
/dev form <forma>
```
