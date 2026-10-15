package reversi.game;

import java.util.ArrayList;
import java.util.List;

// Доска 8x8. Неизменяемая, любой ход возвращает новую доску
public final class Board {
    public static final int SIZE = 8;

    // cells[row][col] хранит 1 чёрная, -1 белая, 0 пусто
    private final int[][] cells;

    private Board(int[][] cells) {
        this.cells = cells;
    }

    // Стартовая позиция, по две фишки каждого цвета крестом в центре
    public static Board initial() {
        int[][] cells = new int[SIZE][SIZE];
        cells[3][3] = Player.WHITE.value(); // d4
        cells[3][4] = Player.BLACK.value(); // e4
        cells[4][3] = Player.BLACK.value(); // d5
        cells[4][4] = Player.WHITE.value(); // e5
        return new Board(cells);
    }

    // Возвращает 1 чёрная, -1 белая, 0 пусто по координате
    public int get(int row, int col) {
        return cells[row][col];
    }

    // Сколько фишек игрока p на доске
    public int count(Player p) {
        int n = 0;
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (cells[row][col] == p.value()) {
                    n++;
                }
            }
        }
        return n;
    }

    // Превращает поле в 64 числа для нейросети с точки зрения p. Свои фишки +1, чужие -1, пусто 0
    public double[] toInput(Player p) {
        double[] input = new double[SIZE * SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                // Умножение на value() переворачивает знак, если p играет белыми
                input[row * SIZE + col] = cells[row][col] * p.value();
            }
        }
        return input;
    }

    // 8 направлений от клетки. Пары {сдвиг по строке, сдвиг по столбцу}
    private static final int[][] DIRECTIONS = {
            {-1, 0},    // ↑
            {-1, 1},    // ↗
            {0, 1},     // →
            {1, 1},     // ↘
            {1, 0},     // ↓
            {1, -1},    // ↙
            {0, -1},    // ←
            {-1, -1}    // ↖
    };

    // Проверка на выход за край доски. Отличие от проверки в Move в том, что эта просто отвечает да или нет, а не выдаёт Exception
    // и нужен внутри для поверок функций класса Board, в отличие от проверки в Move которая нужна для тех кто делает ходы
    private static boolean onBoard(int row, int col) {
        return row >= 0 && row < SIZE && col >= 0 && col < SIZE;
    }

    // Сколько фишек соперника перевернётся в одном направлении, если p поставит фишку в (row, col)
    // dRow и dCol это шаг по строке и по столбцу, по сути дельта, берёт из DIRECTIONS
    private int flipsInDirection(Player p, int row, int col, int dRow, int dCol) {
        int r = row + dRow;
        int c = col + dCol;
        int count = 0;
        // Цикл работает, пока встречает чужие фишки
        while (onBoard(r, c) && cells[r][c] == p.opponent().value()) {
            count++;
            r += dRow;
            c += dCol;
        }
        // Каждый раз проверяет цвет следующей фишки, если она своя, то закрываем цикл и отправляем количество вражеских фишек на пути
        if (count > 0 && onBoard(r, c) && cells[r][c] == p.value()) {
            return count;
        }
        // Если своей фишки на пути нет, цикл не закроется по предыдущему условию, вследствие чего по окончанию цикла (достижения границы) отдаст 0
        return 0;
    }

    // Ход легален, если клетка пустая и хотя бы в одном направлении что-то переворачивается
    // Иными словами cells[row][m.col] == 0 && flipsInDirection > 0
    public boolean isValidMove(Player p, Move m) {
        if (cells[m.row()][m.col()] != 0) {
            return false;
        }
        for (int[] d : DIRECTIONS) {
            if (flipsInDirection(p, m.row(), m.col(), d[0], d[1]) > 0) {
                return true;
            }
        }
        return false;
    }

    // Все легальные ходы для p, по порядку строк сверху вниз, слева направо
    public List<Move> getValidMoves(Player p) {
        List<Move> moves = new ArrayList<>();
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                Move m = new Move(row, col);
                if (isValidMove(p, m)) {
                    moves.add(m);
                }
            }
        }
        return moves;
    }
}