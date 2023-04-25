package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class MultiThreadedServer implements Runnable {

  protected int serverPort = 8080;
  protected ServerSocket serverSocket = null;
  protected boolean isStopped = false;
  public UserList userList = new UserList();

  public MultiThreadedServer(int port) {
    this.serverPort = port;
  }

  public void run() {
    openServerSocket();
    while (!isStopped()) {
      Socket clientSocket = null;
      try {
        clientSocket = this.serverSocket.accept();
      } catch (IOException e) {
        if (isStopped()) {
          System.out.println("Server stopped.");
          return;
        }
        throw new RuntimeException("Error accepting client connection", e);
      }
      new Thread(new WorkerRunnable(clientSocket, userList)).start();
    }
    System.out.println("Server stopped.");
  }

  private synchronized boolean isStopped() {
    return this.isStopped;
  }

  public synchronized void stop() {
    this.isStopped = true;
    try {
      this.serverSocket.close();
    } catch (IOException e) {
      throw new RuntimeException("Error closing server", e);
    }
  }

  private void openServerSocket() {
    try {
      this.serverSocket = new ServerSocket(this.serverPort);
    } catch (IOException e) {
      throw new RuntimeException("Cannot open port " + this.serverPort, e);
    }
  }

  public static void main(String[] args) {
    MultiThreadedServer server = new MultiThreadedServer(1234);
    new Thread(server).start();
    FileServer fileServer = new FileServer(1235, server.userList);
    new Thread(fileServer).start();

  }
}

class WorkerRunnable implements Runnable {

  protected Socket clientSocket = null;
  protected String user_name;
  InputStream inputStream;
  BufferedReader reader;
  OutputStream outputStream;
  PrintStream out;
  User user;
  UserList userList;
  boolean isExit = false;
  Timer timer = new Timer();

  public WorkerRunnable(Socket clientSocket, UserList userList) {
    this.clientSocket = clientSocket;
    this.userList = userList;
  }

  public void run() {
    try {
      inputStream = this.clientSocket.getInputStream();
      reader = new BufferedReader(new InputStreamReader(inputStream));
      String client_name = reader.readLine();
      String client_password = reader.readLine();
      outputStream = this.clientSocket.getOutputStream();
      out = new PrintStream(outputStream);

      if (!userList.containsUser(client_name)) {
        user = new User(client_name, client_password);
        user_name = user.getUsername();
        userList.addUser(user);
        userList.addOnlineUser(user_name);
        System.out.println("Add new client: " + client_name);
        userList.findUser.put(user_name, user);
        userList.findSocket.put(user_name, clientSocket);
        out.println("not");
      } else {
        if (!userList.containsOnlineUser(client_name)) {
          user = userList.findUser.get(client_name);
          if (user.getPassword().equals(client_password)) {
            user_name = user.getUsername();
            userList.addOnlineUser(user_name);
            System.out.println(client_name + " log in");
            userList.findSocket.remove(user_name);
            userList.findSocket.put(user_name, clientSocket);
            out.println("log in");
          } else {
            System.out.println("Client already exists: " + client_name);
            out.println("exist");
          }
        } else {
          System.out.println("Client already exists: " + client_name);
          out.println("exist");
        }
      }
      while (!isExit) {
        if (reader.ready()) {
          String message = reader.readLine();
          if (message.equals("getChatList")) {
            getOnlineList();
          } else if (message.equals("getChatted")) {
            getChatted(user);
          } else if (message.equals("addChatted")) {
            addChatted(user);
          } else if (message.equals("getMessages")) {
            getMessages(user);
          } else if (message.equals("addMessages")) {
            addMessages(user);
          } else if (message.equals("setMembers")) {
            setMembers();
          } else if (message.equals("getMembers")) {
            getMembers();
          } else if (message.equals("setInvited")) {
            setInvited();
          } else if (message.equals("exit")) {
            System.out.println(user_name + " exit");
            userList.removeOnlineUser(user_name);
            isExit = true;
          } else if (message.equals("isOnline")) {
            out.println("Online");
          }
        }
        Thread.sleep(100);
      }
    } catch (IOException | InterruptedException e) {
//            System.out.println("InterruptedException");
    } finally {
      try {
        if (user_name != null && !isExit) {
          System.out.println(user_name + " offline");
          userList.removeOnlineUser(user_name);
        }
        clientSocket.close();
      } catch (IOException e) {
        System.out.println(user_name + ": Fail close clientSocket");
      }
    }

  }

  public void getOnlineList() {
    if (!out.checkError()) {
      List<String> list = userList.getonlineList();
      out.println(String.join(",", list));
    } else {
      System.out.println(user_name + ": Fail getOnlineList");
    }
  }

  public void getChatted(User user) {
    List<String> list = user.getChatted();
    if (!out.checkError()) {
      if (list.isEmpty()) {
        out.println("no data");
      } else {
        out.println(String.join(",", list));
      }
    } else {
      System.out.println(user_name + ": Fail getChatted");
    }
  }

  public void addChatted(User user) {
    try {
      String name = reader.readLine();
      user.chatted.add(name);
    } catch (IOException e) {
      System.out.println(user_name + ": Fail addChatted");
    }
  }

  public void getMessages(User user) {
    Map<String, List<Message>> messages = user.messages;
    if (!out.checkError()) {
      if (messages.size() == 0) {
        out.println("nothing");
      } else {
        for (String key : messages.keySet()) {
          List<Message> list = messages.get(key);
          for (Message message : list) {
            long timestamp = message.getTimestamp();
            out.println(timestamp);
            String by = message.getSentBy();
            out.println(by);
            String to = message.getSendTo();
            out.println(to);
            String data = message.getData();
            out.println(data);
            String chatName = message.getChatName();
            out.println(chatName);
            int type = message.getType();
            out.println(type);
          }
        }
      }
    } else {
      System.out.println(user_name + ": Fail getMessages");
    }
  }

  public void addMessages(User user) throws IOException {
    int flag = 0;
    long timeStamp = 0;
    String data = null;
    String to = null;
    String chatName = null;
    int type = -1;
    while (flag < 5) {
      if (reader.ready()) {
        String str = reader.readLine();
        if (flag == 0) {
          timeStamp = Long.parseLong(str);
        } else if (flag == 1) {
          data = str;
        } else if (flag == 2) {
          to = str;
        } else if (flag == 3) {
          chatName = str;
        } else {
          type = Integer.parseInt(str);
        }
        flag++;
      }
    }
    Message newMessage = new Message(timeStamp, user.getUsername(), to, data, chatName, type);
    if (!user.messages.containsKey(newMessage.getSendTo())) {
      List<Message> m = new ArrayList<>();
      m.add(newMessage);
      user.messages.put(newMessage.getSendTo(), m);
    } else {
      List<Message> m = user.messages.get(newMessage.getSendTo());
      m.add(newMessage);
      user.messages.put(newMessage.getSendTo(), m);
    }

    String testTo = newMessage.getSendTo();
    User receiver = userList.findUser.get(testTo);
//                User receiver = userList.findUser.get(newMessage.getSendTo());
    if (!receiver.messages.containsKey(newMessage.getSentBy())) {
      List<Message> m = new ArrayList<>();
      m.add(newMessage);
      receiver.messages.put(newMessage.getSentBy(), m);
    } else {
      List<Message> m = receiver.messages.get(newMessage.getSentBy());
      m.add(newMessage);
      receiver.messages.put(newMessage.getSentBy(), m);
    }
    receiveMessage(timeStamp, data, user.getUsername(), to, chatName, type);

  }

  public void receiveMessage(long timeStamp, String data, String by, String to, String chatName,
      int type) {
    try {
      OutputStream outputStream1 = null;
      Socket receiverSocket = userList.findSocket.get(to);
      outputStream1 = receiverSocket.getOutputStream();

      PrintStream out1 = new PrintStream(outputStream1);
      if (!out1.checkError()) {
        out1.println("newMessage");
        out1.println(timeStamp);
        out1.println(data);
        out1.println(by);
        out1.println(chatName);
        out1.println(type);
//                out1.close();
      } else {
        System.out.println(user_name + ": Fail newMessage");
      }
    } catch (IOException e) {
      System.out.println(user_name + ": Fail receiveMessage <- " + to);
    }
  }

  public void setMembers() {
    int size = 0;
    int i = -2;
    String chatName = null;
    List<String> members = new ArrayList<>();
    try {
      while (i < size || size <= 0) {
        if (reader.ready()) {
          String str = reader.readLine();
          if (i == -2) {
            size = Integer.parseInt(str);
          } else if (i == -1) {
            chatName = str;
          } else {
            members.add(str);
          }
          i++;
        }
      }
    } catch (IOException e) {
      System.out.println(user_name + ": Fail setMembers");
    }
    userList.groupList.put(chatName, members);
  }

  public void getMembers() {
    try {
      String chatName = reader.readLine();
      List<String> members = userList.groupList.get(chatName);
      out.println("");
      out.println("addMembers");
      out.println(members.size());
      for (String member : members) {
        out.println(member);
      }
    } catch (IOException e) {
      System.out.println(user_name + ": Fail getMembers");
    }
  }

  public void setInvited() {
    int type = -1;
    int flag = -2;
    int size = 0;
    String chatName = null;
    try {
      while (flag < 1) {
        if (reader.ready()) {
          String str = reader.readLine();
          if (flag == -2) {
            type = Integer.parseInt(str);
            if (type == 0) {
              flag = flag + 2;
            } else {
              flag++;
            }
          } else if (flag == -1) {
            size = Integer.parseInt(str);
            flag++;
          } else {
            chatName = str;
            flag++;
          }
        }
      }
    } catch (IOException e) {
      System.out.println("Fail setInvited when receiving: ");
    }

    if (type == 0) {
      try {
        Socket socket1 = userList.findSocket.get(chatName);
        OutputStream outputStream1 = socket1.getOutputStream();
        PrintStream out1 = new PrintStream(outputStream1);
        out1.println("beInvited");
        out1.println(type);
        out1.println(user_name);
//                out1.close();
      } catch (IOException e) {
        System.out.println(user_name + ": Fail setInvited when sending to private");
      }
    } else {
      try {
        List<String> list = userList.groupList.get(chatName);
        for (String s : list) {
          Socket socket1 = userList.findSocket.get(s);
          OutputStream outputStream1 = socket1.getOutputStream();
          PrintStream out1 = new PrintStream(outputStream1);
          out1.println("beInvited");
          out1.println(type);
          out1.println(chatName);
//                    out1.close();
        }
      } catch (IOException e) {
        System.out.println(user_name + ": Fail setInvited when sending to group");
      }
    }
  }

}
