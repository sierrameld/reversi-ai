# Experiments

This file contains the full data behind the results in the [README](../README.md).

All percentages are **scores**. A win counts 1, a draw counts 0.5.

## Training runs

Every run generated its own data from random games, trained a network and played 400 games against each of three opponents. Times are measured on an Intel i5-11400F.

| # | Games | Positions | Teacher | Network | Epochs | Learning rate | Data gen | Training | Held-out error | vs Random | vs Minimax(1) | vs Minimax(3) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | 3,000 | 162,327 | Minimax(3) | 64-32-16-1 | 20 | 0.01 | 2 min | 1 min | 0.04036 | 95.6% | 72.2% | 24.2% |
| 2 | 10,000 | 533,904 | Minimax(3) | 64-32-16-1 | 20 | 0.01 | 7 min | 2 min | 0.03658 | 96.0% | 78.2% | 35.2% |
| 3 | 10,000 | 533,904 | Minimax(3) | 64-64-32-1 | 20 | 0.01 | 7 min | 5 min | 0.03300 | 96.9% | 77.4% | 34.8% |
| 4 | 20,000 | 1,059,732 | Minimax(3) | 64-32-16-1 | 20 | 0.01 | 14 min | 4 min | 0.03447 | 98.0% | 82.6% | 39.5% |
| 5 | 20,000 | 1,059,732 | Minimax(3) | 64-64-32-1 | 20 | 0.01 | 14 min | 9 min | 0.03109 | 96.0% | 81.4% | 43.2% |
| 6 | 10,000 | 533,904 | Minimax(4) | 64-32-16-1 | 20 | 0.01 | 30 min | 2 min | 0.04605 | 94.2% | 77.6% | 32.5% |
| 7 | 10,000 | 533,904 | Minimax(4) | 64-64-32-1 | 20 | 0.01 | 30 min | 4 min | 0.04257 | 93.5% | 77.2% | 29.2% |
| 8 | 20,000 | 1,059,732 | Minimax(4) | 64-32-16-1 | 20 | 0.01 | 59 min | 4 min | 0.04369 | 94.1% | 80.0% | 40.0% |
| 9 | 5,000 | 269,008 | Minimax(5) | 64-32-16-1 | 20 | 0.01 | 86 min | 1 min | 0.04812 | 94.8% | 79.9% | 33.6% |
| 10 | 5,000 | 269,008 | Minimax(5) | 64-64-32-1 | 20 | 0.01 | 86 min | 2 min | 0.04546 | 95.1% | 81.1% | 30.5% |
| 11 | 8,000 | 428,158 | Minimax(5) | 64-32-16-1 | 20 | 0.01 | 136 min | 1 min | 0.04798 | 97.4% | 82.2% | 37.2% |
| 12 | 8,000 | 428,158 | Minimax(5) | 64-64-32-1 | 20 | 0.01 | 136 min | 3 min | 0.04624 | 95.5% | 85.4% | 36.1% |
| 13 | 40,000 | 2,103,058 | Minimax(3) | 64-64-32-1 | 20 | 0.01 | 26 min | 16 min | 0.03239 | 97.2% | 76.0% | 34.2% |
| 14 | 40,000 | 2,103,058 | Minimax(3) | 64-64-32-1 | 40 | 0.01 | 26 min | 33 min | 0.03052 | 95.9% | 85.9% | 33.1% |
| 15 | 40,000 | 2,103,058 | Minimax(3) | 64-64-32-1 | 60 | 0.01 | 26 min | 50 min | 0.03055 | 96.0% | 79.2% | 36.6% |
| 16 | 30,000 | 1,582,525 | Minimax(4) | 64-64-32-1 | 20 | 0.01 | 88 min | 12 min | 0.03981 | 97.6% | 84.5% | 38.0% |
| **17 (final)** | 30,000 | 1,582,525 | Minimax(4) | 64-64-32-1 | 40 | 0.01 | 88 min | 25 min | 0.03764 | 97.5% | 81.2% | 45.1% |
| 18 | 30,000 | 1,582,525 | Minimax(4) | 64-64-32-1 | 60 | 0.01 | 88 min | 38 min | 0.03752 | 97.1% | 88.2% | 44.2% |
| 19 | 30,000 | 1,582,525 | Minimax(4) | 64-128-32-1 | 20 | 0.01 | 88 min | 24 min | 0.03579 | 97.9% | 80.2% | 40.9% |
| 20 | 30,000 | 1,582,525 | Minimax(4) | 64-128-32-1 | 40 | 0.01 | 88 min | 49 min | 0.03448 | 95.6% | 72.5% | 39.9% |
| 21 | 30,000 | 1,582,525 | Minimax(4) | 64-128-32-1 | 60 | 0.01 | 88 min | 74 min | 0.03830 | 97.1% | 82.2% | 42.1% |
| 22 | 20,000 | 1,059,732 | Minimax(4) | 64-64-32-1 | 20 | 0.01 | 57 min | 8 min | 0.03968 | 97.0% | 81.2% | 48.0% |
| 23 | 20,000 | 1,059,732 | Minimax(4) | 64-64-32-1 | 40 | 0.01 → 0.003 → 0.001 (decay) | 57 min | 17 min | 0.03653 | 97.0% | 84.8% | 43.8% |
| 24 | 20,000 | 1,059,732 | Minimax(4) | 64-64-32-1 | 20 | 0.003 | 57 min | 8 min | 0.03861 | 97.2% | 82.1% | 42.5% |
| 25 | 20,000 | 1,059,732 | Minimax(4) | 64-64-32-1 | 40 | 0.003 | 57 min | 18 min | 0.03757 | 95.8% | 83.0% | 39.5% |

With 400 games the margin of error is about ±5%, so small differences in this table mean nothing.

### Findings

- **More positions** is the most reliable way to a stronger network. With teacher depth 3, going from 3,000 to 20,000 games raised the score against `Minimax(3)` from 24% to about 40%.
- **Teacher depth 3 has a ceiling.** 40,000 games were not better than 20,000.
- **A deeper teacher** (depth 4) only helps when there are many positions. Depth 5 is too slow (about 23 ms per position), so too few positions could be generated.
- **Network size.** 64-64-32-1 was better than 64-32-16-1 with large data. 64-128-32-1 gave no further improvement.
- **Held-out error is not the same as playing strength.** Some runs with lower error played worse.
- **Learning rate decay** (run 23) reached the same strength as the final network on two thirds of the data.

### Choosing the final network

Results on 400 games turned out to be too noisy for choosing a network. Run 22 looked best (48.0% against `Minimax(3)`), but on a re-check with 2,000 games it scored only about 37%. Therefore the 9 best candidates were re-checked on 2,000 games against `Minimax(3)` with a different seed.

- The network with the best re-check result wins.
- If two networks differ by less than 2%, the simpler one wins.

Run 17 scored 42.4% on the re-check. The decay run 23 scored about the same, within the margin of error, so run 17 was chosen as the simpler one.

## Tournament

2,000 games per pair, 1,000 with each colour. The first 6 moves of every game are random, all pairs use the same set of openings, seed 3. Total time 15 minutes. Every game is stored in `models/tournament.csv`.

| Pair | Wins | Losses | Draws | Score of the first player |
|---|---|---|---|---|
| Random vs Minimax(1) | 184 | 1789 | 27 | 9.9% |
| Random vs Minimax(2) | 60 | 1927 | 13 | 3.3% |
| Random vs Minimax(3) | 25 | 1974 | 1 | 1.3% |
| Random vs Minimax(4) | 6 | 1994 | 0 | 0.3% |
| Random vs Neural | 61 | 1924 | 15 | 3.4% |
| Minimax(1) vs Minimax(2) | 354 | 1609 | 37 | 18.6% |
| Minimax(1) vs Minimax(3) | 195 | 1781 | 24 | 10.4% |
| Minimax(1) vs Minimax(4) | 75 | 1919 | 6 | 3.9% |
| Minimax(1) vs Neural | 330 | 1650 | 20 | 17.0% |
| Minimax(2) vs Minimax(3) | 564 | 1400 | 36 | 29.1% |
| Minimax(2) vs Minimax(4) | 250 | 1729 | 21 | 13.0% |
| Minimax(2) vs Neural | 848 | 1119 | 33 | 43.2% |
| Minimax(3) vs Minimax(4) | 380 | 1588 | 32 | 19.8% |
| Minimax(3) vs Neural | 1138 | 813 | 49 | 58.1% |
| Minimax(4) vs Neural | 1611 | 373 | 16 | 81.0% |

### Notes

- **Repeated games.** Minimax and the neural player are deterministic, so two of them can only play different games if the openings differ. After 6 random moves there are 7,092 different positions, so some openings repeat by chance. In the pair `Minimax(3)` vs `Neural`, about 130 of 2,000 games are exact repeats. This widens the margin of error from ±2.2% to about ±2.3% and does not change any conclusion. Games with the random player never repeat.
- **Colour.** Every player scores about 2% better with white than with black. Colours are split equally in every pair, so the tables are not affected.
- **Random wins against minimax.** The random player won 25 games against `Minimax(3)` and 6 against `Minimax(4)`. These games ended with a close score. The likely reason is the limited search depth and the hand-made evaluation in the endgame. The openings of these games are in `models/tournament.csv` for later analysis.
