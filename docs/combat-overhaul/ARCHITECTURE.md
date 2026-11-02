# Combate implementado en esta iteracion

## Flujo

El cliente envia intenciones mediante C2SCombatActionPacket. El servidor comprueba estado, recuperacion, alcance y visibilidad. Los ataques ligeros programan el impacto dos ticks despues de la animacion; los fuertes, cuatro ticks. El tick vuelve a resolver el objetivo para que salir del alcance permita evitar el golpe. Cada intencion admitida tiene un solo impacto pendiente.

La cadena requiere impactos confirmados contra el mismo objetivo para habilitar su lanzamiento final. Los fallos y cambios de objetivo eliminan la confirmacion. El servidor limita las persecuciones a la ventana abierta por el lanzamiento; Meteor Smash consume esa ventana. El desplazamiento usa la fisica de colision normal. Vanish comprueba el volumen de destino y el borde del mundo antes de teleportar.

La esquiva usa la direccion de movimiento horizontal; estando quieto retrocede. Su ventana de evasion dura seis ticks; los primeros dos permiten confirmar una esquiva perfecta al evitar un impacto. Esta abre 16 ticks para un contraataque con bonificacion. No concede TP por pulsar la tecla.

La vida real, transformaciones, atributos y recompensas siguen pasando por DragonBlockEvents y PlayerDBCData. El marcador resolvingHit evita sumar por segunda vez STR y cobrar por segunda vez la guardia. Los golpes administrados conservan el multiplicador especifico de FormEffects. Los ataques normales de Minecraft conservan su ruta previa. Ki y vuelo rechazan entradas durante preparacion, recuperacion o aturdimiento.

El protocolo pasa de 8 a 9: cliente y servidor deben actualizarse juntos.

## Entrenamiento

Los desafios reciben eventos reales de combo confirmado, impacto bloqueado, esquiva perfecta y remate aereo. Activar la guardia sin bloquear un golpe no completa un desafio. La primera recompensa se registra con una flag estable training:<perfil>:<desafio> dentro del guardado existente. Repetir sirve para practicar sin cobrar de nuevo. Una sesion termina al morir, desconectarse, cambiar de dimension, alejarse 128 bloques del inicio, cancelarla o transcurrir diez minutos.

## Controles

- Z: fijar o soltar objetivo.
- Tab / Mayus + Tab: cambiar objetivo.
- Clic de ataque con objetivo fijado: golpe ligero; sin fijacion se conserva el uso normal de Minecraft.
- U: golpe fuerte.
- B: guardia.
- R: esquiva; durante la persecucion valida, impulso hacia el objetivo.
- B + R: Z-Vanish.
- M: remate aereo dentro de la ventana de persecucion.
- O: cancelar entrenamiento.

Las nuevas teclas se pueden cambiar en Controles.

## Alcance pendiente del MD

Esta iteracion NO completa la especificacion entera. Continuan pendientes el sparring autonomo de maestros usando el motor comun, escenarios QTE completos con evaluacion de precision, catalogo amplio de personajes y especialidades, perfiles de movimientos cargados desde datos/configuracion, buffer de combos y ramas avanzadas, HUD sincronizado de estados, y verificacion de todas las reglas con dos clientes y servidor dedicado. El entrenamiento actual requiere un oponente real apropiado fuera de zonas protegidas; no genera un rival de practica.

No se ha comprobado la sensacion en juego ni la sincronizacion visual bajo latencia. Los tiempos introducidos son parametros iniciales de implementacion y requieren balance, no representan un balance definitivo.
