/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package verticaldin;

import datos.AccesoDatos;
import datos.AccesoDatosMG;
import datos.AccesoDatosMy;
import datos.AccesoDatosPG;
import java.util.ArrayList;
import modelo.Atributo;
import modelo.BD;
import modelo.Tabla;

/**
 *
 * @author pipe
 */
public class Fragmentador {

    private AccesoDatos origen;
    private AccesoDatos destino;
    private ArrayList<Atributo> atts;
    private BD datosBD;
    int nFragmento;

    public Fragmentador(AccesoDatos origen, AccesoDatos destino, ArrayList<Atributo> atts, BD datosBD, int nFragmento) {
        this.setOrigen(origen);
        this.setDestino(destino);
        this.setAtts(atts);
        this.datosBD = datosBD;
        this.nFragmento = nFragmento;
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
                arr = accOrigen.ejecutarConsulta("select " + att_nombre + " from " + this.datosBD.getTabla().getNombre());
                accOrigen.desconectar();
            }
            if (!this.isNullorEmpty(arr)) {
                //Crear nueva tabla
                if (accDestino.conectar()) {
                    accDestino.ejecutarComando("create table " + this.datosBD.getTabla().getNombre() + "_f" + this.nFragmento + " (" + att_tipo + ", PRIMARY KEY (" + this.datosBD.getTabla().getAtributoLlave().getNombre() + "));");
                    //agregar datos
                    for (int i = 0; i < arr.size(); i++) {
                        String valores = "";
                        for (int j=0; j < ((ArrayList) arr.get(0)).size(); j++) {
                            if (this.transTipoDato(this.getAtts().get(j).getTipo()) == 0 || this.transTipoDato(this.getAtts().get(j).getTipo()) == 2) {
                                valores += "\"" + ((ArrayList) arr.get(i)).get(j) + "\", ";
                            } else {
                                valores += ((ArrayList) arr.get(i)).get(j) + ", ";
                            }
                            valores = valores.substring(0, valores.length() - 2);
                            String q = "INSERT INTO " + this.datosBD.getTabla().getNombre() + "_f" + this.nFragmento+ " VALUES (" + valores + ");";
                            accDestino.ejecutarComando(q);
                        }
                    }
                    accDestino.desconectar();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generarFragmentoPost() {
        AccesoDatosPG accOrigen = (AccesoDatosPG) this.origen;
        AccesoDatosPG accDestino = (AccesoDatosPG) this.destino;
        try {
            if (accOrigen.conectar()) {
                accOrigen.desconectar();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generarFragmentoMongo() {
        AccesoDatosMG accOrigen = (AccesoDatosMG) this.origen;
        AccesoDatosMG accDestino = (AccesoDatosMG) this.destino;
        try {
            if (accOrigen.conectar()) {
                accOrigen.desconectar();
            }
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
}
