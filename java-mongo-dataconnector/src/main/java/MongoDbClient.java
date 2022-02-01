import com.mongodb.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class MongoDbClient {

    private DB connection;

    MongoDbClient(){
        this.connection = this.createConnection();
    }
    private DB createConnection() {
        MongoClient mongoClient = new MongoClient(Constants.hostname, Constants.port);
        // Creating Credentials
        MongoCredential credential;
        credential =
                MongoCredential.createCredential(Constants.username, Constants.dbName, Constants.pwd.toCharArray());
        System.out.println("Connected to the database successfully");

        // Accessing the database
        DB database = mongoClient.getDB(Constants.dbName);
        return database;
    }

    public void insertIntoDb(String CollectionName, DBEntity entity) {
        try {
            DBCollection collection = this.connection.getCollection(CollectionName);
            WriteResult result = collection.insert(entity.createDBObject());

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void insertIntoDb(String CollectionName, BasicDBObject entity) {
        try {
            DBCollection collection = this.connection.getCollection(CollectionName);
            WriteResult result = collection.insert(entity);
            System.out.println(result.getUpsertedId());
            System.out.println(result.getN());
            System.out.println(result.isUpdateOfExisting());

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void insertIntoDb(String CollectionName, List<DBEntity> entity) {
        try {
            DBCollection collection = this.connection.getCollection(CollectionName);
            BulkWriteOperation writeOperation = collection.initializeUnorderedBulkOperation();
            for(DBEntity obj: entity) {
                writeOperation.insert(obj.createDBObject());
            }
            BulkWriteResult result = writeOperation.execute();
            System.out.println(result);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public DBEntity getEntity(String collectionName, String id, String targetEntityName) {
        DBObject query = BasicDBObjectBuilder.start().add("_id", id).get();
        DBCollection collection = this.connection.getCollection(collectionName);
        DBCursor cursor = collection.find(query);
        DBEntity dbEntity = null;
        if (cursor.hasNext()) {
            DBObject obj = cursor.next();
            try {
                Class<?> clazz = Class.forName(targetEntityName);
                Constructor<?> constructor = clazz.getConstructor();
                dbEntity = ((DBEntity) constructor.newInstance()).fromDBObject(obj);
            } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

        }

        return dbEntity;
    }

    public  DBObject getDBObject(String collectionName, String key) {
        DBObject query = BasicDBObjectBuilder.start().add("name", key).get();
        DBCollection collection = this.connection.getCollection(collectionName);
        DBCursor cursor = collection.find(query);
        DBEntity dbEntity = null;
        if (cursor.hasNext()) {
            DBObject obj = cursor.next();
            return obj;
        }
        return null;
    }
}
