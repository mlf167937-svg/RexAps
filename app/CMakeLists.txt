cmake_minimum_required(VERSION 3.22.1)
project(rexaps LANGUAGES C CXX)

# Node.js 26.13 / V8 headers require C++20.
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(REXCHAT_LIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}")
set(NODE_HEADERS "${CMAKE_CURRENT_SOURCE_DIR}/libnode/include/node")

foreach(REQUIRED_FILE
        "${REXCHAT_LIB_DIR}/libnode.so"
        "${REXCHAT_LIB_DIR}/libcares.so"
        "${REXCHAT_LIB_DIR}/libsqlite3.so"
        "${NODE_HEADERS}/node.h")
    if(NOT EXISTS "${REQUIRED_FILE}")
        message(FATAL_ERROR "Missing RexChat native runtime input: ${REQUIRED_FILE}")
    endif()
endforeach()

add_library(node SHARED IMPORTED GLOBAL)
set_target_properties(node PROPERTIES IMPORTED_LOCATION "${REXCHAT_LIB_DIR}/libnode.so")

add_library(cares SHARED IMPORTED GLOBAL)
set_target_properties(cares PROPERTIES IMPORTED_LOCATION "${REXCHAT_LIB_DIR}/libcares.so")

add_library(sqlite3 SHARED IMPORTED GLOBAL)
set_target_properties(sqlite3 PROPERTIES IMPORTED_LOCATION "${REXCHAT_LIB_DIR}/libsqlite3.so")

add_library(rexnode SHARED src/main/cpp/rexnode.cpp)
target_include_directories(rexnode PRIVATE "${NODE_HEADERS}")
target_compile_features(rexnode PRIVATE cxx_std_20)
target_compile_options(rexnode PRIVATE -fvisibility=hidden -Wall -Wextra)
target_link_options(rexnode PRIVATE -Wl,--no-undefined)
target_link_libraries(rexnode PRIVATE node cares sqlite3 log android dl)
