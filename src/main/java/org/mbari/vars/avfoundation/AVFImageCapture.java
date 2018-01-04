package org.mbari.vars.avfoundation;

import org.scijava.nativelib.NativeLibraryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * AVFImageCapture
 *
 */
public class AVFImageCapture {

    private final Logger log = LoggerFactory.getLogger(getClass());
    public static final String LIBRARY_NAME = "avfimagesnap";
    private volatile boolean isStarted = false;
    
    /** The device to use for image capture */
    private String device;

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
        if (!isStarted) {
            startDevice();
        }

        saveSnapshotToSpecifiedPath(file.getAbsolutePath());

        // -- Read file as image
        BufferedImage image = null;
        try {
            image = watchForAndReadNewImage(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read image off of disk. It may not have been written.", e);

        }

        return Optional.ofNullable(image);
    }


    public void startSession(String device) {
        if (device.equals(this.device)) {
            return;
        }

        if (isStarted) {
            stopDevice();
        }
        this.device = device;
    }

    public String getDevice() {
        return device;
    }


    private void startDevice() {
        if (isStarted) {
            log.info("The video device '" + device + "' is already opened");
        }
        else if (device != null && !device.isEmpty()) {
            log.debug("Starting image capture service, {}, using {}", getClass().getName(), device);
            startSessionWithNamedDevice(device);
            isStarted = true;
        }
        else {
            log.warn("A video device to capture from has not been selected");
        }
    }

    private void stopDevice() {
        log.debug("Stopping image capture service, {}", getClass().getName());
        stopSession();
        isStarted = false;
    }

    private void extractAndLoadNativeLibraries() {
        NativeLibraryUtil.loadNativeLibrary(this.getClass(), LIBRARY_NAME);

//        try {
//            //ativeLoader.loadLibrary(LIBRARY_NAME);
//        }
//        catch (IOException e) {
//            log.error("Failed to load " + LIBRARY_NAME + " from a jar file", e);
//        }

//        String libraryName = System.mapLibraryName(LIBRARY_NAME);
//        String os = System.getProperty("os.name");
//
//        if (libraryName != null) {
//
//            File tempDir = new File(System.getProperty("java.io.tmpdir"));
//            // This finds the native library, extracts it and hacks the java.library.path if needed
//            try {
//                NativeLoader.loadLibrary(LIBRARY_NAME);
//            }
//            catch (IOException e) {
//                log.error("Failed to load " + LIBRARY_NAME + " from a jar file", e);
//            }
//
//
//        }
//        else {
//            log.error( "A native '" + LIBRARY_NAME + "' library for your platform is not available. " +
//                    "You will not be able to use AVFoundation to capture images");
//        }

    }

    /**
     * AVFoundation writes the image asynchronously. We need to block and watch for them to be created.
     * Lame, but even using Java Future's forces us to block.
     * @param file
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    private BufferedImage watchForAndReadNewImage(File file) throws IOException, InterruptedException {
        BufferedImage image = null;
        long timeoutNanos = 3000000000L; // 3 seconds
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


        imageCaptureService.startSessionWithNamedDevice(device);
        Optional<Image> opt = imageCaptureService.capture(file);
        if (opt.isPresent()) {
            System.out.println("Captured image from  " + device + " to " +
                    file.getAbsolutePath());
        }
        else {
            System.out.println("Failed to write " + file.getAbsolutePath());
        }
        imageCaptureService.stopSession();
    }
}