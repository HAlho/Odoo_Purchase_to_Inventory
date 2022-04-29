package com.example.projectodoo;

import static java.util.Arrays.asList;

import android.util.Log;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class rpcClient {
    private static XmlRpcClient model;
    private Credentials credentials;

    public rpcClient(Credentials credentials){
        this.credentials = credentials;
        setClient();
    }


    public void setClient(){
        XmlRpcClient models = new XmlRpcClient();
        try {
            models = new XmlRpcClient() {{
                setConfig(new XmlRpcClientConfigImpl() {{
                    setServerURL(new URL("http://192.168.0.123:8069/xmlrpc/2/object"));
                }});
            }};
        } catch (Exception e) {
            e.getMessage();
        }
        model=models;
    }

    //static method
    public static XmlRpcClient getStaticClient(){
        XmlRpcClient models = new XmlRpcClient();
        try {
            models = new XmlRpcClient() {{
                setConfig(new XmlRpcClientConfigImpl() {{
                    setServerURL(new URL("http://192.168.0.123:8069/xmlrpc/2/object"));
                }});
            }};
        } catch (Exception e) {
            e.getMessage();
        }
        return models;
    }

    //method read
    //method search and read
    public Object SearchAndRead(String odooModel, String method,List searchConditions, HashMap filter){
        String db=this.credentials.getDatabase();
        int uid = this.credentials.getId();
        String password = this.credentials.getPassword();
        XmlRpcClient model = this.model;
        final Object[] result = {null};
        Log.e("searchAndRead", Arrays.deepToString(result));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    result[0] = (Object[]) model.execute("execute_kw", asList(
                            db, uid, password,
                            odooModel, method,
                            searchConditions, //equivalent to draft or sent or to approve
                            filter
                    ));
                } catch (XmlRpcException e) {
                    Log.e("xmlerror" , e.getMessage());
                } catch (Exception e) {
                    Log.e("error" , e.getMessage());
                }
            }
        });
        thread.start();
        while(thread.isAlive()){
            Log.e("waiting for client", "search and read");
        }
        return result[0];
    }
    //empty method??? for button_confirm etc.
    public Object odooMethod(String odooModel, String method,List Conditions){
        String db=this.credentials.getDatabase();
        int uid = this.credentials.getId();
        String password = this.credentials.getPassword();
        XmlRpcClient model = this.model;
        final Object[] result = {null};
        Log.e("searchAndRead", Arrays.deepToString(result));
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    result[0] = (Object) model.execute("execute_kw", asList(
                            db, uid, password,
                            odooModel, method,
                            Conditions //equivalent to draft or sent or to approve
                    ));
                } catch (XmlRpcException e) {
                    Log.e("xmlerror" , e.getMessage());
                }
            }
        });
        thread.start();
        while(thread.isAlive()){
            Log.e("waiting for client", "search and read");
        }
        return result[0];
    }
}
