package com.example.sudokuia;

import com.example.sudokuia.model.Matriz;
import com.example.sudokuia.solver.Dfs;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
        int[][] m = {
                {2, 7, 0, 0, 0, 0, 0, 3, 5},
                {0, 3, 8, 0, 5, 9, 2, 1, 0},
                {0, 0, 5, 0, 3, 0, 8, 0, 0},
                {0, 8, 0, 3, 0, 0, 0, 0, 0},
                {0, 6, 3, 9, 0, 7, 1, 8, 0},
                {0, 0, 0, 0, 0, 1, 0, 6, 0},
                {0, 0, 6, 0, 9, 0, 7, 0, 0},
                {0, 9, 2, 6, 7, 0, 3, 4, 0},
                {7, 4, 0, 0, 0, 0, 0, 9, 6}
        };
        Matriz ma = new Matriz();
        ma.setMatriz(m);
        Dfs d = new Dfs(ma);

        d.exibir();
    }
}
