package com.github.sorinescu.piqr;

import java.io.Closeable;
import java.util.logging.Logger;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import io.sentry.Sentry;

class PiDoorOpener implements Closeable {
    private static final Logger logger = Logging.getLogger(PiDoorOpener.class.getName());

    GpioController gpio;
    GpioPinDigitalOutput doorLockPin;

    public PiDoorOpener() {
	    gpio = GpioFactory.getInstance();
	    doorLockPin = gpio.provisionDigitalOutputPin(
	    	RaspiPin.GPIO_02,
	    	PinState.LOW);

        // set shutdown state for this pin
        doorLockPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF, PinMode.DIGITAL_INPUT);

		logger.info("Initialised GPIO");
	}

	public void close() {
		logger.info("Shutting down GPIO");
        gpio.shutdown();
	}

	public void openDoor() throws InterruptedException {
        logger.info("Opening door");

		doorLockPin.high();
		try {
			Thread.sleep(200);
        } catch (Exception e) {
            Sentry.capture(e);
		} finally {
			doorLockPin.low();
		}
	}
}