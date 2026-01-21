#!/system/bin/sh
echo "Starting Magisk setup for Pico..."
cd "$(dirname "$0")"
umask 000

# Install Magisk app BEFORE entering init namespace
echo "Installing Magisk app..."
if ! pm path com.topjohnwu.magisk >/dev/null 2>&1; then
  pm install -r -g "$PWD/magisk.apk"
else
  echo "Magisk app already installed"
fi

# Now enter init namespace and run main setup
export FIRST_STAGE=1
export ASH_STANDALONE=1
exec ./busybox setsid ./busybox nsenter -m/proc/1/ns/mnt "$PWD/busybox" sh "$PWD/pico_setup.sh" >/data/local/tmp/pico_magisk_start.txt 2>&1