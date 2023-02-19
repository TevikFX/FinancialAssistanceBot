package io.proj3ct.FinancialAssistantBot.service;

import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.FinancialAssistantBot.model.User;
import io.proj3ct.FinancialAssistantBot.model.UserRepisitory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;


//На веб-хуках
@Slf4j
// Slf4j создает объект через который мы можем писать логи
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${bot.name}")
    private String botName;
    @Value("${bot.token}")
    private String botToken;

    public static final String ENTER_EXPENSE = "ENTER_EXPENSE";
    public static final String SHOW_ALL_EXPENSES = "SHOW_ALL_EXPENSES";
    public static final String DELETE_EXPENSE = "DELETE_EXPENSE";
    public static final String DELETE_CATEGORY = "DELETE_CATEGORY";
    public static final String GET_BIG_CATEGORY = "GET_BIG_CATEGORY";
    static final String ERROR_TEXT = "Произошла ошибка: ";

    // Прикручиваем репозиторий
    @Autowired
    private UserRepisitory userRepisitory;

    @Override
    // Сюда нужно предоставить имя бота
    public String getBotUsername() {
        return botName;
    }

    @Override
    // Сюда нужно предоставить наш IP ключ
    public String getBotToken() {
        return botToken;
    }

    @Override
    // Тут происходит обработка того что нам пишет пользователь и возвращает ответ
    public void onUpdateReceived(Update update) {

        //1. проверяем что есть текст
        //2. и есть ли текст в сообщении
        if (update.hasMessage() && update.getMessage().hasText()) {

            //Обработка того что нам написали
            String messageText = update.getMessage().getText();

            // Что бы бот мог нам написать, ему необходимо знать ID пользователя
            long chatId = update.getMessage().getChatId();

            // В зависимости от того что в messageText мы выполняем действие
            switch (messageText) {

                // Начало общения
                case "/start":
                    // Регистрация пользователя который нажал старт
                    registerUser(update.getMessage());
                    // Тут НАПРИМЕР бот отвечает нам(пользователю) сообщением приветствия
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "Доступные команды":
                    menu(chatId);
                    break;
                default:
                    sendMessage(chatId, "Извини данный функционал пока не поддерживается");
            }

            // Если прислали ид кнопки
        } else if (update.hasCallbackQuery()) {
            // ИД сообщения например: "SHOW_ALL_EXPENSES"
            String callbackData = update.getCallbackQuery().getData();
            // Для обновления текста над кнопками
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            // Получение чатИд
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            //Рефакторинг
            // Проверяем что за ид мы получили (какую кнопку нажали)
            if (callbackData.contains(ENTER_EXPENSE)) {
                String text = "Введите трату: ";
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.contains(SHOW_ALL_EXPENSES)) {
                String text = "Ваши траты: ";
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.contains(DELETE_EXPENSE)) {
                String text = "Трата удалена";
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.contains(DELETE_CATEGORY)) {
                String text = "Категория удалена";
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.contains(GET_BIG_CATEGORY)) {
                String text4 = "Ваша самая большая категория";
                executeEditMessageText(text4, chatId, messageId);
            }
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();

        }
    }

    //Создание меню с возможностями бота (Сами кнопки под сообщением)


    // Метод начала работы(тут можно добавить меню после приветствия или еще что-то интересное)
    // Во многих методах этого бота chatId это ID нашего пользователя
    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Привет, " + name + " :blush:");
        // Добавляем запись в журнал
        log.info("Ответил пользователю: " + name);
        // Вызываем метод для отправки сообщения
        sendMessage(chatId, answer);

    }

    // Метод для отправки сообщения
    // Метод рассылки сообщений
    private void sendMessage(long chatId, String textToSend) {
        // Это специальный клас для отправки сообщения ботом
        SendMessage message = new SendMessage();
        // Что-бы что-то отправить обязательно нужен chatId
        // Присвоение ИД исходящему сообщению (нужно перевести в String)
        message.setChatId(String.valueOf(chatId));
        // Само сообщение
        message.setText(textToSend);

        executeMessage(message);

    }

    // Метод регистрации пользователя
    private void registerUser(Message msg) {
        // Проверяем зарегистрирован ли пользователь с таким id
        // Мы вызываем метод findById и если он пуст, данного пользователя не существует и мы его добавляем
        if (userRepisitory.findById(msg.getChatId()).isEmpty()) {
            // Добавляем пользователя(мы можем использовать локальные переменные, он сам определит к какому
            // типу данных относится)
            var chatId = msg.getChatId();
            // Нужен чат
            var chat = msg.getChat();
            // Добавляем объект User
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegistereAt(new Timestamp(System.currentTimeMillis()));
            //Сохранение
            userRepisitory.save(user);
            log.info("Пользователь добавлен: " + user);
        }
    }

    private void menu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        // Это сообщение будет выводиться первым, а под ним будут кнопки
        message.setText("Доступные команды:");

        // Сами кнопки
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();

        //Список в котором хранятся кнопки
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        // Первая кнопка
        List<InlineKeyboardButton> rowOne = new ArrayList<>();
        var buttonOne = new InlineKeyboardButton();
        buttonOne.setText("Добавить трату:");
        // Это ид сообщения, оно константное
        buttonOne.setCallbackData(ENTER_EXPENSE);

        // Вторая кнопка
        List<InlineKeyboardButton> rowTwo = new ArrayList<>();
        var buttonTwo = new InlineKeyboardButton();
        buttonTwo.setText("Показать все траты:");
        buttonTwo.setCallbackData(SHOW_ALL_EXPENSES);

        //Третья кнопка
        List<InlineKeyboardButton> rowThree = new ArrayList<>();
        var buttonThree = new InlineKeyboardButton();
        buttonThree.setText("Удалить трату:");
        buttonThree.setCallbackData(DELETE_EXPENSE);

        //Четвертая кнопка
        List<InlineKeyboardButton> rowFour = new ArrayList<>();
        var buttonFour = new InlineKeyboardButton();
        buttonFour.setText("Удалить категорию:");
        buttonFour.setCallbackData(DELETE_CATEGORY);

        //Пятая кнопка
        List<InlineKeyboardButton> rowFive = new ArrayList<>();
        var buttonFive = new InlineKeyboardButton();
        buttonFive.setText("Получить имя самой дорогой категории:");
        buttonFive.setCallbackData(GET_BIG_CATEGORY);

        rowOne.add(buttonOne);
        rowTwo.add(buttonTwo);
        rowThree.add(buttonThree);
        rowFour.add(buttonFour);
        rowFive.add(buttonFive);

        rowsInLine.add(rowOne);
        rowsInLine.add(rowTwo);
        rowsInLine.add(rowThree);
        rowsInLine.add(rowFour);
        rowsInLine.add(rowFive);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);

        executeMessage(message);
    }

    //Кнопка команд
    private void keyBoard(SendMessage message) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Доступные команды");

        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    // Метод, что бы не повторять
    private void executeMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        // Класс позволяет заменить текст сообщения
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }


}