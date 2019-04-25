package com.github.sorinescu.piqr;

import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;
import io.sentry.Sentry;


class LifecycleManager {
    private static final Logger logger = Logging.getLogger(LifecycleManager.class.getName());

    private PiRelayController relayController;

    LifecycleManager(Config config, PiRelayController relayController) {
        this.relayController = relayController;

        if (config.serviceExitTimeoutMinutes > 0)
            scheduleShutdown(config.serviceExitTimeoutMinutes);
    }

    private void scheduleShutdown(int timeoutMinutes) {
        Timer timer = new Timer();

        logger.warning("Scheduling service shutdown in " + timeoutMinutes + " minutes");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                logger.info("Shutting down after timeout");
                resetWebcamAndExit(0);
            }
        }, timeoutMinutes * 60 * 1000);
    }

    private void resetWebcam() throws InterruptedException {
        logger.warning("Power cycling the webcam");
        relayController.resetWebcam();
    }

    public void reboot() {
        // System.exit() does not exit correctly here
        // System.exit(-2);

        // Not even halt() works all the time - if the webcam fails, the process can't exit
        // Runtime.getRuntime().halt(-2);

        Runtime rt = Runtime.getRuntime();

        try {
            resetWebcam();

            logger.warning("Rebooting");
            rt.exec("reboot");
        } catch (Exception e) {
            logger.severe("Got exception while rebooting: " + e.toString());
            Sentry.capture(e);
        }
    }

    public void resetWebcamAndExit(int status) {
        try {
            resetWebcam();
        } catch (Exception e) {
            logger.severe("Got exception while resetting webcam: " + e.toString());
            Sentry.capture(e);
        }

        System.exit(status);
    }
}
