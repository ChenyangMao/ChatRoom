package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;


import java.io.*;
import java.net.Socket;
import java.util.*;

public class ClientThread implements Runnable {
    protected Socket client;
    protected String name;
    protected String password;
    protected OutputStream os;
    PrintStream out;
    InputStream is;
    BufferedReader re;

    protected boolean isStopped = false;
    protected boolean isValid = true;
    protected List<String> chatList;
    public List<String> chatted = new ArrayList<>();
    protected String nowTo;//label of chat, not the person
    protected Map<String, Integer> findState = new HashMap<>();//String chatName, 0 is private, 1 is group
    protected List<String> members;
    protected List<String> newMessages = new ArrayList<>();
    protected Map<String, List<Message>> messages = new HashMap<>();//unique

    private ValueChangeListener listener;

    boolean isOnline = true;

    protected Socket fileSocket;
    protected DataOutputStream dos;
    protected DataInputStream dis;
    protected Map<String,List<String>> fileList = new HashMap<>();

    public ClientThread(Socket client, Socket fileSocket, String name, String password) {
        try {
            this.client = client;
            this.name = name;
            this.password = password;
            this.os = this.client.getOutputStream();
            out = new PrintStream(os);
            out.println(name);
            out.println(password);
            is = this.client.getInputStream();
            re = new BufferedReader(new InputStreamReader(is));
            this.isValid = checkValid();

            this.fileSocket = fileSocket;
            dos = new DataOutputStream(fileSocket.getOutputStream());
            dis = new DataInputStream(fileSocket.getInputStream());
            if(isValid) {
                dos.writeUTF(name);
            }
        } catch (IOException e) {
            System.out.println("Fail create clientThread");
        }

    }

    public void run() {
        try {
            System.out.println(name + " log in");
            getMessages();
            Thread thread = new Thread();
            while (!isStopped) {
                if (!thread.isAlive()) {
                    thread = new Thread(() -> {
                        checkOnline();
                        try {
                            Thread.sleep(1000);
                            if (!isOnline) {
                                if (listener != null) {
                                    listener.onIsOffLine();
                                }
                            }
                        } catch (InterruptedException e) {
                            System.out.println(name + ": Fail checkOnline");
                        }
                    });
                    thread.start();
                }
                try {
                    if (re.ready()) {
                        String str = re.readLine();
                        if (str.equals("newMessage")) {
                            int flag = 0;
                            long timeStamp = 0;
                            String data = null;
                            String by = null;
                            String chatName = null;
                            int type = -1;
                            try {
                                while (flag < 5) {
                                    if (re.ready()) {
                                        String temp = re.readLine();
                                        if (flag == 0) {
                                            timeStamp = Long.parseLong(temp);
                                        } else if (flag == 1) {
                                            data = temp;
                                        } else if (flag == 2) {
                                            by = temp;
                                        } else if (flag == 3) {
                                            chatName = temp;
                                        } else {
                                            type = Integer.parseInt(temp);
                                        }
                                        flag++;
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println("Fail newMessage");
                                stop();
                            }
                            Message message = new Message(timeStamp, by, name, data, chatName, type);
                            if (type == 0) {
                                if (!newMessages.contains(by)) {
                                    newMessages.add(by);
                                }
                            } else {
                                if (!newMessages.contains(chatName)) {
                                    newMessages.add(chatName);
                                }
                            }
                            if (!findState.containsKey(chatName)) {
                                findState.put(chatName, type);
                            }
                            receive(message);
                            if (listener != null) {
                                listener.onMessageChanged(message);
                            }
                        } else if (str.equals("addMembers")) {
                            int size = 0;
                            int i = -1;
                            List<String> members = new ArrayList<>();
                            try {
                                while (i < size || size <= 0) {
                                    if (re.ready()) {
                                        String temp = re.readLine();
                                        if (i == -1) {
                                            size = Integer.parseInt(temp);
                                        } else {
                                            members.add(temp);
                                        }
                                        i++;
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println("Fail addMembers");
                                stop();
                            }
                            this.members = members;
                        } else if (str.equals("beInvited")) {
                            int flag = 0;
                            int type = -1;
                            String beInvitedChatName = null;
                            try {
                                while (flag < 2) {
                                    if (re.ready()) {
                                        String temp = re.readLine();
                                        if (flag == 0) {
                                            type = Integer.parseInt(temp);
                                            flag++;
                                        } else {
                                            beInvitedChatName = temp;
                                            flag++;
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println("Fail beInvited");
                                stop();
                            }
                            if (!findState.containsKey(beInvitedChatName)) {
                                findState.put(beInvitedChatName, type);
                            }
                            if (!chatted.contains(beInvitedChatName)) {
                                chatted.add(beInvitedChatName);
                            }
                            if (listener != null) {
                                listener.onChatNameChanged();
                            }
                        } else if (str.equals("Online")) {
                            isOnline = true;
                        }

                    }
                    try {
                        while(dis.available()>0) {
                            String from = dis.readUTF();
                            String fileName = dis.readUTF();
                            long fileSize = dis.readLong();

                            File file = new File("C:\\Users\\cheny\\IdeaProjects\\chatting\\chatting-client\\src\\main\\java\\cn\\edu\\sustech\\cs209\\chatting\\client\\" + name + "_receive_"+fileName);
                            FileOutputStream fos = new FileOutputStream(file);

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            long bytesReceived = 0;

                            while (bytesReceived < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                                bytesReceived += bytesRead;
                            }

                            if(!fileList.containsKey(from)){
                                List<String> list = new ArrayList<>();
                                list.add(file.getName());
                                fileList.put(from,list);
                            }else{
                                List<String> list = fileList.get(from);
                                list.add(file.getName());
                                fileList.put(from,list);
                            }

                            if (listener != null) {
                                listener.onReceiveFile();
                            }

                            System.out.println("finishReceive: "+fileName);
                        }
                    }catch (IOException e){
                        System.out.println("Fail receiveFile");
                    }

                } catch (IOException e) {
                    System.out.println("Fail when running");
                    stop();
                }

            }
        } finally {
            out.println("exit");
            System.out.println(name + " exit");
            try {
                client.close();
            } catch (IOException e) {
                System.out.println("Fail close the socket");
            }
        }
    }

    public synchronized void stop() {
        isStopped = true;
    }

    public boolean checkValid() {
        try {
            while (true) {
                if (re.ready()) {
                    String test = re.readLine();
                    if (test.equals("exist")) {
                        return false;
                    }
                    return true;
                }
            }
        } catch (IOException e) {
            System.out.println("Fail checkVaild");
            stop();
            return false;
        }
    }

    public void send(Message message) {
        if (message.getChatName() != null) {
            if (!messages.containsKey(message.getChatName())) {
                List<Message> m = new ArrayList<>();
                m.add(message);
                messages.put(message.getChatName(), m);
            } else {
                List<Message> m = messages.get(message.getChatName());
                boolean flag = true;
                for (Message m1 : m) {
                    if (m1.getTimestamp().equals(message.getTimestamp()) && m1.getSentBy().equals(message.getSentBy())) {
                        flag = false;
                    }
                }
                if (flag) {
                    m.add(message);
                    messages.put(message.getChatName(), m);
                }
            }
        }
    } //local

    public void receive(Message message) {
        String key;
        if (message.getType() == 0) {
            key = message.getSentBy();
        } else {
            key = message.getChatName();
        }
        if (key != null) {
            if (!messages.containsKey(key)) {
                List<Message> m = new ArrayList<>();
                m.add(message);
                messages.put(key, m);
            } else {
                List<Message> m = messages.get(key);
                boolean flag = true;
                for (Message m1 : m) {
                    if (m1.getTimestamp().equals(message.getTimestamp()) && m1.getSentBy().equals(key)) {
                        flag = false;
                    }
                }
                if (flag) {
                    m.add(message);
                    messages.put(key, m);
                }
            }
        }
    } //local

    public void set(String to) {
        if (!messages.containsKey(to)) {
            List<Message> m = new ArrayList<>();
            messages.put(to, m);
        }
    } //local

    public List<String> getChatList() {
        try {
            out.println("getChatList");
            String response = re.readLine();
            chatList = Arrays.asList(response.split(","));
        } catch (IOException e) {
            System.out.println("Fail getChatList");
            stop();
//            e.printStackTrace();
        }
        return chatList;
    }

    public List<String> getChatted() {
        try {
            out.println("getChatted");
            String response = re.readLine();
            if (response.equals("no data")) {
            } else {
                List<String> get = Arrays.asList(response.split(","));
                for (String s : get) {
                    if (!chatted.contains(s)) {
                        chatted.add(s);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Fail getChatted");
            stop();
        }
        return chatted;
    }

    public void addChatted(String name) {
        if (!out.checkError()) {
            out.println("addChatted");
            out.println(name);
        } else {
            System.out.println("Fail addChatted");
        }
    }

    public void getMessages() {
        out.println("getMessages");
        int flag = 0;
        long timestamp = 0;
        String by = null;
        String to = null;
        String data = null;
        String chatName = null;
        int type = -1;
        try {
            while (flag < 6) {
                if (re.ready()) {
                    String str = re.readLine();
                    if (flag == 0 && str.equals("nothing")) {
                        break;
                    }
                    if (flag == 0) {
                        timestamp = Long.parseLong(str);
                    } else if (flag == 1) {
                        by = str;
                    } else if (flag == 2) {
                        to = str;
                    } else if (flag == 3) {
                        data = str;
                    } else if (flag == 4) {
                        chatName = str;
                    } else {
                        type = Integer.parseInt(str);
                    }
                    flag++;
                }
            }
        } catch (IOException e) {
            System.out.println("Fail getMessages");
            stop();
        }
        Message message = new Message(timestamp, by, to, data, chatName, type);
        if (!findState.containsKey(chatName)) {
            findState.put(chatName, type);
        }
        send(message);
    }

    public void addMessages(long timeStamp, String data, String to) {
        if (!out.checkError()) {
            out.println("addMessages");
            out.println(timeStamp);
            out.println(data);
            out.println(to);//to
            out.println(nowTo);
            out.println(findState.get(nowTo));
        } else {
            System.out.println("Fail addMessages");
            stop();
        }
    }

    public void getMembers() {
        try {
            out.println("getMembers");
            out.println(nowTo);
            int size = 0;
            int i = -1;
            String str = re.readLine();
            List<String> members = new ArrayList<>();
            Thread.sleep(100);
        } catch (Exception e) {
            System.out.println("Fail getMembers");
            stop();
        }
    }

    public void setMembers(String chatName, List<String> members) {
        if (!out.checkError()) {
            out.println("setMembers");
            out.println(members.size());
            out.println(chatName);
            for (String member : members) {
                out.println(member);
            }
        } else {
            System.out.println("Fail setMembers");
            stop();
        }
    }

    public void setInvited(String chatName, int type, int size) {
        if (!out.checkError()) {
            out.println("setInvited");
            out.println(type);
            if (type == 0) {
                out.println(chatName);
            } else {
                out.println(size);
                out.println(chatName);
            }
        } else {
            System.out.println("setInvited");
            stop();
        }
    }

    public void setListener(ValueChangeListener listener) {
        this.listener = listener;
    }

    public void checkOnline() {
        out.println("isOnline");
        isOnline = false;
    }

    public String sendFile(String filePath) throws IOException {
//        isSendFile = true;
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            System.out.println("Fail wait sendFile");
        }
        dos.writeUTF("sendFile");
        // 创建文件输入流
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);

        // 发送对象和接收对象
        dos.writeUTF(name);
        dos.writeUTF(nowTo);
        dos.write(findState.get(nowTo));

        // 发送文件名和长度
        dos.writeBytes(file.getName() + "\n");
        dos.writeLong(file.length());

        // 发送文件内容
        byte[] buffer = new byte[4096];
        int read;
        while ((read = fis.read(buffer)) > 0) {
            dos.write(buffer, 0, read);
        }

        // 关闭流
        fis.close();
        dos.flush();

        return file.getName();

    }
}

