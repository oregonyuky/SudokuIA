package controller;

import com.example.sudokuia.HelloApplication;
import com.example.sudokuia.model.Matriz;
import com.example.sudokuia.solver.Dfs;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ResolverSudokuController {
    private static final int SIZE = 9;
    @FXML private GridPane gridSudoku;
    @FXML private Label lblAlgoritmo, lblEstado, lblProfundidade, lblTempo, lblNos,
            lblPassos, lblBacktracks, lblVelocidade, lblLinhaAtual, lblColunaAtual,
            lblValoresValidos, lblTentando, lblAcao, lblNosArvore;
    @FXML private Button btnIniciar, btnPausar, btnPasso, btnParar, btnResultados;
    @FXML private Slider sliderVelocidade;
    @FXML private ListView<String> listPilha;
    @FXML private TextArea txtLog;
    @FXML private ScrollPane scrollArvore;
    @FXML private Pane paneArvore;

    private final Label[][] cells = new Label[SIZE][SIZE];
    private final Set<Integer> fixed = new HashSet<Integer>();
    private final Object pauseLock = new Object();
    private int[][] initialBoard, finalBoard;
    private String algorithm = "DFS com Backtracking";
    private volatile double speed = 1;
    private volatile boolean paused, oneStep, stopping;
    private Task<int[][]> task;
    private Dfs solver;
    private long startedAt, elapsedNanos;
    private int treeNodes;

    @FXML private void initialize() {
        for (int row = 0; row < SIZE; row++)
            for (int column = 0; column < SIZE; column++) {
            Label cell = new Label();
            cell.setAlignment(Pos.CENTER);
            cell.setMinSize(40, 40); cell.setPrefSize(40, 40); cell.setMaxSize(40, 40);
            cells[row][column] = cell;
            gridSudoku.add(cell, column, row);
        }
        render(new int[SIZE][SIZE]);
        sliderVelocidade.valueProperty().addListener((o, oldValue, newValue) -> {
            speed = newValue.doubleValue();
            lblVelocidade.setText(String.format("%.1fx", speed));
        });
    }

    public void setConfiguration(int[][] sudoku, String algorithm, double initialSpeed) {
        initialBoard = copy(sudoku);
        this.algorithm = algorithm == null ? this.algorithm : algorithm;
        lblAlgoritmo.setText(this.algorithm);
        sliderVelocidade.setValue(initialSpeed);
        fixed.clear();
        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++)
            if (sudoku[r][c] != 0) fixed.add(r * SIZE + c);
        render(initialBoard);
        log("Sudoku recebido. Clique em Iniciar para executar o DFS.");
    }

    @FXML public void iniciar(ActionEvent event) {
        if (task != null && task.isRunning()) return;
        if (initialBoard == null) { alert("Sudoku ausente", "Volte e selecione um Sudoku."); return; }
        reset();
        Matriz matriz = new Matriz();
        matriz.setMatriz(copy(initialBoard));
        solver = new Dfs(matriz);
        solver.setStepListener(this::receiveStep);
        startedAt = System.nanoTime();
        task = new Task<int[][]>() { @Override protected int[][] call() { return solver.dfs(); } };
        task.setOnSucceeded(e -> finish(task.getValue()));
        task.setOnCancelled(e -> stopped());
        task.setOnFailed(e -> failed(task.getException()));
        controls(true); lblEstado.setText("Executando"); log("DFS iniciado.");
        Thread thread = new Thread(task, "sudoku-dfs"); thread.setDaemon(true); thread.start();
    }

    @FXML public void pausar(ActionEvent event) {
        if (task == null || !task.isRunning()) return;
        synchronized (pauseLock) { paused = !paused; if (!paused) pauseLock.notifyAll(); }
        lblEstado.setText(paused ? "Pausado" : "Executando");
        btnPausar.setText(paused ? "Retomar" : "Pausar"); btnPasso.setDisable(!paused);
    }

    @FXML public void proximoPasso(ActionEvent event) {
        if (task == null || !task.isRunning()) return;
        synchronized (pauseLock) { paused = true; oneStep = true; pauseLock.notifyAll(); }
        lblEstado.setText("Passo a passo"); btnPausar.setText("Retomar"); btnPasso.setDisable(false);
    }

    @FXML public void parar(ActionEvent event) { stopSolver(); }
    @FXML public void centralizarArvore(ActionEvent event) { scrollArvore.setHvalue(.5); scrollArvore.setVvalue(.5); }
    @FXML public void limparArvore(ActionEvent event) {
        paneArvore.getChildren().clear(); treeNodes = 0; lblNosArvore.setText("Nós exibidos: 0");
    }
    @FXML public void sair(ActionEvent event) {
        stopSolver(); ((Stage)((Node)event.getSource()).getScene().getWindow()).close();
    }
    @FXML public void voltar(ActionEvent event) {
        stopSolver();
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("ConfigurarAlgoritmo.fxml"));
            Parent root = loader.load();
            ConfigurarAlgoritmoController controller = loader.getController();
            if (initialBoard != null) controller.setSudoku(initialBoard);
            Stage stage = (Stage)((Node)event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root); scene.getStylesheets().addAll(stage.getScene().getStylesheets());
            stage.setScene(scene); stage.setMaximized(true); stage.setFullScreen(true);
        } catch (IOException e) { alert("Erro de navegação", e.getMessage()); }
    }
    @FXML public void resultados(ActionEvent event) {
        if (solver != null && solver.isSolved()) alert("Resultado do DFS",
                "Solução encontrada em " + formatTime(elapsedNanos) + "\nNós explorados: "
                        + solver.getExploredNodes() + "\nPassos: " + solver.getSteps()
                        + "\nBacktracks: " + solver.getBacktracks()
                        + "\nProfundidade máxima: " + solver.getMaximumDepth());
    }

    private void receiveStep(Dfs.Step step) throws InterruptedException {
        synchronized (pauseLock) {
            while (paused && !oneStep && !stopping) pauseLock.wait();
            if (stopping || Thread.currentThread().isInterrupted()) throw new InterruptedException();
            if (oneStep) oneStep = false;
        }
        Platform.runLater(() -> applyStep(step));
        if (step.getType() != Dfs.Step.Type.SOLVED) Thread.sleep(Math.max(10, (long)(100 / speed)));
    }

    private void applyStep(Dfs.Step step) {
        render(step.getBoard());
        lblProfundidade.setText("" + step.getDepth()); lblNos.setText("" + step.getExploredNodes());
        lblPassos.setText("" + step.getSteps()); lblBacktracks.setText("" + step.getBacktracks());
        lblTempo.setText(formatTime(System.nanoTime() - startedAt));
        lblLinhaAtual.setText(step.getRow() < 0 ? "-" : "" + (step.getRow() + 1));
        lblColunaAtual.setText(step.getColumn() < 0 ? "-" : "" + (step.getColumn() + 1));
        lblValoresValidos.setText(step.getValidValues().toString());
        lblTentando.setText(step.getValue() == 0 ? "-" : "" + step.getValue());
        lblAcao.setText(action(step));
        listPilha.getItems().clear();
        for (int i = step.getStackSize(); i >= Math.max(1, step.getStackSize() - 14); i--)
            listPilha.getItems().add("Estado " + i);
        if (step.getType() == Dfs.Step.Type.PLACE || step.getType() == Dfs.Step.Type.BACKTRACK) {
            addTreeNode(step); log(step.getType() == Dfs.Step.Type.BACKTRACK ? "Backtrack." :
                    "Inserido " + step.getValue() + " em L" + (step.getRow()+1) + "C" + (step.getColumn()+1) + ".");
        }
    }

    private void finish(int[][] result) {
        elapsedNanos = System.nanoTime() - startedAt; finalBoard = copy(result); render(finalBoard); controls(false);
        lblTempo.setText(formatTime(elapsedNanos)); lblEstado.setText(solver.isSolved() ? "Concluído" : "Sem solução");
        lblAcao.setText(solver.isSolved() ? "Solução encontrada" : "Busca encerrada sem solução");
        btnResultados.setDisable(!solver.isSolved()); btnIniciar.setText("Reiniciar");
        log(solver.isSolved() ? "Solução encontrada pelo DFS." : "Não foi encontrada uma solução.");
    }
    private void stopped() { controls(false); lblEstado.setText("Interrompido"); log("Execução interrompida."); }
    private void failed(Throwable error) { controls(false); lblEstado.setText("Erro"); alert("Erro na execução", error.getMessage()); }

    private void render(int[][] board) {
        for (int r = 0; r < SIZE; r++) for (int c = 0; c < SIZE; c++) {
            Label cell = cells[r][c]; int value = board[r][c]; cell.setText(value == 0 ? "" : "" + value);
            String right = c == 2 || c == 5 ? "2" : "1", bottom = r == 2 || r == 5 ? "2" : "1";
            boolean isFixed = fixed.contains(r * SIZE + c);
            cell.setStyle("-fx-border-color:#9ca3af;-fx-border-width:1 " + right + " " + bottom
                    + " 1;-fx-background-color:" + (isFixed ? "#e5e7eb" : "white")
                    + ";-fx-font-weight:" + (isFixed ? "bold" : "normal") + ";");
        }
    }
    private void addTreeNode(Dfs.Step step) {
        if (treeNodes >= 100) return;
        Label node = new Label(step.getType() == Dfs.Step.Type.BACKTRACK ? "↩" : "" + step.getValue());
        node.setAlignment(Pos.CENTER); node.setPrefSize(30,30);
        node.setStyle("-fx-background-color:" + (step.getType() == Dfs.Step.Type.BACKTRACK ? "#fecaca" : "#bfdbfe")
                + ";-fx-background-radius:15;-fx-border-radius:15;-fx-border-color:#64748b;");
        node.relocate(10 + (treeNodes % 16) * 36, 12 + (treeNodes / 16) * 45);
        paneArvore.getChildren().add(node); lblNosArvore.setText("Nós exibidos: " + (++treeNodes));
    }
    private void reset() {
        stopping = paused = oneStep = false; finalBoard = null; txtLog.clear(); listPilha.getItems().clear();
        limparArvore(null); render(initialBoard); lblNos.setText("0"); lblPassos.setText("0");
        lblBacktracks.setText("0"); lblProfundidade.setText("0"); lblTempo.setText("00:00:00.000");
        btnResultados.setDisable(true);
    }
    private void controls(boolean running) {
        btnIniciar.setDisable(running); btnPausar.setDisable(!running); btnPasso.setDisable(true);
        btnParar.setDisable(!running); if (!running) btnPausar.setText("Pausar");
    }
    private void stopSolver() {
        if (task != null && task.isRunning()) {
            stopping = true; solver.cancel(); synchronized (pauseLock) { pauseLock.notifyAll(); } task.cancel(true);
        }
    }
    private String action(Dfs.Step step) {
        switch (step.getType()) {
            case TRY: return "Procurando valor válido"; case PLACE: return "Valor inserido";
            case BACKTRACK: return "Retornando ao estado anterior"; case SOLVED: return "Solução encontrada";
            default: return "Busca encerrada sem solução";
        }
    }
    private void log(String text) { txtLog.appendText(text + System.lineSeparator()); }
    private int[][] copy(int[][] source) {
        int[][] target = new int[SIZE][SIZE]; for (int r=0;r<SIZE;r++) System.arraycopy(source[r],0,target[r],0,SIZE); return target;
    }
    private String formatTime(long nanos) {
        long ms=nanos/1_000_000, h=ms/3_600_000, m=(ms/60_000)%60, s=(ms/1_000)%60;
        return String.format("%02d:%02d:%02d.%03d",h,m,s,ms%1_000);
    }
    private void alert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(null);
        alert.setContentText(message == null ? "Falha desconhecida." : message); alert.showAndWait();
    }
}
