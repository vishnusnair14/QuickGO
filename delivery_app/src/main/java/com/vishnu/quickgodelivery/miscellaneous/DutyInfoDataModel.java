package com.vishnu.quickgodelivery.miscellaneous;

public class DutyInfoDataModel {

    String dp_name;
    String dp_id;
    double dp_lat;
    double dp_lon;
    String time_stamp;

    public DutyInfoDataModel(String dp_name, String dp_id, double dp_lat, double dp_lon, String time_stamp) {
        this.dp_name = dp_name;
        this.dp_id = dp_id;
        this.dp_lat = dp_lat;
        this.dp_lon = dp_lon;
        this.time_stamp = time_stamp;
    }
}
