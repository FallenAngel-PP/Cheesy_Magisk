#!/system/bin/sh
#####################################################################
#   Pico Magisk Setup (Secure Boot Safe)
#   Filename: pico_setup.sh
#####################################################################
#
# Adapted for Pico with Secure Boot enabled
# This script does NOT modify any protected partitions
# Everything runs from tmpfs and /data only
#
# Support API level: 23 - 35
#
#####################################################################

mount_tmpfs() {
  mv magisk magisk.tmp
  mount -t tmpfs -o 'mode=0755' magisk $1
  mv magisk.tmp magisk
}

mount_sbin() {
  mount_tmpfs /sbin
  chcon u:object_r:rootfs:s0 /sbin
}

publish_su_to_namespaces() {
  echo "Publishing su through /system/xbin..."

  SU_OVERLAY="/data/adb/magisk/.su_path"
  mkdir -p "$SU_OVERLAY"
  chmod 755 "$SU_OVERLAY"

  if [ -d /system/xbin ]; then
    cp -af /system/xbin/. "$SU_OVERLAY/" 2>/dev/null
  fi

  ln -sf "$MAGISKTMP/su" "$SU_OVERLAY/su"

  echo "Mounting xbin overlay in current namespace..."
  "$SCRIPT_DIR/busybox" mount -o bind "$SU_OVERLAY" /system/xbin || \
    echo "WARNING: Could not mount xbin overlay in current namespace"

  for ZPID in $(pidof zygote zygote32 zygote64 2>/dev/null); do
    echo "Mounting xbin overlay in Zygote namespace PID $ZPID..."
    "$SCRIPT_DIR/busybox" nsenter -t "$ZPID" -m -- \
      "$SCRIPT_DIR/busybox" mount -o bind "$SU_OVERLAY" /system/xbin || \
      echo "WARNING: Could not mount xbin overlay in Zygote PID $ZPID"
  done

  ADBD_PID="$(pidof adbd)"
  if [ -n "$ADBD_PID" ]; then
    echo "Mounting xbin overlay in adbd namespace PID $ADBD_PID..."
    "$SCRIPT_DIR/busybox" nsenter -t "$ADBD_PID" -m -- \
      "$SCRIPT_DIR/busybox" mount -o bind "$SU_OVERLAY" /system/xbin || \
      echo "WARNING: Could not mount xbin overlay in adbd namespace"
  fi

  if [ -e /system/xbin/su ]; then
    echo "Public su path:"
    ls -la /system/xbin/su
  else
    echo "WARNING: /system/xbin/su is not visible in current namespace"
  fi
}

if [ ! -f /system/build.prop ]; then
  echo 'Please run this script on the device!'
  exit 1
fi

# ===== BOOT PROTECTION FOR PICO WITH SECURE BOOT =====
# Protect ALL boot partitions from accidental flashing
echo "Protecting boot partitions..."

# Standard boot partitions
if [ -L /dev/block/by-name/boot_a ]; then
  mv /dev/block/by-name/boot_a /dev/block/by-name/DO_NOT_FLASH_boot_a 2>/dev/null
  ln -s /dev/does/not/exist_a /dev/block/by-name/boot_a
fi
if [ -L /dev/block/by-name/boot_b ]; then
  mv /dev/block/by-name/boot_b /dev/block/by-name/DO_NOT_FLASH_boot_b 2>/dev/null
  ln -s /dev/does/not/exist_b /dev/block/by-name/boot_b
fi

# Init boot partitions (GKI - contains ramdisk)
if [ -L /dev/block/by-name/init_boot_a ]; then
  mv /dev/block/by-name/init_boot_a /dev/block/by-name/DO_NOT_FLASH_init_boot_a 2>/dev/null
  ln -s /dev/does/not/exist_a /dev/block/by-name/init_boot_a
fi
if [ -L /dev/block/by-name/init_boot_b ]; then
  mv /dev/block/by-name/init_boot_b /dev/block/by-name/DO_NOT_FLASH_init_boot_b 2>/dev/null
  ln -s /dev/does/not/exist_b /dev/block/by-name/init_boot_b
fi

# Vendor boot partitions
if [ -L /dev/block/by-name/vendor_boot_a ]; then
  mv /dev/block/by-name/vendor_boot_a /dev/block/by-name/DO_NOT_FLASH_vendor_boot_a 2>/dev/null
  ln -s /dev/does/not/exist_a /dev/block/by-name/vendor_boot_a
fi
if [ -L /dev/block/by-name/vendor_boot_b ]; then
  mv /dev/block/by-name/vendor_boot_b /dev/block/by-name/DO_NOT_FLASH_vendor_boot_b 2>/dev/null
  ln -s /dev/does/not/exist_b /dev/block/by-name/vendor_boot_b
fi

echo "Boot partitions protected. Magisk cannot flash anything."

# ===== SETUP =====
cd "$(dirname "$0")"

if [ -z "$FIRST_STAGE" ]; then
  export FIRST_STAGE=1
  export ASH_STANDALONE=1
  if [ $(./busybox id -u) -ne 0 ]; then
    # Re-exec script with root
    exec /system/xbin/su 0 ./busybox sh $0
  else
    # Re-exec script with busybox
    exec ./busybox sh $0
  fi
fi

# NOTE: Magisk app should be installed BEFORE running this script
# (see start_magisk.sh pre-install step)

# Extract files from APK using absolute path
SCRIPT_DIR="$(pwd)"
echo "Extracting Magisk files from $SCRIPT_DIR/magisk.apk..."
unzip -oj "$SCRIPT_DIR/magisk.apk" 'assets/util_functions.sh' 'assets/stub.apk'
if [ ! -f "./util_functions.sh" ]; then
  echo "ERROR: Failed to extract util_functions.sh"
  exit 1
fi
. ./util_functions.sh

api_level_arch_detect

unzip -oj "$SCRIPT_DIR/magisk.apk" "lib/$ABI/*" -x "lib/$ABI/libbusybox.so"
for file in lib*.so; do
  chmod 755 $file
  mv "$file" "${file:3:${#file}-6}"
done

if $IS64BIT && [ -e "/system/bin/linker" ]; then
  unzip -oj "$SCRIPT_DIR/magisk.apk" "lib/$ABI32/libmagisk.so"
  mv libmagisk.so magisk32
  chmod 755 magisk32
fi

# Stop zygote (and previous setup if exists)
echo "Stopping system services..."
magisk --stop 2>/dev/null

# SELinux workaround - try both methods

if [ -d /debug_ramdisk ]; then
  umount -l /debug_ramdisk 2>/dev/null
fi

# Make sure boot completed props are not set
runcon u:r:init:s0 setprop sys.boot_completed 0 2>/dev/null || setprop sys.boot_completed 0

# Mount /cache if not already mounted
if ! grep -q ' /cache ' /proc/mounts; then
  mount -t tmpfs -o 'mode=0755' tmpfs /cache
fi

MAGISKTMP=/sbin

# Setup bin overlay (all in tmpfs - nothing persistent!)
echo "Setting up Magisk in tmpfs..."
if mount | grep -q rootfs; then
  # Legacy rootfs
  mount -o rw,remount /
  rm -rf /root
  mkdir /root /sbin 2>/dev/null
  chmod 750 /root /sbin
  ln /sbin/* /root
  mount -o ro,remount /
  mount_sbin
  ln -s /root/* /sbin
elif [ -e /sbin ]; then
  # Legacy SAR
  mount_sbin
  mkdir -p /dev/sysroot
  block=$(mount | grep ' / ' | awk '{ print $1 }')
  [ $block = "/dev/root" ] && block=/dev/block/vda1
  mount -o ro $block /dev/sysroot
  for file in /dev/sysroot/sbin/*; do
    [ ! -e $file ] && break
    if [ -L $file ]; then
      cp -af $file /sbin
    else
      sfile=/sbin/$(basename $file)
      touch $sfile
      mount -o bind $file $sfile
    fi
  done
  umount -l /dev/sysroot
  rm -rf /dev/sysroot
else
  # Android Q+ without sbin (most likely for Pico)
  MAGISKTMP=/debug_ramdisk
  mount_tmpfs /debug_ramdisk
  echo "Using /debug_ramdisk for Magisk temp files"
fi

# Magisk data directories (only place where files persist)
echo "Creating Magisk data directories..."
mkdir -p $MAGISKBIN 2>/dev/null
unzip -oj "$SCRIPT_DIR/magisk.apk" 'assets/*.sh' -d $MAGISKBIN
mkdir -p /data/adb/modules 2>/dev/null
mkdir -p /data/adb/post-fs-data.d 2>/dev/null
mkdir -p /data/adb/service.d 2>/dev/null

LOCALTMP="/data/local/tmp"
FRIDA="$LOCALTMP/frida.zip"
CHEAT="$LOCALTMP/cheat_engine.zip"
MODULE_DIR="/data/adb/modules"

# Install Cheat-Engine Server module if it is checked
if [ "$(getprop debug.magisk.cheatengine)" = "1" ]; then

  if [ ! -d "$MODULE_DIR/magisk-cheat-engine" ]; then

    if [ -f "$CHEAT" ]; then
      echo "Installing Cheat Engine Server"

      unzip -o "$CHEAT" "magisk-cheat-engine/*" -d "$MODULE_DIR" >/dev/null 2>&1

      setprop persist.magisk.cheatengine 1
      echo "Cheat Engine Server Installed"

    else
      echo "ZIP not found: $CHEAT"
    fi
  fi
fi

# Install Frida Server module if it is checked
if [ "$(getprop debug.magisk.frida)" = "1" ]; then

  if [ ! -d "$MODULE_DIR/frida" ]; then

    if [ -f "$FRIDA" ]; then
      echo "Installing Frida-Server"

      unzip -o "$FRIDA" "frida/*" -d "$MODULE_DIR" >/dev/null 2>&1

      setprop persist.magisk.frida 1
      echo "Frida-Server Installed"

    else
      echo "ZIP not found: $FRIDA"
    fi
  fi
fi

FRIDADIR="/data/adb/modules/frida"
CHEATDIR="/data/adb/modules/magisk-cheat-engine"

if [ ! -d "$CHEATDIR" ] || [ -f "$CHEATDIR/remove" ]; then
  setprop persist.magisk.cheatengine 0
fi

if [ ! -d "$FRIDADIR" ] || [ -f "$FRIDADIR/remove" ]; then
  setprop persist.magisk.frida 0
fi

# Copy Magisk binaries to tmpfs
echo "Copying Magisk binaries to tmpfs..."
for file in magisk magisk32 magiskpolicy stub.apk; do
  if [ -f ./$file ]; then
    chmod 755 ./$file
    cp -af ./$file $MAGISKTMP/$file
    cp -af ./$file $MAGISKBIN/$file
  fi
done
cp -af ./magiskboot $MAGISKBIN/magiskboot 2>/dev/null
cp -af ./magiskinit $MAGISKBIN/magiskinit 2>/dev/null
cp -af ./busybox $MAGISKBIN/busybox

# Create symlinks
# Create symlinks in Magisk tmpfs
ln -sf ./magisk "$MAGISKTMP/su"
ln -sf ./magisk "$MAGISKTMP/resetprop"
ln -sf ./magiskpolicy "$MAGISKTMP/supolicy"

# Create aliases in /data/adb/magisk too
ln -sf ./magisk "$MAGISKBIN/su"
ln -sf ./magisk "$MAGISKBIN/resetprop"
ln -sf ./magiskpolicy "$MAGISKBIN/supolicy"

export MAGISKTMP
export PATH="$MAGISKTMP:$MAGISKBIN:/system/bin:/system/xbin:$PATH"

echo "Running preinit..."
MAKEDEV=1 "$MAGISKTMP/magisk" --preinit-device
echo "preinit exit: $?"

# Setup Magisk tmpfs structure
mkdir -p $MAGISKTMP/.magisk/device
mkdir -p $MAGISKTMP/.magisk/worker
mount_tmpfs $MAGISKTMP/.magisk/worker
WORKER="$MAGISKTMP/.magisk/worker"

/system/bin/mount --make-private "$WORKER" 2>/dev/null || \
/system/bin/toybox mount --make-private "$WORKER" 2>/dev/null || \
echo "WARNING: Could not mark $WORKER private"
touch $MAGISKTMP/.magisk/config

export MAGISKTMP

RULESCMD=""
rule="$MAGISKTMP/.magisk/preinit/sepolicy.rule"
[ -f "$rule" ] && RULESCMD="--apply $rule"

# SELinux policy patching (in memory only!)
echo "Patching SELinux policy in memory..."
if [ -d /sys/fs/selinux ]; then
  if [ -f /vendor/etc/selinux/precompiled_sepolicy ]; then
    ./magiskpolicy --load /vendor/etc/selinux/precompiled_sepolicy --live --magisk $RULESCMD 2>&1
  elif [ -f /sepolicy ]; then
    ./magiskpolicy --load /sepolicy --live --magisk $RULESCMD 2>&1
  else
    ./magiskpolicy --live --magisk $RULESCMD 2>&1
  fi
fi

# Boot up Magisk
echo "Starting Magisk..."

echo "Running post-fs-data..."
"$MAGISKTMP/magisk" --post-fs-data
POST_FS_RC=$?
echo "post-fs-data exit: $POST_FS_RC"

echo "Starting Magisk service..."
"$MAGISKTMP/magisk" --service
SERVICE_RC=$?
echo "service exit: $SERVICE_RC"

sleep 2

echo "Completing Magisk boot..."
"$MAGISKTMP/magisk" --boot-complete
BOOT_RC=$?
echo "boot-complete exit: $BOOT_RC"

echo "Checking Magisk daemon..."
ps -A -Z | grep -i '[m]agisk' || true

echo "Testing su..."
"$MAGISKTMP/su" -c id
SU_RC=$?
echo "su exit: $SU_RC"

# Publish su before restarting Android framework. Newly started apps
# inherit the /system/xbin overlay through Zygote.
publish_su_to_namespaces

echo "Testing public su..."
if [ -x /system/xbin/su ]; then
  /system/xbin/su -c id
  echo "public su exit: $?"
else
  echo "public su exit: 127"
fi

# Cleanup tmp directory
echo "Cleanup data/local/tmp"
TMP=/data/local/tmp

rm -f \
  "$TMP/busybox" \
  "$TMP/magisk" \
  "$TMP/magisk32" \
  "$TMP/magiskinit" \
  "$TMP/start_magisk.sh" \
  "$TMP/util_functions.sh" \
  "$TMP/init-ld" \
  "$TMP/magisk.apk" \
  "$TMP/magiskboot" \
  "$TMP/magiskpolicy" \
  "$TMP/pico_setup.sh" \
  "$TMP/frida.zip" \
  "$TMP/cheat_engine.zip" \
  "$TMP/resetprop" \
  "$TMP/stub.apk"

echo "Cleanup done"

setprop debug.magisk.preinit 2
runcon u:r:init:s0 setprop sys.boot_completed 1 2>/dev/null || setprop sys.boot_completed 1

echo ""
echo "===== Magisk Setup Complete ====="
echo "Magisk is now running in tmpfs (RAM only)"
echo "NO partitions were modified - Secure Boot safe!"
echo ""
echo "IMPORTANT: This setup is NOT persistent!"
echo "After reboot, you need to run this script again."
echo ""
echo "You can now use Magisk Manager and install modules."
echo "==================================="