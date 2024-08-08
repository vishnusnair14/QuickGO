package com.vishnu.quickgoorder.eventbus;

public class EBDeliveryAddressData {
    public double addr_lat;
    public double addr_lon;
    public String addr_city;
    public String addr_ph_key;
    public String pincode;
    public String addr_full;

    public EBDeliveryAddressData(double addr_lat, double addr_lon, String addr_city, String addr_ph_key, String pincode, String addr_full) {
        this.addr_lat = addr_lat;
        this.addr_lon = addr_lon;
        this.addr_city = addr_city;
        this.addr_ph_key = addr_ph_key;
        this.pincode = pincode;
        this.addr_full=addr_full;
    }
}
