package reversi.ai;

import java.util.List;
import java.util.Random;

import reversi.game.Board;
import reversi.game.Move;
import reversi.game.Player;

// Выбирает случайный легальный ход. Самый слабый соперник, с ним сравниваем остальных
public class RandomAgent implements Agent {
    private final Random random;

    // Каждый раз разные партии
    public RandomAgent() {
        random = new Random();
    }

    // Одинаковый seed даёт одинаковую последовательность ходов, партии можно повторить
    public RandomAgent(long seed) {
        random = new Random(seed);
    }

    @Override
    public Move chooseMove(Board b, Player p) {
        List<Move> moves = b.getValidMoves(p);
        return moves.get(random.nextInt(moves.size()));
    }

    @Override
    public String name() {
        return "Random";
    }
}