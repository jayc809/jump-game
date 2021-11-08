package jump61;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Formatter;

import java.util.function.Consumer;

import static jump61.Side.*;
import static jump61.Square.INITIAL;
import static jump61.Square.square;

/** Represents the state of a Jump61 game.  Squares are indexed either by
 *  row and column (between 1 and size()), or by square number, numbering
 *  squares by rows, with squares in row 1 numbered from 0 to size()-1, in
 *  row 2 numbered from size() to 2*size() - 1, etc. (i.e., row-major order).
 *
 *  A Board may be given a notifier---a Consumer<Board> whose
 *  .accept method is called whenever the Board's contents are changed.
 *
 *  @author Jay Chiang
 */
class Board {

    /** An uninitialized Board.  Only for use by subtypes. */
    protected Board() {
        _notifier = NOP;
    }

    /** An N x N board in initial configuration. */
    Board(int N) {
        this();
        _size = N;
        for (int i = 0; i < N * N; i += 1) {
            _board.add(Square.INITIAL);
        }
        _history.add(new ArrayList<>(_board));
        _readonlyBoard = new ConstantBoard(this);
    }

    /** A board whose initial contents are copied from BOARD0, but whose
     *  undo history is clear, and whose notifier does nothing. */
    Board(Board board0) {
        this(board0.size());
        copy(board0);
        _readonlyBoard = new ConstantBoard(this);
    }

    /** Returns a readonly version of this board. */
    Board readonlyBoard() {
        return _readonlyBoard;
    }

    /** (Re)initialize me to a cleared board with N squares on a side. Clears
     *  the undo history and sets the number of moves to 0. */
    void clear(int N) {
        _size = N;
        _board = new ArrayList<>();
        int bigN = N * N;
        for (int i = 0; i < bigN; i += 1) {
            _board.add(Square.INITIAL);
        }
        _history = new ArrayList<>();
        _history.add(new ArrayList<>(_board));
        _numMoves = 0;
        announce();
    }

    /** Copy the contents of BOARD into me. */
    void copy(Board board) {
        if (size() == board.size()) {
            for (int i = 0; i < board.getBoard1().size(); i += 1) {
                Square square = board.getBoard1().get(i);
                this._board.set(i, Square.square(square.getSide(),
                        square.getSpots()));
            }
        } else {
            _size = board.size();
            _board = new ArrayList<>();
            for (int i = 0; i < board.getBoard1().size(); i += 1) {
                this._board.add(Square.INITIAL);
            }
            for (int i = 0; i < board.getBoard1().size(); i += 1) {
                Square square = board.getBoard1().get(i);
                this._board.set(i, Square.square(square.getSide(),
                        square.getSpots()));
            }
        }
        _history = new ArrayList<>();
        _history.add(_board);
        _numMoves = 0;
    }

    /** Copy the contents of BOARD into me, without modifying my undo
     *  history. Assumes BOARD and I have the same size. */
    private void internalCopy(Board board) {
        if (size() == board.size()) {
            for (int i = 0; i < board._board.size(); i += 1) {
                Square square = board._board.get(i);
                _board.set(i, Square.square(square.getSide(),
                        square.getSpots()));
            }
        } else {
            _size = board.size();
            _board = new ArrayList<>();
            for (int i = 0; i < board._board.size(); i += 1) {
                _board.add(Square.INITIAL);
            }
            for (int i = 0; i < board._board.size(); i += 1) {
                Square square = board._board.get(i);
                _board.set(i, Square.square(square.getSide(),
                        square.getSpots()));
            }
        }
    }

    /** Return the number of rows and of columns of THIS. */
    int size() {
        return _size;
    }

    /** Returns the contents of the square at row R, column C
     *  1 <= R, C <= size (). */
    Square get(int r, int c) {
        return get(sqNum(r, c));
    }

    /** Returns the contents of square #N, numbering squares by rows, with
     *  squares in row 1 number 0 - size()-1, in row 2 numbered
     *  size() - 2*size() - 1, etc. */
    Square get(int n) {
        return _board.get(n);
    }

    /** Returns the total number of spots on the board. */
    int numPieces() {
        int numSpots = 0;
        for (int i = 0; i < _board.size(); i += 1) {
            numSpots += get(i).getSpots();
        }
        return numSpots;
    }

    /** Returns the Side of the player who would be next to move.  If the
     *  game is won, this will return the loser (assuming legal position). */
    Side whoseMove() {
        return ((numPieces() + size()) & 1) == 0 ? RED : BLUE;
    }

    /** Return true iff row R and column C denotes a valid square. */
    final boolean exists(int r, int c) {
        return 1 <= r && r <= size() && 1 <= c && c <= size();
    }

    /** Return true iff S is a valid square number. */
    final boolean exists(int s) {
        int N = size();
        return 0 <= s && s < N * N;
    }

    /** Return the row number for square #N. */
    final int row(int n) {
        return n / size() + 1;
    }

    /** Return the column number for square #N. */
    final int col(int n) {
        return n % size() + 1;
    }

    /** Return the square number of row R, column C. */
    final int sqNum(int r, int c) {
        return (c - 1) + (r - 1) * size();
    }

    /** Return a string denoting move (ROW, COL)N. */
    String moveString(int row, int col) {
        return String.format("%d %d", row, col);
    }

    /** Return a string denoting move N. */
    String moveString(int n) {
        return String.format("%d %d", row(n), col(n));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
        to square at row R, column C. */
    boolean isLegal(Side player, int r, int c) {
        return isLegal(player, sqNum(r, c));
    }

    /** Returns true iff it would currently be legal for PLAYER to add a spot
     *  to square #N. */
    boolean isLegal(Side player, int n) {
        return get(n).getSide().equals(player)
                || get(n).getSide().equals(WHITE);
    }

    /** Returns true iff PLAYER is allowed to move at this point. */
    boolean isLegal(Side player) {
        return whoseMove().equals(player);
    }

    /** Returns the winner of the current position, if the game is over,
     *  and otherwise null. */
    final Side getWinner() {
        Side winningSide = get(0).getSide();
        if (winningSide.equals(WHITE)) {
            return null;
        }
        for (int i = 0; i < _board.size(); i += 1) {
            if (!(winningSide.equals(get(i).getSide()))) {
                return null;
            }
        }
        return winningSide;
    }

    /** Return the number of squares of given SIDE. */
    int numOfSide(Side side) {
        int numSquares = 0;
        for (int i = 0; i < _board.size(); i += 1) {
            if (get(i).getSide().equals(side)) {
                numSquares += 1;
            }
        }
        return numSquares;
    }

    /** Add a spot from PLAYER at row R, column C.  Assumes
     *  isLegal(PLAYER, R, C). */
    void addSpot(Side player, int r, int c) {
        addSpot(player, sqNum(r, c));
    }

    /** Add a spot from PLAYER at square #N.  Assumes isLegal(PLAYER, N). */
    void addSpot(Side player, int n) {
        if (isLegal(player, n) && isLegal(player)) {
            Square square = get(n);
            if (square.equals(INITIAL)) {
                internalSet(n, 2, player);
            } else {
                int numSpotsOriginal = square.getSpots();
                internalSet(n, numSpotsOriginal + 1, player);
            }
            jump(n);
            _numMoves += 1;
            _history.add(new ArrayList<>(_board));
        } else {
            ArrayList<Integer> i = new ArrayList();
            i.get(1);
        }
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white). */
    void set(int r, int c, int num, Side player) {
        internalSet(r, c, num, player);
        announce();
    }

    /** Set the square at row R, column C to NUM spots (0 <= NUM), and give
     *  it color PLAYER if NUM > 0 (otherwise, white).  Does not announce
     *  changes. */
    private void internalSet(int r, int c, int num, Side player) {
        internalSet(sqNum(r, c), num, player);
    }

    /** Set the square #N to NUM spots (0 <= NUM), and give it color PLAYER
     *  if NUM > 0 (otherwise, white). Does not announce changes. */
    private void internalSet(int n, int num, Side player) {
        if (num <= 0) {
            _board.set(n, INITIAL);
        } else {
            _board.set(n, Square.square(player, num));
        }
    }

    /** Undo the effects of one move (that is, one addSpot command).  One
     *  can only undo back to the last point at which the undo history
     *  was cleared, or the construction of this Board. */
    void undo() {
        if (_history.size() > 0) {
            ArrayList<Square> prevBoard = new ArrayList<>(
                    _history.get(_history.size() - 2));
            _board = new ArrayList<>(prevBoard);
            _history.remove(_history.size() - 1);
            _numMoves -= 1;
        }
    }

    /** returns history.*/
    public ArrayList<ArrayList<Square>> getHistory() {
        return _history;
    }

    /** Record the beginning of a move in the undo history. */
    private void markUndo() {
        if (_history.size() > 0) {
            ArrayList<Square> prevBoard =
                    new ArrayList<>(_history.get(_history.size() - 2));
            _history = new ArrayList<>();
            _history.add(prevBoard);
            _numMoves = 0;
        }
    }

    /** Add DELTASPOTS spots of side PLAYER to row R, column C,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int r, int c, int deltaSpots) {
        internalSet(r, c, deltaSpots + get(r, c).getSpots(), player);
    }

    /** Add DELTASPOTS spots of color PLAYER to square #N,
     *  updating counts of numbers of squares of each color. */
    private void simpleAdd(Side player, int n, int deltaSpots) {
        internalSet(n, deltaSpots + get(n).getSpots(), player);
    }

    /** Used in jump to keep track of squares needing processing.  Allocated
     *  here to cut down on allocations. */
    private final ArrayDeque<Integer> _workQueue = new ArrayDeque<>();

    /** helper for jump for corners using S and CORNERVAL. */
    private void jumpCorner(int s, int cornerVal) {
        Square square = get(s);
        Side player = square.getSide();
        if (square.getSpots() >= 3) {
            internalSet(s, 1, player);
            if (cornerVal == 1) {
                internalSet(1, get(1).getSpots() + 1, player);
                internalSet(_size, get(_size).getSpots() + 1, player);
                _workQueue.offer(1);
                _workQueue.offer(_size);
            } else if (cornerVal == 2) {
                internalSet(_size - 2,
                        get(_size - 2).getSpots() + 1, player);
                internalSet(_size * 2 - 1,
                        get(_size * 2 - 1).getSpots() + 1, player);
                _workQueue.offer(_size - 2);
                _workQueue.offer(_size * 2 - 1);
            } else if (cornerVal == 3) {
                internalSet(_size * _size - _size + 1,
                        get(_size * _size - _size + 1).getSpots() + 1, player);
                internalSet(_size * (_size - 2),
                        get(_size * (_size - 2)).getSpots() + 1, player);
                _workQueue.offer(_size * _size - _size + 1);
                _workQueue.offer(_size * (_size - 2));
            } else if (cornerVal == 4) {
                internalSet(_size * _size - 2,
                        get(_size * _size - 2).getSpots() + 1, player);
                internalSet(_size * _size - _size - 1,
                        get(_size * _size - _size - 1).getSpots() + 1, player);
                _workQueue.offer(_size * _size - 2);
                _workQueue.offer(_size * _size - _size - 1);
            }
        }
    }

    /** helper for jump for sides using S and SIDEVAL. */
    private void jumpSide(int s, int sideVal) {
        Square square = get(s);
        Side player = square.getSide();
        if (square.getSpots() >= 4) {
            internalSet(s, 1, player);
            if (sideVal == 1) {
                internalSet(s + 1, get(s + 1).getSpots() + 1, player);
                internalSet(s - _size, get(s - _size).getSpots() + 1, player);
                internalSet(s + _size, get(s + _size).getSpots() + 1, player);
                _workQueue.offer(s + 1);
                _workQueue.offer(s - _size);
                _workQueue.offer(s + _size);
            } else if (sideVal == 2) {
                internalSet(s + 1, get(s + 1).getSpots() + 1, player);
                internalSet(s - 1, get(s - 1).getSpots() + 1, player);
                internalSet(s + _size, get(s + _size).getSpots() + 1, player);
                _workQueue.offer(s + 1);
                _workQueue.offer(s - 1);
                _workQueue.offer(s + _size);
            } else if (sideVal == 3) {
                internalSet(s - 1, get(s - 1).getSpots() + 1, player);
                internalSet(s - _size, get(s - _size).getSpots() + 1, player);
                internalSet(s + _size, get(s + _size).getSpots() + 1, player);
                _workQueue.offer(s - 1);
                _workQueue.offer(s - _size);
                _workQueue.offer(s + _size);
            } else if (sideVal == 4) {
                internalSet(s + 1, get(s + 1).getSpots() + 1, player);
                internalSet(s - 1, get(s - 1).getSpots() + 1, player);
                internalSet(s - _size, get(s - _size).getSpots() + 1, player);
                _workQueue.offer(s + 1);
                _workQueue.offer(s - 1);
                _workQueue.offer(s - _size);
            }
        }
    }

    /** helper for jump for middles using S. */
    public void jumpMiddle(int s) {
        Square square = get(s);
        Side player = square.getSide();
        if (square.getSpots() >= 5) {
            internalSet(s, 1, player);
            internalSet(s + 1, get(s + 1).getSpots() + 1, player);
            internalSet(s - 1, get(s - 1).getSpots() + 1, player);
            internalSet(s - _size, get(s - _size).getSpots() + 1, player);
            internalSet(s + _size, get(s + _size).getSpots() + 1, player);
            _workQueue.offer(s + 1);
            _workQueue.offer(s - 1);
            _workQueue.offer(s - _size);
            _workQueue.offer(s + _size);
        }
    }

    /** Do all jumping on this board, assuming that initially, S is the only
     *  square that might be over-full. */
    private void jump(int s) {
        Side winner = getWinner();
        if (winner == null) {
            int cornerVal = isCorner(s);
            int sideVal = isSide(s);
            if (cornerVal > 0) {
                jumpCorner(s, cornerVal);
            } else if (sideVal > 0) {
                jumpSide(s, sideVal);
            } else {
                jumpMiddle(s);
            }
            while (!_workQueue.isEmpty()) {
                jump(_workQueue.pop());
            }
        }
    }

    /** checks if S is a corner returns int. */
    private int isCorner(int s) {
        if (s == 0) {
            return 1;
        } else if (s == (_size - 1)) {
            return 2;
        } else if (s == (_size * _size - _size)) {
            return 3;
        } else if (s == (_size * _size - 1)) {
            return 4;
        } else {
            return 0;
        }
    }

    /** checks if S and SIZE is a corner returns int. */
    static int isCornerStatic(int s, int size) {
        if (s == 0) {
            return 1;
        } else if (s == (size - 1)) {
            return 2;
        } else if (s == (size * size - size)) {
            return 3;
        } else if (s == (size * size - 1)) {
            return 4;
        } else {
            return 0;
        }
    }

    /** checks if S and SIZE is a side returns int. */
    private int isSide(int s) {
        if (isCorner(s) == 0) {
            if (s % _size == 0) {
                return 1;
            } else if (s < _size - 1 && s > 0) {
                return 2;
            } else if (s % _size == _size - 1) {
                return 3;
            } else if ((s < (_size * _size - 1)
                    && s > (_size * _size - _size))) {
                return 4;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /** checks if S and SIZE is a side returns int. */
    static int isSideStatic(int s, int size) {
        if (isCornerStatic(s, size) == 0) {
            if (s % size == 0) {
                return 1;
            } else if (s < size - 1 && s > 0) {
                return 2;
            } else if (s % size == size - 1) {
                return 3;
            } else if ((s < (size * size - 1)
                    && s > (size * size - size))) {
                return 4;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    /** Returns my dumped representation. */
    @Override
    public String toString() {
        String result = "";
        result += "===\n";
        for (int i = 0; i < _board.size(); i += 1) {
            if (i % _size == 0) {
                result += "    ";
            }
            result += get(i).getSpots();
            Side player = get(i).getSide();
            if (player.equals(WHITE)) {
                result += "-";
            } else if (player.equals(RED)) {
                result += "r";
            } else {
                result += "b";
            }
            result += " ";
            if (i % _size == (_size - 1)) {
                result += "\n";
            }
        }
        result += "===\n";
        return result;
    }

    /** returns the board. */
    public ArrayList<Square> getBoard1() {
        return _board;
    }

    /** Returns an external rendition of me, suitable for human-readable
     *  textual display, with row and column numbers.  This is distinct
     *  from the dumped representation (returned by toString). */
    public String toDisplayString() {
        String[] lines = toString().trim().split("\\R");
        Formatter out = new Formatter();
        for (int i = 1; i + 1 < lines.length; i += 1) {
            out.format("%2d %s%n", i, lines[i].trim());
        }
        out.format("  ");
        for (int i = 1; i <= size(); i += 1) {
            out.format("%3d", i);
        }
        return out.toString();
    }

    /** Returns the number of neighbors of the square at row R, column C. */
    int neighbors(int r, int c) {
        int size = size();
        int n;
        n = 0;
        if (r > 1) {
            n += 1;
        }
        if (c > 1) {
            n += 1;
        }
        if (r < size) {
            n += 1;
        }
        if (c < size) {
            n += 1;
        }
        return n;
    }

    /** Returns the number of neighbors of square #N. */
    int neighbors(int n) {
        return neighbors(row(n), col(n));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Board)) {
            return false;
        } else {
            Board B = (Board) obj;
            for (int i = 0; i < _board.size(); i += 1) {
                Square square = get(i);
                Square squareB = B.get(i);
                if (!(square.getSide().equals(squareB.getSide())
                        && square.getSpots() == squareB.getSpots())) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    public int hashCode() {
        return numPieces();
    }

    /** Set my notifier to NOTIFY. */
    public void setNotifier(Consumer<Board> notify) {
        _notifier = notify;
        announce();
    }

    /** Take any action that has been set for a change in my state. */
    private void announce() {
        _notifier.accept(this);
    }

    /** A notifier that does nothing. */
    private static final Consumer<Board> NOP = (s) -> { };

    /** A read-only version of this Board. */
    private ConstantBoard _readonlyBoard;

    /** Use _notifier.accept(B) to announce changes to this board. */
    private Consumer<Board> _notifier;

    /** list of moves. */
    private ArrayList<ArrayList<Square>> _history = new ArrayList<>();

    /** board contents. */
    private ArrayList<Square> _board = new ArrayList<>();

    /** number of moves. */
    private int _numMoves = 0;

    /** board size. */
    private int _size;

}
