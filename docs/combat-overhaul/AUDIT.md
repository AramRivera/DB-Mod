# Auditoría Técnica y Diagnóstico de Arquitectura — Combat Overhaul

Este documento establece el mapa real de sistemas, clases y paquetes del repositorio `dragonblock` en **Minecraft Forge 52.1.20+ (Java 21, MC 1.21.1)** previo al desarrollo del Dragon Ball Combat Overhaul (inspirado en Dragon Ball Xenoverse 2).

---

## 1. Entorno de Ejecución y Mappings

* **Minecraft:** 1.21.1
* **Forge:** 52.1.20 (`net.minecraftforge:forge:1.21.1-52.1.20`)
* **Java:** Toolchain Java 21 (`JavaLanguageVersion.of(21)`)
* **Gradle Plugin:** `net.minecraftforge.gradle: [7.0.3,8)`
* **Mappings:** Oficiales de Mojang (`channel: 'official', version: '1.21.1'`)
* **Mod ID:** `dragonblock`
* **Clase Principal:** `com.dragonblock.DragonBlockMod`
* **Dependencias Externas:** Ninguna biblioteca de terceros instalada (sin GeckoLib, sin mixins externos). Compilación 100% autocontenida sobre Forge.

---

## 2. Mapa de Sistemas Existentes y Archivos Reales

| Sistema Existente | Dónde Vive | Cómo Funciona Actualmente | Qué Debe Conservarse Obligatoriamente | Integración con Combat Overhaul |
| :--- | :--- | :--- | :--- | :--- |
| **Medallas TP (Entrenamiento)** | [`PlayerDBCData.java`](file:///src/main/java/com/dragonblock/common/data/PlayerDBCData.java)<br>[`C2SUpgradeStatPacket.java`](file:///src/main/java/com/dragonblock/common/network/packet/C2SUpgradeStatPacket.java) | Saldo entero `tp` guardado en NBT. Se obtiene en combate (daño infligido), misiones y torneo. Se consume en atributos y técnicas. | La economía original de TP. No añadir monedas paralelas ni saltarse costes. | Los ataques melee básicos (ligeros, pesados, guardias, esquivas) son universales y gratuitos. Habilidades y estilos avanzados consumirán TP con validación en servidor. |
| **Técnicas de Ki** | [`Technique.java`](file:///src/main/java/com/dragonblock/common/data/Technique.java)<br>[`KiAttacks.java`](file:///src/main/java/com/dragonblock/common/combat/KiAttacks.java)<br>[`KiBlastEntity.java`](file:///src/main/java/com/dragonblock/common/entity/KiBlastEntity.java) | 13 técnicas (Kamehameha, Masenko, Galick Ho, Final Flash, Genkidama, etc.). Barra de técnicas (`Ctrl` + rueda), carga con `X`, choques de Ki (*clash*). | Todo el catálogo existente, sus costes de Ki, recargas y entidades de proyectil. | Se añaden ventanas de castigo: el spam de proyectiles puede ser interrumpido por golpes físicos o esquivado con *Perfect Dodge*. Las técnicas se pueden encadenar tras confirmar combos melee. |
| **Razas** | [`Race.java`](file:///src/main/java/com/dragonblock/common/data/Race.java)<br>[`RaceSkins.java`](file:///src/main/java/com/dragonblock/client/renderer/RaceSkins.java)<br>[`DBPlayerLayer.java`](file:///src/main/java/com/dragonblock/client/renderer/DBPlayerLayer.java) | 5 razas (Saiyajin, Humano/Híbrido, Namekiano, Demonio del Frío, Majin) con capas 3D y texturas dinámicas teñidas sin usar skin vanilla. | Fisiología racial, rasgos visuales (colas, cuernos, antenas, tentáculos) y bonificaciones de estadísticas. | El combate melee respetará multiplicadores de velocidad y fuerza raciales. |
| **Transformaciones y Maestría** | [`Transformation.java`](file:///src/main/java/com/dragonblock/common/data/Transformation.java)<br>[`FormController.java`](file:///src/main/java/com/dragonblock/common/combat/FormController.java)<br>[`FormEffects.java`](file:///src/main/java/com/dragonblock/common/combat/FormEffects.java) | Formas canónicas ordenadas jerárquicamente. Drenaje de Ki, multiplicador de BP, mecánicas únicas (esquiva UI, furia Ultra Ego, críticos Bestia). | Identificadores NBT de formas, árboles de transformación y maestría individual (0-100%). | **Regla estricta:** Requisito del 100% de maestría en la forma padre antes de acceder a la siguiente (`UNLOCK_MASTERY = 100.0f`). El combate físico acumula maestría en la forma activa. |
| **Vida y Cuerpo** | [`PlayerDBCData.java`](file:///src/main/java/com/dragonblock/common/data/PlayerDBCData.java)<br>[`DragonBlockEvents.java`](file:///src/main/java/com/dragonblock/common/event/DragonBlockEvents.java) | Desacople de corazones vanilla: la vida real es `body / maxBody`. Corazones solo reflejan porcentaje. | La fórmula de daño a `body` y el escalado con CON y formas. | El daño físico y aturdimiento de los golpes se aplica a `body` mediante eventos de servidor autoritativos. |
| **Vuelo y Movilidad** | `C2SFlyPacket.java`<br>[`SpeedTrail.java`](file:///src/main/java/com/dragonblock/client/fx/SpeedTrail.java)<br>[`ClientRenderEvents.java`](file:///src/main/java/com/dragonblock/client/ClientRenderEvents.java) | Vuelo libre con `F`, inclinación del cuerpo según velocidad y estela de partículas de energía. | Toda la física de vuelo existente. | Integración con combate aéreo: *Boost Dash* y persecuciones (*chases*) en el aire tras lanzar al oponente. |
| **NPCs y Maestros** | [`NpcEntity.java`](file:///src/main/java/com/dragonblock/common/entity/NpcEntity.java)<br>[`NpcKind.java`](file:///src/main/java/com/dragonblock/common/npc/NpcKind.java)<br>[`NpcDialogues.java`](file:///src/main/java/com/dragonblock/common/npc/NpcDialogues.java) | Maestros en el mundo: Kaido (Roshi), Guardián (Kamisama), Maestro Celeste (Kaio), Gran Anciano. | Entidades en el mundo, diálogos existentes y zonas seguras. | Sistema de entrenamiento estructurado y pedagógico (empezando con Maestro Kaido/Roshi) que utiliza **el mismo motor de combate del PvP** con desafíos QTE de precisión. |
| **Animaciones del Jugador** | [`DBPlayerModel.java`](file:///src/main/java/com/dragonblock/client/anim/DBPlayerModel.java)<br>[`AnimState.java`](file:///src/main/java/com/dragonblock/client/anim/AnimState.java)<br>[`S2CAnimPacket.java`](file:///src/main/java/com/dragonblock/common/network/packet/S2CAnimPacket.java) | Modelo inyectado en `PlayerRenderer` (`swapModel`) que mezcla poses suaves con decaimiento exponencial sobre animaciones vanilla en 1ª y 3ª persona. | Interpolación suave (`rotLerp`), compatibilidad total con capas de raza, skins y accesorios. | **Extender `DBPlayerModel`** añadiendo poses de combate melee (golpes ligeros, patadas pesadas, guardia, esquivas direccionales) sin añadir dependencias externas riesgosas. |
| **Red y Sincronización** | [`ModMessages.java`](file:///src/main/java/com/dragonblock/common/network/ModMessages.java)<br>[`C2SActionPacket.java`](file:///src/main/java/com/dragonblock/common/network/packet/C2SActionPacket.java)<br>`S2CSyncDBCDataPacket.java` | Protocolo Forge `SimpleChannel` autoritativo en servidor. Clientes envían intenciones/inputs; servidor valida y replica estado. | Autoridad del servidor sobre daño, recursos y cooldowns. | Creación de paquetes de combate especializados (`C2SCombatActionPacket`, `S2CCombatFeedbackPacket`). |
| **Cámara** | [`CinematicCamera.java`](file:///src/main/java/com/dragonblock/client/fx/CinematicCamera.java) | Interceptación de `ViewportEvent.ComputeCameraAngles`. Suavizado con lerp y comprobación de oclusión de bloques (`ClipContext`). | Control de cámara en 3ª persona sin desconectar al jugador. | Implementación de **Lock-On libre estilo XV2**: asistencia de ángulo hacia el objetivo fijado manteniendo el control del ratón del jugador. |

---

## 3. Riesgos de Integración y Mitigaciones

1. **Riesgo de Desincronización en Red (Hitboxes y Daño):**
   * *Mitigación:* El cliente inicia la animación de forma anticipada para respuesta inmediata a 60+ FPS, pero el servidor realiza la validación geométrica (rango de ataque, cono de visión, estado del atacante y cooldown) antes de infligir daño o aplicar stun.
2. **Riesgo de Combos Infinitos en PvP:**
   * *Mitigación:* Se implementa *Combo Scaling* (cada golpe subsiguiente dentro de la misma cadena reduce su daño y stun un 12%) y un límite estricto de golpes antes del remate/lanzamiento forzado.
3. **Riesgo de Incompatibilidad con Modelos y Skins:**
   * *Mitigación:* No se altera la estructura de `LivingEntityRenderer` ni se reemplaza el modelo del jugador por una librería externa como GeckoLib. Las poses se aplican en los 6 huesos estándar (`head`, `body`, `rightArm`, `leftArm`, `rightLeg`, `leftLeg`) mediante `DBPlayerModel`.

---

## 4. Plan de Fases de Implementación

* **Fase 0:** Auditoría, documentación técnica y decisiones de diseño (`AUDIT.md`, `ANIMATION_DECISION.md`, `PROGRESSION_INTEGRATION.md`).
* **Fase 1:** Sistema de Fijación de Objetivo (Lock-On) y Cámara Libre Asistida estilo Xenoverse 2.
* **Fase 2:** Núcleo de Combate Melee (Ataque Ligero, Ataque Pesado, animaciones en `DBPlayerModel` y sincronización en servidor).
* **Fase 3:** Sistema de Combos, Ventanas de Acción y Aturdimiento (*Hit Stun*).
* **Fase 4:** Mecánicas Defensivas (Guardia, *Guard Break*, Esquiva Direccional y *Perfect Dodge*).
* **Fase 5:** Movimiento de Combate, Lanzamiento y Persecución Aérea (*Boost Dash Chase*).
* **Fase 6:** Integración y Contrajuego con Técnicas de Ki (castigo de spam y confirmación de combos).
* **Fase 7:** Sistema Extensible de Entrenamiento con Maestros (Maestro Kaido/Roshi funcional con pruebas QTE).
* **Fase 8:** Pulido, Balance, Retroalimentación y Efectos de Sonido.
