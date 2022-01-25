import com.mongodb.client.MongoDatabase;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;

/**
 * App
 */
public class App {
    public static void main(String[] args) {
        try {
            MongoDbClient client = new MongoDbClient();
            // client.createConnection();

            client.insertIntoDb(client.createConnection());

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void generateToken() {

    }
}
