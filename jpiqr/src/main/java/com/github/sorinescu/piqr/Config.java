package com.github.sorinescu.piqr;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.Properties;

class Config {
    String apiUrl;
    String apiKey;
    String sentryDsn;
    String hardcodedQRCode;
    int doorOpenGpioPin;
    int webcamPowerGpioPin;

    public Config(String configFilePath) throws IOException {
        InputStream inputStream = null;

        try {
            Properties prop = new Properties();
            // inputStream = getClass().getClassLoader().getResourceAsStream(configFilePath);
            inputStream = new FileInputStream(configFilePath);
            prop.load(inputStream);

            apiUrl = prop.getProperty("apiUrl", System.getProperty("apiUrl"));
            if (apiUrl == null)
                throw new InvalidParameterException("Parameter 'apiUrl' is required");
            apiKey = prop.getProperty("apiKey", System.getProperty("apiKey"));
            if (apiKey == null)
                throw new InvalidParameterException("Parameter 'apiKey' is required");

            sentryDsn = prop.getProperty("sentryDsn", System.getProperty("sentryDsn"));
            hardcodedQRCode = prop.getProperty("hardcodedQRCode", System.getProperty("hardcodedQRCode"));

            doorOpenGpioPin = Integer.parseInt(prop.getProperty("doorOpenGpioPin", System.getProperty("doorOpenGpioPin", "2")));
            webcamPowerGpioPin = Integer.parseInt(prop.getProperty("webcamPowerGpioPin", System.getProperty("webcamPowerGpioPin", "3")));
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
    }
}