package com.fallenangel.cheesymagisk;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.util.concurrent.Executor;
import java.net.InetSocketAddress;

import com.cgutman.adblib.AdbBase64;
import com.cgutman.adblib.AdbConnection;
import com.cgutman.adblib.AdbCrypto;
import com.cgutman.adblib.AdbStream;

public class MainActivity extends AppCompatActivity {

    private TextView console;
    private ScrollView scroll;
    private Button installBtn;
    private Button logbutton;
    private Button uninstall;
    private CheckBox checkfrida;
    private CheckBox checkcheat;
    public boolean frida = false;
    public boolean cheat = false;
    private Process cheeseProcess;
    private BufferedWriter shellIn;

    private static final String TMP_DIR = "/data/local/tmp";
    private static final String ADB_HOST = "127.0.0.1";
    private static final int ADB_PORT = 5555;
    private static final int CHUNK_SIZE = 3 * 1024;
    private static final long SHELL_TIMEOUT_MS = 120_000;

    String propValue = "0";

    private final AdbBase64 adbBase64 = data ->
            Base64.encodeToString(data, Base64.NO_WRAP);

    private final List<String> assetFiles = Arrays.asList(
            "busybox",
            "magisk.apk",
            "start_magisk.sh",
            "pico_setup.sh",
            "frida.zip",
            "cheat_engine.zip"
    );

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        console = findViewById(R.id.console);
        scroll = findViewById(R.id.scroll);
        installBtn = findViewById(R.id.installBtn);
        logbutton = findViewById(R.id.logbutton);
        uninstall = findViewById(R.id.uninstall);
        checkfrida = findViewById(R.id.checkfrida);
        checkcheat = findViewById(R.id.checkcheat);

        String fridaValue = "0";
        String fridainstalledValue = "0";
        String cheatValue = "0";
        String cheatinstalledValue = "0";
        String product = "";
        String version = "";
        List<String> versionList = List.of("5.11.1", "5.12.0", "5.12.6", "5.13.0", "5.13.2", "5.13.3");

        try {
            Process p = Runtime.getRuntime().exec("getprop ro.build.product");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            product = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Process p = Runtime.getRuntime().exec("getprop ro.pui.build.version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            version = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (! "sparrow".equals(product) || (! versionList.contains(version))) {
            log("\n\nYour Pico is not compatible to this exploit (cheese exploit)\n\nOnly Pico 4 Ultra on OS 5.13.3 and below is supported");
            logbutton.setEnabled(false);
            installBtn.setEnabled(false);
            checkfrida.setEnabled(false);
            checkcheat.setEnabled(false);
            return;
        }
        try {
            Process p = Runtime.getRuntime().exec("getprop debug.magisk.preinit");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            propValue = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Process p = Runtime.getRuntime().exec("getprop debug.magisk.frida");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            fridaValue = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Process p = Runtime.getRuntime().exec("getprop persist.magisk.frida");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            fridainstalledValue = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Process p = Runtime.getRuntime().exec("getprop debug.magisk.cheatengine");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            cheatValue = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Process p = Runtime.getRuntime().exec("getprop persist.magisk.cheatengine");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            cheatinstalledValue = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if ("1".equals(fridaValue)) {
            frida = true;
            checkfrida.setChecked(true);
        }
        if ("1".equals(cheatValue)) {
            cheat = true;
            checkcheat.setChecked(true);
        }

        String selinuxStatus = "enforcing";
        try {
            Process p = Runtime.getRuntime().exec("getenforce");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            selinuxStatus = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if ("permissive".equalsIgnoreCase(selinuxStatus) && !"2".equals(propValue)) {
            try {
                Runtime.getRuntime().exec(new String[]{"setprop", "debug.magisk.preinit", "1"});
                propValue = "1";
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if ("1".equals(fridainstalledValue)) {
            checkfrida.setEnabled(false);
            checkfrida.setChecked(true);
        }
        if ("1".equals(cheatinstalledValue)) {
            checkcheat.setEnabled(false);
            checkcheat.setChecked(true);
        }

        final String preinitProp = propValue;

        if ("1".equals(preinitProp)) {
            installBtn.setText("Start Magisk");
            logbutton.setEnabled(false);
            log("---------------------STAGE  1---------------------\n\n");
        } else if ("2".equals(preinitProp)) {
            installBtn.setText("Magisk Already Running");
            checkfrida.setEnabled(false);
            checkcheat.setEnabled(false);
            log("---------------------STAGE  2---------------------\n\n");
        } else {
            installBtn.setText("Start Magisk");
            logbutton.setEnabled(false);
            log("---------------------STAGE  0---------------------\n\n");
        }

        log("Install Magisk in your Ram to get systemwide root access over su\n\n\nWhile the exploit runs, the Pico could crash. This is no problem and will not harm you\n\n\n--------------------------------------------------\n!!! DON'T click 'Update', 'Install' or 'Uninstall' in Magisk App, this could brick your device !!!\n--------------------------------------------------\n\n\n-------------------CONSOLE LOGS-------------------\n\n");

        if ("2".equals(preinitProp)) {
            log("--------------------------------------------------");
            log(" Magisk is already running. Root apps are usable.");
            log("--------------------------------------------------");
            installBtn.setEnabled(false);
        }

        installBtn.setOnClickListener(v -> {
            installBtn.setEnabled(false);
            checkfrida.setEnabled(false);
            checkcheat.setEnabled(false);

            if ("1".equals(preinitProp)) {
                log("--------------------------------------------------");
                log("               Preparing Magisk...");
                log("--------------------------------------------------\n");
                Executors.newSingleThreadExecutor().execute(this::runCheese2);
            } else {
                log("--------------------------------------------------");
                log("          Exploiting P4U to get root...");
                log("--------------------------------------------------\n");
                Executors.newSingleThreadExecutor().execute(this::runCheese1);
            }
        });

        logbutton.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(this::showMagiskStartLog);
        });

        uninstall.setOnClickListener(v -> {
            installBtn.setEnabled(false);
            checkfrida.setEnabled(false);
            checkcheat.setEnabled(false);
            Executors.newSingleThreadExecutor().execute(this::uninstallmagisk);
        });

        checkfrida.setOnCheckedChangeListener((buttonView, isChecked) -> {
            frida = isChecked;
        });
        checkcheat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cheat = isChecked;
        });
    }

    private void showMagiskStartLog() {
        runOnUiThread(() -> console.setText(""));

        Executors.newSingleThreadExecutor().execute(() -> {
            StringBuilder sb = new StringBuilder();
            try {
                Process p = Runtime.getRuntime().exec(
                        new String[]{"su", "-c", "cat /data/local/tmp/pico_magisk_start.txt"}
                );

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                BufferedReader err = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }

                while ((line = err.readLine()) != null) {
                    sb.append("[ERROR] ").append(line).append("\n");
                }

                br.close();
                err.close();
                p.waitFor();

            } catch (Exception e) {
                sb.append("[EXCEPTION] ").append(e.toString());
                e.printStackTrace();
            }

            runOnUiThread(() -> console.setText(sb.toString()));
        });
    }



    private void uninstallmagisk() {
        if ("2".equals(propValue)) {
            runOnUiThread(() -> console.setText(""));

            uiLog("--------------------------------------------------");
            uiLog("             Uninstalling Magisk...");
            uiLog("--------------------------------------------------\n");

            Executors.newSingleThreadExecutor().execute(() -> {
                StringBuilder sb = new StringBuilder();
                try {
                    int exitCode = Runtime.getRuntime().exec(new String[]{
                            "su",
                            "-c",
                            "rm -rf " +
                                    "/data/adb/magisk " +
                                    "/data/adb/modules " +
                                    "/data/adb/post-fs-data.d " +
                                    "/data/adb/service.d " +
                                    "/data/adb/magisk.db; " +
                                    "setprop persist.magisk.frida 0; " +
                                    "setprop persist.magisk.cheatengine 0;" +
                                    "pm uninstall com.topjohnwu.magisk; " +
                                    "reboot"
                    }).waitFor();

                    runOnUiThread(() ->
                            log("[*] Cleanup exited with code " + exitCode)
                    );

                    installBtn.setEnabled(false);
                    logbutton.setEnabled(false);
                    uninstall.setEnabled(false);

                } catch (IOException e) {
                    Log.e("CLEANUP", "Failed to execute cleanup", e);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e("CLEANUP", "Cleanup was interrupted", e);
                }
            });
        } else {
            try {
                uiLog("--------------------------------------------------");
                uiLog("          Exploiting P4U to get root...");
                uiLog("--------------------------------------------------\n");
                copyAssetsToInternalOnly();
                String libPath = getApplicationInfo().nativeLibraryDir + "/libcheese.so";

                Process p = new ProcessBuilder(libPath)
                        .redirectErrorStream(true)
                        .start();

                BufferedReader out = new BufferedReader(
                        new InputStreamReader(p.getInputStream())
                );
                BufferedWriter in = new BufferedWriter(
                        new OutputStreamWriter(p.getOutputStream())
                );

                String line;

                while ((line = out.readLine()) != null) {
                    final String l = line;
                    runOnUiThread(() -> log(l));

                }

                int rc = p.waitFor();
                runOnUiThread(() -> log("[*] libcheese exited with code " + rc));
                if (rc == 1) {
                    runOnUiThread(() -> log("restarting..."));
                    uninstallmagisk();
                }
                uiLog("--------------------------------------------------");
                uiLog("             Uninstalling Magisk...");
                uiLog("--------------------------------------------------\n");
                executor.execute(() -> {
                    AdbConnection connection = null;
                    Socket socket = null;

                    try {
                        AdbCrypto crypto = loadOrCreateCrypto();

                        socket = waitForAdb(20, 1000);

                        connection = AdbConnection.create(socket, crypto);
                        connection.connect();

                        uiLog(shell(connection,
                                "rm -rf " +
                                        "/data/adb/magisk " +
                                        "/data/adb/modules " +
                                        "/data/adb/post-fs-data.d " +
                                        "/data/adb/service.d " +
                                        "/data/adb/magisk.db; " +
                                        "setprop persist.magisk.frida 0; " +
                                        "setprop persist.magisk.cheatengine 0;" +
                                        "pm uninstall com.topjohnwu.magisk; " +
                                        "setprop debug.magisk.preinit 1"
                        ));

                        uiLog("\n\n--------------------------------------------------");
                        uiLog("              Uninstalled Magisk");
                        uiLog("--------------------------------------------------");

                        runOnUiThread(() -> installBtn.setEnabled(false));
                        runOnUiThread(() -> logbutton.setEnabled(false));
                        runOnUiThread(() -> uninstall.setEnabled(false));

                    } catch (Exception e) {
                        Log.e("ADBLIB", "Uninstall failed", e);
                        uiLog("[ERROR] "
                                + e.getClass().getName()
                                + ": "
                                + e.getMessage()
                                + "\n"
                                + Log.getStackTraceString(e));

                    } finally {
                        try {
                            if (connection != null) {
                                connection.close();
                            }
                        } catch (Exception ignored) {
                        }

                        try {
                            if (socket != null && !socket.isClosed()) {
                                socket.close();
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> log("ERROR: " + e));
            }
        }
    }

    private void runCheese1() {
        try {
            copyAssetsToInternalOnly();
            String libPath = getApplicationInfo().nativeLibraryDir + "/libcheese.so";

            Process p = new ProcessBuilder(libPath)
                    .redirectErrorStream(true)
                    .start();

            BufferedReader out = new BufferedReader(
                    new InputStreamReader(p.getInputStream())
            );
            BufferedWriter in = new BufferedWriter(
                    new OutputStreamWriter(p.getOutputStream())
            );

            String line;

            while ((line = out.readLine()) != null) {
                final String l = line;
                runOnUiThread(() -> log(l));

            }

            int rc = p.waitFor();
            runOnUiThread(() -> log("[*] libcheese exited with code " + rc));
            if (rc == 1) {
                uiLog("\n\n--------------------------------------------------");
                uiLog("             Restarting Exploit...");
                uiLog("--------------------------------------------------\n");
                runCheese1();
            }

            uiLog("\n\n--------------------------------------------------");
            uiLog("  Successfully rooted, starting magisk setup...");
            uiLog("--------------------------------------------------");

            runCheese2();

        } catch (Exception e) {
            runOnUiThread(() -> log("ERROR: " + e));
        }
    }

    private void runCheese2() {
        executor.execute(() -> {
            AdbConnection connection = null;
            Socket socket = null;

            try {
                AdbCrypto crypto = loadOrCreateCrypto();

                socket = waitForAdb(20, 1000);

                connection = AdbConnection.create(socket, crypto);
                connection.connect();

                pushAssets(connection);

                if (frida) {
                    uiLog(shell(connection, "setprop debug.magisk.frida 1"));
                } else {
                    uiLog(shell(connection, "setprop debug.magisk.frida 0"));
                }

                if (cheat) {
                    uiLog(shell(connection, "setprop debug.magisk.cheatengine 1"));
                } else {
                    uiLog(shell(connection, "setprop debug.magisk.cheatengine 0"));
                }

                uiLog(shell(connection, "ls -lah " + TMP_DIR));

                uiLog(shell(connection,
                        "chmod 755 " +
                                TMP_DIR + "/busybox " +
                                TMP_DIR + "/start_magisk.sh " +
                                TMP_DIR + "/pico_setup.sh"
                ));

                uiLog(shell(connection,
                        "ls -l " +
                                TMP_DIR + "/busybox " +
                                TMP_DIR + "/start_magisk.sh " +
                                TMP_DIR + "/pico_setup.sh"
                ));

                uiLog("Starting Magisk setup for Pico...");
                uiLog("This will take a few seconds ...");

                uiLog(shell(
                        connection,
                        "cd " + TMP_DIR + " && sh ./start_magisk.sh"
                ));

                String propValue = shell(connection, "getprop debug.magisk.preinit").trim();

                if ("2".equals(propValue)) {
                    runOnUiThread(() -> {
                        installBtn.setText("Magisk Already Running");
                        installBtn.setEnabled(false);
                        logbutton.setEnabled(true);
                        uninstall.setEnabled(true);
                    });
                }

                uiLog("\n\n--------------------------------------------------");
                uiLog("               Magisk is running...");
                uiLog("--------------------------------------------------");

            } catch (Exception e) {
                Log.e("ADBLIB", "runCheese2 failed", e);
                uiLog("[ERROR] "
                        + e.getClass().getName()
                        + ": "
                        + e.getMessage()
                        + "\n"
                        + Log.getStackTraceString(e));

            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (Exception ignored) {
                }

                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                } catch (Exception ignored) {
                }
            }
        });
    }

    private void uiLog(String msg) {
        runOnUiThread(() -> log(msg));
    }

    private void copyAssetsToInternalOnly() throws IOException {
        String[] assets = getAssets().list("");
        if (assets == null) return;

        for (String name : assets) {

            String[] sub = getAssets().list(name);
            if (sub != null && sub.length > 0) continue;

            File out = new File(getFilesDir(), name);
            if (out.exists()) continue;

            InputStream in = getAssets().open(name);
            FileOutputStream fos = new FileOutputStream(out);

            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) {
                fos.write(buf, 0, len);
            }

            in.close();
            fos.close();
            out.setExecutable(true, false);
        }
    }

    private void log(String msg) {
        SpannableString span = new SpannableString(msg + "\n");
        span.setSpan(new ForegroundColorSpan(Color.GREEN), 0, span.length(), 0);
        console.append(span);
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private AdbCrypto loadOrCreateCrypto() throws Exception {
        File privKeyFile = new File(getFilesDir(), "adb_priv.key");
        File pubKeyFile = new File(getFilesDir(), "adb_pub.key");

        if (privKeyFile.exists() && pubKeyFile.exists()) {
            try {
                return AdbCrypto.loadAdbKeyPair(adbBase64, privKeyFile, pubKeyFile);
            } catch (Exception e) {
                log("Saved ADB key invalid, generating a new one ...");
                privKeyFile.delete();
                pubKeyFile.delete();
            }
        }

        log("Generating new ADB keypair ...");
        AdbCrypto crypto = AdbCrypto.generateAdbKeyPair(adbBase64);
        crypto.saveAdbKeyPair(privKeyFile, pubKeyFile);

        log("New ADB keypair generated.");
        log("Confirm ADB authorization on the first connection attempt.");

        return crypto;
    }

    private void pushAssets(AdbConnection connection) throws Exception {
        for (String name : assetFiles) {
            try (InputStream input = getAssets().open(name)) {
                byte[] data = readAllBytes(input);
                String remotePath = TMP_DIR + "/" + name;

                adbPush(connection, data, remotePath, 0644);
                uiLog("Push OK: " + name + " (" + data.length + " bytes)");
            } catch (Exception e) {
                uiLog("Push FAILED: " + name + " -> " + e.getMessage());
                throw e;
            }
        }
    }

    private void adbPush(AdbConnection connection, byte[] data, String remotePath, int mode) throws Exception {
        AdbStream stream = connection.open("sync:");

        try {
            syncSend(stream, "SEND", remotePath + "," + mode);

            int offset = 0;
            while (offset < data.length) {
                int len = Math.min(CHUNK_SIZE, data.length - offset);
                byte[] chunk = Arrays.copyOfRange(data, offset, offset + len);

                syncSendBytes(stream, "DATA", chunk);
                offset += len;
            }

            syncSendInt(stream, "DONE", (int) (System.currentTimeMillis() / 1000));

            byte[] status = stream.read();
            if (status == null || status.length < 4) {
                throw new IOException("SYNC push: no valid status response received");
            }

            String s = new String(status, 0, 4, StandardCharsets.US_ASCII);

            if (!"OKAY".equals(s)) {
                throw new IOException("SYNC push failed: " + new String(status, StandardCharsets.UTF_8));
            }
        } finally {
            stream.close();
        }
    }

    private Socket waitForAdb(int attempts, long delayMs) throws Exception {
        IOException lastError = null;

        for (int i = 1; i <= attempts; i++) {
            try {
                uiLog("[*] ADB connection attempt " + i + "/" + attempts);

                Socket socket = new Socket();
                socket.connect(
                        new java.net.InetSocketAddress(ADB_HOST, ADB_PORT),
                        3000
                );

                uiLog("[+] ADB port is reachable");
                return socket;

            } catch (IOException e) {
                lastError = e;
                uiLog("[-] ADB not ready: " + e.getMessage());
                Thread.sleep(delayMs);
            }
        }

        throw new IOException(
                "ADB on " + ADB_HOST + ":" + ADB_PORT +
                        " was not reachable after " + attempts + " attempts",
                lastError
        );
    }

    private void syncSend(AdbStream stream, String id, String payload)
            throws IOException, InterruptedException {
        byte[] data = payload.getBytes(StandardCharsets.UTF_8);
        syncSendRaw(stream, id, data);
    }

    private void syncSendBytes(AdbStream stream, String id, byte[] data)
            throws IOException, InterruptedException {
        syncSendRaw(stream, id, data);
    }

    private void syncSendInt(AdbStream stream, String id, int value)
            throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(id.getBytes(StandardCharsets.US_ASCII));
        writeIntLE(out, value);
        stream.write(out.toByteArray());
    }

    private void syncSendRaw(AdbStream stream, String id, byte[] data)
            throws IOException, InterruptedException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(id.getBytes(StandardCharsets.US_ASCII));
        writeIntLE(out, data.length);
        out.write(data);
        stream.write(out.toByteArray());
    }

    private void writeIntLE(ByteArrayOutputStream out, int value) {
        out.write(value & 0xff);
        out.write((value >> 8) & 0xff);
        out.write((value >> 16) & 0xff);
        out.write((value >> 24) & 0xff);
    }

    private String shell(AdbConnection connection, String command) throws Exception {
        String wrappedCommand = command + " ; echo __SCRIPT_DONE__$?";
        AdbStream stream = connection.open("shell:" + wrappedCommand);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(SHELL_TIMEOUT_MS);
                stream.close();
            } catch (InterruptedException ignored) {
            } catch (IOException ignored) {
            }
        });
        watchdog.setDaemon(true);
        watchdog.start();

        try {
            while (!stream.isClosed()) {
                byte[] data;
                try {
                    data = stream.read();
                } catch (IOException e) {
                    break;
                }

                if (data != null && data.length > 0) {
                    output.write(data);
                    String text = output.toString("UTF-8");

                    if (text.contains("__SCRIPT_DONE__")) {
                        break;
                    }
                }
            }
        } finally {
            watchdog.interrupt();
            try {
                stream.close();
            } catch (Exception ignored) {
            }
        }

        return output.toString("UTF-8")
                .replaceAll("__SCRIPT_DONE__\\d+\\r?\\n?", "")
                .trim();
    }

    private void shellLive(AdbConnection connection, String command) throws Exception {
        String wrappedCommand = command + " ; echo __SCRIPT_DONE__$?";
        AdbStream stream = connection.open("shell:" + wrappedCommand);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(SHELL_TIMEOUT_MS);
                stream.close();
            } catch (InterruptedException ignored) {
            } catch (IOException ignored) {
            }
        });
        watchdog.setDaemon(true);
        watchdog.start();

        try {
            while (!stream.isClosed()) {
                byte[] data;

                try {
                    data = stream.read();
                } catch (IOException e) {
                    break;
                }

                if (data != null && data.length > 0) {
                    output.write(data);

                    String text = output.toString("UTF-8");

                    if (text.contains("__SCRIPT_DONE__")) {
                        String clean = text.replaceAll("__SCRIPT_DONE__\\d+\\r?\\n?", "").trim();

                        if (!clean.isEmpty()) {
                            log(clean);
                        }

                        break;
                    }

                    String chunk = new String(data, StandardCharsets.UTF_8);
                    log(chunk);
                }
            }
        } finally {
            watchdog.interrupt();
            try {
                stream.close();
            } catch (Exception ignored) {
            }
        }
    }

    private byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;

        while ((read = input.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }

        return buffer.toByteArray();
    }
}

