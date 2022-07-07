/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verticaldin;

import modelo.Tabla;
import modelo.BD;
import datos.AccesoDatosMy;
import datos.AccesoDatosPG;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import modelo.Atributo;
import modelo.Fragmento;
import modelo.TablaCosto;

/**
 *
 * @author pipe
 */
public class Vigilante {

    AccesoDatosMy accesoXamana;
    private String token;
    String miIP; //IP del nodo a donde esté el vigilante
    BD datosBD;
    String dirLogs;
    int numeroFragmento;
    estadoHilo hilo;
    long caracterLog;
    String archivoActualLog;

    HashMap<String, String> NombreIp = new HashMap<>();

    public Vigilante(String token, String DirLogs) {
        this.dirLogs = DirLogs;
        this.token = token;
        hilo = new estadoHilo();
        miIP = this.getIP();
        try {
            ArrayList res = null;
            ArrayList res2 = null;
            ArrayList res3 = null;
            accesoXamana = new AccesoDatosMy();
            if (accesoXamana.conectar()) {
                String sql = "select BD.nombre, direccion, puerto, gestor, usr, psw, "
                        + "Tabla.nombre, Tabla.umbral_op, Tabla.umbral_des, Tabla.ndesempenio from BD "
                        + "join Tabla on Tabla.id_BD=BD.id_BD "
                        + "join Fragmento on Fragmento.id_tabla=Tabla.id_tabla "
                        + "where Fragmento.id_tabla=" + token;

                res = accesoXamana.ejecutarConsulta(sql);
                sql = "select id_atributo, tipo, size, nom, descriptor, multimedia "
                        + "from Atributo "
                        + "join Fragmento on Fragmento.id_fragmento=Atributo.id_fragmento "
                        + "where Fragmento.id_tabla=" + token + " and Atributo.pk=0";

                res3 = accesoXamana.ejecutarConsulta(sql);
                sql = "select id_atributo, tipo, size, nom, descriptor, multimedia "
                        + "from Atributo "
                        + "where Atributo.id_tabla=" + token + " and Atributo.pk=1";

                res2 = accesoXamana.ejecutarConsulta(sql);
                accesoXamana.desconectar();
            } else {
                System.out.println("Sin conexión");
            }
            if (!isNullorEmpty(res) && !this.isNullorEmpty(res2) && !this.isNullorEmpty(res3)) {
                System.out.println("Datos encontrados");
                this.datosBD = new BD();
                this.datosBD.setNombd("" + ((ArrayList) res.get(0)).get(0));
                this.datosBD.setDirbd("" + ((ArrayList) res.get(0)).get(1));
                this.datosBD.setPortbd("" + ((ArrayList) res.get(0)).get(2));
                this.datosBD.setTipoBD("" + ((ArrayList) res.get(0)).get(3));
                this.datosBD.setUsubd("" + ((ArrayList) res.get(0)).get(4));
                this.datosBD.setPassbd("" + ((ArrayList) res.get(0)).get(5));
                Tabla tabla = new Tabla();
                tabla.setIdTabla(token);
                tabla.setNombre("" + ((ArrayList) res.get(0)).get(6));
                tabla.setUmbralOP(Double.parseDouble("" + ((ArrayList) res.get(0)).get(7)));
                tabla.setUmbralCO(Double.parseDouble("" + ((ArrayList) res.get(0)).get(8)));
                tabla.setNdesempenio(Double.parseDouble("" + ((ArrayList) res.get(0)).get(9)));
                //buscar Atributos de tabla y agregarlos a la tabla
                ArrayList<Atributo> attTabla = new ArrayList<>();
                Atributo pk = new Atributo();
                pk.setIdAtributo("" + ((ArrayList) res2.get(0)).get(0));
                pk.setTipo("" + ((ArrayList) res2.get(0)).get(1));
                pk.setTamanio("" + ((ArrayList) res2.get(0)).get(2));
                pk.setNombre("" + ((ArrayList) res2.get(0)).get(3));
                pk.setDescriptor(false);
                pk.setMultimedia(false);
                pk.setPk(true);
                for (int k = 0; k < res3.size(); k++) {
                    Atributo a = new Atributo();
                    a.setIdAtributo("" + ((ArrayList) res3.get(k)).get(0));
                    a.setTipo("" + ((ArrayList) res3.get(k)).get(1));
                    a.setTamanio("" + ((ArrayList) res3.get(k)).get(2));
                    a.setNombre("" + ((ArrayList) res3.get(k)).get(3));
                    a.setDescriptor((("" + ((ArrayList) res3.get(k)).get(4)).compareTo("0") != 0));
                    a.setMultimedia((("" + ((ArrayList) res3.get(k)).get(5)).compareTo("0") != 0));
                    attTabla.add(a);
                }
                tabla.setAtributoLlave(pk);
                attTabla.add(pk);
                tabla.setAtributos(attTabla);
                tabla.generaTablaFragmento(); //crear fragmento de la tabla original, para que sea tratado posteriormente como un fragmento más
                tabla.llenaFragmentosxSitio(miIP);
                this.datosBD.setTabla(tabla);
                if (this.datosBD.getDirbd().compareTo(miIP) == 0) {
                    this.datosBD.getTabla().getFragmentos().add(this.datosBD.getTabla().getTablaFragmento());
                }
                System.out.println(this.datosBD.getTabla().getAtributoLlave().getNombre() + ":" + this.datosBD.getTabla().getAtributoLlave().getTipo() + ":" + this.datosBD.getTabla().getAtributoLlave().getTamanio());
                //Análisis de logs para detectar nuevos atributos (alter table add column), comparar atributos de base de xamana
                // para detectar el atributo nuevo, preguntar si es descriptor o multimedia, agregarlo a la base de xamana
                //si es descriptor o multimedia, hasta que este el duo de atributos separar en un nuevo fragmento.
                //Si no es ninguno de esos dos, entonces dejarlo en la tabla original.
                //Analizar si algun fragmento superó umbrales (incluida la tabla inicial)) mediante el analisis del log

                if (this.datosBD.getTipoBD().compareTo("a") == 0) {
                    //MYSQL
                    //Abro socket
                    hilo.start();
                    while (true) {
                        //La bandera de refragmentación indica cuando los fragmentos a analizar en los logs ya no son los mismos y es necesario 
                        //llenar nuevamente con los nuevos fragmentos que se analizaran.
                        if (hilo.banderaRefragmentacion) {
                            this.datosBD.getTabla().llenaFragmentosxSitio(miIP);
                            if (this.datosBD.getDirbd().compareTo(miIP) == 0) {
                                this.datosBD.getTabla().getFragmentos().add(this.datosBD.getTabla().getTablaFragmento());
                            }
                            hilo.banderaRefragmentacion = false;
                        }
                        //¿Es el servidor original?
                        if (this.datosBD.getDirbd().compareTo(miIP) == 0) {
                            //revisar agregado de atributos solo en servidor original
                            nuevosAtributos();
                        } else {
                            eliminadoAtributos();
                        }
                        logMySQL();
                        Thread.sleep(5000);
                    }
                } else if (this.datosBD.getTipoBD().compareTo("b") == 0 || this.datosBD.getTipoBD().compareTo("c") == 0) {
                    //POSTGRES
                } else {
                    //MONGO
                }
            } else {
                System.out.println("Ninguna base de datos o tabla para el token dado");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isNullorEmpty(ArrayList arr) {
        boolean res = false;
        try {
            if (arr == null || arr.isEmpty()) {
                res = true;
            } else {
                res = false;
            }
        } catch (Exception e) {
            res = true;
        }
        return res;
    }

    public boolean isNullorEmpty(String s) {
        return (s != null) ? ((!s.isEmpty()) ? false : true) : true;
    }

    protected String getIP() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (addr instanceof Inet6Address) {
                        continue;
                    }

                    ip = addr.getHostAddress();
                    System.out.println(ip);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ip;
    }

    private void logMySQL() {
        try {
            File carpeta = new File(dirLogs);
            File archivos[] = carpeta.listFiles();
            File ultimoModificado = null;
            for (int i = 0; i < archivos.length; i++) {
                if (archivos[i].isFile()) {
                    if (ultimoModificado == null) {
                        ultimoModificado = archivos[i];
                    }
                    if (archivos[i].lastModified() > ultimoModificado.lastModified()) {
                        ultimoModificado = archivos[i];
                    }
                }
            }
            if (ultimoModificado != null) {
                if (archivoActualLog != null) {
                    //se debe revisar el ultimo modificado
                    if (ultimoModificado.length() < this.caracterLog) {
                        this.caracterLog = 0;
                    }
                    if (ultimoModificado.toString() != archivoActualLog) {
                        archivoActualLog = ultimoModificado.toString();
                    }
                    RandomAccessFile raf = new RandomAccessFile(ultimoModificado, "r");
                    raf.seek(this.caracterLog);
                    logMySQLextencion(raf);
                } else {
                    for (File archivo : archivos) {
                        RandomAccessFile raf = new RandomAccessFile(archivo, "r");
                        logMySQLextencion(raf);
                    }
                    archivoActualLog = ultimoModificado.toString();
                }
                //revisar si algun fragmento superó umbrales
                double costoXFragmento = 0.0;
                for (Fragmento f : this.datosBD.getTabla().getFragmentos()) {
                    costoXFragmento = 0.0;
                    for (Atributo a : f.getAtributos()) {
                        for (String clave : a.getCostoAtributoxSitio().keySet()) {
                            costoXFragmento += a.getCostoAtributoxSitio().get(clave);
                        }
                    }
                    System.out.println("Costo de " + f.getNombre() + " es: " + costoXFragmento);
                    System.out.println("Umbral en " + this.datosBD.getTabla().getUmbralCO());
                    System.out.println("Desempeño inicial: " + f.getNdesempenio());
                    System.out.println("Desempeño a alcanzar: " + f.getNdesempenio() * (this.datosBD.getTabla().getUmbralCO() * .01));
                    if (costoXFragmento >= (f.getNdesempenio() * (this.datosBD.getTabla().getUmbralCO() * .01))) {
                        //ALCANZÓ UMBRAL
                        if (!f.isCbir()) {
                            HashMap<String, ArrayList<Atributo>> sitioAtributos = new HashMap<>();
                            ArrayList<TablaCosto> nuevoEsquema = new ArrayList<>();
                            ArrayList<Atributo> attsSinCosto = new ArrayList<>();
                            for (Atributo a : f.getAtributos()) {
                                try {
                                    String ip = this.getMaxEntry(a.getCostoAtributoxSitio()).getKey();
                                    if (sitioAtributos.containsKey(ip)) {
                                        //no se deben agregar atributos cbir
                                        if (!a.isDescriptor() && !a.isMultimedia() && !a.isPk()) {
                                            sitioAtributos.get(ip).add(a);
                                        }
                                    } else {
                                        ArrayList<Atributo> temp = new ArrayList<>();
                                        if (!a.isDescriptor() && !a.isMultimedia() && !a.isPk()) {
                                            temp.add(a);
                                        }
                                        sitioAtributos.put(ip, temp);
                                    }
                                } catch (Exception e) {
                                    //e.printStackTrace();
                                    if (!attsSinCosto.contains(a)) {
                                        if (!a.isDescriptor() && !a.isMultimedia() && !a.isPk()) {
                                            attsSinCosto.add(a);
                                        }
                                    }
                                }
                            }
                            
                            for (String key : sitioAtributos.keySet()) {
                                if (!sitioAtributos.get(key).isEmpty()) {
                                    TablaCosto tc = new TablaCosto();
                                    tc.setNombre(f.getNombre());
                                    tc.setSitio(key);
                                    tc.setCosto(costoXFragmento);
                                    tc.setArrAtri(sitioAtributos.get(key));
                                    tc.getArrAtri().add(this.datosBD.getTabla().getAtributoLlave());
                                    nuevoEsquema.add(tc);
                                }
                            }
                            if (attsSinCosto.size() > 0) {
                                nuevoEsquema.get(0).getArrAtri().addAll(attsSinCosto);
                            }
                            nuevoEsquema = this.nombrar(nuevoEsquema);
                            //si es fragmento que no sea la tabla original
                            //y la ip a donde se asignará es igual a la ip a donde estoy parado
                            //y es el único atributo
                            //no se debe refragmentar,
                            //si es fragmento de la tabla original, si se debe fragmentar
                            System.out.println("-------------------------NUEVO ESQUEMA---------------------");
                            for (TablaCosto tc : nuevoEsquema) {
                                System.out.println(tc.getNombre() + "\t" + tc.getSitio() + "\t" + tc.getAtributosxComa() + "\t" + tc.getCosto());
                                if (f.isFragmentoTabla()) {
                                    AccesoDatosMy origen = new AccesoDatosMy(this.miIP + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                                    AccesoDatosMy destino = new AccesoDatosMy(tc.getSitio() + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                                    int nFragmento = Integer.parseInt(tc.getNombre().substring(tc.getNombre().lastIndexOf("f") + 1, tc.getNombre().length()));
                                    Fragmentador fragmentador = new Fragmentador(origen, destino, tc.getArrAtri(), this.datosBD, nFragmento, false, 0, tc.getCosto(),f.getNombre());
                                    fragmentador.generarFragmentos();
                                    System.out.println(">>>>>>>>>>Generado<<<<<<<<<<<");
                                    //eliminar atributos del modelo despues de fragmentar 
                                    for (Atributo a : tc.getArrAtri()) {
                                        if (!a.isPk()) {
                                            this.datosBD.getTabla().getAtributos().removeIf(n -> (n.getIdAtributo().compareTo(a.getIdAtributo()) == 0));
                                            this.datosBD.getTabla().getTablaFragmento().getAtributos().removeIf(n -> (n.getIdAtributo().compareTo(a.getIdAtributo()) == 0));
                                        }
                                    }
                                } else {
                                    if (tc.getSitio().compareTo(this.miIP) == 0 && (f.getAtributos().size()) <= 2) {
                                        System.out.println("Alcanzó umbrales, pero no se fragmentará, ya que está en su \nmínima expresión y el sitio destino es el mismo al de origen");
                                    } else if (tc.getSitio().compareTo(this.miIP) == 0 && (f.getAtributos().size()) == tc.getArrAtri().size()) {
                                        System.out.println("Alcanzó umbrales, pero no se fragmentará, ya que son los mismos atributos actuales de este fragmento y el sitio destino es el mismo al de origen");
                                    } else {
                                        AccesoDatosMy origen = new AccesoDatosMy(this.miIP + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                                        AccesoDatosMy destino = new AccesoDatosMy(tc.getSitio() + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                                        int nFragmento = Integer.parseInt(tc.getNombre().substring(tc.getNombre().lastIndexOf("f") + 1, tc.getNombre().length()));
                                        Fragmentador fragmentador = new Fragmentador(origen, destino, tc.getArrAtri(), this.datosBD, nFragmento, false, 0, tc.getCosto(),f.getNombre());
                                        fragmentador.generarFragmentos();
                                        System.out.println(">>>>>>>>>>Generado<<<<<<<<<<<");
                                        if((f.getAtributos().size()) == tc.getArrAtri().size()){
                                            //borrar fragmento original (reasignación)
                                            fragmentador.eliminarOrigen(f.getId_fragmento());
                                        }
                                    }
                                }
                            }
                        } else {
                            System.out.println("No fragmentar, el fragmento es CBIR");
                        }
                    }
                }
            } else {
                System.out.println("No hay archivos en la carpeta");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logMySQLextencion(RandomAccessFile raf) {
        try {
            System.out.println("-----------COSTOS PIPE-------------");

            while (raf.getFilePointer() < raf.length()) {
                for (Fragmento f : this.datosBD.getTabla().getFragmentos()) {
                    f.setNdesempenioLineaLog(0.0);
                    f.setNoperacionesLineaLog(0.0);
                }
                String actual = raf.readLine();
                String ipLinea = "";
                String nombre = "";
                if (actual.contains("Connect\t") && !actual.contains("denied")) {
                    String y[] = actual.split("\t");
                    ipLinea = y[2].substring(y[2].indexOf("@") + 1, y[2].indexOf(" ", y[2].indexOf("@") + 1));
                    nombre = y[1].substring(0, y[1].indexOf(" "));
                    if (ipLinea.toLowerCase().compareTo("localhost") == 0 || ipLinea.toLowerCase().compareTo("::1") == 0) {
                        ipLinea = "127.0.0.1";
                    }
                    NombreIp.put(nombre, ipLinea);
                }
                if (actual.toLowerCase().contains("select") || actual.toLowerCase().contains("delete")
                        || actual.toLowerCase().contains("insert") || actual.toLowerCase().contains("update")) {
                    for (Fragmento f : this.datosBD.getTabla().getFragmentos()) {
                        System.out.println("Para fragmento: " + f.getNombre());
                        System.out.println(actual);
                        if (actual.toLowerCase().contains(f.getNombre().toLowerCase())) {
                            if (actual.toLowerCase().contains("select")) {
                                f.setNdesempenioLineaLog(1);
                            } else if (actual.toLowerCase().contains("delete") || actual.toLowerCase().contains("insert")) {
                                f.setNdesempenioLineaLog(2);
                            } else if (actual.toLowerCase().contains("update")) {
                                f.setNdesempenioLineaLog(3);
                            }
                            String y[] = actual.split("\t");
                            String sujeto = y[1].substring(0, y[1].indexOf(" "));
                            String ipTraduccion = NombreIp.get(sujeto);
                            System.out.println("SITIO: " + ipTraduccion);
                            if (ipTraduccion.compareTo(this.miIP) != 0) {
                                f.setNdesempenioLineaLog(f.getNdesempenioLineaLog() * 2);
                            }
                            System.out.println("VAO: " + f.getNdesempenioLineaLog());
                            int nJoins = incidencias(actual.toLowerCase(), " join ");
                            System.out.println("CR: " + nJoins);
                            for (int j = 0; j < f.getAtributos().size(); j++) {
                                System.out.println("Atributo: " + f.getAtributos().get(j).getNombre().toLowerCase());
                                if (actual.toLowerCase().contains(f.getAtributos().get(j).getNombre().toLowerCase()) || actual.contains(" * ")) {
                                    System.out.println("Tamaño de atributo " + f.getAtributos().get(j).getNombre() + ": " + Double.parseDouble(f.getAtributos().get(j).getTamanio()));
                                    Double valorActual = 0.0;
                                    if (f.getAtributos().get(j).getCostoAtributoxSitio().containsKey(ipTraduccion)) {
                                        valorActual = f.getAtributos().get(j).getCostoAtributoxSitio().get(ipTraduccion);
                                    }
                                    valorActual += f.getNdesempenioLineaLog() * nJoins * Double.parseDouble(f.getAtributos().get(j).getTamanio());
                                    f.getAtributos().get(j).getCostoAtributoxSitio().put(ipTraduccion, valorActual);
                                }
                            }
                        }
                    }
                }
            }
            this.caracterLog = raf.getFilePointer();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String buscaSitioXUsoAtributosMYSQL(String att1, String att2) {
        /*
         Método para buscar en log el sitio que más ocupa atributos CBIR
         */
        String IPmasOcupada = "";
        try {
            File carpeta = new File(dirLogs);
            File archivos[] = carpeta.listFiles();
            HashMap<String, ArrayList> NombreIpUsos = new HashMap<>();

            for (File item : archivos) {
                if (item.isFile()) {
                    RandomAccessFile raf = new RandomAccessFile(item.toString(), "r");
                    String ip = miIP;
                    String nombre = "";
                    while (raf.getFilePointer() < raf.length()) {
                        String actual = raf.readLine();
                        if (actual.contains("Connect\t") && !actual.contains("denied")) {
                            String y[] = actual.split("\t");
                            ip = y[2].substring(y[2].indexOf("@") + 1, y[2].indexOf(" ", y[2].indexOf("@") + 1));
                            nombre = y[1].substring(0, y[1].indexOf(" "));
                            if (ip.toLowerCase().compareTo("localhost") == 0 || ip.toLowerCase().compareTo("::1") == 0) {
                                ip = miIP;
                            }
                            ArrayList IpUso = new ArrayList<>();
                            IpUso.add(ip);  //IP
                            if (NombreIpUsos.get(nombre) != null) {
                                IpUso.add(((Integer) NombreIpUsos.get(nombre).get(1)));
                            } else {
                                IpUso.add(new Integer(0));
                            }
                            NombreIpUsos.put(nombre, IpUso);
                        }
                        if (actual.toLowerCase().contains(att1) || actual.toLowerCase().contains(att2)) {
                            String y[] = actual.split("\t");
                            nombre = y[1].substring(0, y[1].indexOf(" "));
                            NombreIpUsos.get(nombre).set(1, ((Integer) NombreIpUsos.get(nombre).get(1)) + 1);
                        }

                    }
                }
            }
            ArrayList valores = new ArrayList<>(NombreIpUsos.values());
            int aux = 0;
            for (int p = 0; p < valores.size(); p++) {
                ArrayList aux2 = (ArrayList) valores.get(p);
                if (((Integer) aux2.get(1)) > aux) {
                    aux = (Integer) aux2.get(1);
                    IPmasOcupada = "" + aux2.get(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (IPmasOcupada.isEmpty()) {
            IPmasOcupada = miIP;
        }
        return IPmasOcupada;
    }

    // Solo para servidor original (nodos no deben modificar sus atributos directamente)
    public void nuevosAtributos() {
        try {
            //Buscar si atributos en tabla original estan registrados en xamana
            AccesoDatosMy accExterno = new AccesoDatosMy(this.datosBD.getDirbd() + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
            ArrayList<String> atts = null;
            if (accExterno.conectar()) {
                atts = accExterno.atributos(this.datosBD.getNombd(), this.datosBD.getTabla().getNombre());
                accExterno.desconectar();
            }
            ArrayList<String> nombresAtributosEnXamana = new ArrayList<>();
            this.datosBD.getTabla().getAtributos().forEach(x -> nombresAtributosEnXamana.add(x.getNombre()));
            //nombresAtributosEnXamana.forEach(x -> System.out.println("Atributo logico: " + x));
            if (!this.isNullorEmpty(atts)) {
                for (String att : atts) {
                    String nombre = att.split(":")[0];
                    //System.out.println("Atributo fisico: " + nombre);
                    if (!nombresAtributosEnXamana.contains(nombre)) {
                        JRadioButton rbtn1 = new JRadioButton("Descriptor", true);
                        JRadioButton rbtn2 = new JRadioButton("Multimedia", false);
                        JRadioButton rbtn3 = new JRadioButton("Ninguna opción anterior", false);
                        ButtonGroup grupo1 = new ButtonGroup();
                        grupo1.add(rbtn1);
                        grupo1.add(rbtn2);
                        grupo1.add(rbtn3);
                        String message = "El atributo " + nombre + ", agregado recientemente, es:";
                        Object[] params = {message, rbtn1, rbtn2, rbtn3};
                        int n = JOptionPane.showConfirmDialog(null, params, "Tipo de atributo", JOptionPane.DEFAULT_OPTION);
                        //Si es descriptor o multimedia, revisar si ya se encuentra agregado el otro, para lazar una nueva fragmentación
                        //y agregar este atributo a la base de datos
                        //en caso de no ser descriptor o multimedia, agregarlo directamente a la base de datos
                        if (!rbtn3.isSelected()) {
                            if (rbtn1.isSelected() || rbtn2.isSelected()) {
                                //revisar si ya hay un multimedia agregado a xamana en la tabla original
                                ArrayList res = null;
                                if (accesoXamana.conectar()) {
                                    if (rbtn1.isSelected()) {
                                        res = accesoXamana.ejecutarConsulta("select id_atributo, nom, tipo, size from Atributo where multimedia=1 and id_tabla=" + this.token);
                                    } else if (rbtn2.isSelected()) {
                                        res = accesoXamana.ejecutarConsulta("select id_atributo, nom, tipo, size from Atributo where descriptor=1 and id_tabla=" + this.token);
                                    }
                                    accesoXamana.desconectar();
                                }
                                //registrar atributo en xamana
                                int t = 0;
                                String idAtributo = "";
                                if (this.accesoXamana.conectar()) {
                                    Atributo nAtt = new Atributo();
                                    if (rbtn1.isSelected()) {
                                        nAtt.setNombre(att.split(":")[0]);
                                        nAtt.setTipo(att.split(":")[1]);
                                        nAtt.setTamanio(att.split(":")[2]);
                                        nAtt.setDescriptor(true);
                                        nAtt.setMultimedia(false);
                                        nAtt.setPk(false);
                                        t = this.accesoXamana.ejecutarComando("insert into Atributo (tipo,size,nom,descriptor,multimedia,pk,id_tabla) values ('" + att.split(":")[1] + "'," + att.split(":")[2] + ",'" + att.split(":")[0] + "',1 , 0 ,0," + this.token + ")");
                                        idAtributo = "" + ((ArrayList) this.accesoXamana.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                                    } else if (rbtn2.isSelected()) {
                                        nAtt.setNombre(att.split(":")[0]);
                                        nAtt.setTipo(att.split(":")[1]);
                                        nAtt.setTamanio(att.split(":")[2]);
                                        nAtt.setDescriptor(false);
                                        nAtt.setMultimedia(true);
                                        nAtt.setPk(false);
                                        t = this.accesoXamana.ejecutarComando("insert into Atributo (tipo,size,nom,descriptor,multimedia,pk,id_tabla) values ('" + att.split(":")[1] + "'," + att.split(":")[2] + ",'" + att.split(":")[0] + "',0 ,1 ,0," + this.token + ")");
                                        idAtributo = "" + ((ArrayList) this.accesoXamana.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                                    }
                                    if (t == 1 && !this.isNullorEmpty(idAtributo)) {
                                        nAtt.setIdAtributo(idAtributo);
                                        this.datosBD.getTabla().getAtributos().add(nAtt);
                                        this.datosBD.getTabla().getTablaFragmento().getAtributos().add(nAtt);
                                        System.out.println("Atributo insertado");
                                    } else {
                                        System.out.println("Atributo no insertado");
                                    }
                                    this.accesoXamana.desconectar();
                                }
                                if (!this.isNullorEmpty(res)) {

                                    //buscar en log que sitio ocupó más estos atributos
                                    String sitio = buscaSitioXUsoAtributosMYSQL(nombre, "" + ((ArrayList) res.get(0)).get(0));
                                    //origen
                                    AccesoDatosMy accO = new AccesoDatosMy(this.datosBD.getDirbd() + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                                    //Destino
                                    AccesoDatosMy accD = new AccesoDatosMy(sitio + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                                    //Atributos
                                    Atributo att1 = new Atributo(idAtributo, nombre, att.split(":")[1], att.split(":")[2], (rbtn1.isSelected()) ? true : false, (rbtn2.isSelected()) ? true : false, false);
                                    Atributo att2 = new Atributo("" + ((ArrayList) res.get(0)).get(0), "" + ((ArrayList) res.get(0)).get(1), "" + ((ArrayList) res.get(0)).get(2), "" + ((Double) Double.parseDouble("" + ((ArrayList) res.get(0)).get(3))).intValue(), (rbtn1.isSelected()) ? true : false, (rbtn2.isSelected()) ? true : false, false);
                                    ArrayList<Atributo> atributos = new ArrayList<>(Arrays.asList(this.datosBD.getTabla().getAtributoLlave(), att1, att2));
                                    Fragmentador frag = new Fragmentador(accO, accD, atributos, this.datosBD, this.numeroFragmento++, true,this.datosBD.getTabla().getNombre());
                                    frag.generarFragmentos();
                                }
                            }
                        } else {
                            //atributo normal, no fragmentar, se debe analizar el log
                            //registrar atributo en xamana
                            int t = 0;
                            String idAtributo = "";
                            if (this.accesoXamana.conectar()) {
                                Atributo nAtt = new Atributo();
                                nAtt.setNombre(att.split(":")[0]);
                                nAtt.setTipo(att.split(":")[1]);
                                nAtt.setTamanio(att.split(":")[2]);
                                nAtt.setDescriptor(false);
                                nAtt.setMultimedia(false);
                                nAtt.setPk(false);
                                t = this.accesoXamana.ejecutarComando("insert into Atributo (tipo,size,nom,descriptor,multimedia,pk,id_tabla) values ('" + att.split(":")[1] + "'," + att.split(":")[2] + ",'" + att.split(":")[0] + "',0 , 0 ,0," + this.token + ")");
                                idAtributo = "" + ((ArrayList) this.accesoXamana.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                                if (t == 1 && !this.isNullorEmpty(idAtributo)) {
                                    nAtt.setIdAtributo(idAtributo);
                                    this.datosBD.getTabla().getAtributos().add(nAtt);
                                    this.datosBD.getTabla().getTablaFragmento().getAtributos().add(nAtt);
                                    System.out.println("Atributo insertado");
                                } else {
                                    System.out.println("Atributo no insertado");
                                }
                                this.accesoXamana.desconectar();
                            }
                        }
                    }
                }
            } else {
                System.out.println("Alguien borró los atributos de la tabla recientemente");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Atributo llavePostgres(AccesoDatosPG acc, String nombreBase, String nombreTabla) {

        Atributo att = null;
        ArrayList res = null;
        try {
            if (acc.conectar()) {
                String q = "select nombre, data_type "
                        + "from (SELECT column_name as nombre "
                        + "FROM information_schema.key_column_usage "
                        + "WHERE TABLE_NAME='" + nombreTabla + "' "
                        + "AND TABLE_CATALOG='" + nombreBase + "') as a "
                        + "join INFORMATION_SCHEMA.COLUMNS "
                        + "on INFORMATION_SCHEMA.COLUMNS.COLUMN_NAME=nombre "
                        + "where INFORMATION_SCHEMA.COLUMNS.TABLE_NAME='" + nombreTabla + "' "
                        + "and INFORMATION_SCHEMA.COLUMNS.TABLE_CATALOG='" + nombreBase + "';";
                res = acc.ejecutarConsulta(q);
                acc.desconectar();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (res != null) {
            att = new Atributo();
            att.setNombre("" + ((ArrayList) res.get(0)).get(0));
            att.setTipo("" + ((ArrayList) res.get(0)).get(1));
        }
        return att;
    }

    protected Atributo llaveMySQL(AccesoDatosMy acc, String nombreBase, String nombreTabla) {

        Atributo att = null;
        ArrayList res = null;
        try {
            if (acc.conectar()) {
                res = acc.ejecutarConsulta("SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = '" + nombreBase + "' AND TABLE_NAME = '" + nombreTabla + "' and COLUMN_KEY IN('PRI', 'UNI')");
                acc.desconectar();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (res != null) {
            att = new Atributo();
            att.setNombre("" + ((ArrayList) res.get(0)).get(0));
            att.setTipo("" + ((ArrayList) res.get(0)).get(1));
            att.setTipo("" + ((ArrayList) res.get(0)).get(2));
        }
        return att;
    }

    private void eliminadoAtributos() {
        try {
            for (Fragmento f : this.datosBD.getTabla().getFragmentos()) {
                //buscar si atributos en xamana no estan presentes (eliminados) en ESTE NODO (no en el servidor principal)
                AccesoDatosMy accExterno = new AccesoDatosMy(this.datosBD.getDirbd() + ":" + this.datosBD.getPortbd(), this.datosBD.getNombd(), this.datosBD.getUsubd(), this.datosBD.getPassbd());
                ArrayList<String> atributosEnFragmento = new ArrayList<String>();
                if (accExterno.conectar()) {
                    atributosEnFragmento = accExterno.atributos(this.datosBD.getNombd(), f.getNombre());
                    accExterno.desconectar();
                }
                if (!this.isNullorEmpty(atributosEnFragmento)) {
                    for (Atributo a : f.getAtributos()) {
                        if (!atributosEnFragmento.contains(a.getNombre())) {
                            //Se eliminó. ahora se requiere eliminar ese atributo en xamana
                            AccesoDatosMy xamana = new AccesoDatosMy();
                            if (xamana.conectar()) {
                                xamana.ejecutarComando("delete from Atributo where id_atributo=" + a.getIdAtributo());
                                xamana.desconectar();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int incidencias(String origen, String palabra) {
        //costo de reuniones=n+1 
        int i = 1;
        while (origen.indexOf(palabra) > -1) {
            origen = origen.substring(origen.indexOf(palabra) + palabra.length(), origen.length());
            i++;
        }
        return i;
    }

    public Map.Entry<String, Double> getMaxEntry(Map<String, Double> map) {
        Map.Entry<String, Double> maxEntry = null;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (maxEntry == null) {
                maxEntry = entry;
            } else {
                Double value = entry.getValue();
                if (null != value && value > maxEntry.getValue()) {
                    maxEntry = entry;
                }
            }
        }

        return maxEntry;
    }

    public ArrayList<TablaCosto> nombrar(ArrayList<TablaCosto> ori) {
        for (int i = 0; i < ori.size(); i++) {
            ori.get(i).setNombre(ori.get(i).getNombre() + "_f" + (i + 1));
        }
        return ori;
    }
}
