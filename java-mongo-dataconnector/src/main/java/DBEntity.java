import com.google.gson.JsonObject;
import com.mongodb.DBObject;

public interface DBEntity {
    DBObject createDBObject();
    DBEntity fromDBObject(DBObject dbobj);
    DBEntity fromJsonObject(JsonObject jsonObject);
    DBEntity addExtraDetails(JsonObject jsonObject);
}
