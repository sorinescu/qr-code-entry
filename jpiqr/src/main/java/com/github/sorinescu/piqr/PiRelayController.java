package com.github.sorinescu.piqr;

import java.io.Closeable;
import java.util.logging.Logger;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalMultipurpose;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinMode;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import io.sentry.Sentry;

class PiRelayController implements Closeable {
    private static final Logger logger = Logging.getLogger(PiRelayController.class.getName());

    GpioController gpio;
    GpioPinDigitalOutput doorLockPin;
    // GpioPinDigitalMultipurpose webcamUsbPin;
    GpioPinDigitalOutput webcamUsbPin;

    public PiRelayController(Config config) {
        gpio = GpioFactory.getInstance();

        doorLockPin = gpio.provisionDigitalOutputPin(
                          RaspiPin.getPinByAddress(config.doorOpenGpioPin),
                          PinState.LOW);

        // We're using a LOW LEVEL TRIGGERED relay which needs 5V to turn off.
        // Fortunately, the relay has a pull-up resistor, so just keep this pin in High-Z
        // and just switch it low when we need to reset the webcam.
        // webcamUsbPin = gpio.provisionDigitalMultipurposePin(
        //     RaspiPin.getPinByAddress(config.webcamPowerGpioPin),
        //     PinMode.DIGITAL_INPUT,
        //     PinPullResistance.OFF);
        webcamUsbPin = gpio.provisionDigitalOutputPin(
                           RaspiPin.getPinByAddress(config.webcamPowerGpioPin),
                           PinState.LOW);

        // logger.info("Using pin " + webcamUsbPin.toString() + " for webcam control");
        // while (true) {
        //     logger.info("ON");
        //     // webcamUsbPin.setMode(PinMode.DIGITAL_OUTPUT);
        //     // webcamUsbPin.low();
        //     webcamUsbPin.high();
        //     try { Thread.sleep(1000); } catch (InterruptedException e) {}
        //     logger.info("OFF");
        //     // webcamUsbPin.high();
        //     // webcamUsbPin.setMode(PinMode.DIGITAL_INPUT);
        //     webcamUsbPin.low();
        //     try { Thread.sleep(1000); } catch (InterruptedException e) {}
        // }

        // Allow webcam to initialise after being powered up
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        // Set shutdown state for these pins
        doorLockPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF, PinMode.DIGITAL_INPUT);
        webcamUsbPin.setShutdownOptions(true, PinState.LOW, PinPullResistance.OFF, PinMode.DIGITAL_INPUT);

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
            Thread.sleep(3000);     // poor man's debounce
        }
    }

    public void resetWebcam() throws InterruptedException {
        logger.info("Resetting webcam");

        webcamUsbPin.high();
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            Sentry.capture(e);
        } finally {
            webcamUsbPin.low();
        }
    }
}