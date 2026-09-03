package com.example.sudokuia.model;

import com.example.sudokuia.Definicao;

public class Matriz implements Definicao {
    private int[][] matriz = new int[n][n];

    public Matriz() {
        for(int i=0;i<n;i++) {
            for (int j = 0; j < n; j++) {
                matriz[i][j] = 0;
            }
        }
    }

    public int[][] getMatriz() {
        return matriz;
    }

    public void setMatriz(int[][] matriz) {
        this.matriz = matriz;
    }
}
