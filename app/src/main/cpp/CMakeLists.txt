cmake_minimum_required(VERSION 3.4.1)
# #####OpenCV 环境
include(dlib/cmake)
#设置OpenCV-android-sdk路径
set( OpenCV_DIR E:\\OpenCV-android-sdk-4.4.0\\sdk\\native\\jni )
# 查找opencv库
find_package(OpenCV REQUIRED )
if(OpenCV_FOUND)
    include_directories(${OpenCV_INCLUDE_DIRS})
    message(STATUS "OpenCV library status:")
    message(STATUS "    version: ${OpenCV_VERSION}")
    message(STATUS "    libraries: ${OpenCV_LIBS}")
    message(STATUS "    include path: ${OpenCV_INCLUDE_DIRS}")
else(OpenCV_FOUND)
    message(FATAL_ERROR "OpenCV library not found")
endif(OpenCV_FOUND)
# ###################### 项目原生模块 ###################

add_library( native-lib
        SHARED
        native-lib.cpp)

# 增加opencv链接库 ${OpenCV_LIBS}
target_link_libraries( native-lib
        ${OpenCV_LIBS}
        dlib
        log
        jnigraphics)

add_library( run
        SHARED
        run.cpp)

# 增加opencv链接库 ${OpenCV_LIBS}
target_link_libraries( run
        ${OpenCV_LIBS}
        dlib
        log
        jnigraphics)