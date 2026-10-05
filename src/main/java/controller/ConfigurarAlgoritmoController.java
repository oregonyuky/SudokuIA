package controller;

import com.example.sudokuia.HelloApplication;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;

public class ConfigurarAlgoritmoController {
    @FXML private GridPane gridSudoku;
    @FXML private Label lblNome;
    @FXML private Label lblDificuldade;
    @FXML private Label lblPreenchidas;
    @FXML private Label lblAlgoritmo;
    @FXML private Label lblDescricaoAlgoritmo;
    @FXML private Label lblVelocidade;
    @FXML private RadioButton rbBuscaCega;
    @FXML private RadioButton rbBuscaHeuristica;
    @FXML private ComboBox<String> cbBuscaCega;
    @FXML private ComboBox<String> cbHeuristica;
    @FXML private Slider sliderVelocidade;
    @FXML private ScrollPane scrollConfiguracao;

    private int[][] sudoku;
    private Scene previousScene;
    private boolean previousMaximized;
    private boolean previousFullScreen;

    @FXML
    private void initialize() {
        scrollConfiguracao.setFitToHeight(false);
        scrollConfiguracao.setFitToWidth(true);
        scrollConfiguracao.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollConfiguracao.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        Platform.runLater(() -> {
            if (gridSudoku.getScene() != null && gridSudoku.getScene().getWindow() instanceof Stage) {
                Stage stage = (Stage) gridSudoku.getScene().getWindow();
                stage.setMaximized(true);
                stage.setFullScreen(true);
            }
        });

        cbBuscaCega.getSelectionModel().selectFirst();
        cbHeuristica.getSelectionModel().selectFirst();
        sliderVelocidade.valueProperty().addListener((observable, oldValue, newValue) ->
                lblVelocidade.setText(String.format("%.1fx", newValue.doubleValue())));
        selecionarBuscaCega(null);
    }

    public void setSudoku(int[][] sudoku) {
        this.sudoku = copyBoard(sudoku);
        renderBoard();
        int filled = 0;
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                if (sudoku[row][column] != 0) {
                    filled++;
                }
            }
        }
        lblNome.setText("Sudoku selecionado");
        lblDificuldade.setText("Personalizado");
        lblPreenchidas.setText(filled + " / 81");
    }

    public void setPreviousScene(Scene previousScene) {
        this.previousScene = previousScene;
    }

    public void setPreviousWindowState(boolean maximized, boolean fullScreen) {
        this.previousMaximized = maximized;
        this.previousFullScreen = fullScreen;
    }

    @FXML
    public void trocarSudoku(ActionEvent actionEvent) {
        returnToSudoku(actionEvent);
    }

    @FXML
    public void selecionarBuscaCega(ActionEvent actionEvent) {
        cbBuscaCega.setDisable(false);
        cbHeuristica.setDisable(true);
        updateAlgorithmDescription(cbBuscaCega.getValue());
    }

    @FXML
    public void selecionarBuscaHeuristica(ActionEvent actionEvent) {
        cbBuscaCega.setDisable(true);
        cbHeuristica.setDisable(false);
        updateAlgorithmDescription(cbHeuristica.getValue());
    }

    @FXML
    public void alterarAlgoritmoCego(ActionEvent actionEvent) {
        if (rbBuscaCega.isSelected()) {
            updateAlgorithmDescription(cbBuscaCega.getValue());
        }
    }

    @FXML
    public void alterarHeuristica(ActionEvent actionEvent) {
        if (rbBuscaHeuristica.isSelected()) {
            updateAlgorithmDescription(cbHeuristica.getValue());
        }
    }

    @FXML
    public void sair(ActionEvent actionEvent) {
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    public void voltar(ActionEvent actionEvent) {
        returnToSudoku(actionEvent);
    }

    @FXML
    public void proximo(ActionEvent actionEvent) {
        if (sudoku == null) {
            showError("Nenhum Sudoku foi selecionado.");
            return;
        }
        if (!lblAlgoritmo.getText().contains("DFS")) {
            showError("A tela de resolução está disponível para DFS com Backtracking.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("ResolverSudoku.fxml"));
            Parent root = loader.load();
            ResolverSudokuController controller = loader.getController();
            controller.setConfiguration(sudoku, lblAlgoritmo.getText(), sliderVelocidade.getValue());
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setFullScreen(false);
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (IOException exception) {
            showError("Não foi possível abrir a resolução: " + exception.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void renderBoard() {
        gridSudoku.getChildren().clear();
        if (sudoku == null) {
            return;
        }
        for (int row = 0; row < 9; row++) {
            for (int column = 0; column < 9; column++) {
                Label cell = new Label(sudoku[row][column] == 0 ? "" : Integer.toString(sudoku[row][column]));
                cell.setAlignment(Pos.CENTER);
                cell.setMinSize(30, 30);
                cell.setPrefSize(30, 30);
                cell.setMaxSize(30, 30);
                cell.setStyle("-fx-border-color: #aaaaaa; -fx-background-color: white; -fx-font-weight: bold;");
                gridSudoku.add(cell, column, row);
            }
        }
    }

    private void updateAlgorithmDescription(String algorithm) {
        if (algorithm == null) {
            return;
        }
        lblAlgoritmo.setText(algorithm);
        if (algorithm.contains("DFS")) {
            lblDescricaoAlgoritmo.setText(
                    "Explora os estados em profundidade e volta quando encontra um caminho inválido.");
        } else if (algorithm.contains("Largura")) {
            lblDescricaoAlgoritmo.setText(
                    "Explora os estados nível por nível até encontrar uma solução.");
        } else {
            lblDescricaoAlgoritmo.setText(
                    "Usa a heurística selecionada para priorizar os estados mais promissores.");
        }
    }

    private int[][] copyBoard(int[][] source) {
        int[][] copy = new int[9][9];
        for (int row = 0; row < 9; row++) {
            System.arraycopy(source[row], 0, copy[row], 0, 9);
        }
        return copy;
    }

    private void openScreen(ActionEvent actionEvent, String resource) {
        try {
            Parent root = FXMLLoader.load(HelloApplication.class.getResource(resource));
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setFullScreen(false);
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (IOException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erro de navegação");
            alert.setHeaderText(null);
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }

    private void returnToSudoku(ActionEvent actionEvent) {
        if (previousScene == null) {
            openScreen(actionEvent, "EscolherSudoku.fxml");
            return;
        }

        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setFullScreen(previousFullScreen);
        stage.setScene(previousScene);
        stage.setMaximized(previousMaximized);
    }
}
