package reversi.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

// Тесты правил доски

class BoardTest {

    // Ход из записи вроде "d3". Буква это столбец, цифра это строка
    private static Move mv(String s) {
        return new Move(s.charAt(1) - '1', s.charAt(0) - 'a');
    }

    @Test
    void initialPositionHasTwoDiscsEach() {
        Board b = Board.initial();
        assertEquals(2, b.count(Player.BLACK));
        assertEquals(2, b.count(Player.WHITE));
        assertEquals(Player.WHITE.value(), b.get(3, 3)); // d4
        assertEquals(Player.BLACK.value(), b.get(3, 4)); // e4
        assertEquals(Player.BLACK.value(), b.get(4, 3)); // d5
        assertEquals(Player.WHITE.value(), b.get(4, 4)); // e5
    }

    @Test
    void initialPositionHasFourMovesForEachPlayer() {
        Board b = Board.initial();
        assertEquals(List.of(mv("d3"), mv("c4"), mv("f5"), mv("e6")), b.getValidMoves(Player.BLACK));
        assertEquals(List.of(mv("e3"), mv("f4"), mv("c5"), mv("d6")), b.getValidMoves(Player.WHITE));
    }

    @Test
    void moveFlipsDiscs() {
        Board b = Board.initial().applyMove(Player.BLACK, mv("d3"));
        // d4 была белой и оказалась между d3 и d5, значит стала чёрной
        assertEquals(Player.BLACK.value(), b.get(3, 3));
        assertEquals(4, b.count(Player.BLACK));
        assertEquals(1, b.count(Player.WHITE));
    }

    @Test
    void applyMoveDoesNotChangeOldBoard() {
        Board before = Board.initial();
        before.applyMove(Player.BLACK, mv("d3"));
        assertEquals(0, before.get(2, 3)); // d3 осталась пустой
        assertEquals(Player.WHITE.value(), before.get(3, 3)); // d4 осталась белой
        assertEquals(2, before.count(Player.BLACK));
        assertEquals(2, before.count(Player.WHITE));
    }

    @Test
    void illegalMovesAreRejected() {
        Board b = Board.initial();
        assertFalse(b.isValidMove(Player.BLACK, mv("d4"))); // клетка занята
        assertFalse(b.isValidMove(Player.BLACK, mv("a1"))); // ничего не переворачивает
        assertThrows(IllegalArgumentException.class, () -> b.applyMove(Player.BLACK, mv("a1")));
    }

    @Test
    void toInputIsFromPlayersPointOfView() {
        Board b = Board.initial();
        int d4 = 3 * Board.SIZE + 3; // белая фишка
        int e4 = 3 * Board.SIZE + 4; // чёрная фишка
        // Для белых своя d4 это +1, чужая e4 это -1
        assertEquals(1.0, b.toInput(Player.WHITE)[d4]);
        assertEquals(-1.0, b.toInput(Player.WHITE)[e4]);
        // Для чёрных наоборот
        assertEquals(-1.0, b.toInput(Player.BLACK)[d4]);
        assertEquals(1.0, b.toInput(Player.BLACK)[e4]);
    }
}