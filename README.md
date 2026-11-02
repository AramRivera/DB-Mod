# 🐉 Dragon Block - Minecraft 1.21.1 (Forge)

Mod de Dragon Ball para **Minecraft Java 1.21.1** desarrollado sobre **Minecraft Forge 52.1.20+** y **Java 21**, fuertemente inspirado en las mecánicas clásicas de **Dragon Block C (1.7.10)**.

---

## ⚡ Características Principales

### 1. Ki, vida y aura
* **Ki**: máximo según Voluntad (WIL) y raza. Se carga manteniendo `C` y se regenera solo.
* **Vida (cuerpo)**: reserva propia que crece con la Constitución (CON) efectiva, así que transformarte te hace más resistente. Se muestra en una **barra roja** en lugar de los corazones; los corazones vanilla solo reflejan el porcentaje, por eso nunca se apilan. La comida, pociones y regeneración siguen funcionando.
* **Aura**: tres capas de llamas, más **ancha y alta** que el cuerpo, con lenguas que suben, chispas que se desprenden y un anillo de presión en el suelo. Al empezar a cargar **se hincha** y late; al soltar se apaga poco a poco. **Crece con el poder de pelea y el multiplicador de la forma.** Se puede ocultar con `J`.
* **Vuelo (`F`)**: velocidad cercana a la de creativo. En el aire el aura se apaga; solo al **volar rápido** (correr volando) aparece un aura ceñida al cuerpo y una **estela de energía** detrás que ven todos. El cuerpo se inclina según la velocidad.
* **Velocidad**: la Destreza da hasta +35% al caminar, **sin el efecto de "poción de velocidad"** en el campo de visión.

---

### Interfaz
Estilo Dragon Ball sobrio: paneles azul noche, naranja de gi como acento, dorado de esfera del dragón para lo importante y la esfera de cuatro estrellas en los encabezados.
* **Ficha (`V`)**: se abre con animación y muestra una **vista 3D de tu personaje** que sigue el ratón (con pelo, aura y cola). Pestañas **Atributos, Formas, Técnicas y Apariencia** con subrayado animado, barras que crecen al mejorar y tarjetas con descripción al pasar el ratón.
* **Creación de personaje**: la primera vez que entras eliges raza y personalizas piel, ojos, peinado, color de pelo y ropa, viéndolo en 3D al momento. El visor se **gira arrastrando**, la **rueda hace zoom** hasta la cara y el clic derecho lo reinicia. Las transformaciones no se pueden previsualizar: son sorpresa. La apariencia se puede cambiar después en la pestaña *Apariencia*; la raza es permanente.
* **HUD**: barras que se mueven con suavidad, "fantasma" de daño en la vida, barra de Ki que brilla al cargar, anillo de carga en la mira, título al transformarte, TP ganados flotando y la **barra de técnicas** a la derecha.

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
Cada raza tiene **su propio cuerpo**; la skin de Minecraft no se usa. Las texturas se generan con `tools/gen_race_skins.py` como capas que el mod tiñe con los colores elegidos en el creador:

| Raza | Cuerpo |
| :--- | :--- |
| **Saiyajin** | Guerrero con el torso al aire, **shorts** (negros por defecto) y descalzo; cola y peinado *Guerrero* por defecto. |
| **Humano / Híbrido** | Gi de artes marciales con camiseta interior, faja, muñequeras y botas. El color del gi se elige. |
| **Namekiano** | Piel verde con **manchas musculares** en los brazos, gi morado sin mangas, faja azul, antenas y **orejas puntiagudas**. |
| **Arcosiano** | Cuerpo acorazado liso con **gemas** brillantes en cabeza, hombros, pecho y rodillas, cola gruesa y cuernos. |
| **Majin** | Cuerpo rosado, pantalón holgado, cinturón y puños dorados, botas y apéndice en la cabeza. |


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
Elige una forma en la pestaña **Formas** de la ficha (`V`) y mantén `G` para transformarte. **Estando transformado la `G` no hace nada**: primero vuelve a la forma base con `H`.

* **Requisitos**: nivel (suma de atributos) y, para formas avanzadas, **25% de maestría** en la forma anterior.
* **Maestría (0-100%)**: sube estando transformado y combatiendo. Reduce hasta un 60% el consumo de Ki.
* **Agotamiento**: sin Ki vuelves a la forma base.
* **Ozaru**: si eres Saiyajin con la cola suelta y **miras la luna llena** de noche, te transformas solo. También puedes elegirlo y mantener `G` cualquier noche a cielo abierto. Al salir el sol (o al enrollar la cola) vuelves a la normalidad. `/dev form oozaru` lo fuerza de día para pruebas.
* **Peinados base** (creador de personaje): *Guerrero* (picos salvajes en todas direcciones, el de Saiyan por defecto), *Llama* (todo hacia arriba), *Rebelde*, *Corto*, *Melena* o sin pelo, en el color que elijas. Son diseños propios, no copias de personajes.
* **Cambio de color**: al cargar una transformación el pelo late con el color nuevo, cada vez más rápido, hasta quedarse; al volver a la base se apaga suavemente al color original.
* **Peinados de forma**: 8 diseños propios por nivel (no copias del pelo de personajes concretos), hechos con mechones triangulares escalonados que salen de un volumen de pelo: *Flare*, *Heavy* (grados musculosos), *Ridge* (cresta afilada con un mechón suelto), *Mane* (melena hasta la cintura), *Wild* (oscuro y salvaje), *Sleek* (hacia atrás), *Spire* (cresta central) y *Crest* (plateado y sereno). Se definen en `assets/dragonblock/hair/*.json`.

#### Nombres e IDs
Una sola forma de cada cosa, con IDs coherentes para los comandos:

| ID | Forma | | ID | Forma |
| :--- | :--- | :--- | :--- | :--- |
| `ssj1` | Super Saiyajin 1 | | `ssj_god` | Super Saiyajin Dios |
| `ssj2` | Super Saiyajin 2 | | `ssj_blue` | Super Saiyajin Blue |
| `ssj3` | Super Saiyajin 3 | | `ssj_blue_evolved` | Blue Evolución |
| `ssj4` | Super Saiyajin 4 | | `ultra_ego`, `beast` | Ultra Ego, Bestia |
| `oozaru`, `golden_oozaru` | Ozaru | | `ui_sign`, `ui_mastered` | Instinto Superior |
| `kaioken`, `kaioken_x10`, `kaioken_x20` | Kaio-ken | | `potential` | Potencial Liberado |

Las partidas antiguas se migran solas (los antiguos "Grado 2/3" y "SSJ Pleno" pasan a SSJ1).

**Tamaño y músculo**: las formas más poderosas te hacen más alto y musculoso: Blue Evolución (x1,1 de altura, +20% de músculo), Namekiano Naranja, Forma Final 100%, SSJ4, Ultra Ego, Bestia.

#### La esencia de cada forma
Cada forma tiene su secuencia de transformación (más larga y violenta cuanto más poderosa), partículas, ojos y una mecánica propia:

| Forma | Cómo se ve | Qué hace distinto |
| :--- | :--- | :--- |
| Super Saiyajin 1 | Pelo dorado *Flare*, ojos verde azulado, destello dorado | Base dorada equilibrada |
| Super Saiyajin 2 | Cresta afilada con mechón, **rayos constantes**, chispas | +15% daño de Ki |
| Super Saiyajin 3 | **El pelo crece** hasta la cintura mientras cargas, terremoto, rayos | +15% golpe, consumo alto |
| Dios | Pelo *Sleek* rojizo, brasas, columna de luz | Cura el cuerpo poco a poco, técnicas más baratas |
| Blue / Blue Evolución | Pelo y aura cian, destellos; Evolución con pelo *Heavy*, **más alto (x1,1) y musculoso** | Control perfecto: técnicas 30% más baratas |
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
3. **Escombros**: **rocas redondeadas** con la textura del suelo se arrancan con polvo y **flotan girando** a tu alrededor (se ven desde cualquier ángulo).
   En las formas más poderosas (**Super Saiyajin 3 en adelante**) además **caen rayos** del cielo a tu alrededor: pocos en SSJ3, una tormenta en Ultra Ego o la Forma Negra; las formas divinas (Dios, Instinto Superior) no tienen tormenta.
4. **Implosión**: justo antes del final el aura se contrae un instante.
5. **Estallido**: explosión de energía, los escombros salen despedidos, onda en el suelo, destello en pantalla y el **nombre de la forma** aparece en grande.

**Formas poderosas, versión cinemática** (Super Saiyajin 2 en adelante, y las de tipo cataclismo, divina o gigante):
* **Cámara de cine** (solo para quien se transforma): sale de tus ojos a un plano fijo de frente que te encuadra entero, aguanta la explosión y vuelve; aparecen franjas negras de cine. Se apaga con `cinematicTransformations = false` en `config/dragonblock-client.toml`.
* **El cielo se oscurece** para todos los que estén cerca, teñido del color del aura, y un retumbar que sube de tono acompaña la carga.
* **Intocable mientras te transformas**: durante la carga nadie te puede golpear (jugadores, mobs ni técnicas). Si sueltas la `G` a medias, esperas 3 s para volver a intentarlo.
* **Onda expansiva**: al completar, todo lo que esté cerca sale despedido; es solo un empujón, nunca quita vida. Con pared de polvo y, desde SSJ3, un trueno.
* **Temblor de cámara**: solo las formas cumbre (Blue Evolución, Ultra Ego, Instinto Superior y Bestia) sacuden la cámara de todos en 20 bloques.
* **Líneas de velocidad** de anime al final de la carga y un **fotograma de impacto** (blanco y negro) en el estallido.
* **El cuerpo cambia de color** en las formas que lo hacen (Forma Dorada, Forma Negra, Namekiano Naranja, Majin Puro) y vuelve a tu color al revertir.
* **Sentir el ki**: los demás jugadores reciben un aviso tipo "Sientes un ki colosal al noreste (54 m)".

La carga dura al menos 1,5 s y más en las formas poderosas (SSJ1 unos 3 s, SSJ3 unos 6 s, Blue unos 8 s); la maestría la acorta como mucho un 40%. Cada forma tiene su estilo: *Destello* (el pelo parpadea), *Oleada* (rayos), *Cataclismo* (larga, terremoto, el pelo crece), *Divina* (serena, columna de luz), *Gigante* (creces poco a poco), *Tensión* (llamarada roja). Con más maestría la carga es más corta.


---


| Línea | Formas |
| :--- | :--- |
| Universal | Kaio-ken (x1, x10, x20, drenan vida), Instinto Superior (Presagio / Dominado) |
| Humano / Namekiano / Híbrido | Potencial Liberado |
| Saiyajin / Híbrido | Super Saiyajin 1, 2, 3, Dios, Blue, Blue Evolución |
| Saiyajin (con cola) | Ozaru (luna llena), Ozaru Dorado, SSJ4, Ultra Ego |
| Híbrido | Bestia |
| Arcosiano | Segunda, Tercera, Final, Final 100%, Dorada, Negra |
| Namekiano | Gigante, Fusión Namekiana, Namekiano Naranja |
| Majin | Majin Superior, Majin Puro |

Las formas tienen modelos propios: peinados procedurales que brillan, pelaje (Ozaru / SSJ4), cambio de tamaño (Ozaru x4, Gigante x3), aura animada y rayos en las formas más altas. Las razas tienen cola animada, antenas, cuernos o tentáculo. Todo se sincroniza con los demás jugadores.

---

### 5. Combate y técnicas de Ki
**El poder se nota**: cuanto más fuerte es la forma (y tu poder de pelea), más **grandes** son las técnicas, más grande la explosión y más tiembla la pantalla de los que estén cerca; transformado, tus técnicas llevan un **halo del color de tu aura**. El daño ya crece con los atributos de la forma.

**Choque de ondas**: si dos ondas o esferas de jugadores distintos se encuentran, se quedan trabadas unos segundos en un punto que crece y lanza rayos. Mientras dura, **mantener `X` empuja** (gasta Ki). Al terminar gana la más fuerte (con un poco de suerte): la otra explota y la ganadora sigue, más débil. Las ráfagas simples se anulan al chocar. El Makankosappo y el Kienzan atraviesan, no chocan.

**Golpes con peso**: un puñetazo de alguien mucho más fuerte que tú te manda a volar, con estampido y sonido de impacto; cuanta más diferencia de poder, más lejos.

Las técnicas se aprenden con TP en la pestaña **Técnicas** de la ficha y se equipan (hasta 8) en la **barra de la derecha**. Con **`Ctrl` + rueda** eliges cuál usar (mientras mantienes `Ctrl` se ven todos los nombres) y con **`X`** la usas: las de carga se cargan manteniendo y se disparan al soltar (una marca blanca indica la carga mínima). Cada una tiene su postura de carga, su forma de salir y su recarga, y todo se ve igual en primera y tercera persona y para los demás jugadores.

| Técnica | Carga | Cómo se ve y qué hace |
| :--- | :--- | :--- |
| Ráfaga de Ki | instantánea | Esfera rápida; manteniendo `X` disparas en ráfaga. |
| Kamehameha | 2,5 s | Manos ahuecadas en la cadera con la esfera azul creciendo; la onda sale pegada a las palmas. |
| Galick Ho | 2 s | Torso girado, palmas al lado, chispas violetas; onda violeta. |
| Masenko | 1,5 s | Manos sobre la frente; destello amarillo rápido. |
| Final Flash | 4 s | Brazos abiertos de par en par con una esfera en cada mano; el rayo más grueso. |
| Makankosappo | 3 s | Dos dedos en la frente; rayo fino con espiral violeta que **atraviesa** enemigos. |
| Rayo Mortal | instantánea | Un dedo; rayo fino y muy rápido que atraviesa. |
| Kienzan | 1,5 s | Disco giratorio sobre la mano; corta a través de los enemigos. |
| Genkidama | 7 s | Ambos brazos al cielo; una esfera colosal crece sobre tu cabeza y cae lenta con una explosión enorme. |
| Big Bang Attack | 1,2 s | Una palma extendida; esfera grande y explosiva. |
| Kiai | 0,8 s | Grito que empuja y daña a todos alrededor, con onda expansiva. |
| Taiyoken | 0,5 s | Destello que ciega a quien te mire (pantalla blanca para jugadores, ceguera para mobs, que pierden tu rastro). |
| Teletransportación | 0,75 s | Dos dedos en la frente; apareces detrás de quien miras o donde apuntes (hasta 64 bloques). |

* **Impulso (`R`)**: dash con 6 ticks de invulnerabilidad.
* **Guardia (mantener `B`)**: bloquea el 65% del daño pagando Ki.
* **Vuelo (`F`)**: corre en el aire para ir más rápido a cambio de más Ki.
* **Zenkai**: Saiyajins e Híbridos ganan TP y se curan al borde de la muerte (una vez cada 10 min).

---

### Animaciones
Funcionan en primera y tercera persona y las ven todos los jugadores. En primera persona los brazos salen del mismo modelo animado que ven los demás, colocados delante de la cámara para que se vean. Las poses **nunca saltan**: cada jugador mezcla suavemente de una pose a otra y de vuelta a caminar.

| Acción | Pose |
| :--- | :--- |
| Cargar Ki | Puños apretados a los lados, piernas abiertas, respiración; la pantalla tiembla un poco más cuanto más fuerte eres |
| Transformarse | Brazos tensos hacia abajo, cabeza hacia atrás, temblor |
| Técnicas | Postura de carga propia de cada técnica (ver la tabla de técnicas) y golpe al soltar: palmas al frente, dedos que apuntan, lanzamiento por encima de la cabeza, brazos que se abren con el grito |
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

### 7. Accesorios (botón en el inventario o `K`)
Un segundo inventario con cuatro casillas: **Visor, Torso, Muñecas y Cintura**, que se abre con el botón de la esfera junto al inventario normal (o con `K`). Los accesorios solo funcionan **puestos**: en la mano no hacen nada. Lo que llevas se ve en tu personaje para todos.

| Accesorio | Casilla | Efecto |
| :--- | :--- | :--- |
| **Scouter** | Visor | Lente verde en tu ojo izquierdo: fija lo que miras, cuenta su poder dígito a dígito con un pitido, marca la distancia y una flecha apunta al ki más fuerte cercano. **Si mide un poder abrumador (25 veces el tuyo y más de 20.000), estalla y lo pierdes.** |
| **Pesas de entrenamiento** | Torso | +50% de TP en combate, te mueves un poco más lento. Se ve un chaleco oscuro. |
| **Muñequeras de peso** | Muñecas | +20% de TP. Bandas rojas en los antebrazos. |
| **Cinturón de combate** | Cintura | El ki se regenera un 25% más rápido. Cinturón con hebilla dorada. |

### 8. Esferas del Dragón y el Dragón Eterno
Las siete esferas se **colocan como bloques**, con sus estrellas a la vista. Una vez puestas **no se pueden romper** (ni minar, ni explotar, ni empujar): solo la **Cápsula de esferas** las recoge (se craftea con 4 lingotes de hierro en cruz y una perla de ender en el centro). El ritual exige colocarlas **en círculo**: las siete al mismo nivel, a entre 2 y 4 bloques de un centro y sin tocarse. Entonces **usa una** y empieza:
1. Las esferas laten cada vez más fuerte, **el cielo se vuelve casi negro** para todos en 256 bloques y caen rayos alrededor.
2. Un **pilar de luz** sale disparado de las esferas.
3. El **Dragón Eterno** brota de la luz con la cabeza por delante y se enrosca hasta el cielo, ondulando, con astas, bigotes y ojos rojos que brillan, y **mira a quien lo invocó**.
4. Se abre el **diálogo del deseo** (si lo cierras, vuelve a usar una esfera). Deseos: más fuerza (TP), riquezas, semillas del ermitaño, dominar tu poder (+40% de maestría) o renacer (eliges raza y apariencia de nuevo).
5. Se marcha deshaciéndose en luz desde la cola, y las esferas **suben y salen volando** en siete direcciones: aparecen colocadas entre 400 y 1.500 bloques de distancia. El **Radar del Dragón** las encuentra estén donde estén.

El dragón es un diseño propio de dragón oriental (modelo en `assets/dragonblock/dragon.json`, generado con `tools/gen_dragon_json.py` y revisado en Blender con `tools/preview_dragon_blender.py`). Para probar: `/dev dragon` pone las siete esferas a tu alrededor e invoca.

---

### 9. Personajes y misiones
Personajes propios sobre los arquetipos del género; clic derecho para hablar (diálogo con texto escrito letra a letra, opciones con el ratón o con los números 1-9).

| Personaje | Dónde | Qué ofrece |
| :--- | :--- | :--- |
| **Maestro Kaido** | Su casa de tejado rojo, junto al spawn de la Tierra | La **prueba del maestro**: derrota 10 criaturas hostiles. Recompensa: 400 TP, el Kamehameha y ser su discípulo (luego te enseña el Kiai y te da consejos). |
| **Doctora Vela** | Su laboratorio-cúpula junto a la casa del maestro | El **radar del dragón** y una cápsula de esferas; cuando el maestro te acepta, la **nave espacial**. |
| **El Gran Juez** | Otro Mundo | Te devuelve a la vida (gratis tras 2 minutos o pagando 300 TP) o te manda a entrenar con el Maestro Celeste. |
| **Maestro Celeste** | Planeta Celeste | Te enseña la Genkidama (nivel 90) y te explica su gravedad. |
| **El Presentador** | Junto al ring del Torneo | Te inscribe en el **Gran Torneo de Artes Marciales** y explica las reglas. |
| **El Guardián** | Palacio del Cielo, en lo alto de la torre | Te enseña la **Teletransportación** (nivel 50). |
| **Gran Anciano Tsuru** | Aldea de Namek | **Despierta tu poder oculto** una vez (+1500 TP); a los namekianos les enseña el Makankosappo. |
| **Aldeanos namekianos** | Aldea de Namek | Consejos y rumores del planeta. |

Hablar con alguien te enseña a **reconocer su ki**: luego puedes viajar hasta él con la teletransportación.

### 10. El Otro Mundo
Al morir, tu alma llega al **Otro Mundo**: cielo dorado sobre un mar de nubes, la gran plaza del Gran Juez con su **salón de columnas** y escritorio gigante, alfombra roja y un **camino dorado** serpenteante de 700 bloques con faroles. Al final del camino, una puerta dorada lleva al **Planeta Celeste**. Llevas una **aureola** que todos ven. El juez te devuelve a donde caíste. Si te caes de las nubes, vuelves a la plaza.

### Lugares de la Tierra
- **Casa del Maestro Kaido y laboratorio de la Doctora Vela**, junto al spawn.
- **Gran Torneo de Artes Marciales**: ring de piedra elevado, gradas, salón y puerta con dos torres de pagoda.
- **Torre y Palacio del Cielo**: una torre blanca con escalera hasta las nubes (y=228) que sostiene una plataforma con templo de cúpula, palmeras y el Guardián.

### 11. Planetas y la nave espacial
- **Tierra**: el mundo normal.
- **Namek**: mundo propio generado con cuevas y minerales, cielo verde pálido, hierba turquesa, agua esmeralda y árboles de copa redonda; tres soles, **nunca anochece**. Patrullan soldados del Imperio.
- **Namek** tiene ahora una **aldea** de cúpulas blancas alrededor de una plaza (donde aterrizan las naves) y la gran cúpula del Gran Anciano.
- **Planeta Celeste**: pequeño (ahora el doble de ancho, con árboles y un sendero), flotando en un cielo violeta. **Gravedad x2,5** (caes rápido, saltas poco, te mueves lento) y **el entrenamiento vale el doble de TP**.

La **nave en cápsula** se usa en el suelo; clic derecho para subir y elegir destino en la consola de navegación. Despega con fuego y humo, cruza el **hiperespacio** (estrellas que pasan a toda velocidad) y aterriza suave en el destino, contigo dentro. Agachado y golpéala vacía para guardarla. Las almas no pueden pilotar naves.

### 12. Enemigos
| Enemigo | Dónde | Cómo pelea |
| :--- | :--- | :--- |
| **Bandido del Desierto** | Tierra | Cuerpo a cuerpo con su sable. |
| **Soldado del Imperio** | Namek | Armadura blanca con hombreras y scouter; levanta el brazo para apuntar y dispara ráfagas de Ki. Puede soltar un scouter o la **Baliza imperial**. |
| **Androide de Combate** | Namek (raro) | Muy resistente, golpea fuerte y dispara láser rojo por el visor. |
| **Kryos, Emperador Galáctico** | Se invoca con la Baliza imperial | Jefe con barra de vida. Ráfagas y esferas explosivas. **A mitad de vida se transforma**: intocable unos segundos entre rayos y aura, y vuelve dorado, más grande, más rápido y más fuerte. |

| **Germinado** | Tierra y Namek, en grupos | Pequeño y rápido, salta sobre ti. Malherido, **se agarra y explota** (se hincha y parpadea en blanco: ¡aléjate!). |
| **Dinosaurio** | Selvas, sabanas, llanuras y badlands | Muerde fuerte y ruge. Suelta carne. |
| **Invasor Saiyajin** | Tierra (raro) | Armadura, scouter y ráfagas de Ki. **Bajo la luna llena** se convierte en un **Ozaru gigante** con barra de jefe, golpes enormes y ráfagas por la boca. |
| **Majin Glotón** | Se invoca con el **Capullo Majin** (receta: ojo de ender, lana rosa, bloque de slime) | Jefe rosa: **se regenera** si lo dejas tranquilo, salta y aplasta el suelo, y al 66% y 33% de vida **suelta Pequeños Majin**. |

Derrotar enemigos da TP a quien da el golpe final; cuentan para la prueba del Maestro Kaido.

### 13. Zonas seguras (sin PvP)
- **Todo el Otro Mundo** y un **círculo alrededor de cada personaje** (casa del maestro, laboratorio, Palacio del Cielo, aldea de Namek, Planeta Celeste) son zonas seguras: los jugadores no se pueden dañar, las técnicas no les afectan y las explosiones no rompen nada. Solo los operadores pueden construir o romper allí.
- **Mientras hablas con un personaje** nadie puede hacerte daño (ni tú a nadie).
- Arriba aparece **ZONA SEGURA · SIN PVP** mientras estás dentro.
- El ring del Torneo **no** es zona segura (es para pelear), pero sus edificios están protegidos.

### 14. Teletransportación instantánea entre mundos
Usa **Teletransportación agachado**: dos dedos en la frente y sientes los ki conocidos, en **cualquier mundo**: los jugadores conectados (ordenados por poder) y los personajes con los que has hablado. Elige uno y apareces a su lado. Cuesta **un cuarto de tu ki** y tiene **30 s** de espera. Los vivos solo llegan a los vivos y las almas solo dentro del Otro Mundo. Sin agacharte sigue siendo el salto corto de siempre.

### 15. El Gran Torneo de Artes Marciales
Inscríbete con el Presentador. Cuando hay dos guerreros en la lista, se anuncia el combate a todo el servidor y ambos aparecen en el ring con una cuenta atrás. Pierdes si **tocas el suelo fuera del ring**, si te dejan **K.O.** (no mueres: el golpe final termina el combate) o si huyes o te desconectas. Nadie de fuera puede intervenir. A los 3 minutos deciden los jueces por la salud restante. El ganador se lleva **300 TP**.

---

## 🕹️ Controles por Defecto

| Tecla | Acción |
| :---: | :--- |
| `C` *(mantener)* | Cargar Ki |
| `G` *(mantener)* | Transformarse en la forma seleccionada (solo desde la forma base) |
| `H` | Volver a la forma base |
| `V` | Ficha de personaje (atributos, formas, técnicas, apariencia) |
| `F` | Volar |
| `X` *(mantener)* | Usar la técnica elegida (cargar y soltar) |
| `Ctrl` + rueda | Elegir técnica en la barra de la derecha |
| `R` | Impulso |
| `B` *(mantener)* | Guardia |
| `K` | Accesorios (scouter, pesas, muñequeras, cinturón) |
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
| `/dev form <id>` | Fuerza una transformación (Tab autocompleta). | `/dev form ssj2` |
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
│   │   └── ModKeyBindings.java      # Definición de KeyMappings (C, G, H, V, F, X, R, B, J)
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
