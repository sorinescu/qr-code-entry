package com.github.sorinescu.piqr;

import java.io.IOException;
import java.util.logging.Logger;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import io.sentry.Sentry;

public class PiQRController {
    private static final Logger logger = Logging.getLogger(PiQRController.class.getName());

    @Parameter(names = "--api_url", description = "Base URL of authentication API", required = true)
    private String apiUrl;

    public static void main(String[] argv) throws IOException, InterruptedException {
        logger.info("Pi QR Controller");

        // Reads DSN from the "SENTRY_DSN" env var
        Sentry.init();

        try {
            PiQRController controller = new PiQRController();

            JCommander.newBuilder()
              .addObject(controller)
              .build()
              .parse(argv);

            WebcamQRCodeWatcher qrCodeWatcher = new WebcamQRCodeWatcher();
            qrCodeWatcher.start();

            PiQRValidKeyCache validKeyCache = new PiQRValidKeyCache(controller.apiUrl);
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
            Sentry.capture(e);
        }
    }
}