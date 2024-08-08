package com.vishnu.quickgodelivery.ui.all_orders;

public class AllOrdersModel {
    private String orderType;
    private OBSOrderModel obsOrderData;
    private OBVOrderModel obvOrderData;


    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public OBSOrderModel getObsOrderData() {
        return obsOrderData;
    }

    public void setObsOrderData(OBSOrderModel obsOrderData) {
        this.obsOrderData = obsOrderData;
    }

    public OBVOrderModel getObvOrderData() {
        return obvOrderData;
    }

    public void setObvOrderData(OBVOrderModel obvOrderData) {
        this.obvOrderData = obvOrderData;
    }
}
