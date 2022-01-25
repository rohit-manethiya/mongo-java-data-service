import com.mongodb.client.MongoDatabase;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.DBCollection;
import com.mongodb.DB;
import com.mongodb.WriteResult;
import com.mongodb.DBCursor;



public class MongoDbClient {

    DB database;

    public DB createConnection() {
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        // Creating Credentials
        MongoCredential credential;
        credential =
                MongoCredential.createCredential("sampleUser", "myDb", "password".toCharArray());
        System.out.println("Connected to the database successfully");

        // Accessing the database
        DB database = mongoClient.getDB("myDb");
        System.out.println("Credentials ::" + credential);
        return database;
    }

    public void insertIntoDb(DB database) {
        Properties properties = createProperties();
        DBObject doc = createDBObject(properties);
        try {
            DBCollection collection = database.getCollection("Properties");
            // WriteResult result = collection.insert(doc);

            /*
             * System.out.println(result.getUpsertedId()); System.out.println(result.getN());
             * System.out.println(result.isUpdateOfExisting());
             */

            DBObject query = BasicDBObjectBuilder.start().add("_id", "12345").get();
            DBCursor cursor = collection.find(query);
            while (cursor.hasNext()) {
                System.out.println(cursor.next());
            }

        } catch (Exception e) {
            System.out.println(e);
        }


    }

    private static DBObject createDBObject(Properties properties) {
        BasicDBObjectBuilder docBuilder = BasicDBObjectBuilder.start();

        docBuilder.append("_id", "12345");
        return docBuilder.get();
    }

    private static Properties createProperties() {
        Properties p = new Properties();
        p.setId(2);
        return p;
    }

}
