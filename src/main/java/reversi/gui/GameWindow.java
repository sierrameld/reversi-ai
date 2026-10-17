package reversi.gui;

import java.awt.BorderLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

import reversi.ai.Agent;
import reversi.game.Board;
import reversi.game.Game;
import reversi.game.Move;
import reversi.game.Player;

// Окно партии человек против бота. Человек ходит кликом, бот отвечает сам
public class GameWindow extends JFrame {
    // Бот думает не меньше этого времени, чтобы было видно, как сначала сходил человек, потом бот
    private static final long MIN_BOT_DELAY_MS = 400;

    private final Game game = new Game();
    private final Agent bot;
    private final Player human;
    private final BoardPanel boardPanel = new BoardPanel();
    private final JLabel status = new JLabel(" ", SwingConstants.CENTER);
    // Пока бот думает, клики человека не принимаются
    private boolean botThinking = false;
    // Сообщение о пропуске хода, показывается в строке статуса
    private String note = "";

    public GameWindow(Agent bot, Player human) {
        super("Reversi");
        this.bot = bot;
        this.human = human;

        boardPanel.setOnCellClick(this::onHumanClick);
        status.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(boardPanel, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        refresh();
        // Если человек играет белыми, первым ходит бот
        botMoveIfNeeded();
    }

    private void onHumanClick(Move m) {
        if (botThinking || game.isOver() || game.currentPlayer() != human) {
            return;
        }
        if (!game.board().isValidMove(human, m)) {
            return;
        }
        game.play(m);
        // Game сам отдал ход обратно человеку, значит у бота не было ходов
        note = !game.isOver() && game.currentPlayer() == human ? bot.name() + " has no moves and passes. " : "";
        refresh();
        botMoveIfNeeded();
    }

    // Если сейчас ход бота, запускает его поиск в отдельном потоке, чтобы окно не зависало
    private void botMoveIfNeeded() {
        if (game.isOver() || game.currentPlayer() == human) {
            return;
        }
        botThinking = true;
        refresh();
        Board board = game.board();
        Player p = game.currentPlayer();

        new SwingWorker<Move, Void>() {
            // Выполняется в фоновом потоке. Доска неизменяемая, поэтому её можно спокойно читать оттуда
            @Override
            protected Move doInBackground() throws InterruptedException {
                long start = System.currentTimeMillis();
                Move m = bot.chooseMove(board, p);
                long left = MIN_BOT_DELAY_MS - (System.currentTimeMillis() - start);
                if (left > 0) {
                    Thread.sleep(left);
                }
                return m;
            }

            // Выполняется снова в потоке окна, когда ход найден
            @Override
            protected void done() {
                botThinking = false;
                try {
                    game.play(get());
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException("Bot failed to choose a move", e);
                }
                // Game снова отдал ход боту, значит у человека не было ходов
                note = !game.isOver() && game.currentPlayer() != human ? "You have no moves and pass. " : "";
                refresh();
                botMoveIfNeeded();
            }
        }.execute();
    }

    // Перерисовывает доску и обновляет строку статуса
    private void refresh() {
        boolean humanTurn = !game.isOver() && !botThinking && game.currentPlayer() == human;
        boardPanel.setPosition(game.board(), humanTurn ? game.validMoves() : List.of());
        status.setText(note + statusText());
    }

    private String statusText() {
        Board b = game.board();
        String score = "Black " + b.count(Player.BLACK) + " : " + b.count(Player.WHITE) + " White";
        if (game.isOver()) {
            Player winner = b.getWinner();
            if (winner == null) {
                return "Draw. " + score;
            }
            return (winner == human ? "You win. " : bot.name() + " wins. ") + score;
        }
        if (botThinking) {
            return bot.name() + " is thinking. " + score;
        }
        return "Your move, you play " + (human == Player.BLACK ? "black" : "white") + ". " + score;
    }
}