package reversi.neural;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import reversi.ai.MinimaxAgent;
import reversi.ai.RandomAgent;

// Перебор вариантов обучения. Каждую сеть сохраняет в models/experiments, итоги дописывает в results.csv
public class TrainingExperiments {
    private static final Path DIR = Path.of("models", "experiments");
    private static final Path RESULTS = DIR.resolve("results.csv");

    // Общие параметры, такие же, как в TrainingRun
    private static final double TEST_SHARE = 0.1;
    private static final long SEED = 1;

    // Каждые CHECKPOINT эпох сеть сохраняется и играет партии. Так за один прогон видно, помогают ли лишние эпохи
    private static final int CHECKPOINT = 20;

    // Сколько партий играет каждая обученная сеть против каждого соперника
    private static final int MATCH_GAMES = 400;
    private static final int RANDOM_OPENING_MOVES = 4;

    // Одна сеть, сколько эпох её учить и с каким шагом. При decay шаг к концу обучения уменьшается
    private record Net(int[] layers, int epochs, double learningRate, boolean decay) {
    }

    // Один набор данных и сети, которые на нём учатся. Данные генерируются один раз на весь набор
    private record Plan(int games, int teacherDepth, List<Net> networks) {
    }

    // Уже обученные сети, которые перепроверяем на большем числе партий и на других случайных началах
    private static final List<String> RECHECK = List.of(
            "net_g30000_d4_64-64-32-1_e40.txt",
            "net_g20000_d4_64-64-32-1_decay_e40.txt",
            "net_g20000_d4_64-64-32-1_lr0.003_e40.txt"
    );

    private static final int RECHECK_GAMES = 2000;
    private static final int RECHECK_OPENING_MOVES = 6;
    private static final long RECHECK_SEED = 2;

    // Новых обучений нет, только перепроверка
    private static final List<Plan> PLANS = List.of();

    public static void main(String[] args) throws IOException {
        Files.createDirectories(DIR);
        if (!Files.exists(RESULTS)) {
            Files.writeString(RESULTS, "games,teacher_depth,positions,layers,epochs,learning_rate,"
                    + "generate_s,train_s,test_error,vs_random,vs_minimax1,vs_minimax3\n");
        }

        recheck();

        for (Plan plan : PLANS) {
            try {
                runPlan(plan);
            } catch (IOException | RuntimeException | OutOfMemoryError e) {
                // Один неудачный набор не должен останавливать остальные. Его данные при этом освобождаются
                log("FAILED games " + plan.games() + ", depth " + plan.teacherDepth() + ": " + e);
            }
        }
        log("All done");
    }

    private static void runPlan(Plan plan) throws IOException {
        log("Generating " + plan.games() + " games, teacher Minimax(" + plan.teacherDepth() + ")");
        long start = System.currentTimeMillis();
        DatasetGenerator generator = new DatasetGenerator(new MinimaxAgent(plan.teacherDepth())::search, SEED);
        List<TrainingExample> examples = new ArrayList<>(generator.generate(plan.games()));
        double generateSeconds = seconds(start);
        log(examples.size() + " positions in " + Math.round(generateSeconds) + " s");

        // Тот же порядок действий и те же seed, что в TrainingRun, поэтому сеть получится та же самая
        Collections.shuffle(examples, new Random(SEED));
        int split = (int) (examples.size() * (1 - TEST_SHARE));
        List<TrainingExample> train = examples.subList(0, split);
        List<TrainingExample> test = examples.subList(split, examples.size());

        for (Net netPlan : plan.networks()) {
            String layersText = layersText(netPlan.layers());
            String rateText = netPlan.decay() ? "decay" : "lr" + netPlan.learningRate();
            log("Training " + layersText + ", " + netPlan.epochs() + " epochs, " + rateText);

            NeuralNetwork net = NeuralNetwork.withSeed(SEED, netPlan.layers());
            Trainer trainer = new Trainer(net, netPlan.learningRate(), SEED);
            long trainStart = System.currentTimeMillis();
            for (int epoch = 1; epoch <= netPlan.epochs(); epoch++) {
                double rate = rateFor(netPlan, epoch);
                trainer.setLearningRate(rate);
                double trainError = trainer.trainEpoch(train);
                double testError = trainer.averageError(test);
                System.out.printf("  Epoch %d: train error %.5f, test error %.5f%n", epoch, trainError, testError);

                if (epoch % CHECKPOINT == 0) {
                    String name = "net_g" + plan.games() + "_d" + plan.teacherDepth() + "_" + layersText
                            + "_" + rateText + "_e" + epoch + ".txt";
                    evaluate(net, name, plan, examples.size(), layersText, epoch, rate, generateSeconds,
                            seconds(trainStart), testError);
                }
            }
        }
    }

    // Сохраняет сеть, играет ей партии и дописывает строку в results.csv
    private static void evaluate(NeuralNetwork net, String name, Plan plan, int positions, String layersText, int epochs,
                                 double rate, double generateSeconds, double trainSeconds, double testError) {
        net.save(DIR.resolve(name));

        NeuralAgent agent = new NeuralAgent(net);
        String vsRandom = score(Match.play(agent, new RandomAgent(SEED), MATCH_GAMES, RANDOM_OPENING_MOVES, SEED));
        String vsMinimax1 = score(Match.play(agent, new MinimaxAgent(1), MATCH_GAMES, RANDOM_OPENING_MOVES, SEED));
        String vsMinimax3 = score(Match.play(agent, new MinimaxAgent(3), MATCH_GAMES, RANDOM_OPENING_MOVES, SEED));
        log(name + ": test error " + String.format("%.5f", testError) + ", vs Random " + vsRandom
                + ", vs Minimax(1) " + vsMinimax1 + ", vs Minimax(3) " + vsMinimax3 + " (wins-losses-draws)");

        // Строку дописываем сразу, чтобы результат сохранился, даже если следующий вариант сорвётся
        String row = plan.games() + "," + plan.teacherDepth() + "," + positions + "," + layersText + ","
                + epochs + "," + rate + "," + Math.round(generateSeconds) + "," + Math.round(trainSeconds) + ","
                + String.format("%.5f", testError).replace(',', '.') + "," + vsRandom + "," + vsMinimax1 + "," + vsMinimax3 + "\n";
        try {
            Files.writeString(RESULTS, row, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Например, если results.csv открыт в Excel. Сеть уже сохранена, итог есть в консоли, поэтому идём дальше
            log("Could not write " + RESULTS + ": " + e);
        }
    }

    // Первая половина эпох с исходным шагом, потом шаг в 3 раза меньше, последняя четверть в 10 раз меньше
    private static double rateFor(Net net, int epoch) {
        if (!net.decay() || epoch <= net.epochs() / 2) {
            return net.learningRate();
        }
        if (epoch <= net.epochs() * 3 / 4) {
            return net.learningRate() * 0.3;
        }
        return net.learningRate() * 0.1;
    }

    // Перепроверка готовых сетей. Больше партий, значит меньше случайный разброс в процентах
    private static void recheck() {
        for (String name : RECHECK) {
            Path file = DIR.resolve(name);
            if (!Files.exists(file)) {
                log("Recheck skipped, no file " + file);
                continue;
            }
            NeuralAgent agent = new NeuralAgent(NeuralNetwork.load(file));
            String vsMinimax1 = score(Match.play(agent, new MinimaxAgent(1), RECHECK_GAMES, RECHECK_OPENING_MOVES, RECHECK_SEED));
            String vsMinimax3 = score(Match.play(agent, new MinimaxAgent(3), RECHECK_GAMES, RECHECK_OPENING_MOVES, RECHECK_SEED));
            log("Recheck " + name + ": vs Minimax(1) " + vsMinimax1 + ", vs Minimax(3) " + vsMinimax3
                    + " out of " + RECHECK_GAMES + " (wins-losses-draws)");
        }
    }

    private static String score(Match.Result r) {
        return r.wins() + "-" + r.losses() + "-" + r.draws();
    }

    private static String layersText(int[] layers) {
        return Arrays.toString(layers).replaceAll("[\\[\\] ]", "").replace(',', '-');
    }

    private static void log(String message) {
        System.out.println("[" + LocalTime.now().truncatedTo(ChronoUnit.SECONDS) + "] " + message);
    }

    private static double seconds(long since) {
        return (System.currentTimeMillis() - since) / 1000.0;
    }
}