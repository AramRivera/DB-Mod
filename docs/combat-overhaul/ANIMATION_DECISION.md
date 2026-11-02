# Decisión Técnica de Animación — GeckoLib vs. DBPlayerModel Propio

## 1. Evaluación y Comparativa

El objetivo del Combat Overhaul es dotar al jugador y a las entidades de animaciones ágiles de combate (golpes, patadas, bloqueos, esquivas y persecuciones). Se evaluaron dos caminos técnicos para Minecraft Forge 1.21.1:

| Criterio de Evaluación | Opción A: GeckoLib 4 (Biblioteca Externa) | Opción B: Extensión de DBPlayerModel (Sistema Propio Existente) |
| :--- | :--- | :--- |
| **Compatibilidad con Modelo de Jugador Vanilla** | ⚠️ **Pobre / Problemática.** GeckoLib está diseñado primordialmente para entidades personalizadas (`GeoEntity`). Aplicarlo al jugador requiere wrappers agresivos o mixins en `PlayerRenderer` que rompen compatibilidad con skins personalizadas, capas y armaduras vanilla. | ✅ **Excelente.** Ya está implementado e inyectado en `ClientModEvents.swapModel` mediante `LivingEntityRenderer.model`. Funciona transparentemente sobre el modelo estándar de Minecraft. |
| **Compatibilidad con Razas y Accesorios del Mod** | ❌ **Incompatible.** Nuestro mod renderiza razas sin skin vanilla (`RaceSkins`), colas animadas en 7 segmentos, antenas, cuernos, scouters y pesas (`DBPlayerLayer`). GeckoLib ignoraría estas capas a menos que se reescriba todo el pipeline de renderizado. | ✅ **Totalmente compatible.** Las capas de raza (`DBPlayerLayer`) se anclan directamente a los huesos de `DBPlayerModel` (`head`, `body`, `rightArm`, etc.). |
| **Soporte de 1ª y 3ª Persona** | ⚠️ Requiere configuraciones separadas complejas para render de brazos en primera persona. | ✅ **Unificado.** `DBPlayerModel.firstPerson` ya adapta los hombros y brazos para que la vista en primera persona y tercera persona compartan la misma postura sin saltos. |
| **Interpolación y Mezcla de Poses** | ✅ Soporta curvas bezier y canales de animación de Blockbench. | ✅ **Soporta decaimiento exponencial (`1f - Math.exp(-dt * speed)`).** Las poses nunca se entrecortan y se mezclan suavemente con el caminar/correr vanilla. |
| **Rendimiento y Dependencias en Servidores** | ⚠️ Añade ~4-8 MB de biblioteca externa, posibles incompatibilidades de versión entre Forge 52.1.20 y mappings Mojang 1.21.1. | ✅ **Cero dependencias externas.** 100% código limpio en Java 21, seguro para clientes y servidores dedicados sin crashes por `Dist`. |

---

## 2. Decisión Tomada

**Se decide mantener y extender `DBPlayerModel` y `AnimState`.**

No se añadirá GeckoLib al modelo del jugador porque rompería el trabajo previo de personalización de razas (`RaceSkins`), los accesorios 3D (`Scouter`, pesas, cinturones) y la vista en primera persona, introduciendo riesgos críticos de estabilidad en Forge 1.21.1.

---

## 3. Plan de Expansión de Poses en DBPlayerModel

Se incorporan al enum `AnimState.Pose` y al despachador de poses en `DBPlayerModel.applyPose` las siguientes posturas de combate:

1. `PUNCH_LIGHT_1`: Golpe directo rápido de derecha con rotación leve de torso.
2. `PUNCH_LIGHT_2`: Golpe cruzado rápido de izquierda.
3. `KICK_HEAVY`: Patada circular ascendente con extensión de pierna y balanceo de brazos.
4. `UPPERCUT`: Gancho ascendente que inicia la secuencia de lanzamiento.
5. `GUARD`: Postura defensiva con ambos antebrazos protegiendo torso y cabeza.
6. `DODGE_LEFT` / `DODGE_RIGHT`: Inclinación evasiva lateral rápida.
7. `CHASE`: Postura aerodinámica de persecución hacia adelante (*Boost Dash*).

Las animaciones de un solo golpe (*one-shot*) se transmiten en red mediante `S2CAnimPacket.Anim` existente, asegurando que todos los clientes en el área vean los impactos y poses sin sobrecargar la red con transformaciones continuas de huesos.
