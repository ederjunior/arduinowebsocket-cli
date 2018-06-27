/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ifms.cx.iot.app;

import br.ifms.cx.iot.gui.ExemploStreamCamera;
import br.ifms.cx.iot.util.RXTXUtil;
import com.google.gson.Gson;
import gnu.io.CommPortIdentifier;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_IMWRITE_WEBP_QUALITY;
import static org.bytedeco.javacpp.opencv_imgcodecs.imencode;
import org.bytedeco.javacpp.opencv_videoio;
import static org.bytedeco.javacpp.opencv_videoio.CV_CAP_PROP_FRAME_HEIGHT;
import static org.bytedeco.javacpp.opencv_videoio.CV_CAP_PROP_FRAME_WIDTH;

/**
 *
 * @author Gustavo
 */
public class App {

    opencv_core.Mat matFrame;
    private String base64String;
    private BytePointer buffer;
    private IntPointer parametros;

    private ScheduledExecutorService executor;
    private opencv_videoio.VideoCapture captura = new opencv_videoio.VideoCapture();
    private boolean cameraAtiva = false;
    private Session session;

    static Gson gson = new Gson();

    static RXTXUtil rxtx;

    public App() throws Exception {
        rxtx = new RXTXUtil();
        rxtx.conectar(CommPortIdentifier.getPortIdentifier("/dev/ttyACM0"));
    }

    /**
     * Metodo para conexao com o Websocket
     */
    public void conectarAoWebsocket() {
        WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        try {
            session = webSocketContainer.connectToServer(ExemploStreamCamera.MyEndpoint.class, URI.create("ws://arduinowebsocket-ws.herokuapp.com/projetoiot/websocket"));
            //Quando for testar localmente:            
            //session = webSocketContainer.connectToServer(ExemploStreamCamera.MyEndpoint.class, URI.create("ws://LOCALHOST:3000/projetoiot/websocket"));
        } catch (DeploymentException ex) {
            System.out.println(ex.getMessage());
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Metodo utilizado para ligar ou desligar a camera (acionado pelo botao)
     */
    public void ligarCamera() {
        // Verifica se a camera está ativa
        if (!cameraAtiva) {
            // Começa a captura de video na camera 0
            captura.open(0);
            // Definindo largura do quadro a ser capturado
            captura.set(CV_CAP_PROP_FRAME_WIDTH, 150.0);
            // Definindo altura do quadro a ser capturado
            captura.set(CV_CAP_PROP_FRAME_HEIGHT, 150.0);

            parametros = new IntPointer(CV_IMWRITE_WEBP_QUALITY, 10);
            matFrame = new opencv_core.Mat();
            buffer = new BytePointer();
            // buffer2 = new BytePointer();

            // verifica se a captura está aberta
            if (this.captura.isOpened()) {
                // muda para camera ativa
                cameraAtiva = true;
                // Thread para capturar um quadro(frame)
                Runnable atualizarQuadro = new Runnable() {
                    @Override
                    public void run() {
                        capturarQuadro();
                    }
                };

                executor = Executors.newSingleThreadScheduledExecutor();
                // A cada 100 milisegundos o atualizarQuadro é executado
                executor.scheduleAtFixedRate(atualizarQuadro, 0, 100, TimeUnit.MILLISECONDS);
            } else {
                System.out.println("Impossivel abrir uma conexão com a camera...");
            }
        } else {
            // Muda estado da camera
            cameraAtiva = false;
            // Muda o texto do botão
            try {
                // Desativa o executor
                executor.shutdown();
                executor.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            // Desliga a Camera
            captura.release();
        }
    }

    private void capturarQuadro() {
        // Criar Matriz para amarzenar o quadro
        if (captura.isOpened()) {
            try {
                // Realiza a captura do quadro e coloca na Matriz
                captura.read(matFrame);
                if (!matFrame.empty()) {

                    // Transforma a imagem capturada em webp comprimido em memória (esse será enviado via websocket, é mais leve)
                    imencode(".webp", matFrame, buffer, parametros);
                    // Codifica os bytes da imagem comprimida em Base64 (estudar codificacao Base64))
                    base64String = Base64.getEncoder().encodeToString(buffer.getStringBytes());
                    // Envia a imagem codificada em String de Base64 pelo WebSocket
                    String dados = "{\"imagem\":\"" + base64String + "\",\"dataCliente\":\"" + new Date() + "\"}";
                    session.getAsyncRemote().sendText(dados);
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    /**
     * Classe interna que define ações a serem tomadas conforme eventos do
     * Websocket
     */
    @ClientEndpoint
    public static class MyEndpoint {

        @OnOpen
        public void onOpen(Session session) {
            System.out.println("Conectado: " + session);
        }

        @OnMessage
        public void processGreeting(String message, Session session) {
            Mensagem msg = gson.fromJson(message, Mensagem.class);
            if (msg.getComando() != null) {
                System.out.println("Mensagem Recebida:" + msg.getComando());
                try {
                    rxtx.enviarDados(msg.getComando());
                } catch (IOException ex) {
                    Logger.getLogger(ExemploStreamCamera.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
        // @OnClose, @OnOpen, @OnError
    }
}
