package com.Bot.FortuneTellerBot.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.Objects;

/**
 * Головний клас додатка для входу адміністратора.
 */
public class AdminLoginApp extends Application {

    private Stage primaryStage;



    /**
     * Головний метод, який запускає додаток.
     *
     * @param args аргументи командного рядка
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Метод, який ініціалізує головний етап додатка.
     *
     * @param primaryStage головний етап (Stage) додатка
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FortuneTellerBot.fxml"));
            Parent root = loader.load();
            primaryStage.setMinWidth(350);
            primaryStage.setMinHeight(260);
            primaryStage.setScene(new Scene(root));
            primaryStage.setTitle("Admin Login");
            Image icon = new Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/icon.png")));
            primaryStage.getIcons().add(icon);
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private TextField passwordField;

    private static final String PASSWORD = "123";



    /**
     * Обробник події для кнопки входу.
     *
     * @throws IOException виняток, якщо виникає помилка під час завантаження нового FXML-файлу
     */
    public void handleLogin() throws IOException {
        String password = passwordField.getText();

        if (password.equals(PASSWORD)) {
            // Успішний вхід
            showSuccessAlert("Успішний вхід", "Ласкаво просимо, Адміністраторе!");

            // Завантажуємо новий FXML файл та створюємо нову сцену
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/WelcomeScreen.fxml"));
            Parent welcomeRoot = loader.load();
            WelcomeScreenController controller = loader.getController();
            controller.setAdminLoginApp(this); // Передача посилання на поточний об'єкт AdminLoginApp
            Scene welcomeScene = new Scene(welcomeRoot);

            // Отримуємо розмір екрану
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();

            // Отримуємо поточний Stage
            Stage currentStage = (Stage) passwordField.getScene().getWindow();
            currentStage.setMinWidth(660);
            currentStage.setMinHeight(540);

            // Встановлюємо нову сцену та згортаємо поточний Stage
            currentStage.setScene(welcomeScene);
            currentStage.setTitle("Welcome Screen");
            currentStage.setIconified(false);
        } else {
            // Недійсний пароль
            showErrorAlert("Невірний пароль", "Будь ласка, введіть правильний пароль.");

            // Очистка поля та зміна стилю
            passwordField.clear();
            passwordField.setStyle("-fx-background-color: #ecbece;");
        }
    }

    /**
     * Показати спливаюче повідомлення про успішний вхід.
     *
     * @param title   заголовок повідомлення
     * @param content текст повідомлення
     */
    public void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Показати спливаюче повідомлення про помилку.
     *
     * @param title   заголовок повідомлення
     * @param content текст повідомлення
     */
    public void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
