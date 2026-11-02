# Validacion del combate

## Comprobaciones ejecutadas

- Compilacion Java 21 con Forge 1.21.1-52.1.20.
- Construccion del JAR mediante gradlew.bat build.
- Revision de diferencias con git diff --check.

El proyecto no contiene pruebas automaticas de combate: Gradle informa test NO-SOURCE. Una compilacion correcta no demuestra comportamiento multijugador.

## Pruebas manuales pendientes

Usar dos clientes con el mismo JAR y un mundo de prueba, fuera de zonas seguras.

1. Fijar con Z, cambiar objetivo con Tab y liberar con Z. Verificar control manual de camara y limpieza al cambiar dimension.
2. Atacar con clic: comprobar cuatro poses, un impacto por entrada y fin de cadena al fallar o cambiar objetivo. Salir de alcance durante la preparacion evita el impacto. Una pared debe impedir impactos.
3. Probar U y B: bloqueo frontal, golpe por detras, gasto unico de Ki y ruptura de guardia con fuerte o Ki insuficiente. Guardar sin recibir golpe no supera el desafio.
4. Esquivar con R mientras se camina en cada direccion y estando quieto. Verificar que evitar un ataque en los primeros dos ticks habilita el contraataque y que este expira tras 16 ticks.
5. Completar el lanzamiento de cuatro golpes, R y M: probar dentro y fuera de alcance/tiempo, en tierra y en aire. Repetir M no debe reutilizar el mismo lanzamiento.
6. Probar B+R junto a paredes y borde del mundo: nunca aparecer dentro de bloques; el destino rechazado devuelve el coste.
7. Intentar disparar Ki, volar y enviar ataques durante aturdimiento/recuperacion. Verificar rechazo y posterior recuperacion de controles.
8. Completar el entrenamiento, reconectar y repetirlo: solo la primera finalizacion de cada desafio recompensa TP. O, muerte, desconexion y cambio de dimension deben cancelar la sesion.
9. Comparar dano, TP y maestria de un golpe vanilla y uno administrado. Confirmar que STR, transformacion y guardia no se aplican dos veces.
10. Revisar animaciones con ambos clientes y con latencia. Conservar mundos anteriores para comprobar compatibilidad de TP, formas, maestria y flags.

## Criterio para declarar finalizado el MD

Completar el alcance pendiente de ARCHITECTURE.md y ejecutar los criterios de aceptacion del documento original. No marcar una funcion como terminada solamente por compilar o por existir su clase.
