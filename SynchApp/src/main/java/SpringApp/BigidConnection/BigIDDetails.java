package SpringApp.BigidConnection;

import lombok.Data;

@Data
public class BigIDDetails {

    private String bigIDUrl;
    private String dsConnectionEndpoint;
    private String idConnectionEndpoint;
    private String bigIDToken;
}
