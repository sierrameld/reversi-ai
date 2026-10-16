package reversi.neural;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Тесты цикла обучения
class TrainerTest {

    private static final List<TrainingExample> XOR = List.of(
            new TrainingExample(new double[]{-1, -1}, -1),
            new TrainingExample(new double[]{-1, 1}, 1),
            new TrainingExample(new double[]{1, -1}, 1),
            new TrainingExample(new double[]{1, 1}, -1)
    );

    // Ошибка за последнюю эпоху должна быть во много раз меньше, чем за первую
    @Test
    void errorFallsOverEpochs() {
        NeuralNetwork net = NeuralNetwork.withSeed(42, 2, 4, 1);
        Trainer trainer = new Trainer(net, 0.1, 42);

        // trainEpoch вместо train, чтобы тест не печатал 1500 строк
        double first = trainer.trainEpoch(XOR);
        double last = first;
        for (int epoch = 1; epoch < 1500; epoch++) {
            last = trainer.trainEpoch(XOR);
        }

        assertTrue(last < first / 10, "first " + first + ", last " + last);
    }

    // averageError только считает и не должна менять сеть
    @Test
    void averageErrorDoesNotTrain() {
        NeuralNetwork net = NeuralNetwork.withSeed(42, 2, 4, 1);
        Trainer trainer = new Trainer(net, 0.1, 42);

        double before = net.predict(XOR.get(0).input());
        trainer.averageError(XOR);
        double after = net.predict(XOR.get(0).input());

        assertEquals(before, after, 0);
    }
}