package com.harnk.whereru;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by scottnull on 1/1/16.
 */
public class MessageAdapter extends ArrayAdapter<Message> {

    private ArrayList<Message> objects;

    public MessageAdapter(Context context, int textViewResourceId, ArrayList<Message> objects) {
        super(context, textViewResourceId, objects);
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        // assign the view we are converting to a local variable
        View view = convertView;

        // first check to see if the view is null. if so, we have to inflate it.
        // to inflate it basically means to render, or show, the view.
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.single_row, null);
        }

		/*
		 * Recall that the variable position is sent in as an argument to this method.
		 * The variable simply refers to the position of the current object in the list. (The ArrayAdapter
		 * iterates through the list we sent it)
		 *
		 * Therefore, i refers to the current Item object.
		 */
        Message message = objects.get(position);

        if (message != null) {

            TextView senderInfo = (TextView) view.findViewById(R.id.senderNameDateDistanceTextView);
            TextView messageToShow = (TextView) view.findViewById(R.id.messageTextView);
            ImageView bubbleImage = (ImageView) view.findViewById(R.id.bubbleImageView);

            // check to see if each individual textview is null.
            // if not, assign some text!
            if (senderInfo != null){
                float distanceInYards = (float) (message.getDistanceFromMeInMeters() * 1.09361);
                float distanceInMiles = distanceInYards / 1760;
                String showDistance;

            if (distanceInYards > 500) {
                    showDistance = String.format("%.1f", distanceInMiles) + " mi";
                } else {
                    showDistance =  String.format("%.1f", distanceInYards) + " y";
                }
                senderInfo.setText(message.getSenderName() + " DATE, " + showDistance);
            }
            if (messageToShow != null){
                messageToShow.setText(message.getText());
            }
            if (bubbleImage != null){
//                mt.setText("Price: ");
            }
        }

        // the view must be returned to our activity
        return view;

    }
}
