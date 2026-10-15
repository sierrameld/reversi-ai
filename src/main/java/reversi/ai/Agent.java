package reversi.ai;

import reversi.game.Board;
import reversi.game.Move;
import reversi.game.Player;

// Любой, кто умеет выбирать ход. Случайный игрок, минимакс, нейросеть.
public interface Agent {
    // Выбрать ход за p на доске b. Вызывается только если у p есть ходы
    Move chooseMove(Board b, Player p);

    // Имя для окна и таблицы турнира
    String name();
}