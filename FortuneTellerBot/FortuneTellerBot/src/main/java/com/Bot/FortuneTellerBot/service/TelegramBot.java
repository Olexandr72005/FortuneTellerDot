/**
 * Клас TelegramBot є реалізацією Telegram-бота для Fortune Teller.
 * Він взаємодіє з користувачами через Telegram, надаючи їм можливість отримувати передбачення.
 */
package com.Bot.FortuneTellerBot.service;

import com.Bot.FortuneTellerBot.config.BotConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final BotConfig config;
    private final DatabaseService databaseService;
    private Map<Long, Integer> userCategoryMap;
    private Map<Long, Integer> userQuestionIndexMap;

    /**
     * Конструктор TelegramBot ініціалізує бота з заданою конфігурацією і сервісом бази даних.
     *
     * @param config          конфігурація бота
     * @param databaseService сервіс бази даних
     */
    public TelegramBot(BotConfig config, DatabaseService databaseService) {
        this.config = config;
        this.databaseService = databaseService;
        userCategoryMap = new HashMap<>();
        userQuestionIndexMap = new HashMap<>();
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "отримати вітальне повідомлення"));
        listOfCommands.add(new BotCommand("/info", "інформація про використання бота"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Помилка встановлення списку команд бота: " + e.getMessage());
        }
    }

    /**
     * Отримує ім'я бота.
     *
     * @return ім'я бота
     */
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    /**
     * Отримує токен бота.
     *
     * @return токен бота
     */
    @Override
    public String getBotToken() {
        return config.getToken();
    }

    /**
     * Обробляє отримані оновлення від Telegram.
     *
     * @param update отримане оновлення
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
            } else if (messageText.equals("/info")) {
                sendBotInfo(chatId);
            } else {
                Integer questionIndex = userQuestionIndexMap.get(chatId);
                if (questionIndex != null) {
                    handleAnswer(chatId, messageText);
                } else {
                    try {
                        int categoryNumber = Integer.parseInt(messageText);
                        List<String> questionCategories = databaseService.getQuestionCategoriesFromDatabase();
                        if (categoryNumber >= 1 && categoryNumber <= questionCategories.size()) {
                            int categoryId = categoryNumber - 1;
                            userCategoryMap.put(chatId, categoryId);
                            retrieveNextQuestion(chatId);
                        } else {
                            sendMessage(chatId, "Невірний номер категорії. Будь ласка, спробуйте знову.");
                        }
                    } catch (NumberFormatException e) {
                        sendMessage(chatId, "Невірний ввід. Будь ласка, введіть номер категорії.");
                    }
                }
            }
        }
    }

    /**
     * Надсилає інформацію про бота користувачеві з вказаним chatId.
     *
     * @param chatId ідентифікатор чату
     */
    private void sendBotInfo(long chatId) {
        String botInfo = generateBotInfo(); // Згенерувати інформацію про бота
        sendMessage(chatId, botInfo);
    }

    /**
     * Генерує інформацію про бота.
     *
     * @return інформація про бота
     */
    private String generateBotInfo() {
        StringBuilder infoBuilder = new StringBuilder();
        infoBuilder.append("Цей бот надає послуги гадання.\n");
        infoBuilder.append(
                "Ви можете вибрати категорію і відповісти на серію питань, щоб отримати передбачення.\n");
        infoBuilder.append("Щоб почати, натисніть /start.\n");
        return infoBuilder.toString();
    }

    /**
     * Обробляє отриману команду /start, ініціалізуючи взаємодію з користувачем.
     *
     * @param chatId   ідентифікатор чату
     * @param username ім'я користувача
     */
    private void startCommandReceived(long chatId, String username) {

        sendMessage(chatId,
                "Привіт, " + username + "!\uD83D\uDC4B Введіть номер категорії, яку бажаєте обрати:");

        // Створити клавіатуру меню
        ReplyKeyboardMarkup menuKeyboard = new ReplyKeyboardMarkup();
        menuKeyboard.setResizeKeyboard(true);
        menuKeyboard.setOneTimeKeyboard(false);
        menuKeyboard.setSelective(true);

        // Створити рядки кнопок для меню
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("/start"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("/info"));
        keyboardRows.add(row1);
        keyboardRows.add(row2);

        // Встановити рядки кнопок для клавіатури меню
        menuKeyboard.setKeyboard(keyboardRows);

        // Створити повідомлення з клавіатурою меню
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(
                "Категорії:\n" + formatCategoriesList(databaseService.getQuestionCategoriesFromDatabase()));
        message.setReplyMarkup(menuKeyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Сталася помилка: " + e.getMessage());
        }

        // Зберегти chatId користувача для подальшого використання
        databaseService.saveUserToDatabase(chatId, username);
    }

    /**
     * Форматує список категорій у вигляді рядка.
     *
     * @param categories список категорій
     * @return сформатований рядок категорій
     */

    private String formatCategoriesList(List<String> categories) {
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            messageBuilder.append((i + 1)).append(". ").append(categories.get(i)).append("\n");
        }
        return messageBuilder.toString();
    }

    /**
     * Надсилає повідомлення з вказаним текстом користувачеві з вказаним chatId.
     *
     * @param chatId      ідентифікатор чату
     * @param textToSend  текст для надсилання
     */
    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Сталася помилка: " + e.getMessage());
        }
    }

    /**
     * Отримує наступне питання для користувача з вказаним chatId.
     *
     * @param chatId ідентифікатор чату
     */
    private void retrieveNextQuestion(long chatId) {
        int categoryId = userCategoryMap.get(chatId);
        List<String> questions = databaseService.getQuestionsFromDatabase(categoryId);
        if (questions.isEmpty()) {
            sendMessage(chatId, "В обраній категорії не знайдено питань.");
            return;
        }

        Integer questionIndex = userQuestionIndexMap.get(chatId);
        if (questionIndex == null) {
            questionIndex = 0;
            userQuestionIndexMap.put(chatId, questionIndex);
        } else {
            questionIndex++;
            if (questionIndex >= questions.size()) {
                String prophecy = databaseService.getRandomProphecyFromDatabase(categoryId);
                sendMessage(chatId, "Ваше пророцтво:\n" + prophecy);

                List<String> categories = databaseService.getQuestionCategoriesFromDatabase();
                StringBuilder messageBuilder = new StringBuilder("Категорії:\n");
                for (int i = 0; i < categories.size(); i++) {
                    messageBuilder.append((i + 1)).append(". ").append(categories.get(i)).append("\n");
                }
                sendMessage(chatId, messageBuilder.toString());

                userQuestionIndexMap.remove(chatId); // Видалити індекс питання для цього користувача
                userCategoryMap.remove(chatId); // Видалити вибір категорії для цього користувача
                return;
            }
            userQuestionIndexMap.put(chatId, questionIndex);
        }

        String question = questions.get(questionIndex);
        List<String> answerOptions = databaseService.getAnswerOptionsFromDatabase(question);
        sendMessage(chatId, "Питання:\n" + question);
        sendMessage(chatId, "Введіть варіант відповіді:\n" + formatAnswerOptionsList(answerOptions));
    }

    /**
     * Форматує список варіантів відповідей у вигляді рядка.
     *
     * @param answerOptions список варіантів відповідей
     * @return сформатований рядок варіантів відповідей
     */
    private String formatAnswerOptionsList(List<String> answerOptions) {
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < answerOptions.size(); i++) {
            messageBuilder.append((i + 1)).append(". ").append(answerOptions.get(i)).append("\n");
        }
        return messageBuilder.toString();
    }


    /**
     * Обробляє відповідь користувача на питання.
     *
     * @param chatId       ідентифікатор чату
     * @param answer відповідь користувача
     */
    private void handleAnswer(long chatId, String answer) {
        Integer questionIndex = userQuestionIndexMap.get(chatId);
        if (questionIndex != null) {
            int categoryId = userCategoryMap.get(chatId);
            List<String> questions = databaseService.getQuestionsFromDatabase(categoryId);
            if (questionIndex >= 0 && questionIndex < questions.size()) {
                String question = questions.get(questionIndex);
                List<String> answerOptions = databaseService.getAnswerOptionsFromDatabase(question);
                try {
                    int answerNumber = Integer.parseInt(answer);
                    if (answerNumber >= 1 && answerNumber <= answerOptions.size()) {
                        // Відповідь є валідною, зберегти її у базі даних
                        long userId = chatId; // Використовуйте відповідний ідентифікатор користувача
                        long questionId = databaseService.getQuestionIdFromDatabase(question); // Отримайте ідентифікатор питання з бази даних
                        String selectedAnswer = answerOptions.get(answerNumber - 1); // Отримайте вибрану відповідь
                        long answerOptionId = databaseService.getAnswerOptionIdFromDatabase(question, selectedAnswer); // Отримайте ідентифікатор вибраної відповіді з бази даних
                        databaseService.saveUserAnswerToDatabase(userId, questionId, answerOptionId);

                        // Отримати наступне питання
                        retrieveNextQuestion(chatId);
                    } else {
                        sendMessage(chatId, "Невірний номер відповіді. Будь ласка, спробуйте знову.");
                    }
                } catch (NumberFormatException e) {
                    sendMessage(chatId, "Невірний ввід. Будь ласка, введіть номер відповіді.");
                }
            }
        }
    }
}
