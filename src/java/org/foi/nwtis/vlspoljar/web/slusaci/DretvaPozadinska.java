/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.foi.nwtis.vlspoljar.web.slusaci;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.foi.nwtis.vlspoljar.konfiguracije.Konfiguracija;
import org.foi.nwtis.vlspoljar.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.vlspoljar.rest.klijenti.GoogleMapsKlijent;
import org.foi.nwtis.vlspoljar.web.podaci.Location;

/**
 *
 * @author Branko
 */
public class DretvaPozadinska extends Thread {

    Socket veza;
    String komanda;
    String odgovor;
    InputStream is;
    OutputStream os;
    Store store;
    Session session;
    long zavrsetak;
    static Statement stmt1;
    static Statement stmt2;
    static Statement stmt3;
    static Statement stmt4;

    public DretvaPozadinska(Socket veza) {
        this.veza = veza;
    }

    static void spajanje() {
        try {
            BP_Konfiguracija baza_konfig = (BP_Konfiguracija) SlusacAplikacije.context.getAttribute("BP_Konfig");
            Class.forName(baza_konfig.getDriver_database());
            String connUrl = baza_konfig.getServer_database() + baza_konfig.getUser_database();
            Connection conn = DriverManager.getConnection(connUrl, baza_konfig.getUser_username(), baza_konfig.getUser_password());
            stmt1 = conn.createStatement();
            stmt2 = conn.createStatement();
            stmt3 = conn.createStatement();
            stmt4 = conn.createStatement();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DretvaPozadinska.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException ex) {
            Logger.getLogger(DretvaPozadinska.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void interrupt() {
        try {
            if (veza != null) {
                veza.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(DretvaPozadinska.class.getName()).log(Level.SEVERE, null, ex);
        }
        super.interrupt();
    }

    @Override
    public void run() {

        if (SlusacAplikacije.status == 3) {
            System.out.println("Prekid preuzimanja korisničkih komandi!");
        } else {
            komanda = "";
            try {
                is = veza.getInputStream();
                os = veza.getOutputStream();
                int znak;
                while ((znak = is.read()) != -1) {
                    komanda += (char) znak;
                }
            } catch (IOException ex) {
                Logger.getLogger(DretvaPozadinska.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.out.println("Komanda: " + komanda);
            odgovor = "";
            Matcher m = Pattern.compile("^USER ([A-Za-z]+);( PASSWD ([^\\s\\\\]+);(( PAUSE;)|( START;)|( STOP;)|( ADD \\\"([^/]+)\\\";)|( TEST \\\"([^/]+)\\\";)|( GET \\\"([^/]+)\\\";)|( ADD ([A-Za-z]+); NEWPASSWD ([^\\s\\\\]+);))|( GET \\\"([^/]+)\\\";))$").matcher(komanda);

            if (m.matches()) {

                //USER korisnik; GET "adresa";
                if (m.group(18) != null) {
                    try {
                        String zadnjiMeteo = "";
                        spajanje();
                        ResultSet rs = stmt1.executeQuery("SELECT temperature, humidity, pressureSeaLevel, vrijeme, vlspoljar_adrese.latitude, vlspoljar_adrese.longitude FROM vlspoljar_meteopodaci INNER JOIN vlspoljar_adrese ON vlspoljar_meteopodaci.adresa=vlspoljar_adrese.idAdresa WHERE vlspoljar_adrese.adresa='" + m.group(18) + "' ORDER BY vrijeme DESC LIMIT 1");
                        while (rs.next()) {
                            zadnjiMeteo = "TEMP " + rs.getString("temperature") + " VLAGA " + rs.getString("humidity") + " TLAK " + rs.getString("pressureSeaLevel") + " GEOSIR " + rs.getString("latitude") + " GEODUZ " + rs.getString("longitude");
                        }
                        if (zadnjiMeteo != null) {
                            odgovor = "OK 10; " + zadnjiMeteo;
                        } else {
                            odgovor = "ERR 52;";
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(DretvaPozadinska.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    long pocetak = System.nanoTime();
                    try {
                        spajanje();
                        ResultSet rsPrvojeriKorisnika = stmt2.executeQuery("SELECT username, password FROM vlspoljar_korisnici WHERE username='" + m.group(1) + "' AND password='" + m.group(3) + "'");
                        while (rsPrvojeriKorisnika.next()) {

                            if (m.group(1).equals(rsPrvojeriKorisnika.getString("username")) && m.group(3).equals(rsPrvojeriKorisnika.getString("password"))) {

                                //USER korisnik; PASSWD lozinka; PAUSE;
                                if (m.group(5) != null) {
                                    if (SlusacAplikacije.status == 1) {
                                        SlusacAplikacije.status = 2;
                                        SlusacAplikacije.primljene++;
                                        SlusacAplikacije.izvrsene++;
                                        odgovor = "OK 10; PAUSE";
                                    } else {
                                        SlusacAplikacije.primljene++;
                                        odgovor = "ERR 40;";
                                    }
                                }

                                //USER korisnik; PASSWD lozinka; START;
                                if (m.group(6) != null) {
                                    if (SlusacAplikacije.status == 2) {
                                        SlusacAplikacije.status = 1;
                                        SlusacAplikacije.primljene++;
                                        SlusacAplikacije.izvrsene++;
                                        odgovor = "OK 10; START";
                                    } else {
                                        SlusacAplikacije.primljene++;
                                        odgovor = "ERR 41;";
                                    }
                                }

                                //USER korisnik; PASSWD lozinka; STOP;
                                if (m.group(7) != null) {
                                    if (SlusacAplikacije.status != 3) {
                                        SlusacAplikacije.status = 3;
                                        SlusacAplikacije.primljene++;
                                        SlusacAplikacije.izvrsene++;
                                        odgovor = "OK 10; STOP";
                                    } else {
                                        SlusacAplikacije.primljene++;
                                        odgovor = "ERR 42;";
                                    }
                                }

                                //USER korisnik; PASSWD lozinka; ADD "adresa";
                                if (m.group(9) != null) {
                                    spajanje();

                                    GoogleMapsKlijent gmk = new GoogleMapsKlijent();
                                    Location location = gmk.getGeoLocation(m.group(9));
                                    int rsAddAdresa = stmt4.executeUpdate("INSERT INTO vlspoljar_adrese (adresa, latitude, longitude, korisnik) VALUES ('" + m.group(9) + "', '" + location.getLatitude() + "', '" + location.getLongitude() + "', '" + m.group(1) + "')");
                                    SlusacAplikacije.primljene++;
                                    SlusacAplikacije.izvrsene++;
                                    odgovor = "OK 10; ADD adresa";

                                }

                                //USER korisnik; PASSWD lozinka; TEST "adresa";
                                if (m.group(11) != null) {
                                    spajanje();
                                    ResultSet rsTest = stmt3.executeQuery("SELECT adresa FROM vlspoljar_adrese WHERE adresa='" + m.group(11) + "'");
                                    while (rsTest.next()) {
                                        if (m.group(11).equals(rsTest.getString("adresa"))) {
                                            SlusacAplikacije.primljene++;
                                            SlusacAplikacije.izvrsene++;
                                            odgovor = "OK 10; TEST adresa";
                                        } else {
                                            SlusacAplikacije.primljene++;
                                            odgovor = "ERR 51;";
                                        }
                                    }
                                }

                                //USER korisnik; PASSWD lozinka; GET "adresa";
                                if (m.group(13) != null) {
                                    try {
                                        String zadnjiMeteo = "";
                                        spajanje();
                                        ResultSet rsGetAdresa = stmt3.executeQuery("SELECT temperature, humidity, pressureSeaLevel, vrijeme, vlspoljar_adrese.latitude, vlspoljar_adrese.longitude FROM vlspoljar_meteopodaci INNER JOIN vlspoljar_adrese ON vlspoljar_meteopodaci.adresa=vlspoljar_adrese.idAdresa WHERE vlspoljar_adrese.adresa='" + m.group(13) + "' ORDER BY vrijeme DESC LIMIT 1");
                                        while (rsGetAdresa.next()) {
                                            zadnjiMeteo = "TEMP " + rsGetAdresa.getString("temperature") + " VLAGA " + rsGetAdresa.getString("humidity") + " TLAK " + rsGetAdresa.getString("pressureSeaLevel") + " GEOSIR " + rsGetAdresa.getString("latitude") + " GEODUZ " + rsGetAdresa.getString("longitude");
                                        }
                                        if (zadnjiMeteo != null) {
                                            SlusacAplikacije.primljene++;
                                            SlusacAplikacije.izvrsene++;
                                            odgovor = "OK 10; " + zadnjiMeteo;
                                        } else {
                                            SlusacAplikacije.primljene++;
                                            odgovor = "ERR 52;";
                                        }
                                    } catch (SQLException ex) {
                                        Logger.getLogger(DretvaPozadinska.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }

                                //USER korisnik; PASSWD lozinka; ADD korisnik1; NEWPASSWD lozinka1;
                                if (m.group(14) != null) {
                                    spajanje();

                                    int rsAddKorisnik = stmt4.executeUpdate("INSERT INTO vlspoljar_korisnici (username, password, vrsta) VALUES ('" + m.group(15) + "', '" + m.group(16) + "', 2)");
                                    SlusacAplikacije.primljene++;
                                    SlusacAplikacije.izvrsene++;
                                    odgovor = "OK 10; ADD korisnik";

                                }

                            } else {
                                SlusacAplikacije.primljene++;
                                odgovor = "ERR 30;";
                            }
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(DretvaPozadinska.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    zavrsetak = System.nanoTime() - pocetak;
                    try {
                        saljiMail(m.group(1), zavrsetak, SlusacAplikacije.primljene, SlusacAplikacije.neispravne, SlusacAplikacije.izvrsene);
                    } catch (MessagingException ex) {
                        Logger.getLogger(DretvaPozadinska.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

            } else {
                SlusacAplikacije.neispravne++;
                System.out.println("Kriva naredba!");
            }
            System.out.println(odgovor);
        }
        try {
            os.write(odgovor.getBytes());
            os.flush();
            os.close();
            is.close();
            veza.close();
        } catch (IOException ex) {
            Logger.getLogger(DretvaPozadinska.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public synchronized void start() {
        super.start(); //To change body of generated methods, choose Tools | Templates.
    }

    public void saljiMail(String korisnik, long vrijeme, int brP, int brN, int brI) throws NoSuchProviderException, MessagingException {

        Konfiguracija konfig = (Konfiguracija) SlusacAplikacije.context.getAttribute("Konfig");

        java.util.Properties properties = System.getProperties();
        session = Session.getInstance(properties, null);
        store = session.getStore("imap");
        store.connect(konfig.dajPostavku("email.posluzitelj"), konfig.dajPostavku("email.adresa"), konfig.dajPostavku("email.lozinka"));
        Folder mapa = store.getDefaultFolder();
        MimeMessage msg = new MimeMessage(session);
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(konfig.dajPostavku("email.adresa")));
        msg.setFrom(new InternetAddress(korisnik + "@foi.hr"));
        msg.setSubject(konfig.dajPostavku("email.predmet"));
        msg.setSentDate(new Date());
        msg.setText("Vrijeme izvrsavanja: " + vrijeme / 1000000 + "ms Broj primljenih: " + brP + " Broj neispravnih: " + brN + " Broj izvrsenih: " + brI);

        mapa = store.getFolder("Sent");
        if (!mapa.exists()) {
            mapa.create(Folder.HOLDS_MESSAGES);
        }
        mapa.appendMessages(new Message[]{msg});
        Transport.send(msg);
    }

}
