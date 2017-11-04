//
// Created by whmwi on 2016/9/15.
//
#include <stdlib.h>
#include <stdio.h>
#include <zconf.h>
#include <string.h>
#include <ctype.h>
#include <fcntl.h>
#include "../include/d_helper.h"

#define NEED_FREE_RET
static int zygote_pid = 0;
static int is_target_pid = 0;

char *getpid_str() {
    char *pid_str = malloc(sizeof(char) * 20);
    sprintf(pid_str, "%d", getpid());
    return pid_str;
}

int clear_str(char *str, int length) {
    int x = 0;
    for (; x < length; x++) {
        str[x] = '\0';
    }
}

char *get_pkg(int pid) {
    char *buff = malloc(sizeof(char) * BUFF_SIZE);
    sprintf(buff, "/proc/%d/cmdline", pid);
    char *result = read_all_from_file(buff);
    free(buff);
    return result;
}

char *get_filename(int fd) {
    char *path = malloc(sizeof(char) * BUFF_SIZE);
    char *filename = malloc(sizeof(char) * BUFF_SIZE);
    clear_str(path, BUFF_SIZE);
    clear_str(filename, BUFF_SIZE);
    sprintf(path, "/proc/%d/fd/%d", getpid(), fd);
    readlink(path, filename, BUFF_SIZE);
    free(path);
    return filename;
}

#include "include/inlineHook.h"


char *stringReplace(const char *strbuf, const char *sstr, const char *dstr) {
    char *p, *p1;
    size_t len;
    if ((strbuf == NULL) || (sstr == NULL) || (dstr == NULL))
        return "";
    p = strstr(strbuf, sstr);
    if (p == NULL)  /*not found*/
        return (char *) strbuf;
    len = strlen(strbuf) + strlen(dstr) - strlen(sstr);
    p1 = malloc(len);
    bzero(p1, len);
    strncpy(p1, strbuf, p - strbuf);
    strcat(p1, dstr);
    p += strlen(sstr);
    strcat(p1, p);
    return p1;
}

void stringToLower(char *s1) {
    int x = 0;
    for (; x < strlen(s1); x++) {
        s1[x] = tolower(s1[x]);
    }
}

int stringEndWith(const char *s1, const char *s2) {
    return strstr(s1, s2) - s1 == strlen(s1) - strlen(s2);
}

int stringStartWith(const char *s1, const char *s2) {
    return strstr(s1, s2) - s1 == 0;
}

int stringContains(const char *s1, const char *s2) {
    return !(strstr(s1, s2) == NULL);
}

size_t old_read(int fp, char *buff, size_t length) {
    return read(fp, buff, length);
}

int string_replace_char(char *str, char src, char target) {
    int x = 0;
    int count = 0;
    while (true) {
        if (str[x] == '\0')break;
        if (str[x] == src) {
            str[x] = target;
            count++;
        }
        x++;
    }
    return count;
}

char *read_all_from_file(const char *fn) {
    char *buff;
    FILE *f;
    long len;
    dlog("read target:%s", fn);
    f = fopen(fn, "rb");
    if (f == NULL) {
        derr("fail to open file:%s", fn);
        return "";
    }
    fseek(f, 0, SEEK_END);
    len = ftell(f);
    fseek(f, 0, SEEK_SET);
    buff = (char *) malloc(len + 1);
    fread(buff, 1, len, f);
    buff[len] = '\0';
    dlog("data:%s", buff);
    fclose(f);
    return buff;
}

char *get_pkg_name() {
    char *clpath = malloc(sizeof(char) * BUFF_SIZE);
    sprintf(clpath, "/proc/%d/cmdline", getpid());
    int fd = open(clpath, O_RDONLY);
    if (fd == -1) {
        derr("fail to open cmdline.");
        return "";
    }
    read(fd, clpath, BUFF_SIZE);
    close(fd);
    dlog("pkg:%s", clpath);
    return clpath;
}

char *get_pkg_name_with_fopen() {
    char *clpath = malloc(sizeof(char) * BUFF_SIZE);
    sprintf(clpath, "/proc/%d/cmdline", getpid());
    FILE *cl = fopen(clpath, "r");
    if (cl == NULL) {
        derr("fail to get pkg name.");
        return "";
    }
    fread(clpath, BUFF_SIZE, BUFF_SIZE, cl);
    dlog("pkg:%s", clpath);
    fclose(cl);
    return clpath;
}

char *read_all_byte_from_file(const char *fn, int *size) {
    char *buff;
    FILE *f;
    long len;
    dlog("read target:%s", fn);
    f = fopen(fn, "rb");
    if (f == NULL) {
        derr("fail to open file:%s", fn);
        *size = 0;
        return "";
    }
    fseek(f, 0, SEEK_END);
    len = ftell(f);
    *size = len;
    fseek(f, 0, SEEK_SET);
    buff = (char *) malloc(len + 1);
    fread(buff, 1, len, f);
    fclose(f);
    return buff;
}

/**
 * like java
 * String.lastIndexOf()
 * */
int stringLastIndexOf(const char *line, char c) {
    for (int x = strlen(line) - 1; x >= 0; x--) {
        if (line[x] == c)return x;
    }
}

void *get_module_base(pid_t pid, const char *module_name) {
    FILE *fp;
    long addr = 0;
    char *pch;
    char filename[32];
    char line[1024];

    if (pid < 0) {
        /* self process */
        snprintf(filename, sizeof(filename), "/proc/self/maps");
    } else {
        snprintf(filename, sizeof(filename), "/proc/%d/maps", pid);
    }

    fp = fopen(filename, "r");

    if (fp != NULL) {
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, module_name)) {
                pch = strtok(line, "-");
                addr = strtoul(pch, NULL, 16);

                if (addr == 0x8000)
                    addr = 0;

                break;
            }
        }

        fclose(fp);
    }

    return (void *) addr;
}
