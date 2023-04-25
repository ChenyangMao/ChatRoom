package cn.edu.sustech.cs209.chatting.server;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserList {

  private List<User> userList = new ArrayList<>();
  public List<String> onlineList = new ArrayList<>();
  public Map<String, User> findUser = new HashMap<>();
  public Map<String, Socket> findSocket = new HashMap<>();
  public Map<String, Socket> findFileSocket = new HashMap<>();
  public Map<String, List<String>> groupList = new HashMap<>();

  public synchronized void addUser(User user) {
    userList.add(user);
    findUser.put(user.getUsername(), user);
  }

  public synchronized void addOnlineUser(String user) {
    onlineList.add(user);
  }

  public synchronized void removeOnlineUser(String user) {
    onlineList.remove(user);
  }

  public synchronized List<User> getUserList() {
    return userList;
  }

  public synchronized List<String> getonlineList() {
    return onlineList;
  }

  public synchronized boolean containsUser(String username) {
    for (User user : userList) {
      if (user.getUsername().equals(username)) {
        return true;
      }
    }
    return false;
  }

  public synchronized boolean containsOnlineUser(String username) {
    for (String user : onlineList) {
      if (user.equals(username)) {
        return true;
      }
    }
    return false;
  }
}
