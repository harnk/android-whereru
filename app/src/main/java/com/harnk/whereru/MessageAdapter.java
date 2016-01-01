package com.harnk.whereru;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

/**
 * Created by scottnull on 1/1/16.
 */
public class MessageAdapter extends ArrayAdapter<Message> {

    Context context;

    public MessageAdapter(Context context, int resource, Message[] objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return super.getView(position, convertView, parent);
    }
}
