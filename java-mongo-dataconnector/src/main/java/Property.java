import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Property implements DBEntity{
    private String id;
    private String name;
    private GeoCode geoCode;
    private Map<String, String> descriptions = new HashMap<>();
    private List<String> addresses = new ArrayList<>();

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

    public List<String> getAddresses() {
        return this.addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
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
        docBuilder.append("addresses", this.getAddresses());
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
        List<String> addresses = gson.fromJson(String.valueOf(dbobj.get("addresses")), ArrayList.class);
        this.setAddresses(addresses);
        return this;
    }

    public Property fromJsonObject(JsonObject propertyInfoJson) {
        this.setId(propertyInfoJson.get("propertyId").getAsString());
        GeoCode geoCode = new GeoCode();
        JsonElement jegeo = propertyInfoJson.get(Constants.geoCode);
        JsonObject geoCodeJson = jegeo != null ? jegeo.getAsJsonObject() : null;
        if(geoCodeJson == null) {
            this.setGeoCode(null);
            return this;
        }
        geoCode.setLatitude(geoCodeJson.get("latitude").getAsString());
        geoCode.setLongitude(geoCodeJson.get("longitude").getAsString());
        this.setGeoCode(geoCode);

        if(propertyInfoJson.get("addresses") != null && propertyInfoJson.get("addresses").getAsJsonArray() != null) {
            JsonArray addresses = propertyInfoJson.get("addresses").getAsJsonArray();
            int i = 0;
            List<String> adds = new ArrayList<>();
            while (i < addresses.size()) {
                JsonElement addressEle = addresses.get(i);
                i++;
                JsonObject address = addressEle.getAsJsonObject();
                if(address != null) {
                    String addressLine = address.get("addressLine") != null ? address.get("addressLine").getAsString() : "";
                    String cityName = address.get("cityName") != null ? address.get("cityName").getAsString() : "";
                    String postalCode = address.get("postalCode") != null ? address.get("postalCode").getAsString() : "";
                    String country = address.get("country") != null ? address.get("country").getAsJsonObject().get("codeA3").getAsString() : "";
                    adds.add(addressLine + "\n" + cityName + "\n" + country + "\nPostal Code: " + postalCode);
                }
            }
            this.setAddresses(adds);
        }
        return this;
    }

    public Property addExtraDetails(JsonObject propJson) {
        this.setName(propJson.get("name") != null ? propJson.get("name").getAsString() : null);
        if(propJson.get("descriptions") != null &&
                propJson.get("descriptions").getAsJsonArray() != null) {
            JsonArray descriptions = propJson.get("descriptions").getAsJsonArray();
            int i=0;
            while(i<descriptions.size()) {
                JsonElement desc = descriptions.get(i);
                JsonObject descJson = desc.getAsJsonObject();
                if(descJson != null) {
                    JsonObject descType = descJson.getAsJsonObject("descriptionType");
                    if (descType != null) {
                        this.getDescriptions().put(descType.get("name").getAsString(), descJson.get("text").getAsString());
                    }
                }
                i++;
            }
        }
        return this;
    }

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
