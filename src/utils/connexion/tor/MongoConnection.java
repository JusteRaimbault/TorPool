package utils.connexion.tor;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;

public class MongoConnection {

    public static MongoClient mongoClient;
    public static MongoDatabase mongoDatabase;


    public static void initMongo(String host, int port, String db) {
        try {
            mongoClient = new MongoClient( host , port );
            mongoDatabase = mongoClient.getDatabase(db);
        } catch(Exception e){
            System.out.println("No mongo connection possible : ");
            e.printStackTrace();
        }
    }

    public static void initMongo(String db) {
        initMongo("127.0.0.1",27017,db);
    }
    public static void initMongo(){initMongo(Context.mongoDBName);}

    public static void closeMongo() {
        try{
            mongoClient.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static String getPortFromMongo(boolean exclusivity){
        initMongo();
        MongoCollection<Document> collection = mongoDatabase.getCollection(Context.mongoDBCollection);
        String res = "";
        if(exclusivity){
            Document d = collection.findOneAndDelete(exists("port"));
            if(d.containsKey("port")){res = d.getString("port");}
        }else{
            FindIterable fi =collection.find();
            if(fi.iterator().hasNext()){
                Document d = (Document) fi.iterator().next();
                if(d.containsKey("port")){res = d.getString("port");}
            }
        }
        closeMongo();
        return(res);
    }

    public static void deletePortInMongo(String port){
        initMongo();
        MongoCollection<Document> collection = mongoDatabase.getCollection(Context.mongoDBCollection);
        collection.findOneAndDelete(eq("port",port));
        closeMongo();
    }

    public static void writePortInMongo(String port){
        initMongo();
        MongoCollection<Document> collection = mongoDatabase.getCollection(Context.mongoDBCollection);
        // write only if not here ? => it shouldnt be anyway
        FindIterable fi = collection.find(eq("port",port));
        if(!fi.iterator().hasNext()){
            collection.insertOne(new Document("port",port));
        }
        closeMongo();
    }


}
