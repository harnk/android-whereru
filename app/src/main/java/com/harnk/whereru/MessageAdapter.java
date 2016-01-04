package com.harnk.whereru;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

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

        Message message = objects.get(position);
        // assign the view we are converting to a local variable
        View view = convertView;
        String showDistance = "";

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

        if (message != null) {

            TextView senderInfo = (TextView) view.findViewById(R.id.senderNameDateDistanceTextView);
            TextView messageToShow = (TextView) view.findViewById(R.id.messageTextView);

            // check to see if each individual textview is null.
            // if not, assign some text!
            if (senderInfo != null){
                float distanceInYards = (float) (message.getDistanceFromMeInMeters() * 1.09361);
                float distanceInMiles = distanceInYards / 1760;


            if (distanceInYards > 500) {
                    showDistance = String.format("%.1f", distanceInMiles) + " miles";
                } else {
                    showDistance =  String.format("%.1f", distanceInYards) + " y";
                }
            }
            if (messageToShow != null){
                messageToShow.setText(message.getText());
            }

            //Check whether message is mine to show green background and align to right
//            if(message.isMine())
            DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
            if (message.getSenderName().equals(deviceSingleton.getNickname())) {
                messageToShow.setBackgroundResource(R.drawable.bubble_right_hand);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)messageToShow.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                messageToShow.setLayoutParams(params); //causes layout update
                senderInfo.setGravity(Gravity.RIGHT);
                try {
                    senderInfo.setText(" You " + formatDateTime(message.getDate()));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            } else {
                //If not mine then it is from sender to show orange background and align to left

                messageToShow.setBackgroundResource(R.drawable.bubble_left_hand);
                messageToShow.setGravity(Gravity.LEFT);
                senderInfo.setGravity(Gravity.LEFT);
                try {
                    senderInfo.setText(message.getSenderName() + " " + formatDateTime(message.getDate()) + ", " + showDistance);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
//            messageToShow.setTextColor(R.color.textColor);


        }

        // the view must be returned to our activity
        return view;

    }

    private boolean isToday(String dateStringToCheck){
        try{
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Date checkDate = formatter.parse(dateStringToCheck);
            Date todaysDate = formatter.parse(today);

            if (checkDate.compareTo(todaysDate)<0) {
//                Log.d("SCXTT", "today is Greater than dateStringToCheck: " + checkDate.compareTo(todaysDate));
                return false;
            } else {
                return true;
            }

        }catch (ParseException e1){
            e1.printStackTrace();
        }
        return true;
    }

    private boolean isYesterday(String dateStringToCheck){
        return false;
    }

    private String formatDateTime(String inputDateTime) throws ParseException {
        // formatDateTime received: 2015-11-26 01:58:37
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date myDate = simpleDateFormat.parse(inputDateTime);
        DateFormat outputFormat = new SimpleDateFormat("M/d/yy, h:mm aa");
        DateFormat todayFormat = new SimpleDateFormat("h:mm aa");
        String outputDateStr = "";
        if (isToday(inputDateTime.split(" ")[0])){
            outputDateStr = "Today, " + todayFormat.format(myDate);
        } else {
            outputDateStr = outputFormat.format(myDate);
        }
        return outputDateStr;
    }
}
