package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;

import java.util.List;

public interface ValueChangeListener {

  void onMessageChanged(Message message);

  void onChatNameChanged();

  void onIsOffLine();

  void onReceiveFile();
}
