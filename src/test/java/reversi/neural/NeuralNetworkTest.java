package reversi.neural;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Тесты нейросети
class NeuralNetworkTest {

    // Градиенты из backpropagation должны совпасть с численными на нескольких случайных сетях
    @Test
    void gradientsMatchNumericalGradients() {
        Random rng = new Random(1);
        double eps = 1e-5;

        for (int trial = 0; trial < 5; trial++) {
            // Два скрытых слоя, чтобы проверить передачу ошибки между скрытыми слоями
            NeuralNetwork net = new NeuralNetwork((long) trial, 3, 4, 3, 1);
            double[] input = {
                    rng.nextDouble() * 2 - 1,
                    rng.nextDouble() * 2 - 1,
                    rng.nextDouble() * 2 - 1
            };
            double target = rng.nextDouble() * 2 - 1;

            NeuralNetwork.Gradients analytic = net.gradients(input, target);
            double[][][] w = net.weights();
            double[][] b = net.biases();

            for (int l = 0; l < w.length; l++) {
                for (int j = 0; j < w[l].length; j++) {
                    for (int i = 0; i < w[l][j].length; i++) {
                        double numeric = numericalGradient(net, input, target, w[l][j], i, eps);
                        assertEquals(numeric, analytic.weights()[l][j][i], 1e-7,
                                "weight [" + l + "][" + j + "][" + i + "]");
                    }
                    double numeric = numericalGradient(net, input, target, b[l], j, eps);
                    assertEquals(numeric, analytic.biases()[l][j], 1e-7,
                            "bias [" + l + "][" + j + "]");
                }
            }
        }
    }

    // XOR нельзя разделить одной прямой, поэтому без скрытого слоя он не учится. Проверяем, что сеть его выучивает
    @Test
    void learnsXor() {
        // Входы и ответы в кодировке -1 и 1, как клетки доски в toInput
        double[][] inputs = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        double[] targets = {-1, 1, 1, -1};

        NeuralNetwork net = new NeuralNetwork(42L, 2, 4, 1);
        Random rng = new Random(42);

        // 5000 шагов обучения, каждый раз на случайном из четырёх примеров
        for (int step = 0; step < 5000; step++) {
            int k = rng.nextInt(inputs.length);
            net.train(inputs[k], targets[k], 0.1);
        }

        // tanh физически не может достичь ровно ±1, поэтому требования приблизительны, а не строгое равенство
        for (int k = 0; k < inputs.length; k++) {
            assertEquals(targets[k], net.predict(inputs[k]), 0.2,
                    "XOR of " + inputs[k][0] + " and " + inputs[k][1]);
        }
    }

    // Численная производная ошибки по числу values[index]. Сдвигаем его на +eps и -eps и смотрим на ошибку
    private static double numericalGradient(NeuralNetwork net, double[] input, double target,
                                            double[] values, int index, double eps) {
        double original = values[index];

        values[index] = original + eps;
        double errorPlus = error(net, input, target);

        values[index] = original - eps;
        double errorMinus = error(net, input, target);

        values[index] = original;
        return (errorPlus - errorMinus) / (2 * eps);
    }

    // Та же ошибка, что в backpropagation, E = (y - target)² / 2
    private static double error(NeuralNetwork net, double[] input, double target) {
        double diff = net.predict(input) - target;
        return 0.5 * diff * diff;
    }
}