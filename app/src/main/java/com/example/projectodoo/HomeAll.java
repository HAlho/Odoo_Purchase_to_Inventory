package com.example.projectodoo;

import static android.content.ContentValues.TAG;
import static java.util.Arrays.asList;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class HomeAll extends AppCompatActivity {

    Thread thread;

    //vendor and product variables
    static List<Object> vendorsList;
    static List<Object> productsList;

    Order order = new Order();

    //spinners
    Spinner vendorSpinner;
    Spinner productSpinner;

    //Odoo authorization variables
    String db;
    String password;
    int uid;

    Credentials credentials;


    int totalLimit=5000;

    rpcClient client;
    BarcodeScan scanner = null;

    String previousBarcode="";
    int barcodeDetectedAgain =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        vendorSpinner = (Spinner) findViewById(R.id.vendor_dropdown);
        productSpinner = (Spinner) findViewById(R.id.product_dropdown);

        //receive intent
        //get intent info
        Intent i=getIntent();

        credentials = i.getParcelableExtra("Credentials");
        db=credentials.getDatabase();
        uid=credentials.getId();
        password=credentials.getPassword();
        Log.e(TAG, "intent is "+db+" "+String.valueOf(uid)+" "+password);

        TextView message = findViewById(R.id.home_message);
        message.append(credentials.getUsername()+"\n");


        client = new rpcClient(credentials);


        getVendors(db,uid,password);
        getProducts(db,uid,password);

        Log.e("check list at create", String.valueOf(productsList));



    }

    @Override
    protected void onStart() {
        super.onStart();
        //initialize buttons
        Button confirmOrder = (Button) findViewById(R.id.confirmOrder);
        Button addOrderLine = (Button) findViewById(R.id.orderLine_button);

        while(vendorsList==null || productsList==null) {
            Log.e("onStart", "waiting for vendors and products list");
        }
        spinnerListener(vendorSpinner, vendorsList,"vendor");
        spinnerListener(productSpinner, productsList, "product");


        FrameLayout createOrderLayout = (FrameLayout) findViewById(R.id.createorder_layout);
        FrameLayout approveOrderLayout = (FrameLayout) findViewById(R.id.approveorder_layout);
        FrameLayout purchaseOrderLayout = findViewById(R.id.purchaseorder_layout);

        TextView welcomeMessage = (TextView) findViewById(R.id.home_message);
        FrameLayout pickingOrderLayout = (FrameLayout) findViewById(R.id.pickingorder_layout);


        TabLayout tab = (TabLayout) findViewById(R.id.tab);

        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.e(TAG, String.valueOf(tab.getPosition()));
                if(tab.getPosition()==0){
                    welcomeMessage.setVisibility(View.VISIBLE);
                    pickingOrderLayout.setVisibility(View.GONE);
                    purchaseOrderLayout.setVisibility(View.GONE);
                    approveOrderLayout.setVisibility(View.GONE);
                }
                else if(tab.getPosition()==1){
                    welcomeMessage.setVisibility(View.GONE);
                    pickingOrderLayout.setVisibility(View.GONE);
                    purchaseOrderLayout.setVisibility(View.VISIBLE);
                    approveOrderLayout.setVisibility(View.GONE);
                    showPurchaseOrders();
                }
                else if(tab.getPosition()==2){
                    welcomeMessage.setVisibility(View.GONE);
                    pickingOrderLayout.setVisibility(View.VISIBLE);
                    purchaseOrderLayout.setVisibility(View.GONE);
                    approveOrderLayout.setVisibility(View.GONE);
                    //get draft orders and add to view
                    showPickingOrders();
                }
                else if(tab.getPosition()==3){
                    welcomeMessage.setVisibility(View.GONE);
                    pickingOrderLayout.setVisibility(View.GONE);
                    purchaseOrderLayout.setVisibility(View.GONE);
                    approveOrderLayout.setVisibility(View.VISIBLE);
                    showDraftOrders();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if(tab.getPosition()==1){
                    closePurchaseCard();
                    resetPurchaseView();
                }
                if(tab.getPosition()==2){
                    resetPickingView();
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if(tab.getPosition()==1){
                    closePurchaseCard();
                    resetPurchaseView();
                    showPurchaseOrders();
                }
                if(tab.getPosition()==2){
                    resetPickingView();
                    showPickingOrders();
                }
            }
        });

        FloatingActionButton createOrderButton = findViewById(R.id.addorder_floatingbutton);

        createOrderButton.setOnClickListener(view -> {
            ScrollView table = findViewById(R.id.purchaseorders_view);
            table.setVisibility(View.GONE);
            createOrderButton.setVisibility(View.GONE);
            createOrderLayout.setVisibility(View.VISIBLE);

        });


        //request quotation listener
        confirmOrder.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                createOrder(); }});


        //add order line
        addOrderLine.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addOrderLine(); }});


        //New
        PreviewView cameraView = findViewById(R.id.scan_surfaceView);
        FloatingActionButton scanFloatingButton = findViewById(R.id.scan_floatingbutton);
        ScrollView table = findViewById(R.id.pickingorders_view);
        scanFloatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get camera permissions
                // Check for the camera permission before accessing the camera.  If the
                // permission is not granted yet, request permission.
                int rc = ActivityCompat.checkSelfPermission(view.getContext(), Manifest.permission.CAMERA);
                if (rc != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission();
                }
                table.setVisibility(View.GONE);
                cameraView.setVisibility(View.VISIBLE);

                PreviewView mPreview =  findViewById(R.id.scan_surfaceView);
                scanner = new BarcodeScan(view.getContext(),mPreview.getRootView(),mPreview.getId());

                scanner.startScan();
                scanner.setCustomObjectListener(new BarcodeScan.BarcodeScanListener() {
                    @Override
                    public void onBarcodeReady(String Barcode) {
                        Log.e("Received Barcode", Barcode);

                        //check order exist
                        if(orderExist(Barcode)){
                            cameraView.setVisibility(View.GONE);
                            scanFloatingButton.setVisibility(View.GONE);
                            CardView pickingOrderCard = findViewById(R.id.pickingorder_card);
                            pickingOrderCard.setVisibility(View.VISIBLE);
                            displayPickingOrder(Barcode);
                            scanner.destroyScanner();
                        }
                    }
                });

            }
        });


    }

    // Handles the requesting of the camera permission.
    private void requestCameraPermission() {
        Log.w("CAMERA PERMISSION", "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, 2);
        }
    }


    public void showPickingOrders(){

        //retrieve from odoo
        List conditions = asList(asList(asList("state","=","assigned"),asList("picking_type_id","=",1)));
        HashMap filter = new HashMap(){{ put("fields", asList("name", "partner_id", "location_dest_id",
                "scheduled_date", "origin","backorder_id","state"));}};

        //a thread starts inside and the main thread waits for the result
        Object result;
        result = client.SearchAndRead("stock.picking","search_read",conditions, filter);


        Object[] list = (Object[])result;
        //bind table
        TableLayout pickingTable = findViewById(R.id.pickingorder_table);

        for(Object order: list){
            //cast object to hashmap
            HashMap data = (HashMap) order;

            //order data
            List<String> orderedData = new ArrayList<>();
            orderedData.add(data.get("name").toString());
            orderedData.add(((Object[])data.get("location_dest_id"))[1].toString());
            orderedData.add(((Object[])data.get("partner_id"))[1].toString());
            orderedData.add(data.get("scheduled_date").toString());
            orderedData.add(data.get("origin").toString());
            orderedData.add(data.get("backorder_id").toString());
            orderedData.add(data.get("state").toString());

            //display in table
            //add new row
            TableRow row = new TableRow(HomeAll.this);

            for(String info: orderedData) {
                TextView text = new TextView(getApplicationContext());
                text.setText(info);
                text.setHeight(100);
                text.setTextSize(16);
                if(info.equals("false")) text.setText("");
                text.setBackground(getDrawable(R.drawable.rectangle));
                row.addView(text);
            }

            //add row to table
            pickingTable.addView(row);

        }

    }


    public void showPurchaseOrders(){

        //retrieve from odoo
        List conditions = asList(asList(asList("state","=","purchase")));
        HashMap filter = new HashMap(){{ put("fields", asList("name", "date_order","partner_id","date_planned",
                "user_id", "origin","amount_total","state"));}};

        //a thread starts inside and the main thread waits for the result
        Object result;
        result = client.SearchAndRead("purchase.order","search_read",conditions, filter);
        Log.e("showPurchaseOrders", Arrays.deepToString((Object[])result));

        Object[] list = (Object[])result;
        //bind table
        TableLayout pickingTable = findViewById(R.id.purchaseorder_table);
        for(Object order: list){
            Log.e("purchase order" , order.toString());
            //cast object to hashmap
            HashMap data = (HashMap) order;

            //order data
            List<String> orderedData = new ArrayList<>();
            orderedData.add(data.get("name").toString());
            orderedData.add(data.get("date_order").toString());
            orderedData.add(((Object[])data.get("partner_id"))[1].toString());
            orderedData.add(data.get("date_planned").toString());
            orderedData.add(((Object[])data.get("user_id"))[1].toString());
            orderedData.add(data.get("origin").toString());
            orderedData.add(data.get("amount_total").toString());
            orderedData.add(data.get("state").toString());

            //display in table
            //add new row
            TableRow row = new TableRow(getApplicationContext());

            for(String info: orderedData) {
                TextView text = new TextView(getApplicationContext());
                text.setText(info);
                text.setHeight(100);
                text.setTextSize(16);
                if(info.equals("false"))text.setText("");
                text.setBackground(getDrawable(R.drawable.rectangle));
                row.addView(text);
            }

            //add row to table
            pickingTable.addView(row);

        }
    }

    public Boolean orderExist(String orderName){
        List condition = asList(asList(asList("name","=",orderName)));
        HashMap filter = new HashMap(){{put("fields", asList("name", "partner_id","origin","backorder_id","move_line_ids"));}};
        Object[] result = (Object[]) client.SearchAndRead("stock.picking","search_read",condition,filter);

        Log.e("orderExist", Arrays.deepToString(result));

        if((Arrays.deepEquals(result, new Object[]{}) || Arrays.deepEquals(result, new Object[]{null})))
            showToast("Order Does not Exist");

        return !(Arrays.deepEquals(result, new Object[]{}) || Arrays.deepEquals(result, new Object[]{null}));
    }


    public void displayPickingOrder(String orderName){
        //prepare for request
        List condition = asList(asList(asList("name","=",orderName)));
        HashMap filter = new HashMap(){{put("fields", asList("name", "partner_id","origin","backorder_id","move_line_ids"));}};
        //get piking order info
        Object[] orderToReceive = (Object[]) client.SearchAndRead("stock.picking","search_read",condition,filter);

        HashMap orderInfo = (HashMap) orderToReceive[0];
        ApproveOrder validateList;

        //get move lines info
        Log.e(TAG, Arrays.deepToString((Object[])orderInfo.get("move_line_ids")));


        //condition = (List) pickingOrder.get("move_line_ids");//might not work
        Object[] ids = (Object[]) orderInfo.get("move_line_ids");

        filter = new HashMap(){{put("fields", asList("product_id","product_uom_qty","product_uom_id","qty_done"));}};
        orderToReceive = (Object[]) client.SearchAndRead("stock.move.line", "read", asList(asList(ids)), filter);

        ApproveOrder pickingOrder = new ApproveOrder(Integer.valueOf(orderInfo.get("id").toString()),
                orderInfo.get("name").toString(), Arrays.deepToString((Object [])orderInfo.get("partner_id")),
                orderInfo.get("origin").toString(),orderInfo.get("backorder_id").toString());



        Log.e("result", Arrays.deepToString(orderToReceive));

        //set orderlines list
        List<HashMap> movelines= new ArrayList<HashMap>();
        for (Object o : orderToReceive) {
            movelines.add((HashMap) o);
        }
        pickingOrder.setOrderLines(movelines);


        //create editText Array
        List<HashMap<String, EditText>> editTexts = new ArrayList<HashMap<String, EditText>>();
        TableLayout table = findViewById(R.id.table);

        //display name and partner
        //set order and vendor name to current card
        TextView name = findViewById(R.id.name);
        TextView vendor = findViewById(R.id.vendor);
        Log.e(TAG,orderInfo.get("name").toString()+" "+"Partner: "+orderInfo.get("partner_id").toString());
        name.setText(orderInfo.get("name").toString());
        vendor.setText("Vendor: "+((Object[])orderInfo.get("partner_id"))[1].toString());


        //this part is from ReceiveOrderAdapter
        //loop for every line
        for (Object o : orderToReceive) {
            HashMap line = (HashMap) o;
            List<String> orderedData = new ArrayList<String>();

            orderedData.add(((Object[]) line.get("product_id"))[1].toString());
            orderedData.add(line.get("product_uom_qty").toString());
            orderedData.add(((Object[]) line.get("product_uom_id"))[1].toString());

            Log.e("id is ", (((Object[]) line.get("product_id"))[0].toString()));

            //create row
            TableRow row = new TableRow(getApplication().getApplicationContext());
            for (String value : orderedData) {
                //Log.e("add to row", value);
                //add text views
                TextView t = new TextView(getApplication().getApplicationContext());
                t.setText(value);
                row.addView(t);
            }
            EditText e = new EditText(getApplication().getApplicationContext());
            e.setText(line.get("qty_done").toString());
            Log.e("ShowPickingOrder", "hello??");
            e.setInputType(InputType.TYPE_CLASS_NUMBER);
            editTexts.add(new HashMap<String, EditText>() {{
                put((((Object[]) line.get("product_id"))[0].toString()), e);
            }});

            row.addView(e);
            table.addView(row);
        }

        //update button
        Button updateButton = findViewById(R.id.update_button);
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //approve the order using the id
                List<Object> Result = new ArrayList<Object>();
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //arrange movelines ids into an array
                        Integer[] moveLines = new Integer[pickingOrder.getOrderLines().size()];
                        //arrange edittext values into an array
                        String[] received = new String[pickingOrder.getOrderLines().size()];

                        for (int i = 0; i < pickingOrder.getOrderLines().size(); i++) {
                            HashMap temp = pickingOrder.getOrderLines().get(i);
                            moveLines[i] = Integer.valueOf(temp.get("id").toString());

                            //get value and store it into the array
                            Set<String> keyset = editTexts.get(i).keySet();
                            String key =keyset.iterator().next();
                            //Log.e("key", key);
                            received[i] = editTexts.get(i).get(key).getText().toString();
                            Log.e("ckeck float", String.valueOf(received[i]));

                        }

                        //get edit text values
                        Result.add(UpdateOrder(moveLines, received));

                    }
                }
                );
                thread.start();
                while (Result.isEmpty()) {
                    Log.e(TAG, "waiting for thread...update order");
                }
                //display feedback for the first move line update
                //String result = Arrays.deepToString((Object[]) Result[0]);
                Log.e(TAG, Result.toString());
                for(int n=0;n<order.getOrderLines().size();n++) {

                }
                if(!Result.isEmpty())
                    Toast.makeText(getApplicationContext(), "Quantity Updated", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(), "Update Failed", Toast.LENGTH_LONG).show();

            }

        });

        //validate button
        Button validateButton = findViewById(R.id.validate_button);
        validateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //approve the order using the id
                final Object[] Result = {null};

                //get edit text values
                Result[0] = ValidateOrder(pickingOrder.getPurchaseId());

                resetPickingView();
                showPickingOrders();
            }
        });

        //scan products button
        Button scanProducts = findViewById(R.id.scanproduct_button);
        PreviewView cameraView =findViewById(R.id.productscan_surfaceView);
        Button close = findViewById(R.id.closescan_button);
        scanProducts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.setVisibility(View.VISIBLE);
                close.setVisibility(View.VISIBLE);

                scanner = new BarcodeScan(view.getContext(), cameraView.getRootView(),cameraView.getId());
                scanner.startScan();
                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        cameraView.setVisibility(View.GONE);
                        scanner.destroyScanner();
                        close.setVisibility(View.GONE);
                    }
                });
                scanner.setCustomObjectListener(new BarcodeScan.BarcodeScanListener() {
                    @Override
                    public void onBarcodeReady(String Barcode) {
                        //check if id taken 3 times
                        if(Barcode.equals(previousBarcode)) barcodeDetectedAgain++;
                        previousBarcode= Barcode;


                        if(barcodeDetectedAgain>1) {
                            previousBarcode="";
                            barcodeDetectedAgain=0;

                            //find row
                            for (int i = 0; i < editTexts.size(); i++) {
                                Set<String> keyset = editTexts.get(i).keySet();
                                String key = keyset.iterator().next();
                                Log.e("key", key);

                                Log.e("reading", editTexts.get(i).get(key).getText().toString());
                                try {
                                    Integer t = Integer.valueOf(Barcode);
                                    Log.e("integer", t.toString());
                                    Barcode = t.toString();
                                } catch (Exception e) {

                                }
                                if (editTexts.get(i).containsKey(Barcode)) {
                                    Log.e("row found", editTexts.get(i).get(Barcode).getText().toString());
                                    //increment edittext in that row
                                    Double d = Double.valueOf(editTexts.get(i).get(Barcode).getText().toString());
                                    int n = (int) Math.floor(d);
                                    editTexts.get(i).get(Barcode).setText(String.valueOf(++n));


                                    // Beep
                                    final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 500);


                                    break;
                                }

                            }
                        }


                    }
                });
            }
        });
    }

    public Object ValidateOrder(Integer pickingId){
        Log.e("ValidateOrder", pickingId.toString());
        Object result = null;
        result= client.odooMethod("stock.picking", "button_validate", asList(pickingId));
        Log.e("ValidateOrder", String.valueOf(result));

        return result;

    }



    public void showDraftOrders(){

        List condition = asList(asList("|", asList("state","=","draft"),
                "|", asList("state", "=", "sent"),
                asList("state", "=", "to approve")
        ));
        HashMap filter = new HashMap() {{
            put("fields", asList("name", "partner_id","order_line"));
        }};
        Object[] ordersToApprove=(Object[])client.SearchAndRead("purchase.order", "search_read", condition, filter);
        Log.e("orders", Arrays.deepToString(ordersToApprove));

        //create a hashmap list that holds all orders
        HashMap[] order = new HashMap[ordersToApprove.length];
        Object[] result;
        List<ApproveOrder> approveList= new ArrayList<ApproveOrder>();
        if(ordersToApprove.length>0) {
            //get order lines

            //object to map
            Object[] ids = null; //list of order line ids for that order
            for (int j = 0; j < ordersToApprove.length; j++) {
                order[j] = (HashMap) ordersToApprove[j];
                ApproveOrder a = new ApproveOrder(Integer.valueOf(
                        order[j].get("id").toString()), order[j].get("name").toString(),
                        Arrays.deepToString((Object [])order[j].get("partner_id")));
                ids = (Object[]) order[j].get("order_line");


                condition = asList(asList(ids));
                filter = new HashMap() {{
                    put("fields", asList("name", "date_planned", "company_id",
                            "product_qty", "product_uom", "price_unit", "price_subtotal"));
                }};
                result = (Object[])client.SearchAndRead("purchase.order.line","read", condition, filter );
                Log.e("order lines", Arrays.deepToString(result));

                //set orderlines list
                List<HashMap> orderlines= new ArrayList<HashMap>();
                for(int i=0;i<result.length;i++) {
                    orderlines.add((HashMap) result[i]);

                }
                a.setOrderLines(orderlines);

                //add order to approveList
                approveList.add(a);
            }
        }



        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.approve_recycler);
        ApproveOrderAdapter recyclerViewAdapter = new ApproveOrderAdapter(approveList, credentials);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recyclerViewAdapter);


    }


    //thread of getDateUOM MUST have finished
    public void addOrderLine(){
        EditText price = (EditText) findViewById(R.id.price);
        EditText quantity = (EditText) findViewById(R.id.quantity);
        TableLayout showOrder = findViewById(R.id.message);
        //get price and quantity from text
        String p = price.getText().toString();
        String q = quantity.getText().toString();
        //integer versions for calculating the subtotal
        int pnum = Integer.parseInt(p);
        int qnum = Integer.parseInt(q);

        //after result is retrieved
        Boolean b=false;
        if(order.getProductId()==null||p.equals("")||q.equals(""))
            showToast("product details missing");

        else{
            //all without the order_id
            try {
                b = order.getOrderLines().add(new HashMap() {{
                    put("product_id", order.getProductId());
                    put("name", order.getProductName());
                    put("product_qty", q);
                    put("price_unit", p);
                    put("date_planned", order.getDatePlanned());
                    put("product_uom", order.getpUOM()[0]);//unit-->1  Liter-->5
                    put("price_subtotal",String.valueOf(pnum*qnum));
                    //put("order_id", orderId);
                }});

                //create row
                TableRow row = new TableRow(this);

                //organize data
                List<String> orderedData = new ArrayList<String>();
                int size = order.getOrderLines().size();

                orderedData.add(order.getOrderLines().get(size-1).get("name").toString());//name
                orderedData.add(order.getOrderLines().get(size-1).get("date_planned").toString());//date planned
                orderedData.add(((Object[])order.getCompanyId())[1].toString());//company
                orderedData.add(order.getOrderLines().get(size-1).get("product_qty").toString());//quantity
                orderedData.add(order.getpUOM()[1].toString());//unit of measure
                orderedData.add(order.getOrderLines().get(size-1).get("price_unit").toString());//price of unit
                orderedData.add(String.valueOf(qnum*pnum));//subtotal

                //add data to row
                for(String value: orderedData){
                    //add text views
                    TextView t = new TextView(this);
                    t.setText(value);
                    row.addView(t);
                }
                showOrder.addView(row);

                Log.e("added order line size", String.valueOf(order.getOrderLines().size()));
            }catch (Exception e){
                Log.e("b", String.valueOf(b));
                Log.e("error", e.getMessage());
                e.getLocalizedMessage();
            }
        }
    }

    public void showToast(String toast)
    {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_LONG).show());
    }

    public int findId(List<Object> list, Object o){

        //iterate over list
        HashMap info=new HashMap();
        Log.e("check 3",String.valueOf(vendorsList));
        Log.e(TAG, String.valueOf(list)+" "+String.valueOf(o));
        for(int num=0; num<list.size();num++){
            try {
                info = (HashMap) list.get(num);
                String name = (String)info.get("name");
                //if name is same as selected name
                if(name.equals(String.valueOf(o))){
                    return (int)info.get("id");
                }
            }catch (Exception e){
                Log.e("Exception", String.valueOf(e));
            }

        }
        return -1;

    }

    public void spinnerListener(Spinner spinner, List<Object> list, String type){
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Log.e("check 2",String.valueOf(vendorsList));
                Object o = spinner.getItemAtPosition(i);
                Log.e("spinner position,item", String.valueOf(i)+" "+String.valueOf(o));
                //save the id
                Log.e("check list", String.valueOf(list));
                if(type.equals("vendor")) {
                    order.setVendorId(findId(list, o));
                }
                else if (type.equals("product")){
                    order.setProductName(String.valueOf(o));
                    order.setProductId(findId(list, o));
                    getDateUOM();

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                //do nothing
            }


        });
    }


    public void displayList(List<Object> list, String spinner){

        Log.e("check list at display", String.valueOf(list));

        // you need to have a list of data that you want the spinner to display
        List<String> spinnerArray =  new ArrayList<String>();

        //iterate over list
        HashMap info=new HashMap();
        for(int num=0; num<list.size();num++){
            try {
                info =(HashMap)list.get(num);
                String vendor = (String)info.get("name");
                Log.e("object name", vendor);
                spinnerArray.add(vendor);
            }catch (Exception e){
                Log.e("Exception", String.valueOf(e));
            }

        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);//might not be neccessary

        if(spinner.equals("vendor"))
            vendorSpinner.setAdapter(adapter);
        else if(spinner.equals("product"))
            productSpinner.setAdapter(adapter);


    }


    public void getDateUOM(){
        try {

            //this is order date
            //SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy kk:mm:ss");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+2"));
            String current = dateFormat.format(new Date());
            Log.e(TAG, current);

            Calendar c=Calendar.getInstance();
            try {
                c.setTime(dateFormat.parse(current));
            }catch(ParseException e){
                Log.e("error", e.getMessage());
            }

            List condition =asList(order.getProductId());
            HashMap filter = new HashMap() {{
                put("fields", asList("seller_ids", "uom_po_id","company_id"));
            }};

            Object[] sellerIds = (Object[])client.SearchAndRead("product.product", "read", condition, filter);
            //Log.e(TAG, Arrays.deepToString(sellerIds));

            HashMap s = (HashMap) sellerIds[0];
            sellerIds=(Object[]) s.get("seller_ids");
            Log.e("sellerIds",Arrays.deepToString(sellerIds));

            //save company_id to global variable
            order.setCompanyId((Object[]) s.get("company_id"));

            //to find delay for all sellers (remove index of ids and delays)
            condition = asList(sellerIds[0]);
            filter = new HashMap() {{
                put("fields", asList("delay"));
            }};

            HashMap delay = (HashMap)((Object[])client.SearchAndRead("product.supplierinfo", "read", condition, filter))[0];
            Integer d = (Integer)delay.get("delay");
            Log.e(TAG, d.toString());

            c.add(Calendar.DAY_OF_MONTH, d);
            current = dateFormat.format(c.getTime());
            Log.e("new date", current);
            order.setDatePlanned(current);

            order.setpUOM((Object[])s.get("uom_po_id"));
            Log.e("product_uom", Arrays.deepToString(order.getpUOM()));
            Log.e("product_uom", order.getpUOM()[0].toString());


        }catch (Exception e) {
            Log.e("err", e.getMessage());
        }

    }

    public void createOrder(){

    try {

        List info = asList(new HashMap() {{
            put("partner_id", order.getVendorId());
        }});
        Integer orderId;

        orderId = (Integer) client.odooMethod("purchase.order", "create", info);
        //partner_id, product_id, product_qty, price_unit
        Log.e("create order", String.valueOf(orderId));

        //append the order ID
        for (int i = 0; i < order.getOrderLines().size(); i++) {
            order.getOrderLines().get(i).put("order_id", orderId.toString());
        }

        if (orderId != null) {

            info = asList(order.getOrderLines());
            Object[] lineId = (Object[]) client.odooMethod("purchase.order.line", "create", info);

            Log.e("line id", Arrays.deepToString(lineId));
            String message = "Order Created:" + orderId;
            showToast(message);
        }

        //reset table
        TableLayout lines = findViewById(R.id.message);
        for(int i=1; i<lines.getChildCount(); i++)
            lines.removeViewAt(1);

        //approve immediately if total is less than 5000rf
        //find the total
        Integer total = 0;
        Object result = null;
        for (HashMap h : order.getOrderLines()) {
            total += Integer.valueOf(h.get("price_subtotal").toString());
        }
        Log.e("total", String.valueOf(total));

        //reset order object for next create order
        order = new Order();

        //find limit
        if (total < totalLimit) {
            //approve order
            result = client.odooMethod("purchase.order","button_confirm",asList(orderId));
            //result = approveOrder(orderId, db, uid, password);
            if (result.toString().equals("true"))
                showToast("Auto Order Approval; order is less than limit");
            else showToast("Order not approved");
        }

        //close card
        closePurchaseCard();

    }catch (Exception e) {
        Log.e("createOrder err", e.getMessage());
    }

    }
    public void closePurchaseCard(){
        CardView card = findViewById(R.id.createorder_layout);
        FloatingActionButton button = findViewById(R.id.addorder_floatingbutton);
        ScrollView tableParent = findViewById(R.id.purchaseorders_view);
        card.setVisibility(View.GONE);
        button.setVisibility(View.VISIBLE);
        tableParent.setVisibility(View.VISIBLE);
    }

    public void resetPurchaseView(){
        TableLayout table = findViewById(R.id.purchaseorder_table);
        while(table.getChildCount()>1)
            table.removeViewAt(1);
    }

    public void resetPickingView(){
        CardView card = findViewById(R.id.pickingorder_card);
        FloatingActionButton button = findViewById(R.id.scan_floatingbutton);
        ScrollView tableParent = findViewById(R.id.pickingorders_view);
        PreviewView cameraView = findViewById(R.id.scan_surfaceView);
        card.setVisibility(View.GONE);
        button.setVisibility(View.VISIBLE);
        tableParent.setVisibility(View.VISIBLE);
        cameraView.setVisibility(View.GONE);
        try {
            if (scanner != null) scanner.destroyScanner();
        }catch (Exception e){
            Log.e("resetPickingView", e.toString());
        }
        //clear tables
        TableLayout cardTable = findViewById(R.id.table);
        while(cardTable.getChildCount()>1)
            cardTable.removeViewAt(1);
        TableLayout ordersTable = findViewById(R.id.pickingorder_table);
        while(ordersTable.getChildCount()>1)
            ordersTable.removeViewAt(1);

    }

    public int getApprovalLimit(){
        return (Integer)client.SearchAndRead("res.config.settings", "read", asList(1),
                new HashMap(){{put("fields",asList("po_double_validation_amount"));}});
    }




    public List<Object> UpdateOrder(Integer[] moveLines, String[] qtyReceived){//update and approve order

        List<Object> resultOfAll = new ArrayList<Object>();

            for(int i=0; i<moveLines.length;i++) {
                //update products received
                int finalI = i;

                resultOfAll.add(client.odooMethod("stock.move.line","write", asList(
                        asList(moveLines[i]),
                        new HashMap() {{
                            put("qty_done", qtyReceived[finalI]);
                        }}
                )));
                Log.e("update qty received", String.valueOf(resultOfAll.get(i)));

            }

        return resultOfAll;

    }
    public void getVendors(String Db, int uid, String Password){
        thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {

                    vendorsList= asList((Object[])rpcClient.getStaticClient().execute("execute_kw", asList(
                            Db, uid, Password,
                            "res.partner", "search_read",
                            asList(asList(
                                    asList("supplier", "=", true)
                            )),
                            new HashMap() {{
                                put("fields", asList("name"));
                            }}
                    )));

                    Log.e("Suppliers ids", String.valueOf(vendorsList));

                    final Handler myHandler = new Handler(Looper.getMainLooper());
                    myHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            displayList(vendorsList, "vendor");
                        }
                    });


                }catch (XmlRpcException e){
                    Log.e("error", e.getMessage());
                } catch (Exception e) {
                    Log.e("err", e.getMessage());
                }
            }
        });
        thread.start();


    }

    public void getProducts(String Db, int uid, String Password){
        thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {

                    productsList= asList((Object[])rpcClient.getStaticClient().execute("execute_kw", asList(
                            Db, uid, Password,
                            "product.product", "search_read",
                            asList(asList(
                                    //empty
                            )),
                            new HashMap() {{
                                put("fields", asList("name"));
                            }}
                    )));

                    Log.e("product ids", String.valueOf(productsList));

                    final Handler myHandler = new Handler(Looper.getMainLooper());
                    myHandler.post(new Runnable() {

                        @Override
                        public void run() {
                            displayList(productsList, "product");
                        }
                    });


                } catch (XmlRpcException e){
                    Log.e("error", e.getMessage());
                }catch (Exception e) {
                    Log.e("err", e.getMessage());
                }
            }
        });
        thread.start();
    }




}
