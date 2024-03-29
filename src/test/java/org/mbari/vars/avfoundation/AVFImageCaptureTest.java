package org.mbari.vars.avfoundation;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * @author Brian Schlining
 * @since 2018-01-16T13:44:00
 */
public class AVFImageCaptureTest {

    // @Ignore
    @Test
    public void test01() throws IOException {
        AVFImageCapture ic = new AVFImageCapture();
        String[] devices = ic.videoDevicesAsStrings();
        ic.startSessionWithNamedDevice(devices[0]);
        Optional<Image> png = ic.capture(new File("target", getClass().getSimpleName() + ".png"));
        ic.stopSession();
        Assert.assertNotNull(png.get());
    }
}
