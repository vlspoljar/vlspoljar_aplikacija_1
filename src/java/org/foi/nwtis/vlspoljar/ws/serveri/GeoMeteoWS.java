/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.foi.nwtis.vlspoljar.ws.serveri;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import org.foi.nwtis.vlspoljar.konfiguracije.bp.BP_Konfiguracija;
import org.foi.nwtis.vlspoljar.rest.klijenti.GoogleMapsKlijent;
import org.foi.nwtis.vlspoljar.rest.klijenti.WeatherBugKlijent;
import org.foi.nwtis.vlspoljar.web.podaci.Adresa;
import org.foi.nwtis.vlspoljar.web.podaci.Location;
import org.foi.nwtis.vlspoljar.web.podaci.WeatherData;
import org.foi.nwtis.vlspoljar.web.slusaci.SlusacAplikacije;

/**
 * SOAP web servis
 *
 * @author Vlatko Špoljarić
 */
@WebService(serviceName = "GeoMeteoWS")
public class GeoMeteoWS {

    String cKey = "xLe1xGFXfID5UiEiVGFaLRYDH0AEPDt2";
    String sKey = "bUgkjOP5awSV7X24";

    /**
     * Operacija za dohvačanje svih adresa iz tablice adrese u bazi podataka
     *
     * Web service operation
     */
    @WebMethod(operationName = "dajSveAdrese")
    public List<Adresa> dajSveAdrese() {
        try {
            BP_Konfiguracija baza_konfig = (BP_Konfiguracija) SlusacAplikacije.context.getAttribute("BP_Konfig");
            Class.forName(baza_konfig.getDriver_database());
            String connUrl = baza_konfig.getServer_database() + baza_konfig.getUser_database();
            String sql = "SELECT * FROM vlspoljar_adrese";
            Connection conn = DriverManager.getConnection(connUrl, baza_konfig.getUser_username(), baza_konfig.getUser_password());

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            List<Adresa> listaAdresa = new ArrayList<>();

            while (rs.next()) {
                Adresa a = new Adresa(rs.getInt(1), rs.getString(2), new Location(rs.getString(3), rs.getString(4)));
                listaAdresa.add(a);
            }
            return listaAdresa;
        } catch (SQLException ex) {
            Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    /**
     * Operacija za dohačanje svih meteopodataka za adresu iz tablice
     * vlspoljar_meteopodaci
     *
     * Web service operation
     */
    @WebMethod(operationName = "dajVazeceMeteoPodatkeZaAdresu")
    public WeatherData dajVazeceMeteoPodatkeZaAdresu(@WebParam(name = "adresa") final String adresa) {
        if (adresa != null && adresa.length() > 0) {
            GoogleMapsKlijent gmk = new GoogleMapsKlijent();
            Location location = gmk.getGeoLocation(adresa);
            WeatherBugKlijent wbk = new WeatherBugKlijent(cKey, sKey);
            WeatherData wd = wbk.getRealTimeWeather(location.getLatitude(), location.getLongitude());
            return wd;
        } else {
            return null;
        }
    }

    /**
     * Operacija koja dohvaća najsvježije meteopodatke za odabranu adresu putem
     * WeatherBug klijenta
     *
     * Web service operation
     */
    @WebMethod(operationName = "dajSveMeteoPodatkeZaAdresu")
    public List<WeatherData> dajSveMeteoPodatkeZaAdresu(@WebParam(name = "adresa") final String adresa) {
        if (adresa != null && adresa.length() > 0) {
            try {
                BP_Konfiguracija baza_konfig = (BP_Konfiguracija) SlusacAplikacije.context.getAttribute("BP_Konfig");
                Class.forName(baza_konfig.getDriver_database());
                String connUrl = baza_konfig.getServer_database() + baza_konfig.getUser_database();
                String sql = "SELECT * FROM vlspoljar_meteopodaci WHERE adresa='" + adresa + "'";
                Connection conn = DriverManager.getConnection(connUrl, baza_konfig.getUser_username(), baza_konfig.getUser_password());

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                List<WeatherData> lista = new ArrayList<>();

                while (rs.next()) {
                    WeatherData wd = new WeatherData();
                    wd.setTemperature(rs.getFloat(3));
                    wd.setPressureSeaLevel(rs.getFloat(4));
                    wd.setHumidity(rs.getFloat(5));
                    wd.setWindSpeed(rs.getFloat(6));
                    lista.add(wd);
                }
                return lista;

            } catch (ClassNotFoundException ex) {
                Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SQLException ex) {
                Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return null;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "dajPosljednjihNZaAdresu")
    public List<WeatherData> dajPosljednjihNZaAdresu(@WebParam(name = "adresa") final String adresa, @WebParam(name = "posljednjihN") final int posljednjihN) {
        if (adresa != null && adresa.length() > 0) {
            try {
                BP_Konfiguracija baza_konfig = (BP_Konfiguracija) SlusacAplikacije.context.getAttribute("BP_Konfig");
                Class.forName(baza_konfig.getDriver_database());
                String connUrl = baza_konfig.getServer_database() + baza_konfig.getUser_database();
                String sql = "SELECT vlspoljar_meteopodaci.*, vlspoljar_adrese.adresa FROM vlspoljar_meteopodaci INNER JOIN vlspoljar_adrese ON vlspoljar_meteopodaci.adresa=vlspoljar_adrese.idAdresa WHERE vlspoljar_meteopodaci.adresa=(SELECT idAdresa FROM vlspoljar_adrese WHERE adresa='" + adresa + "') ORDER BY vlspoljar_meteopodaci.vrijeme DESC LIMIT " + posljednjihN + "";
                Connection conn = DriverManager.getConnection(connUrl, baza_konfig.getUser_username(), baza_konfig.getUser_password());

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                List<WeatherData> lista = new ArrayList<>();

                while (rs.next()) {
                    WeatherData wd = new WeatherData();
                    wd.setTemperature(rs.getFloat(3));
                    wd.setPressureSeaLevel(rs.getFloat(4));
                    wd.setHumidity(rs.getFloat(5));
                    wd.setWindSpeed(rs.getFloat(6));
                    wd.setRainRate(rs.getFloat(7));
                    wd.setSnowRate(rs.getFloat(8));
                    wd.setDate(rs.getString(9));
                    wd.setAddress(rs.getString(10));
                    lista.add(wd);
                }

                return lista;

            } catch (ClassNotFoundException ex) {
                Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SQLException ex) {
                Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return null;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "dajMeteoPodatkeUIntervalu")
    public List<WeatherData> dajMeteoPodatkeUIntervalu(@WebParam(name = "adresa") final String adresa, @WebParam(name = "odDate") final String odDate, @WebParam(name = "doDate") final String doDate) {
        if (adresa != null && adresa.length() > 0) {
            try {
                BP_Konfiguracija baza_konfig = (BP_Konfiguracija) SlusacAplikacije.context.getAttribute("BP_Konfig");
                Class.forName(baza_konfig.getDriver_database());
                String connUrl = baza_konfig.getServer_database() + baza_konfig.getUser_database();
                String sql = "SELECT vlspoljar_meteopodaci.*, vlspoljar_adrese.adresa FROM vlspoljar_meteopodaci INNER JOIN vlspoljar_adrese ON vlspoljar_meteopodaci.adresa=vlspoljar_adrese.idAdresa WHERE vlspoljar_meteopodaci.adresa=(SELECT idAdresa FROM vlspoljar_adrese WHERE adresa='" + adresa + "') AND (vrijeme BETWEEN '" + odDate + "' AND '" + doDate + "')";
                Connection conn = DriverManager.getConnection(connUrl, baza_konfig.getUser_username(), baza_konfig.getUser_password());

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                List<WeatherData> lista = new ArrayList<>();

                while (rs.next()) {
                    WeatherData wd = new WeatherData();
                    wd.setTemperature(rs.getFloat(3));
                    wd.setPressureSeaLevel(rs.getFloat(4));
                    wd.setHumidity(rs.getFloat(5));
                    wd.setWindSpeed(rs.getFloat(6));
                    wd.setRainRate(rs.getFloat(7));
                    wd.setSnowRate(rs.getFloat(8));
                    wd.setDate(rs.getString(9));
                    wd.setAddress(rs.getString(10));
                    lista.add(wd);
                }

                return lista;

            } catch (ClassNotFoundException ex) {
                Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SQLException ex) {
                Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        return null;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "dajPrvihNAdresa")
    public List<Adresa> dajPrvihNAdresa(@WebParam(name = "prvihN") final int prvihN) {
        try {
            BP_Konfiguracija baza_konfig = (BP_Konfiguracija) SlusacAplikacije.context.getAttribute("BP_Konfig");
            Class.forName(baza_konfig.getDriver_database());
            String connUrl = baza_konfig.getServer_database() + baza_konfig.getUser_database();
            String sql = "SELECT vlspoljar_adrese.*, COUNT(vlspoljar_meteopodaci.idMeteo) AS brojMeteo FROM vlspoljar_adrese LEFT JOIN vlspoljar_meteopodaci ON vlspoljar_adrese.idAdresa=vlspoljar_meteopodaci.adresa GROUP BY vlspoljar_adrese.adresa ORDER BY brojMeteo DESC LIMIT " + prvihN + "";
            Connection conn = DriverManager.getConnection(connUrl, baza_konfig.getUser_username(), baza_konfig.getUser_password());

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            List<Adresa> listaAdresa = new ArrayList<>();

            while (rs.next()) {
                Adresa a = new Adresa(rs.getInt(1), rs.getString(2), new Location(rs.getString(3), rs.getString(4)));
                listaAdresa.add(a);
            }

            return listaAdresa;


        } catch (SQLException ex) {
            Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GeoMeteoWS.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

}
