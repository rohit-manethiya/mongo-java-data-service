public class GeoCode {
    private String type = "Point";
    private double[] coordinates = new double[2];

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return this.coordinates[1];
    }

    public void setLatitude(String latitude) {
        this.coordinates[1] = Double.valueOf(latitude);
    }

    public double getLongitude() {
        return this.coordinates[0];
    }

    public void setLongitude(String longitude) {
        this.coordinates[0] = Double.valueOf(longitude);
    }

    public void setCoordinates(double[] coordinates) {
        this.coordinates = coordinates;
    }

    public double[] getCoordinates() {
        return this.coordinates;
    }
}
