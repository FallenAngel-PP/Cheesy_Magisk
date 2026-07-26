# 🧀 Cheesy_Magisk

**Cheesy Magisk** is an Android application that installs **Magisk directly into RAM** using the **cheese root exploit**, enabling **system-wide `su` access** without permanently modifying the system partition.

This project is primarily intended for **research, development, and advanced debugging purposes** on supported Pico devices.

---

## ✨ Features

- Install **Magisk into RAM** via a root exploit  
- Enables **system-wide root (`su`) access**
- **No permanent installation** – nothing is flashed to disk (Secure Boot Safe)
- Optional module installation of:
  - **Frida Server**
  - **Cheat Engine Server**
- Designed for debugging, reverse engineering, and experimentation

---

## ⚠️ Important Limitations

- **Tethered solution**  
  Root access is **temporary** and must be re-applied **after every reboot**
- **Device-specific**  
  Works **only** on:
  - **Pico 4 Ultra**
- **OS restriction**  
  Supported on:
  - **Pico OS 5.13.3 and below**

---

## 🛠 How It Works (High-Level)

1. A root exploit is executed
2. Magisk is injected and initialized **in RAM**
3. System-wide `su` becomes available
4. (Optional) Frida and/or Cheat Engine servers are installed as Magisk modules
5. After a reboot, the process must be repeated

---

## 🧩 Optional Modules

Cheesy_Magisk can optionally install the following as Magisk modules:

- **Frida Server**  
  For dynamic instrumentation and runtime analysis
- **Cheat Engine Server**  
  For memory inspection and modification

---

## 🚨 Disclaimer

> **This project is for educational and research purposes only.**

- Use at your own risk  
- May violate device terms of service or warranty  
- The author is **not responsible** for bricked devices, data loss, or misuse  
- Do **not** use on unsupported devices or OS versions
- Do **not** click 'Install', 'Update', 'Uninstall' in Magisk (This does not concern the modules). When you want to uninstall Magisk, you can do it with Cheesy Magisk

---

## 🙌 Credits

- Magisk by **topjohnwu**
- Cheese Exploit by **Zhuowei**
- Frida & Cheat Engine teams
- The Android and reverse-engineering community