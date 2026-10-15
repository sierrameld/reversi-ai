# Reversi AI with a Neural Network

## Topic

A Java application that plays Reversi (Othello) against a computer opponent. The opponent is a neural network written from scratch, which learns to evaluate board positions from a classic minimax search. A visualization shows how the network rates each possible move.

Key ideas used in the project:

- **Reversi.** Two players place black and white discs on an 8x8 board. A move must enclose a line of opponent discs between the new disc and one of your own, and all enclosed discs are flipped. The player with more discs at the end wins.
- **Minimax with alpha-beta pruning.** Minimax looks several moves ahead, assuming both players always choose their best move. Alpha-beta pruning skips branches that cannot change the result, so the same answer is found much faster.
- **Neural network and backpropagation.** The network takes the board as input and outputs a score for the position. Backpropagation uses derivatives to work out how much each weight contributed to the error, and gradient descent adjusts the weights to reduce it.

## Goal

To build a larger application that uses mathematically and logically complex algorithms, and to make them easy to understand. The network is trained on positions scored by minimax, then compared against a random player and against minimax itself. The visualization shows what the network "thinks" instead of leaving it a black box.

## Structure

The project has four blocks, each in its own package.

- **Game logic** (`reversi.game`). Board, legal moves, flipping discs, passing and end of game.
- **AI players** (`reversi.ai`). The `Agent` interface, a random player, and minimax with alpha-beta pruning and a position evaluation function. Minimax also produces the training data for the network.
- **Neural network** (`reversi.neural`). The network itself, training with backpropagation, dataset generation and a player that uses the network to choose moves.
- **Visual display** (`reversi.gui`). A Swing window that draws the board and, later, the network's ratings of each move.

Dependencies go in one direction. The game logic knows nothing about the other blocks, the AI players use it, the neural network uses both, and the display uses the game logic and the players. The project is built with Gradle and uses only the standard JDK, plus JUnit 5 for tests. There are no other external libraries.

## Initial solution

A Gradle project (Java 21, JUnit 5) with all packages and empty classes for each block. It builds successfully. The work continues in this order. First the game logic, then the visual display so the game becomes playable, then minimax, and finally the neural network with its visualization.