package com.pomodoro.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;

public class Main {

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        var pomodoroBot = new PomodoroBot();
        telegramBotsApi.registerBot(pomodoroBot);
        new Thread(() -> {
            try {
                pomodoroBot.checkTimer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }


    static class PomodoroBot extends TelegramLongPollingBot {

        @Override
        public String getBotUsername() {
            return "Pomodoro bot";
        }

        @Override
        public String getBotToken() {
            return "";
        }

        private static final ConcurrentHashMap<Timer, Long> timers = new ConcurrentHashMap<>();

        enum TimerType {
            WORK,
            BREAK
        }

        record Timer(Instant timer, TimerType timerType){}

        @Override
        public void onUpdateReceived(Update update) {
            if (update.hasMessage() && update.getMessage().hasText()) {
                var chatId = update.getMessage().getChatId();
                if (update.getMessage().getText().equals("/start")) {
                    sendMsg(chatId.toString(), """
                            Pomodoro - сделай свое время более эффективным.
                            Задай мне время работы и отдыха через пробел. Например, '1 1'.
                            PS Я работаю пока в минутах
                            """);
                } else {
                    var args = update.getMessage().getText().split(" ");
                    if (args.length >= 1) {
                        // 18:42 5
                        // 18:47 -- таймер работы истек
                        var workTime = Instant.now().plus(Long.parseLong(args[0]), ChronoUnit.MINUTES);

                        // save
                        // system.now() - 18:47
                        // хранилище -> все истекщие таймеры
                        // key-value, map - ["cat", "кот", "dog", "собака"]
                        // map["cat"] = кот
                        // 111 18:47

                        // 18:42 5 2
                        // workTime - 18:47
                        // workTime + 2 = breakTime
                        timers.put(new Timer(workTime, TimerType.WORK), chatId);
                        if (args.length >= 2) {
                            var breakTime = workTime.plus(Long.parseLong(args[1]), ChronoUnit.MINUTES);
                            timers.put(new Timer(breakTime, TimerType.BREAK), chatId);
                        }
                    }
                }
            }
        }

        @SuppressWarnings("BusyWait")
        public void checkTimer() throws InterruptedException {
            //noinspection InfiniteLoopStatement
            while (true) {
                System.out.println("Количество таймеров пользователей " + timers.size());
                timers.forEach((timer, userId) -> {
                    if (Instant.now().isAfter(timer.timer)) {
                        timers.remove(timer);
                        switch (timer.timerType) {
                            case WORK -> sendMsg(userId.toString(), "Пора отдыхать");
                            case BREAK -> sendMsg(String.valueOf(userId), "Таймер завершил свою работу");
                        }
                    }
                });
                sleep(3000);
            }
        }

        private void sendMsg(String chatId, String text) {
            SendMessage msg = new SendMessage(chatId, text);
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

}
