import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import spark.Request;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static spark.Spark.*;

/**
 * App
 */
public class App {
    public static String token;

    public static void main(String[] args) {
        try {
            String ttoken = getToken(false, "null");
            App.token = ttoken;
            MongoDbClient client = new MongoDbClient();
            put("/properties", (req, res) -> populateMongo(args, client, req));
            get("/properties", (req, res) -> queryMongo(client, req));
            post("/notifications", (req, res) -> interceptIncomingWebhook(client, req));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static JsonArray queryMongo(MongoDbClient client, Request req) {
        Gson gson = new Gson();
        JsonObject queryBodyGson  = gson.fromJson(req.body(), JsonObject.class);
        String username = req.headers("username");
        String pwd = req.headers("password");
        if(!Constants.sparkUsername.equals(username) || !Constants.sparkpwd.equals(pwd)) {
            return gson.fromJson("[{\"401\": \"User Unauthorized\"}]", JsonArray.class);
        }
        String latitude = queryBodyGson.get("latitude").getAsString();
        String longitude = queryBodyGson.get("longitude").getAsString();
        Double radius = Double.valueOf(queryBodyGson.get("radius").getAsString());

        // "db.properties.aggregate([{$geoNear:{near:{type:\"geoCode\",coordinates:[ 118.345971, 29.714212 ]},distanceField: \"propertyDistance\",maxDistance:10000,spherical: true}}]).pretty()";

        Property property = new Property();
        List<DBEntity> dbObjs = client.aggregateDBObjs(Constants.collectionName, latitude, longitude, radius, property.getClass().getName());
        List<Property> propertyList = new ArrayList<>();
        for(DBEntity entity: dbObjs) {
            propertyList.add((Property) entity);
        }

        return gson.fromJson(gson.toJson(propertyList), JsonArray.class);
        // Property entity = (Property) client.getEntity(Constants.collectionName, property.getId(), property.getClass().getName());
    }

    private static String interceptIncomingWebhook(MongoDbClient mongoDbClient, Request req) {
        /**
         * {"id":0,"entityName":"TestEntity","entityId":123,"updateTime":1646409385708}
         */
        try {
            Gson gson = new Gson();
            JsonObject reqBody = gson.fromJson(req.body(), JsonObject.class);
            Long id = reqBody.get("id").getAsLong();
            String entityName = reqBody.get("entityName").getAsString();
            String entityId = reqBody.get("entityId").getAsString();
            List<DBEntity> propertyList = new ArrayList<>();
            if(entityId != null) {
                try {
                    token = fetchSingleProperty(token, gson, propertyList, entityId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (propertyList != null && propertyList.size() == 1) {
                    mongoDbClient.insertIntoDb(Constants.collectionName, propertyList.get(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "success";
    }

    private static String populateMongo(String[] args, MongoDbClient client, Request req) {
        String username = req.headers("username");
        String pwd = req.headers("password");
        if(!Constants.sparkUsername.equals(username) || !Constants.sparkpwd.equals(pwd)) {
            return "401 Unauthorized";
        }
        JsonObject queryBodyGson = new Gson().fromJson(req.body(), JsonObject.class);
        Integer startPage = Integer.valueOf(queryBodyGson.get("startPage").getAsString());
        Integer endPage = Integer.valueOf(queryBodyGson.get("endPage").getAsString());
        for (int i = startPage; i <= endPage; i++) {
            final int itr = i;
            System.out.println("pageNo: " + i);
            CompletableFuture.runAsync(() -> {
                try {
                    processPage(App.token, itr, client);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        return "Success";
    }

    private static void processPage(String token, int i, MongoDbClient mongoDbClient) throws IOException {
        URL url = new URL(Constants.baseurl + Constants.propertiesEndpoint + Constants.paging +
                String.format("?page=%d&size=100", i));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + token);

        int respCode = conn.getResponseCode();
        if (respCode == 401) {
            conn.disconnect();
            token = getToken(true, token);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            respCode = conn.getResponseCode();
        }
        if (respCode != 200) {
            conn.disconnect();
            throw new RuntimeException("Failed : HTTP error code : "
                    + respCode);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));

        Gson gson = new Gson();
        JsonObject propertiesListResp = gson.fromJson(br, JsonObject.class);
        conn.disconnect();
        br.close();
        JsonArray propertiesList = propertiesListResp.getAsJsonArray("result");
        List<Map<String, String>> propMetaMapss = new ArrayList<>();
        for (JsonElement jelement : propertiesList) {
            JsonObject propmeta = jelement.getAsJsonObject();
            Map<String, String> propmetamap = new HashMap<>();
            propmetamap.put("id", propmeta.get("id").getAsString());
            JsonObject supplier = propmeta.get("supplier").getAsJsonObject();
            propmetamap.put("supplierId", supplier.get("id").getAsString());
            propMetaMapss.add(propmetamap);
        }
        persistProperties(token, mongoDbClient, gson, propMetaMapss);
    }

    private static void persistProperties(String token, MongoDbClient mongoDbClient, Gson gson, List<Map<String, String>> propMetaMapss) throws IOException {
        List<DBEntity> propertyList = new ArrayList<>();
        for (Map<String, String> propmetamap : propMetaMapss) {
            token = fetchSingleProperty(token, gson, propertyList, propmetamap.get("id"));
            continue;
        }
        if (propertyList != null && propertyList.size() > 0) {
            mongoDbClient.insertIntoDb(Constants.collectionName, propertyList);
        }
    }

    private static String fetchSingleProperty(String token, Gson gson, List<DBEntity> propertyList, String id) throws IOException {
        BufferedReader br;
        URL url;
        HttpURLConnection conn;
        url = new URL(Constants.baseurl + Constants.propertiesEndpoint + Constants.info + "/" + id);
        DBEntity property = extractPropertyInfo(token, gson, propertyList, id, url);
        if(property != null) {
            url = new URL(Constants.baseurl + Constants.propertiesEndpoint + "/" + id);
            return extractAdditionalPropertyInfo(token, gson, property, id, url);
        }
        return token;
    }

    private static String extractAdditionalPropertyInfo(String token, Gson gson, DBEntity property, String id, URL url) throws IOException {
        HttpURLConnection conn;
        BufferedReader br;
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        int respCode2 = conn.getResponseCode();
        if (respCode2 == 401) {
            conn.disconnect();
            token = getToken(true, token);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            respCode2 = conn.getResponseCode();
        }
        if (respCode2 == 422) {
            conn.disconnect();
            System.out.println("Skipping property without property info: " + id);
            return token;
        }
        if (respCode2 != 200) {
            conn.disconnect();
            throw new RuntimeException("Failed : HTTP error code : "
                    + respCode2);
        }
        br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));

        JsonObject propertyJson = gson.fromJson(br, JsonObject.class);
        br.close();
        conn.disconnect();
        if (!"success".equals(propertyJson.get("status").getAsString())) {
            return token;
        }
        property.addExtraDetails(propertyJson.getAsJsonObject("result"));
        return token;
    }

    private static DBEntity extractPropertyInfo(String token, Gson gson, List<DBEntity> propertyList, String id, URL url) throws IOException {
        HttpURLConnection conn;
        BufferedReader br;
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + token);
        int respCode2 = conn.getResponseCode();
        if (respCode2 == 401) {
            conn.disconnect();
            token = getToken(true, token);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            respCode2 = conn.getResponseCode();
        }
        if (respCode2 == 422) {
            conn.disconnect();
            System.out.println("Skipping property without property info: " + id);
            return null;
        }
        if (respCode2 != 200) {
            conn.disconnect();
            throw new RuntimeException("Failed : HTTP error code : "
                    + respCode2);
        }
        br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));

        JsonObject propertyJson = gson.fromJson(br, JsonObject.class);
        br.close();
        conn.disconnect();
        if (!"success".equals(propertyJson.get("status").getAsString())) {
            return null;
        }
        Property property = new Property();
        property.fromJsonObject(propertyJson.getAsJsonObject("result"));
        if (property.getGeoCode() != null) {
            propertyList.add(property);
            return property;
        }
        return null;
    }

    private static String getToken(boolean hardfetch, String oldToken) throws IOException {
        String filePath = System.getProperty("user.dir") + "/" + Constants.tokenFile;
        File file = new File(filePath);
        Scanner fileReader = new Scanner(file);
        String token;
        if (fileReader.hasNextLine()) {
            token = fileReader.nextLine().trim();
            fileReader.close();
            if (hardfetch && token.equals(oldToken)) {
                new FileWriter(filePath, false).close();
            } else {
                return token;
            }
        }
        fileReader.close();
        URL url = new URL(Constants.baseurl + Constants.tokenEndpoint + String.format("?supplierId=%s&supplierSecret=%s", Constants.supplierId, Constants.supplierSecret));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            throw new RuntimeException("Failed : HTTP error code : "
                    + conn.getResponseCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (conn.getInputStream())));
        FileWriter fileWriter = new FileWriter(file.getName());

        JsonObject tokenObj = new Gson().fromJson(br, JsonObject.class);
        token = tokenObj.get("accessToken").getAsString();
        App.token = token;
        br.close();
        conn.disconnect();
        fileWriter.write(token);
        fileWriter.close();
        return App.token;
    }

}