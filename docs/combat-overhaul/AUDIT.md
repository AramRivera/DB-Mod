# Combat Overhaul — Auditoría del mod actual

Fecha: 2026-11-03. Rama `feature/overhaul`, a partir del commit `f62fb0c`.

## 1. Entorno

| Dato | Valor real |
|---|---|
| Minecraft | 1.21.1 |
| Forge | 52.1.20 (`build.gradle`: `net.minecraftforge:forge:1.21.1-52.1.20`) |
| Java | 21 (toolchain de Gradle) |
| Mappings | `official 1.21.1` |
| Mod ID | `dragonblock` (`DragonBlockMod`) |
| Dependencias | Ninguna aparte de Forge |
| Compilar | `./gradlew build` → `build/libs/dragonblock-1.0.0.jar` |
| Pruebas | `JAVA_TOOL_OPTIONS="-Ddragonblock.autotest=<modo>" ./gradlew runClient` (modos en `client/DevAutotest`); capturas en `run/screenshots/autotest_*.png` |
| Servidor dedicado | `./gradlew runServer` (carpeta `run/`) |

## 2. Arquitectura actual

```
cliente                                   servidor (autoridad)
ClientEvents ── teclas ──► C2SInputStatePacket (teclas mantenidas: cargar, transformar, guardia, técnica)
             └─ acciones ─► C2SActionPacket (revertir, dash, aprender técnica, equipar… y ahora LIGHT/HEAVY)
                                             │
                          DragonBlockEvents.onPlayerTick → FormController / FormEffects / KiAttacks / Melee
                          DragonBlockEvents.onLivingHurt / onLivingDamage (fórmula de daño, guardia, recompensas)
                                             │
AnimState ◄── S2CAnimPacket (one-shots) ◄────┤
DBPlayerModel ◄── S2CSyncDBCDataPacket ◄─────┘ (PlayerDBCData completo en NBT, deduplicado en ModMessages.syncToTracking)
```

## 3. Sistemas existentes

| Sistema existente | Dónde vive | Cómo funciona | Qué debe conservarse | Integración propuesta |
|---|---|---|---|---|
| Medallas TP | `PlayerDBCData.trainingPoints` (NBT `TP`) | Un número entero. Se gana pegando (`onLivingHurt`: daño×0,4 + MND/5, con extras por raza, pesas y planeta pesado), matando enemigos (`DBEnemy`), Zenkai, Shenlong, misiones, el Gran Anciano y el torneo | El saldo, todas las fuentes y todos los costes | Los golpes del nuevo combate pasan por `LivingEntity.hurt`, así que dan TP con la misma fórmula. No hay moneda nueva |
| Aprendizaje de habilidades | `C2SActionPacket.LEARN_TECHNIQUE`, `Technique` (tpCost, minLevel) | El servidor comprueba que no la tenga, el nivel y el saldo, y luego cobra. Va en el hilo principal (`enqueueWork`), así que un doble clic no la cobra dos veces | Costes y requisitos de cada técnica | Sin cambios. El moveset básico de puños es gratis, igual que el puñetazo de vanilla |
| Mejoras de habilidades | No existe. Las técnicas no tienen nivel. Lo que se mejora con TP son los **atributos** (`upgradeStat`: coste = valor × 10) | — | El coste de los atributos | Sin cambios |
| Razas | `Race` (6 razas con modificadores STR/DEX/CON/WIL/MND y rasgo corporal) | Multiplican los atributos efectivos | Modificadores y rasgos | El daño de los golpes sale de `getEffectiveStr()`, así que la raza ya cuenta |
| Transformaciones | `Transformation` (29 formas con padre, razas, nivel mínimo, multiplicador y drenaje) y `FormController` | Se mantiene la tecla para cargar. El servidor revisa `getLockReason` (raza, nivel, maestría del padre, cola) | Cadenas, requisitos y efectos | Los golpes usan `FormEffects.meleeMultiplier` y los atributos efectivos, ya multiplicados por la forma. No hay multiplicadores nuevos |
| Maestría | `PlayerDBCData.mastery` (EnumMap por forma, 0–100, NBT `Mastery`) | +0,05 %/s en la forma (más con MND) y +0,15 % por golpe que conectas. Shenlong da +40 % | Los valores guardados y la maestría por forma | Un golpe del combo da la misma maestría que un puñetazo normal, porque usa el mismo camino de daño |
| Ki/recursos | `currentKi` y `getMaxKi()`, y `body` / `getMaxBody()` (la vida real, que vanilla solo refleja) | El ki se gasta en técnicas, vuelo, dash y guardia; la vida está en el `body` | Fórmulas y persistencia | Los golpes fuertes cuestan 6 de ki. No hay barra de resistencia nueva: el mod no tiene stamina y el ki cumple esa función |
| Animaciones | `client/anim/AnimState` y `client/anim/DBPlayerModel` (reemplaza el `PlayerModel` de todos los `PlayerRenderer`) y `ClientRenderEvents.onRenderHand` (brazos en primera persona) | Poses sobre la animación de vanilla, con mezcla suave por jugador. Los one-shots llegan por `S2CAnimPacket` a todos los que ven al jugador | El modelo y las poses actuales | Nueva pose `MELEE` con fotogramas clave, cronometrada con los mismos ticks que el servidor (ver `ANIMATION_DECISION.md`) |
| Red | `ModMessages` (SimpleChannel, protocolo exacto) | C2S: input, acciones, vuelo, atributos, raza, deseo, diálogo, viaje. S2C: sync de datos, animaciones, pantallas | Validar todo en el servidor | El cliente solo manda "ligero" o "fuerte". Protocolo subido a **9** |

## 4. Combate antes de este cambio

- **Cuerpo a cuerpo:** solo el puñetazo de vanilla. `onLivingHurt` le suma `STR×0,4` y lo multiplica por `FormEffects.meleeMultiplier`. `onMeleeLanded` lanza lejos al rival si la diferencia de poder es grande. No había combos, ventanas ni aturdimiento.
- **Defensa:**
  - **Guardia (tecla B):** bloquea el 65 % del daño pagando ki (daño × 1,5), en cualquier dirección.
  - **Dash (R):** 6 ticks de invulnerabilidad.
  - **Esquiva pasiva:** la del Instinto Superior (`FormEffects.tryDodge`).
- **Ki:** técnicas con carga, mínimo de carga, coste repartido durante la carga, cooldown y pose de lanzamiento (`KiAttacks`). Nada interrumpía una carga.

## 5. Progresión de transformaciones (del código)

La regla es una sola para todas las razas: `getLockReason`. Una forma se desbloquea si la raza puede usarla, si el nivel (la suma de atributos) llega al `minLevel` y si la **maestría de su forma padre es al menos `UNLOCK_MASTERY` = 25 %**.

> **Diferencia con el prompt:** el prompt dice que SSJ2 pide el 100 % de SSJ1, pero el código pide el **25 %** desde el principio. No lo cambié. Subirlo bloquearía de golpe las formas que algunos jugadores ya usan. Es una decisión de diseño que tenéis que tomar vosotros. Si decidís el 100 %, se cambia esa constante y se comprueba con `>=` sobre el valor guardado (que ya se limita a 100).

| Raza | Cadenas (padre → hija) |
|---|---|
| Todas | BASE → KAIOKEN → X10 → X20 · BASE → UI_SIGN → UI_MASTERED |
| Humano / Namek / Híbrido | BASE → POTENTIAL (Híbrido: → BEAST) |
| Saiyajin / Híbrido | BASE → SSJ1 → SSJ2 → SSJ3 → SSJ_GOD → SSJ_BLUE → SSJ_BLUE_EVOLVED (Saiyajin: SSJ_BLUE → ULTRA_EGO) · BASE → OOZARU (Saiyajin: → GOLDEN_OOZARU → SSJ4) |
| Arcosiano | BASE → SECOND → THIRD → FINAL → FULL_POWER · FINAL → GOLDEN → BLACK |
| Namekiano | BASE → GIANT · BASE → SYNC → ORANGE |
| Majin | BASE → SUPER → PURE |

## 6. Maestros NPC que existen de verdad

Los personajes son diseños **originales por arquetipo** (decisión del proyecto: no copiamos personajes concretos de la serie). Por eso no hay Roshi, Piccolo, Vegeta ni ningún otro con ese nombre. Los equivalentes que existen son:

| `NpcKind` | Papel actual | Arquetipo del prompt |
|---|---|---|
| `MASTER` "Maestro Kaido" | Prueba de enemigos y enseña técnicas | Maestro de fundamentos |
| `GUARDIAN` "El Guardián" | Enseña la teletransportación | Guardián del templo celeste |
| `CELESTIAL` "Maestro Celeste" | Enseña la Genkidama, planeta de gravedad | Maestro de ki y concentración |
| `ELDER` "Gran Anciano Tsuru" | Despierta el poder oculto | Despertar del potencial |

El entrenamiento con maestros (fase 9) se montará sobre estos cuatro.

## 7. Riesgos de integración

1. **Clic izquierdo y derecho:** la mano vacía ahora hace golpes del mod. Picar y usar bloques no cambia, ni usar el clic derecho en un personaje o un animal. Con un objeto en la mano todo sigue igual que en vanilla.
2. **TP por segundo:** un combo da TP igual que los puñetazos de antes. El ritmo es parecido al de hacer spam de clics en vanilla, pero hay que vigilarlo en el balance.
3. **Inmunidad entre golpes:** cada golpe pone a 0 la inmunidad de medio segundo de vanilla. El ritmo lo marcan los ticks de cada movimiento, y como cada movimiento golpea una sola vez, no hay daño doble.
4. **Protocolo 9:** todos los clientes necesitan el jar nuevo.

## 8. Qué se reutiliza, qué se extiende y qué es nuevo

- **Se reutiliza:**
  - el camino de daño completo (`onLivingHurt` / `onLivingDamage`: guardia, esquiva, dash, Zenkai, `body`, TP, maestría);
  - `KiAttacks.mayAffect` (zonas seguras, PvP del servidor);
  - `ModMessages.broadcastAnim`;
  - `DBPlayerModel` y su mezcla de poses.
- **Se extiende:**
  - `C2SActionPacket` (LIGHT, HEAVY);
  - `S2CAnimPacket` (MELEE);
  - `AnimState` (pose MELEE);
  - la guardia, que no funciona si estás aturdido;
  - `onMeleeLanded`, que ahora es `powerGap()` y también lo llama `Melee`.
- **Nuevo:** `common/combat/MeleeMove` (definiciones) y `common/combat/Melee` (estado por jugador y resolución).

## 9. Plan por fases

| Fase | Estado |
|---|---|
| 0. Auditoría | Este documento |
| 1+3. Motor y cuerpo a cuerpo mínimo | **Hecho**: golpe ligero y fuerte validados en el servidor, ventanas startup/activo/recuperación, un impacto por movimiento, alcance, cono y línea de visión, empuje propio, aturdimiento, animación sincronizada |
| 4. Combos | **Base hecha**: cadena JAB→CROSS→KICK→LAUNCHER, buffer de input, el daño baja un 8 % por golpe (hasta un mínimo del 50 %) y deja de aturdir a partir del 8.º golpe |
| 5. Defensa | **Parcial**: el HAMMER rompe la guardia (30 ticks sin guardia) y los golpes fuertes interrumpen la carga de técnicas. Faltan la esquiva direccional, la esquiva perfecta y el contraataque |
| 2. Lock-on y cámara | Pendiente (va después porque no la necesita el combate básico) |
| 6. Movimiento aéreo y persecución | Pendiente (el LAUNCHER ya lanza hacia arriba; falta la persecución) |
| 7. Ki en el motor | Pendiente |
| 8. Razas y transformaciones | Ya integrado por diseño, porque se usan los atributos efectivos y `meleeMultiplier`. Pendiente la decisión del 25 % o el 100 % |
| 9. Entrenamiento con maestros | Pendiente |
| 10. Pulido | Pendiente |
