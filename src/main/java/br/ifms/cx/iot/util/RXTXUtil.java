package br.ifms.cx.iot.util;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 *
 * @author Gustavo
 */
public class RXTXUtil {

    private OutputStream serialOut;
    private InputStream serialIn;
    private byte[] readBuffer;
    private SerialPort serialPort;

    public List<CommPortIdentifier> listarPortasSeriais() {
        Enumeration<CommPortIdentifier> identificadoresDasPortas
                = CommPortIdentifier.getPortIdentifiers();

        List<CommPortIdentifier> listaDePortas
                = new ArrayList<CommPortIdentifier>();

        CommPortIdentifier porta;
        while (identificadoresDasPortas.hasMoreElements()) {
            porta = identificadoresDasPortas.nextElement();
            listaDePortas.add(porta);
        }
        return listaDePortas;
    }

    public CommPortIdentifier listarPortaSerial() {
        Enumeration<CommPortIdentifier> identificadoresDasPortas
                = CommPortIdentifier.getPortIdentifiers();

        List<CommPortIdentifier> listaDePortas
                = new ArrayList<CommPortIdentifier>();

        CommPortIdentifier porta;
        while (identificadoresDasPortas.hasMoreElements()) {
            porta = identificadoresDasPortas.nextElement();
            listaDePortas.add(porta);
        }
        System.out.println(listaDePortas.get(0));
        return listaDePortas.get(0);

    }

    public void conectar(CommPortIdentifier porta) throws Exception {
        serialPort = (SerialPort) porta.open("Comunicação serial", 9600);
        serialOut = serialPort.getOutputStream();
        serialIn = serialPort.getInputStream();
        readBuffer = new byte[800];
        serialPort.setSerialPortParams(9600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
    }

    public void desconectar() throws Exception {
        serialPort.close();
        serialOut.close();
    }

    public void enviarDados(String mensagem) throws IOException {
        serialOut.write(mensagem.getBytes());
    }

    public String receberDados() throws IOException {
        int availableBytes = serialIn.available();
        if (availableBytes > 0) {
            serialIn.read(readBuffer, 0, availableBytes);
            return new String(readBuffer, 0, availableBytes);
        }
        return null;
    }
}
