import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * App
 */
public class App {
    public static void main(String[] args) {
        try {
            String token = getToken(false);
            MongoDbClient client = new MongoDbClient();
            int startPage = Integer.valueOf(args[0]);
            int totalPages = Integer.valueOf(args[1]);
            Map<String, Boolean> falseSuppliers = new HashMap<>();
            System.out.println(startPage);
            System.out.println(totalPages);
            for(int i=totalPages; i>startPage; i--) {
                processPage(token, i, client, falseSuppliers);
            }
//            client.getEntity(Constants.collectionName, property.getId(), property.getClass().getName());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void processPage(String token, int i, MongoDbClient mongoDbClient, Map<String, Boolean> falseSuppliers) throws IOException {
        URL url = new URL(Constants.baseurl+Constants.propertiesEndpoint+Constants.paging+
                String.format("?page=%d&size=100", i));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer "+ token);

        int respCode = conn.getResponseCode();
        if(respCode == 401) {
            conn.disconnect();
            token = getToken(true);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer "+ token);
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
        for(JsonElement jelement:propertiesList) {
            JsonObject propmeta = jelement.getAsJsonObject();
            Map<String, String> propmetamap = new HashMap<>();
            propmetamap.put("id", propmeta.get("id").getAsString());
            JsonObject supplier = propmeta.get("supplier").getAsJsonObject();
            propmetamap.put("supplierId", supplier.get("id").getAsString());
            propMetaMapss.add(propmetamap);
        }
        List<DBEntity> propertyList = new ArrayList<DBEntity>();
        for(Map<String, String> propmetamap:propMetaMapss) {
            String id = propmetamap.get("id");
            String supplierId = propmetamap.get("supplierId");
            if(falseSuppliers.get(supplierId) != null) {
                continue;
            }
            url = new URL(Constants.baseurl+Constants.propertiesEndpoint+Constants.info+"/"+id);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer "+ token);
            int respCode2 = conn.getResponseCode();
            if(respCode2 == 422) {
                falseSuppliers.put(supplierId, true);
                conn.disconnect();
                System.out.println("Skipping supplierId without property info: "+supplierId);
                continue;
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
            if(!"success".equals(propertyJson.get("status").getAsString())) {
                continue;
            }
            Property property = new Property();
            property.fromJsonObject(propertyJson.getAsJsonObject("result"));
            if(property.getGeoCode() != null) {
                propertyList.add(property);
            }
        }
        if(propertyList != null) {
            mongoDbClient.insertIntoDb(Constants.collectionName, propertyList);
        }
    }

    private static String getToken(boolean hardfetch) throws IOException {
        String filePath = System.getProperty("user.dir") + "/" +Constants.tokenFile;
        File file = new File(filePath);
        if(hardfetch) {
            new FileWriter(filePath, false).close();
        }
        Scanner fileReader = new Scanner(file);
        String token;
        if(fileReader.hasNextLine()) {
            token = fileReader.nextLine().trim();
            fileReader.close();
            return token;
        }
        fileReader.close();
        URL url = new URL(Constants.baseurl+Constants.tokenEndpoint+String.format("?supplierId=%s&supplierSecret=%s", Constants.supplierId, Constants.supplierSecret));
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
        br.close();
        conn.disconnect();
        fileWriter.write(token);
        fileWriter.close();
        return token;
    }

}
