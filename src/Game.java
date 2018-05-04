/*
Short and sweet minesweeper impl:
There are a few types of piece:
Unknown -> renders as empty.
Mine -> renders as X, ends game.
Question -> renders as ?. Asks for confirmation if a player enters it
Flag -> renders as M. Asks for confirmation if a player enters it
clicked -> renders as a ·. Cannot be clicked.
Edge -> renders as a number counting nearby mines.

Rules: seed a NxM board with K mines.
ask a player for input: clear x y ; flag x y ; question x y;
h prints the help menu.


Winning is defined as follows: all moves are exhausted, meaning they are clicked or they are mines.
 */

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Game {
    private Random rand = new Random();

    private enum Action {
        flag,
        clear,
        question
    }

    private enum State {
        clicked,
        flagged,
        questioned,
        none
    }

    private static class BoardLocation {
        private boolean isMine = false;
        private byte neighborCount;
        private State state = State.none;

        private BoardLocation() {
        }

        private void setNeighbors(byte count) {
            neighborCount = count;
        }

        private void setMine() {
            isMine = true;
        }

        private char render() {
            if (isMine && state == State.clicked) {
                return clickedMine;
            }

            switch (state) {
                case flagged:
                    return flaggedSymbol;
                case questioned:
                    return questionSymbol;
                case clicked:
                    if (neighborCount == 0)
                        return clickedSymbol;
                    else
                        return (char) (neighborCount + '0'); //clever trick to compute the character 1-9 from the value
                case none:
                default:
                    return unClickedSymbol;
            }
        }
    }


    private enum Response {
        win,
        lose,
        ok,
        confirm,
        reject_invalid,
        reject_already_done
    }


    private static final char clickedMine = 'X';
    private static final char flaggedSymbol = 'M';
    private static final char questionSymbol = '?';
    private static final char clickedSymbol = ' ';
    private static final char unClickedSymbol = '·';

    private BoardLocation[][] board;
    private int remainingMaybeMines;
    private int remainingMoves;
    private int gridSize;
    private int boardWidth;
    private String headerRow;

    /*
        Generate a new game as follows: create the NxM board.
        Create a set of mined pieces

     */
    public Game(int width, int height, int mineCount) {
        board = new BoardLocation[height][width];
        remainingMaybeMines = mineCount;
        remainingMoves = width * height - mineCount; //Remaining valid moves;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                board[j][i] = new BoardLocation();
            }
        }

        while (mineCount > 0) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            if (!board[y][x].isMine) {
                board[y][x].setMine();
                mineCount--;
            }
        }

        byte mineCt;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                mineCt = 0;
                //Loop over the 8 spaces around us checking to see if they are inbounds and update mine count if so.

                for (int y = j - 1; y <= j + 1; y++) {
                    for (int x = i - 1; x <= i + 1; x++) {
                        if (x < 0 || x >= width || y < 0 || y >= height)
                            continue;
                        mineCt += board[y][x].isMine ? 1 : 0;
                    }
                }
                board[j][i].setNeighbors(mineCt);
            }
        }

        gridSize = Math.max(("" + (width - 1)).length(), ("" + (height - 1)).length()) + 2;
        boardWidth = Math.max(gridSize * width + 1, ("Mines: " + remainingMaybeMines).length());

        headerRow = vCenter(String.format("%" + gridSize + "s", "") + IntStream.range(0, width).mapToObj(i -> center("" + i, gridSize)
        ).collect(Collectors.joining("")));
    }

    /*
    Renders the game using this format:
       Mines: 5
         1 2 3
       1 · · ·
       2 · · ·
       3 · · ·
     */
    public String render() {
        StringBuilder renderedBoard = new StringBuilder(center("Mines: " + remainingMaybeMines, boardWidth) + "\n" + headerRow);
        for (int row = 0; row < board.length; row++) {
            renderedBoard.append(vCenter(
                    center("" + row, gridSize) + Arrays.stream(board[row]).map(bl -> center("" + bl.render(), gridSize)).collect(Collectors.joining(""))
            ));
        }
        return renderedBoard.toString();
    }

    private String center(String s, int width) {
        int paddingDim = width - s.length();
        int leftPad = s.length() + paddingDim / 2;
        return String.format("%-" + width + "s", String.format("%" + leftPad + "s", s));
    }

    // pad 3, pad 4 : 1,1,a,1 or 1,a,1
    private String vCenter(String s) {
        int topPad = gridSize / 2;
        int botPad = gridSize - topPad;
        return new String(new char[topPad]).replace("\0", "\n") + s + new String(new char[botPad]).replace("\0", "\n");
    }

    /*
        Results:
        board updates,
            -> win game
            -> lose game
            -> game continues
        board requires confirmation
        board rejects move for cause
     */
    private Response move(int x, int y, Action action, boolean confirmed) {
        if (x < 0 || y > board.length || y < 0 || x > board[y].length) {
            return Response.reject_invalid;
        }
        BoardLocation move = board[y][x];
        if (move.state == State.clicked) {
            return Response.reject_already_done;
        }

        if (!confirmed && (move.state == State.flagged || move.state == State.questioned))
            return Response.confirm;

        switch (action) {
            case clear:
                move.state = State.clicked;
                break;
            case flag:
                move.state = State.flagged;
                break;
            case question:
                move.state = State.questioned;
        }

        if (move.isMine) {
            if (action == Action.clear)
                return Response.lose;
            if (action == Action.flag)
                remainingMaybeMines--;
            return Response.ok;
        }

        if (action == Action.clear) {
            //if we cleared, and the spot had no neighboring mines, check neighbors to see if they have mines. if they don't, clear them too
            if (move.neighborCount == 0) {
                recursiveClear(x, y);
            }
            remainingMoves--;
            if (remainingMoves == 0)
                return Response.win;
        }

        return Response.ok;
    }

    private void recursiveClear(int i, int j) {

        for (int y = j - 1; y <= j + 1; y++) {
            for (int x = i - 1; x <= i + 1; x++) {
                if ((x == i && y == j) || x < 0 || y < 0 || y >= board.length || x >= board[y].length)
                    continue;
                BoardLocation considered = board[y][x];
                if (considered.neighborCount == 0 && considered.state != State.clicked) {
                    considered.state = State.clicked;
                    remainingMoves--;
                    recursiveClear(x, y);
                } else if (considered.state != State.clicked) {
                    considered.state = State.clicked;
                    remainingMoves--;
                }
            }
        }
    }

    private void win() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                BoardLocation move = board[i][j];
                if (move.isMine) {
                    move.state = State.flagged;
                } else {
                    move.state = State.clicked;
                }
            }
            remainingMaybeMines = 0;
            remainingMoves = 0;
        }
    }



    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);

        System.out.println("Welcome To minesweeper");
        try {
            int width;
            int height;
            int mineCount;
            while (true) {
                System.out.println("Please enter game dimensions: width height number_of_mines");
                String line = scan.nextLine();
                String[] entries = line.trim().split(" ");
                if (entries.length == 3) {
                    width = Integer.parseInt(entries[0]);
                    height = Integer.parseInt(entries[1]);
                    mineCount = Integer.parseInt(entries[2]);
                    if (width < 0 || height < 0) {
                        System.out.println("Width and height must be positive");
                        continue;
                    }
                    break;
                }
            }

            while (mineCount >= width * height) {
                System.out.println("Mine count too large, please enter a smaller number of mines");
                mineCount = getInt("Please enter the desired number of mines", scan);
            }

            Game game = new Game(width, height, mineCount);

            outer:
            while (true) {
                System.out.println(game.render());
                System.out.println("Please enter a move: x y [action], or h to print out valid actions");
                String line = scan.nextLine();
                switch (line) {
                    case "h":
                        System.out.println("flag: mark a space as mined.\n clear: clear a space, if no action is provided this is the default action.\nquestion: mark a space as maybe mined.");
                        break;
                    case "cheat":
                        System.out.println("Congratulations you won!");
                        game.win();
                        System.out.println(game.render());
                        return;
                    default:
                        String[] entries = line.trim().split(" ");
                        try {
                            int x = Integer.parseInt(entries[0]);
                            int y = Integer.parseInt(entries[1]);
                            Action action = entries.length == 3 ? Action.valueOf(entries[2]) : Action.clear;

                            Response response = game.move(x, y, action, false);
                            if (response == Response.confirm) {
                                System.out.println("You marked this tile, are you sure you want to change it's marking? y/n");
                                while (true) {
                                    String token = scan.next();
                                    if (token.equals("y")) {
                                        response = game.move(x, y, action, true);
                                        break;
                                    } else if (token.equals("n"))
                                        continue outer;
                                }
                            }


                            switch (response) {
                                case win:
                                    System.out.println("Congratulations you won!");
                                    game.win();
                                    System.out.println(game.render());
                                    return;
                                case lose:
                                    System.out.println("Unfortunately you lost.");
                                    System.out.println(game.render());
                                    return;
                                case reject_invalid:
                                    System.out.println("Please enter an in bounds move");
                                    break;
                                case reject_already_done:
                                    System.out.println("This tile has already been clicked");
                                    break;
                            }

                        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
                        }
                }
            }

        } catch (NoSuchElementException | IllegalStateException e) {
            System.out.println("The game couldn't be started because you closed the input stream");
        }
    }


    private static int getInt(String initial, Scanner scanner) {
        while (true) {
            System.out.println(initial);
            try {
                return scanner.nextInt();
            } catch (InputMismatchException e) {
            }
        }
    }
}
