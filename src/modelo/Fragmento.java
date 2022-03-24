/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modelo;

import datos.AccesoDatosMy;
import java.util.ArrayList;

/**
 *
 * @author pipe
 */
public class Fragmento {
    private String id_fragmento;
    private String nombre;
    private double noperaciones;
    private double ndesempenio;
    private boolean cbir;
    private ArrayList<Atributo> atributos;

    /**
     * @return the nombre
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre the nombre to set
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * @return the noperaciones
     */
    public double getNoperaciones() {
        return noperaciones;
    }

    /**
     * @param noperaciones the noperaciones to set
     */
    public void setNoperaciones(double noperaciones) {
        this.noperaciones = noperaciones;
    }

    /**
     * @return the ndesempenio
     */
    public double getNdesempenio() {
        return ndesempenio;
    }

    /**
     * @param ndesempenio the ndesempenio to set
     */
    public void setNdesempenio(double ndesempenio) {
        this.ndesempenio = ndesempenio;
    }

    /**
     * @return the cbir
     */
    public boolean isCbir() {
        return cbir;
    }

    /**
     * @param cbir the cbir to set
     */
    public void setCbir(boolean cbir) {
        this.cbir = cbir;
    }

    /**
     * @return the id_fragmento
     */
    public String getId_fragmento() {
        return id_fragmento;
    }

    /**
     * @param id_fragmento the id_fragmento to set
     */
    public void setId_fragmento(String id_fragmento) {
        this.id_fragmento = id_fragmento;
    }
    
    public ArrayList<Atributo> obtenerAtributos(){
        ArrayList<Atributo> res=new ArrayList<>();
        try{
            AccesoDatosMy acc=new AccesoDatosMy();
            ArrayList arr=null;    
            if(acc.conectar()){
                arr=acc.ejecutarConsulta("select nom, tipo, size, descriptor, multimedia from atributo where id_fragmento="+this.id_fragmento);
                acc.desconectar();
            }
            if(arr!=null){
                for(int i=0;i<arr.size();i++){
                    Atributo att=new Atributo();
                    att.setNombre(""+((ArrayList)arr.get(i)).get(0));
                    att.setTipo(""+((ArrayList)arr.get(i)).get(1));
                    att.setTamanio(""+((ArrayList)arr.get(i)).get(2));
                    att.setDescriptor(Boolean.parseBoolean(""+((ArrayList)arr.get(i)).get(3)));
                    att.setMultimedia(Boolean.parseBoolean(""+((ArrayList)arr.get(i)).get(4)));
                    res.add(att);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return res;
    }

    /**
     * @return the atributos
     */
    public ArrayList<Atributo> getAtributos() {
        return atributos;
    }

    /**
     * @param atributos the atributos to set
     */
    public void setAtributos(ArrayList<Atributo> atributos) {
        this.atributos = atributos;
    }
    
}
