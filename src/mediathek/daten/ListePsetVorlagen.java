/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.daten;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import javax.swing.JFrame;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import mediathek.controller.Log;
import mediathek.file.GetFile;
import mediathek.tool.Funktionen;
import mediathek.tool.GuiFunktionen;
import mediathek.tool.Konstanten;
import mediathek.tool.TModel;
import msearch.tool.MSConst;

public class ListePsetVorlagen extends LinkedList<String[]> {

    public static final String BS_WIN_32 = "Windows-32Bit";
    public static final String BS_WIN_64 = "Windows-64Bit";
    public static final String BS_LINUX = "Linux";
    public static final String BS_MAC = "Mac";
    public static final String[] BS = {"", BS_WIN_32, BS_WIN_64, BS_LINUX, BS_MAC};
    //
    public static final String PGR = "Vorlage";
    public static final String PGR_NAME = "Name";
    public static final int PGR_NAME_NR = 0;
    public static final String PGR_BESCHREIBUNG = "Beschreibung";
    public static final int PGR_BESCHREIBUNG_NR = 1;
    public static final String PGR_VERSION = "Version";
    public static final int PGR_VERSION_NR = 2;
    public static final String PGR_BS = "Bs";
    public static final int PGR_BS_NR = 3;
    public static final String PGR_URL = "URL";
    public static final int PGR_URL_NR = 4;
    public static final String PGR_INFO = "Info";
    public static final int PGR_INFO_NR = 5;
    public static final int PGR_MAX_ELEM = 6;
    public static final String[] PGR_COLUMN_NAMES = {PGR_NAME, PGR_BESCHREIBUNG, PGR_VERSION, PGR_BS, PGR_URL, PGR_INFO};
    private final static int TIMEOUT = 10000;

    public TModel getTModel(String bs) {
        LinkedList<String[]> tmp = new LinkedList<>();
        String[][] object;
        if (this.size() > 0) {
            if (!bs.equals("")) {
                for (String[] aThi : this) {
                    if (aThi[PGR_BS_NR].contains(bs)) {
                        tmp.add(aThi);
                    }
                }
                object = new String[tmp.size()][PGR_MAX_ELEM];
                for (int i = 0; i < tmp.size(); i++) {
                    object[i] = tmp.get(i);
                }
            } else {
                object = new String[this.size()][PGR_MAX_ELEM];
                for (int i = 0; i < this.size(); i++) {
                    object[i] = this.get(i);
                }
            }
            return new TModel(object, PGR_COLUMN_NAMES);
        } else {
            return new TModel(new Object[][]{}, PGR_COLUMN_NAMES);
        }
    }

    public static ListePset getStandarset(JFrame parent, Daten ddaten, boolean replaceMuster) {
        ListePset listePset = null;
        String[] vorlage = null;
        ListePsetVorlagen listePsetVorlagen = new ListePsetVorlagen();
        if (listePsetVorlagen.loadListOfSets()) {
            for (String[] ar : listePsetVorlagen) {
                if (ar[PGR_NAME_NR].equalsIgnoreCase("Standardset " + Funktionen.getOsString())) {
                    vorlage = ar;
                    break;
                }
            }
            if (vorlage != null) {
                if (!vorlage[PGR_URL_NR].equals("")) {
                    listePset = ListePsetVorlagen.importPsetFile(parent, vorlage[ListePsetVorlagen.PGR_URL_NR], true);
                    if (listePset != null) {
                        listePset.version = vorlage[PGR_VERSION_NR];
                    }
                }
            }
        }
        if (listePset == null) {
            // dann nehmen wir halt die im jar-File
            // liefert das Standard Programmset für das entsprechende BS
            InputStreamReader inReader;
            switch (Funktionen.getOs()) {
                case LINUX:
                    inReader = new GetFile().getPsetVorlageLinux();
                    break;
                case MAC:
                    inReader = new GetFile().getPsetVorlageMac();
                    break;

                default:
                    inReader = new GetFile().getPsetVorlageWindows();
            }
            // Standardgruppen laden
            listePset = ListePsetVorlagen.importPset(inReader, true);
        }

        if (replaceMuster && listePset != null) {
            // damit die Variablen ersetzt werden
            ListePset.progMusterErsetzen(parent, listePset);
        }
        return listePset;
    }

    public boolean loadListOfSets() {
        try {
            this.clear();
            int event;
            XMLInputFactory inFactory = XMLInputFactory.newInstance();
            inFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            XMLStreamReader parser;
            InputStreamReader inReader;
            URLConnection conn;
            conn = new URL(Konstanten.ADRESSE_VORLAGE_PROGRAMMGRUPPEN).openConnection();
            conn.setRequestProperty("User-Agent", Daten.getUserAgent());
            conn.setReadTimeout(TIMEOUT);
            conn.setConnectTimeout(TIMEOUT);
            inReader = new InputStreamReader(conn.getInputStream(), MSConst.KODIERUNG_UTF);
            parser = inFactory.createXMLStreamReader(inReader);
            while (parser.hasNext()) {
                event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    if (parser.getLocalName().equals(PGR)) {
                        //wieder ein neuer Server, toll
                        String[] p = new String[PGR_MAX_ELEM];
                        get(parser, PGR, PGR_COLUMN_NAMES, p);
                        if (!p[PGR_URL_NR].equals("")) {
                            this.add(p);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.fehlerMeldung(398001963, ex);
            return false;
        }
        return true;
    }

    public static ListePset importPsetFile(JFrame parent, String dateiUrl, boolean log) {
        int timeout = 10_000; //10 Sekunden
        try {
            if (GuiFunktionen.istUrl(dateiUrl)) {
                URLConnection conn;
                conn = new URL(dateiUrl).openConnection();
                conn.setConnectTimeout(timeout);
                conn.setReadTimeout(timeout);
                conn.setRequestProperty("User-Agent", Daten.getUserAgent());
                return ListePsetVorlagen.importPset(new InputStreamReader(conn.getInputStream(), MSConst.KODIERUNG_UTF), log);
            } else {
                return ListePsetVorlagen.importPset(new InputStreamReader(new FileInputStream(dateiUrl), MSConst.KODIERUNG_UTF), log);
            }
        } catch (Exception ex) {
            if (log) {
                Log.fehlerMeldung(630048926, ex);
            }
            return null;
        }
    }

    public static ListePset importPsetText(Daten dd, String text, boolean log) {
        return ListePsetVorlagen.importPset(new InputStreamReader(new ByteArrayInputStream(text.getBytes())), log);
    }

    private static ListePset importPset(InputStreamReader in, boolean log) {
        DatenPset datenPset = null;
        ListePset liste = new ListePset();
        try {
            int event;
            XMLInputFactory inFactory = XMLInputFactory.newInstance();
            inFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            XMLStreamReader parser;
            parser = inFactory.createXMLStreamReader(in);
            while (parser.hasNext()) {
                event = parser.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    //String t = parser.getLocalName();
                    switch (parser.getLocalName()) {
                        case DatenPset.PROGRAMMSET:
                            datenPset = new DatenPset();
                            if (!get(parser, DatenPset.PROGRAMMSET, DatenPset.COLUMN_NAMES_, datenPset.arr)) {
                                datenPset = null;
                            } else {
                                liste.add(datenPset);
                            }
                            break;
                        case DatenProg.PROGRAMM:
                            if (datenPset != null) {
                                DatenProg datenProg = new DatenProg();
                                if (get(parser, DatenProg.PROGRAMM, DatenProg.COLUMN_NAMES_, datenProg.arr)) {
                                    datenPset.addProg(datenProg);
                                }
                            }
                            break;
                    }
                }
            }
            in.close();
        } catch (Exception ex) {
            if (log) {
                Log.fehlerMeldung(467810360, ex);
            }
            return null;
        }
        if (liste.isEmpty()) {
            return null;
        } else {
            return liste;
        }
    }

    private static boolean get(XMLStreamReader parser, String xmlElem, String[] xmlNames, String[] strRet) {
        boolean ret = true;
        int maxElem = strRet.length;
        for (int i = 0; i < maxElem; ++i) {
            strRet[i] = "";
        }
        try {
            while (parser.hasNext()) {
                int event = parser.next();
                if (event == XMLStreamConstants.END_ELEMENT) {
                    if (parser.getLocalName().equals(xmlElem)) {
                        break;
                    }
                }
                if (event == XMLStreamConstants.START_ELEMENT) {
                    for (int i = 0; i < maxElem; ++i) {
                        if (parser.getLocalName().equals(xmlNames[i])) {
                            strRet[i] = parser.getElementText();
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ret = false;
            Log.fehlerMeldung(467256394, ex);
        }
        return ret;
    }

}
