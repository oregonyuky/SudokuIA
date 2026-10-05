package com.example.sudokuia.solver;

import com.example.sudokuia.Definicao;
import com.example.sudokuia.model.Matriz;
import com.example.sudokuia.utils.Pair;
import com.example.sudokuia.validation.Valid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AEstrela implements Definicao {
    private final Matriz matriz;
    private volatile boolean cancelado;
    private volatile boolean resolvido;
    private long nosExplorados;
    private long passos;
    private long backtracks;
    private int profundidadeMaxima;
    private StepListener listener;

    public interface StepListener {
        void onStep(Step etapa) throws InterruptedException;
    }

    public static final class Step {
        public enum Tipo { TRY, PLACE, BACKTRACK, SOLVED, NO_SOLUTION }

        private final Tipo tipo;
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

        private Step(Tipo tipo, int[][] tabuleiro, int linha, int coluna, int valor,
                     List<Integer> valoresValidos, int profundidade, int tamanhoPilha,
                     long nosExplorados, long passos, long backtracks) {
            this.tipo = tipo;
            this.tabuleiro = copiar(tabuleiro);
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

        public Tipo getTipo() { return tipo; }
        public int[][] getTabuleiro() { return copiar(tabuleiro); }
        public int getLinha() { return linha; }
        public int getColuna() { return coluna; }
        public int getValor() { return valor; }
        public List<Integer> getValoresValidos() { return valoresValidos; }
        public int getProfundidade() { return profundidade; }
        public int getTamanhoPilha() { return tamanhoPilha; }
        public long getNosExplorados() { return nosExplorados; }
        public long getPassos() { return passos; }
        public long getBacktracks() { return backtracks; }

        public Type getType() { return Type.valueOf(tipo.name()); }
        public enum Type { TRY, PLACE, BACKTRACK, SOLVED, NO_SOLUTION }

        public int getRow() { return linha; }
        public int getColumn() { return coluna; }
        public int getValue() { return valor; }
        public List<Integer> getValidValues() { return valoresValidos; }
        public int getDepth() { return profundidade; }
        public int getStackSize() { return tamanhoPilha; }
        public long getExploredNodes() { return nosExplorados; }
        public long getSteps() { return passos; }
    }

    public AEstrela(Matriz matriz) {
        this.matriz = matriz;
    }

    public void setStepListener(StepListener listener) {
        this.listener = listener;
    }

    public void cancelar() {
        cancelado = true;
    }

    public boolean isSolved() {
        return resolvido;
    }

    public boolean estaResolvido() {
        return resolvido;
    }

    public long getExploredNodes() {
        return nosExplorados;
    }

    public long getNosExplorados() {
        return nosExplorados;
    }

    public long getSteps() {
        return passos;
    }

    public long getPassos() {
        return passos;
    }

    public long getBacktracks() {
        return backtracks;
    }

    public long getRetornos() {
        return backtracks;
    }

    public int getMaximumDepth() {
        return profundidadeMaxima;
    }

    public int getProfundidadeMaxima() {
        return profundidadeMaxima;
    }

    public int[][] A() {
        int[][] tabuleiro = copiar(matriz.getMatriz());
        resolverRecursivo(tabuleiro, 0);
        return tabuleiro;
    }

    public int[][] resolver() {
        return A();
    }

    public int[][] solucionar() {
        return A();
    }

    public int[][] solve() {
        return A();
    }

    public int[][] dfs() {
        return A();
    }

    public Pair encontrarVazio() {
        return escolherCelulaMaisRestrita(matriz.getMatriz());
    }

    public Pair findBlank() {
        return encontrarVazio();
    }

    public Pair acharVazio() {
        return encontrarVazio();
    }

    public void exibir() {
        int[][] tabuleiro = A();
        for (int linha = 0; linha < n; linha++) {
            for (int coluna = 0; coluna < n; coluna++) {
                System.out.print(tabuleiro[linha][coluna] + " ");
            }
            System.out.println();
        }
    }

    private boolean resolverRecursivo(int[][] tabuleiro, int profundidade) {
        if (cancelado || Thread.currentThread().isInterrupted()) {
            return false;
        }
        profundidadeMaxima = Math.max(profundidadeMaxima, profundidade);
        Pair vazio = escolherCelulaMaisRestrita(tabuleiro);
        if (vazio.getFirst() == -1) {
            resolvido = true;
            emitirEtapa(Step.Tipo.SOLVED, vazio.getFirst(), vazio.getSecond(), 0,
                    Collections.<Integer>emptyList(), profundidade, profundidade, tabuleiro);
            return true;
        }

        int linha = vazio.getFirst();
        int coluna = vazio.getSecond();
        List<Integer> candidatos = valoresValidos(tabuleiro, linha, coluna);

        if (candidatos.isEmpty()) {
            resolvido = false;
            emitirEtapa(Step.Tipo.BACKTRACK, linha, coluna, 0, candidatos, profundidade, profundidade, tabuleiro);
            return false;
        }

        for (int valor : candidatos) {
            if (cancelado || Thread.currentThread().isInterrupted()) {
                return false;
            }
            nosExplorados++;
            tabuleiro[linha][coluna] = valor;
            passos++;
            emitirEtapa(Step.Tipo.PLACE, linha, coluna, valor, candidatos, profundidade + 1, profundidade + 1, tabuleiro);
            if (resolverRecursivo(tabuleiro, profundidade + 1)) {
                return true;
            }
            // Desfaz a tentativa que não levou à solução.
            tabuleiro[linha][coluna] = 0;
            backtracks++;
            emitirEtapa(Step.Tipo.BACKTRACK, linha, coluna, valor, candidatos, profundidade, profundidade, tabuleiro);
        }

        resolvido = false;
        return false;
    }

    private Pair escolherCelulaMaisRestrita(int[][] tabuleiro) {
        // Sem células vazias, permanece (-1, -1).
        Pair melhorCelula = new Pair(-1, -1);
        int menorQuantidade = Integer.MAX_VALUE;

        for (int linha = 0; linha < n; linha++) {
            for (int coluna = 0; coluna < n; coluna++) {
                if (tabuleiro[linha][coluna] != 0) {
                    continue;
                }

                List<Integer> opcoes = valoresValidos(tabuleiro, linha, coluna);
                int quantidade = opcoes.size();

                if (quantidade == 0) {
                    return new Pair(linha, coluna);
                }

                if (quantidade < menorQuantidade) {
                    menorQuantidade = quantidade;
                    melhorCelula = new Pair(linha, coluna);
                    if (menorQuantidade == 1) {
                        return melhorCelula;
                    }
                }
            }
        }

        return melhorCelula;
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

    private void emitirEtapa(Step.Tipo tipo, int linha, int coluna, int valor,
                        List<Integer> valoresValidos, int profundidade, int tamanhoPilha,
                        int[][] tabuleiro) {
        if (listener == null) {
            return;
        }
        try {
            listener.onStep(new Step(tipo, tabuleiro, linha, coluna, valor,
                    valoresValidos == null ? Collections.<Integer>emptyList() : valoresValidos,
                    profundidade, tamanhoPilha, nosExplorados, passos, backtracks));
        } catch (InterruptedException excecao) {
            Thread.currentThread().interrupt();
            cancelado = true;
        }
    }

    private static int[][] copiar(int[][] origem) {
        int[][] destino = new int[n][n];
        for (int linha = 0; linha < n; linha++) {
            System.arraycopy(origem[linha], 0, destino[linha], 0, n);
        }
        return destino;
    }
}
