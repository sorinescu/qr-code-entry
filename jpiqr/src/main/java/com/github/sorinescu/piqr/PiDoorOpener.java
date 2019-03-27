package com.github.sorinescu.piqr;

import java.io.Closeable;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import io.sentry.Sentry;

class PiDoorOpener implements Closeable {
    GpioController gpio;
    GpioPinDigitalMultipurpose doorLockPin;

    public PiDoorOpener() {
	    gpio = GpioFactory.getInstance();
	    doorLockPin = gpio.provisionDigitalMultipurposePin(
	    	RaspiPin.GPIO_02,
	    	PinMode.DIGITAL_INPUT,
	    	PinPullResistance.OFF);

        // set shutdown state for this pin
        doorLockPin.setShutdownOptions(true, PinState.HIGH, PinPullResistance.OFF, PinMode.DIGITAL_INPUT);

		System.out.println("Initialised GPIO");
	}

	public void close() {
		System.out.println("Shutting down GPIO");
        gpio.shutdown();
	}

	public void openDoor() throws InterruptedException {
		doorLockPin.setMode(PinMode.DIGITAL_OUTPUT);
		doorLockPin.low();
		try {
			Thread.sleep(200);
        } catch (Exception e) {
            Sentry.capture(e);
		} finally {
			doorLockPin.high();
			doorLockPin.setMode(PinMode.DIGITAL_INPUT);
		}
	}
}