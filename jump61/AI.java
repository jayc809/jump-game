package jump61;

import java.util.ArrayList;
import java.util.Random;

import static jump61.Side.*;

/** An automated Player.
 *  @author P. N. Hilfinger
 */
class AI extends Player {

    /** A new player of GAME initially COLOR that chooses moves automatically.
     *  SEED provides a random-number seed used for choosing moves.
     */
    AI(Game game, Side color, long seed) {
        super(game, color);
        _random = new Random(seed);
    }

    @Override
    String getMove() {
        Board board = getGame().getBoard();
        assert getSide() == board.whoseMove();
        int choice = searchForMove();
        getGame().reportMove(board.row(choice), board.col(choice));
        return String.format("%d %d", board.row(choice), board.col(choice));
    }

    /** Return a move after searching the game tree to DEPTH>0 moves
     *  from the current position. Assumes the game is not over. */
    private int searchForMove() {
        Board b = new Board(getBoard());
        assert getSide() == b.whoseMove();
        _foundMove = -5;
        if (getSide() == RED) {
            minMax(b, 4, true, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        } else if (getSide() == BLUE) {
            minMax(b, 4, true, -1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        return _foundMove;
    }

    /** return top 2 moves usinf SIDE, BOARD2. */
    private ArrayList<Integer> getTop2(Side side, Board board2) {
        Board board = new Board(board2);
        ArrayList<Integer> bestMoves = new ArrayList<>();
        bestMoves.add(null);
        bestMoves.add(null);
        if (board.getBoard1().isEmpty()) {
            throw new GameException("empty board");
        }
        if (side == RED) {
            int bestScore = Integer.MIN_VALUE;
            int secondScore = Integer.MIN_VALUE;
            for (int i = 0; i < board.getBoard1().size(); i += 1) {
                if (board.isLegal(RED, i) && board.isLegal(RED)) {
                    Board t = new Board(board);
                    t.addSpot(RED, i);
                    int score = staticEval(t, Integer.MAX_VALUE);
                    if (score > bestScore) {
                        secondScore = bestScore;
                        bestScore = score;
                        bestMoves.set(1, bestMoves.get(0));
                        bestMoves.set(0, i);
                    } else if (score > secondScore) {
                        secondScore = score;
                        bestMoves.set(1, i);
                    }
                }
            }
            return bestMoves;
        } else if (side == BLUE) {
            int bestScore = Integer.MAX_VALUE;
            int secondScore = Integer.MAX_VALUE;
            for (int i = 0; i < board.getBoard1().size(); i += 1) {
                if (board.isLegal(BLUE, i) && board.isLegal(BLUE)) {
                    Board t = new Board(board);
                    t.addSpot(BLUE, i);
                    int score = staticEval(t, Integer.MAX_VALUE);
                    System.out.println("score: " + score + ", move: " + i);
                    if (score < bestScore) {
                        secondScore = bestScore;
                        bestScore = score;
                        bestMoves.set(1, bestMoves.get(0));
                        bestMoves.set(0, i);
                    } else if (score < secondScore) {
                        secondScore = score;
                        bestMoves.set(1, i);
                    }
                }
            }
        }
        return bestMoves;
    }

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _foundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _foundMove. If the game is over
     *  on BOARD, does not set _foundMove. */
    private int minMax(Board board, int depth, boolean saveMove,
                       int sense, int alpha, int beta) {
        if (depth == 0 || board.getWinner() != null) {
            return staticEval(board, Integer.MAX_VALUE);
        }
        if (sense == 1) {
            int bestSoFar = Integer.MIN_VALUE;
            for (int i = 0; i < board.getBoard1().size(); i += 1) {
                if (board.isLegal(RED, i) && board.isLegal(RED)) {
                    Board child = new Board(board);
                    child.addSpot(RED, i);
                    if (saveMove && child.getWinner() == RED) {
                        _foundMove = i;
                        break;
                    }
                    int response = minMax(child,
                            depth - 1, false, -1, alpha, beta);
                    if (saveMove && response >= bestSoFar) {
                        _foundMove = i;
                    }
                    bestSoFar = Integer.max(response, bestSoFar);
                    alpha = Integer.max(bestSoFar, alpha);
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
            return bestSoFar;
        } else if (sense == -1) {
            int bestSoFar = Integer.MAX_VALUE;
            for (int i = 0; i < board.getBoard1().size(); i += 1) {
                if (board.isLegal(BLUE, i) && board.isLegal(BLUE)) {
                    Board child = new Board(board);
                    child.addSpot(BLUE, i);
                    if (saveMove && child.getWinner() == BLUE) {
                        _foundMove = i;
                        break;
                    }
                    int response = minMax(child,
                            depth - 1, false, 1, alpha, beta);
                    if (saveMove && response <= bestSoFar) {
                        _foundMove = i;
                    }
                    bestSoFar = Integer.min(response, bestSoFar);
                    beta = Integer.min(bestSoFar, beta);
                    if (alpha >= beta) {
                        break;
                    }
                }
            }
            return bestSoFar;
        }
        throw new GameException("uncaught condition");
    }

    /** Return a heuristic estimate of the value of board position B.
     *  Use WINNINGVALUE to indicate a win for Red and -WINNINGVALUE to
     *  indicate a win for Blue. */
    private int staticEval(Board b, int winningValue) {
        Side winner = b.getWinner();
        if (winner == RED) {
            return winningValue;
        } else if (winner == BLUE) {
            return -winningValue;
        } else {
            ArrayList board = b.getBoard1();
            int numRed = 0; int numBlue = 0;
            int edgeSideRed = 0; int edgeSideBlue = 0;
            int totalRed = 0; int totalBlue = 0;
            int clusterRed = 0; int clusterBlue = 0;
            double valueRed;
            double valueBlue;
            for (int i = 0; i < board.size(); i += 1) {
                if (b.get(i).getSide().equals(RED)) {
                    numRed += 1;
                    totalRed += b.get(i).getSpots();
                    if (Board.isCornerStatic(i, b.size()) > 0) {
                        edgeSideRed += 2;
                    } else if (Board.isSideStatic(i, b.size()) > 0) {
                        edgeSideRed += 1;
                    }
                    clusterRed += checkNeighbors(b, i,
                            board.size() - 1, 0, RED);
                } else if (b.get(i).getSide().equals(BLUE)) {
                    numBlue += 1;
                    totalBlue += b.get(i).getSpots();
                    if (Board.isCornerStatic(i, b.size()) > 0) {
                        edgeSideBlue += 2;
                    } else if (Board.isSideStatic(i, b.size()) > 0) {
                        edgeSideBlue += 1;
                    }
                    clusterBlue += checkNeighbors(b, i,
                            board.size() - 1, 0, BLUE);
                }
            }
            final double factor1 = 0.5;
            final double factor2 = 0.05;
            final double factor3 = 1.5;
            valueRed = numRed + factor1 * edgeSideRed + factor2
                    * totalRed + factor3 * clusterRed;
            valueBlue = numBlue + factor1 * edgeSideBlue + factor2
                    * totalBlue + factor3 * clusterBlue;
            return (int) (valueRed - valueBlue);
        }
    }

    /** checks the clustering effect using B, S, MAX, MIN, SIDE
     *  and returns a cluster val. */
    static int checkNeighbors(Board b, int s, int max, int min, Side side) {
        int returnVal = 0;
        if (s - 1 >= min) {
            if (b.get(s - 1).getSide().equals(side)) {
                returnVal += 1;
            }
        }
        if (s + 1 <= max) {
            if (b.get(s + 1).getSide().equals(side)) {
                returnVal += 1;
            }
        }
        if (s - b.size() >= min) {
            if (b.get(s - b.size()).getSide().equals(side)) {
                returnVal += 1;
            }
        }
        if (s + b.size() <= max) {
            if (b.get(s + b.size()).getSide().equals(side)) {
                returnVal += 1;
            }
        }
        return returnVal;
    }

    /** A random-number generator used for move selection. */
    private Random _random;

    /** Used to convey moves discovered by minMax. */
    private int _foundMove;
}
