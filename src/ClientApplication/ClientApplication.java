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
}