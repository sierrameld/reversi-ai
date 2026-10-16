package reversi.neural;

// Один обучающий пример. Вход сети и правильный ответ для него
public record TrainingExample(double[] input, double target) {
}