/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.foi.nwtis.vlspoljar.web.slusaci;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.foi.nwtis.vlspoljar.konfiguracije.Konfiguracija;
import org.foi.nwtis.vlspoljar.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.vlspoljar.rest.klijenti.WeatherBugKlijent;
import org.foi.nwtis.vlspoljar.web.podaci.WeatherData;

/**
 * Pozadinska dretva u kojoj se preuzimaju meteopodaci za skup adresa u bazi
 * podataka putem REST web servisa WeatherBug i pohranjuju se u bazu podataka
 *
 * @author Vlatko Špoljarić
 */
public class DretvaZaMeteopodatke extends Thread {

    
    Konfiguracija konfig;

    public DretvaZaMeteopodatke() {
        konfig = (Konfiguracija) SlusacAplikacije.context.getAttribute("Konfig");
    }

    @Override
    public void interrupt() {
        super.interrupt();
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {
            try {
                long pocetak = System.nanoTime();
                
                if (SlusacAplikacije.status == 2) {
                    SlusacAplikacije.status = 1;
                    continue;
                }
                if (SlusacAplikacije.status == 3) {
                    break;
                }
                
                BP_Konfiguracija baza_konfig = (BP_Konfiguracija) SlusacAplikacije.context.getAttribute("BP_Konfig");
                Class.forName(baza_konfig.getDriver_database());
                String connUrl = baza_konfig.getServer_database() + baza_konfig.getUser_database();
                Connection conn = DriverManager.getConnection(connUrl, baza_konfig.getUser_username(), baza_konfig.getUser_password());
                Statement stmt = conn.createStatement();
                
                String sql = "SELECT * FROM vlspoljar_adrese";
                ResultSet rs = stmt.executeQuery(sql);

                while (rs.next()) {
                    int adresa = rs.getInt("idAdresa");
                    String lat = rs.getString("latitude");
                    String lng = rs.getString("longitude");
                    String cKey = konfig.dajPostavku("weatherBug.cKey");
                    String sKey = konfig.dajPostavku("weatherBug.sKey");
                    WeatherBugKlijent wbk = new WeatherBugKlijent(cKey, sKey);
                    WeatherData wd = wbk.getRealTimeWeather(lat, lng);

                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH.mm.ss");
                    Date datum = new Date();
                    String vrijeme = sdf.format(datum);

                    String sqlInsert = "INSERT INTO vlspoljar_meteopodaci (adresa, temperature, pressureSeaLevel, humidity, windSpeed, rainRate, snowRate, vrijeme) VALUES ('" + adresa + "', " + wd.getTemperature() + ", " + wd.getPressureSeaLevel() + ", " + wd.getHumidity() + ", " + wd.getWindSpeed() + ", " + wd.getRainRate() + ", " + wd.getSnowRate() + ", '" + vrijeme + "')";
                    Statement stmtInsert = conn.createStatement();
                    int rsInsert = stmtInsert.executeUpdate(sqlInsert);
                    if (rsInsert != 0) {
                        System.out.println("Dodani meteopodaci za adresu " + adresa);
                    }
                }

                try {
                    long spavanje = Integer.parseInt(konfig.dajPostavku("dzm.interval"))*1000 - (System.nanoTime() - pocetak) / 1000000;
                    if (spavanje < 0) {
                        spavanje = 0;
                    }
                    Thread.sleep(spavanje);
                } catch (InterruptedException ex) {
                    Logger.getLogger(DretvaZaMeteopodatke.class.getName()).log(Level.SEVERE, null, ex);
                }
            } catch (SQLException ex) {
                Logger.getLogger(DretvaZaMeteopodatke.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(DretvaZaMeteopodatke.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    @Override
    public synchronized void start() {
        super.start(); //To change body of generated methods, choose Tools | Templates.
    }

}
