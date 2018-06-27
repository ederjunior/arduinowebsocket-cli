package br.ifms.cx.iot.app;

import br.ifms.cx.iot.gui.ExemploStreamCamera;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;

public class Main {
    public static void main(String[] args) throws IOException, DeploymentException {
        ExemploStreamCamera j;
        double width = 100;
        double heigth = 100;
        int quality = 10;
        try {
            for (String arg : args) {
                System.out.println(arg);
             
            }
            if(args.length>2){
                width = Double.parseDouble(args[0]);
                heigth = Double.parseDouble(args[1]);
                quality = Integer.parseInt(args[2]);
        
            }
            j = new ExemploStreamCamera(width,heigth,quality);
         //  j.setVisible(true);
        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
