#include <jni.h>
#include <node.h>

#include <string>
#include <vector>

extern "C" JNIEXPORT jint JNICALL
Java_com_rexaps_rexchat_v1_RexNodeBridge_startNodeWithArguments(
        JNIEnv* env,
        jobject /* instance */,
        jobjectArray arguments) {
    if (arguments == nullptr) {
        return static_cast<jint>(-1);
    }

    const jsize count = env->GetArrayLength(arguments);
    if (count < 2) {
        return static_cast<jint>(-2);
    }

    std::vector<std::string> storage;
    storage.reserve(static_cast<size_t>(count));

    for (jsize i = 0; i < count; ++i) {
        auto value = static_cast<jstring>(env->GetObjectArrayElement(arguments, i));
        if (value == nullptr) {
            storage.emplace_back();
            continue;
        }

        const char* chars = env->GetStringUTFChars(value, nullptr);
        if (chars == nullptr) {
            env->DeleteLocalRef(value);
            return static_cast<jint>(-3);
        }

        storage.emplace_back(chars);
        env->ReleaseStringUTFChars(value, chars);
        env->DeleteLocalRef(value);
    }

    // Keep every argument buffer alive and NUL-terminated for libuv/Node.js.
    std::vector<char*> argv;
    argv.reserve(storage.size());
    for (std::string& value : storage) {
        argv.push_back(value.data());
    }

    // Node's embedded runtime can only be started once per Android process.
    return static_cast<jint>(node::Start(static_cast<int>(argv.size()), argv.data()));
}
