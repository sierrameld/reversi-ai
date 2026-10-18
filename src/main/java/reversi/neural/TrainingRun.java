package reversi.neural;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import reversi.ai.MinimaxAgent;

// Программа обучения. Собирает позиции с оценками минимакса, учит сеть и сохраняет её в models/network.txt нейросети
public class TrainingRun {
    // Все параметры обучения в одном месте. Они же идут в README
    private static final int GAMES = 40000;
    private static final int TEACHER_DEPTH = 5;
    private static final int[] LAYERS = {64, 128, 64, 1};
    private static final int EPOCHS = 60;
    // Начальный шаг обучения. К концу он уменьшается, см. rateFor
    private static final double LEARNING_RATE = 0.01;
    private static final double TEST_SHARE = 0.1;
    private static final long SEED = 1;
    private static final Path OUTPUT = Path.of("models", "network.txt");

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        System.out.println("Generating positions from " + GAMES + " random games, teacher Minimax(" + TEACHER_DEPTH + ")...");
        DatasetGenerator generator = new DatasetGenerator(new MinimaxAgent(TEACHER_DEPTH)::search, SEED);
        List<TrainingExample> examples = new ArrayList<>(generator.generate(GAMES));
        System.out.printf("%d positions in %.0f s%n", examples.size(), seconds(start));

        // Часть позиций откладываем. На них сеть не учится, по ним видно, учится она или запоминает
        Collections.shuffle(examples, new Random(SEED));
        int split = (int) (examples.size() * (1 - TEST_SHARE));
        List<TrainingExample> train = examples.subList(0, split);
        List<TrainingExample> test = examples.subList(split, examples.size());
        System.out.println("Training on " + train.size() + ", testing on " + test.size());

        NeuralNetwork net = NeuralNetwork.withSeed(SEED, LAYERS);
        Trainer trainer = new Trainer(net, LEARNING_RATE, SEED);
        System.out.printf("Before training: test error %.5f%n", trainer.averageError(test));

        long trainStart = System.currentTimeMillis();
        for (int epoch = 1; epoch <= EPOCHS; epoch++) {
            trainer.setLearningRate(rateFor(epoch));
            double trainError = trainer.trainEpoch(train);
            System.out.printf("Epoch %d: train error %.5f, test error %.5f%n", epoch, trainError, trainer.averageError(test));
        }
        System.out.printf("Training took %.0f s%n", seconds(trainStart));

        Files.createDirectories(OUTPUT.getParent());
        net.save(OUTPUT);
        System.out.println("Saved to " + OUTPUT.toAbsolutePath());
    }

    // Decay. Первая половина эпох с исходным шагом, потом шаг в 3 раза меньше, последняя четверть в 10 раз меньше
    private static double rateFor(int epoch) {
        if (epoch <= EPOCHS / 2) {
            return LEARNING_RATE;
        }
        if (epoch <= EPOCHS * 3 / 4) {
            return LEARNING_RATE * 0.3;
        }
        return LEARNING_RATE * 0.1;
    }

    private static double seconds(long since) {
        return (System.currentTimeMillis() - since) / 1000.0;
    }
}