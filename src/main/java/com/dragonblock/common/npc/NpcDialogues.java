package com.dragonblock.common.npc;

import com.dragonblock.common.data.PlayerDBCData;
import com.dragonblock.common.data.Technique;
import com.dragonblock.common.entity.NpcEntity;
import com.dragonblock.common.network.ModMessages;
import com.dragonblock.common.npc.Dialogues.Node;
import com.dragonblock.common.npc.Dialogues.Option;
import com.dragonblock.common.registry.ModItems;
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
            qd.setFlag("vela_radar");
            save(q, qd);
            give(q, new ItemStack(ModItems.DRAGON_RADAR.get()));
            give(q, new ItemStack(ModItems.BALL_CAPSULE.get()));
            reward(q);
            Dialogues.show(q, npc, new Node("¡Claro! Toma mi radar y una cápsula para guardar las esferas cuando las encuentres. Ni se te ocurra romperlos, ¿eh? Son prototipos.", back(npc), bye()));
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
            long left = REVIVE_WAIT - (q.level().getGameTime() - qd.getDeathTime());
            if (left <= 0) {
                revive(q, qd);
                return;
            }
            List<Option> pay = new ArrayList<>();
            if (qd.getTrainingPoints() >= REVIVE_COST) pay.add(new Option("Pagar " + REVIVE_COST + " TP", r -> {
                PlayerDBCData rd = PlayerDBCData.get(r);
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
}
