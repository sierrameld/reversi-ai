package reversi.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleBiFunction;

import org.junit.jupiter.api.Test;

import reversi.game.Board;
import reversi.game.Game;
import reversi.game.Move;
import reversi.game.Player;

// Тесты оценщика и минимакса, замер скорости search и партии против случайного агента
class MinimaxAgentTest {

    // Позиция и тот, кто в ней ходит
    private record Position(Board board, Player player) {
    }

    // Позиции из случайных партий с ходами от first до last, повторяемые за счёт seed
    private static List<Position> randomPositions(int games, int first, int last) {
        List<Position> positions = new ArrayList<>();
        Agent random = new RandomAgent(2026);
        for (int g = 0; g < games; g++) {
            Game game = new Game();
            int moveNumber = 0;
            while (!game.isOver()) {
                if (moveNumber >= first && moveNumber <= last) {
                    positions.add(new Position(game.board(), game.currentPlayer()));
                }
                game.play(random.chooseMove(game.board(), game.currentPlayer()));
                moveNumber++;
            }
        }
        return positions;
    }

    // Обычный минимакс без отсечения, как эталон для сравнения с альфа-бета.
    // 50 должно совпадать с SCALE в MinimaxAgent
    private static double plainMinimax(Board b, Player p, int d) {
        if (b.isGameOver()) {
            Player winner = b.getWinner();
            return winner == null ? 0 : (winner == p ? 1 : -1);
        }
        if (d == 0) {
            return Math.tanh(new PositionEvaluator().evaluate(b, p) / 50);
        }
        List<Move> moves = b.getValidMoves(p);
        if (moves.isEmpty()) {
            return -plainMinimax(b, p.opponent(), d);
        }
        double best = Double.NEGATIVE_INFINITY;
        for (Move m : moves) {
            best = Math.max(best, -plainMinimax(b.applyMove(p, m), p.opponent(), d - 1));
        }
        return best;
    }

    @Test
    void initialPositionIsEven() {
        assertEquals(0.0, new PositionEvaluator().evaluate(Board.initial(), Player.BLACK));
    }

    @Test
    void evaluationIsSymmetric() {
        PositionEvaluator evaluator = new PositionEvaluator();
        for (Position pos : randomPositions(20, 0, 60)) {
            Board b = pos.board();
            assertEquals(-evaluator.evaluate(b, Player.BLACK), evaluator.evaluate(b, Player.WHITE), 1e-12);
        }
    }

    @Test
    void searchIsExactAtGameEnd() {
        MinimaxAgent minimax = new MinimaxAgent(3);
        Game game = new Game();
        Agent random = new RandomAgent(1);
        while (!game.isOver()) {
            game.play(random.chooseMove(game.board(), game.currentPlayer()));
        }
        Board end = game.board();
        Player winner = end.getWinner();
        if (winner == null) {
            assertEquals(0.0, minimax.search(end, Player.BLACK));
        } else {
            assertEquals(1.0, minimax.search(end, winner));
            assertEquals(-1.0, minimax.search(end, winner.opponent()));
        }
    }

    @Test
    void searchStaysInsideRange() {
        // Подходит туда, где его ждёт ИИ чат, как ToDoubleBiFunction
        ToDoubleBiFunction<Board, Player> teacher = new MinimaxAgent(3)::search;
        for (Position pos : randomPositions(10, 0, 60)) {
            double v = teacher.applyAsDouble(pos.board(), pos.player());
            assertTrue(v >= -1 && v <= 1, "search out of range: " + v);
        }
    }

    @Test
    void alphaBetaGivesSameValueAsPlainMinimax() {
        for (int depth = 1; depth <= 3; depth++) {
            MinimaxAgent minimax = new MinimaxAgent(depth);
            for (Position pos : randomPositions(5, 0, 60)) {
                double expected = plainMinimax(pos.board(), pos.player(), depth);
                assertEquals(expected, minimax.search(pos.board(), pos.player()), 1e-12);
            }
        }
    }

    @Test
    void searchAtDepthThreeIsFastEnough() {
        MinimaxAgent minimax = new MinimaxAgent(3);
        List<Position> positions = randomPositions(30, 20, 40);
        // Прогрев, первые вызовы Java выполняет медленнее, их не считаем
        for (Position pos : positions) {
            minimax.search(pos.board(), pos.player());
        }
        long start = System.nanoTime();
        for (Position pos : positions) {
            minimax.search(pos.board(), pos.player());
        }
        double averageMs = (System.nanoTime() - start) / 1e6 / positions.size();
        System.out.printf("search depth 3: %.2f ms on average over %d mid-game positions%n",
                averageMs, positions.size());
        assertTrue(averageMs < 50, "search is too slow: " + averageMs + " ms");
    }

    @Test
    void minimaxBeatsRandomAgent() {
        int wins = 0;
        int losses = 0;
        int draws = 0;
        for (int g = 0; g < 100; g++) {
            Agent minimax = new MinimaxAgent(3);
            Agent random = new RandomAgent(g);
            // Половину партий минимакс играет чёрными, половину белыми
            Player minimaxColor = g % 2 == 0 ? Player.BLACK : Player.WHITE;
            Game game = new Game();
            while (!game.isOver()) {
                Player p = game.currentPlayer();
                Agent agent = p == minimaxColor ? minimax : random;
                Move m = agent.chooseMove(game.board(), p);
                assertTrue(game.board().isValidMove(p, m), "illegal move " + m);
                game.play(m);
            }
            Player winner = game.board().getWinner();
            if (winner == null) {
                draws++;
            } else if (winner == minimaxColor) {
                wins++;
            } else {
                losses++;
            }
        }
        System.out.printf("Minimax(3) vs Random: %d wins, %d losses, %d draws%n", wins, losses, draws);
        assertTrue(wins >= 90, "minimax won only " + wins + " of 100");
    }
}