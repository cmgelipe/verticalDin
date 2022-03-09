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

/**
 *
 * @author pipe
 */
public class Fragmentador {

    private AccesoDatos origen;
    private AccesoDatos destino;
    private ArrayList<Atributo> atts;
    private String tipoBD;
    private String nTabla;
    int nFragmento;

    public Fragmentador(AccesoDatos origen, AccesoDatos destino, ArrayList<Atributo> atts, String tipoBD, String nTabla, int nFragmento) {
        this.setOrigen(origen);
        this.setDestino(destino);
        this.setAtts(atts);
        this.setTipoBD(tipoBD);
        this.setnTabla(nTabla);
        this.nFragmento = nFragmento;
    }

    public boolean generarFragmentos() {
        if (this.getTipoBD().compareTo("a") == 0) {
            generarFragmentosMySQL();
        } else if (this.getTipoBD().compareTo("b") == 0 || this.getTipoBD().compareTo("c") == 0) {
            generarFragmentosPost();
        } else if (this.getTipoBD().compareTo("d") == 0) {
            generarFragmentosMongo();
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

    /**
     * @return the tipoBD
     */
    public String getTipoBD() {
        return tipoBD;
    }

    /**
     * @param tipoBD the tipoBD to set
     */
    public void setTipoBD(String tipoBD) {
        this.tipoBD = tipoBD;
    }

    private void generarFragmentosMySQL() {
        AccesoDatosMy accOrigen = (AccesoDatosMy) this.origen;
        AccesoDatosMy accDestino = (AccesoDatosMy) this.destino;
        try {
            //Crear nueva tabla
            String att_tipo = "";
            for (Atributo r : this.getAtts()) {
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
            if (accDestino.conectar()) {
                accDestino.ejecutarComando("create table " + this.getnTabla() + "_f" + this.nFragmento + " (" + att_tipo + ");");
                accDestino.desconectar();
            }
            if (accOrigen.conectar()) {
                accOrigen.ejecutarConsulta(tipoBD);
                accOrigen.desconectar();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void generarFragmentosPost() {
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

    private void generarFragmentosMongo() {
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

    /**
     * @return the nTabla
     */
    public String getnTabla() {
        return nTabla;
    }

    /**
     * @param nTabla the nTabla to set
     */
    public void setnTabla(String nTabla) {
        this.nTabla = nTabla;
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

}
