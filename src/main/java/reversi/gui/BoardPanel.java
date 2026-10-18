package reversi.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JPanel;

import reversi.game.Board;
import reversi.game.Move;
import reversi.game.Player;

// Рисует доску, фишки и подсказки ходов. Клик по клетке превращает в Move и отдаёт наружу
public class BoardPanel extends JPanel {
    private static final int CELL = 70;
    private static final Color BOARD_COLOR = new Color(0, 120, 60);
    private static final Color HINT_COLOR = new Color(0, 0, 0, 90);

    private Board board = Board.initial();
    private List<Move> hints = List.of();
    private Map<Move, Double> ratings = Map.of();
    private Consumer<Move> onCellClick = m -> { };

    public BoardPanel() {
        setPreferredSize(new Dimension(CELL * Board.SIZE, CELL * Board.SIZE));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Номер клетки это координата мыши, делённая на размер клетки
                int row = e.getY() / CELL;
                int col = e.getX() / CELL;
                if (row < Board.SIZE && col < Board.SIZE) {
                    onCellClick.accept(new Move(row, col));
                }
            }
        });
    }

    // Что делать при клике по клетке. Задаёт GameWindow
    public void setOnCellClick(Consumer<Move> onCellClick) {
        this.onCellClick = onCellClick;
    }

    // Новая доска и клетки, которые подсветить точками. После этого панель перерисовывается
    public void setPosition(Board board, List<Move> hints) {
        setPosition(board, hints, Map.of());
    }

    // То же, плюс оценки сети для клеток. Клетки с оценкой подкрашиваются и подписываются числом вместо точки
    public void setPosition(Board board, List<Move> hints, Map<Move, Double> ratings) {
        this.board = board;
        this.hints = hints;
        this.ratings = ratings;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int size = CELL * Board.SIZE;
        g2.setColor(BOARD_COLOR);
        g2.fillRect(0, 0, size, size);

        // Цветные клетки с оценками поверх фона, но под сеткой
        drawRatings(g2);

        g2.setColor(Color.BLACK);
        for (int i = 0; i <= Board.SIZE; i++) {
            g2.drawLine(i * CELL, 0, i * CELL, size);
            g2.drawLine(0, i * CELL, size, i * CELL);
        }

        int pad = 6;
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                int cell = board.get(row, col);
                if (cell == 0) {
                    continue;
                }
                g2.setColor(cell == Player.BLACK.value() ? Color.BLACK : Color.WHITE);
                g2.fillOval(col * CELL + pad, row * CELL + pad, CELL - 2 * pad, CELL - 2 * pad);
            }
        }

        int dot = 16;
        g2.setColor(HINT_COLOR);
        for (Move m : hints) {
            // На клетке с числом точка не нужна
            if (ratings.containsKey(m)) {
                continue;
            }
            int x = m.col() * CELL + (CELL - dot) / 2;
            int y = m.row() * CELL + (CELL - dot) / 2;
            g2.fillOval(x, y, dot, dot);
        }
    }

    // Клетки с оценками: цвет от красного (-1) до зелёного (1), число с двумя знаками, лучшая клетка в рамке
    private void drawRatings(Graphics2D g2) {
        if (ratings.isEmpty()) {
            return;
        }
        Move best = null;
        for (Map.Entry<Move, Double> e : ratings.entrySet()) {
            if (best == null || e.getValue() > ratings.get(best)) {
                best = e.getKey();
            }
        }

        g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        for (Map.Entry<Move, Double> e : ratings.entrySet()) {
            int x = e.getKey().col() * CELL;
            int y = e.getKey().row() * CELL;
            double value = e.getValue();

            // Оценку от -1 до 1 переводим в оттенок от 0 (красный) через жёлтый до 1/3 (зелёный)
            float t = (float) ((value + 1) / 2);
            g2.setColor(Color.getHSBColor(t / 3, 0.8f, 0.95f));
            g2.fillRect(x, y, CELL, CELL);

            String text = String.format(Locale.ROOT, "%.2f", value);
            g2.setColor(Color.BLACK);
            g2.drawString(text, x + (CELL - fm.stringWidth(text)) / 2, y + (CELL + fm.getAscent()) / 2 - 2);
        }

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(4));
        g2.drawRect(best.col() * CELL + 3, best.row() * CELL + 3, CELL - 6, CELL - 6);
        g2.setStroke(new BasicStroke(1));
    }
}