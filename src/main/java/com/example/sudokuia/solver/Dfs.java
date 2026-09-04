package com.example.sudokuia.solver;

import com.example.sudokuia.Definicao;
import com.example.sudokuia.model.Matriz;
import com.example.sudokuia.utils.Edge;
import com.example.sudokuia.utils.Pair;
import com.example.sudokuia.validation.Valid;

import java.util.Stack;

public class Dfs implements Definicao {
    private Matriz matriz;

    public Dfs(Matriz matriz) {
        this.matriz = matriz;
    }

    public Pair findBlank(){
        int[][] m = matriz.getMatriz();
        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){
                if(m[i][j]==0)return new Pair(i, j);
            }
        }
        return new Pair(-1,-1);
    }
    public boolean isCompleto(Pair p){
        return p.getFirst()==-1;
    }
    public int[][] dfs(){
        int[][] m = matriz.getMatriz();
        Stack<Edge> s = new Stack();
        Pair p = findBlank();
        if(isCompleto(p))return m;
        s.push(new Edge(p.getFirst(), p.getSecond(), 1));
        while(!s.empty()){
            Edge e = s.pop();
            int l = e.getI();
            int c = e.getJ();
            int num = e.getVal();
            m[l][c]=0;
            while(num<=9 && !Valid.isValid(m, l, c, num))num++;
            if (num <= 9){
                m[l][c] = num;
                s.push(new Edge(l, c, num+1));
                Pair prox = findBlank();
                if(isCompleto(prox))return m;
                s.push(new Edge(prox.getFirst(), prox.getSecond(), 1));
            }
        }
        return m;
    }
    public void exibir(){
        int[][] m = dfs();
        for(int i=0;i<n;i++){
            for(int j=0;j<n;j++){
                System.out.print(m[i][j]+" ");
            }
            System.out.println();
        }
    }
}
