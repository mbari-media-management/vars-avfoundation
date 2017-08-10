![MBARI logo](src/site/resources/images/logo-mbari-3b.png)

# vars-avfoundation

Java interface to native Mac OS image capture using [AVFoundation](https://developer.apple.com/av-foundation/). Works with hardware capture cards (as well as built-in cameras).

## Build

mvn package

## Usage

```java
import org.mbari.vars.avfoundation.AVFImageCapture;

// Native libraries are extracted under the hood and automatimaclly
// Added to java.library.path
AVFImageCapture ic = new AVFImageCapture();

// List all available devices
String[] devices = ic.videoDevicesAsStrings();

// Pick one to capture from
ic.startSession(devices[0]);

// Do some capturing, capture as many images as you want
Optional<java.awt.Image> image = ic.capture(new File("foo.png"));

// When done. release the device.
ic.stopSession()

```