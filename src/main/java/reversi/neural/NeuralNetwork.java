package reversi.neural;

import java.util.Random;

// Полносвязная нейросеть с активацией tanh на всех слоях. Выход от -1 до 1
public class NeuralNetwork {

    private final int[] layerSizes;

    // weights[l][j][i] это вес от нейрона i слоя l к нейрону j слоя l + 1
    private final double[][][] weights;

    // biases[l][j] это смещение нейрона j слоя l + 1
    private final double[][] biases;

    // Размеры слоёв от входа к выходу, например 64, 32, 16, 1. Веса случайные
    public NeuralNetwork(int... layerSizes) {
        this(new Random(), layerSizes);
    }

    // Для тестов. Одинаковый seed даёт одинаковые начальные веса
    static NeuralNetwork withSeed(long seed, int... layerSizes) {
        return new NeuralNetwork(new Random(seed), layerSizes);
    }

    private NeuralNetwork(Random rng, int... layerSizes) {
        if (layerSizes.length < 2) {
            throw new IllegalArgumentException("Network needs at least an input and an output layer");
        }
        for (int size : layerSizes) {
            if (size <= 0) {
                throw new IllegalArgumentException("Layer size must be positive: " + size);
            }
        }
        this.layerSizes = layerSizes.clone();

        int transitions = layerSizes.length - 1;
        weights = new double[transitions][][];
        biases = new double[transitions][];

        for (int l = 0; l < transitions; l++) {
            int in = layerSizes[l];
            int out = layerSizes[l + 1];
            weights[l] = new double[out][in];
            biases[l] = new double[out];

            // Случайные веса в пределах ±1/√in, чтобы сумма z была небольшой и tanh не упирался в ±1
            // Смещения остаются нулевыми, симметрию нейронов ломают уже веса
            double limit = 1.0 / Math.sqrt(in);
            for (int j = 0; j < out; j++) {
                for (int i = 0; i < in; i++) {
                    weights[l][j][i] = (rng.nextDouble() * 2 - 1) * limit;
                }
            }
        }
    }

    // Выход первого нейрона последнего слоя, от -1 до 1
    public double predict(double[] input) {
        double[][] activations = forward(input);
        return activations[activations.length - 1][0];
    }

    // Прямой проход. Возвращает выходы всех слоёв, activations[0] это сам вход
    // Промежуточные выходы понадобятся для backpropagation
    private double[][] forward(double[] input) {
        if (input.length != layerSizes[0]) {
            throw new IllegalArgumentException(
                    "Expected " + layerSizes[0] + " inputs, got " + input.length);
        }
        double[][] activations = new double[layerSizes.length][];
        activations[0] = input.clone();

        for (int l = 0; l < weights.length; l++) {
            double[] prev = activations[l];
            double[] next = new double[layerSizes[l + 1]];
            for (int j = 0; j < next.length; j++) {
                // z_j = b_j + Σ w_ji * a_i, потом a_j = tanh(z_j)
                double z = biases[l][j];
                for (int i = 0; i < prev.length; i++) {
                    z += weights[l][j][i] * prev[i];
                }
                next[j] = Math.tanh(z);
            }
            activations[l + 1] = next;
        }
        return activations;
    }

    // Один шаг обучения на одном примере. Сдвигает все веса и смещения так, чтобы ответ стал ближе к target
    public void train(double[] input, double target, double learningRate) {
        Gradients g = gradients(input, target);
        for (int l = 0; l < weights.length; l++) {
            for (int j = 0; j < weights[l].length; j++) {
                for (int i = 0; i < weights[l][j].length; i++) {
                    // Шаг против градиента. Если ошибка растёт при росте веса, вес уменьшаем, и наоборот
                    weights[l][j][i] -= learningRate * g.weights()[l][j][i];
                }
                biases[l][j] -= learningRate * g.biases()[l][j];
            }
        }
    }

    // Backpropagation. Градиенты ошибки E = (y - target)² / 2 по всем весам и смещениям
    Gradients gradients(double[] input, double target) {
        double[][] a = forward(input);
        int last = a.length - 1;

        // delta[k][j] это dE/dz, насколько ошибка чувствительна к сумме z нейрона j слоя k
        double[][] delta = new double[a.length][];

        // Выходной нейрон. dE/dy = y - target, dy/dz = 1 - y²
        double y = a[last][0];
        delta[last] = new double[layerSizes[last]];
        delta[last][0] = (y - target) * (1 - y * y);

        // Скрытые слои от выхода к входу. Нейрон i получает долю ошибки от каждого нейрона j
        // следующего слоя через тот же вес, которым на него влиял, и умножает на tanh'
        for (int k = last - 1; k >= 1; k--) {
            delta[k] = new double[layerSizes[k]];
            for (int i = 0; i < layerSizes[k]; i++) {
                double sum = 0;
                for (int j = 0; j < layerSizes[k + 1]; j++) {
                    sum += weights[k][j][i] * delta[k + 1][j];
                }
                delta[k][i] = sum * (1 - a[k][i] * a[k][i]);
            }
        }

        // dE/dw_ji = delta_j * a_i, dE/db_j = delta_j
        double[][][] gw = new double[weights.length][][];
        double[][] gb = new double[weights.length][];
        for (int l = 0; l < weights.length; l++) {
            gw[l] = new double[layerSizes[l + 1]][layerSizes[l]];
            gb[l] = new double[layerSizes[l + 1]];
            for (int j = 0; j < layerSizes[l + 1]; j++) {
                for (int i = 0; i < layerSizes[l]; i++) {
                    gw[l][j][i] = delta[l + 1][j] * a[l][i];
                }
                gb[l][j] = delta[l + 1][j];
            }
        }
        return new Gradients(gw, gb);
    }

    // Нужен, чтобы gradients вернул сразу два массива, градиенты весов и градиенты смещений, в той же форме, что weights и biases
    record Gradients(double[][][] weights, double[][] biases) {
    }

    // Для тестов. Возвращают сами массивы, а не копии, так что тест может менять веса на месте
    double[][][] weights() {
        return weights;
    }

    double[][] biases() {
        return biases;
    }
}