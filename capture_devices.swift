#!/usr/bin/env xcrun swift

import AVFoundation

let devices2 = AVCaptureDevice.devices()
print("All available AV Capture devices:")
for d in devices2 {
    print(d)
}
