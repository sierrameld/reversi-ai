package reversi.ai;

import reversi.game.Board;
import reversi.game.Player;

// Оценивает позицию числом с точки зрения p. Минимакс вызывает её там, где перестаёт перебирать ходы
public class PositionEvaluator {
    // Веса трёх признаков. Угол ценится больше всего, потом свобода ходов, меньше всего сами фишки
    private static final double DISC_WEIGHT = 1;
    private static final double MOBILITY_WEIGHT = 5;
    private static final double CORNER_WEIGHT = 25;

    // Чем больше, тем лучше для p. Ноль значит позиция равная
    public double evaluate(Board b, Player p) {
        Player q = p.opponent();
        int discs = b.count(p) - b.count(q);
        int mobility = b.getValidMoves(p).size() - b.getValidMoves(q).size();
        int corners = corners(b, p) - corners(b, q);
        return DISC_WEIGHT * discs + MOBILITY_WEIGHT * mobility + CORNER_WEIGHT * corners;
    }

    // Сколько из четырёх углов занято фишками p
    private int corners(Board b, Player p) {
        int last = Board.SIZE - 1;
        int n = 0;
        if (b.get(0, 0) == p.value()) n++;
        if (b.get(0, last) == p.value()) n++;
        if (b.get(last, 0) == p.value()) n++;
        if (b.get(last, last) == p.value()) n++;
        return n;
    }
}