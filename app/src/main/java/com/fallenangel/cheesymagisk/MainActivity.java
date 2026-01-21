package com.fallenangel.cheesymagisk;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.graphics.Color;

import java.util.List;
import java.util.concurrent.Executors;

import androidx.appcompat.app.AppCompatActivity;

import java.io.*;

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

        String propValue = "0";
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
            uninstall.setEnabled(false);
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
            installBtn.setText("Activate Magisk");
            logbutton.setEnabled(false);
            log("---------------------STAGE  1---------------------\n\n");
        } else if ("2".equals(preinitProp)) {
            installBtn.setText("Magisk Already Running");
            uninstall.setEnabled(false);
            checkfrida.setEnabled(false);
            checkcheat.setEnabled(false);
            log("---------------------STAGE  2---------------------\n\n");
        } else {
            installBtn.setText("Prepare for Magisk");
            logbutton.setEnabled(false);
            log("---------------------STAGE  0---------------------\n\n");
        }

        log("Install Magisk in your Ram to get systemwide root access over su\n\nYou have to run the app twice, because on first run 'mount' isn't accessible on app context\n\n1. Runs the exploit, sets selinux permissive and restarts system-server\n\n2. Runs the exploit and starts magisk-setup.sh\n\n\nWhile the exploit runs, the Pico could crash. This is no problem and will not harm you\n\n\n--------------------------------------------------\n!!! DON'T click 'Update', 'Install' or 'Uninstall' in Magisk App, this could brick your device !!!\n--------------------------------------------------\n\n\n-------------------CONSOLE LOGS-------------------\n\n");

        if ("2".equals(preinitProp)) {
            log("[*] Magisk is already running. You can use root apps now.\n");
            installBtn.setEnabled(false);
        }

        installBtn.setOnClickListener(v -> {
            installBtn.setEnabled(false);
            uninstall.setEnabled(false);
            checkfrida.setEnabled(false);
            checkcheat.setEnabled(false);

            if ("1".equals(preinitProp)) {
                log("[*] Running Stage 2");
                Executors.newSingleThreadExecutor().execute(this::runCheese2);
            } else {
                log("[*] Running Stage 1");
                Executors.newSingleThreadExecutor().execute(this::runCheese1);
            }
        });

        logbutton.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(this::showMagiskStartLog);
        });

        uninstall.setOnClickListener(v -> {
            installBtn.setEnabled(false);
            uninstall.setEnabled(false);
            checkfrida.setEnabled(false);
            checkcheat.setEnabled(false);
            log("[*] Uninstalling Magisk");
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
        try {

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
            boolean rootReady = false;

            while ((line = out.readLine()) != null) {
                final String l = line;
                runOnUiThread(() -> log(l));

                if (!rootReady && l.contains("write enforce ok")) {
                    rootReady = true;
                    runOnUiThread(() -> log("[+] Root shell active\n\n[+] Uninstalling Magisk..."));
                    Thread.sleep(3000);
                    in.write("rm -f /data/local/tmp/pico_magisk_start.txt\n");
                    in.flush();
                    in.write("setprop debug.magisk.preinit 1\n");
                    in.flush();
                    in.write("setprop persist.magisk.frida 0\n");
                    in.flush();
                    in.write("setprop persist.magisk.cheatengine 0\n");
                    in.flush();
                    in.write("rm -rf /data/adb/*\n");
                    in.flush();
                    in.write("pm uninstall com.topjohnwu.magisk\n");
                    in.flush();
                    in.write("killall system_server\n");
                    in.flush();
                }
            }

            int rc = p.waitFor();
            runOnUiThread(() -> log("[*] libcheese exited with code " + rc));
            uninstallmagisk();

        } catch (Exception e) {
            runOnUiThread(() -> log("ERROR: " + e));
        }
    }

    private void runCheese1() {
        try {
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
            boolean rootReady = false;

            while ((line = out.readLine()) != null) {
                final String l = line;
                runOnUiThread(() -> log(l));

                if (!rootReady && l.contains("write enforce ok")) {
                    rootReady = true;
                    runOnUiThread(() -> log("[+] Root shell active\n\n[+] Restart System-Server..."));

                    Thread.sleep(3000);
                    if (frida) {
                        in.write("setprop debug.magisk.frida 1\n");
                        in.flush();
                    } else {
                        in.write("setprop debug.magisk.frida 0\n");
                        in.flush();
                    }
                    if (cheat) {
                        in.write("setprop debug.magisk.cheatengine 1\n");
                        in.flush();
                    } else {
                        in.write("setprop debug.magisk.cheatengine 0\n");
                        in.flush();
                    }
                    in.write("setprop debug.magisk.preinit 1\n");
                    in.flush();
                    in.write("killall system_server\n");
                    in.flush();
                }
            }

            int rc = p.waitFor();
            runOnUiThread(() -> log("[*] libcheese exited with code " + rc));
            runCheese1();

        } catch (Exception e) {
            runOnUiThread(() -> log("ERROR: " + e));
        }
    }

    private void runCheese2() {
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
            boolean rootReady = false;
            String src = getFilesDir().getAbsolutePath();

            while ((line = out.readLine()) != null) {
                final String l = line;
                runOnUiThread(() -> log(l));

                if (!rootReady && l.contains("write enforce ok")) {
                    rootReady = true;
                    runOnUiThread(() -> log("[+] Root shell active\n\n[+] Starting Magisk..."));

                    try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                    if (frida) {
                        in.write("setprop debug.magisk.frida 1\n");
                        in.flush();
                    } else {
                        in.write("setprop debug.magisk.frida 0\n");
                        in.flush();
                    }
                    if (cheat) {
                        in.write("setprop debug.magisk.cheatengine 1\n");
                        in.flush();
                    } else {
                        in.write("setprop debug.magisk.cheatengine 0\n");
                        in.flush();
                    }
                    in.write("rm -f /data/local/tmp/pico_magisk_start.txt\n");
                    in.flush();
                    in.write("cd /data/local/tmp\n");
                    in.write("cp " + src + "/* .\n");
                    in.write("chmod 755 *\n");
                    in.flush();
                    in.write("setprop debug.magisk.preinit 2\n");
                    in.flush();
                    in.write(
                            "exec /data/local/tmp/busybox nsenter -t 1 -m -- " +
                                    "/data/local/tmp/busybox sh /data/local/tmp/start_magisk.sh\n"
                    );
                    in.flush();
                }
            }

            int rc = p.waitFor();
            runOnUiThread(() -> log("[*] libcheese exited with code " + rc));

        } catch (Exception e) {
            runOnUiThread(() -> log("ERROR: " + e));
        }
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
}