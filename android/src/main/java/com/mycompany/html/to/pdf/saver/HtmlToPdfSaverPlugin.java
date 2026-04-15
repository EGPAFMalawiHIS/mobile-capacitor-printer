package com.mycompany.html.to.pdf.saver;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;

@CapacitorPlugin(
        name = "HtmlToPdfSaver",
        permissions = {
                @Permission(alias = "internet", strings = {Manifest.permission.INTERNET}),
                @Permission(alias = "storage", strings = {
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }),
                @Permission(alias = "network", strings = {
                        Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION
                })
        }
)
public class HtmlToPdfSaverPlugin extends Plugin {

    private PluginCall savedCall;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;

    @Override
    public void load() {
        super.load();
        connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @PluginMethod
    public void printWebPage(PluginCall call) {
        String content = call.getString("content");

        if (content == null) {
            call.reject("Must provide content to print");
            return;
        }

        getActivity().runOnUiThread(() -> {
            WebView webView = new WebView(getContext());
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    String jobName = "Document";
                    PrintManager printManager = (PrintManager) getContext().getSystemService(Context.PRINT_SERVICE);

                    PrintAttributes.Builder builder = new PrintAttributes.Builder();
                    builder.setMediaSize(PrintAttributes.MediaSize.ISO_A5);

                    PrintDocumentAdapter printAdapter = view.createPrintDocumentAdapter(jobName);

                    PrintDocumentAdapter wrapper = new PrintDocumentAdapter() {
                        @Override
                        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                             CancellationSignal cancellationSignal, LayoutResultCallback callback,
                                             Bundle extras) {
                            PrintAttributes.Builder builder = new PrintAttributes.Builder();

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                builder.setColorMode(newAttributes.getColorMode())
                                        .setDuplexMode(newAttributes.getDuplexMode())
                                        .setMinMargins(new PrintAttributes.Margins(219, 38, 48, 0))
                                        .setResolution(Objects.requireNonNull(newAttributes.getResolution()));
                            }

                            builder.setMediaSize(PrintAttributes.MediaSize.ISO_A5);
                            printAdapter.onLayout(oldAttributes, builder.build(), cancellationSignal, callback, extras);
                        }

                        @Override
                        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                            CancellationSignal cancellationSignal, WriteResultCallback callback) {
                            printAdapter.onWrite(pages, destination, cancellationSignal, callback);
                        }

                        @Override
                        public void onFinish() {
                            printAdapter.onFinish();
                        }
                    };

                    printManager.print(jobName, wrapper, builder.build());
                    call.resolve();
                }
            });

            webView.loadDataWithBaseURL(null, content, "text/html", "UTF-8", null);
        });
    }

    @PluginMethod
    public void printWebPageToNetworkPrinter(PluginCall call) {
        String content = call.getString("content");
        String printerIp = call.getString("printerIp");
        Integer printerPort = call.getInt("printerPort", 9100);

        if (content == null) {
            call.reject("Must provide content to print");
            return;
        }

        if (printerIp == null) {
            call.reject("Must provide printer IP address");
            return;
        }

        savedCall = call;

        // Try to print regardless of hotspot detection
        // The connection attempt will fail if printer is not reachable
        performHotspotPrinting(printerIp, printerPort, content);
    }

    @PluginMethod
    public void discoverHotspotDevices(PluginCall call) {
        // Scan for devices connected to the phone's hotspot
        new Thread(() -> {
            try {
                List<String> connectedDevices = getConnectedHotspotDevices();

                com.getcapacitor.JSObject result = new com.getcapacitor.JSObject();
                result.put("devices", new com.getcapacitor.JSArray(connectedDevices));
                result.put("count", connectedDevices.size());
                result.put("hotspotIp", getHotspotIpAddress());

                call.resolve(result);
            } catch (Exception e) {
                call.reject("Error discovering devices: " + e.getMessage());
            }
        }).start();
    }

    @PluginMethod
    public void checkNetworkStatus(PluginCall call) {
        boolean cellularConnected = isCellularConnected();
        boolean vpnActive = isVpnActive();
        boolean hotspotEnabled = isHotspotEnabled();
        String hotspotIp = getHotspotIpAddress();

        String status = "Cellular: " + cellularConnected +
                ", VPN: " + vpnActive +
                ", Hotspot: " + hotspotEnabled +
                ", Hotspot IP: " + hotspotIp;

        call.resolve(new com.getcapacitor.JSObject()
                .put("cellularConnected", cellularConnected)
                .put("vpnActive", vpnActive)
                .put("hotspotEnabled", hotspotEnabled)
                .put("hotspotIp", hotspotIp)
                .put("status", status)
                .put("canPrint", true) // Always allow printing attempt
                .put("canAccessApi", cellularConnected));
    }

    private boolean isCellularConnected() {
        if (connectivityManager == null) return false;

        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true;
            }
        }
        return false;
    }

    private boolean isVpnActive() {
        if (connectivityManager == null) return false;

        Network[] networks = connectivityManager.getAllNetworks();
        for (Network network : networks) {
            NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(network);
            if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                return true;
            }
        }
        return false;
    }

    private boolean isHotspotEnabled() {
        if (wifiManager == null) return false;

        try {
            // Method 1: Check using reflection (more reliable)
            if (isHotspotEnabledReflection()) {
                return true;
            }

            // Method 2: Check hotspot IP
            String hotspotIp = getHotspotIpAddress();
            return hotspotIp != null && !hotspotIp.equals("Not Available");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isHotspotEnabledReflection() {
        try {
            Method method = wifiManager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (Exception e) {
            // Fall back to other methods
            return false;
        }
    }

    private String getHotspotIpAddress() {
        try {
            // Common hotspot interface names and IP ranges
            String[] hotspotInterfaces = {"ap0", "wlan0", "wlan1", "softap0", "swlan0", "p2p0"};
            String[] hotspotRanges = {"192.168.43.", "192.168.44.", "192.168.49.", "192.168.173.", "192.168.1."};

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                String interfaceName = networkInterface.getName().toLowerCase();

                // Check if this interface might be a hotspot
                boolean isHotspotInterface = false;
                for (String hotspotIf : hotspotInterfaces) {
                    if (interfaceName.contains(hotspotIf) || interfaceName.contains("ap")) {
                        isHotspotInterface = true;
                        break;
                    }
                }

                if (isHotspotInterface) {
                    Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress address = addresses.nextElement();
                        if (!address.isLoopbackAddress() && address instanceof Inet4Address) {
                            String ip = address.getHostAddress();

                            // Check if IP is in typical hotspot ranges
                            for (String range : hotspotRanges) {
                                if (ip.startsWith(range)) {
                                    return ip;
                                }
                            }

                            // If we found an IP on a hotspot interface, return it even if not in typical range
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Not Available";
    }

    private List<String> getConnectedHotspotDevices() {
        List<String> devices = new ArrayList<>();

        try {
            // Read ARP cache to find connected devices
            BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");

                if (parts.length >= 4) {
                    String ip = parts[0];
                    String mac = parts[3];
                    String flags = parts[2];

                    // Skip invalid entries
                    if (mac.matches("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}") &&
                            !mac.equals("00:00:00:00:00:00") &&
                            !flags.equals("0x0")) {

                        // Filter by common hotspot IP ranges
                        if (ip.startsWith("192.168.43.") || ip.startsWith("192.168.44.") ||
                                ip.startsWith("192.168.49.") || ip.startsWith("192.168.173.")) {
                            devices.add(ip + " (" + mac + ")");
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return devices;
    }

    private void performHotspotPrinting(String printerIp, int printerPort, String content) {
        new Thread(() -> {
            Socket socket = null;
            try {
                socket = new Socket();

                // Connect to printer with timeout
                socket.connect(new InetSocketAddress(printerIp, printerPort), 5000);

                OutputStream outputStream = socket.getOutputStream();

                // Convert content to printer format (PCL)
                byte[] printData = convertToPCL(content);

                // Send data to printer
                outputStream.write(printData);
                outputStream.flush();

                // Wait for printer to process
                Thread.sleep(500);

                savedCall.resolve(new com.getcapacitor.JSObject()
                        .put("success", true)
                        .put("message", "Print job sent successfully to " + printerIp)
                        .put("bytesTransferred", printData.length));

            } catch (IOException e) {
                String errorMessage = "Failed to connect to printer at " + printerIp + ":" + printerPort +
                        ". Make sure:\n" +
                        "1. Printer is connected to your phone's hotspot\n" +
                        "2. Printer IP is correct\n" +
                        "3. Printer is powered on\n" +
                        "Error: " + e.getMessage();
                savedCall.reject(errorMessage);
            } catch (InterruptedException e) {
                savedCall.reject("Print operation interrupted: " + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed()) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private byte[] convertToPCL(String content) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String plainTextContent = content.replaceAll("\\<.*?\\>", "");

        try {
            // Start PCL sequence
            output.write("\u001B%-12345X".getBytes());
            output.write("@PJL ENTER LANGUAGE = PCL\r\n".getBytes());

            // Basic printer initialization commands
            output.write("\u001BE".getBytes()); // Printer reset
            output.write("\u001B&l25A".getBytes()); // Set page size to A5
            output.write("\u001B&l0O".getBytes()); // Set orientation to portrait
            output.write("\u001B&l1E".getBytes()); // Set top margin
            output.write("\u001B&a5L".getBytes()); // Set left margin
            output.write("\u001B&l8D".getBytes()); // Set line spacing to 8 lines per inch
            output.write("\u001B(s0p10h12V".getBytes()); // Set font

            // Insert plain text content
            output.write(plainTextContent.getBytes());

            // Form feed to eject the page
            output.write("\u000C".getBytes());

            // End PCL sequence
            output.write("\u001B%-12345X".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            if (savedCall != null) {
                savedCall.reject("convertToPCL: " + e.getMessage());
            }
        }

        return output.toByteArray();
    }
}