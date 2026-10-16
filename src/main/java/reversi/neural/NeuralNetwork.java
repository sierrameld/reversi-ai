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
    NeuralNetwork(long seed, int... layerSizes) {
        this(new Random(seed), layerSizes);
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
}