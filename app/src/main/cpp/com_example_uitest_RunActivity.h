/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_example_uitest_RunActivity */

#ifndef _Included_com_example_uitest_RunActivity
#define _Included_com_example_uitest_RunActivity
#ifdef __cplusplus


extern "C" {
#endif
/*
 * Class:     com_example_uitest_RunActivity
 * Method:    stringFromJNI
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_example_uitest_RunActivity_stringFromJNI
  (JNIEnv *, jobject);

/*
 * Class:     com_example_uitest_RunActivity
 * Method:    decode
 * Signature: ([BLjava/lang/Object;IIIIIILjava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_com_example_uitest_RunActivity_decode
  (JNIEnv *, jobject, jbyteArray, jobject, jint, jint, jint, jint, jint, jint, jobject, jobject, jobject);

/*
 * Class:     com_example_uitest_RunActivity
 * Method:    loadFaceModel
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_example_uitest_RunActivity_loadFaceModel
  (JNIEnv *, jobject, jstring);

/*
 * Class:     com_example_uitest_RunActivity
 * Method:    loadMedModel
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_example_uitest_RunActivity_loadMedModel
  (JNIEnv *, jobject, jstring);

#ifdef __cplusplus
}
#endif
#endif
