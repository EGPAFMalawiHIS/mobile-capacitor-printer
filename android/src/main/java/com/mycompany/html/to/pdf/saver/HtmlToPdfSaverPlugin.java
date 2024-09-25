package com.mycompany.html.to.pdf.saver;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

@CapacitorPlugin(
        name = "HtmlToPdfSaver",
        permissions = {
            @Permission(alias = "internet", strings = { Manifest.permission.INTERNET }),
            @Permission(alias = "storage", strings = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }),
    }
)
public class HtmlToPdfSaverPlugin extends Plugin {

    private PluginCall savedCall;
    private UsbManager usbManager;
    private PendingIntent permissionIntent;
    private static final String ACTION_USB_PERMISSION = "com.mycompany.html.to.pdf.saver.USB_PERMISSION";

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void load() {
        super.load();
        usbManager = (UsbManager) getContext().getSystemService(Context.USB_SERVICE);

        permissionIntent = PendingIntent.getBroadcast(
                getContext(),
                0,
                new Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED);
        }
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
    public void printWebPageUsingSilentPrinter(PluginCall call) {
        try {
            String content = call.getString("content");

            if (content == null) {
                call.reject("Must provide content to print");
                return;
            }

            savedCall = call;

            HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
            if (deviceList.isEmpty()) {
                call.reject("No USB devices found");
                return;
            }

            UsbDevice device = deviceList.values().iterator().next();

            if (usbManager.hasPermission(device)) {
                performPrinting(device, content);
            } else {
                usbManager.requestPermission(device, permissionIntent);
            }
        } catch (Exception e) {
            savedCall.reject(String.valueOf(e));
        }
    }

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            performPrinting(device, savedCall.getString("content"));
                        }
                    } else {
                        savedCall.reject("USB permission denied");
                    }
                }
            }
        }
    };

    private void performPrinting(UsbDevice device, String content) {
        try {
            UsbDeviceConnection connection = usbManager.openDevice(device);
            if (connection != null) {
                UsbEndpoint printEndpoint = null;
                UsbInterface printInterface = null;

                for (int i = 0; i < device.getInterfaceCount(); i++) {
                    UsbInterface usbInterface = device.getInterface(i);
                    for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                        UsbEndpoint ep = usbInterface.getEndpoint(j);
                        if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                            printInterface = usbInterface;
                            printEndpoint = ep;
                            break;
                        }
                    }
                    if (printInterface != null) break;
                }

                if (printInterface == null) {
                    savedCall.reject("Printer does not have a suitable bulk OUT endpoint.");
                    return;
                }

                connection.claimInterface(printInterface, true);

                byte[] printData = convertToPCL(content);
                int bytesTransferred = connection.bulkTransfer(printEndpoint, printData, printData.length, 5000);

                connection.releaseInterface(printInterface);
                connection.close();

                if (bytesTransferred == printData.length) {
                    savedCall.resolve();
                } else {
                    savedCall.reject("Failed to send all data to printer. Please check your connection and try again.");
                }
            } else {
                savedCall.reject("Failed to open USB connection. Please ensure the printer is properly connected and powered on.");
            }
        } catch (Exception e) {
            savedCall.reject("performPrinting: " + e.getMessage());
        }
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
            output.write("\u001B&a5L".getBytes()); // Set left margin (slightly smaller for A5)
            output.write("\u001B&l8D".getBytes()); // Set line spacing to 8 lines per inch (for better fit on A5)
            output.write("\u001B(s0p10h12V".getBytes()); // Set font (Courier, 12cpi, 10-point for better fit on A5)

            // Insert plain text content
            output.write(plainTextContent.getBytes());

            // Form feed to eject the page
            output.write("\u000C".getBytes());

            // End PCL sequence
            output.write("\u001B%-12345X".getBytes()); // UEL command to end PCL
        } catch (IOException e) {
            e.printStackTrace();
            savedCall.reject("convertToPCL: " + e.getMessage());
        }

        return output.toByteArray();
    }

}
