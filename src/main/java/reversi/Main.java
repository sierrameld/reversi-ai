package reversi;

import javax.swing.SwingUtilities;

import reversi.ai.MinimaxAgent;
import reversi.game.Player;
import reversi.gui.GameWindow;

// Запуск окна. Временно соперник минимакс глубины 3
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameWindow(new MinimaxAgent(3), Player.BLACK).setVisible(true));
    }
}