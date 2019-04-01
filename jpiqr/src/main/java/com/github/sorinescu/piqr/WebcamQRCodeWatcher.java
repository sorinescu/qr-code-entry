package com.github.sorinescu.piqr;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import io.sentry.Sentry;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Logger;
import java.util.concurrent.SynchronousQueue;

class WebcamQRCodeWatcher extends Thread {
	private static final Logger logger = Logging.getLogger(WebcamQRCodeWatcher.class.getName());

	private SynchronousQueue<String> codeQueue = new SynchronousQueue();

	static {
		// This is necessary for RaspberryPi
		Webcam.setDriver(new V4l4jDriver());
	}

	public void run() {
		logger.info("Starting QR code reader thread");

		long exceptionResetT0 = System.currentTimeMillis();
		int exceptionCount = 0;

		Webcam webcam = null;

		while (true) {
			try {
				webcam = UtilWebcamCapture.openDefault(320, 240);
			} catch (Exception e) {
				logger.severe("Got fatal exception in webcam thread: " + e.toString());
				Sentry.capture(e);

				System.exit(-1);
			}

			try {
				logger.info("Opened webcam");

				QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null, GrayU8.class);
				logger.info("Opened QR code detector");

				while (true) {
					BufferedImage input = webcam.getImage();
					if (input == null)
						break;

					// logger.info("Got image");

					GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8)null);
					// logger.info("Converted image");

					detector.process(gray);
					// logger.info("Processed image");

					// Get's a list of all the qr codes it could successfully detect and decode
					List<QrCode> detections = detector.getDetections();
					// logger.info("Got detections: " + detections.toString());

					for (QrCode qr : detections) {
						// The message encoded in the marker
						logger.info("Got QR code: " + qr.message);

						codeQueue.offer(qr.message);
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

				// We allow at most 5 exceptions per minute to prevent Sentry flooding
				long t = System.currentTimeMillis();
				if (t - exceptionResetT0 > 60000) {
					exceptionResetT0 = t;
					exceptionCount = 0;
				}

				if (++exceptionCount == 5) {
					logger.severe("Got too many exceptions too quickly in webcam thread, exiting");
					System.exit(-2);
				}

				Sentry.capture(e);
			} finally {
				webcam.close();
			}
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
}