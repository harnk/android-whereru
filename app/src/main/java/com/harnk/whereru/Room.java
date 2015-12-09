package com.harnk.whereru;

/**
 * Created by scottnull on 12/7/15.
 */
public class Room {

    private String roomName;
    private String memberNickName;
    private String memberLocation;
    private String memberUpdateTime;
    private String memberPinImage;
    //Empty constructor
    public Room() {

    }
    //Constructor with params
//    -(id)initWithRoomName:(NSString *)rName andMemberNickName:(NSString *)mNickName andMemberLocation:(NSString *)mLocation
//    andMemberLocTime:(NSString *)mLocTime andMemberPinImage:(NSString *)mPinImageString;
    public Room(String roomName, String memberNickName, String memberLocation, String memberUpdateTime){
        this.roomName = roomName;
        this.memberNickName = memberNickName;
        this.memberLocation = memberLocation;
        this.memberUpdateTime = memberUpdateTime;
        String[] myPinImages = new String[]{"blue","cyan","darkgreen","gold","green","orange","pink","purple","red","yellow","cyangray"};
        char ch = memberNickName.charAt(0);
        int asciiCode = (int) ch;
        int digit = asciiCode % 10;
        this.memberPinImage = myPinImages[digit];
//        int resID = getResources().getIdentifier(myPinImages[digit], "drawable", getPackageName());

    }

    public String getRoomName() {
        return roomName;
    }

    public String getMemberNickName() {
        return memberNickName;
    }

    public String getMemberLocation() {
        return memberLocation;
    }

    public String getMemberUpdateTime() {
        return memberUpdateTime;
    }

    public String getMemberPinImage() {
        return memberPinImage;
    }
}
