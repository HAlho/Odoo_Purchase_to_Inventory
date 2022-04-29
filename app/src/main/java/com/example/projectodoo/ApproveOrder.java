package com.example.projectodoo;

import java.util.HashMap;
import java.util.List;

public class ApproveOrder {
    private int purchaseId;
    private String orderName;
    private String Vendor;

    private List<HashMap> orderLines;

    //exclusive to picking orders
    private String backorderId;
    private String origin;

    //binding solution
    private boolean binded=false;

    public ApproveOrder(Integer purchaseId, String orderName, String Vendor){
        this.purchaseId=purchaseId;
        this.orderName=orderName;
        this.Vendor=Vendor;
    }

    public ApproveOrder(Integer purchaseId, String orderName, String Vendor, List<HashMap> orderLines){
        this.purchaseId=purchaseId;
        this.orderName=orderName;
        this.Vendor=Vendor;
        this.orderLines = orderLines;
    }

    public ApproveOrder(Integer purchaseId, String orderName, String Vendor, String origin, String backorderId){
        this.purchaseId=purchaseId;
        this.orderName=orderName;
        this.Vendor=Vendor;
        this.origin=origin;
        this.backorderId=backorderId;
    }

    public String getOrderName() {
        return orderName;
    }

    public void setOrderName(String orderName) {
        this.orderName = orderName;
    }

    public String getVendor() {
        return Vendor;
    }

    public void setVendor(String vendor) {
        Vendor = vendor;
    }

    public int getPurchaseId() {
        return purchaseId;
    }

    public void setPurchaseId(int purchaseId) {
        this.purchaseId = purchaseId;
    }

    public List<HashMap> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<HashMap> orderLines) {
        this.orderLines = orderLines;
    }


    public String getBackorderId() {
        return backorderId;
    }

    public void setBackorderId(String backorderId) {
        this.backorderId = backorderId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public boolean isBinded() {
        return binded;
    }

    public void setBinded(boolean binded) {
        this.binded = binded;
    }

}