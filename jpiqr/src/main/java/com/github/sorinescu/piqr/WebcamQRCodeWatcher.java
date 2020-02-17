package com.github.sorinescu.piqr;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.EciEncoding;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.GrayU8;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import io.sentry.Sentry;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.SynchronousQueue;
import java.util.logging.Logger;

class WebcamQRCodeWatcher extends Thread implements SignalHandler {
    private static final Logger logger = Logging.getLogger(WebcamQRCodeWatcher.class.getName());

    private LifecycleManager lifecycleManager = null;
    private SynchronousQueue<String> codeQueue = new SynchronousQueue();
    private BufferedImage currentImage = null;

    static {
        // This is necessary for RaspberryPi
        Webcam.setDriver(new V4l4jDriver());
    }

    public WebcamQRCodeWatcher(LifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    public void run() {
        logger.info("Starting QR code reader thread");

        Signal.handle(new Signal("USR2"), this);

        Webcam webcam = null;

        while (true) {
            try {
                webcam = UtilWebcamCapture.openDefault(320, 240);

                logger.info("Opened webcam " + webcam.getName());
            } catch (Exception e) {
                logger.severe("Got fatal exception in webcam thread: " + e.toString());
                Sentry.capture(e);

                // System.exit() does not exit correctly here
                // System.exit(-1);
                Runtime.getRuntime().halt(-1);
            }

            try {
                ConfigQrCode qrConfig = new ConfigQrCode();
                qrConfig.forceEncoding = EciEncoding.ISO8859_1;
                QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(qrConfig, GrayU8.class);
                logger.info("Opened QR code detector");

                while (true) {
                    BufferedImage input = webcam.getImage();
                    int nullCount = 0;

                    while (input == null) {
                        ++nullCount;

                        if (nullCount > 2)
                            logger.info("Got " + Integer.toString(nullCount) + " null webcam images");

                        if (nullCount == 10) {
                            // This can't be salvaged
                            throw new RuntimeException("Got too many null webcam images");
                        } else
                            Thread.sleep(1000);

                        input = webcam.getImage();
                    }

                    synchronized (this) {
                        currentImage = input;
                    }

                    // logger.info("Got image");

                    GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8) null);
                    // logger.info("Converted image");

                    detector.process(gray);
                    // logger.info("Processed image");

                    // Get's a list of all the qr codes it could successfully detect and decode
                    List<QrCode> detections = detector.getDetections();
                    // logger.info("Got detections: " + detections.toString());

                    for (QrCode qr : detections) {
                        if (isAlphanumeric(qr.message)) {
                            logger.info("Got alphanumeric QR code: " + qr.message);
                            codeQueue.offer(qr.message);
                        } else {
                            String message = getUUIDFromStr(qr.message);
                            logger.info("Got raw QR code: " + message);
                            codeQueue.offer(message);
                        }
                    }

                    // List of objects it thinks might be a QR Code but failed for various reasons
                    List<QrCode> failures = detector.getFailures();
                    // logger.info("Got failures: " + failures.toString());
                    for (QrCode qr : failures) {
                        // If the 'cause' is ERROR_CORRECTION or later then it's probably a real QR Code that
                        if (qr.failureCause.ordinal() < QrCode.Failure.ERROR_CORRECTION.ordinal())
                            continue;

                        logger.info("QR failure");
                    }
                }
            } catch (Exception e) {
                logger.severe("Got exception in webcam thread: " + e.toString());
                Sentry.capture(e);

                lifecycleManager.reboot();
            }
        }
    }

    public static boolean isAlphanumeric(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isDigit(c) && !Character.isLetter(c))
                return false;
        }
        return true;
    }

    public static String getUUIDFromBytes(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte aByte : bytes) {
            buffer.append(String.format("%02x", aByte));
        }
        return buffer.toString();
    }

    public static String getUUIDFromStr(String s) {
        try {
            return getUUIDFromBytes(s.getBytes(EciEncoding.ISO8859_1));
        } catch (UnsupportedEncodingException e) {
            return "invalid-uuid";
        }
    }

    public String getQRCode() {
        try {
            return codeQueue.take();
        } catch (InterruptedException e) {
            logger.severe("Got exception" + e.toString());
            Sentry.capture(e);
            return "";
        }
    }

    public void handle(Signal signal) {
        logger.info("Signal: " + signal);

        if (signal.toString().trim().equals("SIGTERM")) {
            System.out.println("SIGTERM raised, terminating...");
            System.exit(1);
        }

        synchronized (this) {
            if (currentImage != null)
                UtilImageIO.saveImage(currentImage, "/tmp/piqr-webcam-image.jpg");
        }
    }
}