# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
#
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE :=  hb
LOCAL_SRC_FILES := ../libhookbase.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog -L$(LOCAL_PATH)/../
LOCAL_MODULE := toto
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/../include
LOCAL_SRC_FILES := ../d_helper.c ../cJSON.c ../main.c
LOCAL_STATIC_LIBRARIES :=  hb
LOCAL_CFLAGS += -Wl,-Bstatic -lstdc++ -lsupc++ -Wl,-Bdynamic -D LOG_ON=1 -fvisibility=hidden
LOCAL_CFLAGS += -std=c99
include $(BUILD_SHARED_LIBRARY)


