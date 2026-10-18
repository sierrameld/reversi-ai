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
    // Показывать ли на клетках оценки сети для ходов человека
    private static final boolean SHOW_NETWORK_RATINGS = true;

    public static void main(String[] args) {
        // null, если сеть не загрузилась
        NeuralAgent network = loadNetwork();

        // Выбор соперника
        // new RandomAgent()
        // new MinimaxAgent(глубина)
        // network
        Agent chosen = network;
        // За кого играет человек. Чёрные ходят первыми
        Player human = Player.BLACK;

        // Выбрана сеть, но она не загрузилась. Играем с Minimax(3) и пишем об этом в заголовке окна
        Agent opponent = chosen != null ? chosen : new MinimaxAgent(3);
        String title = chosen != null ? "Reversi" : "Reversi. Network not loaded, playing against Minimax(3)";
        // Если сети нет, оценки просто не показываются
        NeuralAgent advisor = SHOW_NETWORK_RATINGS ? network : null;

        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow(opponent, human, advisor);
            window.setTitle(title);
            window.setVisible(true);
        });
    }

    // Сеть из models/network.txt или null, если файла нет или он испорчен
    private static NeuralAgent loadNetwork() {
        try {
            return new NeuralAgent(NeuralNetwork.load(NETWORK_FILE));
        } catch (RuntimeException e) {
            System.out.println("Cannot load network from " + NETWORK_FILE.toAbsolutePath()
                    + " (" + e.getMessage() + ")");
            return null;
        }
    }
}