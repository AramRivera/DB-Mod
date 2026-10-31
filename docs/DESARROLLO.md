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

## Texturas
Las texturas de `textures/entity` son en escala de grises y se tiñen en el juego (pelo, pelaje, rasgos, aura, ki). Para cambiar el color de una forma edita `Transformation`, no la textura.

## Probar
```
./gradlew.bat runClient
/dev max
/dev mastery 100
/dev form <forma>
```
