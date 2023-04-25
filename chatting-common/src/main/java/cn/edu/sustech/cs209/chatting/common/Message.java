package cn.edu.sustech.cs209.chatting.common;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Scanner;

public class Message implements Runnable, Serializable {

    private Long timestamp;

    private String sentBy;

    private String sendTo;

    private String data;

    private String chatName;

    private int type;

    public List<String> members;

    public Message(Long timestamp, String sentBy, String sendTo, String data,String chatName,int type) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
        this.chatName = chatName;
        this.type = type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    public String getChatName(){
        return chatName;
    }

    public int getType(){
        return type;
    }

    public void setMembers(List<String> members){
        this.members = members;
    }

    public void run(){

   }
}
