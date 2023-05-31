module FortuneTellerBot {
  requires javafx.graphics;
  requires spring.boot;
  requires spring.boot.autoconfigure;
  requires javafx.fxml;
  requires javafx.controls;
  requires java.sql;
  requires spring.context;
  requires spring.beans;

  requires telegrambots.meta;
  requires telegrambots;
  requires org.slf4j;

  requires org.apache.commons.codec;
  requires java.desktop;
  requires java.sql.rowset;
  requires reload4j;
  requires spring.core;

  opens com.Bot.FortuneTellerBot.application to javafx.graphics, javafx.fxml, lombok, spring.core, spring.beans, spring.context;
  opens com.Bot.FortuneTellerBot.config to lombok, spring.core, spring.beans, spring.context;
  opens com.Bot.FortuneTellerBot.service to lombok, spring.core, spring.beans, spring.context;
  opens com.Bot.FortuneTellerBot to lombok, spring.core, spring.beans, spring.context;

  exports com.Bot.FortuneTellerBot;
  exports com.Bot.FortuneTellerBot.config;
  exports com.Bot.FortuneTellerBot.service;
  exports com.Bot.FortuneTellerBot.application;
  exports com.Bot.FortuneTellerBot.connection;
}