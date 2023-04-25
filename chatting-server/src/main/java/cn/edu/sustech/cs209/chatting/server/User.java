package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User{
    private String name;
    private String password;
    boolean isOnline;
    protected List<String> chatted = new ArrayList<>();
    protected Map<String, List<Message>> messages = new HashMap<>();
    public User(String name,String password){
        this.name = name;
        this.password = password;
    }

    public String getUsername(){
        return name;
    }

    public List<String> getChatted(){
        return chatted;
    }

    public String getPassword(){
        return password;
    }
}
