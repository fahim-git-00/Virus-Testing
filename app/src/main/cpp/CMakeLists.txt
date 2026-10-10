cmake_minimum_required(VERSION 3.22.1)
project(securitylab LANGUAGES C CXX)

add_library(securitylab SHARED native.cpp native.c)

find_library(log-lib log)
find_package(OpenSSL REQUIRED)

target_link_libraries(securitylab ${log-lib} OpenSSL::Crypto)

add_library(rust_core STATIC IMPORTED)
set_target_properties(rust_core PROPERTIES IMPORTED_LOCATION
    ${CMAKE_SOURCE_DIR}/../../../rust-core/target/aarch64-linux-android/release/libsecurity_lab_core.a)
target_link_libraries(securitylab rust_core)
