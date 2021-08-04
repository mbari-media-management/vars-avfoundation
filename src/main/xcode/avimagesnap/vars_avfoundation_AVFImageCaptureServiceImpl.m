#include "vars_avfoundation_AVFImageCaptureServiceImpl.h"
#include "AVFStillImageCapture.h"
//#import <JavaNativeFoundation/JavaNativeFoundation.h>


AVFStillImageCapture *imageCapture = nil;

void initImageCapture() {
    if (imageCapture == nil) {
        imageCapture = [[AVFStillImageCapture alloc] init];
        [imageCapture initSession];
    }
}


/*
 * ConvertToNSString
 *
 * given a non-null jstring argument, return the equivalent NSString representation. The object is autoreleased.
 *
 * This function returns NULL if the argument is NULL, or if the NSString couldn't be created. Requires the JNIEnv
 * to be passed as the first argument
 *
 */
NSString *ToNSString(JNIEnv *env, jstring str)
{
    if (str == NULL)
    {
        return NULL;
    }

    const jchar *chars = (*env)->GetStringChars(env, str, NULL);
    NSString *myNSString =
        [NSString stringWithCharacters:(UniChar *)chars  length:(*env)->GetStringLength(env, str)];
    (*env)->ReleaseStringChars(env, str, chars);

    return myNSString;
}

/*
 * CreateJavaStringFromNSString
 *
 * given a non-null NSString argument, return the equivalent Java String representation.
 *
 * This function returns NULL if the argument is NULL, or if the jstring couldn't be created. Requires the JNIEnv
 * to be passed as the first argument
 *
 */
jstring ToJavaString(JNIEnv *env, NSString *nativeStr)
{
    if (nativeStr == NULL)
    {
        return NULL;
    }
    // Note that length returns the number of UTF-16 characters,
    // which is not necessarily the number of printed/composed characters
    jsize buflength = [nativeStr length];
    unichar buffer[buflength];
    [nativeStr getCharacters:buffer];
    jstring javaStr = (*env)->NewString(env, (jchar *)buffer, buflength);
    return javaStr;
}

/*
 * Class:     vars_avfoundation_AVFImageCaptureServiceImpl
 * Method:    videoDevicesAsStrings
 * Signature: ()[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_org_mbari_vars_avfoundation_AVFImageCapture_videoDevicesAsStrings
(JNIEnv *env, jobject clazz) {
    
    initImageCapture();
	
	// Grab the array of NSStrings
	NSArray *videoDevicesAsStrings = [imageCapture videoCaptureDevicesAsStrings];
	
	// Look for the class for Java String
	jclass stringClass = (*env)->FindClass(env, "Ljava/lang/String;");
	
	// The array to return
	jobjectArray result = (*env)->NewObjectArray(env, [videoDevicesAsStrings count], stringClass, NULL);
	
	// Loop over devices
	for (int i = 0; i < [videoDevicesAsStrings count]; i++) {
		// Set the object in the java array to the device name converted to java string
		(*env)->SetObjectArrayElement(env, result, i, ToJavaString(env, [videoDevicesAsStrings objectAtIndex:i]));
	}
	
	// Return the result
	return result;

};

/*
 * Class:     vars_avfoundation_AVFImageCaptureServiceImpl
 * Method:    startSessionWithNamedDevice
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_mbari_vars_avfoundation_AVFImageCapture_startSessionWithNamedDevice
(JNIEnv *env, jobject clazz, jstring namedDevice) {
	
	// Convert the incoming Device Name to an NSString
	NSString *device = ToNSString(env, namedDevice);
    
    initImageCapture();

    [imageCapture setupCaptureSessionUsingNamedDevice: device];
	
	// Convert the filename back to jstring for return
	return ToJavaString(env, device);
	
};

/*
 * Class:     vars_avfoundation_AVFImageCaptureServiceImpl
 * Method:    saveSnapshotToSpecifiedPath
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_mbari_vars_avfoundation_AVFImageCapture_saveSnapshotToSpecifiedPath
(JNIEnv *env, jobject clazz, jstring specifiedPath) {
	
	// Convert the incoming filename to an NSString
	NSString *path = ToNSString(env, specifiedPath);
	
    if (imageCapture != nil) {
        [imageCapture saveStillImageToPath:path];
    }
    else {
        NSLog(@"Still image capture has not been configured. Unable to save %@", path);
    }
	
	// Convert the filename back to jstring for return
	return ToJavaString(env, path);

};

/*
 * Class:     vars_avfoundation_AVFImageCaptureServiceImpl
 * Method:    stopSession
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_org_mbari_vars_avfoundation_AVFImageCapture_stopSession
(JNIEnv *env, jobject clazz) {
	
	// [imageCapture dealloc]; // Don't need. This project is using ARC
    imageCapture = nil; // On deallocation the session will be terminated
};


