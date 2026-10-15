package reversi.neural;

import reversi.ai.Agent;
import reversi.game.Board;
import reversi.game.Move;
import reversi.game.Player;

// Выбирает ход по оценкам нейросети. Пока заглушка
public class NeuralAgent implements Agent {
    @Override
    public Move chooseMove(Board b, Player p) {
        throw new UnsupportedOperationException("NeuralAgent is not implemented yet");
    }

    @Override
    public String name() {
        return "Neural";
    }
}