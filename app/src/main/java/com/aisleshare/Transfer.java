package com.aisleshare;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class Transfer extends AppCompatActivity {
    // Class Variables
    private ListView listView;
    private ArrayList<Item> items;
    private TransferAdapter customAdapter;
    private boolean isIncreasingOrder;
    private int currentOrder;
    private String listName;
    private JSONObject aisleShareData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        listView = (ListView)findViewById(R.id.currentItems);
        items = new ArrayList<>();

        setTitle(savedInstanceState);
        readSavedItems();
        setListeners();
        sortList(isIncreasingOrder, currentOrder);

        customAdapter = new TransferAdapter(this, items, R.layout.row_transfer);
        listView.setAdapter(customAdapter);
    }

    // Sorted based on the order index parameter
    public void sortList(boolean reverseOrder, int order) {
        if(reverseOrder) {
            isIncreasingOrder = !isIncreasingOrder;
        }
        if(order != currentOrder){
            currentOrder = order;
            isIncreasingOrder = true;
        }

        ItemComparator compare = new ItemComparator(Transfer.this);

        switch (currentOrder){
            // Name
            case 0:{
                ItemComparator.Name sorter = compare.new Name();
                Collections.sort(items, sorter);
                break;}
            // Quantity
            case 1:{
                ItemComparator.Quantity sorter = compare.new Quantity();
                Collections.sort(items, sorter);
                break;}
            // Time Created
            case 2:{
                ItemComparator.Created sorter = compare.new Created();
                Collections.sort(items, sorter);
                break;}
            // Type
            case 3:{
                ItemComparator.Type sorter = compare.new Type();
                Collections.sort(items, sorter);
                break;}
            // Owner
            case 4:{
                ItemComparator.Owner sorter = compare.new Owner();
                Collections.sort(items, sorter);
                break;}
        }
    }

    // Checks/UnChecks an item by clicking on any element in its row
    public void itemClick(View v){
        final CheckBox cb = (CheckBox) findViewById(R.id.select_all_cb);
        final Button done = (Button) findViewById(R.id.Done);
        Item item = items.get(v.getId());
        boolean allChecked = true;
        boolean allUnChecked = true;
        if(cb.isChecked() && item.getChecked()){
            cb.setChecked(false);
        }
        item.toggleChecked();
        for(Item i : items){
            if(!i.getChecked()){
                allChecked = false;
            }
            else{
                allUnChecked = false;
            }
        }
        if(allChecked){
            cb.setChecked(true);
        }
        if(allUnChecked){
            done.setEnabled(false);
        }
        if(item.getChecked()){
            done.setEnabled(true);
        }
        customAdapter.notifyDataSetChanged();
    }

    public void setTitle(Bundle savedInstanceState){
        String title;
        Bundle extras = getIntent().getExtras();
        if (savedInstanceState == null) {
            title = getIntent().getStringExtra("com.ShoppingList.MESSAGE");
        }
        else {
            title = extras.getString("com.ShoppingList.MESSAGE");
        }
        setTitle(title);
    }

    public void readSavedItems(){
        try {
            File file = new File(getFilesDir().getPath() + "/Aisle_Share_Data.json");
            // Read or Initializes aisleShareData
            // Assumes the File itself has already been Initialized
            aisleShareData = new JSONObject(loadJSONFromAsset(file));
            currentOrder = aisleShareData.optJSONObject("Transfers").getInt("sort");
            isIncreasingOrder = aisleShareData.optJSONObject("Transfers").getBoolean("direction");
            listName = aisleShareData.optJSONObject("Transfers").getString("name");
            JSONArray read_items = aisleShareData.optJSONObject("Transfers").getJSONArray("items");
            for(int index = 0; index < read_items.length(); index++){
                try {
                    JSONObject obj = new JSONObject(read_items.get(index).toString());
                    items.add(new Item(
                            obj.getString("owner"),
                            obj.getString("name"),
                            obj.getString("type"),
                            obj.getDouble("quantity"),
                            obj.getString("units"),
                            false,
                            obj.getLong("timeCreated")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String loadJSONFromAsset(File f) {
        String json;
        try {
            FileInputStream fis = new FileInputStream(f);
            int bytes = fis.available();
            byte[] buffer = new byte[bytes];
            fis.read(buffer, 0, bytes);
            fis.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    public void setListeners() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                itemClick(view);
            }
        });

        final Button cancel = (Button) findViewById(R.id.Cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final Button done = (Button) findViewById(R.id.Done);
        done.setEnabled(false);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean someChecked = false;
                for (Item i : items) {
                    if (i.getChecked()) {
                        i.toggleChecked();
                        aisleShareData.optJSONObject("Lists").optJSONObject(listName).
                                optJSONArray("items").put(i.getJSONString());
                        someChecked = true;
                    }
                }

                try {
                    FileOutputStream fos = new FileOutputStream(getFilesDir().getPath() + "/Aisle_Share_Data.json");
                    fos.write(aisleShareData.toString().getBytes());
                    fos.close();

                    if (someChecked) {
                        Toast toast = Toast.makeText(Transfer.this, "Items Saved to List", Toast.LENGTH_LONG);
                        toast.show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                finish();
            }
        });

        final CheckBox cb = (CheckBox) findViewById(R.id.select_all_cb);
        cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAllToggle();
            }
        });

        final TextView tv = (TextView) findViewById(R.id.select_all);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cb.toggle();
                selectAllToggle();
            }
        });

        final LinearLayout ll = (LinearLayout) findViewById(R.id.LinearLayout);
        ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cb.toggle();
                selectAllToggle();
            }
        });
    }

    public void minusClick(View v){
        double quantity = items.get(v.getId()).getQuantity();
        if (quantity > 1) {
            items.get(v.getId()).setQuantity(quantity - 1);
        }
        customAdapter.notifyDataSetChanged();
    }

    public void plusClick(View v){
        double quantity = items.get(v.getId()).getQuantity();
        if (quantity < 99999) {
            items.get(v.getId()).setQuantity(quantity + 1);
        }
        customAdapter.notifyDataSetChanged();
    }

    public void selectAllToggle(){
        final Button done = (Button) findViewById(R.id.Done);
        final CheckBox cb = (CheckBox) findViewById(R.id.select_all_cb);
        if(cb.isChecked()){
            done.setEnabled(true);
            for(Item i: items){
                i.setChecked(true);
            }
        }
        else{
            done.setEnabled(false);
            for(Item i: items){
                i.setChecked(false);
            }
        }
        customAdapter.notifyDataSetChanged();
    }
}