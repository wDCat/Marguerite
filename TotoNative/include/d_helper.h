//
// Created by whmwi on 2016/9/15.
//
#include <android/log.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <dlfcn.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <sys/select.h>
#include <sys/syscall.h>
#include <string.h>
#include <termios.h>
#include <pthread.h>
#include <sys/epoll.h>
#include <unistd.h>
#include <jni.h>
#include <stdlib.h>


#ifndef ANDROID_INLINE_HOOK_D_HELPER_H
#define ANDROID_INLINE_HOOK_D_HELPER_H
#define BUFF_SIZE 512
#define LOG_TAG "DINJ"
#define true 1
#define false 0
#ifdef LOG_ON
#define LOGD(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define dlog(fmt, args...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, fmt, ##args)
#define derr(fmt, args...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##args)
#else
#ifdef LOG_ON_TO_FILE
#define LOGD(...) \
        {char* p=malloc(sizeof(char)*1024);\
        sprintf(p,PATH_ROOT"/log/%d.session",getpid());\
        FILE *fp = fopen(p, "a+"); if (fp) {\
        fprintf(fp, __VA_ARGS__);\
        fclose(fp);free(p);}}
#define dlog(...){char* p=malloc(sizeof(char)*1024);\
        sprintf(p,PATH_ROOT"/log/%d.session",getpid());\
        FILE *fp = fopen(p, "a+"); if (fp) {\
        fprintf(fp, __VA_ARGS__);\
        fclose(fp);free(p);}}
#define derr(...){char* p=malloc(sizeof(char)*1024);\
        sprintf(p,PATH_ROOT"/log/%d.session",getpid());\
        FILE *fp = fopen(p, "a+"); if (fp) {\
        fprintf(fp, __VA_ARGS__);\
        fclose(fp);free(p);}}
#else
#define LOGD(fmt, args...)
#define dlog(fmt, args...)
#define derr(fmt, args...)
#endif
#endif

typedef int func;

char *getpid_str();

int clear_str(char *str, int length);

char *get_pkg(int pid);

char *get_filename(int fd);

char *stringReplace(const char *strbuf, const char *sstr, const char *dstr);

void stringToLower(char *s1);

int stringEndWith(const char *s1, const char *s2);

int stringStartWith(const char *s1, const char *s2);

int stringContains(const char *s1, const char *s2);

int string_replace_char(char *str, char src, char target);

char *read_all_from_file(const char *fn);

void *get_module_base(pid_t pid, const char *module_name);

#endif //ANDROID_INLINE_HOOK_D_HELPER_H
