package io.proj3ct.FinancialAssistantBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.sql.Timestamp;

@Entity(name = "usersDataTable")
public class User {

    @Id
    private Long chatId;
    private String firstName;
    private String lastName;
    private String userName;
    private Timestamp registereAt;
    private Integer expense;
    private String category;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }



    public Integer getExpense() {
        return expense;
    }

    public void setExpense(Integer expense) {
        this.expense = expense;
    }


    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Timestamp getRegistereAt() {
        return registereAt;
    }

    public void setRegistereAt(Timestamp registereAt) {
        this.registereAt = registereAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", userName='" + userName + '\'' +
                ", registereAt=" + registereAt +
                ", expense=" + expense +
                ", category=" + category +
                '}';
    }
}
