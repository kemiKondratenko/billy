package com.eugene.bill.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiException;

@SpringBootApplication
public class CoreDemoApplication implements CommandLineRunner {

	static {
		ApiContextInitializer.init();
	}

	@Autowired
	private SimpleBot simpleBot;

	public static void main(String[] args) {
		SpringApplication.run(CoreDemoApplication.class, args);
	}

	@Override
	public void run(String... strings) throws Exception {
		TelegramBotsApi botapi = new TelegramBotsApi();
		try {
			botapi.registerBot(simpleBot);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}
}
