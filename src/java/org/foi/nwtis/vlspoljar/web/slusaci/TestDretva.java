/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.foi.nwtis.vlspoljar.web.slusaci;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.foi.nwtis.vlspoljar.konfiguracije.Konfiguracija;

/**
 *
 * @author Personal
 */
public class TestDretva extends Thread {

    Konfiguracija konfig;
    Socket veza;

    public TestDretva() {
        konfig = (Konfiguracija) SlusacAplikacije.context.getAttribute("Konfig");
    }

    @Override
    public void interrupt() {
        try {
            if (veza != null) {
                veza.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(TestDretva.class.getName()).log(Level.SEVERE, null, ex);
        }
        super.interrupt(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void run() {
        try {
            int i=0;
            while (true) {
                sleep(5000);
                veza = new Socket(konfig.dajPostavku("socket.adresa"), Integer.parseInt(konfig.dajPostavku("socket.port")));
                OutputStream os = veza.getOutputStream();
                System.out.println("i=" + i);
                switch (i) {
                    case 0:
                        os.write("USER admin; PASSWD 123456; PAUSE;".getBytes());
                        break;
                    case 1:
                        os.write("USER admin; PASSWD 123456; STOP;".getBytes());                      
                        break;
                    case 2:
                        os.write("USER admin; PASSWD 123456; START;".getBytes());
                        break;
                    case 3:
                        os.write("USER admin; PASSWD 123456; ADD \"Karlovac\";".getBytes());
                        break;
                    case 4:
                        os.write("USER admin; PASSWD 123456; TEST \"Split\";".getBytes());
                        break;
                    case 5:
                        os.write("USER admin; PASSWD 123456; GET \"Rijeka\";".getBytes());
                        break;
                    case 6:
                        os.write("USER admin; PASSWD 123456; ADD Marija; NEWPASSWD 4321;".getBytes());
                        break;
                    case 7:
                        os.write("USER vlspoljar; GET \"Split\";".getBytes());
                        break;
                }
                os.close();
                i++;
                if(i==8)i=0;
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(TestDretva.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(TestDretva.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            veza.close();
        } catch (IOException ex) {
            Logger.getLogger(TestDretva.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public synchronized void start() {
        System.out.println("~~~~~TestDretva.start()");
        super.start(); //To change body of generated methods, choose Tools | Templates.
    }

}
