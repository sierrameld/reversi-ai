# Reversi AI with a Neural Network

## Topic

A Java application that plays Reversi (Othello). It contains two computer players built on two different ideas.

- **Minimax with alpha-beta pruning.** A classic search algorithm that looks several half-moves ahead.
- **A neural network written from scratch.** It learns to evaluate board positions from the minimax player and then plays by looking only one half-move ahead.

The project also includes a playable Swing window and a tournament that compares all players against each other.

## Goal

To build a larger application that uses mathematically and logically complex algorithms, and to make them easy to understand. The network is trained without any machine learning library, so every step of the mathematics (forward pass, backpropagation, gradient descent) is implemented and tested by hand. The tournament measures how much a trained network can replace a deep search.

## How it works

### Reversi

Two players place black and white discs on an 8x8 board. A move must enclose a line of opponent discs (horizontal, vertical or diagonal) between the new disc and another disc of the player's own colour. All enclosed discs are flipped. A player without a legal move passes. The game ends when neither player can move, and the player with more discs wins. An equal disc count is a draw.

### Minimax with alpha-beta pruning

Minimax looks a fixed number of half-moves ahead. A half-move is one move of one player, so `Minimax(3)` sees its own move, the opponent's reply and its own next move. Minimax assumes that both players always choose their best move. It is implemented in the negamax form. The value of a position for the player to move is the maximum over all moves of the negated value of the resulting position for the opponent.

Alpha-beta pruning keeps two bounds. `alpha` is the best value the current player can already guarantee, `beta` is the highest value the opponent will allow the current player, because the opponent already has a better option elsewhere. When `alpha >= beta`, the remaining moves of that branch cannot change the result and are skipped. A test checks that alpha-beta returns exactly the same values as plain minimax on depths 1 to 3. In a one-off measurement on 900 positions from random games, alpha-beta was about 3.5 times faster on depth 3. A test checks that one search on depth 3 from the middle of a game stays fast (about 1 ms per position).

- Finished games are scored exactly. 1 for a win, -1 for a loss, 0 for a draw.
- At the search depth limit, the position is scored by an evaluation function and squashed into the range (-1, 1) with `tanh(score / 50)`. A heuristic score therefore never reaches the value of a real win.
- If the player to move has no legal move, the search continues for the opponent without using up depth.

The evaluation function combines three differences between the two players, with weights chosen by hand.

| Feature | Weight |
| --- | --- |
| Disc count | 1 |
| Mobility (number of legal moves) | 5 |
| Corners | 25 |

### Neural network

A fully connected network with layers **64-128-64-1** and `tanh` activation on every layer (16,641 parameters).

- **Input.** The 64 board cells from the point of view of the player to move. +1 for the player's own discs, -1 for the opponent's discs, 0 for empty cells. Because of this encoding the network does not need to know which colour it plays.
- **Output.** One number from -1 to 1, the value of the position for the player to move.

Training uses the squared error `E = (y - t)^2 / 2`, where `y` is the network output and `t` is the target. **Backpropagation** applies the chain rule layer by layer, from the output back to the input, and computes how much every weight and bias contributed to the error. **Stochastic gradient descent** then moves each parameter a small step against its gradient: `w = w - learningRate * dE/dw`.

Two tests make sure the mathematics is correct.

- **Gradient check.** The analytic gradients from backpropagation are compared with numerical gradients computed by central differences on several random networks. The largest difference is about 4·10⁻¹¹ (tolerance 10⁻⁷). With a deliberately broken formula the test fails.
- **XOR.** A 2-4-1 network learns XOR, which is impossible without a hidden layer. A 2-1 network without a hidden layer stays at an error of 0.50, as expected.

### Training data

The network learns to imitate the evaluation of `Minimax(5)`.

1. 40,000 games between two random players are played.
2. Every distinct position from these games (2,103,058 positions) gets a target value from `Minimax(5)`.
3. 90% of the positions are used for training, 10% are held out to measure the error on positions the network has never seen.
4. The network is trained for 60 epochs, shuffling the training set before every epoch. The learning rate decays in steps. 0.01 for the first half of the epochs, 0.003 for the next quarter and 0.001 for the last quarter. Large steps learn quickly at the start, small steps fine-tune at the end.

On an Intel i5-11400F, generating the data took about 13.5 hours and training about 2.2 hours, while two other experiments were running in parallel. The generated positions are cached in a file, so further experiments on the same data skip the generation. The error on the held-out positions after training is 0.0334. Every step uses a fixed seed, so training is reproducible. With the same seeds, training gave identical numbers on two different computers. On a small dataset, `TrainingRun` produces a byte-identical network file to the experiment run. The full run (about 15.5 hours) was not repeated, but the network in `models/network.txt` reproduces its re-check result exactly (943 wins, 1,001 losses, 56 draws).

### Neural player

The neural player looks only one half-move ahead. For each legal move it builds the resulting board and asks the network how good that board is for the opponent. The value of the move is the negated opponent value, and the move with the highest value is played. If the opponent has to pass, the player moves again, so the network rates the resulting board from the player's own point of view and the value is used without negation, and a move that ends the game gets its exact result.

### Choosing the final network

23 training runs were made. Longer runs were saved and tested at several checkpoints, which gives 37 tested network states. The runs differ in the amount of data, the depth of the teacher, the network size, the number of epochs and the learning rate. One of them (a huge network on little data) was planned in advance as a control example that should fail. All results are in [docs/experiments.md](docs/experiments.md). The most important lessons were these.

- **First enough positions, then a better teacher.** With few positions a deeper teacher does not help. `Minimax(6)` on 162,000 positions gave networks among the weakest of all. With enough positions a better teacher wins. With the same network, `Minimax(5)` on 2.1 million positions scored higher than `Minimax(4)` on 4.2 million positions (48.5% and 45.4% on the re-check).
- **The network size has to match the data.** On the same 4.2 million positions, 64-128-64-1 scored higher than 64-64-32-1 at both checkpoints, but the difference is below the margin for comparing two networks, so it is a likely trend, not a proven one. A very large network (64-256-256-1) on little data overfitted. Its error on the training positions kept falling while the error on the held-out positions stopped improving, and it played no better than a small network.
- **Learning rate decay.** After about 40 epochs a constant learning rate gave no further gain. With decay, the second half of training raised the three candidates by 2.9–4.9 points. Part of this may come from the extra epochs themselves. The direct comparison on the same data is a smaller experiment, where decay beat a constant learning rate of 0.003 by 4.1 points.
- **A lower held-out error does not always mean stronger play.** The huge 64-256-256-1 network and a 64-64-32-1 network were trained on the same positions with the same teacher. The huge network has the lower error (0.049 and 0.053), but it plays no better (24.6% and 25.6% on the re-check). The error shows how close the network's numbers are to the teacher's numbers, but choosing a move only depends on which move the network rates highest. For example, the teacher rates move A at 0.30 and move B at 0.28, so A is better. The first network answers A 0.25 and B 0.26. Its numbers are very close to the teacher's, but it chooses B. The second network answers A 0.10 and B 0.05. Its numbers are further from the teacher's and its error is larger, but it chooses the right move A.
- **Results on 400 games are not reliable enough to choose a network.** Twice a network looked best after 400 games and then dropped by 7–10 points when re-checked on 2,000 games. Most of this was luck. The re-check also uses 6 random opening moves instead of 4, which on average lowers scores by about 2 points. The final network was therefore chosen only by re-checks on 2,000 games against `Minimax(3)`, and a new network replaced the previous one only if it was better by more than the margin of error. The final network scored 48.5% on the re-check, 6.1 points more than the previous one.

## Results

To find out how strong the final network is, it has to be compared with other players. This is done in a tournament with six players.

- **Random** chooses a random legal move. It is the bottom bar, any sensible player should beat it easily.
- **Minimax(1)**, **Minimax(2)**, **Minimax(3)** and **Minimax(4)** are minimax with a search depth of 1 to 4 half-moves. The greater the depth, the stronger the player and the longer it thinks. Together they form a ladder of opponents that shows which step the network reached.
- **Neural** is the final network, which looks only one half-move ahead.

The tournament is a round robin, so everyone plays everyone. Each pair played 2,000 games, 1,000 with each colour. The first 6 moves of every game are random, otherwise two deterministic players would repeat the same game over and over. The tournament uses seed 4, which was not used for training (seed 1), for choosing the network (seed 2) or for the tournament of the previous network (seed 3). The result is therefore an independent check that the network could not have been tuned to.

The first table shows how each player did against each other player. The number in a cell is the score of the row player against the column player (a win counts 1, a draw counts 0.5). For example, row Neural and column Minimax(2) show that the network scored 63.1% against `Minimax(2)`.

| | Random | Minimax(1) | Minimax(2) | Minimax(3) | Minimax(4) | Neural |
|---|---|---|---|---|---|---|
| **Random** | - | 9.1% | 3.6% | 1.5% | 0.3% | 2.6% |
| **Minimax(1)** | 90.9% | - | 18.3% | 9.6% | 3.7% | 12.9% |
| **Minimax(2)** | 96.4% | 81.8% | - | 28.8% | 12.6% | 37.0% |
| **Minimax(3)** | 98.5% | 90.4% | 71.2% | - | 19.8% | 53.5% |
| **Minimax(4)** | 99.7% | 96.4% | 87.4% | 80.2% | - | 75.7% |
| **Neural** | 97.4% | 87.1% | 63.1% | 46.6% | 24.3% | - |

| Place | Player | Score |
|---|---|---|
| 1 | Minimax(4) | 87.9% |
| 2 | Minimax(3) | 66.7% |
| 3 | **Neural** | 63.7% |
| 4 | Minimax(2) | 51.3% |
| 5 | Minimax(1) | 27.1% |
| 6 | Random | 3.4% |

The margin of error is about ±2.2% per pair (95% confidence, `1.96 × √(0.25 / 2000)`). Smaller differences count as equal strength. Wins, losses and draws for every pair, and the tournament of the previous network, are in [docs/experiments.md](docs/experiments.md).

**What the results show**

- The neural player looks only one half-move ahead, but it is clearly stronger than `Minimax(2)` (63.1%) and close to `Minimax(3)` (46.6%). The learned evaluation is worth more than one half-move of search, but still less than two.
- Against `Minimax(4)` it scores more than `Minimax(3)` does (24.3% and 19.8%, more than the margin of error), although `Minimax(3)` still wins their direct match.
- It is fast. 2,000 games against the random player took 13 seconds, compared with 44 seconds for `Minimax(3)` and 128 seconds for `Minimax(4)`.
- During selection (seed 2) the network scored 48.5% against `Minimax(3)`, in this independent tournament (seed 4) 46.6%. The difference is within the margin of error. A small drop is expected. The network did not start playing worse, its selection result was simply raised a little by luck. Because of chance, the result of any network on 2,000 games is a couple of points above or below its true strength. When the network with the highest result is picked from many, the one picked is usually also the one that was lucky that time.
- Compared with the previous network, the total score rose from 59.5% to 63.7%, and the gap to `Minimax(3)` shrank from 7.9 to 3.0 points.

## Project structure

| Path | Contents |
|---|---|
| `src/main/java/reversi/Main.java` | starts the game window |
| `src/main/java/reversi/game` | board, moves, rules, passing, end of game |
| `src/main/java/reversi/ai` | `Agent` interface, random player, evaluation function, minimax with alpha-beta |
| `src/main/java/reversi/neural` | network, backpropagation, training, data generation, neural player, matches, training experiments, tournament |
| `src/main/java/reversi/gui` | Swing window and board drawing |
| `src/test/java/reversi` | JUnit 5 tests |
| `models/network.txt` | the trained network |
| `models/tournament.csv` | every game of the tournament, one per line |
| `docs/experiments.md` | all training runs and full tournament results |

Dependencies go in one direction only. `game` knows nothing about the other packages, `ai` uses `game`, `neural` uses `game` and `ai`, and `gui` uses all of them. The project uses only the standard JDK, plus JUnit 5 for tests.

## How to run

Requires JDK 21 or newer.

```
.\gradlew test     run all tests
.\gradlew run      open the game window
```

The opponent and the human player's colour are set at the beginning of the `main` method in `reversi.Main`. By default the opponent is the neural network loaded from `models/network.txt`. If the network is chosen as the opponent but the file cannot be loaded, `Minimax(3)` plays instead and the window title says so.

With `SHOW_NETWORK_RATINGS = true` in `reversi.Main`, the window shows how the network rates each of the human player's legal moves. Every legal square gets the network's value from -1 to 1 and a colour from red (bad) through yellow to green (good), and the move the network would choose is framed in white. The ratings are shown on the human player's turn whenever the network is loaded, whatever the opponent is. Without the network the legal moves are marked with dots. This is a first look inside the network's decisions.

Other programs can be started from the IDE.

- `reversi.neural.Tournament` plays the tournament (about 20 minutes).
- `reversi.neural.TrainingRun` trains the final network from scratch (about 15.5 hours).