package com.github.sorinescu.piqr;

import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.io.IOException;

class Logging {
    public static final String LOG_FILE_PATH = "/%t/piqr.log";
    public static final int MAX_LOG_SIZE = 1048576;
    public static final int MAX_LOG_FILES = 3;

    static {
        // Global log format
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "[%1$tF %1$tT.%1$tL][%4$s] %5$s%n");

        Logger logger = Logger.getLogger("");   // root logger

        try {
            // Creating an instance of FileHandler with 5 logging files
            // sequences.
            FileHandler handler = new FileHandler(LOG_FILE_PATH, MAX_LOG_SIZE, MAX_LOG_FILES, true);
            handler.setFormatter(new SimpleFormatter());
            logger.addHandler(handler);
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            logger.warning("Failed to initialize logger handler.");
        }
    }

    static Logger getLogger(String name) {
        return Logger.getLogger(name);
    }
}