package br.ifms.cx.iot.gui;

import br.ifms.cx.iot.util.RXTXUtil;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import gnu.io.CommPortIdentifier;
import java.util.List;

@ClientEndpoint
public class JanelaPrincipal extends JFrame {

    private JLabel labelStatus;
    private JLabel webSocketSatus;

    private JLabel labelMensagem;
    private JLabel webSocketMensagem;

    private JPanel painelStatus;
    private JPanel painelMensagem;
    private JPanel painelCentro;

    private OutputStream serialOut;
    private InputStream serialIn;

    private RXTXUtil rxtxUtil;

    public JanelaPrincipal() {
        rxtxUtil = new RXTXUtil();
        // Tamanho da Janela
        setSize(500, 200);

        // Operação padrão ao fechar janela (Encerrar processo)
        setDefaultCloseOperation(JanelaPrincipal.EXIT_ON_CLOSE);

        // Painel de status
        Border borderStatus = BorderFactory.createTitledBorder("Status do WebSocket");
        painelStatus = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelStatus.setBorder(borderStatus);

        labelStatus = new JLabel("Status: ");
        webSocketSatus = new JLabel("-");

        painelStatus.add(labelStatus);
        painelStatus.add(webSocketSatus);

        // Painel de mensagens
        Border borderMensagem = BorderFactory.createTitledBorder("Mensagens recebidas do WebSocket");
        painelMensagem = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelMensagem.setBorder(borderMensagem);

        labelMensagem = new JLabel("Ultima Mensagem: ");
        webSocketMensagem = new JLabel("-");

        painelMensagem.add(labelMensagem);
        painelMensagem.add(webSocketMensagem);

        // Painel central
        painelCentro = new JPanel(new GridLayout(2, 1));
        painelCentro.add(painelStatus);
        painelCentro.add(painelMensagem);

        add(painelCentro, BorderLayout.CENTER);

        // Conectando ao WebSocket;
        try {
            //conectarAoWebSocket("ws://arduinowebsocket-ws.herokuapp.com/projetoiot/websocket");
            conectarAoWebSocket("ws://localhost:3000/projetoiot/websocket");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage() + " ", "Exception",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void conectarSerial() {
        List<CommPortIdentifier> portas = rxtxUtil.listarPortasSeriais();
       
    }

    private void conectarAoWebSocket(String url) throws DeploymentException, IOException {
        WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        URI uri = URI.create(url);
        webSocketContainer.connectToServer(this, uri);
    }

    @OnOpen
    public void quandoConectar(Session session) {
        webSocketSatus.setText("Conectado ao WebSocket, ID: " + session.getId());
    }

    @OnMessage
    public void quandoReceberMensagem(String mensagem, Session session) {
        webSocketMensagem.setText(mensagem);
        try {
            if (mensagem.equals("1")) {
                rxtxUtil.enviarDados("1");
            } else if (mensagem.equals("2")) {
                rxtxUtil.enviarDados("2");
            } else if (mensagem.equals("3")) {
                rxtxUtil.enviarDados("3");
            } else if (mensagem.equals("4")) {
                rxtxUtil.enviarDados("4");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // @OnClose, @OnOpen, @OnError
}
