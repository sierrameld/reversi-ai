package reversi.ai;

import java.util.List;
import java.util.Random;

import reversi.game.Board;
import reversi.game.Move;
import reversi.game.Player;

// Выбирает случайный легальный ход. Самый слабый соперник, с ним сравниваем остальных
public class RandomAgent implements Agent {
    private final Random random = new Random();

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