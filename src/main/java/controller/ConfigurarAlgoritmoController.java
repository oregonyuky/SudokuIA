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

    public void definirSudoku(int[][] sudoku) {
        this.sudoku = copiarTabuleiro(sudoku);
        renderizarTabuleiro();
        int filled = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (sudoku[i][j] != 0) {
                    filled++;
                }
            }
        }
        lblNome.setText("Sudoku selecionado");
        lblDificuldade.setText("Personalizado");
        lblPreenchidas.setText(filled + " / 81");
    }

    public void definirCenaAnterior(Scene previousScene) {
        this.previousScene = previousScene;
    }

    public void definirEstadoAnteriorDaJanela(boolean maximized, boolean fullScreen) {
        this.previousMaximized = maximized;
        this.previousFullScreen = fullScreen;
    }

    @FXML
    public void trocarSudoku(ActionEvent actionEvent) {
        retornarAoSudoku(actionEvent);
    }

    @FXML
    public void selecionarBuscaCega(ActionEvent actionEvent) {
        cbBuscaCega.setDisable(false);
        cbHeuristica.setDisable(true);
        atualizarDescricaoAlgoritmo(cbBuscaCega.getValue());
    }

    @FXML
    public void selecionarBuscaHeuristica(ActionEvent actionEvent) {
        cbBuscaCega.setDisable(true);
        cbHeuristica.setDisable(false);
        atualizarDescricaoAlgoritmo(cbHeuristica.getValue());
    }

    @FXML
    public void alterarAlgoritmoCego(ActionEvent actionEvent) {
        if (rbBuscaCega.isSelected()) {
            atualizarDescricaoAlgoritmo(cbBuscaCega.getValue());
        }
    }

    @FXML
    public void alterarHeuristica(ActionEvent actionEvent) {
        if (rbBuscaHeuristica.isSelected()) {
            atualizarDescricaoAlgoritmo(cbHeuristica.getValue());
        }
    }

    @FXML
    public void sair(ActionEvent actionEvent) {
        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    public void voltar(ActionEvent actionEvent) {
        retornarAoSudoku(actionEvent);
    }

    @FXML
    public void proximo(ActionEvent actionEvent) {
        if (sudoku == null) {
            exibirErro("Nenhum Sudoku foi selecionado.");
            return;
        }
        String algoritmo = lblAlgoritmo.getText();
        if (!(algoritmo.contains("DFS") || algoritmo.contains("MRV") || algoritmo.contains("Minimum Remaining Values"))) {
            exibirErro("A tela de resolução está disponível para DFS com Backtracking ou MRV.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("ResolverSudoku.fxml"));
            Parent root = loader.load();
            ResolverSudokuController controller = loader.getController();
            controller.definirConfiguracao(sudoku, algoritmo, sliderVelocidade.getValue());
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setFullScreen(false);
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (IOException exception) {
            exibirErro("Não foi possível abrir a resolução: " + exception.getMessage());
        }
    }

    private void exibirErro(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void renderizarTabuleiro() {
        gridSudoku.getChildren().clear();
        if (sudoku == null) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                Label cell = new Label(sudoku[i][j] == 0 ? "" : Integer.toString(sudoku[i][j]));
                cell.setAlignment(Pos.CENTER);
                cell.setMinSize(30, 30);
                cell.setPrefSize(30, 30);
                cell.setMaxSize(30, 30);
                cell.setStyle("-fx-border-color: #aaaaaa; -fx-background-color: white; -fx-font-weight: bold;");
                gridSudoku.add(cell, j, i);
            }
        }
    }

    private void atualizarDescricaoAlgoritmo(String algorithm) {
        if (algorithm == null) {
            return;
        }
        lblAlgoritmo.setText(algorithm);
        if (algorithm.contains("DFS")) { lblDescricaoAlgoritmo.setText( "Explora os estados em profundidade e volta quando encontra um caminho inválido.");
        } else if (algorithm.contains("Largura")) { lblDescricaoAlgoritmo.setText( "Explora os estados nível por nível até encontrar uma solução.");
        } else { lblDescricaoAlgoritmo.setText( "Usa a heurística selecionada para priorizar os estados mais promissores."); }
    }

    private int[][] copiarTabuleiro(int[][] source) {
        int[][] copy = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(source[i], 0, copy[i], 0, 9);
        }
        return copy;
    }

    private void abrirTela(ActionEvent actionEvent, String resource) {
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

    private void retornarAoSudoku(ActionEvent actionEvent) {
        if (previousScene == null) {
            abrirTela(actionEvent, "EscolherSudoku.fxml");
            return;
        }

        Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        stage.setFullScreen(previousFullScreen);
        stage.setScene(previousScene);
        stage.setMaximized(previousMaximized);
    }
}
