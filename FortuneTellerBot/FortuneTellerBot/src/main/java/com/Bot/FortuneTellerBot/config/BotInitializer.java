package com.Bot.FortuneTellerBot.config;

import com.Bot.FortuneTellerBot.service.TelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**

 Клас BotInitializer відповідає за ініціалізацію та реєстрацію Telegram-бота під час запуску контексту додатка.

 Клас є компонентом Spring та використовується для автоматичної реєстрації бота при завантаженні контексту.
 */
@Component
public class BotInitializer {
    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);

    private final TelegramBot bot;

    @Autowired
    public BotInitializer(TelegramBot bot) {
        this.bot = bot;
    }
    /**

     Метод init() виконує реєстрацію бота при події ContextRefreshedEvent.
     Під час ініціалізації використовується TelegramBotsApi для створення екземпляру бота і реєстрації його у Telegram.
     Якщо під час реєстрації сталася помилка TelegramApiException, виводиться повідомлення про помилку у журнал.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            log.error("Під час реєстрації бота сталася помилка: {}", e.getMessage());
        }
    }
}
