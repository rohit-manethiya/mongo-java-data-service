import com.mongodb.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MongoDbClient {

    private DB db;
    private DB customDB;

    MongoDbClient(){
        this.db = this.createConnection();
        this.customDB = getCustomDBConnection();
    }

    private DB getCustomDBConnection() {
        MongoClientOptions mongoClientOptions = MongoClientOptions.builder()
                .codecRegistry(com.mongodb.MongoClient.getDefaultCodecRegistry()).build();

        MongoClient customClient= new MongoClient(Constants.hostname, mongoClientOptions);
        DB db = customClient.getDB(Constants.dbName);
        return db;
    }

    private DB createConnection() {
        MongoClient mongoClient = new MongoClient(Constants.hostname, Constants.port);
        // Creating Credentials
        MongoCredential.createCredential(Constants.username, Constants.dbName, Constants.pwd.toCharArray());

        System.out.println("Connected to the database successfully");

        // Accessing the database
        DB database = mongoClient.getDB(Constants.dbName);
        return database;
    }

    public void insertIntoDb(String CollectionName, DBEntity entity) {
        DBCollection collection = this.db.getCollection(CollectionName);
        try {
            WriteResult result = collection.insert(entity.createDBObject());
            System.out.println(result.getUpsertedId());
            System.out.println(result.getN());
            System.out.println(result.isUpdateOfExisting());

        } catch (DuplicateKeyException e) {
            System.out.println(e);
            System.out.println("Updating existing data");
            WriteResult result = collection.save(entity.createDBObject());
            System.out.println(result.getUpsertedId());
            System.out.println(result.getN());
            System.out.println(result.isUpdateOfExisting());
        }
    }

    public void insertIntoDb(String CollectionName, List<DBEntity> entity) {
        try {
            DBCollection collection = this.db.getCollection(CollectionName);
            BulkWriteOperation writeOperation = collection.initializeUnorderedBulkOperation();
            for(DBEntity obj: entity) {
                System.out.println(obj.toString());
                writeOperation.insert(obj.createDBObject());
            }
            BulkWriteResult result = writeOperation.execute();
            System.out.println(result);

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public List<DBEntity> aggregateDBObjs(String CollectionName, String lat, String lng, double radius, String targetEntityName){
        DBCollection collection = this.db.getCollection(CollectionName);
        BasicDBObject geoNear = new BasicDBObject();
        BasicDBObject near = new BasicDBObject();
        BasicDBObject geometry = new BasicDBObject();

        geoNear.put("$near", near);
    //    geoNear.put("distanceField", "propertyDistance");
        near.put("$maxDistance", radius);
   //     geoNear.put("spherical", true);

        near.put("$geometry", geometry);
        geometry.put("type", "Point");
        geometry.put("coordinates", Arrays.asList(Double.valueOf(lng), Double.valueOf(lat)));

        BasicDBObject argument = new BasicDBObject();
        argument.put("geoCode", geoNear);

//        Cursor cursor = collection.aggregate(
//                Arrays.asList(argument), AggregationOptions.builder().allowDiskUse(true)
//                                .bypassDocumentValidation(true)
//                                .maxTime(15000, TimeUnit.MILLISECONDS)
//                        .batchSize(50)
//                        .build());
        Cursor cursor = collection.find(argument);

        List<DBEntity> objectList = new ArrayList<>();
        while(cursor.hasNext()) {
            DBObject x = cursor.next();
                try {
                    Class<?> clazz = Class.forName(targetEntityName);
                    Constructor<?> constructor = clazz.getConstructor();
                    objectList.add(((DBEntity) constructor.newInstance()).fromDBObject(x));
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
        }
        return objectList;
    }

    public DBEntity getEntity(String collectionName, String id, String targetEntityName) {
        DBObject query = BasicDBObjectBuilder.start().add("_id", id).get();
        DBCollection collection = this.db.getCollection(collectionName);
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
        DBCollection collection = this.db.getCollection(collectionName);
        DBCursor cursor = collection.find(query);
        DBEntity dbEntity = null;
        if (cursor.hasNext()) {
            DBObject obj = cursor.next();
            return obj;
        }
        return null;
    }
}
