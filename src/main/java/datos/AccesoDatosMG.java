package datos;

import java.io.Serializable;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Aggregates.project;
import com.mongodb.client.model.Projections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author pipe
 */
public class AccesoDatosMG extends AccesoDatos implements Serializable {

    public String sUrl = null;  // sUrl de conexión
    public String ip = null;
    public String sUsr = null; // Usuario
    public String sPwd = null;  // Contraseña
    public String tabla = null;
    public String nombd = null;
    private MongoClient mongoClient;  // La conexión

    public AccesoDatosMG(String url, String nombd, String tabla, String usr, String pwd) throws Exception {
        ip = url.split(":")[0];
        sUrl = url;//"jdbc:mysql://localhost/Master";
        sUsr = usr;//"root";
        sPwd = pwd;//"sunny_4teamo";
        this.nombd = nombd;
        this.setTabla(tabla);
    }

    /**
     * @return la base de datos con la que se construyó esta conexión (la
     * propia del sitio registrado, no necesariamente la del sitio local)
     */
    public String getNombd() {
        return nombd;
    }

    @Override
    public boolean conectar() throws Exception {
        String mongoClientURI = "";
        if (sUsr.isEmpty()) {
            mongoClientURI = "mongodb://" + sUrl + "/";
        } else {
            mongoClientURI = "mongodb://" + sUsr + ":" + sPwd + "@" + sUrl + "/";
        }
        setMongoClient(MongoClients.create(mongoClientURI));

        return getMongoClient() != null;
    }

    /**
     * @return the tabla
     */
    public String getTabla() {
        return tabla;
    }

    /**
     * @param tabla the tabla to set
     */
    public void setTabla(String tabla) {
        this.tabla = tabla;
    }

    @Override
    public synchronized ArrayList<String> atributos(String nombreBase, String nombre) {
        ArrayList<String> res=new ArrayList<>();
        MongoDatabase db = this.getMongoClient().getDatabase(nombreBase);
        MongoCollection<Document> collection = db.getCollection(nombre);
        Document doc = collection.find().first();
        try {
            System.out.println(nombreBase+" "+nombre+" "+sUrl);
            Iterator js = new JSONObject(doc.toJson()).keys();
            while (js.hasNext()) {
                String key=js.next().toString();
                Bson group=project(Projections.computed(key, Document.parse("{$type: '$"+key+"'}")));
                List<Document> r=collection.aggregate(Arrays.asList(group)).into(new ArrayList<>());
                String tipo=""+(new JSONObject(r.get(0).toJson()).get(key));
                System.out.println(key+":"+tipo);
                res.add(key+":"+tipo);
            }
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    /**
     * @return the mongoClient
     */
    public MongoClient getMongoClient() {
        return mongoClient;
    }

    /**
     * @param mongoClient the mongoClient to set
     */
    public void setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public void desconectar() throws Exception {
        this.mongoClient.close();
        
    }
}
