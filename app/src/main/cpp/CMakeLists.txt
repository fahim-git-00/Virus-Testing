cmake_minimum_required(VERSION 3.22.1)
project(securitylab LANGUAGES C CXX)

add_library(securitylab SHARED native.cpp native.c)
