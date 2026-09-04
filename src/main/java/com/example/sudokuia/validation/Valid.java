package com.example.sudokuia.validation;

import com.example.sudokuia.Definicao;
import com.example.sudokuia.model.Matriz;

public class Valid implements Definicao {
    public static boolean isValid(int m[][], int l, int c, int num){
        for(int i=0;i<n;i++){
            if(m[i][c]==num || m[l][i]==num)return false;
        }
        for(int i=l/3*3;i<l/3*3+3;i++){
            for(int j=c/3*3;j<c/3*3+3;j++){
                if(m[i][j]==num)return false;
            }
        }
        return true;
    }
}
