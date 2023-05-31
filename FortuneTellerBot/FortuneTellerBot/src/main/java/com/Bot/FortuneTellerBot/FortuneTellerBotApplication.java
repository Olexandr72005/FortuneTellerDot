/**
 * Головний клас FortuneTellerBotApplication для запуску додатку Fortune Teller Bot.
 */
package com.Bot.FortuneTellerBot;

import com.Bot.FortuneTellerBot.application.AdminLoginApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FortuneTellerBotApplication {

	private static final Logger logger = LoggerFactory.getLogger(FortuneTellerBotApplication.class);

	/**
	 * Метод main, який запускає додаток Fortune Teller Bot.
	 *
	 * @param args аргументи командного рядка
	 */
	public static void main(String[] args) {
		try {
			SpringApplication.run(FortuneTellerBotApplication.class, args);

			AdminLoginApp.launch(AdminLoginApp.class, args);
		} catch (Exception e) {
			System.out.println("Під час виконання програми FortuneTellerBot сталася помилка: " + e);
		}
	}
}
