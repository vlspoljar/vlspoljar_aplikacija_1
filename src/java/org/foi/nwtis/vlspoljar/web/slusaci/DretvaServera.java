/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.foi.nwtis.vlspoljar.web.slusaci;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.foi.nwtis.vlspoljar.konfiguracije.Konfiguracija;

/**
 *
 * @author Branko
 */
public class DretvaServera extends Thread{
    
    Konfiguracija konfig;
    Socket veza;
    ServerSocket server;
    
    public DretvaServera () {
        konfig = (Konfiguracija) SlusacAplikacije.context.getAttribute("Konfig");
    }

    @Override
    public void interrupt() {
        try {
            if (veza != null) {
                veza.close();
            }
            if (server != null) {
                server.close();
            }
            super.interrupt(); //To change body of generated methods, choose Tools | Templates.
        }
        catch (IOException ex) {
            Logger.getLogger(DretvaServera.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            server = new ServerSocket(Integer.parseInt(konfig.dajPostavku("socket.port")));
            while(true) {
                veza = server.accept();
                new DretvaPozadinska(veza).start();
            }
        } catch (IOException ex) {
            Logger.getLogger(DretvaServera.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public synchronized void start() {
        super.start(); //To change body of generated methods, choose Tools | Templates.
    }
    
}
