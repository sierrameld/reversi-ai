package reversi.neural;

import java.util.LinkedHashMap;
import java.util.Map;

import reversi.ai.Agent;
import reversi.game.Board;
import reversi.game.Move;
import reversi.game.Player;

// Выбирает ход по оценкам нейросети. Пробует каждый ход и смотрит, насколько хороша позиция после него
public class NeuralAgent implements Agent {
    private final NeuralNetwork net;

    public NeuralAgent(NeuralNetwork net) {
        this.net = net;
    }

    // Оценка каждого легального хода для p от -1 до 1, чем больше, тем лучше для p
    public Map<Move, Double> rateMoves(Board b, Player p) {
        // LinkedHashMap сохраняет порядок ходов, как в getValidMoves, чтобы при равных оценках выбор был предсказуемым
        Map<Move, Double> ratings = new LinkedHashMap<>();
        for (Move m : b.getValidMoves(p)) {
            ratings.put(m, rate(b.applyMove(p, m), p));
        }
        return ratings;
    }

    // Ход с самой высокой оценкой. При равных оценках первый из них
    @Override
    public Move chooseMove(Board b, Player p) {
        Move best = null;
        double bestRating = Double.NEGATIVE_INFINITY;
        for (Map.Entry<Move, Double> e : rateMoves(b, p).entrySet()) {
            if (e.getValue() > bestRating) {
                best = e.getKey();
                bestRating = e.getValue();
            }
        }
        return best;
    }

    @Override
    public String name() {
        return "Neural";
    }

    // Оценка позиции next для p, где p только что сходил
    private double rate(Board next, Player p) {
        Player opponent = p.opponent();

        if (next.isGameOver()) {
            Player winner = next.getWinner();
            if (winner == null) {
                return 0;
            }
            return winner == p ? 1 : -1;
        }

        // Ход переходит сопернику. Сеть оценивает позицию с его точки зрения,
        // а что хорошо для соперника p, ровно настолько же плохо для текущего p. Поэтому минус
        if (!next.getValidMoves(opponent).isEmpty()) {
            return -net.predict(next.toInput(opponent));
        }

        // У соперника нет ходов, он пропускает, и снова ходит p. Оцениваем с точки зрения p, без минуса
        return net.predict(next.toInput(p));
    }
}