package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Pair;
import javafx.util.Callback;


import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Controller implements Initializable, ValueChangeListener {

  Socket socket;
  Socket fileSocket;
  @FXML
  ListView<Message> chatContentList;

  String username;

  ClientThread client;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

//        Dialog<String> dialog = new TextInputDialog();
    Dialog<Pair<String, String>> dialog = new Dialog<>();
    dialog.setTitle("Login");
    dialog.setHeaderText("Enter your username and password:");
//        dialog.setContentText("Username:");
//
//
//        Optional<String> input = dialog.showAndWait();

    // 设置对话框的按钮
    ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

    // 创建用于输入用户名和密码的文本字段
    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    grid.setPadding(new Insets(20, 150, 10, 10));

    TextField name = new TextField();
    name.setPromptText("Username");
    PasswordField password = new PasswordField();
    password.setPromptText("Password");

    grid.add(new Label("Username:"), 0, 0);
    grid.add(name, 1, 0);
    grid.add(new Label("Password:"), 0, 1);
    grid.add(password, 1, 1);

    // 将文本字段添加到对话框中
    dialog.getDialogPane().setContent(grid);

    // 设置结果转换器，以将文本字段中的值转换为用户名和密码
    dialog.setResultConverter(dialogButton -> {
      if (dialogButton == loginButtonType) {
        return new Pair<>(name.getText(), password.getText());
      }
      return null;
    });

    // 显示对话框并等待用户响应
    Optional<Pair<String, String>> result = dialog.showAndWait();

    chatContentList.setCellFactory(new MessageCellFactory());
    if (result.isPresent()) {
            /*
               TODO: Check if there is a user with the same name among the currently logged-in users,
                     if so, ask the user to change the username
             */

      try {
        try {
          socket = new Socket("localhost", 1234);
          fileSocket = new Socket("localhost", 1235);
          client = new ClientThread(socket, fileSocket, result.get().getKey(),
              result.get().getValue());
        } catch (IOException e) {
          System.out.println("Controller: Fail create socket");
        }
        client.setListener(this);
        Thread newThread = new Thread(client);
        if (client.isValid) {
          username = result.get().getKey();
          setCnt();
          setText();
          setFont();
          client.getChatted();
          setChatList();
          newThread.start();
        } else {
          System.out.println(
              "Invalid username " + result.get().getKey() + " is exiting, please change");
          handleClose();
          Platform.exit();
        }
      } catch (Exception e) {
        System.out.println("Controller: Fail initialize");
      }
    } else {
      System.out.println("Quit");
      Platform.exit();
    }
  }

  @FXML
  private Label currentUsername;

  public void setText() {
    currentUsername.setText("Current User: " + username);
  }

  @FXML
  private Label currentOnlineCnt;

  public void setCnt() {
    currentOnlineCnt.setText("Online: " + client.getChatList().size());
  }

  @FXML
  private ListView<String> chatList;


  @FXML
  public void clickChatList() {
    chatList.setOnMouseClicked(event -> {
      ObservableList<String> selectedItems = chatList.getSelectionModel().getSelectedItems();
      if (selectedItems.size() != 0) {
        client.nowTo = selectedItems.get(0);
        System.out.println("Send message to: " + selectedItems.get(0));
        client.set(client.nowTo);
        setChatContentList();
        if (client.newMessages.contains(client.nowTo)) {
          client.newMessages.remove(client.nowTo);
        }
      }
    });
  }

  private boolean isSetChatList = false;

  @FXML
  public void setChatList() {
    if (!isSetChatList) {
      ObservableList<String> observableList = FXCollections.observableArrayList();
      observableList.addAll(client.chatted);
      chatList.setItems(observableList);
      isSetChatList = true;
    } else {
      ObservableList<String> ObservableList = chatList.getItems();
      ObservableList.clear();
      ObservableList.addAll(client.chatted);
    }
  }

  private boolean isSetChatContentList = false;

  public void setChatContentList() {
    if (!isSetChatContentList) {
      ObservableList<Message> newObservableList = FXCollections.observableArrayList();
      newObservableList.addAll(client.messages.get(client.nowTo));
      chatContentList.setItems(newObservableList);
      isSetChatContentList = true;
    } else {
      ObservableList<Message> ObservableList = chatContentList.getItems();
      ObservableList.clear();
      ObservableList.addAll(client.messages.get(client.nowTo));
    }
  }

  @FXML
  public void createPrivateChat() {
    AtomicReference<String> user = new AtomicReference<>();

    Stage stage = new Stage();
    ComboBox<String> userSel = new ComboBox<>();

    // FIXME: get the user list from server, the current user's name should be filtered out
    userSel.getItems().addAll(client.getChatList().stream().filter(s -> !s.equals(username))
        .collect(Collectors.toList()));

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      user.set(userSel.getSelectionModel().getSelectedItem());
      stage.close();
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();

    // TODO: if the current user already chatted with the selected user, just open the chat with that user
    // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name

    if (user.get() != null) {
      if (client.findState == null) {
        client.findState.put(user.get(), 0);
      } else if (!client.findState.containsKey(user.get())) {
        client.findState.put(user.get(), 0);
      }

      if (client.chatted == null) {
        client.chatted.add(user.get());
        client.addChatted(user.get());
        setChatList();
      } else if (!client.chatted.contains(user.get())) {
        client.chatted.add(user.get());
        client.addChatted(user.get());
        setChatList();
      }

      client.setInvited(user.get(), 0, 1);
    }
  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name. You can select
   * several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat: If there are > 3 users: display the first
   * three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for
   * example: UserA, UserB, UserC... (10) If there are <= 3 users: do not display the ellipsis, for
   * example: UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() {
    AtomicReference<ObservableList<String>> users = new AtomicReference<>();

    Stage stage = new Stage();
    ListView<String> groupSel = new ListView<>();
    groupSel.getItems().addAll(client.getChatList());
    groupSel.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    Label selectedLabel = new Label("Create a group: ");
    selectedLabel.setWrapText(true);
    selectedLabel.setMaxWidth(200);

    groupSel.setOnMouseClicked(event -> {
      ObservableList<String> selectedOptions = groupSel.getSelectionModel().getSelectedItems()
          .sorted();
      String selectedOptionsString = String.join("\n", selectedOptions);
      selectedLabel.setText("Create a group: \n" + selectedOptionsString);
      users.set(selectedOptions);
    });

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      stage.close();
    });

    ScrollPane scrollPane = new ScrollPane();
    scrollPane.setContent(groupSel);
    scrollPane.setPrefViewportHeight(100);

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(groupSel, okBtn, selectedLabel, scrollPane);
    stage.setScene(new Scene(box));
    stage.showAndWait();

    String groupName = "";
    if (users.get() != null) {
      ObservableList<String> members = users.get();
      if (members.size() > 3) {
        groupName = members.get(0) + "，" + members.get(1) + "，" + members.get(2) + " ... " + "("
            + members.size() + ")";
      } else if (members.size() == 3) {
        groupName = members.get(0) + "，" + members.get(1) + "，" + members.get(2) + " (3)";
      } else if (members.size() == 2) {
        groupName = members.get(0) + "，" + members.get(1) + " (2)";
      }

      if (groupName != "") {
        client.setMembers(groupName, members);

        if (client.findState == null) {
          client.findState.put(groupName, 1);
        } else if (!client.findState.containsKey(groupName)) {
          client.findState.put(groupName, 1);
        }

        if (client.chatted == null) {
          client.addChatted(groupName);
          client.chatted.add(groupName);
          setChatList();
        } else if (!client.chatted.contains(groupName)) {
          client.addChatted(groupName);
          client.chatted.add(groupName);
          setChatList();
        }

        client.setInvited(groupName, 1, members.size());
      }
    }
  }

  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed. After sending the message, you should clear the text input
   * field.
   */
  @FXML

  private TextArea inputArea;
  Font font = new Font("Segoe UI Emoji", 14);

  public void setFont() {
    inputArea.setFont(font);
  }

  public void doSendMessage() {
    // TODO
    AtomicReference<String> input = new AtomicReference<>();

    if (client.nowTo != null) {
      if (!inputArea.getText().isEmpty()) {
        input.set(inputArea.getText().trim());
        long timeStamp = System.currentTimeMillis();
        if (client.findState.get(client.nowTo) == 0) {
          Message message = new Message(timeStamp, client.name, client.nowTo, input.get(),
              client.nowTo, client.findState.get(client.nowTo));
          client.send(message);
          client.addMessages(timeStamp, input.get(), client.nowTo);
        } else {
          client.getMembers();
          List<String> members = client.members;
          if (client.members.size() > 0) {
            for (String to : members) {
              if (!to.equals(client.name)) {
                Message message = new Message(timeStamp, client.name, to, input.get(), client.nowTo,
                    client.findState.get(client.nowTo));
                client.send(message);
                client.addMessages(timeStamp, input.get(), to);
              }
            }
          }
        }
        setChatContentList();
        inputArea.clear();
      }
    }
  }

  @FXML
  public void selectEmoji() {
//        AtomicReference<String> emoji = new AtomicReference<>();

    Stage stage = new Stage();

    ComboBox<String> comboBox = new ComboBox<>();

//        Font font = new Font("Segoe UI Emoji", 14);
    comboBox.setCellFactory(listView -> {
      ListCell<String> cell = new ListCell<String>() {
        @Override
        protected void updateItem(String item, boolean empty) {
          super.updateItem(item, empty);
          if (item != null) {
            setText(item);
            setFont(font);
          }
        }
      };
      return cell;
    });
    comboBox.getItems()
        .addAll("\uD83D\uDE00", "\uD83D\uDE01", "\uD83D\uDE02", "\uD83E\uDD23", "\uD83D\uDE05",
            "\uD83D\uDE06", "\uD83D\uDE07", "\uD83D\uDE0A", "\uD83D\uDE0B", "\uD83D\uDE0D");

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      inputArea.appendText(comboBox.getSelectionModel().getSelectedItem());
      stage.close();
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(comboBox, okBtn);
    stage.setScene(new Scene(box));
    stage.showAndWait();
  }

  @FXML
  public void sendFile() {
    if (client.nowTo != null) {
      long timeStamp = System.currentTimeMillis();

      FileChooser fileChooser = new FileChooser();
      File selectedFile = fileChooser.showOpenDialog(stage);
      if (selectedFile != null) {
        String filePath = selectedFile.getAbsolutePath();
        System.out.println(filePath + " is selected");
        try {
          String fileName = client.sendFile(filePath);
          if (client.findState.get(client.nowTo) == 0) {
            Message message = new Message(timeStamp, client.name, client.nowTo,
                "< " + fileName + " >", client.nowTo, client.findState.get(client.nowTo));
            client.send(message);
            client.addMessages(timeStamp, "< " + fileName + " >", client.nowTo);
          } else {
            client.getMembers();
            List<String> members = client.members;
            if (client.members.size() > 0) {
              for (String to : members) {
                if (!to.equals(client.name)) {
                  Message message = new Message(timeStamp, client.name, to, "< " + fileName + " >",
                      client.nowTo, client.findState.get(client.nowTo));
                  client.send(message);
                  client.addMessages(timeStamp, "< " + fileName + " >", to);
                }
              }
            }
          }
          setChatContentList();

        } catch (IOException ex) {
          System.out.println("Error sending file: " + ex.getMessage());
        }
      }
    }
  }

  @FXML
  boolean isSetReceiveFile = false;

  public void receiveFile() {
    if (client.nowTo != null) {
      Stage stage = new Stage();
      ListView<String> memberList = new ListView();

      if (client.findState.containsKey(client.nowTo) && client.fileList.containsKey(client.nowTo)) {
        if (!isSetReceiveFile) {
          ObservableList<String> observableList = FXCollections.observableArrayList();
          observableList.addAll(client.fileList.get(client.nowTo));
          memberList.setItems(observableList);
          isSetReceiveFile = true;
        } else {
          ObservableList<String> ObservableList = memberList.getItems();
          ObservableList.clear();
          ObservableList.addAll(client.fileList.get(client.nowTo));
        }

      }

      HBox box = new HBox(10);
      box.setAlignment(Pos.CENTER);
      box.setPadding(new Insets(20, 20, 20, 20));
      box.getChildren().addAll(memberList);
      stage.setScene(new Scene(box));
      stage.showAndWait();
    }
  }

  @FXML
  boolean isSetMemberList = false;

  public void checkGroupMembers() {
    Stage stage = new Stage();
    ListView<String> memberList = new ListView();
    if (client.nowTo != null) {
      if (client.findState.containsKey(client.nowTo)) {
        if (client.findState.get(client.nowTo) == 1) {
          client.getMembers();
          if (!isSetMemberList) {
            ObservableList<String> observableList = FXCollections.observableArrayList();
            observableList.addAll(client.members);
            memberList.setItems(observableList);
            isSetMemberList = true;
          } else {
            ObservableList<String> ObservableList = memberList.getItems();
            ObservableList.clear();
            ObservableList.addAll(client.members);
          }
        }
      }
    }
    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(memberList);
    stage.setScene(new Scene(box));
    stage.showAndWait();
  }

  @FXML
  boolean isSetNewMessagesList = false;

  public void checkNewMessages() {
    Stage stage = new Stage();
    ListView<String> memberList = new ListView();

    if (!isSetNewMessagesList) {
      ObservableList<String> observableList = FXCollections.observableArrayList();
      observableList.addAll(client.newMessages);
      memberList.setItems(observableList);
      isSetNewMessagesList = true;
    } else {
      ObservableList<String> ObservableList = memberList.getItems();
      ObservableList.clear();
      ObservableList.addAll(client.newMessages);
    }
    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(memberList);
    stage.setScene(new Scene(box));
    stage.showAndWait();
  }

  @FXML
  @Override
  public void onMessageChanged(Message message) {
    if (client.nowTo != null) {
      if (message.getType() == 0) {
        if (client.nowTo.equals(message.getSentBy())) {
          Platform.runLater(() -> {
            setChatContentList();
          });
        }
      } else {
        if (client.nowTo.equals(message.getChatName())) {
          Platform.runLater(() -> {
            setChatContentList();
          });
        }
      }
      if (client.newMessages.contains(client.nowTo)) {
        client.newMessages.remove(client.nowTo);
      }
    }

  }

  @Override
  public void onChatNameChanged() {
    Platform.runLater(() -> {
      setChatList();
    });
  }

  @FXML
  @Override
  public void onIsOffLine() {
    Platform.runLater(() -> {
      setStageTitle();
    });
  }

  @FXML
  @Override
  public void onReceiveFile() {
    Platform.runLater(() -> {
      receiveFile();
    });
  }

  @FXML
  private Stage stage;

  public void setStage(Stage stage) {
    this.stage = stage;
  }

  public void setStageTitle() {
    stage.setTitle("Chatting Client: Fail connect server");
  }

  public void handleClose() {
    client.stop();
  }

  /**
   * You may change the cell factory if you changed the design of {@code Message} model. Hint: you
   * may also define a cell factory for the chats displayed in the left panel, or simply override
   * the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
//                    if (empty || Objects.isNull(msg)) {
//                        return;
//                    }
          if (empty || Objects.isNull(msg)) {
            // 清空单元格
            setText(null);
            setGraphic(null);
            setStyle(null);
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label(msg.getSentBy());
          Label msgLabel = new Label(msg.getData());
          msgLabel.setFont(font);

          nameLabel.setPrefSize(50, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (username.equals(msg.getSentBy())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameLabel);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }
}

