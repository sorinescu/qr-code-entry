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

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

public class WebcamQRCodeWatcher extends Thread {
	private SynchronousQueue<String> codeQueue = new SynchronousQueue();

	static {
		// This is necessary for RaspberryPi
		Webcam.setDriver(new V4l4jDriver());
	}

	public void run() {
		System.out.println("Starting QR code reader thread");

		Webcam webcam = UtilWebcamCapture.openDefault(320, 240);
		System.out.println("Opened webcam");

		QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null, GrayU8.class);
		System.out.println("Opened QR code detector");


		while( true ) {
			BufferedImage input = webcam.getImage();
			if( input == null ) break;

			// System.out.println("Got image");

			GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8)null);
			// System.out.println("Converted image");

			detector.process(gray);
			// System.out.println("Processed image");

			// Get's a list of all the qr codes it could successfully detect and decode
			List<QrCode> detections = detector.getDetections();
			// System.out.println("Got detections: " + detections.toString());

			for( QrCode qr : detections ) {
				// The message encoded in the marker
				System.out.println("Got QR code: " + qr.message);

				codeQueue.offer(qr.message);
			}

			// List of objects it thinks might be a QR Code but failed for various reasons
			List<QrCode> failures = detector.getFailures();
			// System.out.println("Got failures: " + failures.toString());
			for( QrCode qr : failures ) {
				// If the 'cause' is ERROR_CORRECTION or later then it's probably a real QR Code that
				if( qr.failureCause.ordinal() < QrCode.Failure.ERROR_CORRECTION.ordinal() )
					continue;

				System.out.println("QR failure");
			}
		}
	}

	public String getQRCode() {
		try {
			return codeQueue.take();
		} catch (InterruptedException e) {
			System.err.println("Got exception" + e.toString());
			return "";
		}
	}
}