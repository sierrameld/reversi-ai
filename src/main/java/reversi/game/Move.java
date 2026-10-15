package reversi.game;

// Ход, клетка куда ставится фишка. row 0-7 сверху вниз, col 0-7 слева направо
public record Move(int row, int col) {

    public Move {
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) {   // Проверка на выход за край доски
            throw new IllegalArgumentException("Move outside the board: " + row + ", " + col);
        }
    }

    // Делает запись как на обычной доске, сверху буковки a-h, сбоку циферки 1-8
    @Override
    public String toString() {
        return "" + (char) ('a' + col) + (row + 1);
    }
}