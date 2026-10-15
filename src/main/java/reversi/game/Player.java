package reversi.game;

// Два игрока. Число value() хранится в клетках доски
public enum Player {
    BLACK(1),
    WHITE(-1);

    private final int value;

    Player(int value) {
        this.value = value;
    }

    // Возвращает 1 у чёрных и -1 у белых
    public int value() {
        return value;
    }

    // Функция для переворота фишки, смены хода, и смены точки зрения на оппонента для минмакса и нейронки, в общем нужно типо да
    public Player opponent() {
        return this == BLACK ? WHITE : BLACK;
    }
}