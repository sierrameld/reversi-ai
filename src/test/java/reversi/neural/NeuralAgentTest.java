package reversi.neural;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import reversi.ai.Agent;
import reversi.ai.RandomAgent;
import reversi.game.Board;
import reversi.game.Game;
import reversi.game.Move;
import reversi.game.Player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Тесты агента на нейросети
class NeuralAgentTest {

    @Test
    void nameIsNeural() {
        assertEquals("Neural", new NeuralAgent(NeuralNetwork.withSeed(1, 64, 16, 1)).name());
    }

    // Оценку получают только все легальные ходы, и каждая оценка от -1 до 1
    @Test
    void ratesExactlyTheLegalMoves() {
        NeuralAgent agent = new NeuralAgent(NeuralNetwork.withSeed(1, 64, 16, 1));
        RandomAgent random = new RandomAgent(1);
        Game game = new Game();

        while (!game.isOver()) {
            Board b = game.board();
            Player p = game.currentPlayer();

            Map<Move, Double> ratings = agent.rateMoves(b, p);
            assertEquals(new HashSet<>(b.getValidMoves(p)), ratings.keySet());
            for (double r : ratings.values()) {
                assertTrue(r >= -1 && r <= 1, "rating " + r);
            }

            game.play(random.chooseMove(b, p));
        }
    }

    // Необученная сеть играет плохо, но ходы должны быть легальными. Доигрываем партии против случайного до конца
    @Test
    void playsOnlyLegalMovesToTheEnd() {
        NeuralAgent agent = new NeuralAgent(NeuralNetwork.withSeed(1, 64, 16, 1));
        RandomAgent random = new RandomAgent(1);

        for (int g = 0; g < 20; g++) {
            Game game = new Game();
            // В чётных партиях сеть играет чёрными, в нечётных белыми
            Player neuralColor = g % 2 == 0 ? Player.BLACK : Player.WHITE;

            while (!game.isOver()) {
                Player p = game.currentPlayer();
                Agent mover = p == neuralColor ? agent : random;
                Move m = mover.chooseMove(game.board(), p);
                assertTrue(game.board().isValidMove(p, m), "illegal move " + m);
                game.play(m);
            }
        }
    }

    // Проверка знака. Сеть, обученная на разнице фишек, должна почти всегда выбирать ход, после которого у неё больше всего фишек
    // Если знак в оценке будет перепутан, агент будет выбирать худший ход
    @Test
    void trainedOnPieceDifferenceChoosesMostPieces() {
        NeuralNetwork net = NeuralNetwork.withSeed(1, 64, 32, 16, 1);
        Trainer trainer = new Trainer(net, 0.01, 1);
        List<TrainingExample> examples = new DatasetGenerator(DatasetGenerator::pieceDifference, 1).generate(20);
        for (int epoch = 0; epoch < 5; epoch++) {
            trainer.trainEpoch(examples);
        }

        NeuralAgent agent = new NeuralAgent(net);
        RandomAgent random = new RandomAgent(2);
        int agree = 0;
        int total = 0;

        for (int g = 0; g < 20; g++) {
            Game game = new Game();
            while (!game.isOver()) {
                Board b = game.board();
                Player p = game.currentPlayer();
                List<Move> moves = b.getValidMoves(p);

                // Позиции с одним ходом ничего не проверяют, выбирать там не из чего
                if (moves.size() > 1) {
                    int best = Integer.MIN_VALUE;
                    for (Move m : moves) {
                        best = Math.max(best, pieceLead(b.applyMove(p, m), p));
                    }
                    if (pieceLead(b.applyMove(p, agent.chooseMove(b, p)), p) == best) {
                        agree++;
                    }
                    total++;
                }

                game.play(random.chooseMove(b, p));
            }
        }

        assertTrue(agree >= total * 0.9, agree + " of " + total);
    }

    // На сколько фишек текущий p впереди соперника
    private static int pieceLead(Board b, Player p) {
        return b.count(p) - b.count(p.opponent());
    }
}