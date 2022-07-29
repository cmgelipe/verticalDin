/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verticaldin;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Projections;
import static com.mongodb.client.model.Updates.unset;
import datos.AccesoDatos;
import datos.AccesoDatosMG;
import datos.AccesoDatosMy;
import datos.AccesoDatosPG;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import modelo.Atributo;
import modelo.BD;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 *
 * @author pipe
 */
public class Fragmentador {

    private AccesoDatos origen;
    private AccesoDatos destino;
    AccesoDatosMy xamanaAcc;
    private ArrayList<Atributo> atts;
    private BD datosBD;
    int nFragmento;
    boolean fragmentacionCBIR;
    double noperaciones;
    double ndesempenio;
    String sTablaOrigen;

    public Fragmentador(AccesoDatos origen, AccesoDatos destino, ArrayList<Atributo> atts, BD datosBD, int nFragmento, boolean CBIR, String nombreTablaOrigen) {
        this.setOrigen(origen);
        this.setDestino(destino);
        this.setAtts(atts);
        this.datosBD = datosBD;
        this.nFragmento = nFragmento;
        this.fragmentacionCBIR = CBIR;
        this.sTablaOrigen = nombreTablaOrigen;
    }

    public Fragmentador(AccesoDatos origen, AccesoDatos destino, ArrayList<Atributo> atts, BD datosBD, int nFragmento, boolean CBIR, double noperaciones, double ndesempenio, String nombreTablaOrigen) {
        //Cuando no es CBIR tiene valores en ndesempenio y noperaciones
        this.setOrigen(origen);
        this.setDestino(destino);
        this.setAtts(atts);
        this.datosBD = datosBD;
        this.nFragmento = nFragmento;
        this.fragmentacionCBIR = CBIR;
        this.ndesempenio = ndesempenio;
        this.noperaciones = noperaciones;
        this.sTablaOrigen = nombreTablaOrigen;
    }

    public boolean generarFragmentos() {
        if (this.datosBD.getTipoBD().compareTo("a") == 0) {
            generarFragmentoMySQL();
        } else if (this.datosBD.getTipoBD().compareTo("b") == 0 || this.datosBD.getTipoBD().compareTo("c") == 0) {
            generarFragmentoPost();
        } else if (this.datosBD.getTipoBD().compareTo("d") == 0) {
            generarFragmentoMongo();
        } else {
            System.out.println("No es un tipo de BD válido");
        }
        return true;
    }

    /**
     * @return the origen
     */
    public AccesoDatos getOrigen() {
        return origen;
    }

    /**
     * @param origen the origen to set
     */
    public void setOrigen(AccesoDatos origen) {
        this.origen = origen;
    }

    /**
     * @return the destino
     */
    public AccesoDatos getDestino() {
        return destino;
    }

    /**
     * @param destino the destino to set
     */
    public void setDestino(AccesoDatos destino) {
        this.destino = destino;
    }

    /**
     * @return the atts
     */
    public ArrayList<Atributo> getAtts() {
        return atts;
    }

    /**
     * @param atts the atts to set
     */
    public void setAtts(ArrayList<Atributo> atts) {
        this.atts = atts;
    }

    private void generarFragmentoMySQL() {
        AccesoDatosMy accOrigen = (AccesoDatosMy) this.origen;
        AccesoDatosMy accDestino = (AccesoDatosMy) this.destino;
        try {
            String att_tipo = "";
            String att_nombre = "";
            for (Atributo r : this.getAtts()) {
                att_nombre += r.getNombre() + ", ";
                if (this.transTipoDato(r.getTipo()) != 0) {
                    att_tipo += r.getNombre() + " " + r.getTipo() + ", ";
                } else {
                    if (r.getTipo().compareTo("BIT") != 0) {
                        att_tipo += r.getNombre() + " " + r.getTipo() + "(" + r.getTamanio() + "), ";
                    } else {
                        att_tipo += r.getNombre() + " TINYINT, ";
                    }
                }
            }
            att_tipo = att_tipo.substring(0, att_tipo.length() - 2);
            att_nombre = att_nombre.substring(0, att_nombre.length() - 2);
            //recuperar datos
            ArrayList arr = null;
            if (accOrigen.conectar()) {
                arr = accOrigen.ejecutarConsulta("select " + att_nombre + " from " + this.sTablaOrigen);
                accOrigen.desconectar();
            }
            if (!this.isNullorEmpty(arr)) {
                //Crear nueva tabla
                if (accDestino.conectar()) {
                    accDestino.ejecutarComando("create table " + this.sTablaOrigen + "_f" + this.nFragmento + " (" + att_tipo + ", PRIMARY KEY (" + this.datosBD.getTabla().getAtributoLlave().getNombre() + "));");
                    //agregar datos
                    for (int i = 0; i < arr.size(); i++) {
                        String valores = "";
                        for (int j = 0; j < ((ArrayList) arr.get(0)).size(); j++) {
                            if (this.transTipoDato(this.getAtts().get(j).getTipo()) == 0 || this.transTipoDato(this.getAtts().get(j).getTipo()) == 2) {
                                valores += "\"" + ((ArrayList) arr.get(i)).get(j) + "\", ";
                            } else {
                                valores += ((ArrayList) arr.get(i)).get(j) + ", ";
                            }
                        }
                        valores = valores.substring(0, valores.length() - 2);
                        String q = "INSERT INTO " + this.sTablaOrigen + "_f" + this.nFragmento + " VALUES (" + valores + ");";
                        accDestino.ejecutarComando(q);

                    }
                    accDestino.desconectar();
                }

                //Registrar datos en xamana
                System.out.println("registra en xamana");
                xamanaAcc = new AccesoDatosMy();
                String idFragmento = "";
                String idSitio = "";
                if (xamanaAcc.conectar()) {
                    //id del sitio
                    ArrayList arr2 = null;
                    arr2 = xamanaAcc.ejecutarConsulta("select id_sitio from Sitio where direccion='" + accDestino.ip + "'");
                    if (this.isNullorEmpty(arr2)) {
                        xamanaAcc.ejecutarComando("insert into Sitio (direccion) values('" + accDestino.ip + "')");
                        idSitio = "" + ((ArrayList) xamanaAcc.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                    } else {
                        idSitio = "" + ((ArrayList) arr2.get(0)).get(0);
                    }
                    if (this.fragmentacionCBIR) {
                        xamanaAcc.ejecutarComando("insert into Fragmento(nombre, id_tabla, id_sitio, noperaciones, ndesempenio, isCBIR) value ('"
                                + this.sTablaOrigen + "_f" + this.nFragmento + "', " + this.datosBD.getTabla().getIdTabla()
                                + ", " + idSitio + ",0,0,1)");
                        idFragmento = "" + ((ArrayList) xamanaAcc.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);

                    } else {
                        xamanaAcc.ejecutarComando("insert into Fragmento(nombre, id_tabla, id_sitio, noperaciones, ndesempenio, isCBIR) value ('"
                                + this.sTablaOrigen + "_f" + this.nFragmento + "', " + this.datosBD.getTabla().getIdTabla()
                                + ", " + idSitio + "," + this.noperaciones + "," + this.ndesempenio + ",0)");
                        idFragmento = "" + ((ArrayList) xamanaAcc.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                    }
                    //Modificar atributos en xamana
                    if (!this.isNullorEmpty(idFragmento)) {
                        for (Atributo a : this.getAtts()) {
                            if (!a.isPk()) {
                                String sql = "update Atributo set id_tabla=NULL, id_fragmento=" + idFragmento + " where id_atributo=" + a.getIdAtributo();
                                System.out.println(sql);
                                xamanaAcc.ejecutarComando(sql);
                                System.out.println("Atributo " + a.getNombre() + " actualizado a nuevo fragmento");
                            }
                        }
                        xamanaAcc.ejecutarComando("insert into Atributo (tipo,size,nom,id_fragmento,descriptor,multimedia,pk,id_tabla) values ('"
                                + this.datosBD.getTabla().getAtributoLlave().getTipo() + "'," + this.datosBD.getTabla().getAtributoLlave().getTamanio() + ",'" + this.datosBD.getTabla().getAtributoLlave().getNombre() + "',"
                                + idFragmento + ",0,0,1,null)");
                    }
                    xamanaAcc.desconectar();
                }

                //Eliminar atributos en tabla original
                //Pendiente: primero eliminar relaciones de la tabla (foraneas)
                if (accOrigen.conectar()) {
                    for (Atributo b : this.getAtts()) {
                        //No eliminar PK
                        if (!b.isPk()) {
                            accOrigen.ejecutarComando("alter table " + this.sTablaOrigen + " drop column " + b.getNombre());
                            System.out.println(b.getNombre() + ": Atributo eliminado de la tabla original");
                        }
                    }
                    accOrigen.desconectar();
                }
                //solicitar actualización de fragmentos en el servidor destino (socket)
                Socket socket = new Socket(accDestino.ip, 5000);
                Socket socket2 = new Socket(accOrigen.ip, 5000);
                new DataOutputStream(socket.getOutputStream()).writeBoolean(true);
                new DataOutputStream(socket2.getOutputStream()).writeBoolean(true);
                socket.close();
                socket2.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generarFragmentoPost() {
        AccesoDatosPG accOrigen = (AccesoDatosPG) this.origen;
        AccesoDatosPG accDestino = (AccesoDatosPG) this.destino;
        try {
            String att_tipo = "";
            String att_nombre = "";
            for (Atributo r : this.getAtts()) {
                att_nombre += r.getNombre() + ", ";
                if (this.transTipoDato(r.getTipo()) != 0) {
                    if (r.isPk()) {
                        att_tipo += r.getNombre() + " " + r.getTipo() + " PRIMARY KEY, ";
                    } else {
                        att_tipo += r.getNombre() + " " + r.getTipo() + ", ";
                    }
                } else {
                    if (r.getTipo().compareTo("BIT") != 0) {
                        if (r.isPk()) {
                            att_tipo += r.getNombre() + " " + r.getTipo() + "(" + ((Double) Double.parseDouble(r.getTamanio())).intValue() + ") PRIMARY KEY, ";
                        } else {
                            att_tipo += r.getNombre() + " " + r.getTipo() + "(" + ((Double) Double.parseDouble(r.getTamanio())).intValue() + "), ";
                        }
                    } else {
                        if (r.isPk()) {
                            att_tipo += r.getNombre() + " TINYINT PRIMARY KEY, ";
                        } else {
                            att_tipo += r.getNombre() + " TINYINT, ";
                        }
                    }
                }
            }
            att_tipo = att_tipo.substring(0, att_tipo.length() - 2);
            att_nombre = att_nombre.substring(0, att_nombre.length() - 2);
            //recuperar datos
            ArrayList arr = null;
            if (accOrigen.conectar()) {
                arr = accOrigen.ejecutarConsulta("select " + att_nombre + " from " + this.sTablaOrigen);
                accOrigen.desconectar();
            }
            if (!this.isNullorEmpty(arr)) {
                //Crear nueva tabla
                if (accDestino.conectar()) {
                    accDestino.ejecutarComando("create table " + this.sTablaOrigen + "_f" + this.nFragmento + " (" + att_tipo + ");");
                    //agregar datos
                    for (int i = 0; i < arr.size(); i++) {
                        String valores = "";
                        for (int j = 0; j < ((ArrayList) arr.get(0)).size(); j++) {
                            if (this.transTipoDato(this.getAtts().get(j).getTipo()) == 0 || this.transTipoDato(this.getAtts().get(j).getTipo()) == 2) {
                                valores += "'" + ((ArrayList) arr.get(i)).get(j) + "', ";
                            } else {
                                valores += ((ArrayList) arr.get(i)).get(j) + ", ";
                            }
                        }
                        valores = valores.substring(0, valores.length() - 2);
                        String q = "INSERT INTO " + this.sTablaOrigen + "_f" + this.nFragmento + " VALUES (" + valores + ");";
                        accDestino.ejecutarComando(q);
                    }
                    accDestino.desconectar();
                }
                //Registrar datos en xamana
                System.out.println("registra en xamana");
                xamanaAcc = new AccesoDatosMy();
                String idFragmento = "";
                String idSitio = "";
                if (xamanaAcc.conectar()) {
                    //id del sitio
                    ArrayList arr2 = null;
                    arr2 = xamanaAcc.ejecutarConsulta("select id_sitio from Sitio where direccion='" + accDestino.ip + "'");
                    if (this.isNullorEmpty(arr2)) {
                        xamanaAcc.ejecutarComando("insert into Sitio (direccion) values('" + accDestino.ip + "')");
                        idSitio = "" + ((ArrayList) xamanaAcc.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                    } else {
                        idSitio = "" + ((ArrayList) arr2.get(0)).get(0);
                    }
                    if (this.fragmentacionCBIR) {
                        xamanaAcc.ejecutarComando("insert into Fragmento(nombre, id_tabla, id_sitio, noperaciones, ndesempenio, isCBIR) value ('"
                                + this.sTablaOrigen + "_f" + this.nFragmento + "', " + this.datosBD.getTabla().getIdTabla()
                                + ", " + idSitio + ",0,0,1)");
                        idFragmento = "" + ((ArrayList) xamanaAcc.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);

                    } else {
                        xamanaAcc.ejecutarComando("insert into Fragmento(nombre, id_tabla, id_sitio, noperaciones, ndesempenio, isCBIR) value ('"
                                + this.sTablaOrigen + "_f" + this.nFragmento + "', " + this.datosBD.getTabla().getIdTabla()
                                + ", " + idSitio + "," + this.noperaciones + "," + this.ndesempenio + ",0)");
                        idFragmento = "" + ((ArrayList) xamanaAcc.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                    }
                    //Modificar atributos en xamana
                    if (!this.isNullorEmpty(idFragmento)) {
                        for (Atributo a : this.getAtts()) {
                            if (!a.isPk()) {
                                String sql = "update Atributo set id_tabla=NULL, id_fragmento=" + idFragmento + " where id_atributo=" + a.getIdAtributo();
                                System.out.println(sql);
                                xamanaAcc.ejecutarComando(sql);
                                System.out.println("Atributo " + a.getNombre() + " actualizado a nuevo fragmento");
                            }
                        }
                        xamanaAcc.ejecutarComando("insert into Atributo (tipo,size,nom,id_fragmento,descriptor,multimedia,pk,id_tabla) values ('"
                                + this.datosBD.getTabla().getAtributoLlave().getTipo() + "'," + this.datosBD.getTabla().getAtributoLlave().getTamanio() + ",'" + this.datosBD.getTabla().getAtributoLlave().getNombre() + "',"
                                + idFragmento + ",0,0,1,null)");
                    }
                    xamanaAcc.desconectar();
                }

                //Eliminar atributos en tabla original
                //Pendiente: primero eliminar relaciones de la tabla (foraneas)
                if (accOrigen.conectar()) {
                    for (Atributo b : this.getAtts()) {
                        //No eliminar PK
                        if (!b.isPk()) {
                            accOrigen.ejecutarComando("alter table " + this.sTablaOrigen + " drop column " + b.getNombre());
                            System.out.println(b.getNombre() + ": Atributo eliminado de la tabla original");
                        }
                    }
                    accOrigen.desconectar();
                }
                //solicitar actualización de fragmentos en el servidor destino (socket)
                Socket socket = new Socket(accDestino.ip, 5000);
                Socket socket2 = new Socket(accOrigen.ip, 5000);
                new DataOutputStream(socket.getOutputStream()).writeBoolean(true);
                new DataOutputStream(socket2.getOutputStream()).writeBoolean(true);
                socket.close();
                socket2.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generarFragmentoMongo() {
        AccesoDatosMG accOrigen = (AccesoDatosMG) this.origen;
        AccesoDatosMG accDestino = (AccesoDatosMG) this.destino;
        try {
            if (accOrigen.conectar() && accDestino.conectar()) {
                //Destino
                MongoDatabase dbDestino = accDestino.getMongoClient().getDatabase(this.datosBD.getNombd());
                MongoCollection<Document> docDestino = dbDestino.getCollection(this.sTablaOrigen + "_f" + this.nFragmento);
                dbDestino.createCollection(this.sTablaOrigen + "_f" + this.nFragmento);

                //Origen
                MongoDatabase dbOrigen = accOrigen.getMongoClient().getDatabase(this.datosBD.getNombd());
                MongoCollection<Document> docOrigen = dbOrigen.getCollection(this.sTablaOrigen);

                List<String> nombresAtt = new ArrayList<>();
                this.atts.forEach(x -> nombresAtt.add(x.getNombre()));
                Bson proyeccion = Projections.fields(Projections.include(nombresAtt));
                MongoCursor<Document> resultado = docOrigen.find().projection(proyeccion).iterator();
                while (resultado.hasNext()) {
                    docDestino.insertOne(resultado.next());
                }
            }
            //Registrar datos en xamana
            System.out.println("registra en xamana");
            xamanaAcc = new AccesoDatosMy();
            String idFragmento = "";
            String idSitio = "";
            if (xamanaAcc.conectar()) {
                //id del sitio
                ArrayList arr2 = null;
                arr2 = xamanaAcc.ejecutarConsulta("select id_sitio from Sitio where direccion='" + accDestino.ip + "'");
                if (this.isNullorEmpty(arr2)) {
                    xamanaAcc.ejecutarComando("insert into Sitio (direccion) values('" + accDestino.ip + "')");
                    idSitio = "" + ((ArrayList) xamanaAcc.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                } else {
                    idSitio = "" + ((ArrayList) arr2.get(0)).get(0);
                }
                if (this.fragmentacionCBIR) {
                    xamanaAcc.ejecutarComando("insert into Fragmento(nombre, id_tabla, id_sitio, noperaciones, ndesempenio, isCBIR) value ('"
                            + this.sTablaOrigen + "_f" + this.nFragmento + "', " + this.datosBD.getTabla().getIdTabla()
                            + ", " + idSitio + ",0,0,1)");
                    idFragmento = "" + ((ArrayList) xamanaAcc.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);

                } else {
                    xamanaAcc.ejecutarComando("insert into Fragmento(nombre, id_tabla, id_sitio, noperaciones, ndesempenio, isCBIR) value ('"
                            + this.sTablaOrigen + "_f" + this.nFragmento + "', " + this.datosBD.getTabla().getIdTabla()
                            + ", " + idSitio + "," + this.noperaciones + "," + this.ndesempenio + ",0)");
                    idFragmento = "" + ((ArrayList) xamanaAcc.ejecutarConsulta("Select LAST_INSERT_ID()").get(0)).get(0);
                }
                //Modificar atributos en xamana
                if (!this.isNullorEmpty(idFragmento)) {
                    for (Atributo a : this.getAtts()) {
                        if (!a.isPk()) {
                            String sql = "update Atributo set id_tabla=NULL, id_fragmento=" + idFragmento + " where id_atributo=" + a.getIdAtributo();
                            System.out.println(sql);
                            xamanaAcc.ejecutarComando(sql);
                            System.out.println("Atributo " + a.getNombre() + " actualizado a nuevo fragmento");
                        }
                    }
                    xamanaAcc.ejecutarComando("insert into Atributo (tipo,size,nom,id_fragmento,descriptor,multimedia,pk,id_tabla) values ('"
                            + this.datosBD.getTabla().getAtributoLlave().getTipo() + "'," + this.datosBD.getTabla().getAtributoLlave().getTamanio() + ",'" + this.datosBD.getTabla().getAtributoLlave().getNombre() + "',"
                            + idFragmento + ",0,0,1,null)");
                }
                xamanaAcc.desconectar();
            }

            //Eliminar atributos en tabla original
            //Pendiente: primero eliminar relaciones de la tabla (foraneas)
            if (accOrigen.conectar()) {
                MongoDatabase dbOrigen = accOrigen.getMongoClient().getDatabase(this.datosBD.getNombd());
                MongoCollection<Document> docOrigen = dbOrigen.getCollection(this.sTablaOrigen);
                for (Atributo b : this.getAtts()) {
                    //No eliminar PK
                    if (!b.isPk()) {
                        docOrigen.updateMany(new Document(), unset(b.getNombre()));
                        System.out.println(b.getNombre() + ": Atributo eliminado de la tabla original");
                    }
                }
                accOrigen.desconectar();
            }
            //solicitar actualización de fragmentos en el servidor destino (socket)
            //TEMPORAL: COMENTADO DESTINO PORQUE NO ESTA EN EJECUCIÓN VIGILANTE FRAGMENTADOR EN DESTINO (CONNECTION REFUSED)
            
            //Socket socket = new Socket(accDestino.ip, 5000);
            Socket socket2 = new Socket(accOrigen.ip, 5000);
            //new DataOutputStream(socket.getOutputStream()).writeBoolean(true);
            new DataOutputStream(socket2.getOutputStream()).writeBoolean(true);
            //socket.close();
            socket2.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int transTipoDato(String tipo) {
        tipo = tipo.toUpperCase();
        switch (tipo) {
            case "CHAR":
            case "VARCHAR":
            case "BIT":
            case "TINYINT":
                return 0;
            case "INTEGER":
            case "SMALLINT":
            case "BIGINT":
            case "NUMERIC":
            case "DECIMAL":
            case "FLOAT":
            case "DOUBLE":
                return 1;
            case "DATE":
            case "TIME":
            case "TIMESTAMP":
            case "DATETIME":
                return 2;
            default:
                return 3;
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

    //Se elimina origen cuando el fragmento solo posee PK 
    void eliminarOrigenMySQL(String idFragmentoOriginal) {
        AccesoDatosMy accOrigen = (AccesoDatosMy) this.origen;
        try {
            xamanaAcc = new AccesoDatosMy();
            if (accOrigen.conectar()) {
                accOrigen.ejecutarComando("drop table " + this.sTablaOrigen);
                accOrigen.desconectar();
            }
            //eliminar el fragmento en xamana
            if (this.xamanaAcc.conectar()) {
                xamanaAcc.ejecutarComando("delete from Atributo where id_fragmento=" + idFragmentoOriginal);//solo deberia ser PK
                xamanaAcc.ejecutarComando("delete from Fragmento where id_fragmento=" + idFragmentoOriginal);
                xamanaAcc.desconectar();
            }

            //Recargar fragmentos en origen
            Socket socket2 = new Socket(accOrigen.ip, 5000);
            new DataOutputStream(socket2.getOutputStream()).writeBoolean(true);
            socket2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void eliminarOrigenPost(String idFragmentoOriginal) {
        AccesoDatosPG accOrigen = (AccesoDatosPG) this.origen;
        try {
            xamanaAcc = new AccesoDatosMy();
            if (accOrigen.conectar()) {
                accOrigen.ejecutarComando("drop table " + this.sTablaOrigen);
                accOrigen.desconectar();
            }
            //eliminar el fragmento en xamana
            if (this.xamanaAcc.conectar()) {
                xamanaAcc.ejecutarComando("delete from Atributo where id_fragmento=" + idFragmentoOriginal);//solo deberia ser PK
                xamanaAcc.ejecutarComando("delete from Fragmento where id_fragmento=" + idFragmentoOriginal);
                xamanaAcc.desconectar();
            }

            //Recargar fragmentos en origen
            Socket socket2 = new Socket(accOrigen.ip, 5000);
            new DataOutputStream(socket2.getOutputStream()).writeBoolean(true);
            socket2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void eliminarOrigenMongo(String idFragmentoOriginal) {
        AccesoDatosMG accOrigen = (AccesoDatosMG) this.origen;
        try {
            if (accOrigen.conectar()) {
                accOrigen.getMongoClient().getDatabase(this.datosBD.getNombd()).getCollection(sTablaOrigen).drop();
            }
            xamanaAcc = new AccesoDatosMy();
            //eliminar el fragmento en xamana
            if (this.xamanaAcc.conectar()) {
                xamanaAcc.ejecutarComando("delete from Atributo where id_fragmento=" + idFragmentoOriginal);//solo deberia ser PK
                xamanaAcc.ejecutarComando("delete from Fragmento where id_fragmento=" + idFragmentoOriginal);
                xamanaAcc.desconectar();
            }

            //Recargar fragmentos en origen
            Socket socket2 = new Socket(accOrigen.ip, 5000);
            new DataOutputStream(socket2.getOutputStream()).writeBoolean(true);
            socket2.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
