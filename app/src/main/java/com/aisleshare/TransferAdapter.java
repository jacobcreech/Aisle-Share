package com.aisleshare;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class TransferAdapter extends ArrayAdapter {
    private ArrayList<Item> items = null;
    private Context context;
    private int layout;
    private int primary_layout;

    public TransferAdapter(Context context, ArrayList<Item> items, int layout) {
        super(context,layout,items);
        this.context = context;
        this.items = items;
        this.primary_layout = layout;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Is it an Item or Header
        if(items.get(position).isItem()) {
            layout = primary_layout;
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            convertView = inflater.inflate(layout, parent, false);

            LinearLayout row = (LinearLayout) convertView.findViewById(R.id.row);
            CheckBox cb = (CheckBox) convertView.findViewById(R.id.checkBox);
            TextView name = (TextView) convertView.findViewById(R.id.name);
            TextView type = (TextView) convertView.findViewById(R.id.type);
            Button minus = (Button) convertView.findViewById(R.id.Minus);
            TextView quantity = (TextView) convertView.findViewById(R.id.quantity);
            TextView units = (TextView) convertView.findViewById(R.id.units);
            Button plus = (Button) convertView.findViewById(R.id.Plus);

            boolean hasType = items.get(position).getType().equals("");
            boolean hasUnits = items.get(position).getUnits().equals("");

            // Frame
            row.setId(position);

            // Checked
            cb.setChecked(items.get(position).getChecked());
            cb.setId(position);

            // Name
            name.setText(items.get(position).getName());
            name.setId(position);

            // Type
            if (!hasType) {
                type.setText(items.get(position).getType());
                type.setId(position);
            } else {
                type.setVisibility(View.GONE);
            }

            // Minus
            minus.setId(position);

            // Quantity
            Double value = items.get(position).getQuantity();
            if (value % 1 == 0) {
                quantity.setText(Integer.toString((int) Math.round(value)));
            } else {
                quantity.setText(Double.toString(value));
            }
            quantity.setId(position);

            // Units
            if (!hasUnits) {
                units.setText(items.get(position).getUnits());
                units.setId(position);
            } else {
                units.setVisibility(View.GONE);
            }

            // Minus
            plus.setId(position);
        }
        else {
            layout = R.layout.row_header;
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            convertView = inflater.inflate(layout, parent, false);

            RelativeLayout row = (RelativeLayout) convertView.findViewById(R.id.row);
            row.setId(position);
            row.setTag("Disable Swipe");

            TextView name = (TextView) convertView.findViewById(R.id.separator);
            name.setText(items.get(position).getName());
            name.setId(position);
            name.setTag("Disable Swipe");

            if (items.get(position).showTrash()) {
                ImageView trash = (ImageView) convertView.findViewById(R.id.trash);
                trash.setVisibility(View.VISIBLE);
                trash.setId(position);
                trash.setTag("Disable Swipe");
            }
        }

        return convertView;
    }
}
