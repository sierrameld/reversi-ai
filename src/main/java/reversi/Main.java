package reversi;

import java.nio.file.Path;

import javax.swing.SwingUtilities;

import reversi.ai.Agent;
import reversi.ai.MinimaxAgent;
import reversi.ai.RandomAgent;
import reversi.game.Player;
import reversi.gui.GameWindow;
import reversi.neural.NeuralAgent;
import reversi.neural.NeuralNetwork;

// Запуск окна. Соперник и цвет человека выбираются здесь, без меню
public class Main {
    private static final Path NETWORK_FILE = Path.of("models", "network.txt");

    public static void main(String[] args) {
        // Выбор соперника
        // new RandomAgent()
        // new MinimaxAgent(глубина)
        // loadNeuralAgent()
        Agent opponent = loadNeuralAgent();
        // За кого играет человек. Чёрные ходят первыми
        Player human = Player.BLACK;

        SwingUtilities.invokeLater(() -> new GameWindow(opponent, human).setVisible(true));
    }

    // Сеть из models/network.txt. Если файл не найден или он испорчен, вместо сети Minimax(3), чтобы окно всё равно открылось
    private static Agent loadNeuralAgent() {
        try {
            return new NeuralAgent(NeuralNetwork.load(NETWORK_FILE));
        } catch (RuntimeException e) {
            System.out.println("Cannot load network from " + NETWORK_FILE.toAbsolutePath()
                    + " (" + e.getMessage() + "), playing against Minimax(3) instead");
            return new MinimaxAgent(3);
        }
    }
}