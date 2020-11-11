# Hands-free 2048
The purpose of the project is to develop a mobile app that enables hands-free interaction. <br>
Hands-free 2048 combines two hands-free interactions, sensor-based interaction and gaze-based interaction with game 2048.<br>
People can play 2048 by tilting their phone and eye-tracking. For example, tilt the phone to the left to move the numbered tiles to left.<br>
Move the numbered tiles to the right by looking to the right.

## Requires
The eye-tracking mode depends on two open source libraries:
* OpenCV (OpenCV-4.4.0)    https://opencv.org/
* Dlib (Dlib 19.21)    http://dlib.net/
* Compress Dlib  https://github.com/miaoerduo/dlib-face-landmark-compression  

## Workflow
The application includes two hands-free interaction - tilt mode and eye-tracking mode.
* Tilt mode
>The tilt mode derives data from the accelerometer and magnetometer, computes the orientation angles of the phone in XYZ axes<br>
>When the rotation angle of the mobile phone in a certain axis reaches the set threshold, the corresponding operation is triggered.
* Eye-tracking mode
>The eye tracking mode uses haar cascade classifier in OpenCV to detect human face, uses Dlib to align landmarks and extract eye images.<br>
>Camera captures real-time eye pictures, compare it with templates to judge which direction the user is looking

## Usage
1. First clone the repository.<br>
2. Before importing the project into Android Studio, you need to:<br>
   * Open the `app/src/main/cpp/CMakeLists.txt` 
   * Then, replace with your path the variable **"OpenCV-android-sdk"**
3. Now the project is ready to be run on your Android Studio.