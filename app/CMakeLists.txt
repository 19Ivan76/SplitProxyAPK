cmake_minimum_required(VERSION 3.22.1)
project(splitproxy_apk)

add_library(splitproxy_jni SHARED src/main/cpp/native_bridge.cpp)
find_library(log-lib log)
find_library(dl-lib dl)
target_link_libraries(splitproxy_jni ${log-lib} ${dl-lib})
