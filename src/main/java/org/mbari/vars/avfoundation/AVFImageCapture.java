package org.mbari.vars.avfoundation;

import org.scijava.nativelib.NativeLibraryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

/**
 * AVFImageCapture
 *
 */
public class AVFImageCapture {

    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String LIBRARY_NAME = "avfimagesnap";


    public AVFImageCapture() {
        try {
            System.loadLibrary(LIBRARY_NAME);
            log.info(LIBRARY_NAME + " was found on the java.library.path and loaded");
        }
        catch (UnsatisfiedLinkError e) {
            extractAndLoadNativeLibraries();
        }
        // HACK: The 1st call to videoDevicesAsStrings returns empty. 2nd call is OK
        videoDevicesAsStrings();
    }

    ////////////////////////
    // Start of Natives   //
    ////////////////////////
    /**
     * This is a JNI method that returns an array of strings that are the names of
     * the various video devices available on the underlying platform
     *
     * @return An array of <code>Strings</code> that echos back the names of
     * video devices available.
     */
    public native String[] videoDevicesAsStrings();
    
    /**
     * This is a JNI method that provides a device Name to begin a Capture Session
     * with on the underlying platform.
     *
     * @param deviceName is a String that specifies the device that is to be
     * used for the capture session
     *
     * @return A <code>String</code> that echos back the device name that is
     * to be used for the session.
     */
    public native String startSessionWithNamedDevice(String deviceName);


    /**
     * This is a JNI method that provides a Path to save an image to as a file.
     * Available on the underlying platform.
     *
     * @param specifiedPath is a String that specifies the path/filename to save
     * the captured image to.
     *
     * @return A <code>String</code> that echos back the path/filename that the
     * image is to be saved as.
     */
    public native String saveSnapshotToSpecifiedPath(String specifiedPath);


    /**
     * This is a JNI method that stops the Capture Session currently running.
     * Available on the underlying platform.
     *
     * @return
     */
    public native void stopSession();

    /////////////////////
    // End of Natives  //
    /////////////////////


    public Optional<Image> capture(File file) {
        return capture(file, Duration.ofSeconds(3));
    }

    public Optional<Image> capture(File file, Duration timeout) {

        log.debug("Capturing image to " + file.getAbsolutePath());

        saveSnapshotToSpecifiedPath(file.getAbsolutePath());

        // -- Read file as image
        BufferedImage image = null;
        try {
            image = watchForAndReadNewImage(file, timeout);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read image off of disk. It may not have been written.", e);

        }

        return Optional.ofNullable(image);
    }

    private void extractAndLoadNativeLibraries() {
        NativeLibraryUtil.loadNativeLibrary(this.getClass(), LIBRARY_NAME);
    }

    /**
     * AVFoundation writes the image asynchronously. We need to block and watch for them to be created.
     * Lame, but even using Java Future's forces us to block.
     * @param file
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private BufferedImage watchForAndReadNewImage(File file, Duration timeout)
            throws IOException, InterruptedException {
        BufferedImage image = null;
        long timeoutNanos = timeout.toNanos();
        long elapsedNanos = 0L;
        long startNanos = System.nanoTime();
        while (elapsedNanos < timeoutNanos) {
            if (file.exists()) {
                break;
            }
            Thread.sleep(50L);
            elapsedNanos = System.nanoTime() - startNanos;
        }
        if (file.exists()) {
            image = ImageIO.read(file);
        }
        return image;
    }

    public static void main(String[] args) {
        AVFImageCapture imageCaptureService = new AVFImageCapture();

        if (args.length == 0 || args.length > 2) {
            System.out.println("Usage: " + AVFImageCapture.class.getName() + " <file> <device>");
            System.out.println("\nArguments:\n");
            System.out.println("\tfile: Where to save the image to.");
            System.out.println("\tdevice: The name of the image capture device [Optional] default = FaceTime HD Camera");
            String[] devices = imageCaptureService.videoDevicesAsStrings();
            for (String d : devices) {
                System.out.printf("\t\t" + d);
            }
            return;
        }

        File file = new File(args[0]);
        String device = args.length == 1 ? "FaceTime HD Camera" : args[1];

        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }


        //imageCaptureService.startSession(device);
        imageCaptureService.startSessionWithNamedDevice(device);
        Optional<Image> opt = imageCaptureService.capture(file);
        if (opt.isPresent()) {
            System.out.println("Captured image from " + device + " to " +
                    file.getAbsolutePath());
        }
        else {
            System.out.println("Failed to write " + file.getAbsolutePath());
        }
        imageCaptureService.stopSession();
    }
}