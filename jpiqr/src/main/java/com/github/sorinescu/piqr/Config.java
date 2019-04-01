package com.github.sorinescu.piqr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

class Config {
    String apiUrl;
    String apiKey;
    String sentryDsn;
    String hardcodedQRCode;

    public Config(String configFilePath) throws IOException {
        InputStream inputStream = null;

        try {
            Properties prop = new Properties();
            inputStream = getClass().getClassLoader().getResourceAsStream(configFilePath);

            if (inputStream != null)
                prop.load(inputStream);
            else
                throw new FileNotFoundException("Property file '" + configFilePath + "' not found in the classpath");

            apiUrl = prop.getProperty("apiUrl", System.getProperty("apiUrl"));
            apiKey = prop.getProperty("apiKey", System.getProperty("apiKey"));
            sentryDsn = prop.getProperty("sentryDsn", System.getProperty("sentryDsn"));
            hardcodedQRCode = prop.getProperty("hardcodedQRCode", System.getProperty("hardcodedQRCode"));
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
    }
}