cmake_minimum_required(VERSION 3.22.1)
project(rexaps LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

set(NODE_LIBRARY "${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libnode.so")
set(CARES_LIBRARY "${CMAKE_CURRENT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/libcares.so")
set(NODE_HEADERS "${CMAKE_CURRENT_SOURCE_DIR}/libnode/include/node")

if(NOT EXISTS "${NODE_LIBRARY}")
    message(FATAL_ERROR "Missing Node.js shared library for ${ANDROID_ABI}: ${NODE_LIBRARY}")
endif()
if(NOT EXISTS "${CARES_LIBRARY}")
    message(FATAL_ERROR "Missing Android c-ares shared library for ${ANDROID_ABI}: ${CARES_LIBRARY}")
endif()
if(NOT EXISTS "${NODE_HEADERS}/node.h")
    message(FATAL_ERROR "Missing Node.js ${NODE_HEADERS}/node.h headers")
endif()

add_library(node SHARED IMPORTED GLOBAL)
set_target_properties(node PROPERTIES IMPORTED_LOCATION "${NODE_LIBRARY}")

add_library(cares SHARED IMPORTED GLOBAL)
set_target_properties(cares PROPERTIES IMPORTED_LOCATION "${CARES_LIBRARY}")

add_library(rexnode SHARED src/main/cpp/rexnode.cpp)
target_include_directories(rexnode PRIVATE "${NODE_HEADERS}")
target_compile_features(rexnode PRIVATE cxx_std_20)
target_compile_options(rexnode PRIVATE -fvisibility=hidden -Wall -Wextra)
target_link_libraries(rexnode PRIVATE node cares log android)
