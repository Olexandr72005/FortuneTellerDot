package com.Bot.FortuneTellerBot;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Утилітарний клас для роботи з файлом властивостей.
 */
public class PropertiesUtil {
    private static final Properties PROPERTIES = new Properties();

    static {
        loadProperties();
    }

    private PropertiesUtil() {
    }

    /**
     * Отримати значення властивості за ключем.
     *
     * @param key ключ властивості
     * @return значення властивості
     */
    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    /**
     * Завантажити властивості з файлу.
     */
    private static void loadProperties() {
        try (InputStream applicationProperties = PropertiesUtil.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            PROPERTIES.load(applicationProperties);
        } catch (IOException e) {
            throw new RuntimeException("Помилка читання файлу властивостей.", e);
        }
    }
}
