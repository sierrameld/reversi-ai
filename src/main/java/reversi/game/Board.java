package reversi.game;

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
}