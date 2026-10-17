package reversi.neural;

import java.util.ArrayList;
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

    // Одна партия. Играл ли первый агент чёрными, случайное начало и сколько фишек у каждого цвета в конце
    public record GameResult(boolean firstIsBlack, String opening, int blackDiscs, int whiteDiscs) {
    }

    public static Result play(Agent a, Agent b, int games, int randomOpeningMoves, long seed) {
        return summarize(playGames(a, b, games, randomOpeningMoves, seed));
    }

    // Победы, поражения и ничьи первого агента по списку партий
    public static Result summarize(List<GameResult> games) {
        int wins = 0;
        int losses = 0;
        int draws = 0;
        for (GameResult g : games) {
            int firstDiscs = g.firstIsBlack() ? g.blackDiscs() : g.whiteDiscs();
            int secondDiscs = g.firstIsBlack() ? g.whiteDiscs() : g.blackDiscs();
            if (firstDiscs > secondDiscs) {
                wins++;
            } else if (firstDiscs < secondDiscs) {
                losses++;
            } else {
                draws++;
            }
        }
        return new Result(wins, losses, draws);
    }

    // Те же партии, что в play, но каждая по отдельности
    public static List<GameResult> playGames(Agent a, Agent b, int games, int randomOpeningMoves, long seed) {
        Random random = new Random(seed);
        List<GameResult> results = new ArrayList<>();

        for (int g = 0; g < games; g++) {
            Game game = new Game();
            // Минимакс и сеть всегда выбирают один и тот же ход, без случайного начала все партии были бы одинаковыми
            StringBuilder opening = new StringBuilder();
            for (int k = 0; k < randomOpeningMoves && !game.isOver(); k++) {
                List<Move> moves = game.validMoves();
                Move m = moves.get(random.nextInt(moves.size()));
                opening.append(k == 0 ? "" : " ").append(m);
                game.play(m);
            }

            // В чётных партиях первый агент играет чёрными, в нечётных белыми
            Player colorA = g % 2 == 0 ? Player.BLACK : Player.WHITE;
            while (!game.isOver()) {
                Player p = game.currentPlayer();
                Agent mover = p == colorA ? a : b;
                game.play(mover.chooseMove(game.board(), p));
            }

            results.add(new GameResult(colorA == Player.BLACK, opening.toString(),
                    game.board().count(Player.BLACK), game.board().count(Player.WHITE)));
        }
        return results;
    }
}