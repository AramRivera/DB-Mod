package com.dragonblock.common.npc;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Race;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.entity.NpcEntity;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.npc.Dialogues.Node;
import com.dragonblock.common.npc.Dialogues.Option;
import com.dragonblock.common.registry.ModItems;
import com.dragonblock.common.world.Tournament;
import com.dragonblock.common.world.Worlds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** What each character says and does. Everything here runs on the server. */
public final class NpcDialogues {
    /** First quest of the master: defeat this many hostile creatures. */
    public static final String QUEST_TRIAL = "kaido_trial";
    public static final int TRIAL_KILLS = 10;
    /** Wait before the judge sends a soul back for free, and the TP to skip it. */
    private static final long REVIVE_WAIT = 20 * 120;
    private static final int REVIVE_COST = 300;

    private NpcDialogues() {}

    public static Node root(ServerPlayer p, NpcEntity npc) {
        return switch (npc.getKind()) {
            case MASTER -> master(p, npc);
            case SCIENTIST -> scientist(p, npc);
            case JUDGE -> judge(p, npc);
            case CELESTIAL -> celestial(p, npc);
            case ANNOUNCER -> announcer(p, npc);
            case GUARDIAN -> guardian(p, npc);
            case ELDER -> elder(p, npc);
            case VILLAGER -> villager(p, npc);
        };
    }

    private static Option bye() {
        return new Option("Adiós", Dialogues::close);
    }

    private static Option back(NpcEntity npc) {
        return new Option("Otra cosa...", p -> Dialogues.show(p, npc, root(p, npc)));
    }

    private static void save(ServerPlayer p, PlayerDBCData d) {
        d.saveToPlayer(p);
        ModMessages.syncToTracking(p);
    }

    private static void give(ServerPlayer p, ItemStack stack) {
        if (!p.getInventory().add(stack)) p.drop(stack, false);
    }

    private static void reward(ServerPlayer p) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.2f);
    }

    // ------------------------------------------------------------------ the master

    private static Node master(ServerPlayer p, NpcEntity npc) {
        PlayerDBCData d = PlayerDBCData.get(p);
        List<Option> o = new ArrayList<>();
        String text;
        if (d.hasFlag("kaido_disciple")) {
            text = "Mi alumno favorito. Tu ki ha crecido... pero el camino de un guerrero nunca termina.";
            o.add(new Option("Dame un consejo, maestro", q -> Dialogues.show(q, npc, new Node(tip(q), back(npc), bye()))));
            if (!d.hasLearned(Technique.KIAI)) o.add(new Option("Enséñame algo nuevo", q -> {
                PlayerDBCData qd = PlayerDBCData.get(q);
                if (qd.hasLearned(Technique.KIAI) || !qd.hasFlag("kaido_disciple")) {
                    Dialogues.close(q);
                    return;
                }
                qd.learn(Technique.KIAI);
                save(q, qd);
                reward(q);
                Dialogues.show(q, npc, new Node("Grita desde el estómago y empuja con todo tu ki. Eso es el Kiai: nadie se te acercará sin permiso.", back(npc), bye()));
            }));
        } else if (QUEST_TRIAL.equals(d.getQuest())) {
            int done = Math.min(TRIAL_KILLS, d.getQuestProgress());
            text = done >= TRIAL_KILLS ? "¡Has vuelto, y con el ki más afilado! Lo noto." : "¿Ya de vuelta? Llevas " + done + " de " + TRIAL_KILLS + ". Un guerrero termina lo que empieza.";
            if (done >= TRIAL_KILLS) o.add(new Option("Prueba superada, maestro", q -> {
                PlayerDBCData qd = PlayerDBCData.get(q);
                if (!QUEST_TRIAL.equals(qd.getQuest()) || qd.getQuestProgress() < TRIAL_KILLS) {
                    Dialogues.close(q);
                    return;
                }
                qd.setQuest("");
                qd.setFlag("kaido_disciple");
                qd.addTrainingPoints(400);
                qd.learn(Technique.KAMEHAMEHA);
                if (qd.getEquipped().size() < Technique.MAX_EQUIPPED && !qd.getEquipped().contains(Technique.KAMEHAMEHA)) qd.toggleEquipped(Technique.KAMEHAMEHA);
                save(q, qd);
                reward(q);
                Dialogues.show(q, npc, new Node("Desde hoy eres mi discípulo. Toma 400 TP y mi técnica secreta: junta las manos en la cadera, reúne el ki... y suéltalo de golpe. Ya la tienes en tu barra.", bye()));
            }));
        } else {
            text = "¡Vaya! Un joven con el ki inquieto. Soy Kaido, maestro de artes marciales desde antes de que tu abuelo naciera. ¿Qué te trae a mi casa?";
            o.add(new Option("Quiero ser tu discípulo", q -> {
                PlayerDBCData qd = PlayerDBCData.get(q);
                qd.setQuest(QUEST_TRIAL);
                save(q, qd);
                Dialogues.show(q, npc, new Node("Je, todos dicen eso. Demuéstralo: derrota a " + TRIAL_KILLS + " criaturas hostiles y vuelve a verme. Sin trampas, ¿eh?", bye()));
            }));
        }
        o.add(new Option("🥋 Entrenamiento de Combate Físico", q -> com.dragonblock.common.combat.training.MasterTrainingManager.startTraining(q, "roshi")));
        o.add(new Option("Háblame de las Esferas del Dragón", q -> Dialogues.show(q, npc, new Node(
                "Siete esferas naranjas con estrellas dentro, repartidas por el mundo. Si las reúnes en círculo, aparece el Dragón Eterno y te concede un deseo. "
                        + "Luego vuelan lejos y hay que buscarlas otra vez. La Doctora Vela tiene un radar que las encuentra... dile que vas de mi parte.", back(npc), bye()))));
        o.add(bye());
        return new Node(text, o);
    }

    private static String tip(ServerPlayer p) {
        String[] tips = {
                "Carga tu ki (C) antes de una pelea. Un guerrero sin ki es solo un hombre fuerte.",
                "Las técnicas grandes se cargan; suéltalas cuando la barra pase la marca blanca.",
                "Si dos ondas chocan, mantén la tecla de técnica: gana quien empuja con más ki.",
                "Los saiyajin se hacen más fuertes cada vez que sobreviven al borde de la muerte.",
                "En el planeta del Maestro Celeste la gravedad aplasta... y por eso se entrena el doble.",
                "No mires la luna llena con la cola suelta si no quieres sorpresas."};
        return tips[p.getRandom().nextInt(tips.length)];
    }

    // ------------------------------------------------------------------ the inventor

    private static Node scientist(ServerPlayer p, NpcEntity npc) {
        PlayerDBCData d = PlayerDBCData.get(p);
        List<Option> o = new ArrayList<>();
        if (!d.hasFlag("vela_radar")) o.add(new Option("¿Me prestas un radar del dragón?", q -> {
            PlayerDBCData qd = PlayerDBCData.get(q);
            if (qd.hasFlag("vela_radar")) {
                Dialogues.close(q);
                return;
            }
            qd.setFlag("vela_radar");
            save(q, qd);
            give(q, new ItemStack(ModItems.DRAGON_RADAR.get()));
            give(q, new ItemStack(ModItems.BALL_CAPSULE.get()));
            reward(q);
            Dialogues.show(q, npc, new Node("¡Claro! Toma mi radar y una cápsula para guardar las esferas cuando las encuentres. Ni se te ocurra romperlos, ¿eh? Son prototipos.", back(npc), bye()));
        }));
        if (!d.hasFlag("vela_ship")) o.add(new Option("¿Puedo viajar a otros planetas?", q -> {
            PlayerDBCData qd = PlayerDBCData.get(q);
            if (qd.hasFlag("vela_ship")) {
                Dialogues.close(q);
                return;
            }
            if (!qd.hasFlag("kaido_disciple")) {
                Dialogues.show(q, npc, new Node("¿Al espacio, tú? Primero que el Maestro Kaido te acepte como discípulo... no pienso mandar a un novato a otro planeta.", back(npc), bye()));
                return;
            }
            qd.setFlag("vela_ship");
            save(q, qd);
            give(q, new ItemStack(ModItems.SHIP_CAPSULE.get()));
            reward(q);
            Dialogues.show(q, npc, new Node("¡Así que Kaido te aceptó! Toma: una nave en cápsula. Úsala en el suelo, súbete y elige planeta en la consola. "
                    + "Tiene combustible infinito... creo.", back(npc), bye()));
        }));
        o.add(new Option("¿Qué inventas aquí?", q -> Dialogues.show(q, npc, new Node(
                "Un poco de todo: cápsulas que guardan cosas enormes, radares, rastreadores de ki... Ahora estudio el viaje entre planetas. "
                        + "Dicen que hay un planeta verde de aguas esmeralda, y otro tan pequeño que puedes darle la vuelta en un minuto.", back(npc), bye()))));
        o.add(bye());
        return new Node("¡Hola! Soy la Doctora Vela, inventora y genio, aunque esté feo que lo diga yo. ¿En qué te ayudo?", o);
    }

    // ------------------------------------------------------------------ the judge of souls

    private static Node judge(ServerPlayer p, NpcEntity npc) {
        PlayerDBCData d = PlayerDBCData.get(p);
        if (!d.isDead()) {
            return new Node("¡¿Un vivo en el Otro Mundo?! Esto es muy irregular. Vuelve a tu planeta antes de que lo apunte en el libro.",
                    new Option("Volver a mi mundo", q -> {
                        Dialogues.close(q);
                        Worlds.sendTo(q, Level.OVERWORLD, q.server.overworld().getSharedSpawnPos());
                    }), bye());
        }
        long waited = p.level().getGameTime() - d.getDeathTime();
        List<Option> o = new ArrayList<>();
        o.add(new Option("Quiero volver a la vida", q -> {
            PlayerDBCData qd = PlayerDBCData.get(q);
            if (!qd.isDead()) {
                Dialogues.close(q);
                return;
            }
            long left = REVIVE_WAIT - (q.level().getGameTime() - qd.getDeathTime());
            if (left <= 0) {
                revive(q, qd);
                return;
            }
            List<Option> pay = new ArrayList<>();
            if (qd.getTrainingPoints() >= REVIVE_COST) pay.add(new Option("Pagar " + REVIVE_COST + " TP", r -> {
                PlayerDBCData rd = PlayerDBCData.get(r);
                if (!rd.isDead() || rd.getTrainingPoints() < REVIVE_COST) {
                    Dialogues.close(r);
                    return;
                }
                rd.addTrainingPoints(-REVIVE_COST);
                revive(r, rd);
            }));
            pay.add(back(npc));
            Dialogues.show(q, npc, new Node("Las almas no van y vienen cuando quieren. Espera " + (left / 20) + " segundos... "
                    + "o paga " + REVIVE_COST + " TP de tu entrenamiento y haré una excepción.", pay));
        }));
        o.add(new Option("Quiero entrenar con el Maestro Celeste", q -> {
            Dialogues.close(q);
            Worlds.send(q, Worlds.CELESTIAL);
            q.displayClientMessage(Component.literal("Un pequeño planeta... y una gravedad aplastante.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }));
        o.add(new Option("¿Qué es este lugar?", q -> Dialogues.show(q, npc, new Node(
                "El Otro Mundo. Aquí llegan todas las almas y yo decido adónde van. Los guerreros con buen corazón conservan su cuerpo... "
                        + "por eso llevas esa aureola. Al final del camino dorado vive un maestro que entrena a los muertos más valientes.", back(npc), bye()))));
        return new Node("Siguiente... " + p.getGameProfile().getName() + ". Tu vida ha terminado, pero tu ki aún arde."
                + (waited < REVIVE_WAIT ? "" : " Ya has esperado bastante."), o);
    }

    private static void revive(ServerPlayer p, PlayerDBCData d) {
        Dialogues.close(p);
        d.revive();
        save(p, d);
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(d.getDeathDim()));
        Worlds.sendTo(p, dim, d.getDeathPos());
        reward(p);
        p.displayClientMessage(Component.literal("Vuelves a la vida.").withStyle(ChatFormatting.GOLD), true);
    }

    // ------------------------------------------------------------------ the celestial master

    private static Node celestial(ServerPlayer p, NpcEntity npc) {
        PlayerDBCData d = PlayerDBCData.get(p);
        List<Option> o = new ArrayList<>();
        if (!d.hasLearned(Technique.GENKIDAMA)) o.add(new Option("Enséñame tu técnica", q -> {
            PlayerDBCData qd = PlayerDBCData.get(q);
            if (qd.hasLearned(Technique.GENKIDAMA)) {
                Dialogues.close(q);
                return;
            }
            if (qd.getLevel() < Technique.GENKIDAMA.getMinLevel()) {
                Dialogues.show(q, npc, new Node("Aún no. Tu cuerpo no aguantaría reunir tanta energía. Vuelve cuando seas nivel "
                        + Technique.GENKIDAMA.getMinLevel() + " (ahora " + qd.getLevel() + "). Entrena aquí: con esta gravedad se aprende el doble.", back(npc), bye()));
                return;
            }
            qd.learn(Technique.GENKIDAMA);
            save(q, qd);
            reward(q);
            Dialogues.show(q, npc, new Node("Levanta los brazos al cielo y pide prestada la energía de todo lo que vive. Necesita tiempo... y un corazón limpio. "
                    + "Ya la tienes: equípala en tu ficha.", back(npc), bye()));
        }));
        o.add(new Option("¿Por qué pesa tanto todo aquí?", q -> Dialogues.show(q, npc, new Node(
                "Mi planeta es pequeño pero muy denso: diez veces la gravedad de la Tierra. Te mueves más lento y saltas menos... "
                        + "pero cada golpe que das aquí te enseña el doble.", back(npc), bye()))));
        o.add(new Option(d.isDead() ? "Volver con el Gran Juez" : "Volver a casa", q -> {
            Dialogues.close(q);
            if (PlayerDBCData.get(q).isDead()) Worlds.send(q, Worlds.OTHERWORLD);
            else Worlds.sendTo(q, Level.OVERWORLD, q.server.overworld().getSharedSpawnPos());
        }));
        o.add(bye());
        return new Node("¡Jo, jo! Un visitante. Soy el Maestro Celeste, guardián de este rinconcito del cielo. ¿Vienes a entrenar?", o);
    }

    // ------------------------------------------------------------------ the tournament host

    private static Node announcer(ServerPlayer p, NpcEntity npc) {
        List<Option> o = new ArrayList<>();
        if (Tournament.isFighting(p)) {
            return new Node("¿Qué haces aquí abajo? ¡Te esperan en el ring!", bye());
        }
        if (Tournament.isQueued(p)) {
            o.add(new Option("Retirar mi inscripción", q -> {
                Tournament.leave(q);
                Dialogues.show(q, npc, new Node("Hecho. Vuelve cuando quieras: el torneo nunca cierra.", bye()));
            }));
        } else {
            o.add(new Option("Inscribirme en el torneo", q -> {
                PlayerDBCData qd = PlayerDBCData.get(q);
                if (qd.isDead() || !qd.isRaceChosen()) {
                    Dialogues.close(q);
                    return;
                }
                Tournament.join(q);
                Dialogues.show(q, npc, new Node(Tournament.isFighting(q) ? "¡Tenemos rival! ¡Sube al ring!"
                        : "¡Inscrito! Hay " + Tournament.queueSize() + " guerreros en la lista. Te llamaré cuando te toque: no te vayas lejos.", bye()));
            }));
        }
        o.add(new Option("¿Cuáles son las reglas?", q -> Dialogues.show(q, npc, new Node(
                "Combates uno contra uno en el ring. Pierdes si tocas el suelo de fuera, si te dejan K.O. o si huyes. Nadie de fuera puede intervenir. "
                        + "Si en tres minutos no hay vencedor, deciden los jueces por la salud que os quede. El ganador se lleva " + Tournament.REWARD + " TP.", back(npc), bye()))));
        o.add(bye());
        String now = Tournament.current(p.server);
        return new Node("¡Damas y caballeros, bienvenidos al Gran Torneo de Artes Marciales! " + (now == null ? "Ahora mismo el ring está libre." : "En el ring: " + now + ".")
                + " ¿Quieres demostrar lo que vales?", o);
    }

    // ------------------------------------------------------------------ the guardian of the sky palace

    private static Node guardian(ServerPlayer p, NpcEntity npc) {
        PlayerDBCData d = PlayerDBCData.get(p);
        List<Option> o = new ArrayList<>();
        if (!d.hasLearned(Technique.TELEPORT)) o.add(new Option("Enséñame a moverme como el rayo", q -> {
            PlayerDBCData qd = PlayerDBCData.get(q);
            if (qd.hasLearned(Technique.TELEPORT)) {
                Dialogues.close(q);
                return;
            }
            if (qd.getLevel() < Technique.TELEPORT.getMinLevel()) {
                Dialogues.show(q, npc, new Node("Tu ki aún es demasiado ruidoso para esconderse en el de otro. Vuelve al nivel "
                        + Technique.TELEPORT.getMinLevel() + " (ahora " + qd.getLevel() + ").", back(npc), bye()));
                return;
            }
            qd.learn(Technique.TELEPORT);
            save(q, qd);
            reward(q);
            Dialogues.show(q, npc, new Node("Dos dedos en la frente. Busca un ki que conozcas... y deja que tu cuerpo lo siga. "
                    + "Usada sin más te lleva a donde miras; agachado, a cualquier ki conocido de cualquier mundo. Cuesta mucho ki: úsala con cabeza.", back(npc), bye()));
        }));
        o.add(new Option("¿Qué es este lugar?", q -> Dialogues.show(q, npc, new Node(
                "El Palacio del Cielo. Desde aquí se vigila toda la Tierra. Pocos suben la torre a pie... los que lo hacen ya son guerreros de verdad.", back(npc), bye()))));
        o.add(new Option("¿Cómo encuentro a alguien lejos?", q -> Dialogues.show(q, npc, new Node(
                "Cada ser vivo tiene un ki único. Habla con la gente y aprenderás a reconocer el suyo; a los guerreros los sentirás siempre. "
                        + "Con la teletransportación puedes llegar a ellos aunque estén en otro planeta.", back(npc), bye()))));
        o.add(bye());
        return new Node("Has llegado hasta lo más alto. Bienvenido al palacio. Soy el Guardián de la Tierra.", o);
    }

    // ------------------------------------------------------------------ the elder of the green planet

    private static final String ELDER_AWAKENED = "elder_awakened";
    private static final int AWAKEN_TP = 1500;

    private static Node elder(ServerPlayer p, NpcEntity npc) {
        PlayerDBCData d = PlayerDBCData.get(p);
        List<Option> o = new ArrayList<>();
        if (!d.hasFlag(ELDER_AWAKENED)) o.add(new Option("Despierta mi poder oculto", q -> {
            PlayerDBCData qd = PlayerDBCData.get(q);
            if (qd.hasFlag(ELDER_AWAKENED)) {
                Dialogues.close(q);
                return;
            }
            qd.setFlag(ELDER_AWAKENED);
            qd.addTrainingPoints(AWAKEN_TP);
            boolean namek = qd.getRace() == Race.NAMEKIAN && !qd.hasLearned(Technique.MAKANKOSAPPO);
            if (namek) qd.learn(Technique.MAKANKOSAPPO);
            save(q, qd);
            reward(q);
            Dialogues.show(q, npc, new Node("Acércate... pon tu cabeza bajo mi mano. Sí... había mucho poder dormido aquí dentro. (+" + AWAKEN_TP + " TP)"
                    + (namek ? " Y como hijo de este planeta, te entrego la técnica del rayo perforador." : ""), bye()));
        }));
        o.add(new Option("Háblame de tu pueblo", q -> Dialogues.show(q, npc, new Node(
                "Somos un pueblo tranquilo: bebemos agua y cuidamos los árboles de copa redonda. Hace mucho tuvimos que huir de nuestro mundo... "
                        + "y aquí, bajo tres soles, volvimos a empezar. Los soldados del Imperio patrullan ahora nuestras tierras. Ten cuidado.", back(npc), bye()))));
        o.add(bye());
        return new Node(d.hasFlag(ELDER_AWAKENED) ? "Tu poder ya está despierto, joven. Ahora solo el entrenamiento lo hará crecer."
                : "Bienvenido, viajero. Siento un gran poder dormido en ti... Soy Tsuru, el más anciano de este planeta.", o);
    }

    private static Node villager(ServerPlayer p, NpcEntity npc) {
        String[] lines = {
                "¡Un visitante! El Gran Anciano vive en la casa grande del centro. Seguro que quiere conocerte.",
                "Aquí nunca anochece: con tres soles, ¿quién necesita dormir?",
                "Los soldados del Imperio llevan aparatos en el ojo que miden el ki. Si escondes el tuyo... no te ven venir.",
                "Nuestros árboles dan sombra a todo el pueblo. Por favor, no los derribes con tus rayos.",
                "Dicen que este planeta también guarda esferas mágicas... pero esa es otra historia."};
        return new Node(lines[Math.floorMod(npc.getUUID().hashCode(), lines.length)], bye());
    }
}
