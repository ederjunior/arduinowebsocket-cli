/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ifms.cx.iot.gui;

import br.ifms.cx.iot.app.Mensagem;
import br.ifms.cx.iot.util.RXTXUtil;
import com.google.gson.Gson;
import gnu.io.CommPortIdentifier;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_IMWRITE_JPEG_QUALITY;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_IMWRITE_PNG_COMPRESSION;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_IMWRITE_WEBP_QUALITY;
import static org.bytedeco.javacpp.opencv_imgcodecs.imencode;
import org.bytedeco.javacpp.opencv_videoio;
import static org.bytedeco.javacpp.opencv_videoio.CV_CAP_PROP_FRAME_HEIGHT;
import static org.bytedeco.javacpp.opencv_videoio.CV_CAP_PROP_FRAME_WIDTH;

/**
 *
 * @author Gustavo
 */
public class ExemploStreamCamera {

    opencv_core.Mat matFrame;
    private String base64String;
    private BytePointer buffer;
    private BytePointer buffer2;
    private IntPointer parametros;

    private JButton botaoCamera;
    private JPanel painelPrincipal;
    private JPanel painelBotao;
    private JLabel painelDeImagem;

    private ScheduledExecutorService executor;
    private opencv_videoio.VideoCapture captura = new opencv_videoio.VideoCapture();
    private boolean cameraAtiva = false;
    private Session session;

    static Gson gson = new Gson();

    static RXTXUtil rxtx;

    private double width;
    private double heigth;
    private int quality;

    public ExemploStreamCamera(double width, double heigth, int quality) throws Exception {
        this.width = width;
        this.heigth = heigth;
        this.quality = quality;
        // Tamanho da Janela
        // setSize(370, 360);
        // Operação ao fechar a janela
        // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Inicialização de componentes graficos
        painelPrincipal = new JPanel(new BorderLayout(10, 10));
        painelBotao = new JPanel();
        painelDeImagem = new JLabel();
        botaoCamera = new JButton("Ligar Câmera");

        painelDeImagem.setHorizontalAlignment(JLabel.CENTER);
        painelDeImagem.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        painelPrincipal.add(painelDeImagem, BorderLayout.CENTER);
        painelPrincipal.add(painelBotao, BorderLayout.SOUTH);
        painelBotao.add(botaoCamera);

        painelPrincipal.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //add(painelPrincipal);
        // Definindo ação do botão
        botaoCamera.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ligarCamera();
            }
        });

        // Conectar ao websocket
        conectarAoWebsocket();
        rxtx = new RXTXUtil();
//        List<CommPortIdentifier> lista = rxtx.listarPortasSeriais();
//        System.out.println("Portas:");
//        for (CommPortIdentifier object : lista) {
//            System.out.println(object.getName());
//        }
//        Scanner s = new Scanner(System.in);
//        System.out.println("Digite a porta");
        String os = System.getProperty("os.name");
        System.out.println("OS: " + os);
        if (os.contains("Windows")) {
            rxtx.conectar(rxtx.listarPortaSerial());
        } else {
            rxtx.conectar(CommPortIdentifier.getPortIdentifier("/dev/ttyACM0"));
        }

        Thread t = new Thread(new Runnable() {
            public void run() {
                String dadoRecebido;
                while (true) {
                    try {
                        dadoRecebido = rxtx.receberDados();
                        if (dadoRecebido != null) {
                            System.out.println("Arduino: "+dadoRecebido);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(ExemploStreamCamera.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        //t.start();
        ligarCamera();

    }

    /**
     * Metodo para conexao com o Websocket
     */
    private void conectarAoWebsocket() {
        WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        try {
            session = webSocketContainer.connectToServer(ExemploStreamCamera.MyEndpoint.class, URI.create("ws://arduinowebsocket-ws.herokuapp.com/projetoiot/websocket"));

            //Quando for testar localmente:            
            //session = webSocketContainer.connectToServer(ExemploStreamCamera.MyEndpoint.class, URI.create("ws://LOCALHOST:3000/projetoiot/websocket"));
            // session = webSocketContainer.connectToServer(ExemploStreamCamera.MyEndpoint.class, URI.create("ws://192.168.0.1:3000/projetoiot/websocket"));
        } catch (DeploymentException ex) {
            System.out.println(ex.getMessage());
            //JOptionPane.showMessageDialog(this, "Erro " + ex.getMessage(), "Problema", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            //JOptionPane.showMessageDialog(this, "Erro " + ex.getMessage(), "Problema", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Metodo utilizado para ligar ou desligar a camera (acionado pelo botao)
     */
    private void ligarCamera() {
        // Verifica se a camera está ativa
        if (!cameraAtiva) {
            // Começa a captura de video na camera 0
            captura.open(0);
            // Definindo largura do quadro a ser capturado
            captura.set(CV_CAP_PROP_FRAME_WIDTH, width);
            // Definindo altura do quadro a ser capturado
            captura.set(CV_CAP_PROP_FRAME_HEIGHT, heigth);

            parametros = new IntPointer(CV_IMWRITE_WEBP_QUALITY, quality);
            //parametros = new IntPointer(CV_IMWRITE_PNG_COMPRESSION, 10);
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

                botaoCamera.setText("Desligar Câmera");
            } else {
                System.out.println("Erro de conexão");
                //JOptionPane.showMessageDialog(this, "Impossivel abrir uma conexão com a camera...", "Problema", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Muda estado da camera
            cameraAtiva = false;
            // Muda o texto do botão
            botaoCamera.setText("Ligar Câmera");

            try {
                // Desativa o executor
                executor.shutdown();
                executor.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                //JOptionPane.showMessageDialog(this, "Erro ao capturar, desconectando da camera... " + e, "Problema", JOptionPane.ERROR_MESSAGE);
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
                    // Buffer de bytes que armazenará a imagem depois de comprimida                  
                    // Seta parametro de qualidade da imagem (quanto maior a qualidade, maior é a imagem...)

                    // Transforma a imagem capturada em webp comprimido em memória (esse será enviado via websocket, é mais leve)
                    imencode(".webp", matFrame, buffer, parametros);
                    // Transforma a imagem capturada em jpg comprimido em memória (esse será mostrado no painel de imagem da aplicação, pois o java não tem suporte para webp) 
                    //imencode(".jpg", matFrame, buffer2);

                    //painelDeImagem.setIcon(new ImageIcon(buffer.getStringBytes()));
                    // Codifica os bytes da imagem comprimida em Base64 (estudar codificacao Base64))
                    base64String = Base64.getEncoder().encodeToString(buffer.getStringBytes());
                    // Envia a imagem codificada em String de Base64 pelo WebSocket

                    String dados = "{\"imagem\":\"" + base64String + "\",\"dataCliente\":\"" + new Date() + "\"}";
                    session.getAsyncRemote().sendText(dados);
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                //System.err.println(e);
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
            System.out.println("Client Receiver conectado: " + session);
        }

        @OnMessage
        public void processGreeting(String message, Session session) {
            Mensagem msg = gson.fromJson(message, Mensagem.class);
            if (msg.getComando() != null) {
                try {
                    System.out.println(msg.getComando());
                    if ("38".equals(msg.getComando())) {
                        rxtx.enviarDados("1");
                    } else if ("37".equals(msg.getComando())) {
                        rxtx.enviarDados("2");
                    } else if ("39".equals(msg.getComando())) {
                        rxtx.enviarDados("3");
                    } else if ("40".equals(msg.getComando())) {
                        rxtx.enviarDados("4");
                    } else if ("0".equals(msg.getComando())) {
                        rxtx.enviarDados("0");
                    }
                } catch (IOException ex) {
                    Logger.getLogger(ExemploStreamCamera.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }

        @OnClose
        public void onClose() {
            System.out.println("Conexão fechada");
            try {
                rxtx.enviarDados("0");
            } catch (IOException ex) {
                Logger.getLogger(ExemploStreamCamera.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        // @OnClose, @OnOpen, @OnError
    }
}
