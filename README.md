# 🐉 Dragon Block - Minecraft 1.21.1 (Forge)

Mod de Dragon Ball para **Minecraft Java 1.21.1** desarrollado sobre **Minecraft Forge 52.1.20+** y **Java 21**, fuertemente inspirado en las mecánicas clásicas de **Dragon Block C (1.7.10)**.

---

## ⚡ Características Principales

### 1. Sistema de Ki & Energía
* **Reserva de Ki**: Ki máximo dinámico determinado por el atributo de Voluntad (WIL) y multiplicadores de raza.
* **Carga de Ki (Tecla `C`)**: Concentración de Ki con partículas de aura eléctrica y efectos de sonido en tiempo real.
* **Regeneración Pasiva**: Recuperación continua de Ki dependiente de la raza y el descanso.
* **Vuelo Bukujutsu (Tecla `F`)**: Capacidad de volar por el aire consumiendo Ki paulatinamente, emitiendo una estela visual de condensación.
* **HUD Overlay**: Barra de Ki en pantalla con porcentaje, estado de carga, transformación activa y nivel de poder (BP).

---

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

### 4. Transformaciones (Tecla `G`)
* **Kaio-ken**: Accesible para todas las razas (requiere 10 STR y 10 WIL). Multiplica las estadísticas por **x1.5** con aura roja a cambio de un drenaje constante de Ki.
* **Super Saiyajin (SSJ1)**: Exclusivo de Saiyajins e Híbridos (requiere 20 STR y 20 WIL). Multiplica las estadísticas por **x2.5** con aura dorada resplandeciente y explosión de energía.
* Si el Ki se agota durante una transformación, el jugador vuelve a la forma base por agotamiento.

---

### 5. Técnicas de Energía
* **Ráfaga de Ki (Tecla `X` o Ítem de Técnica)**: Dispara un proyectil de energía Ki concentrada en la dirección de la mirada. Al colisionar con bloques o entidades, detona provocando daño de área proporcional a la Voluntad (WIL) del usuario. (Coste: 15 Ki).

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
| **`C`** *(Mantener)* | Cargar energía Ki |
| **`G`** | Activar transformación / Revertir a forma base |
| **`V`** | Abrir la Ficha de Personaje (Estadísticas y mejoras) |
| **`F`** | Activar / Desactivar vuelo (Bukujutsu) |
| **`X`** | Disparar Ráfaga de Ki |

> *Los atajos pueden reasignarse en cualquier momento desde **Opciones -> Controles -> Dragon Block**.*

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
| `/dev form <forma>` | Fuerza una transformación (`BASE`, `KAIOKEN`, `SSJ`). | `/dev form ssj` |
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
