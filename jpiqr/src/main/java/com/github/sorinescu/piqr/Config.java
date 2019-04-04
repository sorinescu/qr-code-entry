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
    String hubctrlExe;
    int webcamUsbHub;
    int webcamUsbPort;

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

            hubctrlExe = prop.getProperty("hubctrlExe", System.getProperty("hubctrlExe"));
            webcamUsbHub = Integer.parseInt(prop.getProperty("webcamUsbHub", System.getProperty("webcamUsbHub", "0")));
            webcamUsbPort = Integer.parseInt(prop.getProperty("webcamUsbPort", System.getProperty("webcamUsbPort", "0")));
        } finally {
            if (inputStream != null)
                inputStream.close();
        }
    }
}