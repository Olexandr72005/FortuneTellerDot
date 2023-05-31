package com.Bot.FortuneTellerBot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("application.properties")
public class BotConfig {
    private String botName;
    private String token;

    public String getBotName() {
        return botName;
    }

    @Value("${bot.name}")
    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getToken() {
        return token;
    }

    @Value("${bot.token}")
    public void setToken(String token) {
        this.token = token;
    }

}
