/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.ifms.cx.iot.app;

/**
 *
 * @author Eder
 */
public class Mensagem {
    private String imagem;
    private String comando;
    private String dataCliente;
    private String dataServidor;

    public String getImagem() {
        return imagem;
    }

    public void setImagem(String imagem) {
        this.imagem = imagem;
    }

    public String getComando() {
        return comando;
    }

    public void setComando(String comando) {
        this.comando = comando;
    }

    public String getDataCliente() {
        return dataCliente;
    }

    public void setDataCliente(String dataCliente) {
        this.dataCliente = dataCliente;
    }

    public String getDataServidor() {
        return dataServidor;
    }

    public void setDataServidor(String dataServidor) {
        this.dataServidor = dataServidor;
    }
    
    
}
