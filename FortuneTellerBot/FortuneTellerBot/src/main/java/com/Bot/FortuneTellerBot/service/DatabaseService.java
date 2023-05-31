/**

 Сервіс бази даних для отримання даних з бази даних та збереження даних у базу даних.
 */
package com.Bot.FortuneTellerBot.service;
import com.Bot.FortuneTellerBot.connection.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class DatabaseService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);
    private Connection connection;

    /**
     * Конструктор, що з'єднується з базою даних при створенні об'єкту.
     */
    public DatabaseService() {
        connectToDatabase();
    }

    /**
     * Метод для з'єднання з базою даних.
     */
    void connectToDatabase() {
        connection = ConnectionManager.get();
    }

    /**
     * Отримати список категорій питань з бази даних.
     *
     * @return список категорій питань
     */
    public List<String> getQuestionCategoriesFromDatabase() {
        List<String> categories = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT category_name FROM question_categories");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                categories.add(resultSet.getString("category_name"));
            }
        } catch (SQLException e) {
            log.error("Виникла помилка при отриманні категорій питань з бази даних: " + e.getMessage());
        }
        return categories;
    }

    /**
     * Отримати список питань з бази даних для певної категорії.
     *
     * @param categoryId ідентифікатор категорії питань
     * @return список питань
     */
    public List<String> getQuestionsFromDatabase(int categoryId) {
        List<String> questions = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT question_text FROM questions WHERE question_category_id = ?");
            statement.setInt(1, categoryId + 1); // Додати 1 до categoryId
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                questions.add(resultSet.getString("question_text"));
            }
        } catch (SQLException e) {
            log.error("Виникла помилка при отриманні питань з бази даних: " + e.getMessage());
        }
        return questions;
    }

    /**
     * Отримати список варіантів відповідей з бази даних для певного питання.
     *
     * @param question питання
     * @return список варіантів відповідей
     */
    public List<String> getAnswerOptionsFromDatabase(String question) {
        List<String> answerOptions = new ArrayList<>();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT answer_text FROM answer_options WHERE question_id = (SELECT id FROM questions WHERE question_text = ?)");
            statement.setString(1, question);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                answerOptions.add(resultSet.getString("answer_text"));
            }
        } catch (SQLException e) {
            log.error("Виникла помилка при отриманні варіантів відповідей з бази даних: " + e.getMessage());
        }
        return answerOptions;
    }

    /**
     * Отримати випадкове пророцтво з бази даних для певної категорії.
     *
     * @param categoryId ідентифікатор категорії пророцтв
     * @return випадкове пророцтво
     */
    public String getRandomProphecyFromDatabase(int categoryId) {
        String prophecy = "";
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT prophecy_text FROM prophecies WHERE category_id = ? ORDER BY RAND() LIMIT 1");
            statement.setInt(1, categoryId + 1); // Додати 1 до categoryId
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                prophecy = resultSet.getString("prophecy_text");
            }
        } catch (SQLException e) {
            log.error("Виникла помилка при отриманні випадкового пророцтва з бази даних: " + e.getMessage());
        }
        return prophecy;
    }

    /**
     * Зберегти відповідь користувача до бази даних.
     *
     * @param userId         ідентифікатор користувача
     * @param questionId     ідентифікатор питання
     * @param answerOptionId ідентифікатор варіанту відповіді
     */
    public void saveUserAnswerToDatabase(long userId, long questionId, long answerOptionId) {
        try {
            // Перевірити, чи існує користувач перед збереженням відповіді
            if (isUserExists(userId)) {
                PreparedStatement statement = connection.prepareStatement("INSERT INTO user_answers (user_id, question_id, answer_option_id) VALUES (?, ?, ?)");
                statement.setLong(1, userId);
                statement.setLong(2, questionId);
                statement.setLong(3, answerOptionId);
                statement.executeUpdate();
            } else {
                log.error("Користувач не існує: " + userId);
            }
        } catch (SQLException e) {
            log.error("Виникла помилка при збереженні відповіді користувача в базу даних: " + e.getMessage());
        }
    }

    /**
     * Перевірити, чи існує користувач з певним ідентифікатором.
     *
     * @param userId ідентифікатор користувача
     * @return true, якщо користувач існує; false, якщо користувач не існує
     * @throws SQLException виникає, якщо виникає помилка при виконанні запиту до бази даних
     */
    private boolean isUserExists(long userId) throws SQLException {
        PreparedStatement statement = connection.prepareStatement("SELECT id FROM users WHERE id = ?");
        statement.setLong(1, userId);
        ResultSet resultSet = statement.executeQuery();
        return resultSet.next();
    }

    /**
     * Зберегти користувача до бази даних.
     *
     * @param chatId   ідентифікатор чату
     * @param username ім'я користувача
     */
    public void saveUserToDatabase(long chatId, String username) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO users (id, username) VALUES (?, ?)");
            statement.setLong(1, chatId);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException e) {
            log.error("Виникла помилка при збереженні користувача в базу даних: " + e.getMessage());
        }
    }

    /**
     * Отримати ідентифікатор питання з бази даних.
     *
     * @param question питання
     * @return ідентифікатор питання
     */
    public long getQuestionIdFromDatabase(String question) {
        long questionId = -1;
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM questions WHERE question_text = ?");
            statement.setString(1, question);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                questionId = resultSet.getLong("id");
            }
        } catch (SQLException e) {
            log.error("Виникла помилка при отриманні ідентифікатора питання з бази даних: " + e.getMessage());
        }
        return questionId;
    }

    /**
     * Отримати ідентифікатор варіанту відповіді з бази даних.
     *
     * @param question     питання
     * @param answerOption варіант відповіді
     * @return ідентифікатор варіанту відповіді
     */
    public long getAnswerOptionIdFromDatabase(String question, String answerOption) {
        long answerOptionId = -1;
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM answer_options WHERE question_id = ? AND answer_text = ?");
            statement.setLong(1, getQuestionIdFromDatabase(question));
            statement.setString(2, answerOption);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                answerOptionId = resultSet.getLong("id");
            }
        } catch (SQLException e) {
            log.error("Виникла помилка при отриманні ідентифікатора варіанта відповіді з бази даних: " + e.getMessage());
        }
        return answerOptionId;
    }
}