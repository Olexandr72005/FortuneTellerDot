package com.Bot.FortuneTellerBot.connection;

import com.Bot.FortuneTellerBot.PropertiesUtil;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**

 Клас, що відповідає за керування підключеннями до бази даних.
 */
public final class ConnectionManager {

    public static final Logger LOGGER = LoggerFactory.getLogger(ConnectionManager.class.getSimpleName());
    private static final String URL_KEY = "db.url";
    private static final String USERNAME_KEY = "db.username";
    private static final String PASSWORD_KEY = "db.password";
    private static final String POOL_SIZE_KEY = "db.pool.size";
    private static final Integer DEFAULT_POOL_SIZE = 5;
    private static BlockingQueue<Connection> pool;
    private static List<Connection> sourceConnections;

    static {
        loadDriver();
        initConnectionPool();
    }

    /**

     Отримує з'єднання з пула підключень до бази даних.
     @return з'єднання з базою даних
     */
    public static Connection get() {
        try {
            LOGGER.info("connection received from pool[{}]", pool.size());
            return pool.take();
        } catch (InterruptedException e) {
            LOGGER.error("failed to take connection from pool. {}", e);
            throw new RuntimeException(e);
        }
    }

    private static void loadDriver() {
        try {
            String driverName = "com.mysql.cj.jdbc.Driver";
            Class.forName(driverName);
            LOGGER.info("driver [{}] loaded", driverName);
        } catch (ClassNotFoundException e) {
            LOGGER.error("driver failed to load. {}", e);
            throw new RuntimeException(e);
        }
    }
    /**

     Відкриває нове з'єднання з базою даних.
     @return з'єднання з базою даних
     */
    private static Connection open() {
        try {
            return DriverManager.getConnection(
                PropertiesUtil.get(URL_KEY),
                PropertiesUtil.get(USERNAME_KEY),
                PropertiesUtil.get(PASSWORD_KEY)
            );
        } catch (SQLException e) {
            LOGGER.error("failed to open connection. {}", e);
            throw new RuntimeException(e);
        }
    }
    /**

     Ініціалізує пул підключень до бази даних.
     */
    private static void initConnectionPool() {
        String poolSize = PropertiesUtil.get(POOL_SIZE_KEY);
        int size = poolSize == null ? DEFAULT_POOL_SIZE : Integer.parseInt(poolSize);
        LOGGER.info("connection pool size = {}", size);
        pool = new ArrayBlockingQueue<>(size);
        sourceConnections = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Connection connection = open();
            Connection proxyConnection = (Connection) Proxy.newProxyInstance(
                ConnectionManager.class.getClassLoader(),
                new Class[]{Connection.class},
                ((proxy, method, args) -> method.getName().equals("close")
                    ? pool.add((Connection) proxy)
                    : method.invoke(connection, args)));
            pool.add(proxyConnection);
            sourceConnections.add(connection);
            LOGGER.info("connection №{} opened", i + 1);
        }
    }
}