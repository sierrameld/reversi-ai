package reversi.neural;

import java.util.List;
import java.util.Random;

import reversi.ai.Agent;
import reversi.game.Game;
import reversi.game.Move;
import reversi.game.Player;

// Серия партий между двумя агентами. Цвета чередуются, первые ходы случайные, чтобы партии не повторялись
public class Match {
    // Итог серии с точки зрения первого агента
    public record Result(int wins, int losses, int draws) {
    }

    public static Result play(Agent a, Agent b, int games, int randomOpeningMoves, long seed) {
        Random random = new Random(seed);
        int wins = 0;
        int losses = 0;
        int draws = 0;

        for (int g = 0; g < games; g++) {
            Game game = new Game();
            // Минимакс и сеть всегда выбирают один и тот же ход, без случайного начала все партии были бы одинаковыми
            for (int k = 0; k < randomOpeningMoves && !game.isOver(); k++) {
                List<Move> moves = game.validMoves();
                game.play(moves.get(random.nextInt(moves.size())));
            }

            // В чётных партиях первый агент играет чёрными, в нечётных белыми
            Player colorA = g % 2 == 0 ? Player.BLACK : Player.WHITE;
            while (!game.isOver()) {
                Player p = game.currentPlayer();
                Agent mover = p == colorA ? a : b;
                game.play(mover.chooseMove(game.board(), p));
            }

            Player winner = game.board().getWinner();
            if (winner == null) {
                draws++;
            } else if (winner == colorA) {
                wins++;
            } else {
                losses++;
            }
        }
        return new Result(wins, losses, draws);
    }
}