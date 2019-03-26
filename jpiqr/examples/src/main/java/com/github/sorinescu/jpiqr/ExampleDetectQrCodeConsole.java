package com.github.sorinescu.jpiqr;

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

public class ExampleDetectQrCodeConsole {
	public void run() {
		Webcam webcam = UtilWebcamCapture.openDefault(320, 240);
		QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null, GrayU8.class);

		// Warm it up
		webcam.getImage();

		int frames = 0;
		long t0 = System.nanoTime();

		while( true ) {
			BufferedImage input = webcam.getImage();
			if( input == null ) break;

			if (true) {
				GrayU8 gray = ConvertBufferedImage.convertFrom(input, (GrayU8)null);

				detector.process(gray);

				// Get's a list of all the qr codes it could successfully detect and decode
				List<QrCode> detections = detector.getDetections();

				for( QrCode qr : detections ) {
					// The message encoded in the marker
					System.out.println("\nmessage: "+qr.message);
				}

				// List of objects it thinks might be a QR Code but failed for various reasons
				List<QrCode> failures = detector.getFailures();
				for( QrCode qr : failures ) {
					// If the 'cause' is ERROR_CORRECTION or later then it's probably a real QR Code that
					if( qr.failureCause.ordinal() < QrCode.Failure.ERROR_CORRECTION.ordinal() )
						continue;

					System.out.println("\nfailure: "+qr.message);
				}
			}

			++frames;
			System.out.printf("\rFPS: %.02f", frames * 1e9 / (double)(System.nanoTime() - t0));
		}
	}

	public static void main(String[] args) {
		// This is necessary for RaspberryPi
		Webcam.setDriver(new V4l4jDriver());

		ExampleDetectQrCodeConsole app = new ExampleDetectQrCodeConsole();
		app.run();
	}
}