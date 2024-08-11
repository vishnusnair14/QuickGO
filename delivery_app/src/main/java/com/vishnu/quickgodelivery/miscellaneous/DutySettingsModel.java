package com.vishnu.quickgodelivery.miscellaneous;

public class DutySettingsModel {

    String user_id;
    String user_state;
    String user_district;
    String user_locality;
    String user_pincode;

    public DutySettingsModel(String dp_id, String user_state,
                             String user_district, String user_locality,
                             String user_pincode) {
        this.user_id = dp_id;
        this.user_state = user_state;
        this.user_district = user_district;
        this.user_locality = user_locality;
        this.user_pincode = user_pincode;
    }
}
