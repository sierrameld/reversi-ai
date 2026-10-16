package reversi.neural;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Тесты сбора обучающих примеров
class DatasetGeneratorTest {

    // Оценка должна быть с точки зрения того, кто ходит, то есть того же игрока, что во входе
    @Test
    void targetIsFromPerspectiveOfPlayerToMove() {
        List<TrainingExample> examples = new DatasetGenerator(DatasetGenerator::pieceDifference, 1).generate(20);

        for (TrainingExample ex : examples) {
            assertEquals(64, ex.input().length);
            // Во входе свои фишки +1, чужие -1, поэтому сумма входа это ровно разница фишек для того, кто ходит
            double sum = 0;
            for (double v : ex.input()) {
                sum += v;
            }
            assertEquals(Math.tanh(sum / 10.0), ex.target(), 1e-12);
        }
    }

    // Одна позиция может встретиться в разных партиях, особенно в начале. В данные она должна попасть один раз
    @Test
    void positionsAreNotRepeated() {
        List<TrainingExample> examples = new DatasetGenerator(DatasetGenerator::pieceDifference, 1).generate(20);

        Set<String> unique = new HashSet<>();
        for (TrainingExample ex : examples) {
            unique.add(Arrays.toString(ex.input()));
        }
        assertEquals(examples.size(), unique.size());
    }

    // Вся цепочка от партий до обученной сети, пока с простым учителем. Ошибка на позициях, которых сеть не видела, должна сильно упасть
    // Тест на: учится или запоминает
    @Test
    void networkLearnsFromGeneratedPositions() {
        List<TrainingExample> examples = new ArrayList<>(
                new DatasetGenerator(DatasetGenerator::pieceDifference, 1).generate(50));
        Collections.shuffle(examples, new Random(1));

        // 90% примеров для обучения, 10% откладываем для проверки
        int split = examples.size() * 9 / 10;
        List<TrainingExample> train = examples.subList(0, split);
        List<TrainingExample> test = examples.subList(split, examples.size());

        NeuralNetwork net = NeuralNetwork.withSeed(1, 64, 32, 16, 1);
        Trainer trainer = new Trainer(net, 0.01, 1);

        double before = trainer.averageError(test);
        for (int epoch = 0; epoch < 5; epoch++) {
            trainer.trainEpoch(train);
        }
        double after = trainer.averageError(test);

        assertTrue(after < before / 10, "before " + before + ", after " + after);
    }
}