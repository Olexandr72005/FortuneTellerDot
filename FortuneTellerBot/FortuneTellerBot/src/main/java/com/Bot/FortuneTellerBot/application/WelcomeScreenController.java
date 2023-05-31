package com.Bot.FortuneTellerBot.application;

import com.Bot.FortuneTellerBot.connection.ConnectionManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * Клас-контролер для екрану привітання.
 * Керує взаємодією користувача з таблицями бази даних.
 */
public class WelcomeScreenController {
    @FXML
    private HBox textFieldContainer;
    @FXML
    private Button editButton;
    @FXML
    private Button addButton;
    @FXML
    private Button deleteButton;
    @FXML
    private ChoiceBox<String> choiceBox;
    @FXML
    private TableView<ObservableList<Object>> tableView;

    private List<TextField> textFields = new ArrayList<>();

    /**
     * Конструктор без аргументів.
     */
    public WelcomeScreenController() {
    }

    /**
     * Ініціалізація контролера після завантаження fxml-файлу.
     * Встановлює обробники подій для кнопок та вибіркового списку,
     * завантажує дані таблиці з бази даних та створює динамічні текстові поля.
     */
    @FXML
    private void initialize() {
        choiceBox.setItems(FXCollections.observableArrayList());
        readTablesFromDatabase();
        addButton.setOnAction(this::addButtonClicked);
        editButton.setOnAction(this::editButtonClicked);
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                populateTextFields(newValue);
            }
        });
        deleteButton.setOnAction(this::deleteButtonClicked);
        choiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                loadTableData(newValue);
                clearTextFields();
                try (Connection connection = ConnectionManager.get();
                     Statement statement = connection.createStatement()) {
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM fortune_teller_bot." + newValue);
                    ResultSetMetaData resMetaData = resultSet.getMetaData();
                    int columnCount = resMetaData.getColumnCount();
                    createDynamicTextFields(columnCount, resMetaData);
                } catch (SQLException e) {
                    e.printStackTrace();
                    showErrorMessage("Помилка!", "Не вдалося отримати метадані про цю таблицю");
                }
            }
        });
    }

    /**
     * Завантажує назви таблиць з бази даних та відображає їх у вибірковому списку.
     */
    private void readTablesFromDatabase() {
        try (Connection connection = ConnectionManager.get();
             PreparedStatement prepStatement = connection.prepareStatement("SELECT * FROM information_schema.tables WHERE table_schema = 'fortune_teller_bot'")) {
            ResultSet resultSet = prepStatement.executeQuery();
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                choiceBox.getItems().add(tableName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Помилка!", "Не вдалося отримати таблиці з бази даних");
        }
    }

    /**
     * Завантажує дані таблиці з бази даних та відображає їх у таблиці на екрані.
     *
     * @param tableName назва таблиці
     */
    private void loadTableData(String tableName) {
        try (Connection connection = ConnectionManager.get();
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName);

            ResultSetMetaData resMetaData = resultSet.getMetaData();
            int columnCount = resMetaData.getColumnCount();

            tableView.getColumns().clear();

            for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                final int columnIndexFinal = columnIndex;
                TableColumn<ObservableList<Object>, Object> column = new TableColumn<>(resMetaData.getColumnName(columnIndex));
                column.setCellValueFactory(cellData -> {
                    ObservableList<Object> row = cellData.getValue();
                    if (row != null && row.size() >= columnIndexFinal) {
                        return new SimpleObjectProperty<>(row.get(columnIndexFinal - 1));
                    }
                    return null;
                });
                tableView.getColumns().add(column);
            }

            ObservableList<ObservableList<Object>> data = FXCollections.observableArrayList();

            while (resultSet.next()) {
                ObservableList<Object> row = FXCollections.observableArrayList();
                for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
                    row.add(resultSet.getObject(columnIndex));
                }
                data.add(row);
            }

            tableView.setItems(data);

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Помилка!", "Не вдалося завантажити дані з таблиці");
        }
    }

    /**
     * Заповнює текстові поля даними рядка таблиці.
     *
     * @param row рядок таблиці
     */
    private void populateTextFields(ObservableList<Object> row) {
        if (row.size() == textFields.size()) {
            for (int i = 0; i < row.size(); i++) {
                TextField textField = textFields.get(i);
                Object value = row.get(i);
                textField.setText(value != null ? value.toString() : "");
            }
        }
    }

    /**
     * Обробник натискання кнопки "Редагувати".
     * Оновлює вибраний рядок таблиці в базі даних зі значеннями з текстових полів.
     *
     * @param event подія
     */
    private void editButtonClicked(ActionEvent event) {
        String tableName = choiceBox.getSelectionModel().getSelectedItem();
        if (tableName == null) {
            showErrorMessage("Помилка!", "Будь ласка, виберіть таблицю");
            return;
        }

        ObservableList<Object> selectedRow = tableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showErrorMessage("Помилка!", "Будь ласка, виберіть рядок для оновлення");
            return;
        }

        List<String> primaryKeys = getPrimaryKeys(tableName);
        if (primaryKeys.isEmpty()) {
            showErrorMessage("Помилка!", "Не вдалося отримати первинний ключ для цієї таблиці");
            return;
        }

        List<String> values = textFields.stream()
                .map(TextField::getText)
                .collect(Collectors.toList());

        String updateStatement = generateUpdateStatement(tableName, primaryKeys, values, selectedRow);

        try (Connection connection = ConnectionManager.get();
             PreparedStatement preparedStatement = connection.prepareStatement(updateStatement)) {

            preparedStatement.executeUpdate();

            loadTableData(tableName);
            clearTextFields();

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Помилка!", "Не вдалося оновити запис у таблиці");
        }
    }

    /**
     * Генерує SQL-запит на оновлення вибраного рядка таблиці з вказаними значеннями.
     *
     * @param tableName   назва таблиці
     * @param primaryKeys список первинних ключів таблиці
     * @param values      значення для оновлення
     * @param selectedRow вибраний рядок таблиці
     * @return SQL-запит на оновлення рядка
     */
    private String generateUpdateStatement(String tableName, List<String> primaryKeys,
                                           List<String> values, ObservableList<Object> selectedRow) {
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE fortune_teller_bot.").append(tableName).append(" SET ");

        for (int i = 0; i < values.size(); i++) {
            sb.append(textFields.get(i).getPromptText()).append(" = '").append(values.get(i)).append("'");
            if (i != values.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(" WHERE ");

        for (int i = 0; i < primaryKeys.size(); i++) {
            sb.append(primaryKeys.get(i)).append(" = '").append(selectedRow.get(i)).append("'");
            if (i != primaryKeys.size() - 1) {
                sb.append(" AND ");
            }
        }

        return sb.toString();
    }

    /**
     * Створює динамічні текстові поля залежно від кількості стовпців таблиці.
     *
     * @param columnCount  кількість стовпців таблиці
     * @param resMetaData метадані результату запиту
     */
    private void createDynamicTextFields(int columnCount, ResultSetMetaData resMetaData) {
        textFieldContainer.getChildren().clear();
        textFields.clear();

        for (int columnIndex = 1; columnIndex <= columnCount; columnIndex++) {
            try {
                TextField textField = new TextField();
                textField.setPromptText(resMetaData.getColumnName(columnIndex));
                textFields.add(textField);
                textFieldContainer.getChildren().add(textField);
            } catch (SQLException e) {
                e.printStackTrace();
                showErrorMessage("Помилка!", "Не вдалося створити текстове поле для цього стовпця");
            }
        }
    }

    /**
     * Очищає текстові поля.
     */
    private void clearTextFields() {
        for (TextField textField : textFields) {
            textField.clear();
        }
    }

    /**
     * Обробник натискання кнопки "Додати".
     * Додає новий рядок в таблицю бази даних зі значеннями з текстових полів.
     *
     * @param event подія
     */
    private void addButtonClicked(ActionEvent event) {
        String tableName = choiceBox.getSelectionModel().getSelectedItem();
        if (tableName == null) {
            showErrorMessage("Помилка!", "Будь ласка, виберіть таблицю");
            return;
        }

        List<String> columnNames = getColumnNames(tableName);
        if (columnNames.isEmpty()) {
            showErrorMessage("Помилка!", "Не вдалося отримати назви стовпців для цієї таблиці");
            return;
        }

        List<String> values = textFields.stream()
                .map(TextField::getText)
                .collect(Collectors.toList());

        String insertStatement = generateInsertStatement(tableName, columnNames, values);

        try (Connection connection = ConnectionManager.get();
             PreparedStatement preparedStatement = connection.prepareStatement(insertStatement)) {

            preparedStatement.executeUpdate();

            loadTableData(tableName);
            clearTextFields();

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Помилка!", "Не вдалося додати новий запис у таблицю");
        }
    }

    /**
     * Генерує SQL-запит на додавання нового рядка до таблиці з вказаними значеннями.
     *
     * @param tableName   назва таблиці
     * @param columnNames список назв стовпців таблиці
     * @param values      значення для додавання
     * @return SQL-запит на додавання рядка
     */
    private String generateInsertStatement(String tableName, List<String> columnNames, List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO fortune_teller_bot.").append(tableName).append(" (");

        for (int i = 0; i < columnNames.size(); i++) {
            sb.append(columnNames.get(i));
            if (i != columnNames.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(") VALUES (");

        for (int i = 0; i < values.size(); i++) {
            sb.append("'").append(values.get(i)).append("'");
            if (i != values.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append(")");

        return sb.toString();
    }

    /**
     * Отримує список назв стовпців таблиці.
     *
     * @param tableName назва таблиці
     * @return список назв стовпців
     */
    private List<String> getColumnNames(String tableName) {
        List<String> columnNames = new ArrayList<>();

        try (Connection connection = ConnectionManager.get();
             PreparedStatement prepStatement = connection.prepareStatement("SELECT * FROM " + tableName)) {

            ResultSetMetaData resMetaData = prepStatement.getMetaData();
            int columnCount = resMetaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(resMetaData.getColumnName(i));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columnNames;
    }

    /**
     * Обробник натискання кнопки "Видалити".
     * Видаляє вибраний рядок з таблиці бази даних.
     *
     * @param event подія
     */
    private void deleteButtonClicked(ActionEvent event) {
        String tableName = choiceBox.getSelectionModel().getSelectedItem();
        if (tableName == null) {
            showErrorMessage("Помилка!", "Будь ласка, виберіть таблицю");
            return;
        }

        ObservableList<Object> selectedRow = tableView.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showErrorMessage("Помилка!", "Будь ласка, виберіть рядок для видалення");
            return;
        }

        List<String> primaryKeys = getPrimaryKeys(tableName);
        if (primaryKeys.isEmpty()) {
            showErrorMessage("Помилка!", "Не вдалося отримати первинний ключ для цієї таблиці");
            return;
        }

        String deleteStatement = generateDeleteStatement(tableName, primaryKeys, selectedRow);

        try (Connection connection = ConnectionManager.get();
             PreparedStatement preparedStatement = connection.prepareStatement(deleteStatement)) {

            preparedStatement.executeUpdate();

            loadTableData(tableName);
            clearTextFields();

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Помилка!", "Не вдалося видалити запис з таблиці");
        }
    }

    /**
     * Генерує SQL-запит на видалення вибраного рядка з таблиці.
     *
     * @param tableName   назва таблиці
     * @param primaryKeys список первинних ключів таблиці
     * @param selectedRow вибраний рядок таблиці
     * @return SQL-запит на видалення рядка
     */
    private String generateDeleteStatement(String tableName, List<String> primaryKeys, ObservableList<Object> selectedRow) {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM fortune_teller_bot.").append(tableName).append(" WHERE ");

        for (int i = 0; i < primaryKeys.size(); i++) {
            sb.append(primaryKeys.get(i)).append(" = '").append(selectedRow.get(i)).append("'");
            if (i != primaryKeys.size() - 1) {
                sb.append(" AND ");
            }
        }

        return sb.toString();
    }

    /**
     * Отримує список первинних ключів таблиці.
     *
     * @param tableName назва таблиці
     * @return список первинних ключів
     */
    private List<String> getPrimaryKeys(String tableName) {
        List<String> primaryKeys = new ArrayList<>();

        try (Connection connection = ConnectionManager.get();
             ResultSet resultSet = connection.getMetaData().getPrimaryKeys(null, null, tableName)) {

            while (resultSet.next()) {
                primaryKeys.add(resultSet.getString("COLUMN_NAME"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return primaryKeys;
    }

    /**
     * Показує повідомлення про помилку.
     *
     * @param title   заголовок помилки
     * @param message текст помилки
     */
    private void showErrorMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    public void setAdminLoginApp(AdminLoginApp adminLoginApp) {
        // Method implementation
    }
}
