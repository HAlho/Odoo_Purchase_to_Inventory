package com.example.projectodoo;

import static android.content.ContentValues.TAG;

import static java.util.Arrays.asList;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import org.apache.xmlrpc.XmlRpcException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ApproveOrderAdapter extends RecyclerView.Adapter<ApproveOrderAdapter.MyViewHolder>{
    private List<ApproveOrder> orderList;
    private Credentials credentials;

    //constructor
    ApproveOrderAdapter(List<ApproveOrder> orderList, Credentials credentials){
        this.orderList = orderList;
        this.credentials = credentials;
    }

    @Override
    public ApproveOrderAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.approve_adapter,parent,false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ApproveOrderAdapter.MyViewHolder holder, final int position) {
        final ApproveOrder order = orderList.get(position);
        holder.name.setText(order.getOrderName());
        String[] arr = order.getVendor().split(",");
        holder.vendor.setText("Vendor: "+arr[1].substring(0,arr[1].length()-1));


        //holder.vendor.append(order.getVendor());
        int limit = holder.table.getChildCount();
        for (int i = 1; i < limit; i++) {
            holder.table.removeViewAt(1);
        }

        //populate the table
        //for each hashmap create a row
        for (int i=0;i<order.getOrderLines().size();i++) {
            //order hashmap into string
            HashMap data = order.getOrderLines().get(i);
            List<String> orderedData=new ArrayList<String>();

                orderedData.add(data.get("name").toString());
                orderedData.add(data.get("date_planned").toString());
                orderedData.add(((Object[])data.get("company_id"))[1].toString());
                orderedData.add(data.get("product_qty").toString());
                orderedData.add(((Object[]) data.get("product_uom"))[1].toString());
                orderedData.add(data.get("price_unit").toString());
                orderedData.add(data.get("price_subtotal").toString());



            //create row
            TableRow row = new TableRow(holder.cardView.getContext());
            for(String value: orderedData) {
               // Log.e("values creation", value.toString());
                //add text views
                TextView t = new TextView(holder.cardView.getContext());
                t.setText(value);
                row.addView(t);
            }

            holder.table.addView(row);

        }


        holder.b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //approve the order using the id
                final Object[] Result = {null};
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                            Log.e("purchaseid", String.valueOf(order.getPurchaseId()));
                            Result[0] = approveOrder(order.getPurchaseId());
                    }
                }
                );
                thread.start();
                while(thread.isAlive()){
                    Log.e(TAG,"waiting for thread...update order");
                }

                //if approve successful hid the button
                if(Result[0].toString().equals("true")){//check
                    Log.e("approveOrder", "disable button");
                    Toast.makeText(view.getContext(), "approve success", Toast.LENGTH_LONG).show();
                    holder.b.setVisibility(View.GONE);

                }else {
                    Toast.makeText(view.getContext(), "approve failed", Toast.LENGTH_LONG).show();
                    Log.e("approveOrder", "approve failed");
                }

            }
        });


    }
    @Override
    public int getItemCount() {
        return orderList.size();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView name;
        private TextView vendor;
        private CardView cardView;
        private TableLayout table;

        private Button b;

        public MyViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            vendor = itemView.findViewById(R.id.vendor);
            cardView = itemView.findViewById(R.id.card);
            table = itemView.findViewById(R.id.table);
            b=itemView.findViewById(R.id.update_button);

        }

    }

    public Object approveOrder(Integer orderId){//used to have second input num for the button id

        Object updateOrder =null;
        try {

            //approve order (move to seperate button)
            // List info = asList(orderId);
            updateOrder = rpcClient.getStaticClient().execute("execute_kw", asList(
                    credentials.getDatabase(), credentials.getId(), credentials.getPassword(),
                    "purchase.order", "button_confirm", asList(orderId)
            ));
            //client.odooMethod("purchase.order", "button_confirm", info);
            Log.e("Approve Order", String.valueOf(updateOrder));

        }catch(XmlRpcException e){
            Log.e("ApproveOrder rpcerror" , e.getMessage());
        }catch (Exception e) {
            Log.e("ApproveOrder err", e.getMessage());
        }

        return updateOrder;

    }
}
