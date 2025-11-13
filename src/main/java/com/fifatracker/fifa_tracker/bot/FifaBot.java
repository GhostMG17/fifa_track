package com.fifatracker.fifa_tracker.bot;

import com.fifatracker.fifa_tracker.dto.MatchRequest;
import com.fifatracker.fifa_tracker.dto.PeriodResultDto;
import com.fifatracker.fifa_tracker.dto.PlayerStatsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.*;

@Component
public class FifaBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Random random = new Random();
    private final Set<Long> greetedUsers = new HashSet<>();

    private static class MatchState {
        String player1;
        String player2;
        Integer player1Score;
        Integer player2Score;
        String date;
        String step = "player1";
    }

    private final Map<Long, MatchState> matchStates = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                String text = update.getMessage().getText();
                Long chatId = update.getMessage().getChatId();

                if ("/start".equals(text)) {
                    sendStartMenu(chatId);
                    return;
                }

                // Пошаговое добавление матча
                if (matchStates.containsKey(chatId)) {
                    handleMatchInput(chatId, text);
                    return;
                }

                sendMessage(chatId.toString(), "Неизвестная команда ⚠️\nИспользуйте кнопки или /start для меню.");

            } else if (update.hasCallbackQuery()) {
                String data = update.getCallbackQuery().getData();
                Long chatId = update.getCallbackQuery().getMessage().getChatId();

                // Выбор первого игрока
                if (data.startsWith("player1_")) {
                    String player = data.replace("player1_", "");
                    MatchState state = new MatchState();
                    state.player1 = player;
                    state.step = "player2";
                    matchStates.put(chatId, state);
                    askForSecondPlayer(chatId, player);
                    return;
                }

                // Выбор второго игрока
                if (data.startsWith("player2_")) {
                    String player = data.replace("player2_", "");
                    MatchState state = matchStates.get(chatId);
                    if (state == null) return;
                    state.player2 = player;
                    state.step = "score";
                    sendMessage(chatId.toString(), "Теперь введи счёт в формате: `3 2` (через пробел).");
                    return;
                }

                // Подтверждение матча
                if (data.equals("confirm_yes")) {
                    MatchState state = matchStates.get(chatId);
                    if (state != null) {
                        saveMatch(chatId, state);
                        matchStates.remove(chatId);
                    }
                    return;
                } else if (data.equals("confirm_no")) {
                    matchStates.remove(chatId);
                    sendStartMenu(chatId);
                    return;
                }

                // Выбор даты
                if (data.equals("date_today")) {
                    MatchState state = matchStates.get(chatId);
                    if (state != null) {
                        state.date = LocalDate.now().toString();
                        confirmMatch(chatId, state);
                    }
                    return;
                } else if (data.equals("date_manual")) {
                    sendMessage(chatId.toString(), "Введите дату матча (например: 2025-11-07):");
                    MatchState state = matchStates.get(chatId);
                    if (state != null) state.step = "date";
                    return;
                }

                // Главное меню
                if (data.equals("add_match")) {
                    startMatchCreation(chatId);
                } else if (data.equals("show_stats")) {
                    showStatsOptions(chatId);
                } else if (data.startsWith("stats_")) {
                    String period = data.replace("stats_", "");
                    sendPeriodStats(chatId.toString(), period, LocalDate.now().toString());
                } else if (data.equals("main_menu")) {
                    sendStartMenu(chatId);
                }

                // История матчей
                else if (data.equals("history_ask")) {
                    askMatchHistoryCount(chatId);
                } else if (data.startsWith("history_")) {
                    try {
                        int limit = Integer.parseInt(data.replace("history_", ""));
                        sendMatchHistory(chatId, limit);
                    } catch (NumberFormatException e) {
                        sendMessage(chatId.toString(), "❌ Неверный параметр для истории матчей.");
                    }
                }
            }
        } catch (Exception e) {
            sendMessage("Error", "❌ Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // --- Главное меню ---
    private void sendStartMenu(Long chatId) {
        InlineKeyboardButton addMatch = new InlineKeyboardButton("➕ Добавить матч");
        addMatch.setCallbackData("add_match");

        InlineKeyboardButton showStats = new InlineKeyboardButton("📊 Посмотреть статистику");
        showStats.setCallbackData("show_stats");

        InlineKeyboardButton historyBtn = new InlineKeyboardButton("🕒 История матчей");
        historyBtn.setCallbackData("history_ask");

        InlineKeyboardButton mainMenu = new InlineKeyboardButton("🏠 Главное меню");
        mainMenu.setCallbackData("main_menu");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Collections.singletonList(addMatch));
        keyboard.add(Collections.singletonList(showStats));
        keyboard.add(Collections.singletonList(historyBtn));
        keyboard.add(Collections.singletonList(mainMenu));

        if (!greetedUsers.contains(chatId)) {
            greetedUsers.add(chatId);
            String[] jokes = {
                    "Почему мяч никогда не обманывает?\nПотому что у него всегда есть круглая правда! ⚽",
                    "Почему футболисты берут ручку на матч?\nЧтобы записывать голы! ✍️",
                    "Что сказал вратарь после игры?\n«Мяч, ты опять меня подвёл!» 🥅"
            };
            String joke = jokes[random.nextInt(jokes.length)];

            sendMessageWithButtons(chatId,
                    "⚽ Привет! Я FifaTrackerBot — твой личный ассистент по статистике FIFA.\n\n" +
                            "Вот шутка для разминки:\n" + joke + "\n\n" +
                            "Выбери, что хочешь сделать 👇",
                    keyboard);
        } else {
            sendMessageWithButtons(chatId, "Главное меню ⚽", keyboard);
        }
    }

    // --- Статистика ---
    private void showStatsOptions(Long chatId) {
        InlineKeyboardButton dayBtn = new InlineKeyboardButton("📅 День");
        dayBtn.setCallbackData("stats_daily");

        InlineKeyboardButton weekBtn = new InlineKeyboardButton("📆 Неделя");
        weekBtn.setCallbackData("stats_weekly");

        InlineKeyboardButton monthBtn = new InlineKeyboardButton("🗓 Месяц");
        monthBtn.setCallbackData("stats_monthly");

        InlineKeyboardButton yearBtn = new InlineKeyboardButton("📈 Год");
        yearBtn.setCallbackData("stats_yearly");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Arrays.asList(dayBtn, weekBtn));
        keyboard.add(Arrays.asList(monthBtn, yearBtn));

        sendMessageWithButtons(chatId, "Выбери период статистики:", keyboard);
    }

    private void sendPeriodStats(String chatId, String period, String date) {
        try {
            String url = "http://localhost:8080/api/results/" + period + "?date=" + date;
            PeriodResultDto result = restTemplate.getForObject(url, PeriodResultDto.class);

            if (result == null || result.getPlayers() == null || result.getPlayers().isEmpty()) {
                sendMessage(chatId, "📭 Нет данных за указанный период (" + period + ").");
                return;
            }

            StringBuilder msg = new StringBuilder("📊 Статистика за " + period + " (" +
                    result.getStartDate() + " — " + result.getEndDate() + "):\n\n");

            for (PlayerStatsDto p : result.getPlayers()) {
                msg.append(p.getName())
                        .append(": ")
                        .append(p.getTotalWins()).append("W / ")
                        .append(p.getTotalLosses()).append("L (")
                        .append(String.format("%.1f", p.getWinRate()))
                        .append("%)\n");
            }

            msg.append("\n🏆 Чемпион: ").append(result.getChampion())
                    .append("\n💀 Лузер: ").append(result.getLoser());

            sendMessage(chatId, msg.toString());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            sendMessage(chatId, "📭 Нет матчей за указанный период (" + period + ").");
        } catch (Exception e) {
            sendMessage(chatId, "❌ Ошибка при получении статистики: " + e.getMessage());
        }
    }

    // --- Добавление матча ---
    private void startMatchCreation(Long chatId) {
        matchStates.put(chatId, new MatchState());
        try {
            ResponseEntity<String[]> response = restTemplate.getForEntity("http://localhost:8080/api/players/names", String[].class);
            String[] players = response.getBody();
            if (players == null || players.length == 0) {
                sendMessage(chatId.toString(), "⚠️ Нет зарегистрированных игроков в базе.");
                return;
            }

            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
            for (String player : players) {
                InlineKeyboardButton button = new InlineKeyboardButton(player);
                button.setCallbackData("player1_" + player);
                buttons.add(Collections.singletonList(button));
            }

            sendMessageWithButtons(chatId,
                    "👤 Выбери первого игрока из списка:\n⚠️ Важно: имя должно совпадать с базой данных.",
                    buttons);

        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Ошибка при получении списка игроков: " + e.getMessage());
        }
    }

    private void askForSecondPlayer(Long chatId, String firstPlayer) {
        try {
            ResponseEntity<String[]> response = restTemplate.getForEntity("http://localhost:8080/api/players/names", String[].class);
            String[] players = response.getBody();

            if (players == null || players.length == 0) {
                sendMessage(chatId.toString(), "⚠️ Нет доступных игроков!");
                return;
            }

            List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
            for (String player : players) {
                if (!player.equals(firstPlayer)) {
                    InlineKeyboardButton button = new InlineKeyboardButton(player);
                    button.setCallbackData("player2_" + player);
                    buttons.add(Collections.singletonList(button));
                }
            }

            sendMessageWithButtons(chatId,
                    "👤 Теперь выбери второго игрока (не " + firstPlayer + "):",
                    buttons);
        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Ошибка при получении игроков: " + e.getMessage());
        }
    }

    private void handleMatchInput(Long chatId, String text) {
        MatchState state = matchStates.get(chatId);
        if (state == null) return;

        switch (state.step) {
            case "score":
                try {
                    String[] scores = text.trim().split(" ");
                    if (scores.length != 2) throw new IllegalArgumentException();
                    state.player1Score = Integer.parseInt(scores[0]);
                    state.player2Score = Integer.parseInt(scores[1]);

                    InlineKeyboardButton today = new InlineKeyboardButton("📅 Сегодня");
                    today.setCallbackData("date_today");
                    InlineKeyboardButton manual = new InlineKeyboardButton("🗓️ Указать вручную");
                    manual.setCallbackData("date_manual");
                    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                    keyboard.add(Collections.singletonList(today));
                    keyboard.add(Collections.singletonList(manual));
                    sendMessageWithButtons(chatId, "Выбери дату матча:", keyboard);

                } catch (Exception e) {
                    sendMessage(chatId.toString(), "⚠️ Неверный формат счёта. Пример: 3 2");
                }
                break;

            case "date":
                try {
                    LocalDate.parse(text.trim());
                    state.date = text.trim();
                    confirmMatch(chatId, state);
                } catch (Exception e) {
                    sendMessage(chatId.toString(), "⚠️ Неверный формат даты. Введите 2025-11-07 или 'сегодня'");
                }
                break;
        }
    }

    private void confirmMatch(Long chatId, MatchState state) {
        InlineKeyboardButton yes = new InlineKeyboardButton("✅ Да");
        yes.setCallbackData("confirm_yes");
        InlineKeyboardButton no = new InlineKeyboardButton("❌ Нет");
        no.setCallbackData("confirm_no");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Arrays.asList(yes, no));

        String msg = "📋 Подтвердите данные матча:\n" +
                "👤 " + state.player1 + " (" + state.player1Score + ")\n" +
                "👤 " + state.player2 + " (" + state.player2Score + ")\n" +
                "📅 Дата: " + state.date;
        sendMessageWithButtons(chatId, msg, keyboard);
    }

    private void saveMatch(Long chatId, MatchState state) {
        MatchRequest request = new MatchRequest();
        request.setPlayer1Name(state.player1);
        request.setPlayer2Name(state.player2);
        request.setPlayer1Score(state.player1Score);
        request.setPlayer2Score(state.player2Score);
        request.setDate(state.date);

        try {
            restTemplate.postForEntity("http://localhost:8080/api/matches", request, String.class);
            sendMessage(chatId.toString(),
                    "✅ Матч успешно добавлен!\n" +
                            state.player1 + " " + state.player1Score + ":" + state.player2Score + " " + state.player2 +
                            "\n📅 Дата: " + state.date);
        } catch (Exception e) {
            sendMessage(chatId.toString(),
                    "❌ Ошибка при добавлении матча: " + e.getMessage());
        }
    }

    // --- История матчей ---
    private void askMatchHistoryCount(Long chatId) {
        InlineKeyboardButton btn5 = new InlineKeyboardButton("5 матчей");
        btn5.setCallbackData("history_5");
        InlineKeyboardButton btn10 = new InlineKeyboardButton("10 матчей");
        btn10.setCallbackData("history_10");
        InlineKeyboardButton btn20 = new InlineKeyboardButton("20 матчей");
        btn20.setCallbackData("history_20");

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(Collections.singletonList(btn5));
        keyboard.add(Collections.singletonList(btn10));
        keyboard.add(Collections.singletonList(btn20));

        sendMessageWithButtons(chatId, "Выбери количество последних матчей:", keyboard);
    }

    private void sendMatchHistory(Long chatId, int limit) {
        try {
            String url = "http://localhost:8080/api/matches/history?limit=" + limit;
            List<Map<String, Object>> matches = restTemplate.getForObject(url, List.class);

            if (matches == null || matches.isEmpty()) {
                sendMessage(chatId.toString(), "📭 Нет матчей для отображения.");
                return;
            }

            StringBuilder msg = new StringBuilder("🕒 Последние " + limit + " матчей:\n\n");

            for (Map<String, Object> match : matches) {
                Map<String, Object> player1 = (Map<String, Object>) match.get("player1");
                Map<String, Object> player2 = (Map<String, Object>) match.get("player2");

                String player1Name = player1 != null ? (String) player1.get("name") : "null";
                String player2Name = player2 != null ? (String) player2.get("name") : "null";

                int player1Score = (Integer) match.get("player1Score");
                int player2Score = (Integer) match.get("player2Score");
                String date = (String) match.get("date");

                msg.append(date).append(" — ")
                        .append(player1Name).append(" ").append(player1Score)
                        .append(":").append(player2Score).append(" ")
                        .append(player2Name).append("\n");
            }

            sendMessage(chatId.toString(), msg.toString());

        } catch (Exception e) {
            sendMessage(chatId.toString(), "❌ Ошибка при получении истории матчей: " + e.getMessage());
        }
    }


    // --- Утилиты ---
    private void sendMessage(String chatId, String text) {
        try {
            int maxLength = 4000;
            if (text.length() > maxLength) text = text.substring(0, maxLength - 3) + "...";
            SendMessage message = new SendMessage(chatId, text);
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageWithButtons(Long chatId, String text, List<List<InlineKeyboardButton>> buttons) {
        try {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(buttons);
            SendMessage message = new SendMessage(chatId.toString(), text);
            message.setReplyMarkup(markup);
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}