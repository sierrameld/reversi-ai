package reversi.ai;

import java.util.List;

import reversi.game.Board;
import reversi.game.Move;
import reversi.game.Player;

// Выбирает ход минимаксом. Перебирает ходы обоих игроков на depth ходов вперёд
public class MinimaxAgent implements Agent {
    // Делитель для tanh. Переводит оценку PositionEvaluator в диапазон от -1 до 1
    private static final double SCALE = 50;

    private final int depth;
    private final PositionEvaluator evaluator = new PositionEvaluator();

    public MinimaxAgent(int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("Depth must be at least 1: " + depth);
        }
        this.depth = depth;
    }

    // Ценность позиции для p, когда ходит p. От -1 до 1, больше значит лучше для p
    public double search(Board b, Player p) {
        return value(b, p, depth);
    }

    // Ход с наибольшей ценностью для p
    @Override
    public Move chooseMove(Board b, Player p) {
        Move bestMove = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (Move m : b.getValidMoves(p)) {
            // После хода позиция оценивается с точки зрения соперника, поэтому минус
            double v = -value(b.applyMove(p, m), p.opponent(), depth - 1);
            if (v > bestValue) {
                bestValue = v;
                bestMove = m;
            }
        }
        return bestMove;
    }

    // Минимакс. Ценность позиции для p, когда ходит p, если смотреть на d ходов вперёд
    private double value(Board b, Player p, int d) {
        if (b.isGameOver()) {
            Player winner = b.getWinner();
            if (winner == null) {
                return 0;
            }
            return winner == p ? 1 : -1;
        }
        if (d == 0) {
            // Глубже не смотрим, оцениваем эвристикой. tanh даёт строго меньше 1 по модулю
            return Math.tanh(evaluator.evaluate(b, p) / SCALE);
        }
        List<Move> moves = b.getValidMoves(p);
        if (moves.isEmpty()) {
            // Пропуск хода. Ходит соперник, глубина не тратится
            return -value(b, p.opponent(), d);
        }
        double best = Double.NEGATIVE_INFINITY;
        for (Move m : moves) {
            // Лучший ответ соперника для него это худший для нас, поэтому минус
            double v = -value(b.applyMove(p, m), p.opponent(), d - 1);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    @Override
    public String name() {
        return "Minimax(" + depth + ")";
    }
}