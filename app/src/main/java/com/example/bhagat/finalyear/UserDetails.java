package com.example.bhagat.finalyear;

/**
 * Created by bhagat on 10/25/16.
 */
public class UserDetails {
    private UserDetails() {
    }
    private static UserDetails ourInstance = new UserDetails();
    public String url = "http://54.190.31.30/ServeEasyPHP/";
    public String consumerId = "123";
    public String providerId = "500";
    public String userName = "Bhaskar";
    public String userId = "1";
    public String latitude = "1.1";
    public String longitude = "1.1";
    public boolean isProvider = true;
    public String radialDistance = "100";
    public static UserDetails getInstance() {
        return ourInstance;
    }
}