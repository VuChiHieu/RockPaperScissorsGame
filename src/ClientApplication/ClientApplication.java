package ClientApplication;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.Socket;

public class ClientApplication extends Application {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String password;
    private String currentRoom;
    private int bo;

    private Stage primaryStage;
    private Label statusLabel;
    private TextArea chatArea;
    private TextField chatInput;
    private Label scoreLabel;
    private Button playBtnKeo, playBtnBua, playBtnBao;
    private Label gameStatusLabel;
    private Button createRoomBtn, joinRoomBtn;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        showLoginScreen();
    }

    /** ---------------- MÀN HÌNH ĐĂNG NHẬP ---------------- */
    private void showLoginScreen() {
        VBox loginPane = new VBox(10);
        loginPane.setPadding(new Insets(20));
        loginPane.setAlignment(Pos.CENTER);
        loginPane.setStyle("-fx-font-size: 14;");

        Label titleLabel = new Label("KÉO - BÚA - BAO");
        titleLabel.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu");

        Button loginBtn = new Button("Đăng nhập");
        Button registerBtn = new Button("Đăng ký");

        /** Xử lý nút Đăng nhập **/
        loginBtn.setOnAction(e -> {
            if (!usernameField.getText().isEmpty() && !passwordField.getText().isEmpty()) {
                username = usernameField.getText();
                password = passwordField.getText();
                connectToServer();
                out.println("LOGIN|" + username + "|" + password);
            } else {
                showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            }
        });

        /** Xử lý nút Đăng ký **/
        registerBtn.setOnAction(e -> {
            if (!usernameField.getText().isEmpty() && !passwordField.getText().isEmpty()) {
                username = usernameField.getText();
                password = passwordField.getText();
                connectToServer();
                out.println("REGISTER|" + username + "|" + password);
            } else {
                showAlert("Lỗi", "Vui lòng nhập đầy đủ thông tin!");
            }
        });

        loginPane.getChildren().addAll(titleLabel, usernameField, passwordField, loginBtn, registerBtn);

        Scene scene = new Scene(loginPane, 450, 300);
        primaryStage.setTitle("Đăng nhập - Kéo Búa Bao");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /** ---------------- KẾT NỐI SERVER ---------------- */
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Lắng nghe phản hồi từ server
            new Thread(() -> {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        handleServerMessage(response);
                    }
                } catch (IOException e) {
                    System.err.println("Lỗi kết nối: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            showAlert("Lỗi", "Không thể kết nối đến server");
        }
    }

    /** ---------------- XỬ LÝ TIN NHẮN TỪ SERVER ---------------- */
    private void handleServerMessage(String message) {
        System.out.println("[CLIENT] Nhận từ server: " + message);
        String[] parts = message.split("\\|", 2);
        String command = parts[0];
        String data = parts.length > 1 ? parts[1] : "";

        Platform.runLater(() -> {
            switch (command) {
                case "LOGIN_SUCCESS":
                    showMainScreen(); // chỉ vào giao diện sau khi đăng nhập thành công
                    statusLabel.setText("✓ Đã kết nối: " + username);
                    break;

                case "REGISTER_SUCCESS":
                    // Tự động đăng nhập lại sau khi đăng ký thành công
                    out.println("LOGIN|" + username + "|" + password);
                    break;

                case "ROOM_CREATED":
                    String[] roomData = data.split("\\|");
                    currentRoom = roomData[0];
                    bo = Integer.parseInt(roomData[1]);
                    gameStatusLabel.setText("Phòng: " + currentRoom + " | BO" + bo);
                    break;

                case "ROOM_JOINED":
                    roomData = data.split("\\|");
                    currentRoom = roomData[0];
                    bo = Integer.parseInt(roomData[1]);
                    gameStatusLabel.setText("Phòng: " + currentRoom + " | BO" + bo);
                    chatArea.appendText("[SYSTEM] Tham gia phòng: " + currentRoom + "\n");
                    break;

                case "PLAYER_JOINED":
                    chatArea.appendText("[SYSTEM] " + data + " đã vào phòng\n");
                    break;

                case "ROUND_RESULT":
                    handleRoundResult(data);
                    break;

                case "GAME_END":
                    handleGameEnd(data);
                    break;

                case "CHAT":
                    chatArea.appendText(data + "\n");
                    break;

                case "NOTIFY":
                    chatArea.appendText("[NOTIFY] " + data + "\n");
                    break;

                case "LEADERBOARD": {
                    String leaderboardText = data.replace("\\n", "\n");
                    showLeaderboard(leaderboardText);
                    break;
                }


                case "STATS":
                    statusLabel.setText("Thống kê: " + data);
                    break;

                case "ERROR":
                    showAlert("Lỗi", data);
                    break;

                case "ROOM_LIST":
                    chatArea.appendText("[ROOMS]\n" + data + "\n");
                    break;
            }
        });
    }

    /** ---------------- XỬ LÝ TRÒ CHƠI ---------------- */
    private void handleRoundResult(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 5) {
            String move1 = parts[0];
            String move2 = parts[1];
            String result = parts[2];
            int w1 = Integer.parseInt(parts[3]);
            int w2 = Integer.parseInt(parts[4]);

            scoreLabel.setText("Tỷ số: " + w1 + " - " + w2);
            chatArea.appendText("[ROUND] " + move1 + " vs " + move2 + " => " + result + "\n");

            playBtnKeo.setDisable(false);
            playBtnBua.setDisable(false);
            playBtnBao.setDisable(false);

            playBtnKeo.setText("KÉO");
            playBtnBua.setText("BÚA");
            playBtnBao.setText("BAO");
        }
    }

    private void handleGameEnd(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 4) {
            String winner = parts[0];
            chatArea.appendText("[GAME_END] " + winner + " chiến thắng! Tỷ số: " + parts[2] + " - " + parts[3] + "\n");
            resetGameUI();
        }
    }

    /** ---------------- GIAO DIỆN CHÍNH ---------------- */
    private void showMainScreen() {
        BorderPane mainPane = new BorderPane();
        mainPane.setPadding(new Insets(10));

        VBox topPane = new VBox(5);
        statusLabel = new Label("Đang kết nối...");
        gameStatusLabel = new Label("Không có phòng");
        topPane.getChildren().addAll(statusLabel, gameStatusLabel);
        mainPane.setTop(topPane);

        HBox centerPane = new HBox(10);

        // Khu vực chơi
        VBox gamePane = new VBox(10);
        gamePane.setPadding(new Insets(10));
        gamePane.setStyle("-fx-border-color: #ccc; -fx-border-radius: 5;");

        Label gameLabel = new Label("GIAO DIỆN CHƠI");
        scoreLabel = new Label("Tỷ số: 0 - 0");

        playBtnKeo = createPlayButton("KÉO");
        playBtnBua = createPlayButton("BÚA");
        playBtnBao = createPlayButton("BAO");

        gamePane.getChildren().addAll(gameLabel, scoreLabel, playBtnKeo, playBtnBua, playBtnBao);

        // Khu vực chat
        VBox chatPane = new VBox(5);
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        HBox inputPane = new HBox(5);
        chatInput = new TextField();
        chatInput.setPromptText("Nhập tin nhắn...");
        Button sendBtn = new Button("Gửi");
        sendBtn.setOnAction(e -> {
            if (!chatInput.getText().isEmpty()) {
                out.println("CHAT|" + chatInput.getText());
                chatInput.clear();
            }
        });
        inputPane.getChildren().addAll(new Label("Chat:"), chatInput, sendBtn);

        chatPane.getChildren().addAll(new Label("Chat phòng / sảnh:"), new ScrollPane(chatArea), inputPane);
        HBox.setHgrow(chatPane, Priority.ALWAYS);

        centerPane.getChildren().addAll(gamePane, chatPane);
        mainPane.setCenter(centerPane);

        // Thanh công cụ dưới
        HBox bottomPane = new HBox(10);
        bottomPane.setPadding(new Insets(10));

        createRoomBtn = new Button("Tạo Phòng");
        createRoomBtn.setOnAction(e -> showRoomDialog(false));

        joinRoomBtn = new Button("Tham Gia Phòng");
        joinRoomBtn.setOnAction(e -> showRoomDialog(true));

        Button leaderboardBtn = new Button("Bảng Xếp Hạng");
        leaderboardBtn.setOnAction(e -> out.println("LEADERBOARD|"));

        Button statsBtn = new Button("Thống Kê");
        statsBtn.setOnAction(e -> out.println("GET_STATS|"));

        Button listRoomsBtn = new Button("Danh sách phòng");
        listRoomsBtn.setOnAction(e -> out.println("LIST_ROOMS|"));

        Button quitRoomBtn = new Button("Rời Phòng");
        quitRoomBtn.setOnAction(e -> {
            out.println("QUIT_ROOM|");
            resetGameUI();
            currentRoom = null;
        });

        bottomPane.getChildren().addAll(createRoomBtn, joinRoomBtn, leaderboardBtn, statsBtn, listRoomsBtn, quitRoomBtn);
        mainPane.setBottom(bottomPane);

        Scene scene = new Scene(mainPane, 900, 600);
        primaryStage.setTitle("KÉO - BÚA - BAO - " + username);
        primaryStage.setScene(scene);

        primaryStage.setOnCloseRequest((WindowEvent we) -> {
            try {
                if (out != null) out.println("QUIT_ROOM|");
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    private Button createPlayButton(String move) {
        Button btn = new Button(move);
        btn.setOnAction(e -> {
            if (currentRoom != null) {
                out.println("PLAY|" + move);
                btn.setDisable(true);
                btn.setText(move + " ✓");
            } else {
                showAlert("Thông báo", "Vui lòng tham gia/tạo phòng trước");
            }
        });
        return btn;
    }

    private void resetGameUI() {
        playBtnKeo.setDisable(false);
        playBtnKeo.setText("KÉO");
        playBtnBua.setDisable(false);
        playBtnBua.setText("BÚA");
        playBtnBao.setDisable(false);
        playBtnBao.setText("BAO");
        scoreLabel.setText("Tỷ số: 0 - 0");
        gameStatusLabel.setText("Không có phòng");
    }

    private void showRoomDialog(boolean isJoin) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(isJoin ? "Tham Gia Phòng" : "Tạo Phòng");

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));

        if (isJoin) {
            TextField roomIdField = new TextField();
            roomIdField.setPromptText("Nhập ID phòng (vd: room_1)");
            content.getChildren().add(roomIdField);
            dialog.setResultConverter(result -> roomIdField.getText());
        } else {
            ComboBox<Integer> boCombo = new ComboBox<>();
            boCombo.getItems().addAll(3, 5, 7);
            boCombo.setValue(3);
            content.getChildren().addAll(new Label("Chọn loại trận:"), boCombo);
            dialog.setResultConverter(result -> String.valueOf(boCombo.getValue()));
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (isJoin) out.println("JOIN_ROOM|" + result);
            else out.println("CREATE_ROOM|" + result);
        });
    }

    private void showLeaderboard(String data) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Bảng Xếp Hạng");

        TextArea area = new TextArea(data);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(300);
        area.setPrefWidth(400);

        dialog.getDialogPane().setContent(new ScrollPane(area));
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
