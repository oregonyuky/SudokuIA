package com.example.sudokuia.solver;

import com.example.sudokuia.Definicao;
import com.example.sudokuia.model.Matriz;
import com.example.sudokuia.utils.Edge;
import com.example.sudokuia.utils.Pair;
import com.example.sudokuia.validation.Valid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/** Busca em profundidade iterativa com backtracking. */
public class Dfs implements Definicao {
    private final Matriz matriz;
    private volatile boolean cancelled;
    private volatile boolean solved;
    private long exploredNodes;
    private long steps;
    private long backtracks;
    private int maximumDepth;
    private StepListener stepListener;

    public interface StepListener {
        void onStep(Step step) throws InterruptedException;
    }

    /** Fotografia imutável de uma etapa, usada pela interface sem acoplá-la ao JavaFX. */
    public static final class Step {
        public enum Type { TRY, PLACE, BACKTRACK, SOLVED, NO_SOLUTION }

        private final Type type;
        private final int[][] board;
        private final int row;
        private final int column;
        private final int value;
        private final List<Integer> validValues;
        private final int depth;
        private final int stackSize;
        private final long exploredNodes;
        private final long steps;
        private final long backtracks;

        private Step(Type type, int[][] board, int row, int column, int value,
                     List<Integer> validValues, int depth, int stackSize,
                     long exploredNodes, long steps, long backtracks) {
            this.type = type;
            this.board = board;
            this.row = row;
            this.column = column;
            this.value = value;
            this.validValues = Collections.unmodifiableList(new ArrayList<Integer>(validValues));
            this.depth = depth;
            this.stackSize = stackSize;
            this.exploredNodes = exploredNodes;
            this.steps = steps;
            this.backtracks = backtracks;
        }

        public Type getType() { return type; }
        public int[][] getBoard() { return copy(board); }
        public int getRow() { return row; }
        public int getColumn() { return column; }
        public int getValue() { return value; }
        public List<Integer> getValidValues() { return validValues; }
        public int getDepth() { return depth; }
        public int getStackSize() { return stackSize; }
        public long getExploredNodes() { return exploredNodes; }
        public long getSteps() { return steps; }
        public long getBacktracks() { return backtracks; }
    }

    public Dfs(Matriz matriz) { this.matriz = matriz; }

    public void setStepListener(StepListener stepListener) { this.stepListener = stepListener; }
    public void cancel() { cancelled = true; }
    public boolean isSolved() { return solved; }
    public long getExploredNodes() { return exploredNodes; }
    public long getSteps() { return steps; }
    public long getBacktracks() { return backtracks; }
    public int getMaximumDepth() { return maximumDepth; }

    public Pair findBlank() {
        int[][] board = matriz.getMatriz();
        for (int row = 0; row < n; row++) {
            for (int column = 0; column < n; column++) {
                if (board[row][column] == 0) return new Pair(row, column);
            }
        }
        return new Pair(-1, -1);
    }

    public boolean isCompleto(Pair position) { return position.getFirst() == -1; }

    public int[][] dfs() {
        resetMetrics();
        int[][] board = matriz.getMatriz();
        Stack<Edge> stack = new Stack<Edge>();
        Pair blank = findBlank();
        if (isCompleto(blank)) {
            solved = true;
            emit(Step.Type.SOLVED, -1, -1, 0, Collections.<Integer>emptyList(), 0, 0);
            return board;
        }

        stack.push(new Edge(blank.getFirst(), blank.getSecond(), 1));
        int depth = 0;
        while (!stack.empty() && !cancelled && !Thread.currentThread().isInterrupted()) {
            Edge edge = stack.pop();
            int row = edge.getI();
            int column = edge.getJ();
            int value = edge.getVal();
            board[row][column] = 0;
            List<Integer> validValues = validValues(board, row, column);
            emit(Step.Type.TRY, row, column, value, validValues, depth, stack.size());

            while (value <= n && !Valid.isValid(board, row, column, value)) {
                value++;
                steps++;
            }
            if (value <= n) {
                board[row][column] = value;
                exploredNodes++;
                steps++;
                stack.push(new Edge(row, column, value + 1));
                depth++;
                maximumDepth = Math.max(maximumDepth, depth);
                emit(Step.Type.PLACE, row, column, value, validValues, depth, stack.size());

                Pair next = findBlank();
                if (isCompleto(next)) {
                    solved = true;
                    emit(Step.Type.SOLVED, row, column, value, validValues, depth, stack.size());
                    return board;
                }
                stack.push(new Edge(next.getFirst(), next.getSecond(), 1));
            } else {
                backtracks++;
                depth = Math.max(0, depth - 1);
                emit(Step.Type.BACKTRACK, row, column, 0, validValues, depth, stack.size());
            }
        }

        if (!cancelled && !Thread.currentThread().isInterrupted()) {
            emit(Step.Type.NO_SOLUTION, -1, -1, 0, Collections.<Integer>emptyList(), depth, stack.size());
        }
        return board;
    }

    public void exibir() {
        int[][] board = dfs();
        for (int row = 0; row < n; row++) {
            for (int column = 0; column < n; column++) System.out.print(board[row][column] + " ");
            System.out.println();
        }
    }

    private void resetMetrics() {
        cancelled = false;
        solved = false;
        exploredNodes = 0;
        steps = 0;
        backtracks = 0;
        maximumDepth = 0;
    }

    private List<Integer> validValues(int[][] board, int row, int column) {
        List<Integer> values = new ArrayList<Integer>();
        for (int value = 1; value <= n; value++) {
            if (Valid.isValid(board, row, column, value)) values.add(value);
        }
        return values;
    }

    private void emit(Step.Type type, int row, int column, int value,
                      List<Integer> validValues, int depth, int stackSize) {
        if (stepListener == null) return;
        try {
            stepListener.onStep(new Step(type, copy(matriz.getMatriz()), row, column, value,
                    validValues, depth, stackSize, exploredNodes, steps, backtracks));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            cancelled = true;
        }
    }

    private static int[][] copy(int[][] source) {
        int[][] result = new int[n][n];
        for (int row = 0; row < n; row++) System.arraycopy(source[row], 0, result[row], 0, n);
        return result;
    }
}
