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
    private volatile boolean cancelado;
    private volatile boolean resolvido;
    private long nosExplorados;
    private long passos;
    private long backtracks;
    private int profundidadeMaxima;
    private StepListener ouvinte;

    public interface StepListener {
        void onStep(Step etapa) throws InterruptedException;
    }

    // Guarda os dados de uma etapa para atualizar a interface.
    public static final class Step {
        public enum Type { TRY, PLACE, BACKTRACK, SOLVED, NO_SOLUTION }

        private final Type tipo;
        private final int[][] tabuleiro;
        private final int linha;
        private final int coluna;
        private final int valor;
        private final List<Integer> valoresValidos;
        private final int profundidade;
        private final int tamanhoPilha;
        private final long nosExplorados;
        private final long passos;
        private final long backtracks;

        private Step(Type tipo, int[][] tabuleiro, int linha, int coluna, int valor,
                     List<Integer> valoresValidos, int profundidade, int tamanhoPilha,
                     long nosExplorados, long passos, long backtracks) {
            this.tipo = tipo;
            this.tabuleiro = tabuleiro;
            this.linha = linha;
            this.coluna = coluna;
            this.valor = valor;
            this.valoresValidos = Collections.unmodifiableList(new ArrayList<Integer>(valoresValidos));
            this.profundidade = profundidade;
            this.tamanhoPilha = tamanhoPilha;
            this.nosExplorados = nosExplorados;
            this.passos = passos;
            this.backtracks = backtracks;
        }

        public Type getType() { return tipo; }
        public int[][] getBoard() { return copiarMatriz(tabuleiro); }
        public int getRow() { return linha; }
        public int getColumn() { return coluna; }
        public int getValue() { return valor; }
        public List<Integer> getValidValues() { return valoresValidos; }
        public int getDepth() { return profundidade; }
        public int getStackSize() { return tamanhoPilha; }
        public long getExploredNodes() { return nosExplorados; }
        public long getSteps() { return passos; }
        public long getBacktracks() { return backtracks; }
    }

    public Dfs(Matriz matriz) {
        this.matriz = matriz;
    }

    public void setStepListener(StepListener ouvinte) {
        this.ouvinte = ouvinte;
    }
    public void cancel() {
        cancelado = true;
    }
    public boolean isSolved() { return resolvido; }
    public long getExploredNodes() { return nosExplorados; }
    public long getSteps() { return passos; }
    public long getBacktracks() { return backtracks; }
    public int getMaximumDepth() { return profundidadeMaxima; }

    public Pair findBlank() {
        int[][] tabuleiro = matriz.getMatriz();
        for (int linha = 0; linha < n; linha++) {
            for (int coluna = 0; coluna < n; coluna++) {
                if (tabuleiro[linha][coluna] == 0) {
                    return new Pair(linha, coluna);
                }
            }
        }
        return new Pair(-1, -1);
    }

    public boolean isCompleto(Pair posicao) { return posicao.getFirst() == -1; }

    public int[][] dfs() {
        reiniciarContadores();
        int[][] tabuleiro = matriz.getMatriz();
        Stack<Edge> pilha = new Stack<Edge>();
        Pair vazio = findBlank();
        if (isCompleto(vazio)) {
            resolvido = true;
            emitirEtapa(Step.Type.SOLVED, -1, -1, 0, Collections.<Integer>emptyList(), 0, 0);
            return tabuleiro;
        }

        pilha.push(new Edge(vazio.getFirst(), vazio.getSecond(), 1));
        int profundidade = 0;
        while (!pilha.empty() && !cancelado && !Thread.currentThread().isInterrupted()) {
            Edge tentativa = pilha.pop();
            int linha = tentativa.getI();
            int coluna = tentativa.getJ();
            int valor = tentativa.getVal();
            tabuleiro[linha][coluna] = 0;
            List<Integer> valoresValidos = valoresValidos(tabuleiro, linha, coluna);
            emitirEtapa(Step.Type.TRY, linha, coluna, valor, valoresValidos, profundidade, pilha.size());

            while (valor <= n && !Valid.isValid(tabuleiro, linha, coluna, valor)) {
                valor++;
                passos++;
            }
            if (valor <= n) {
                tabuleiro[linha][coluna] = valor;
                nosExplorados++;
                passos++;
                // Guarda o próximo valor para tentar se for preciso voltar.
                pilha.push(new Edge(linha, coluna, valor + 1));
                profundidade++;
                profundidadeMaxima = Math.max(profundidadeMaxima, profundidade);
                emitirEtapa(Step.Type.PLACE, linha, coluna, valor, valoresValidos, profundidade, pilha.size());

                Pair proximoVazio = findBlank();
                if (isCompleto(proximoVazio)) {
                    resolvido = true;
                    emitirEtapa(Step.Type.SOLVED, linha, coluna, valor, valoresValidos, profundidade, pilha.size());
                    return tabuleiro;
                }
                pilha.push(new Edge(proximoVazio.getFirst(), proximoVazio.getSecond(), 1));
            } else {
                backtracks++;
                profundidade = Math.max(0, profundidade - 1);
                emitirEtapa(Step.Type.BACKTRACK, linha, coluna, 0, valoresValidos, profundidade, pilha.size());
            }
        }

        if (!cancelado && !Thread.currentThread().isInterrupted()) {
            emitirEtapa(Step.Type.NO_SOLUTION, -1, -1, 0, Collections.<Integer>emptyList(), profundidade, pilha.size());
        }
        return tabuleiro;
    }

    public void exibir() {
        int[][] tabuleiro = dfs();
        for (int linha = 0; linha < n; linha++) {
            for (int coluna = 0; coluna < n; coluna++) {
                System.out.print(tabuleiro[linha][coluna] + " ");
            }
            System.out.println();
        }
    }

    private void reiniciarContadores() {
        cancelado = false;
        resolvido = false;
        nosExplorados = 0;
        passos = 0;
        backtracks = 0;
        profundidadeMaxima = 0;
    }

    private List<Integer> valoresValidos(int[][] tabuleiro, int linha, int coluna) {
        List<Integer> valores = new ArrayList<Integer>();
        for (int valor = 1; valor <= n; valor++) {
            if (Valid.isValid(tabuleiro, linha, coluna, valor)) {
                valores.add(valor);
            }
        }
        return valores;
    }

    private void emitirEtapa(Step.Type tipo, int linha, int coluna, int valor,
                      List<Integer> valoresValidos, int profundidade, int tamanhoPilha) {
        if (ouvinte == null) {
            return;
        }
        try {
            ouvinte.onStep(new Step(tipo, copiarMatriz(matriz.getMatriz()), linha, coluna, valor,
                    valoresValidos, profundidade, tamanhoPilha, nosExplorados, passos, backtracks));
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            cancelado = true;
        }
    }

    private static int[][] copiarMatriz(int[][] origem) {
        int[][] copia = new int[n][n];
        for (int linha = 0; linha < n; linha++) {
            System.arraycopy(origem[linha], 0, copia[linha], 0, n);
        }
        return copia;
    }
}
