package reversi.neural;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleBiFunction;

import reversi.ai.RandomAgent;
import reversi.game.Board;
import reversi.game.Game;
import reversi.game.Move;
import reversi.game.Player;

// Собирает обучающие примеры. Играет случайные партии, а каждую позицию оценивает учитель
public class DatasetGenerator {
    // Учитель получает доску и того, кто ходит, и возвращает оценку позиции для него от -1 до 1
    private final ToDoubleBiFunction<Board, Player> teacher;
    private final RandomAgent randomAgent;

    public DatasetGenerator(ToDoubleBiFunction<Board, Player> teacher, long seed) {
        this.teacher = teacher;
        this.randomAgent = new RandomAgent(seed);
    }

    // Играет games случайных партий и возвращает примеры из всех встреченных позиций без повторов
    public List<TrainingExample> generate(int games) {
        List<TrainingExample> examples = new ArrayList<>();
        // Одна и та же позиция встречается во многих партиях, особенно в начале. Её оцениваем один раз
        Set<String> seen = new HashSet<>();

        for (int g = 0; g < games; g++) {
            Game game = new Game();
            while (!game.isOver()) {
                Board board = game.board();
                Player p = game.currentPlayer();

                // Пример по договору это пара (b.toInput(p), оценка для p), где p тот, кто сейчас ходит
                double[] input = board.toInput(p);
                if (seen.add(Arrays.toString(input))) {
                    examples.add(new TrainingExample(input, teacher.applyAsDouble(board, p)));
                }

                Move m = randomAgent.chooseMove(board, p);
                game.play(m);
            }
        }
        return examples;
    }

    // Простой учитель для проверки, пока нет минимакса. Разница фишек, сжатая в диапазон от -1 до 1
    public static double pieceDifference(Board b, Player p) {
        return Math.tanh((b.count(p) - b.count(p.opponent())) / 10.0);
    }
}