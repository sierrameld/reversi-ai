package reversi.neural;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import reversi.ai.Agent;
import reversi.ai.MinimaxAgent;
import reversi.ai.RandomAgent;

// Круговой турнир. Каждая пара агентов играет серию партий, итог печатается таблицами в формате Markdown
public class Tournament {
    private static final int GAMES_PER_PAIR = 2000;
    private static final int RANDOM_OPENING_MOVES = 6;
    // Seed 3 был у турнира с прошлой сетью. Seed 4 новый, ради чистоты тестирования
    private static final long SEED = 4;
    private static final Path NETWORK = Path.of("models", "network.txt");
    private static final Path RESULTS = Path.of("models", "tournament.csv");

    public static void main(String[] args) throws IOException {
        if (!Files.exists(NETWORK)) {
            System.out.println("No trained network at " + NETWORK.toAbsolutePath());
            return;
        }

        List<Agent> agents = List.of(
                new RandomAgent(SEED),
                new MinimaxAgent(1),
                new MinimaxAgent(2),
                new MinimaxAgent(3),
                new MinimaxAgent(4),
                new NeuralAgent(NeuralNetwork.load(NETWORK)));
        int n = agents.size();

        // wins[i][j] это сколько партий агент i выиграл у агента j, draws[i][j] сколько сыграли вничью
        int[][] wins = new int[n][n];
        int[][] draws = new int[n][n];
        // Каждая партия отдельной строкой, чтобы можно было найти странные партии
        StringBuilder csv = new StringBuilder("game,black,white,opening,black_discs,white_discs,winner,random_opening_moves,seed\n");

        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                long pairStart = System.currentTimeMillis();
                // Каждая пара играет с одними и теми же случайными началами, поэтому условия у всех пар одинаковые
                List<Match.GameResult> games = Match.playGames(agents.get(i), agents.get(j), GAMES_PER_PAIR,
                        RANDOM_OPENING_MOVES, SEED);
                Match.Result r = Match.summarize(games);
                wins[i][j] = r.wins();
                wins[j][i] = r.losses();
                draws[i][j] = r.draws();
                draws[j][i] = r.draws();
                for (int g = 0; g < games.size(); g++) {
                    appendGame(csv, g + 1, agents.get(i).name(), agents.get(j).name(), games.get(g));
                }
                System.out.printf(Locale.ROOT, "%s vs %s: %d-%d-%d in %.0f s%n", agents.get(i).name(), agents.get(j).name(),
                        r.wins(), r.losses(), r.draws(), seconds(pairStart));
            }
        }
        double totalSeconds = seconds(start);

        printScoreTable(agents, wins, draws);
        printPairs(agents, wins, draws);
        printStandings(agents, wins, draws);

        // Погрешность 95% для доли очков в одной паре, худший случай, когда силы равны (доля 50%)
        double margin = 1.96 * Math.sqrt(0.25 / GAMES_PER_PAIR) * 100;
        System.out.printf(Locale.ROOT, "%n%d games per pair, %d random opening moves, seed %d. "
                        + "Margin of error about +/-%.1f%% per pair, smaller differences count as equal strength.%n",
                GAMES_PER_PAIR, RANDOM_OPENING_MOVES, SEED, margin);
        System.out.printf(Locale.ROOT, "Total time %.0f min%n", totalSeconds / 60);

        Files.writeString(RESULTS, csv.toString());
        System.out.println("Saved to " + RESULTS.toAbsolutePath());
    }

    // Одна строка CSV на партию. Номер партии внутри пары, кто играл каким цветом, начало, фишки и победитель
    private static void appendGame(StringBuilder csv, int number, String first, String second, Match.GameResult g) {
        String black = g.firstIsBlack() ? first : second;
        String white = g.firstIsBlack() ? second : first;
        String winner = g.blackDiscs() > g.whiteDiscs() ? black : g.blackDiscs() < g.whiteDiscs() ? white : "draw";
        csv.append(number).append(',').append(black).append(',').append(white).append(',').append(g.opening())
                .append(',').append(g.blackDiscs()).append(',').append(g.whiteDiscs()).append(',').append(winner)
                .append(',').append(RANDOM_OPENING_MOVES).append(',').append(SEED).append('\n');
    }

    // Процент очков строки против столбца. Победа 1 очко, ничья половина
    private static void printScoreTable(List<Agent> agents, int[][] wins, int[][] draws) {
        int n = agents.size();
        StringBuilder header = new StringBuilder("\n| |");
        StringBuilder line = new StringBuilder("|---|");
        for (Agent a : agents) {
            header.append(' ').append(a.name()).append(" |");
            line.append("---|");
        }
        System.out.println(header);
        System.out.println(line);

        for (int i = 0; i < n; i++) {
            StringBuilder row = new StringBuilder("| " + agents.get(i).name() + " |");
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    row.append(" - |");
                } else {
                    row.append(String.format(Locale.ROOT, " %.1f%% |", 100.0 * points(wins, draws, i, j) / GAMES_PER_PAIR));
                }
            }
            System.out.println(row);
        }
    }

    // Победы, поражения и ничьи по каждой паре, с точки зрения первого в паре
    private static void printPairs(List<Agent> agents, int[][] wins, int[][] draws) {
        System.out.println("\n| Pair | Wins | Losses | Draws | Score |");
        System.out.println("|---|---|---|---|---|");
        int n = agents.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                System.out.printf(Locale.ROOT, "| %s vs %s | %d | %d | %d | %.1f%% |%n",
                        agents.get(i).name(), agents.get(j).name(), wins[i][j], wins[j][i], draws[i][j],
                        100.0 * points(wins, draws, i, j) / GAMES_PER_PAIR);
            }
        }
    }

    // Итоговые очки за все пары и места, от лучшего к худшему
    private static void printStandings(List<Agent> agents, int[][] wins, int[][] draws) {
        int n = agents.size();
        List<Integer> order = new ArrayList<>();
        double[] total = new double[n];
        for (int i = 0; i < n; i++) {
            order.add(i);
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    total[i] += points(wins, draws, i, j);
                }
            }
        }
        order.sort((a, b) -> Double.compare(total[b], total[a]));

        int maxPoints = (n - 1) * GAMES_PER_PAIR;
        System.out.println("\n| Place | Agent | Points | Score |");
        System.out.println("|---|---|---|---|");
        for (int place = 0; place < n; place++) {
            int i = order.get(place);
            System.out.printf(Locale.ROOT, "| %d | %s | %.1f of %d | %.1f%% |%n",
                    place + 1, agents.get(i).name(), total[i], maxPoints, 100.0 * total[i] / maxPoints);
        }
    }

    // Очки агента i против агента j. Победа 1, ничья 0.5
    private static double points(int[][] wins, int[][] draws, int i, int j) {
        return wins[i][j] + 0.5 * draws[i][j];
    }

    private static double seconds(long since) {
        return (System.currentTimeMillis() - since) / 1000.0;
    }
}