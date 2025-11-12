package com.fifatracker.fifa_tracker.bot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Service
@RequiredArgsConstructor
public class BotInitializer {

    private final FifaBot fifaBot;

    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(fifaBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
