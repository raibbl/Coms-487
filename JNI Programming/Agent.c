
#include <jni.h>  // JNI header provided by JDK
#include <stdio.h>  // C Standard IO Header
#include "Agent.h" // Generated
#include <string.h>
#include <time.h>
#include <sys/utsname.h>      

// Implementation of the native method sayHello()
JNIEXPORT void JNICALL Java_Agent_sayHello(JNIEnv *env, jobject obj)
{
      //printf("hii");
      return;
}
JNIEXPORT jstring JNICALL Java_Agent_GetLocalOs(JNIEnv *env, jobject obj, jstring os)
{
      jint i;
      jboolean iscopy;
      const char *theos;
      (*env)->MonitorEnter(env, obj); 

      theos = (*env)->GetStringUTFChars(env, os, 0);
     // (*env)->ReleaseStringUTFChars(env, os, theos);
      struct utsname operatingsystem;
      uname(&operatingsystem);

      strcpy(theos, operatingsystem.sysname);
      strcat(theos, "-");
      strcat(theos, operatingsystem.release);
      theos=(*env)->NewStringUTF(env, theos);
      (*env)->MonitorExit(env, obj); 

      return theos ;//
}
JNIEXPORT jstring JNICALL Java_Agent_GetLocalTime(JNIEnv *env, jobject obj, jstring atime)
{

      jint i;
 
      const char *thetime;
       (*env)->MonitorEnter(env, obj); 
      thetime = (*env)->GetStringUTFChars(env, atime, 0);
     // (*env)->ReleaseStringUTFChars(env, atime, thetime);

      time_t timedetails;
      struct tm *timeinfo;

      time(&timedetails);
      timeinfo = localtime(&timedetails);
      sprintf(thetime, " 0%d0%d0%d TRUE ", timeinfo->tm_hour, timeinfo->tm_min, timeinfo->tm_sec);
     thetime= (*env)->NewStringUTF(env, thetime);
      (*env)->MonitorExit(env, obj); 

     // printf(" %s\n", thetime);
      return thetime; 
}