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
            int column = gridIndex(GridPane.getColumnIndex(cell));
            int row = gridIndex(GridPane.getRowIndex(cell));
            cells[row][column] = cell;
            applyBlockBorder(cell, row, column);

            cell.setTextFormatter(new TextFormatter<String>(change ->
                    change.getControlNewText().matches("[1-9]?") ? change : null));
            cell.textProperty().addListener((observable, oldValue, newValue) -> {
                if (!updatingBoard) {
                    removeStateClasses(cell);
                    if (!newValue.isEmpty()) {
                        addStyleClass(cell, "sudoku-current");
                    }
                }
                updateFilledCount();
            });
        }

        markExistingValuesAsFixed();
        updateFilledCount();
    }

    @FXML
    public void inserirManual(ActionEvent actionEvent) {
        clearBoard();
        updateInformation("Manual", "Não definida",
                "Digite os valores conhecidos diretamente no tabuleiro.");
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
            applyBoard(board, "sudoku-fixed");
            updateInformation(file.getName(), "Importado",
                    "Sudoku carregado do arquivo " + file.getName() + ".");
            if (!validateBoard()) {
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
        applyBoard(createRandomPuzzle(), "sudoku-fixed");
        updateInformation("Sudoku aleatório", "Médio",
                "Sudoku válido gerado automaticamente com 36 células preenchidas.");
    }

    @FXML
    public void limparTabuleiro(ActionEvent actionEvent) {
        clearBoard();
        updateInformation("Manual", "Não definida", "Tabuleiro vazio.");
    }

    @FXML
    public void verificarTabuleiro(ActionEvent actionEvent) {
        boolean valid = validateBoard();
        int filled = countFilledCells();
        if (!valid) {
            showAlert(Alert.AlertType.ERROR, "Sudoku inválido",
                    "Existem números repetidos em uma linha, coluna ou bloco 3x3.");
        } else if (filled == SIZE * SIZE) {
            showAlert(Alert.AlertType.INFORMATION, "Sudoku completo", "O Sudoku está completo e válido.");
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Sudoku válido",
                    "Nenhum conflito foi encontrado. Ainda faltam " + (SIZE * SIZE - filled) + " células.");
        }
    }

    @FXML
    public void preencherExemplo(ActionEvent actionEvent) {
        applyBoard(EXAMPLE, "sudoku-fixed");
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
        if (countFilledCells() == 0) {
            showAlert(Alert.AlertType.WARNING, "Tabuleiro vazio",
                    "Preencha ou selecione um Sudoku antes de continuar.");
            return;
        }
        if (!validateBoard()) {
            showAlert(Alert.AlertType.ERROR, "Sudoku inválido",
                    "Corrija as células destacadas antes de continuar.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("ConfigurarAlgoritmo.fxml"));
            Parent root = loader.load();
            ConfigurarAlgoritmoController controller = loader.getController();
            controller.setSudoku(readBoard());

            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            Scene nextScene = new Scene(root);
            nextScene.getStylesheets().addAll(stage.getScene().getStylesheets());
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

    private void applyBlockBorder(TextField cell, int row, int column) {
        boolean right = column == 2 || column == 5;
        boolean bottom = row == 2 || row == 5;
        if (right && bottom) {
            addStyleClass(cell, "sudoku-right-bottom");
        } else if (right) {
            addStyleClass(cell, "sudoku-right");
        } else if (bottom) {
            addStyleClass(cell, "sudoku-bottom");
        }
    }

    private void markExistingValuesAsFixed() {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (cells[row][column] != null && !cells[row][column].getText().isEmpty()) {
                    addStyleClass(cells[row][column], "sudoku-fixed");
                }
            }
        }
    }

    private void clearBoard() {
        updatingBoard = true;
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                cells[row][column].clear();
                removeStateClasses(cells[row][column]);
            }
        }
        updatingBoard = false;
        updateFilledCount();
    }

    private void applyBoard(int[][] board, String styleClass) {
        updatingBoard = true;
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                TextField cell = cells[row][column];
                removeStateClasses(cell);
                int value = board[row][column];
                cell.setText(value == 0 ? "" : Integer.toString(value));
                if (value != 0) {
                    addStyleClass(cell, styleClass);
                }
            }
        }
        updatingBoard = false;
        updateFilledCount();
    }

    private int[][] readBoard() {
        int[][] board = new int[SIZE][SIZE];
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                String value = cells[row][column].getText().trim();
                board[row][column] = value.isEmpty() ? 0 : Integer.parseInt(value);
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

    private boolean validateBoard() {
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                cells[row][column].getStyleClass().remove("sudoku-error");
            }
        }

        int[][] board = readBoard();
        boolean valid = true;
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                int value = board[row][column];
                if (value == 0) {
                    continue;
                }
                for (int otherColumn = column + 1; otherColumn < SIZE; otherColumn++) {
                    if (board[row][otherColumn] == value) {
                        markConflict(row, column, row, otherColumn);
                        valid = false;
                    }
                }
                for (int otherRow = row + 1; otherRow < SIZE; otherRow++) {
                    if (board[otherRow][column] == value) {
                        markConflict(row, column, otherRow, column);
                        valid = false;
                    }
                }
                int startRow = row / 3 * 3;
                int startColumn = column / 3 * 3;
                for (int otherRow = startRow; otherRow < startRow + 3; otherRow++) {
                    for (int otherColumn = startColumn; otherColumn < startColumn + 3; otherColumn++) {
                        if ((otherRow > row || (otherRow == row && otherColumn > column))
                                && board[otherRow][otherColumn] == value) {
                            markConflict(row, column, otherRow, otherColumn);
                            valid = false;
                        }
                    }
                }
            }
        }
        return valid;
    }

    private void markConflict(int rowA, int columnA, int rowB, int columnB) {
        addStyleClass(cells[rowA][columnA], "sudoku-error");
        addStyleClass(cells[rowB][columnB], "sudoku-error");
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
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                int pattern = (rows.get(row) * 3 + rows.get(row) / 3 + columns.get(column)) % SIZE;
                puzzle[row][column] = digits.get(pattern);
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

    private int countFilledCells() {
        int count = 0;
        for (int row = 0; row < SIZE; row++) {
            for (int column = 0; column < SIZE; column++) {
                if (!cells[row][column].getText().isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    private void updateFilledCount() {
        if (lblPreenchidas != null) {
            lblPreenchidas.setText(countFilledCells() + " / " + (SIZE * SIZE));
        }
    }

    private void updateInformation(String name, String difficulty, String description) {
        lblNome.setText(name);
        lblDificuldade.setText(difficulty);
        lblDescricao.setText(description);
        updateFilledCount();
    }

    private void removeStateClasses(TextField cell) {
        cell.getStyleClass().removeAll(
                "sudoku-fixed", "sudoku-current", "sudoku-generated", "sudoku-error");
    }

    private void addStyleClass(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    private Window getWindow() {
        return gridSudoku.getScene().getWindow();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(getWindow());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
