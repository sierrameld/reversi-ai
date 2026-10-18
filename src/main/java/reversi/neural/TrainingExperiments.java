package reversi.neural;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.file.StandardCopyOption;
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
    private static final int CHECKPOINT = 30;

    // Сколько партий играет каждая обученная сеть против каждого соперника
    private static final int MATCH_GAMES = 400;
    private static final int RANDOM_OPENING_MOVES = 4;

    // Одна сеть, сколько эпох её учить и с каким шагом. При decay шаг к концу обучения уменьшается
    private record Net(int[] layers, int epochs, double learningRate, boolean decay) {
    }

    // Один набор данных и сети, которые на нём учатся. Данные генерируются один раз на весь набор
    private record Plan(int games, int teacherDepth, List<Net> networks) {
    }

    // Сети ночного опыта. Перепроверка в конце, в тех же условиях, в которых итоговая сеть дала 823-1126-51 против Minimax(3)
    private static final List<String> RECHECK = List.of(
            "net_g3000_d6_64-64-32-1_decay_e30.txt",
            "net_g3000_d6_64-64-32-1_decay_e60.txt",
            "net_g80000_d4_64-64-32-1_decay_e30.txt",
            "net_g80000_d4_64-64-32-1_decay_e60.txt",
            "net_g3000_d6_64-256-256-1_decay_e30.txt",
            "net_g3000_d6_64-256-256-1_decay_e60.txt",
            "net_g3000_d6_64-256-256-1_decay_e90.txt",
            "net_g80000_d4_64-128-64-1_decay_e30.txt",
            "net_g80000_d4_64-128-64-1_decay_e60.txt",
            "net_g40000_d5_64-128-64-1_decay_e30.txt",
            "net_g40000_d5_64-128-64-1_decay_e60.txt"
    );

    private static final int RECHECK_GAMES = 2000;
    private static final int RECHECK_OPENING_MOVES = 6;
    private static final long RECHECK_SEED = 2;

    // Хороший учитель. Сеть и обучение как у вариативности, отличаются только позиции
    private static final Net TEACHER = new Net(new int[]{64, 64, 32, 1}, 60, 0.01, true);

    // Большая вариативность. Сеть и обучение как у хорошего учителя
    private static final Net VARIETY = new Net(new int[]{64, 64, 32, 1}, 60, 0.01, true);

    // Большая нейросеть. Позиции те же, что у хорошего учителя, отличаются только сеть и число эпох
    private static final Net BIG_NETWORK = new Net(new int[]{64, 256, 256, 1}, 90, 0.01, true);

    // Средняя нейросеть. Позиции те же, что у вариативности. Проверяет, раскрывается ли сеть побольше на большом наборе
    private static final Net MEDIUM_NETWORK = new Net(new int[]{64, 128, 64, 1}, 60, 0.01, true);

    // Позиции от Minimax(6), их мало. На них учатся хороший учитель и большая нейросеть
    private static final Plan TEACHER_AND_BIG_NETWORK_DATA = new Plan(3000, 6, List.of(TEACHER, BIG_NETWORK));

    // Позиции от Minimax(4), их почти втрое больше, чем у итоговой сети
    private static final Plan VARIETY_AND_MEDIUM_NETWORK_DATA = new Plan(80000, 4, List.of(VARIETY, MEDIUM_NETWORK));

    // Баланс по времени и потенциалу относительно остальных. Учитель (Minimax(5)) точнее, чем у вариативности, а позиций вдвое меньше, сеть та же средняя
    private static final Plan PERFECT_BALANCED_DATA = new Plan(40000, 5, List.of(MEDIUM_NETWORK));

    public static void main(String[] args) throws IOException {
        Files.createDirectories(DIR);
        if (!Files.exists(RESULTS)) {
            Files.writeString(RESULTS, "games,teacher_depth,positions,layers,epochs,learning_rate,"
                    + "generate_s,train_s,test_error,vs_random,vs_minimax1,vs_minimax3\n");
        }

        // Аргумент запуска выбирает набор позиций. Так оба набора считаются одновременно, каждый в своём окне
        List<Plan> plans = switch (args.length > 0 ? args[0] : "") {
            case "teacher_and_big_network" -> List.of(TEACHER_AND_BIG_NETWORK_DATA);
            case "variety_and_medium_network" -> List.of(VARIETY_AND_MEDIUM_NETWORK_DATA);
            case "perfect_balanced" -> List.of(PERFECT_BALANCED_DATA);
            default -> throw new IllegalArgumentException(
                    "Program argument must be teacher_and_big_network, variety_and_medium_network or balanced");
        };

        for (Plan plan : plans) {
            try {
                runPlan(plan);
            } catch (IOException | RuntimeException | OutOfMemoryError e) {
                // Один неудачный набор не должен останавливать остальные. Его данные при этом освобождаются
                log("FAILED games " + plan.games() + ", depth " + plan.teacherDepth() + ": " + e);
            }
        }

        // Перепроверка после обучения. Сети другого окна, которые ещё не готовы, просто пропускаются
        recheck();
        log("All done");
    }

    private static void runPlan(Plan plan) throws IOException {
        long start = System.currentTimeMillis();
        List<TrainingExample> examples = new ArrayList<>(loadOrGenerate(plan));
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

    // Позиции с оценками учителя. Если файл с ними уже есть, читаем его, иначе генерируем и сохраняем
    private static List<TrainingExample> loadOrGenerate(Plan plan) throws IOException {
        Path file = DIR.resolve("data_g" + plan.games() + "_d" + plan.teacherDepth() + ".txt");
        if (Files.exists(file)) {
            log("Loading " + file);
            return readExamples(file);
        }
        log("Generating " + plan.games() + " games, teacher Minimax(" + plan.teacherDepth() + ")");
        DatasetGenerator generator = new DatasetGenerator(new MinimaxAgent(plan.teacherDepth())::search, SEED);
        List<TrainingExample> examples = generator.generate(plan.games());
        writeExamples(file, examples);
        return examples;
    }

    // Одна позиция на строку. 64 клетки знаками +, - и точка, через пробел оценка учителя
    private static void writeExamples(Path file, List<TrainingExample> examples) throws IOException {
        // Сначала пишем во временный файл. Если запись оборвётся, недописанный файл не примут за готовый
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter out = Files.newBufferedWriter(temp)) {
            for (TrainingExample e : examples) {
                StringBuilder line = new StringBuilder();
                for (double v : e.input()) {
                    line.append(v > 0 ? '+' : v < 0 ? '-' : '.');
                }
                line.append(' ').append(e.target()).append('\n');
                out.write(line.toString());
            }
        }
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    private static List<TrainingExample> readExamples(Path file) throws IOException {
        List<TrainingExample> examples = new ArrayList<>();
        try (BufferedReader in = Files.newBufferedReader(file)) {
            String line;
            while ((line = in.readLine()) != null) {
                double[] input = new double[64];
                for (int i = 0; i < 64; i++) {
                    char c = line.charAt(i);
                    input[i] = c == '+' ? 1 : c == '-' ? -1 : 0;
                }
                examples.add(new TrainingExample(input, Double.parseDouble(line.substring(65))));
            }
        }
        return examples;
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