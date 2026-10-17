# Reversi AI with a Neural Network

## Topic

A Java application that plays Reversi (Othello). It contains two computer players built on two different ideas.

- **Minimax with alpha-beta pruning.** A classic search algorithm that looks several moves ahead.
- **A neural network written from scratch.** It learns to evaluate board positions from the minimax player and then plays by looking only one move ahead.

The project also includes a playable Swing window and a tournament that compares all players against each other.

## Goal

To build a larger application that uses mathematically and logically complex algorithms, and to make them easy to understand. The network is trained without any machine learning library, so every step of the mathematics (forward pass, backpropagation, gradient descent) is implemented and tested by hand. The tournament measures how much a trained network can replace a deep search.

## How it works

### Reversi

Two players place black and white discs on an 8x8 board. A move must enclose a line of opponent discs (horizontal, vertical or diagonal) between the new disc and another disc of the player's own colour. All enclosed discs are flipped. A player without a legal move passes. The game ends when neither player can move, and the player with more discs wins.

### Minimax with alpha-beta pruning

Minimax looks a fixed number of moves ahead and assumes that both players always choose their best move. It is implemented in the negamax form. The value of a position for the player to move is the maximum over all moves of the negated value of the resulting position for the opponent.

Alpha-beta pruning keeps two bounds. `alpha` is the best value the current player can already guarantee, `beta` is the best value the opponent can already guarantee. When `alpha >= beta`, the remaining moves of that branch cannot change the result and are skipped. A test checks that alpha-beta returns exactly the same values as plain minimax on depths 1 to 3. On depth 3 it is about 3.5 times faster.

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

A fully connected network with layers **64-64-32-1** and `tanh` activation on every layer (6,273 parameters).

- **Input.** The 64 board cells from the point of view of the player to move. +1 for the player's own discs, -1 for the opponent's discs, 0 for empty cells. Because of this encoding the network does not need to know which colour it plays.
- **Output.** One number from -1 to 1, the value of the position for the player to move.

Training uses the squared error `E = (y - t)^2 / 2`, where `y` is the network output and `t` is the target. **Backpropagation** applies the chain rule layer by layer, from the output back to the input, and computes how much every weight and bias contributed to the error. **Stochastic gradient descent** then moves each parameter a small step against its gradient: `w = w - learningRate * dE/dw`.

Two tests make sure the mathematics is correct.

- **Gradient check.** The analytic gradients from backpropagation are compared with numerical gradients computed by central differences on several random networks. The largest difference is about 4·10⁻¹¹ (tolerance 10⁻⁷). With a deliberately broken formula the test fails.
- **XOR.** A 2-4-1 network learns XOR, which is impossible without a hidden layer. A 2-1 network without a hidden layer stays at an error of 0.50, as expected.

### Training data

The network learns to imitate the evaluation of `Minimax(4)`.

1. 30,000 games between two random players are played.
2. Every distinct position from these games (1,582,525 positions) gets a target value from `Minimax(4)`.
3. 90% of the positions are used for training, 10% are held out to measure the error on positions the network has never seen.
4. The network is trained for 40 epochs with learning rate 0.01, shuffling the training set before every epoch.

Generating the data takes about 88 minutes and training about 25 minutes on an Intel i5-11400F. The error on the held-out positions after training is 0.0376. Every step uses a fixed seed, so training is fully reproducible and gives the same network on different computers.

### Neural player

The neural player looks only one move ahead. For each legal move it builds the resulting board and asks the network how good that board is for the opponent. The value of the move is the negated opponent value, and the move with the highest value is played. If the opponent has to pass, the value is used without negation, and a move that ends the game gets its exact result.

### Choosing the final network

25 different networks were trained with different amounts of data, teacher depths, network sizes, epochs and learning rates. All results are in [docs/experiments.md](docs/experiments.md). The most important lessons were these.

- More training positions helped most reliably.
- A deeper teacher only helped when there were enough positions.
- A lower error on held-out positions did not always mean stronger play.
- **Results on 400 games are not reliable enough to choose a network.** The network that looked best after 400 games against `Minimax(3)` (48%) dropped to about 37% when re-checked on 2,000 games. It had simply been lucky. The final network was therefore chosen only by re-checks on 2,000 games.

## Results

A round-robin tournament between all players. Each pair played 2,000 games, 1,000 with each colour. The first 6 moves of every game are random, otherwise two deterministic players would repeat the same game. The tournament uses seed 3, which was not used for training (seed 1) or for choosing the network (seed 2), so the result is an independent check.

Score of the row player against the column player (a win counts 1, a draw counts 0.5).

| | Random | Minimax(1) | Minimax(2) | Minimax(3) | Minimax(4) | Neural |
|---|---|---|---|---|---|---|
| **Random** | - | 9.9% | 3.3% | 1.3% | 0.3% | 3.4% |
| **Minimax(1)** | 90.1% | - | 18.6% | 10.4% | 3.9% | 17.0% |
| **Minimax(2)** | 96.7% | 81.4% | - | 29.1% | 13.0% | 43.2% |
| **Minimax(3)** | 98.7% | 89.7% | 70.9% | - | 19.8% | 58.1% |
| **Minimax(4)** | 99.7% | 96.1% | 87.0% | 80.2% | - | 81.0% |
| **Neural** | 96.6% | 83.0% | 56.8% | 41.9% | 19.1% | - |

| Place | Player | Score |
|---|---|---|
| 1 | Minimax(4) | 88.8% |
| 2 | Minimax(3) | 67.4% |
| 3 | **Neural** | 59.5% |
| 4 | Minimax(2) | 52.7% |
| 5 | Minimax(1) | 28.0% |
| 6 | Random | 3.6% |

The margin of error is about ±2.2% per pair. Smaller differences count as equal strength. Wins, losses and draws for every pair are in [docs/experiments.md](docs/experiments.md).

**What the results show**

- The neural player looks only one move ahead, but it is stronger than `Minimax(2)` (56.8%) and weaker than `Minimax(3)` (41.9%). The learned evaluation is worth more than one level of search, but less than two.
- It is as fast as `Minimax(2)`. 2,000 games against the random player took 6 seconds for both, 41 seconds for `Minimax(3)` and 122 seconds for `Minimax(4)`.
- Against `Minimax(4)` it scores the same as `Minimax(3)` (19.1% and 19.8%).
- During network selection (seed 2) it scored 42.4% against `Minimax(3)`, and in this independent tournament (seed 3) 41.9%. The difference is within the margin of error, so the choice of the network was not a lucky accident.
- Each deeper minimax clearly beats the one before it.

## Project structure

```
src/main/java/reversi
  Main      starts the game window
  game      board, moves, rules, passing, end of game
  ai        Agent interface, random player, evaluation function, minimax with alpha-beta
  neural    network, backpropagation, training, data generation, neural player,
            matches, training experiments, tournament
  gui       Swing window and board drawing
src/test/java/reversi   JUnit 5 tests
models/network.txt      the trained network
models/tournament.csv   every game of the tournament, one per line
docs/experiments.md     all training runs and full tournament results
```

Dependencies go in one direction only. `game` knows nothing about the other packages, `ai` uses `game`, `neural` uses `game` and `ai`, and `gui` uses all of them. The project uses only the standard JDK, plus JUnit 5 for tests.

## How to run

Requires JDK 21.

```
.\gradlew test     run all tests
.\gradlew run      open the game window
```

The opponent and the human player's colour are set as constants at the top of `reversi.Main`. By default the opponent is the neural network loaded from `models/network.txt`.

Other programs can be started from the IDE.

- `reversi.neural.Tournament` plays the tournament (about 15 minutes).
- `reversi.neural.TrainingRun` trains the final network from scratch (about 2 hours).