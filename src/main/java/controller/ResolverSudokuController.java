package controller;

import com.example.sudokuia.HelloApplication;
import com.example.sudokuia.model.Matriz;
import com.example.sudokuia.solver.AEstrela;
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
    private static final int TAMANHO = 9;

    @FXML private GridPane gridSudoku;
    @FXML private Label lblAlgoritmo, lblEstado, lblProfundidade, lblTempo, lblNos,
            lblPassos, lblBacktracks, lblVelocidade, lblLinhaAtual, lblColunaAtual,
            lblValoresValidos, lblTentando, lblAcao, lblNosArvore;
    @FXML private Button btnIniciar, btnConstruir, btnPausar, btnPasso, btnParar, btnResultados;
    @FXML private Slider sliderVelocidade;
    @FXML private ListView<String> listPilha;
    @FXML private TextArea txtLog;
    @FXML private ScrollPane scrollArvore;
    @FXML private Pane paneArvore;

    private final Label[][] celulas = new Label[TAMANHO][TAMANHO];
    private final Set<Integer> posicoesFixas = new HashSet<Integer>();
    private final Object controlePausa = new Object();
    private int[][] tabuleiroInicial, tabuleiroFinal;
    private String algoritmo = "DFS com Backtracking";
    private volatile double velocidade = 1;
    private volatile boolean pausado, passoUnico, interrompendo;
    private boolean execucaoAnimada;
    private Task<int[][]> tarefa;
    private Dfs resolvedorDfs;
    private AEstrela resolvedorMrv;
    private long inicio, tempoDecorrido;
    private int nosArvore;

    @FXML
    private void initialize() {
        for (int linha = 0; linha < TAMANHO; linha++) {
            for (int coluna = 0; coluna < TAMANHO; coluna++) {
                Label celula = new Label();
                celula.setAlignment(Pos.CENTER);
                celula.setMinSize(40, 40);
                celula.setPrefSize(40, 40);
                celula.setMaxSize(40, 40);
                celulas[linha][coluna] = celula;
                gridSudoku.add(celula, coluna, linha);
            }
        }
        renderizar(new int[TAMANHO][TAMANHO]);
        sliderVelocidade.valueProperty().addListener((observavel, anterior, atual) -> {
            velocidade = atual.doubleValue();
            lblVelocidade.setText(String.format("%.1fx", velocidade));
        });
    }

    public void definirConfiguracao(int[][] sudoku, String algoritmo, double velocidadeInicial) {
        tabuleiroInicial = copiarMatriz(sudoku);
        if (algoritmo != null) {
            this.algoritmo = algoritmo;
        }
        lblAlgoritmo.setText(this.algoritmo);
        sliderVelocidade.setValue(velocidadeInicial);
        posicoesFixas.clear();
        for (int linha = 0; linha < TAMANHO; linha++) {
            for (int coluna = 0; coluna < TAMANHO; coluna++) {
                if (sudoku[linha][coluna] != 0) {
                    posicoesFixas.add(linha * TAMANHO + coluna);
                }
            }
        }
        renderizar(tabuleiroInicial);
        String nome = this.algoritmo.contains("MRV") ? "MRV" : "DFS";
        registrarLog("Sudoku recebido. Clique em Iniciar para acompanhar o " + nome
                + " ou em Construir Sudoku para preencher automaticamente.");
    }

    @FXML public void iniciar(ActionEvent evento) {
        iniciarResolucao(true);
    }

    @FXML public void construirSudoku(ActionEvent evento) {
        iniciarResolucao(false);
    }

    private void iniciarResolucao(boolean animada) {
        if (tarefa != null && tarefa.isRunning()) {
            return;
        }
        if (tabuleiroInicial == null) {
            exibirAlerta("Sudoku ausente", "Volte e selecione um Sudoku.");
            return;
        }
        reiniciar();
        execucaoAnimada = animada;
        Matriz matriz = new Matriz();
        matriz.setMatriz(copiarMatriz(tabuleiroInicial));
        boolean usarMrv = algoritmo != null && algoritmo.contains("MRV");

        if (usarMrv) {
            resolvedorDfs = null;
            resolvedorMrv = new AEstrela(matriz);
            if (animada) {
                resolvedorMrv.setStepListener(this::receberPassoMrv);
            }
            inicio = System.nanoTime();
            tarefa = new Task<int[][]>() {
                @Override
                protected int[][] call() {
                    return resolvedorMrv.dfs();
                }
            };
        } else {
            resolvedorMrv = null;
            resolvedorDfs = new Dfs(matriz);
            if (animada) {
                resolvedorDfs.setStepListener(this::receberPassoDfs);
            }
            inicio = System.nanoTime();
            tarefa = new Task<int[][]>() {
                @Override
                protected int[][] call() {
                    return resolvedorDfs.dfs();
                }
            };
        }

        atualizarControles(true);
        lblEstado.setText(animada ? "Executando" : "Construindo");
        registrarLog(animada ? (usarMrv ? "MRV iniciado." : "DFS iniciado.")
                : "Construindo o Sudoku automaticamente.");
        tarefa.setOnSucceeded(evento -> finalizar(tarefa.getValue()));
        tarefa.setOnCancelled(evento -> registrarInterrupcao());
        tarefa.setOnFailed(evento -> tratarFalha(tarefa.getException()));
        Thread thread = new Thread(tarefa, "sudoku-solver");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML public void pausar(ActionEvent evento) {
        if (tarefa == null || !tarefa.isRunning()) {
            return;
        }
        synchronized (controlePausa) {
            pausado = !pausado;
            if (!pausado) {
                controlePausa.notifyAll();
            }
        }
        lblEstado.setText(pausado ? "Pausado" : "Executando");
        btnPausar.setText(pausado ? "Retomar" : "Pausar");
        btnPasso.setDisable(!pausado);
    }

    @FXML public void proximoPasso(ActionEvent evento) {
        if (tarefa == null || !tarefa.isRunning()) {
            return;
        }
        synchronized (controlePausa) {
            pausado = true;
            passoUnico = true;
            controlePausa.notifyAll();
        }
        lblEstado.setText("Passo a passo");
        btnPausar.setText("Retomar");
        btnPasso.setDisable(false);
    }

    @FXML public void parar(ActionEvent evento) {
        pararResolucao();
    }

    @FXML public void centralizarArvore(ActionEvent evento) {
        scrollArvore.setHvalue(.5);
        scrollArvore.setVvalue(.5);
    }

    @FXML public void limparArvore(ActionEvent evento) {
        paneArvore.getChildren().clear();
        nosArvore = 0;
        lblNosArvore.setText("Nós exibidos: 0");
    }

    @FXML public void sair(ActionEvent evento) {
        pararResolucao();
        Stage janela = (Stage) ((Node) evento.getSource()).getScene().getWindow();
        janela.close();
    }

    @FXML public void voltar(ActionEvent evento) {
        pararResolucao();
        try {
            FXMLLoader carregador = new FXMLLoader(
                    HelloApplication.class.getResource("ConfigurarAlgoritmo.fxml"));
            Parent raiz = carregador.load();
            ConfigurarAlgoritmoController controller = carregador.getController();
            if (tabuleiroInicial != null) {
                controller.definirSudoku(tabuleiroInicial);
            }
            Stage janela = (Stage) ((Node) evento.getSource()).getScene().getWindow();
            Scene cena = new Scene(raiz);
            cena.getStylesheets().addAll(janela.getScene().getStylesheets());
            janela.setFullScreen(false);
            janela.setScene(cena);
            janela.setMaximized(true);
        } catch (IOException erro) {
            exibirAlerta("Erro de navegação", erro.getMessage());
        }
    }

    @FXML public void resultados(ActionEvent evento) {
        if (resolvedorMrv != null && resolvedorMrv.isSolved()) {
            exibirResultados("MRV", resolvedorMrv.getNosExplorados(), resolvedorMrv.getPassos(),
                    resolvedorMrv.getBacktracks(), resolvedorMrv.getProfundidadeMaxima());
        } else if (resolvedorDfs != null && resolvedorDfs.isSolved()) {
            exibirResultados("DFS", resolvedorDfs.getExploredNodes(), resolvedorDfs.getSteps(),
                    resolvedorDfs.getBacktracks(), resolvedorDfs.getMaximumDepth());
        }
    }

    private void exibirResultados(String nome, long nos, long passos, long retornos, int profundidade) {
        exibirAlerta("Resultado do " + nome,
                "Solução encontrada em " + formatarTempo(tempoDecorrido)
                        + "\nNós explorados: " + nos + "\nPassos: " + passos
                        + "\nBacktracks: " + retornos + "\nProfundidade máxima: " + profundidade);
    }

    private void aguardarPermissao() throws InterruptedException {
        synchronized (controlePausa) {
            while (pausado && !passoUnico && !interrompendo) {
                controlePausa.wait();
            }
            if (interrompendo || Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            if (passoUnico) {
                passoUnico = false;
            }
        }
    }

    private void receberPassoDfs(Dfs.Step etapa) throws InterruptedException {
        aguardarPermissao();
        Platform.runLater(() -> aplicarPassoDfs(etapa));
        if (etapa.getType() != Dfs.Step.Type.SOLVED) {
            Thread.sleep(Math.max(10, (long) (100 / velocidade)));
        }
    }

    private void receberPassoMrv(AEstrela.Step etapa) throws InterruptedException {
        aguardarPermissao();
        Platform.runLater(() -> aplicarPassoMrv(etapa));
        if (etapa.getType() != AEstrela.Step.Type.SOLVED) {
            Thread.sleep(Math.max(10, (long) (100 / velocidade)));
        }
    }

    private void aplicarPassoDfs(Dfs.Step etapa) {
        renderizar(etapa.getBoard());
        atualizarEstatisticas(etapa.getDepth(), etapa.getExploredNodes(),
                etapa.getSteps(), etapa.getBacktracks());
        atualizarCelulaAtual(etapa.getRow(), etapa.getColumn(), etapa.getValue(),
                etapa.getValidValues().toString());
        lblAcao.setText(descreverAcao(etapa.getType().name()));
        atualizarPilha(etapa.getStackSize());
        if (etapa.getType() == Dfs.Step.Type.PLACE || etapa.getType() == Dfs.Step.Type.BACKTRACK) {
            boolean retorno = etapa.getType() == Dfs.Step.Type.BACKTRACK;
            adicionarNoArvore(etapa.getValue(), retorno);
            registrarTentativa(etapa.getRow(), etapa.getColumn(), etapa.getValue(), retorno);
        }
    }

    private void aplicarPassoMrv(AEstrela.Step etapa) {
        renderizar(etapa.getTabuleiro());
        atualizarEstatisticas(etapa.getProfundidade(), etapa.getNosExplorados(),
                etapa.getPassos(), etapa.getBacktracks());
        atualizarCelulaAtual(etapa.getLinha(), etapa.getColuna(), etapa.getValor(),
                etapa.getValoresValidos().toString());
        lblAcao.setText(descreverAcao(etapa.getTipo().name()));
        atualizarPilha(etapa.getTamanhoPilha());
        if (etapa.getTipo() == AEstrela.Step.Tipo.PLACE || etapa.getTipo() == AEstrela.Step.Tipo.BACKTRACK) {
            boolean retorno = etapa.getTipo() == AEstrela.Step.Tipo.BACKTRACK;
            adicionarNoArvore(etapa.getValor(), retorno);
            registrarTentativa(etapa.getLinha(), etapa.getColuna(), etapa.getValor(), retorno);
        }
    }

    private void atualizarEstatisticas(int profundidade, long nos, long passos, long retornos) {
        lblProfundidade.setText("" + profundidade);
        lblNos.setText("" + nos);
        lblPassos.setText("" + passos);
        lblBacktracks.setText("" + retornos);
        lblTempo.setText(formatarTempo(System.nanoTime() - inicio));
    }

    private void atualizarCelulaAtual(int linha, int coluna, int valor, String valoresValidos) {
        lblLinhaAtual.setText(linha < 0 ? "-" : "" + (linha + 1));
        lblColunaAtual.setText(coluna < 0 ? "-" : "" + (coluna + 1));
        lblValoresValidos.setText(valoresValidos);
        lblTentando.setText(valor == 0 ? "-" : "" + valor);
    }

    private void atualizarPilha(int tamanho) {
        listPilha.getItems().clear();
        for (int i = tamanho; i >= Math.max(1, tamanho - 14); i--) {
            listPilha.getItems().add("Estado " + i);
        }
    }

    private void registrarTentativa(int linha, int coluna, int valor, boolean retorno) {
        registrarLog(retorno ? "Backtrack."
                : "Inserido " + valor + " em L" + (linha + 1) + "C" + (coluna + 1) + ".");
    }

    private void finalizar(int[][] resultado) {
        tempoDecorrido = System.nanoTime() - inicio;
        tabuleiroFinal = copiarMatriz(resultado);
        renderizar(tabuleiroFinal);
        atualizarControles(false);
        boolean heuristico = resolvedorMrv != null;
        boolean resolvido;
        if (heuristico) {
            resolvido = resolvedorMrv.estaResolvido();
            atualizarEstatisticas(resolvedorMrv.getProfundidadeMaxima(),
                    resolvedorMrv.getNosExplorados(), resolvedorMrv.getPassos(),
                    resolvedorMrv.getBacktracks());
        } else {
            resolvido = resolvedorDfs.isSolved();
            atualizarEstatisticas(resolvedorDfs.getMaximumDepth(), resolvedorDfs.getExploredNodes(),
                    resolvedorDfs.getSteps(), resolvedorDfs.getBacktracks());
        }
        lblTempo.setText(formatarTempo(tempoDecorrido));
        lblEstado.setText(resolvido ? "Concluído" : "Sem solução");
        lblAcao.setText(resolvido ? "Solução encontrada" : "Busca encerrada sem solução");
        btnResultados.setDisable(!resolvido);
        btnIniciar.setText("Reiniciar");
        registrarLog(resolvido ? "Solução encontrada pelo " + (heuristico ? "MRV." : "DFS.")
                : "Não foi encontrada uma solução.");
    }

    private void registrarInterrupcao() {
        atualizarControles(false);
        lblEstado.setText("Interrompido");
        registrarLog("Execução interrompida.");
    }

    private void tratarFalha(Throwable erro) {
        atualizarControles(false);
        lblEstado.setText("Erro");
        exibirAlerta("Erro na execução", erro.getMessage());
    }

    private void renderizar(int[][] tabuleiro) {
        for (int linha = 0; linha < TAMANHO; linha++) {
            for (int coluna = 0; coluna < TAMANHO; coluna++) {
                Label celula = celulas[linha][coluna];
                int valor = tabuleiro[linha][coluna];
                celula.setText(valor == 0 ? "" : "" + valor);
                String bordaDireita = coluna == 2 || coluna == 5 ? "2" : "1";
                String bordaInferior = linha == 2 || linha == 5 ? "2" : "1";
                boolean fixa = posicoesFixas.contains(linha * TAMANHO + coluna);
                celula.setStyle("-fx-border-color:#9ca3af;-fx-border-width:1 "
                        + bordaDireita + " " + bordaInferior + " 1;-fx-background-color:"
                        + (fixa ? "#e5e7eb" : "white") + ";-fx-font-weight:"
                        + (fixa ? "bold" : "normal") + ";");
            }
        }
    }

    private void adicionarNoArvore(int valor, boolean retorno) {
        if (nosArvore >= 100) {
            return;
        }
        Label no = new Label(retorno ? "↩" : "" + valor);
        no.setAlignment(Pos.CENTER);
        no.setPrefSize(30, 30);
        no.setStyle("-fx-background-color:" + (retorno ? "#fecaca" : "#bfdbfe")
                + ";-fx-background-radius:15;-fx-border-radius:15;-fx-border-color:#64748b;");
        no.relocate(10 + (nosArvore % 16) * 36, 12 + (nosArvore / 16) * 45);
        paneArvore.getChildren().add(no);
        lblNosArvore.setText("Nós exibidos: " + (++nosArvore));
    }

    private void reiniciar() {
        interrompendo = pausado = passoUnico = false;
        tabuleiroFinal = null;
        txtLog.clear();
        listPilha.getItems().clear();
        limparArvore(null);
        renderizar(tabuleiroInicial);
        lblNos.setText("0");
        lblPassos.setText("0");
        lblBacktracks.setText("0");
        lblProfundidade.setText("0");
        lblTempo.setText("00:00:00.000");
        btnResultados.setDisable(true);
    }

    private void atualizarControles(boolean executando) {
        btnIniciar.setDisable(executando);
        btnConstruir.setDisable(executando);
        btnPausar.setDisable(!executando || !execucaoAnimada);
        btnPasso.setDisable(true);
        btnParar.setDisable(!executando);
        if (!executando) {
            btnPausar.setText("Pausar");
        }
    }

    private void pararResolucao() {
        if (tarefa != null && tarefa.isRunning()) {
            interrompendo = true;
            if (resolvedorDfs != null) {
                resolvedorDfs.cancel();
            }
            if (resolvedorMrv != null) {
                resolvedorMrv.cancelar();
            }
            synchronized (controlePausa) {
                controlePausa.notifyAll();
            }
            tarefa.cancel(true);
        }
    }

    private String descreverAcao(String tipo) {
        switch (tipo) {
            case "TRY": return "Procurando valor válido";
            case "PLACE": return "Valor inserido";
            case "BACKTRACK": return "Retornando ao estado anterior";
            case "SOLVED": return "Solução encontrada";
            default: return "Busca encerrada sem solução";
        }
    }

    private void registrarLog(String texto) {
        txtLog.appendText(texto + System.lineSeparator());
    }

    private int[][] copiarMatriz(int[][] origem) {
        int[][] copia = new int[TAMANHO][TAMANHO];
        for (int linha = 0; linha < TAMANHO; linha++) {
            System.arraycopy(origem[linha], 0, copia[linha], 0, TAMANHO);
        }
        return copia;
    }

    private String formatarTempo(long nanos) {
        long milissegundos = nanos / 1_000_000;
        long horas = milissegundos / 3_600_000;
        long minutos = (milissegundos / 60_000) % 60;
        long segundos = (milissegundos / 1_000) % 60;
        return String.format("%02d:%02d:%02d.%03d",
                horas, minutos, segundos, milissegundos % 1_000);
    }

    private void exibirAlerta(String titulo, String mensagem) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensagem == null ? "Falha desconhecida." : mensagem);
        alerta.showAndWait();
    }
}
