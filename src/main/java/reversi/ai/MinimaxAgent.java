package reversi.ai;

import reversi.game.Board;
import reversi.game.Move;
import reversi.game.Player;

// Выбирает ход минимаксом с альфа-бета отсечением. Пока заглушка
public class MinimaxAgent implements Agent {
    @Override
    public Move chooseMove(Board b, Player p) {
        throw new UnsupportedOperationException("MinimaxAgent is not implemented yet");
    }

    @Override
    public String name() {
        return "Minimax";
    }
}