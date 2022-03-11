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
        this.datosBD=datosBD;
        this.nFragmento = nFragmento;
    }

    public boolean generarFragmentos() {
        if (this.datosBD.getTipoBD().compareTo("a") == 0) {
            generarFragmentosMySQL();
        } else if (this.datosBD.getTipoBD().compareTo("b") == 0 || this.datosBD.getTipoBD().compareTo("c") == 0) {
            generarFragmentosPost();
        } else if (this.datosBD.getTipoBD().compareTo("d") == 0) {
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

   

    private void generarFragmentosMySQL() {
        AccesoDatosMy accOrigen = (AccesoDatosMy) this.origen;
        AccesoDatosMy accDestino = (AccesoDatosMy) this.destino;
        try {
            //Crear nueva tabla
            this.datosBD.getTabla().setAtributoLlave(this.llaveMySQL(accOrigen,this.datosBD.getNombd(), this.datosBD.getTabla().getNombre()));
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
                accDestino.ejecutarComando("create table " + this.datosBD.getTabla().getNombre()+ "_f" + this.nFragmento + " (" + att_tipo + ");");
                accDestino.desconectar();
            }
            if (accOrigen.conectar()) {
                accOrigen.ejecutarConsulta("");
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
        }
        return att;
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
}
