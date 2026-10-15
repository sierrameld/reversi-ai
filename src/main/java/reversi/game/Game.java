package reversi.game;

import java.util.List;

// Партия целиком. Хранит текущую доску и того, чей ход, и сама обрабатывает пропуск хода
public final class Game {
    private Board board;
    private Player current;

    // Стартовая позиция, первыми ходят чёрные
    public Game() {
        board = Board.initial();
        current = Player.BLACK;
    }

    public Board board() {
        return board;
    }

    public Player currentPlayer() {
        return current;
    }

    public List<Move> validMoves() {
        return board.getValidMoves(current);
    }

    // Делает ход текущего игрока и передаёт ход дальше
    public void play(Move m) {
        board = board.applyMove(current, m);
        current = current.opponent();
        // Если у соперника нет ходов, но игра не кончилась, значит снова ходит тот же игрок
        if (!board.isGameOver() && board.getValidMoves(current).isEmpty()) {
            current = current.opponent();
        }
    }

    // Для тех классов, которые ведут партию и взаимодействуют с Game, чтобы не лезть в саму логику проверки
    public boolean isOver() {
        return board.isGameOver();
    }
}