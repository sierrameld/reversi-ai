package reversi.neural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

// Обучает сеть на наборе примеров по эпохам. Эпоха это один проход по всем примерам в случайном порядке
public class Trainer {
    private final NeuralNetwork net;
    private final double learningRate;
    private final Random random;

    public Trainer(NeuralNetwork net, double learningRate, long seed) {
        this.net = net;
        this.learningRate = learningRate;
        this.random = new Random(seed);
    }

    // Обучает несколько эпох, печатает и возвращает среднюю ошибку каждой эпохи
    public double[] train(List<TrainingExample> examples, int epochs) {
        double[] errors = new double[epochs];
        for (int epoch = 0; epoch < epochs; epoch++) {
            errors[epoch] = trainEpoch(examples);
            System.out.printf("Epoch %d: error %.5f%n", epoch + 1, errors[epoch]);
        }
        return errors;
    }

    // Одна эпоха. Каждый пример ровно один раз, порядок каждый раз новый. Возвращает среднюю ошибку
    public double trainEpoch(List<TrainingExample> examples) {
        // Перемешиваем копию, чтобы не менять порядок в списке, который передали
        List<TrainingExample> shuffled = new ArrayList<>(examples);
        Collections.shuffle(shuffled, random);

        double sum = 0;
        for (TrainingExample ex : shuffled) {
            // Ошибку считаем до шага обучения, то есть на примере, который сеть в этой эпохе ещё не видела
            sum += error(ex);
            net.train(ex.input(), ex.target(), learningRate);
        }
        return sum / shuffled.size();
    }

    // Средняя ошибка без обучения. Для проверки на примерах, которых сеть не видела
    public double averageError(List<TrainingExample> examples) {
        double sum = 0;
        for (TrainingExample ex : examples) {
            sum += error(ex);
        }
        return sum / examples.size();
    }

    // Насколько ответ сети далёк от правильного. Та же формула, что в backpropagation, E = (y - target)² / 2
    private double error(TrainingExample ex) {
        double diff = net.predict(ex.input()) - ex.target();
        return 0.5 * diff * diff;
    }
}