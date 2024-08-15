package com.vishnu.quickgodelivery.miscellaneous;

public class StartDutyModel {

    String dp_name;
    String dp_id;
    double dp_lat;
    double dp_lon;

    public StartDutyModel(String dp_name, String dp_id, double dp_lat, double dp_lon) {
        this.dp_name = dp_name;
        this.dp_id = dp_id;
        this.dp_lat = dp_lat;
        this.dp_lon = dp_lon;
    }
}
