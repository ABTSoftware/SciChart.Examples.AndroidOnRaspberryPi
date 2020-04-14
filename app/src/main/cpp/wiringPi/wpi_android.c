#include <jni.h>
#include <wiringPi.h>
#include "bme280.h"
#include "bme280-i2c.h"
#include "si1132.h"

#ifdef __cplusplus
extern "C" {
#endif

#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOG_TAG "wpi_android"

#define I2C_DEVICE  "/dev/i2c-1"

u32 pressure;
s32 temperature;
u32 humidity;

float SEALEVELPRESSURE_HPA = 1024.25;

jint Java_com_scichart_scichartonraspberry_fragments_FifoChartsFragment_bme280_1begin(JNIEnv* env, jobject obj) {
    return bme280_begin(I2C_DEVICE);
}

void Java_com_scichart_scichartonraspberry_fragments_FifoChartsFragment_bme280_end(JNIEnv* env, jobject obj) {
    bme280_end();
}

void Java_com_scichart_scichartonraspberry_fragments_FifoChartsFragment_readyData(JNIEnv* env, jobject obj) {
    bme280_read_pressure_temperature_humidity(&pressure, &temperature, &humidity);
}

jint Java_com_scichart_scichartonraspberry_fragments_FifoChartsFragment_getTemperature(JNIEnv* env, jobject obj) {
    return temperature;
}

jint Java_com_scichart_scichartonraspberry_fragments_FifoChartsFragment_getPressure(JNIEnv* env, jobject obj) {
    return pressure;
}

jint Java_com_scichart_scichartonraspberry_fragments_FifoChartsFragment_getHumidity(JNIEnv* env, jobject obj) {
    return humidity;
}

jint Java_com_scichart_scichartonraspberry_fragments_FifoChartsFragment_getAltitude(JNIEnv* env, jobject obj) {
    int result = 0;
    bme280_readAltitude(pressure, &SEALEVELPRESSURE_HPA, &result);
    return result;
}

#ifdef __cplusplus
}
#endif
