#include "com_example_uitest_RunActivity.h"

#include <android/bitmap.h>
#include <opencv2/opencv.hpp>
#include <android/asset_manager_jni.h>
#include <dlib/opencv/cv_image.h>
#include <dlib/data_io.h>
#include <dlib/opencv.h>
#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/model_utils.h>
#include <dlib/image_processing.h>
#include <vector>
using namespace cv;
using namespace std;

dlib::shape_predictor pose_model;
cv::CascadeClassifier face_detecter;

struct membuf : std::streambuf {
    membuf(char* begin, char* end) {
        this->setg(begin, begin, end);
    }
};

int transformCoord(int x, int max_x, int trans_max_x) {
    double scale = (float)x / max_x;
    int y = trans_max_x * scale;
    return y;
}

bool detectFace(const Mat image, cv::Rect& rect){
    Mat image_gray;
    cv::cvtColor(image, image_gray, cv::COLOR_BGR2GRAY);
    std::vector<cv::Rect> faces;
    face_detecter.detectMultiScale(image_gray, faces, 1.2, 2, 0, cv::Size(0, 0));
    if (faces.size()>0){
        rect = faces[0];
        return true;
    }
    else{
        return false;
    }
}

void getEyeRect(const dlib::full_object_detection landmarks, int start, int end, cv::Rect& rect, Mat oldImage, Mat newImage) {
    int left = -1, top = -1, right = 0, bottom = 0;
    for (unsigned long i = start; i < end; ++i) {
        int x = landmarks.part(i).x();
        int y = landmarks.part(i).y();
        if (left < 0 || left > x) {
            left = x;
        }
        if (top < 0 || top > y) {
            top = y;
        }
        if (right <= 0 || right < x) {
            right = x;
        }
        if (bottom < 0 || bottom < y) {
            bottom = y;
        }
    }
    rect.x = transformCoord(left, oldImage.cols, newImage.cols) - 1;
    rect.y = transformCoord(top, oldImage.rows, newImage.rows) - 1;
    rect.width = transformCoord(right-left, oldImage.cols, newImage.cols) + 2;
    rect.height = transformCoord(bottom-top, oldImage.rows, newImage.rows) + 2;
}

void getEye(Mat src, cv::Rect rect, int eh, int ew, Mat & dst){
    Mat Eye;
    Eye = src(rect);
    resize(Eye, Eye, Size(eh, ew));
    transpose(Eye, Eye);
    flip(Eye, Eye, 1);
    cvtColor(Eye, Eye, COLOR_GRAY2RGBA);
    cvtColor(Eye, dst, COLOR_RGBA2mRGBA);
}

void getShowEye(Mat src, cv::Rect left, cv::Rect right, int eh, int ew, Mat &dst){
    Mat leftEye, rightEye;
    Mat HSV;
    cvtColor(src, HSV, COLOR_BGR2HSV);
    std::vector<Mat> hsv;
    split(HSV, hsv);
    Mat v = hsv.at(2);  // 显示V通道
    leftEye = v(left);
    rightEye = v(right);
    resize(leftEye, leftEye, Size(eh, ew));
    resize(rightEye, rightEye, Size(eh, ew));
    Mat tmp = cv::Mat::zeros(Size(eh * 2, ew), CV_8UC1);
    Rect _tmpleft = Rect(0, 0, eh, ew);
    Mat tmproi = tmp(_tmpleft);
    leftEye.copyTo(tmproi);

    Rect _tmpright = Rect(eh, 0, eh, ew);
    tmproi = tmp(_tmpright);
    rightEye.copyTo(tmproi);
    transpose(tmp, tmp);
    flip(tmp, tmp, 1);
    cvtColor(tmp, tmp, COLOR_GRAY2RGBA);
    cvtColor(tmp, dst, COLOR_RGBA2mRGBA);
}

extern "C"
JNIEXPORT void JNICALL Java_com_example_uitest_RunActivity_loadFaceModel
        (JNIEnv *env, jobject obj, jstring file_path){
    const char* cascade_file_name = env->GetStringUTFChars(file_path, NULL);
    face_detecter = cv::CascadeClassifier(cascade_file_name);
}

extern "C"
JNIEXPORT void JNICALL Java_com_example_uitest_RunActivity_loadMedModel
        (JNIEnv *env, jobject obj, jstring file_path){
    const char* med_file_name = env->GetStringUTFChars(file_path, NULL);
    med::load_shape_predictor_model(pose_model, med_file_name);
}

extern "C" JNIEXPORT jint JNICALL
JNICALL Java_com_example_uitest_RunActivity_decode(
        JNIEnv *env, jobject obj,
        jbyteArray yuv, jobject bitmap,
        jint ch, jint cw, jint vh, jint vw,
        jint eh, jint ew,jobject outLeftEye, jobject outRightEye, jobject outEye) {
    jbyte * yuvBuf = (jbyte*)env->GetByteArrayElements(yuv, 0);
    // 输出的摄像图像
    void* facePixels = 0;
    CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &facePixels) >= 0);
    Mat faceBitmap(vh, vw, CV_8UC4, facePixels);

    // 左眼
    void* leftEyePixels = 0;
    CV_Assert(AndroidBitmap_lockPixels(env, outLeftEye, &leftEyePixels) >= 0);
    Mat leftEyeBitmap(eh, ew, CV_8UC4, leftEyePixels);

    // 右眼
    void* rightEyePixels = 0;
    CV_Assert(AndroidBitmap_lockPixels(env, outRightEye, &rightEyePixels) >= 0);
    Mat rightEyeBitmap(eh, ew, CV_8UC4, rightEyePixels);

    // 双眼
    void* eyePixels = 0;
    CV_Assert(AndroidBitmap_lockPixels(env, outEye, &eyePixels) >= 0);
    Mat eyeBitmap(eh * 2, ew, CV_8UC4, eyePixels);


    Mat image(ch + ch/2,cw,CV_8UC1,(unsigned char *)yuvBuf);	//注意这里是height+height/2
    Mat BGR, src;
    cvtColor(image, BGR, COLOR_YUV2BGRA_NV21);
    cvtColor(BGR, BGR, COLOR_BGRA2BGR);
    flip(BGR, BGR, 1); // CV中的翻转
    flip(BGR, BGR, 0);
    transpose(BGR, BGR); // 转置
    BGR.copyTo(src);  // src是原图
    resize(BGR, BGR, Size(vh, vw));  // 这里vh与vw分别对应输出 的bitmap的height与width属性

    Mat HSV;
    std::vector<Mat> hsv;
    cvtColor(src, HSV, COLOR_BGR2HSV);
    split(HSV, hsv);
    Mat V = hsv.at(2);

    cv::Rect faceRect;
    Mat _leftEyeMat;
    bool detected = detectFace(BGR, faceRect);
    if(detected){
        dlib::full_object_detection landmarks;
        dlib::array2d<dlib::rgb_pixel> dlib_image;
        dlib::assign_image(dlib_image, dlib::cv_image<dlib::bgr_pixel>(BGR));
        dlib::rectangle det(faceRect.x, faceRect.y,
                            faceRect.x + faceRect.width, faceRect.y + faceRect.height);
        landmarks = pose_model(dlib_image, det);

        for (int i = 0; i < 36; i++) {
            circle(BGR, cvPoint(landmarks.part(i).x(), landmarks.part(i).y()), 1, cv::Scalar(0, 0, 255), -1);
        }
        for (int i = 48; i < 68; i++) {
            circle(BGR, cvPoint(landmarks.part(i).x(), landmarks.part(i).y()), 1, cv::Scalar(0, 0, 255), -1);
        }

        cv::Rect _left, _right;
        getEyeRect(landmarks, 36, 42, _left, BGR, BGR);// 左眼区域
        getEyeRect(landmarks, 42, 48, _right, BGR, BGR);// 右眼区域

        getShowEye(BGR, _left, _right, eh, ew, eyeBitmap);

        cv::Rect leftRect;
        getEyeRect(landmarks, 36, 42, leftRect, BGR, src);// 左眼区域
        getEye(V, leftRect, eh, ew, leftEyeBitmap);

        cv::Rect rightRect;
        getEyeRect(landmarks, 42, 48, rightRect, BGR, src);// 右眼区域
        getEye(V, rightRect, eh, ew, rightEyeBitmap);
    }


    transpose(BGR, BGR);
    flip(BGR, BGR, 1);
    cvtColor(BGR, BGR, COLOR_BGR2RGBA);
    cvtColor(BGR, faceBitmap, COLOR_RGBA2mRGBA);

    AndroidBitmap_unlockPixels(env, outLeftEye);
    AndroidBitmap_unlockPixels(env, outRightEye);
    AndroidBitmap_unlockPixels(env, outEye);
    AndroidBitmap_unlockPixels(env, bitmap);
    env->ReleaseByteArrayElements(yuv, yuvBuf, 0);

    if (detected){
        return 1;
    }else{
        return 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_uitest_RunActivity_loadShapeModel(
        JNIEnv *env, jobject obj, jobject assetManager, jstring fileName) {

    const char *file_name = env->GetStringUTFChars(fileName, nullptr);
    env->ReleaseStringUTFChars(fileName, file_name);

    //get AAssetManager
    AAssetManager *native_asset = AAssetManager_fromJava(env, assetManager);

    //open file
    AAsset *assetFile = AAssetManager_open(native_asset, file_name, AASSET_MODE_BUFFER);
    //get file length
    size_t file_length = static_cast<size_t>(AAsset_getLength(assetFile));
    char *model_buffer = (char *) malloc(file_length);
    //read file data
    AAsset_read(assetFile, model_buffer, file_length);
    //the data has been copied to model_buffer, so , close it
    AAsset_close(assetFile);

    //LOGI("asset file length %d", file_length);

    //char* to istream
    membuf mem_buf(model_buffer, model_buffer + file_length);
    std::istream in(&mem_buf);

    //load shape_predictor_68_face_landmarks.dat from memory
    dlib::deserialize(pose_model,in);

    //free malloc
    free(model_buffer);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_uitest_RunActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
