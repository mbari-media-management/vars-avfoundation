#!/usr/bin/env xcrun swift

import AVFoundation

let devices = AVCaptureDevice.devices()
print("All available AV Capture devices:")
for d in devices {
    print(d)
}
