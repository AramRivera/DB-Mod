# Integración de Progresión — Medallas TP y Maestría de Transformaciones

Este documento define las reglas no negociables de integración entre el nuevo sistema de combate y la economía y progresión ya presentes en el mod.

---

## 1. Economía de Medallas TP (Puntos de Entrenamiento)

1. **Moneda Única y Soberana:**
   * La única moneda utilizada para el aprendizaje de habilidades, mejora de atributos y adquisición de técnicas es el saldo de **TP** persistido en el NBT del jugador a través de [`PlayerDBCData.java`](file:///src/main/java/com/dragonblock/common/data/PlayerDBCData.java).
   * No se introduce ninguna moneda secundaria, fragmento de habilidad o ficha de combate.

2. **Acceso al Moveset Básico:**
   * Los movimientos físicos fundamentales (golpe ligero, golpe pesado, bloqueo básico, esquiva direccional) son **gratuitos y universales** para todos los jugadores y razas desde el inicio. No están bloqueados tras compras de TP.

3. **Habilidades y Estilos Avanzados:**
   * Técnicas avanzadas de combate, posturas especializadas o técnicas de Ki continúan utilizando `Technique.getTpCost()`, desbloqueadas tras cumplir los requisitos de nivel y atributos correspondientes.

4. **Validación Autorizada en Servidor:**
   * Toda compra o gasto de TP se valida exclusivamente en el hilo del servidor antes de aplicar cualquier cambio de estado. Se previene cualquier exploit por paquetes duplicados o desincronización de cliente.

---

## 2. Progresión de Transformaciones y Regla del 100% de Maestría

1. **Requisito del 100% en la Cadena de Transformación:**
   * La constante [`Transformation.UNLOCK_MASTERY`](file:///src/main/java/com/dragonblock/common/data/Transformation.java) se fija en **`100.0f`** (100% de dominio completo).
   * Un jugador no puede desbloquear ni acceder a una transformación superior (por ejemplo, Super Saiyajin 2) a menos que haya alcanzado el **100% de maestría real persistida en la forma padre** (Super Saiyajin 1).
   * La validación de maestría se ejecuta en el servidor mediante `PlayerDBCData.getMastery(parent) >= 100.0f`.

2. **Ganancia de Maestría mediante Combate Melee:**
   * Además del tiempo transcurrido en combate, encadenar combos y asestar golpes físicos mientras se está transformado otorga progreso de maestría a la forma activa.
   * Esto premia el combate activo y la técnica del jugador frente a simplemente esperar quieto con la transformación activada.

3. **Conservación de Datos Persistentes:**
   * Los jugadores existentes mantienen intactos sus valores de maestría, formas desbloqueadas y atributos. No hay reseteo de estadísticas ni incompatibilidad de guardado.
