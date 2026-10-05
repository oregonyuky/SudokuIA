package controller;

import com.example.sudokuia.HelloApplication;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EscolherSudokuController {
    private static final int SIZE = 9;
    private static final int[][] EXAMPLE = {
            {5, 3, 0, 0, 7, 0, 0, 0, 0},
            {6, 0, 0, 1, 9, 5, 0, 0, 0},
            {0, 9, 8, 0, 0, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 0, 0, 0, 3},
            {4, 0, 0, 8, 0, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 0, 0, 0, 6},
            {0, 6, 0, 0, 0, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 0, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
    };

    @FXML private GridPane gridSudoku;
    @FXML private Label lblNome;
    @FXML private Label lblDificuldade;
    @FXML private Label lblPreenchidas;
    @FXML private Label lblDescricao;

    private final TextField[][] cells = new TextField[SIZE][SIZE];
    private boolean updatingBoard;

    @FXML
    private void initialize() {
        for (Node node : gridSudoku.getChildren()) {
            if (!(node instanceof TextField)) {
                continue;
            }
            TextField cell = (TextField) node;
            int j = gridIndex(GridPane.getColumnIndex(cell));
            int i = gridIndex(GridPane.getRowIndex(cell));
            cells[i][j] = cell;
            applyBlockBorder(cell, i, j);
            cell.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("[1-9]?") ? change : null));
            cell.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!updatingBoard) {
                    removerStateClasses(cell);
                    if (!newValue.isEmpty()) {
                        adicionarStateClasses(cell, "sudoku-current");
                    }
                }
                 atualizarContagemDePreenchidas();
            });
        }

        marcarOsValoresExistentesFixos();
         atualizarContagemDePreenchidas();
    }

    @FXML
    public void inserirManual(ActionEvent actionEvent) {
        limparTabuleiro();
        updateInformation("Manual", "Não definida", "Digite os valores conhecidos diretamente no tabuleiro.");
        cells[0][0].requestFocus();
    }

    @FXML
    public void carregarArquivo(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Carregar Sudoku");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Arquivos de Sudoku", "*.txt", "*.sdk"),
                new FileChooser.ExtensionFilter("Todos os arquivos", "*.*")
        );

        File file = chooser.showOpenDialog(getWindow());
        if (file == null) {
            return;
        }

        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            int[][] board = parseBoard(content);
            aplicarTabuleiro(board, "sudoku-fixed");
            updateInformation(file.getName(), "Importado",
                    "Sudoku carregado do arquivo " + file.getName() + ".");
            if (!validarTabuleiro()) {
                showAlert(Alert.AlertType.WARNING, "Arquivo carregado",
                        "O arquivo contém números repetidos. As células foram destacadas.");
            }
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Erro ao abrir arquivo",
                    "Não foi possível ler o arquivo: " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            showAlert(Alert.AlertType.ERROR, "Arquivo inválido", exception.getMessage());
        }
    }

    @FXML
    public void gerarAleatorio(ActionEvent actionEvent) {
        aplicarTabuleiro(createRandomPuzzle(), "sudoku-fixed");
        updateInformation("Sudoku aleatório", "Médio",
                "Sudoku válido gerado automaticamente com 36 células preenchidas.");
    }

    @FXML
    public void limparTabuleiro(ActionEvent actionEvent) {
        limparTabuleiro();
        updateInformation("Manual", "Não definida", "Tabuleiro vazio.");
    }

    @FXML
    public void verificarTabuleiro(ActionEvent actionEvent) {
        boolean ok = validarTabuleiro();
        int filled = contarCelulasPreenchidas();
        if (!ok) {
            showAlert(Alert.AlertType.ERROR, "Sudoku inválido", "Existem números repetidos em uma linha, coluna ou bloco 3x3.");
        } else if (filled == SIZE * SIZE) {
            showAlert(Alert.AlertType.INFORMATION, "Sudoku completo", "O Sudoku está completo e válido.");
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Sudoku válido",
                    "Nenhum conflito foi encontrado. Ainda faltam " + (SIZE * SIZE - filled) + " células.");
        }
    }

    @FXML
    public void preencherExemplo(ActionEvent actionEvent) {
        aplicarTabuleiro(EXAMPLE, "sudoku-fixed");
        updateInformation("Fácil", "Fácil",
                "Exemplo clássico de Sudoku com nível de dificuldade fácil.");
    }

    @FXML
    public void sair(ActionEvent actionEvent) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(getWindow());
        confirmation.setTitle("Sair");
        confirmation.setHeaderText("Deseja fechar o programa?");
        confirmation.setContentText("Os valores não salvos serão perdidos.");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            ((Stage) getWindow()).close();
        }
    }

    @FXML
    public void proximo(ActionEvent actionEvent) {
        if (contarCelulasPreenchidas() == 0) {
            showAlert(Alert.AlertType.WARNING, "Tabuleiro vazio", "Preencha ou selecione um Sudoku antes de continuar.");
            return;
        }
        if (!validarTabuleiro()) {
            showAlert(Alert.AlertType.ERROR, "Sudoku inválido", "Corrija as células destacadas antes de continuar.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("ConfigurarAlgoritmo.fxml"));
            Parent root = loader.load();
            ConfigurarAlgoritmoController controller = loader.getController();
            controller.definirSudoku(lerTabuleiro());

            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            controller.definirCenaAnterior(stage.getScene());
            controller.definirEstadoAnteriorDaJanela(stage.isMaximized(), stage.isFullScreen());
            Scene nextScene = new Scene(root);
            nextScene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setFullScreen(false);
            stage.setScene(nextScene);
            stage.setMaximized(true);
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Erro de navegação",
                    "Não foi possível abrir a configuração: " + exception.getMessage());
        }
    }

    private int gridIndex(Integer index) {
        return index == null ? 0 : index;
    }

    private void applyBlockBorder(TextField cell, int i, int j) {
        boolean right = j == 2 || j == 5;
        boolean bottom = i == 2 || i == 5;
        if (right && bottom) {
            adicionarStateClasses(cell, "sudoku-right-bottom");
        } else if (right) {
            adicionarStateClasses(cell, "sudoku-right");
        } else if (bottom) {
            adicionarStateClasses(cell, "sudoku-bottom");
        }
    }

    private void marcarOsValoresExistentesFixos() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (cells[i][j] != null && !cells[i][j].getText().isEmpty()) {
                    adicionarStateClasses(cells[i][j], "sudoku-fixed");
                }
            }
        }
    }

    private void limparTabuleiro() {
        updatingBoard = true;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                cells[i][j].clear();
                removerStateClasses(cells[i][j]);
            }
        }
        updatingBoard = false;
         atualizarContagemDePreenchidas();
    }

    private void aplicarTabuleiro(int[][] board, String styleClass) {
        updatingBoard = true;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                TextField cell = cells[i][j];
                removerStateClasses(cell);
                int value = board[i][j];
                cell.setText(value == 0 ? "" : Integer.toString(value));
                if (value != 0) {
                    adicionarStateClasses(cell, styleClass);
                }
            }
        }
        updatingBoard = false;
         atualizarContagemDePreenchidas();
    }

    private int[][] lerTabuleiro() {
        int[][] board = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                String value = cells[i][j].getText().trim();
                board[i][j] = value.isEmpty() ? 0 : Integer.parseInt(value);
            }
        }
        return board;
    }

    private int[][] parseBoard(String content) {
        List<Integer> values = new ArrayList<Integer>();
        Matcher matcher = Pattern.compile("[0-9.]").matcher(content);
        while (matcher.find()) {
            String value = matcher.group();
            values.add(".".equals(value) ? 0 : Integer.parseInt(value));
        }
        if (values.size() != SIZE * SIZE) {
            throw new IllegalArgumentException(
                    "O arquivo deve conter exatamente 81 posições, usando 0 ou ponto nas células vazias.");
        }

        int[][] board = new int[SIZE][SIZE];
        for (int index = 0; index < values.size(); index++) {
            board[index / SIZE][index % SIZE] = values.get(index);
        }
        return board;
    }

    private boolean validarTabuleiro() {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                cells[i][j].getStyleClass().remove("sudoku-error");
            }
        }

        int[][] board = lerTabuleiro();
        boolean ok = true;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                int value = board[i][j];
                if (value == 0) { continue; }
                for (int c = j + 1; c < SIZE; c++) { // 
                    if (board[i][c] == value) {
                        marcarConflito(i, j, i, c);
                        ok = false;
                    }
                }
                for (int k = i + 1; k < SIZE; k++) {
                    if (board[k][j] == value) {
                        marcarConflito(i, j, k, j);
                        ok = false;
                    }
                }
                int startRow = i / 3 * 3;
                int startColumn = j / 3 * 3;
                for (int k = startRow; k < startRow + 3; k++) {
                    for (int c = startColumn; c < startColumn + 3; c++) {
                        if ((k > i || (k == i && c > j)) && board[k][c] == value) {
                            marcarConflito(i, j, k, c);
                            ok = false;
                        }
                    }
                }
            }
        }
        return ok;
    }

    private void marcarConflito(int rowA, int columnA, int rowB, int columnB) {
        adicionarStateClasses(cells[rowA][columnA], "sudoku-error");
        adicionarStateClasses(cells[rowB][columnB], "sudoku-error");
    }

    private int[][] createRandomPuzzle() {
        Random random = new Random();
        List<Integer> digits = new ArrayList<Integer>();
        for (int value = 1; value <= SIZE; value++) {
            digits.add(value);
        }
        Collections.shuffle(digits, random);
        List<Integer> rows = shuffledIndexes(random);
        List<Integer> columns = shuffledIndexes(random);

        int[][] puzzle = new int[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                int pattern = (rows.get(i) * 3 + rows.get(i) / 3 + columns.get(j)) % SIZE;
                puzzle[i][j] = digits.get(pattern);
            }
        }

        List<Integer> positions = new ArrayList<Integer>();
        for (int index = 0; index < SIZE * SIZE; index++) {
            positions.add(index);
        }
        Collections.shuffle(positions, random);
        for (int index = 0; index < 45; index++) {
            int position = positions.get(index);
            puzzle[position / SIZE][position % SIZE] = 0;
        }
        return puzzle;
    }

    private List<Integer> shuffledIndexes(Random random) {
        List<Integer> groups = new ArrayList<Integer>();
        groups.add(0);
        groups.add(1);
        groups.add(2);
        Collections.shuffle(groups, random);

        List<Integer> indexes = new ArrayList<Integer>();
        for (Integer group : groups) {
            List<Integer> members = new ArrayList<Integer>();
            members.add(0);
            members.add(1);
            members.add(2);
            Collections.shuffle(members, random);
            for (Integer member : members) {
                indexes.add(group * 3 + member);
            }
        }
        return indexes;
    }

    private int contarCelulasPreenchidas() {
        int count = 0;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (!cells[i][j].getText().isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    private void  atualizarContagemDePreenchidas() {
        if (lblPreenchidas != null) {
            lblPreenchidas.setText(contarCelulasPreenchidas() + " / " + (SIZE * SIZE));
        }
    }

    private void updateInformation(String name, String difficulty, String description) {
        lblNome.setText(name);
        lblDificuldade.setText(difficulty);
        lblDescricao.setText(description);
         atualizarContagemDePreenchidas();
    }

    private void removerStateClasses(TextField cell) {
        cell.getStyleClass().removeAll(
                "sudoku-fixed", "sudoku-current", "sudoku-generated", "sudoku-error");
    }

    private void adicionarStateClasses(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    private Window getWindow() {
        return gridSudoku.getScene().getWindow();
    }

    private void showAlert(Alert.AlertType tipo, String titulo, String mensagem) {
        Alert alert = new Alert(tipo);
        alert.initOwner(getWindow());
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}
