package com.vishnu.quickgodelivery.miscellaneous;

public class DutySettingsModel {

    String dp_id;
    String duty_state;
    String duty_district;
    String duty_locality;
    String duty_locality_pincode;

    public DutySettingsModel(String dp_id, String duty_state, String duty_district, String duty_locality, String duty_locality_pincode) {
        this.dp_id = dp_id;
        this.duty_state = duty_state;
        this.duty_district = duty_district;
        this.duty_locality = duty_locality;
        this.duty_locality_pincode = duty_locality_pincode;
    }
}
