package com.github.sorinescu.piqr;

import java.io.IOException;
import java.util.logging.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.sentry.Sentry;

public class PiQRController {
    private static final Logger logger = Logging.getLogger(PiQRController.class.getName());

    @Parameter(names = "--config", description = "Configuration file path")
    private String configFilePath = "piqr.properties";

    public static void main(String[] argv) throws IOException, InterruptedException {
        logger.info("Pi QR Controller");

        try {
            PiQRController controller = new PiQRController();

            JCommander.newBuilder()
              .addObject(controller)
              .build()
              .parse(argv);

            Config config = new Config(controller.configFilePath);

            Sentry.init(config.sentryDsn);

            WebcamQRCodeWatcher qrCodeWatcher = new WebcamQRCodeWatcher(config);
            qrCodeWatcher.setDaemon(true);
            qrCodeWatcher.start();

            PiQRValidKeyCache validKeyCache = new PiQRValidKeyCache(config.apiUrl, config.apiKey, config.hardcodedQRCode);
            validKeyCache.setDaemon(true);
            validKeyCache.start();

            try (PiDoorOpener doorOpener = new PiDoorOpener()) {
                while (true) {
                    String key = qrCodeWatcher.getQRCode().trim().toLowerCase();

                    if (validKeyCache.authorize(key)) {
                        doorOpener.openDoor();
                        // TODO: notify key manager that the key has been used to upen the door
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Got exception in main: " + e.toString());
            Sentry.capture(e);
            System.exit(-1);
        }
    }
}