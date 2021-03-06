package com.aisleshare;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.getbase.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.Collections;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Activities extends Fragment {
    public final static String ACTIVITY_NAME = "com.ShoppingList.MESSAGE";
    private ListView listView;
    private ArrayList<ListItem> activities;
    private Map<String, MenuItem> menuActivities;
    private ListItemAdapter itemAdapter;
    private boolean isIncreasingOrder;
    private int currentOrder;
    private Context dashboard;
    private TextView emptyNotice;
    private String deviceName;
    private JSONObject aisleShareData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activities, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        dashboard = getActivity();
        listView = (ListView) getView().findViewById(R.id.activities);
        activities = new ArrayList<>();
        menuActivities = new HashMap<>();
        isIncreasingOrder = true;
        currentOrder = 1;
        emptyNotice = (TextView) getView().findViewById(R.id.empty_notice);
        deviceName = Settings.Secure.getString(dashboard.getContentResolver(), Settings.Secure.ANDROID_ID);

        readSavedData();

        if(activities.size() == 0) {
            emptyNotice.setVisibility(View.VISIBLE);
        }

        itemAdapter = new ListItemAdapter(dashboard, activities, R.layout.row_dashboard);
        listView.setAdapter(itemAdapter);

        setListeners();
    }

    private void sortActivity(boolean reverseOrder, int order) {
        if(reverseOrder) {
            isIncreasingOrder = !isIncreasingOrder;
        }
        if(order != currentOrder){
            currentOrder = order;
            isIncreasingOrder = true;
        }

        ListItemComparator compare = new ListItemComparator(dashboard);

        switch (currentOrder){
            // Name
            case 0:{
                ListItemComparator.Name sorter = compare.new Name();
                Collections.sort(activities, sorter);
                break;}
            // Time Created
            case 1:{
                ListItemComparator.Created sorter = compare.new Created();
                Collections.sort(activities, sorter);
                break;}
            // Owner
            case 2:{
                ListItemComparator.Owner sorter = compare.new Owner();
                Collections.sort(activities, sorter);
                break;}
        }

        if(!isIncreasingOrder) {
            Collections.reverse(activities);
        }
        saveSortInfo();
    }

    private void setSortIcons(){
        if(isIncreasingOrder) {
            menuActivities.get("sort").setIcon(R.mipmap.inc_sort);
        }
        else{
            menuActivities.get("sort").setIcon(R.mipmap.dec_sort);
        }
    }

    // Popup for adding an Activity
    private void addActivityDialog(){
        // custom dialog
        final Dialog dialog = new Dialog(dashboard);
        dialog.setContentView(R.layout.dialog_add_name);
        dialog.setTitle("Add a New Activity");

        final EditText activityName = (EditText) dialog.findViewById(R.id.Name);
        final Button cancel = (Button) dialog.findViewById(R.id.Cancel);
        final Button done = (Button) dialog.findViewById(R.id.Done);

        // Open keyboard automatically
        activityName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!activityName.getText().toString().isEmpty()) {
                    String name = activityName.getText().toString();

                    for (int index = 0; index < activities.size(); index++) {
                        if (activities.get(index).getName().equals(name)) {
                            activityName.setError("Activity already exists");
                            return;
                        }
                    }
                    ListItem activity = new ListItem(deviceName, name);
                    dialog.dismiss();
                    activities.add(activity);
                    sortActivity(false, currentOrder);
                    itemAdapter.notifyDataSetChanged();
                    saveNewActivity(name, activity.getCreated());
                    emptyNotice.setVisibility(View.INVISIBLE);

                    Intent intent = new Intent(dashboard, CurrentActivity.class);
                    intent.putExtra(ACTIVITY_NAME, name);
                    startActivity(intent);
                } else {
                    activityName.setError("Name is empty");
                }
            }
        });

        dialog.show();
    }

    // Popup for editing a Activity
    private void editActivityDialog(final int position){
        if(!deviceName.equals(activities.get(position).getOwner())) {
            Toast toast = Toast.makeText(dashboard, "You are not the owner", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        // custom dialog
        final Dialog dialog = new Dialog(dashboard);
        dialog.setContentView(R.layout.dialog_add_name);
        dialog.setTitle("Edit Activity Name");

        final EditText activityName = (EditText) dialog.findViewById(R.id.Name);
        final Button cancel = (Button) dialog.findViewById(R.id.Cancel);
        final Button done = (Button) dialog.findViewById(R.id.Done);
        final String orig_name = activities.get(position).getName();

        activityName.setText(orig_name);

        // Open keyboard automatically
        activityName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!activityName.getText().toString().isEmpty()) {
                    String name = activityName.getText().toString();

                    if (name.equals(orig_name)) {
                        dialog.dismiss();
                    }

                    for (int index = 0; index < activities.size(); index++) {
                        if (activities.get(index).getName().equals(name) && index != position) {
                            activityName.setError("Activity already exists");
                            return;
                        }
                    }

                    activities.get(position).setName(name);
                    sortActivity(false, currentOrder);
                    dialog.dismiss();
                    itemAdapter.notifyDataSetChanged();

                    try {
                        // Need to update other fragments before saving
                        File file = new File(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
                        aisleShareData = new JSONObject(loadJSONFromAsset(file));

                        JSONObject activityData = aisleShareData.optJSONObject("Activities").optJSONObject(orig_name);
                        aisleShareData.optJSONObject("Activities").remove(orig_name);
                        aisleShareData.optJSONObject("Activities").put(name, activityData);

                        FileOutputStream fos = new FileOutputStream(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
                        fos.write(aisleShareData.toString().getBytes());
                        fos.close();
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    activityName.setError("Name is empty");
                }
            }
        });

        dialog.show();
    }

    private void readSavedData(){
        try {
            File file = new File(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
            // Read or Initializes aisleShareData
            // Assumes the File itself has already been Initialized
            aisleShareData = new JSONObject(loadJSONFromAsset(file));
            JSONArray activityNames = aisleShareData.optJSONObject("Activities").names();
            currentOrder = aisleShareData.optInt("ActivitiesSort");
            isIncreasingOrder = aisleShareData.optBoolean("ActivitiesDirection");
            if(activityNames != null) {
                for (int i = 0; i < activityNames.length(); i++) {
                    try {
                        JSONObject entry = aisleShareData.optJSONObject("Activities").optJSONObject(activityNames.get(i).toString());
                        if(entry != null) {
                            String owner = entry.optString("owner");
                            long created = entry.optLong("time");
                            activities.add(new ListItem(owner, activityNames.get(i).toString(), created));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            sortActivity(false, currentOrder);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String loadJSONFromAsset(File f) {
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

    private void saveSortInfo(){
        try {
            // Need to update other fragments before saving
            File file = new File(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
            aisleShareData = new JSONObject(loadJSONFromAsset(file));

            aisleShareData.put("ActivitiesSort", currentOrder);
            aisleShareData.put("ActivitiesDirection", isIncreasingOrder);

            FileOutputStream fos = new FileOutputStream(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
            fos.write(aisleShareData.toString().getBytes());
            fos.close();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private void saveNewActivity(String activityTitle, long timeCreated){
        try {
            // Need to update other fragments before saving
            File file = new File(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
            aisleShareData = new JSONObject(loadJSONFromAsset(file));

            aisleShareData.optJSONObject("Activities").accumulate(activityTitle, new JSONObject());
            aisleShareData.optJSONObject("Activities").optJSONObject(activityTitle).accumulate("items", new JSONArray());
            aisleShareData.optJSONObject("Activities").optJSONObject(activityTitle).accumulate("category", new JSONArray());
            aisleShareData.optJSONObject("Activities").optJSONObject(activityTitle).accumulate("sort", 2);
            aisleShareData.optJSONObject("Activities").optJSONObject(activityTitle).accumulate("direction", true);
            aisleShareData.optJSONObject("Activities").optJSONObject(activityTitle).accumulate("time", timeCreated);
            aisleShareData.optJSONObject("Activities").optJSONObject(activityTitle).accumulate("owner", deviceName);

            FileOutputStream fos = new FileOutputStream(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
            fos.write(aisleShareData.toString().getBytes());
            fos.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void removeActivity(String activityTitle){
        try {
            // Need to update other fragments before saving
            File file = new File(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
            aisleShareData = new JSONObject(loadJSONFromAsset(file));

            aisleShareData.optJSONObject("Activities").remove(activityTitle);

            FileOutputStream fos = new FileOutputStream(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
            fos.write(aisleShareData.toString().getBytes());
            fos.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuActivities.put("sort", menu.findItem(R.id.sort_root));
        menuActivities.put("name", menu.findItem(R.id.sort_name));
        menuActivities.put("time", menu.findItem(R.id.sort_time));
        menuActivities.put("owner", menu.findItem(R.id.sort_owner));
        menuActivities.put("delete", menu.findItem(R.id.delete));

        menuActivities.get("name").setCheckable(true);
        menuActivities.get("time").setCheckable(true);
        menuActivities.get("owner").setCheckable(true);

        setSortIcons();
        switch (currentOrder){
            case 0:
                menuActivities.get("name").setChecked(true);
                break;
            case 1:
                menuActivities.get("time").setChecked(true);
                break;
            case 2:
                menuActivities.get("owner").setChecked(true);
                break;
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem option) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = option.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id) {
            case R.id.sort_name:
                sortActivity(true, 0);
                setSortIcons();
                clearMenuCheckables();
                option.setChecked(true);
                break;
            case R.id.sort_time:
                sortActivity(true, 1);
                setSortIcons();
                clearMenuCheckables();
                option.setChecked(true);
                break;
            case R.id.sort_owner:
                sortActivity(true, 2);
                setSortIcons();
                clearMenuCheckables();
                option.setChecked(true);
                break;
            case R.id.delete:
                deleteItems();
                break;
            case R.id.sort:
                return super.onOptionsItemSelected(option);
        }
        itemAdapter.notifyDataSetChanged();
        return super.onOptionsItemSelected(option);
    }

    private void clearMenuCheckables(){
        menuActivities.get("name").setChecked(false);
        menuActivities.get("time").setChecked(false);
        menuActivities.get("owner").setChecked(false);
    }

    private AlertDialog confirmDeletion(final int position)
    {
        final String activityName = activities.get(position).getName();
        return new AlertDialog.Builder(dashboard)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure? This cannot be undone.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        emptyNotice = (TextView) getView().findViewById(R.id.empty_notice);

                        removeActivity(activityName);
                        activities.remove(position);
                        itemAdapter.notifyDataSetChanged();
                        dialog.dismiss();
                        Toast toast = Toast.makeText(dashboard, "Activity Deleted", Toast.LENGTH_LONG);
                        toast.show();
                        if (activities.size() == 0) {
                            emptyNotice.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    private void deleteItems(){
        // custom dialog
        final Dialog dialog = new Dialog(dashboard);
        dialog.setContentView(R.layout.dialog_select_list);

        final ListView lv = (ListView) dialog.findViewById(R.id.lists);
        final Button cancel = (Button) dialog.findViewById(R.id.cancel);

        final ArrayList<String> activityNames = new ArrayList<>();
        if(activities.size() != 0) {
            dialog.setTitle("What Should We Delete?");
            for (ListItem i : activities) {
                activityNames.add(i.getName());
            }
        }
        else{
            dialog.setTitle("No Activities to Delete.");
        }

        ArrayAdapter<String> itemAdapter = new ArrayAdapter<>(dashboard,android.R.layout.simple_list_item_1, activityNames);
        lv.setAdapter(itemAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDeletion(position).show();
                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            // Floating Action Button
            FloatingActionButton addButton = (FloatingActionButton) getActivity().findViewById(R.id.float_button);
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addActivityDialog();
                }
            });
        }
    }

    private void setListeners() {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                Intent intent = new Intent(dashboard, CurrentActivity.class);
                String name = activities.get(pos).getName();
                intent.putExtra(ACTIVITY_NAME, name);
                startActivity(intent);
            }
        });

        // Long Click opens contextual menu
        registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context_menu_dash, menu);
        menu.findItem(R.id.add_to_list).getSubMenu().findItem(R.id.addRecipe).setVisible(false);
        menu.findItem(R.id.add_to_list).getSubMenu().findItem(R.id.addActivity).setVisible(false);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        if (!getUserVisibleHint()) {
            return false;
        }

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        int index = info.position;

        switch (menuItem.getItemId()) {
            case R.id.edit:
                editActivityDialog(index);
                return true;
            case R.id.add_to_list:
                addToListDialog(activities.get(index).getName());
                return true;
            case R.id.delete:
                confirmDeletion(index).show();
                return true;
            case R.id.cancel:
                return super.onContextItemSelected(menuItem);
            default:
                return super.onContextItemSelected(menuItem);
        }
    }

    // Popup for adding an Item
    private void addToListDialog(final String activityTitle) {
        // Need to update other fragments before saving
        try {
            File file = new File(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
            aisleShareData = new JSONObject(loadJSONFromAsset(file));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // custom dialog
        final Dialog dialog = new Dialog(dashboard);
        dialog.setContentView(R.layout.dialog_select_list);
        dialog.setTitle("Select a List");

        final ListView lv = (ListView) dialog.findViewById(R.id.lists);
        final Button cancel = (Button) dialog.findViewById(R.id.cancel);

        final ArrayList<String> entries = new ArrayList<>();
        JSONArray names = aisleShareData.optJSONObject("Lists").names();
        if(names != null) {
            for (int i = 0; i < names.length(); i++) {
                try {
                    entries.add(names.get(i).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        else{
            return;
        }

        ArrayAdapter<String> itemAdapter = new ArrayAdapter<>(dashboard,android.R.layout.simple_list_item_1, entries);
        lv.setAdapter(itemAdapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String name = entries.get(position);
                JSONArray sel_items = new JSONArray();
                try {
                    int sel_currentOrder = aisleShareData.optJSONObject("Activities").optJSONObject(activityTitle).optInt("sort");
                    boolean sel_isIncreasingOrder = aisleShareData.optJSONObject("Activities").optJSONObject(activityTitle).optBoolean("order");
                    sel_items = aisleShareData.optJSONObject("Activities").optJSONObject(activityTitle).optJSONArray("items");

                    aisleShareData.optJSONObject("Transfers").put("sort", sel_currentOrder);
                    aisleShareData.optJSONObject("Transfers").put("order", sel_isIncreasingOrder);
                    aisleShareData.optJSONObject("Transfers").put("name", name);
                    aisleShareData.optJSONObject("Transfers").remove("items");
                    aisleShareData.optJSONObject("Transfers").accumulate("items", new JSONArray());
                    for (int index = 0; index < sel_items.length(); index++) {
                        aisleShareData.optJSONObject("Transfers").optJSONArray("items").put(sel_items.get(index));
                    }
                    FileOutputStream fos = new FileOutputStream(dashboard.getFilesDir().getPath() + "/Aisle_Share_Data.json");
                    fos.write(aisleShareData.toString().getBytes());
                    fos.close();
                } catch (JSONException | IOException e) {
                    e.printStackTrace();
                }
                dialog.dismiss();

                if (sel_items.length() > 0) {
                    Intent intent = new Intent(dashboard, Transfer.class);
                    intent.putExtra(ACTIVITY_NAME, "Select which Items to Add");
                    startActivity(intent);
                } else {
                    Toast toast = Toast.makeText(dashboard, activityTitle + " is empty", Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
