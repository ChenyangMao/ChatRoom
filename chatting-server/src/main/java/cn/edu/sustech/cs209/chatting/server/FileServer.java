package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Collectors;

public class FileServer implements Runnable {
    protected int serverPort = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean isStopped = false;
    public UserList userList;

    public FileServer(int port,UserList userList) {
        this.serverPort = port;
        this.userList = userList;
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
            new Thread(new FileRunnable(clientSocket, userList)).start();
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

//    public static void main(String[] args) {
//        FileServer server = new FileServer(1235);
//        new Thread(server).start();
//
//    }
}

class FileRunnable implements Runnable {

    protected Socket clientSocket = null;
    protected String user_name;
    InputStream inputStream;
    DataInputStream dis;
    OutputStream outputStream;
    DataOutputStream dos;
    User user;
    UserList userList;
    boolean isExit = false;
    Timer timer = new Timer();

    public FileRunnable(Socket clientSocket, UserList userList) {
        this.clientSocket = clientSocket;
        this.userList = userList;
    }

    public void run() {
        try {
            inputStream = this.clientSocket.getInputStream();
            outputStream = this.clientSocket.getOutputStream();
            dis = new DataInputStream(inputStream);
            dos = new DataOutputStream(outputStream);
            user_name = dis.readUTF();
            if(!userList.findFileSocket.containsKey(user_name)){
                userList.findFileSocket.put(user_name,clientSocket);
            }else{
                userList.findFileSocket.remove(user_name);
                userList.findFileSocket.put(user_name,clientSocket);
            }
            while (true) {
                    String message = dis.readUTF();
                    if (message.equals("sendFile")) {
                        sendFile();
                    }
                Thread.sleep(100);
            }
        } catch (IOException | InterruptedException e) {
//            System.out.println("InterruptedException");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println(user_name+": Fail close clientSocket");
            }
        }

    }


    public void sendFile() {
        try {
            String from = dis.readUTF();
            String chatName = dis.readUTF();
            int type = dis.read();

            String fileName = dis.readLine();
            long fileSize = dis.readLong();

            String filePath = "C:\\Users\\cheny\\IdeaProjects\\chatting\\chatting-server\\src\\main\\java\\cn\\edu\\sustech\\cs209\\chatting\\server\\" + fileName;
            File file = new File(filePath);
            FileOutputStream fos = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int bytesRead;
            long bytesReceived = 0;

            while (bytesReceived < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
            }

            System.out.println("finishSendFile");
            receiveFile(filePath,from,chatName,type);
        }catch (IOException e){
            System.out.println(user_name+": Fail sendFile");
        }
    }

    public void receiveFile(String filePath, String from, String chatName,int type){
        try {
//            dos.writeUTF("newFile");
            // 创建文件输入流

            if(type==0){
                File file = new File(filePath);
                FileInputStream fis = new FileInputStream(file);
                Socket receiveSocket = userList.findFileSocket.get(chatName);
                DataOutputStream dos1 = new DataOutputStream(receiveSocket.getOutputStream());

                dos1.writeUTF(from);
                // 发送文件名和长度
                dos1.writeUTF(file.getName());
                dos1.writeLong(file.length());

                // 发送文件内容
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) > 0) {
                    dos1.write(buffer, 0, read);
                }
                dos1.flush();
                fis.close();
            }else{
                List<String> receiveList = userList.groupList.get(chatName)
                        .stream()
                        .filter(e -> !e.equals(from))
                        .collect(Collectors.toList());

                for(String receiver: receiveList){
                    File file = new File(filePath);
                    FileInputStream fis = new FileInputStream(file);

                    Socket receiveSocket = userList.findFileSocket.get(receiver);
                    DataOutputStream dos1 = new DataOutputStream(receiveSocket.getOutputStream());

                    dos1.writeUTF(chatName);
                    // 发送文件名和长度
                    dos1.writeUTF(file.getName());
                    dos1.writeLong(file.length());

                    // 发送文件内容
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) > 0) {
                        dos1.write(buffer, 0, read);
                    }
                    dos1.flush();
                    fis.close();
                }
            }
        }catch (IOException e){
            System.out.println(user_name+": Fail receiveFile");
        }
    }

}
