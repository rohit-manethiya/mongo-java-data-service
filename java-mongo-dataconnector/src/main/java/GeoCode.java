import com.google.gson.Gson;

public class GeoCode {
    private long id;
    private double latitude;
    private double longitude;

    public long getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = Long.valueOf(id);
    }

    public double getLatitude() {
        return this.latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = Double.valueOf(latitude);
    }

    public double getLongitude() {
        return this.latitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = Double.valueOf(longitude);
    }
}
