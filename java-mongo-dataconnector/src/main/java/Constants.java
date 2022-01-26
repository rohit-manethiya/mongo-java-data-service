public interface Constants {
    String collectionName = "properties";
    String dbName = "supplierDb";
    String username = "spradh";
    String pwd = "password";
    String hostname = "localhost";
    Integer port = 27017;
    String baseurl = "https://doc.supplier.roomdb.io";
    String tokenEndpoint = "/api/v1/suppliers/get-token";
    String propertiesEndpoint = "/api/v1/properties";
    String supplierId=System.getenv("supplierId");
    String supplierSecret=System.getenv("supplierSecret");
    String paging="/paging";
    String info="/info";
    String geoCode="geoCode";
    String tokenFile="roomdbtoken.txt";
}
