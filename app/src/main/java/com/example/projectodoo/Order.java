package com.example.projectodoo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Order {
    Integer vendorId;
    Integer productId;
    String productName;
    List<HashMap<String,String>> orderLines;

    //orderline
    String datePlanned;
    Object[] pUOM; //product unit of measure
    Object[] companyId;

    public Order(){
        this.vendorId=null;
        this.productId=null;
        orderLines = new ArrayList<>();
    }


    public Integer getVendorId() {
        return vendorId;
    }

    public void setVendorId(Integer vendorId) {
        this.vendorId = vendorId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public List<HashMap<String, String>> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<HashMap<String, String>> orderLines) {
        this.orderLines = orderLines;
    }

    public String getDatePlanned() {
        return datePlanned;
    }

    public void setDatePlanned(String datePlanned) {
        this.datePlanned = datePlanned;
    }

    public Object[] getpUOM() {
        return pUOM;
    }

    public void setpUOM(Object[] pUOM) {
        this.pUOM = pUOM;
    }

    public Object[] getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Object[] companyId) {
        this.companyId = companyId;
    }

}
