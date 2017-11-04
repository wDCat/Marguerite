//
// Created by whmwi on 2017/10/20.
//

#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include "include/inlineHook.h"
#include "include/d_helper.h"
#include "include/cJSON.h"

int main(int argc, char **argv) {
    printf("A Little project..");
    exit(1);
}

typedef struct {
    uint32_t unknown1;
    uint32_t unknown2;
    uint32_t length;
    char *str;//string [h][0][t][0][t][0][p][0].....
} il2cpp_string_t;

cJSON *toto_patch_json = NULL;
uint32_t ilbase = 0;
uint32_t offset_PieceDamaged = 0x2333;
uint32_t offset_PieceEnduceM = 0x2333;
uint32_t offset_SettingGetEndpoint = 0x2333;
uint32_t offset_SettingGetBaseURL = 0x2333;
uint32_t atk_mul = 20;
char *new_baseurl = "http://localhost:3000";
il2cpp_string_t *istr_baseurl = 0;
bool hooked = false;

il2cpp_string_t *il2cpp_string_ctor(const char *str) {
    int len = strlen(str);
    il2cpp_string_t *ret = malloc(0xC + len * 2);
    ret->unknown1 = 0;
    ret->unknown2 = 0;
    ret->length = len;
    char *cptr = (uint32_t) ret + 0xC;
    for (int x = 0; x < len * 2; x += 2) {
        cptr[x] = str[x / 2];
        cptr[x + 1] = 0;
    }
    return ret;
}

int toto_is_enabled(const char *key) {
    if (toto_patch_json == NULL)return false;
    cJSON *obj = cJSON_GetObjectItem(toto_patch_json, key);
    dlog("get cfg:%s ret:%x", key, obj->valueint);
    return obj->valueint != 0;
}

void *(*old_damaged)(void *obj, void *n, int val, void *actor, int hitCound, int multiDamageIndex);

void *(*old_getendpoint)(void *, void *);

int (*old_open)(const char *file, int oflag, mode_t mode);


void *new_damaged(void *obj, void *n, int val, void *actor, int hitCound, int multiDamageIndex) {
    if (ilbase == 0) {
        derr("bad ilbase!!");
        goto _ret;
    }
    float enduce = *(float *) ((uint32_t) obj + offset_PieceEnduceM);
    //dlog("endure:%f", enduce);
    if (enduce == 0.0) {
        float mulrate = (float) atk_mul / 10;
        val = (int) ((float) val * mulrate);
    } else {
        val = 1;
    }

    _ret:
    return old_damaged(obj, n, val, actor, hitCound, multiDamageIndex);
}

void *new_getendpoint(void *obj, void *urlobj) {
    uint32_t ptr = old_getendpoint(obj, urlobj);
    uint32_t size = *(uint32_t *) (ptr + 0x8);
    char *str = (char *) (ptr + 0xC);
    char *result = malloc(size + 1);
    for (int x = 0; x < size; x++) {
        result[x] = str[x * 2];
    }
    result[size] = 0;
    dlog("ret size:%d str:%s", size, result);
    free(result);
    return ptr;
}

void *(*old_getbaseurl)(void *);

void *new_getbaseurl(void *obj) {
    dlog("try to get baseurl.");
    return istr_baseurl;
}

int new_open(const char *a1, int a2, mode_t a3) {
    if (hooked || stringContains(a1, "maps"))goto _ret;
    uint32_t base = get_module_base(getpid(), "libil2cpp.so");
    dlog("base:%x open:%s", base, a1);
    if (base != 0 && !hooked) {
        hooked = true;
        ilbase = base;
        dlog("libil2cpp.so load address:%lx", base);
        uint32_t func = base + offset_PieceDamaged;//Piece$$Damaged
        registerInlineHook((uint32_t) func, (uint32_t) new_damaged, (uint32_t **) &old_damaged);
        if (toto_is_enabled("res_redirect") || toto_is_enabled("dump")) {
            func = base + offset_SettingGetEndpoint;
            registerInlineHook((uint32_t) func, (uint32_t) new_getendpoint, (uint32_t **) &old_getendpoint);
            func = base + offset_SettingGetBaseURL;
            registerInlineHook((uint32_t) func, (uint32_t) new_getbaseurl, (uint32_t **) &old_getbaseurl);
        }
        inlineHookAll();
        dlog("Hook done.");
    }
    _ret:
    return old_open(a1, a2, a3);
}

void load_cfg() {
    char *buff = read_all_from_file("/mnt/sdcard/toto/cfg.json");
    toto_patch_json = cJSON_Parse(buff);
    atk_mul = (uint32_t) cJSON_GetObjectItem(toto_patch_json, "atk_mul")->valueint;
    offset_PieceDamaged = (uint32_t) cJSON_GetObjectItem(toto_patch_json, "damaged")->valueint;
    dlog("offset_PieceDamaged:%x", offset_PieceDamaged);
    offset_SettingGetBaseURL = (uint32_t) cJSON_GetObjectItem(toto_patch_json, "getbaseurl")->valueint;
    dlog("offset_SettingGetBaseURL:%x", offset_SettingGetBaseURL);
    offset_SettingGetEndpoint = (uint32_t) cJSON_GetObjectItem(toto_patch_json, "getendpoint")->valueint;
    dlog("offset_SettingGetEndpoint:%x", offset_SettingGetEndpoint);
    new_baseurl = cJSON_GetObjectItem(toto_patch_json, "base_url")->valuestring;
    dlog("base url:%s", new_baseurl);
}

void __attribute__((constructor)) init(void) {
    dlog("Module init...");
    load_cfg();
    istr_baseurl = il2cpp_string_ctor(new_baseurl);
    registerInlineHook((uint32_t) open, (uint32_t) new_open, (uint32_t **) &old_open);
    inlineHookAll();
    dlog("Module done.");
}