package com.github.sorinescu.jpiqr;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.EciEncoding;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.factory.fiducial.ConfigQrCode;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.GrayU8;
import com.github.sarxos.webcam.Webcam;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class ExampleDetectQrCode extends JPanel {
	JFrame window;
	BufferedImage workImage;

	public ExampleDetectQrCode() {
		window = new JFrame("QR Code Detector");
		window.setContentPane(this);
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}

	@Override
	public void paint (Graphics g) {
		if( workImage != null ) {
			// draw the work image and be careful to make sure it isn't being manipulated at the same time
			synchronized (workImage) {
				((Graphics2D) g).drawImage(workImage, 0, 0, null);
			}
		}
	}

	public void run() {
		Webcam webcam = UtilWebcamCapture.openDefault(640, 480);

		Dimension actualSize = webcam.getViewSize();
		setPreferredSize(actualSize);
		setMinimumSize(actualSize);
		window.setMinimumSize(actualSize);
		window.setPreferredSize(actualSize);
		window.setVisible(true);

		workImage = new BufferedImage((int)actualSize.getWidth(), (int)actualSize.getHeight(), BufferedImage.TYPE_INT_RGB);

		ConfigQrCode qrConfig = new ConfigQrCode();
		qrConfig.forceEncoding = EciEncoding.ISO8859_1;
		QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(qrConfig, GrayU8.class);

		while( true ) {
			BufferedImage input = webcam.getImage();
			if( input == null ) continue;

			GrayU8 gray = ConvertBufferedImage.convertFrom(input,(GrayU8)null);

			detector.process(gray);

			// Get's a list of all the qr codes it could successfully detect and decode
			List<QrCode> detections = detector.getDetections();

			synchronized( workImage ) {
				// copy the latest image into the work buffered
				Graphics2D g2 = workImage.createGraphics();
				g2.drawImage(input, 0, 0, null);

				int strokeWidth = Math.max(4,input.getWidth()/200); // in large images the line can be too thin
				g2.setColor(Color.GREEN);
				g2.setStroke(new BasicStroke(strokeWidth));

				for( QrCode qr : detections ) {
					if (isAlphanumeric(qr.message)) {
						System.out.println("Alphanumeric message: " + qr.message);
					} else {
						System.out.println("Got bytes string len " + qr.message.length() + " corrected len " + qr.corrected.length);
//						String message = getUUIDFromBytes(qr.corrected);
						String message = getUUIDFromStr(qr.message);
						System.out.println("Bytes message: " + message);
					}

					// Visualize its location in the image
					VisualizeShapes.drawPolygon(qr.bounds,true,1,g2);
				}

				// List of objects it thinks might be a QR Code but failed for various reasons
				List<QrCode> failures = detector.getFailures();
				g2.setColor(Color.RED);
				for( QrCode qr : failures ) {
					// If the 'cause' is ERROR_CORRECTION or later then it's probably a real QR Code that
					if( qr.failureCause.ordinal() < QrCode.Failure.ERROR_CORRECTION.ordinal() )
						continue;

					VisualizeShapes.drawPolygon(qr.bounds,true,1,g2);
				}
			}

			repaint();
		}
	}

	public static boolean isAlphanumeric(String str) {
		for (int i=0; i<str.length(); i++) {
			char c = str.charAt(i);
			if (!Character.isDigit(c) && !Character.isLetter(c))
				return false;
		}
		return true;
	}

	public static String getUUIDFromBytes(byte[] bytes) {
		StringBuilder buffer = new StringBuilder();
		for(int i=0; i<bytes.length; i++) {
			buffer.append(String.format("%02x", bytes[i]));
		}
		return buffer.toString();
	}

	public static String getUUIDFromStr(String s) {
		try {
			return getUUIDFromBytes(s.getBytes(EciEncoding.ISO8859_1));
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	public static void main(String[] args) {
		ExampleDetectQrCode app = new ExampleDetectQrCode();
		app.run();
	}
}