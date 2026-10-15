package reversi.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import reversi.game.Board;
import reversi.game.Game;
import reversi.game.Move;
import reversi.game.Player;

// Много случайных партий подряд. Проверяет, что движок не ломается на любых позициях
class RandomGamesTest {

    // Сколько клеток пустые
    private static int empty(Board b) {
        int n = 0;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                if (b.get(row, col) == 0) {
                    n++;
                }
            }
        }
        return n;
    }

    // Все ходы одной партии агента против самого себя
    private static List<Move> movesOfOneGame(Agent agent) {
        List<Move> moves = new ArrayList<>();
        Game game = new Game();
        while (!game.isOver()) {
            Move m = agent.chooseMove(game.board(), game.currentPlayer());
            moves.add(m);
            game.play(m);
        }
        return moves;
    }

    @Test
    void fiveHundredRandomGamesFinish() {
        Agent agent = new RandomAgent();
        for (int g = 0; g < 500; g++) {
            Game game = new Game();
            int movesMade = 0;
            while (!game.isOver()) {
                Board b = game.board();
                Player p = game.currentPlayer();
                Move m = agent.chooseMove(b, p);
                assertTrue(b.isValidMove(p, m));
                game.play(m);
                movesMade++;

                Board after = game.board();
                int black = after.count(Player.BLACK);
                int white = after.count(Player.WHITE);
                // Каждая клетка либо чёрная, либо белая, либо пустая
                assertEquals(64, black + white + empty(after));
                // Каждый ход добавляет ровно одну фишку, перевороты общее число не меняют
                assertEquals(4 + movesMade, black + white);
            }
            // Свободных клеток всего 60, больше ходов в партии быть не может
            assertTrue(movesMade <= 60);
        }
    }

    @Test
    void sameSeedGivesSameGame() {
        assertEquals(movesOfOneGame(new RandomAgent(42)), movesOfOneGame(new RandomAgent(42)));
    }
}