import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

public class Property implements DBEntity{
    private String id;
    private GeoCode geoCode;

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GeoCode getGeoCode() {
        return this.geoCode;
    }

    public void setGeoCode(GeoCode geoCode) {
        this.geoCode = geoCode;
    }

    public DBObject createDBObject() {
        BasicDBObjectBuilder docBuilder = BasicDBObjectBuilder.start();
        Gson gson = new Gson();
        docBuilder.append("_id", this.getId());
        docBuilder.append("geoCode", gson.toJson(this.getGeoCode()));
        return docBuilder.get();
    }

    public Property fromDBObject(DBObject dbobj) {
        DBObject geoCodeObj = (DBObject)dbobj.get("geoCode");
        GeoCode geoCode = new GeoCode();
        geoCode.setId((String)geoCodeObj.get("id"));
        geoCode.setLatitude((String)geoCodeObj.get("latitude") );
        geoCode.setLatitude((String)geoCodeObj.get("longitude") );
        this.setId(String.valueOf((String)dbobj.get("_id")));
        this.setGeoCode(geoCode);
        return this;
    }

    public Property fromJsonObject(JsonObject propertyJson) {
        this.setId(propertyJson.get("propertyId").getAsString());
        GeoCode geoCode = new GeoCode();
        JsonElement jegeo = propertyJson.get(Constants.geoCode);
        JsonObject geoCodeJson = jegeo != null ? jegeo.getAsJsonObject() : null;
        if(geoCodeJson == null) {
            this.setGeoCode(null);
            return null;
        }
        geoCode.setId(geoCodeJson.get("id").getAsString());
        geoCode.setLatitude(geoCodeJson.get("latitude").getAsString());
        geoCode.setLongitude(geoCodeJson.get("longitude").getAsString());
        this.setGeoCode(geoCode);
        return this;
    }
}
