# Decisión de animación

## Qué había

El mod ya anima al jugador con un sistema propio que funciona. `ClientModEvents` cambia el `PlayerModel` de cada `PlayerRenderer` por `DBPlayerModel`. Este modelo pone poses (guardia, carga, técnicas, vuelo) encima de la animación de vanilla y las mezcla suavemente en cada jugador.

- **Primera persona:** dibuja los brazos de ese mismo modelo (`ClientRenderEvents.onRenderHand`).
- **Otros jugadores:** reciben el estado por la sincronización de datos (`S2CSyncDBCDataPacket`) y los golpes sueltos por `S2CAnimPacket`.

## Comparación

| Criterio | Sistema propio (`DBPlayerModel`) | GeckoLib 4.8.x |
|---|---|---|
| Forge 1.21.1 | Ya funciona | Hay versión |
| Modelo del jugador | Es el modelo de vanilla: skins, capas y armadura funcionan solos | Pensado para entidades propias. Animar al jugador obliga a reemplazar su renderer, y eso rompe skins, armaduras y otros mods |
| Entidades y NPC | `EnemyRenderer` y `DinoRenderer` con JSON propio | Su punto fuerte: fotogramas clave de Blockbench |
| Mezcla y prioridades | Una pose por prioridad (`AnimState.pose`) con transición suave | Controladores y transiciones |
| Sincronización | Un paquete por evento; el cliente reproduce la animación con los mismos ticks que el servidor | Hay que sincronizarlo igual a mano |
| Dependencias | Ninguna | Un jar más (se puede meter dentro del nuestro) |
| Mantenimiento | Las poses están en código | Archivos de Blockbench, que es lo que usará Gemini |

## Decisión

- **Jugador:** se extiende el sistema propio. Es lo que pide el combate (golpes cortos, sincronizados a tick) y no rompe skins ni armaduras.
- **GeckoLib:** se deja para las **entidades nuevas** (jefes, NPC con modelos de Gemini) cuando lleguen esos modelos. No se añade ahora porque todavía no hay ningún modelo que lo use.

## Prueba de concepto (hecha)

- Pose `MELEE` en `DBPlayerModel.melee()`, con tres fotogramas clave por movimiento: reposo, preparación e impacto. La preparación ocupa el `startup` del servidor, el impacto cae dentro de su ventana `active` y la vuelta al reposo ocupa la `recovery`.
- Los tiempos salen de `MeleeMove`, compartido entre cliente y servidor, así que la animación y el impacto coinciden.
- `DevAutotest` modo `melee`:
  - combo real contra un husk: 5 golpes, 5 impactos, uno por movimiento;
  - galería de poses vista de lado (`run/screenshots/autotest_melee_poses.png`): jab, directo, patada, golpe pesado y uppercut.
- **No verificado:** que otro cliente vea la animación. El camino es el mismo que el de las técnicas (`broadcastAnim`, que va a todos los que ven al jugador, y `AnimState`, guardado por id de entidad), pero no se ha probado con dos clientes.
