package io.proj3ct.FinancialAssistanceBot.service;

import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.FinancialAssistanceBot.model.User;
import io.proj3ct.FinancialAssistanceBot.model.UserRepository;
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


@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${bot.name}")
    private String botName;
    @Value("${bot.token}")
    private String botToken;

    public static final String ENTER_EXPENSE = "ENTER_EXPENSE";
    public static final String ENTER_CATEGORY = "ENTER_CATEGORY";
    public static final String SHOW_ALL_EXPENSES = "SHOW_ALL_EXPENSES";
    static final String ERROR_TEXT = "Произошла ошибка: ";

    @Autowired
    private UserRepository userRepository;

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            String messageText = update.getMessage().getText();

            long chatId = update.getMessage().getChatId();

            switch (messageText) {

                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                case "Доступные команды":
                    menu(chatId);
                    break;
                default:
                    sendMessage(chatId, "Извини данный функционал пока не поддерживается");
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.contains(ENTER_EXPENSE)){
                String text = "Введите трату: ";
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.contains(ENTER_CATEGORY)) {
                String text = "Введите категорию: ";
                executeEditMessageText(text, chatId, messageId);
            } else if (callbackData.contains(SHOW_ALL_EXPENSES)) {
                String text = "Ваши траты: ";
                executeEditMessageText(text, chatId, messageId);

            }
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Привет, " + name + " :blush:");
        log.info("Ответил пользователю: " + name);
        sendMessage(chatId, answer);
        keyBoard(chatId);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);

    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("Пользователь добавлен: " + user);
        }
    }

    private void menu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Доступные команды");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();

        List<InlineKeyboardButton> rowOne = new ArrayList<>();
        var buttonOne = new InlineKeyboardButton();
        buttonOne.setText("Добавить трату:");
        // Это ид сообщения, оно константное
        buttonOne.setCallbackData(ENTER_EXPENSE);

        List<InlineKeyboardButton> rowTwo = new ArrayList<>();
        var buttonTwo = new InlineKeyboardButton();
        buttonTwo.setText("Добавить категорию:");
        buttonTwo.setCallbackData(ENTER_CATEGORY);

        List<InlineKeyboardButton> rowThree = new ArrayList<>();
        var buttonThree = new InlineKeyboardButton();
        buttonThree.setText("Показать все траты:");
        buttonThree.setCallbackData(SHOW_ALL_EXPENSES);


        rowOne.add(buttonOne);
        rowTwo.add(buttonTwo);
        rowThree.add(buttonThree);

        rowsInLine.add(rowOne);
        rowsInLine.add(rowTwo);
        rowsInLine.add(rowThree);

        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);


        executeMessage(message);
    }

    private void keyBoard(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Доступные команды");

        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        keyboardMarkup.setResizeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);

    }

    private void executeMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
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