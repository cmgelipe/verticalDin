package modelo;

import modelo.Atributo;
import modelo.Fragmento;
import datos.AccesoDatosMy;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pipe
 */
public class Tabla {
    private String nombre;
    private int umbralOP;
    private int umbralCO;
    private String atributoLlave;
    private String idTabla;
    private ArrayList<Fragmento> fragmentos = null;
    private ArrayList<Atributo> atributos;
    AccesoDatosMy accesoXamana;

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
     * @return the umbralOP
     */
    public int getUmbralOP() {
        return umbralOP;
    }

    /**
     * @param umbralOP the umbralOP to set
     */
    public void setUmbralOP(int umbralOP) {
        this.umbralOP = umbralOP;
    }

    /**
     * @return the umbralCO
     */
    public int getUmbralCO() {
        return umbralCO;
    }

    /**
     * @param umbralCO the umbralCO to set
     */
    public void setUmbralCO(int umbralCO) {
        this.umbralCO = umbralCO;
    }

    /**
     * @return the atributoLlave
     */
    public String getAtributoLlave() {
        return atributoLlave;
    }

    /**
     * @param atributoLlave the atributoLlave to set
     */
    public void setAtributoLlave(String atributoLlave) {
        this.atributoLlave = atributoLlave;
    }

    /**
     * @return the idTabla
     */
    public String getIdTabla() {
        return idTabla;
    }

    /**
     * @param idTabla the idTabla to set
     */
    public void setIdTabla(String idTabla) {
        this.idTabla = idTabla;
    }
    
    public void obtenerAtributos(){
        try{
            AccesoDatosMy acc=new AccesoDatosMy();
            ArrayList arr=null;
            if(acc.conectar()){
                arr=acc.ejecutarConsulta("select nom, tipo, size, descriptor, multimedia from atributo "
                        + "join fragmento on fragmento.id_fragmento=atributo.id_fragmento "
                        + "where fragmento.id_tabla="+this.idTabla);
                acc.desconectar();
            }
            if(arr!=null){
                this.setAtributos(new ArrayList<>());
                for(int i=0;i<arr.size();i++){
                    Atributo att=new Atributo();
                    att.setNombre(""+((ArrayList)arr.get(i)).get(0));
                    att.setTipo(""+((ArrayList)arr.get(i)).get(1));
                    att.setTamanio(""+((ArrayList)arr.get(i)).get(2));
                    att.setDescriptor(Boolean.parseBoolean(""+((ArrayList)arr.get(i)).get(3)));
                    att.setMultimedia(Boolean.parseBoolean(""+((ArrayList)arr.get(i)).get(4)));
                    this.getAtributos().add(att);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public void llenaFragmentosxSitio(String ip) {
        try {
            accesoXamana = new AccesoDatosMy();
            ArrayList res = null;
            if (accesoXamana.conectar()) {
                //¿Que fragmentos estan en el sitio a donde el vigilante esta?
                res = accesoXamana.ejecutarConsulta("select nombre, noperaciones, ndesempenio, cbir from fragmento "
                        + "join sitio on sitio.id_sitio=fragmento.id_sitio where id_tabla=" + this.idTabla + " "
                        + "and direccion='" + ip + "'");
                accesoXamana.desconectar();
            } else {
                System.out.println("Sin conexión");
            }
            if (!isNullorEmpty(res)) {
                System.out.println("Fragmentos encontrados");
                setFragmentos(new ArrayList<>());
                for (Object tupla : res) {
                    Fragmento f = new Fragmento();
                    f.setNombre("" + ((ArrayList) tupla).get(0));
                    f.setNoperaciones(Double.parseDouble("" + ((ArrayList) tupla).get(1)));
                    f.setNdesempenio(Double.parseDouble("" + ((ArrayList) tupla).get(2)));
                    f.setCbir(Boolean.parseBoolean("" + ((ArrayList) tupla).get(3)));
                    getFragmentos().add(f);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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

    /**
     * @return the fragmentos
     */
    public ArrayList<Fragmento> getFragmentos() {
        return fragmentos;
    }

    /**
     * @param fragmentos the fragmentos to set
     */
    public void setFragmentos(ArrayList<Fragmento> fragmentos) {
        this.fragmentos = fragmentos;
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