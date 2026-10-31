# 🐉 Dragon Block - Minecraft 1.21.1 (Forge)

Mod de Dragon Ball para **Minecraft Java 1.21.1** desarrollado sobre **Minecraft Forge 52.1.20+** y **Java 21**, fuertemente inspirado en las mecánicas clásicas de **Dragon Block C (1.7.10)**.

---

## ⚡ Características Principales

### 1. Ki, vida y aura
* **Ki**: máximo según Voluntad (WIL) y raza. Se carga manteniendo `C` y se regenera solo.
* **Vida (cuerpo)**: reserva propia que crece con la Constitución (CON) efectiva, así que transformarte te hace más resistente. Se muestra en una **barra roja** en lugar de los corazones; los corazones vanilla solo reflejan el porcentaje, por eso nunca se apilan. La comida, pociones y regeneración siguen funcionando.
* **Aura**: tres capas de llamas, más **ancha y alta** que el cuerpo, con lenguas que suben, chispas que se desprenden y un anillo de presión en el suelo. Al empezar a cargar **se hincha** y late; al soltar se apaga poco a poco. **Crece con el poder de pelea y el multiplicador de la forma.** Se puede ocultar con `J`.
* **Vuelo (`F`)**: velocidad cercana a la de creativo; corriendo en el aire vas más rápido y gastas más Ki. El cuerpo se inclina hacia delante según la velocidad.
* **Velocidad**: la Destreza da hasta +35% al caminar, **sin el efecto de "poción de velocidad"** en el campo de visión.

---

### Interfaz
* **Ficha (`V`)**: se abre con animación y muestra una **vista 3D de tu personaje** que sigue el ratón (con pelo, aura y cola). Pestañas con subrayado animado, barras de atributos que crecen al mejorar y tarjetas de formas con color, maestría y una descripción al pasar el ratón.
* **HUD**: barras que se mueven con suavidad, "fantasma" de daño en la vida, barra de Ki que brilla al cargar, anillo de carga en la mira, título al transformarte y TP ganados flotando.

### 2. Ficha de Personaje y Atributos (Tecla `V`)
Al presionar la tecla `V` se accede a la interfaz de estadísticas del personaje estilo Dragon Block:

* **Atributos de Combate**:
  * **STR (Fuerza)**: Incrementa el daño físico en combate cuerpo a cuerpo y la potencia de impacto.
  * **DEX (Destreza)**: Incrementa la velocidad de desplazamiento y otorga reducción de daño físico.
  * **CON (Constitución)**: Otorga corazones de salud máxima adicionales y mayor regeneración biológica.
  * **WIL (Voluntad)**: Aumenta la capacidad máxima de Ki, velocidad de carga y daño de las técnicas de energía.
  * **MND (Mente)**: Aumenta la eficiencia en la ganancia de TP y reduce el coste de mantenimiento de Ki.
* **Puntos de Entrenamiento (TP)**: Ganados en combate al infligir daño a oponentes y entrenar. Se utilizan para subir de nivel los atributos pulsando los botones `[+]`.
* **Poder de Pelea (Battle Power / BP)**: Nivel de combate calculado en base a todos los atributos efectivos, multiplicador de transformación y porcentaje de Ki disponible.

---

### 3. Razas Disponibles
El jugador puede seleccionar su raza en cualquier momento desde la interfaz:

| Raza | Ventajas & Características |
| :--- | :--- |
| **Humano** | Rápida regeneración de Ki pasiva (+50%) y facilidad para el dominio técnico de la mente. |
| **Saiyajin** | Alto poder ofensivo (STR/WIL) y activación de **Zenkai Boost** (+50 TP al estar en peligro mortal). Acceso a Super Saiyajin. |
| **Híbrido Saiyajin** | Gran potencial latente con bonificación del +25% en ganancia de TP. Acceso a Super Saiyajin. |
| **Namekiano** | Regeneración continua de vida sin depender de saturación alimenticia y alta resistencia base. |
| **Demonio del Frío** | Raza de Freezer. Gran velocidad de movimiento y consumo de Ki en vuelo reducido a la mitad. |
| **Majin** | Constitución y resistencia extrema; cuerpos elásticos con absorción de daño natural. |

---

### 4. Transformaciones (mantener `G`, volver con `H`)
Elige una forma en la pestaña **Formas** de la ficha (`V`) y mantén `G` para transformarte. El tiempo de carga baja de 3 s a 0,5 s con la maestría.

* **Requisitos**: nivel (suma de atributos) y, para formas avanzadas, **25% de maestría** en la forma anterior.
* **Maestría (0-100%)**: sube estando transformado y combatiendo. Reduce hasta un 60% el consumo de Ki.
* **Agotamiento**: sin Ki vuelves a la forma base.
* **Ozaru**: si eres Saiyajin con la cola suelta y **miras la luna llena** de noche, te transformas solo. También puedes elegirlo y mantener `G` cualquier noche a cielo abierto. Al salir el sol (o al enrollar la cola) vuelves a la normalidad. `/dev form oozaru` lo fuerza de día para pruebas.
* **Peinados**: 8 diseños propios por nivel (no copias del pelo de personajes concretos), hechos con mechones triangulares escalonados que salen de un volumen de pelo: *Flare*, *Heavy* (grados musculosos), *Ridge* (cresta afilada con un mechón suelto), *Mane* (melena hasta la cintura), *Wild* (oscuro y salvaje), *Sleek* (hacia atrás), *Spire* (cresta central) y *Crest* (plateado y sereno). Se definen en `assets/dragonblock/hair/*.json`.

#### La esencia de cada forma
Cada forma tiene su secuencia de transformación (más larga y violenta cuanto más poderosa), partículas, ojos y una mecánica propia:

| Forma | Cómo se ve | Qué hace distinto |
| :--- | :--- | :--- |
| Super Saiyajin | Pelo dorado *Flare*, ojos verde azulado, destello dorado | Base dorada equilibrada |
| Grado 2 / Grado 3 | Pelo *Heavy*, más músculo, polvo | G2: -10% daño recibido. G3: +25% golpe, -25% daño recibido, 25% más lento |
| SSJ Pleno | Aura serena | Sin consumo, regenera Ki, técnicas más baratas |
| Super Saiyajin 2 | Cresta afilada con mechón, **rayos constantes**, chispas | +15% daño de Ki |
| Super Saiyajin 3 | **El pelo crece** hasta la cintura mientras cargas, terremoto, rayos | +15% golpe, consumo alto |
| Dios | Pelo *Sleek* rojizo, brasas, columna de luz | Cura el cuerpo poco a poco, técnicas más baratas |
| Blue / Blue Evolución | Pelo y aura cian, destellos | Control perfecto: técnicas 30% más baratas |
| Ultra Ego | Aura violeta, brasas | Se hace más fuerte cuanto más daño recibe |
| Instinto Superior | Aura plateada fina, destellos, columna de luz | **Esquiva sola** (30% / 55%) desplazándose |
| Bestia | Cresta alta plateada, ojos rojos, rayos rojos | 20% de golpes críticos x1,6 |
| Ozaru / Ozaru Dorado | Crece poco a poco hasta 4x, **cabeza de simio con hocico, colmillos y ojos rojos**, pelaje | No puede volar, golpe x1,5, ráfagas x1,6, más resistente |
| SSJ4 | Pelo *Wild* negro, **chaleco de pelaje rojo con pecho descubierto**, ojos dorados con delineado rojo, cola | Regenera el cuerpo |
| Kaio-ken | **Cuerpo brillando en rojo** con latido | Drena vida |
| Arcosiano / Namekiano / Majin | Cuernos, gigante, aura propia | Formas de su línea con sus bonus |

---

### Secuencias de transformación
Mantén `G` y vive la transformación por fases:
1. **Postura**: plantas los pies (casi no te mueves) y tensas los brazos; en primera persona ves tus puños temblar.
2. **Carga**: el aura crece, un anillo alrededor de la mira se va llenando y la pantalla tiembla más cuanto más poderosa es la forma (para ti y para los que estén cerca).
3. **Escombros**: trozos reales del suelo se arrancan y **flotan girando** a tu alrededor.
4. **Implosión**: justo antes del final el aura se contrae un instante.
5. **Estallido**: explosión de energía, los escombros salen despedidos, onda en el suelo, destello en pantalla y el **nombre de la forma** aparece en grande.

Cada forma tiene su estilo: *Destello* (el pelo parpadea), *Oleada* (rayos), *Cataclismo* (larga, terremoto, el pelo crece), *Divina* (serena, columna de luz), *Gigante* (creces poco a poco), *Tensión* (llamarada roja). Con más maestría la carga es más corta.


---


| Línea | Formas |
| :--- | :--- |
| Universal | Kaio-ken (x1, x10, x20, drenan vida), Instinto Superior (Presagio / Dominado) |
| Humano / Namekiano / Híbrido | Potencial Liberado |
| Saiyajin / Híbrido | SSJ, Grado 2, Grado 3, SSJ Pleno, SSJ2, SSJ3, Dios, Blue, Blue Evolución |
| Saiyajin (con cola) | Ozaru (luna llena), Ozaru Dorado, SSJ4, Ultra Ego |
| Híbrido | Bestia |
| Arcosiano | Segunda, Tercera, Final, Final 100%, Dorada, Negra |
| Namekiano | Gigante, Fusión Namekiana, Namekiano Naranja |
| Majin | Majin Superior, Majin Puro |

Las formas tienen modelos propios: peinados procedurales que brillan, pelaje (Ozaru / SSJ4), cambio de tamaño (Ozaru x4, Gigante x3), aura animada y rayos en las formas más altas. Las razas tienen cola animada, antenas, cuernos o tentáculo. Todo se sincroniza con los demás jugadores.

---

### 5. Combate y técnicas de Ki
* **Ráfaga (`X`)**: proyectil rápido, 15 Ki, enfriamiento corto.
* **Onda (mantener `Z`)**: carga hasta 2,5 s y suelta un rayo; tamaño y daño escalan con la carga.
* **Impulso (`R`)**: dash con 6 ticks de invulnerabilidad.
* **Guardia (mantener `B`)**: bloquea el 65% del daño pagando Ki.
* **Vuelo (`F`)**: corre en el aire para ir más rápido a cambio de más Ki.
* **Zenkai**: Saiyajins e Híbridos ganan TP y se curan al borde de la muerte (una vez cada 10 min).

---

### Animaciones
Funcionan en primera y tercera persona y las ven todos los jugadores. En primera persona los brazos salen del mismo modelo animado que ven los demás, colocados delante de la cámara para que se vean (cargar: puños al frente; onda: manos juntas con la esfera entre ellas; guardia: antebrazos cruzados):

| Acción | Pose |
| :--- | :--- |
| Cargar Ki | Puños apretados a los lados, piernas abiertas |
| Transformarse | Brazos tensos hacia abajo, cabeza hacia atrás, temblor |
| Ráfaga | Brazo derecho lanzado hacia delante |
| Cargar onda | Manos juntas en la cadera con la esfera creciendo (visible en primera persona) |
| Soltar onda | Ambas palmas al frente |
| Guardia | Antebrazos cruzados delante de la cara |
| Volar | Cuerpo inclinado según la velocidad, brazos pegados; en el sitio, piernas flotando |

---

### 6. Objetos Especiales y Coleccionables
* **Semilla del Ermitaño (Senzu Bean)**: Cura instantáneamente toda la vida, hambre y Ki, eliminando efectos de estado negativos.
* **Scouter (Rastreador)**: Dispositivo que permite medir y escanear el Poder de Pelea (BP) de cualquier entidad o jugador que tengas en la mira.
* **Radar del Dragón**: Rastrea el radio cercano (128 bloques) e indica la distancia exacta y orientación cardinal hacia las Esferas del Dragón.
* **Esferas del Dragón (1 a 7 estrellas)**: Objetos místicos únicos coleccionables con gemas de estrellas grabadas.
* **Pesas de Entrenamiento**: Equipo que al llevarse en el inventario reduce levemente la velocidad pero otorga **+50%** de ganancia de TP en combate.

---

## 🕹️ Controles por Defecto

| Tecla | Acción |
| :---: | :--- |
| `C` *(mantener)* | Cargar Ki |
| `G` *(mantener)* | Transformarse en la forma seleccionada |
| `H` | Volver a la forma base |
| `V` | Ficha de personaje (atributos, formas, raza) |
| `F` | Volar |
| `X` | Ráfaga de Ki |
| `Z` *(mantener)* | Cargar y soltar onda de Ki |
| `R` | Impulso |
| `B` *(mantener)* | Guardia |
| `J` | Mostrar/ocultar auras (todas, ocultar la mía, ocultar todas). Solo estético, se guarda en tu config. |

> *Reasignables en **Opciones -> Controles -> Dragon Block**.*

---

## 🛠️ Comandos de Modo Desarrollador (`/dev` o `/dbc`)

Para facilitar el testeo y depuración de mecánicas, se incluye un conjunto de comandos accesibles tanto en un jugador como con permisos de operador:

| Comando | Descripción | Ejemplo |
| :--- | :--- | :--- |
| `/dev tp <cantidad>` | Añade Puntos de Entrenamiento (TP) directamente. | `/dev tp 5000` |
| `/dev tp set <cantidad>` | Establece tus TP a una cantidad fija. | `/dev tp set 10000` |
| `/dev ki full` | Llena la barra de Ki al 100% al instante. | `/dev ki full` |
| `/dev ki <cantidad>` | Establece tu Ki actual a un valor exacto. | `/dev ki 500` |
| `/dev stat <atributo> <valor>` | Fija un atributo (`STR`, `DEX`, `CON`, `WIL`, `MND` o `ALL`). | `/dev stat str 50` o `/dev stat all 100` |
| `/dev race <raza>` | Cambia tu raza de inmediato (`HUMAN`, `SAIYAN`, etc.). | `/dev race saiyan` |
| `/dev form <forma>` | Fuerza una transformación (autocompleta todas). | `/dev form super_saiyan_2` |
| `/dev max` | Atributos a 100, 50,000 TP y Ki al 100% para pruebas rápidas. | `/dev max` |
| `/dev reset` | Restaura todos los atributos y datos a los valores iniciales. | `/dev reset` |
| `/dev help` | Muestra la lista de comandos disponibles en el chat. | `/dev help` |

---

## 📦 Estructura del Código

```text
src/main/java/com/dragonblock/
├── DragonBlockMod.java               # Punto de entrada principal y registro del ciclo de vida
├── client/
│   ├── ClientEvents.java            # Detección de teclas (inputs) y ciclo de cliente
│   ├── ClientModEvents.java         # Registro de renderers, overlays y mapeos de teclas
│   ├── gui/
│   │   └── DBCCharacterScreen.java  # GUI de la ficha de personaje y asignación de TP
│   ├── keybind/
│   │   └── ModKeyBindings.java      # Definición de KeyMappings (C, G, V, F, X)
│   ├── overlay/
│   │   └── DBCKiOverlay.java        # Renderizado de la barra de Ki y HUD en pantalla
│   └── renderer/
│       └── KiBlastRenderer.java     # Renderer luminoso para proyectiles de Ki
└── common/
    ├── data/
    │   ├── PlayerDBCData.java       # Manejador de datos NBT, estadísticas y fórmulas
    │   ├── Race.java                # Enum de razas con modificadores y pasivas
    │   └── Transformation.java      # Enum de transformaciones, multiplicadores y auras
    ├── entity/
    │   └── KiBlastEntity.java       # Entidad proyectil de ráfaga de Ki con daño y explosión
    ├── event/
    │   └── DragonBlockEvents.java   # Eventos de daño, regeneración, muerte, clonación y tick
    ├── item/
    │   ├── DragonBallItem.java      # Esferas de 1 a 7 estrellas
    │   ├── DragonRadarItem.java     # Dispositivo radar buscador
    │   ├── KiAttackItem.java        # Técnica activable de lanzamiento de Ki
    │   ├── ScouterItem.java         # Rastreador de niveles de poder
    │   ├── SenzuBeanItem.java       # Semilla del Ermitaño consumible
    │   └── TrainingWeightsItem.java # Pesas de entrenamiento con bonus de TP
    ├── network/
    │   ├── ModMessages.java         # Canal de red SimpleChannel Forge 1.21.1
    │   └── packet/                  # Paquetes C2S y S2C (Ki, Vuelo, Formas, Stats, Sync)
    └── registry/
        ├── ModCreativeTabs.java     # Pestaña "Dragon Block" en creativo
        ├── ModEntities.java         # Registro diferido de entidades
        ├── ModItems.java            # Registro diferido de ítems
        └── ModSounds.java           # Registro diferido de eventos de sonido
```

---

## 🛠️ Compilación y Desarrollo

### Requisitos
* **Java Development Kit (JDK) 21**
* **Git**

### Comandos de Gradle

* **Compilar el archivo JAR del mod**:
  ```powershell
  .\gradlew.bat jar
  ```
  El artefacto resultante se ubicará en `build/libs/dragonblock-1.0.0.jar`.

* **Ejecutar el cliente de pruebas con Forge**:
  ```powershell
  .\gradlew.bat runClient
  ```

* **Construir y verificar el proyecto completo**:
  ```powershell
  .\gradlew.bat build
  ```
