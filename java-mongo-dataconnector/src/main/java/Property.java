import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import java.util.HashMap;
import java.util.Map;

public class Property implements DBEntity{
    private String id;
    private String name;
    private GeoCode geoCode;
    private Map<String, String> descriptions = new HashMap<>();

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GeoCode getGeoCode() {
        return this.geoCode;
    }

    public void setGeoCode(GeoCode geoCode) {
        this.geoCode = geoCode;
    }

    public Map<String, String> getDescriptions() {
        return this.descriptions;
    }

    public void setDescriptions(Map<String, String> descriptions) {
        this.descriptions = descriptions;
    }

    public DBObject createDBObject() {
        BasicDBObjectBuilder docBuilder = BasicDBObjectBuilder.start();
        Gson gson = new Gson();
        docBuilder.append("_id", this.getId());
        BasicDBObject geo= BasicDBObject.parse(gson.toJson(this.getGeoCode()));
        docBuilder.append("geoCode", geo);
        BasicDBObject descriptions = BasicDBObject.parse(gson.toJson(this.getDescriptions()));
        docBuilder.append("descriptions", descriptions);
        docBuilder.append("name", this.getName());
        return docBuilder.get();
    }

    public Property fromDBObject(DBObject dbobj) {
        Gson gson = new Gson();
        GeoCode geoCode  = gson.fromJson(String.valueOf(dbobj.get("geoCode")), GeoCode.class);
        this.setId(String.valueOf(dbobj.get("_id")));
        this.setGeoCode(geoCode);
        this.setName(String.valueOf(dbobj.get("name")));
        Map<String, String> descriptions = gson.fromJson(String.valueOf(dbobj.get("descriptions")), HashMap.class);
        this.setDescriptions(descriptions);
        return this;
    }

    public Property fromJsonObject(JsonObject propertyJson) {
        this.setId(propertyJson.get("propertyId").getAsString());
        this.setName(propertyJson.get("name").getAsString());
        GeoCode geoCode = new GeoCode();
        JsonElement jegeo = propertyJson.get(Constants.geoCode);
        JsonObject geoCodeJson = jegeo != null ? jegeo.getAsJsonObject() : null;
        if(geoCodeJson == null) {
            this.setGeoCode(null);
            return this;
        }
        geoCode.setLatitude(geoCodeJson.get("latitude").getAsString());
        geoCode.setLongitude(geoCodeJson.get("longitude").getAsString());
        this.setGeoCode(geoCode);
        if(! propertyJson.get("descriptions").isJsonNull() &&
                ! propertyJson.get("descriptions").getAsJsonArray().isJsonNull()) {
            JsonArray descriptions = propertyJson.get("descriptions").getAsJsonArray();
            int i=0;
            JsonElement desc = descriptions.get(i);
            while(!desc.isJsonNull()) {
                JsonObject descJson = desc.getAsJsonObject();
                this.getDescriptions().put(descJson.get("name").getAsString(), descJson.get("text").getAsString());
            }
        }
        return this;
    }
}
