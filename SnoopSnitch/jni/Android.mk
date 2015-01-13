LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE:=		diag-helper
LOCAL_SRC_FILES:=	diag-helper.c
LOCAL_CFLAGS+=		-Wall -std=gnu99 -fPIE
LOCAL_LDLIBS:=		$(LOCAL_LDLIBS) -llog -pie

# We want to create the executable name with .so suffix, so that it
# gets automatically packaged into the apk.  However, the clever NDK
# makefiles prevent this.  Secondly, we need to prefix the library
# with "lib", or else it will not be installed by the PackageManager.
# To work around this, we include the content
# of NDK's build-executable.mk, with prefix and suffix changed accordingly.

# include $(BUILD_EXECUTABLE)

LOCAL_BUILD_SCRIPT := BUILD_EXECUTABLE
LOCAL_MAKEFILE     := $(local-makefile)

$(call check-defined-LOCAL_MODULE,$(LOCAL_BUILD_SCRIPT))
$(call check-LOCAL_MODULE,$(LOCAL_MAKEFILE))
$(call check-LOCAL_MODULE_FILENAME)

# we are building target objects
my := TARGET_

$(call handle-module-filename,lib,.so)
$(call handle-module-built)

LOCAL_MODULE_CLASS := EXECUTABLE
include $(BUILD_SYSTEM)/build-module.mk
