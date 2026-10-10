cmake_minimum_required(VERSION 3.22.1)
project(securitylab LANGUAGES C CXX)

add_library(securitylab SHARED native.cpp native.c)

find_library(log-lib log)
target_link_libraries(securitylab ${log-lib})

set(RUST_LIB_DIR ${CMAKE_SOURCE_DIR}/../../../../rust-core/target)
set(RUST_LIB_NAME libsecurity_lab_core.a)

add_library(rust_core STATIC IMPORTED)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    set_target_properties(rust_core PROPERTIES
        IMPORTED_LOCATION ${RUST_LIB_DIR}/aarch64-linux-android/release/${RUST_LIB_NAME})
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set_target_properties(rust_core PROPERTIES
        IMPORTED_LOCATION ${RUST_LIB_DIR}/armv7-linux-androideabi/release/${RUST_LIB_NAME})
elseif(ANDROID_ABI STREQUAL "x86_64")
    set_target_properties(rust_core PROPERTIES
        IMPORTED_LOCATION ${RUST_LIB_DIR}/x86_64-linux-android/release/${RUST_LIB_NAME})
endif()

target_link_libraries(securitylab rust_core)
