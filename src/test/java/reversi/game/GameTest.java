package reversi.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Тесты очерёдности ходов, пропуска хода и конца партии
class GameTest {

    // Ход из записи вроде "d3". Буква это столбец, цифра это строка
    private static Move mv(String s) {
        return new Move(s.charAt(1) - '1', s.charAt(0) - 'a');
    }

    // Сыграть ходы по очереди с начала партии
    private static Game play(String... moves) {
        Game game = new Game();
        for (String m : moves) {
            game.play(mv(m));
        }
        return game;
    }

    @Test
    void blackStarts() {
        Game game = new Game();
        assertEquals(Player.BLACK, game.currentPlayer());
        assertEquals(4, game.validMoves().size());
        assertFalse(game.isOver());
    }

    @Test
    void turnPassesToOpponent() {
        Game game = play("d3");
        assertEquals(Player.WHITE, game.currentPlayer());
    }

    @Test
    void playerWithoutMovesIsSkipped() {
        // После этих 8 ходов очередь чёрных, но ходов у них нет, а у белых есть
        Game game = play("d3", "c3", "b3", "b2", "f5", "a3", "a1", "c1");
        assertTrue(game.board().getValidMoves(Player.BLACK).isEmpty());
        assertFalse(game.isOver());
        assertEquals(Player.WHITE, game.currentPlayer());
    }

    @Test
    void gameEndsWhenNobodyCanMove() {
        // Самая короткая партия. После 9 ходов на доске не осталось белых фишек
        Game game = play("d3", "c3", "b3", "d2", "e1", "d6", "d7", "e3", "f4");
        assertTrue(game.isOver());
        assertEquals(13, game.board().count(Player.BLACK));
        assertEquals(0, game.board().count(Player.WHITE));
        assertEquals(Player.BLACK, game.board().getWinner());
    }
}