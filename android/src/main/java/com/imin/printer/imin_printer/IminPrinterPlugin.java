package com.imin.printer.imin_printer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;

import com.imin.library.IminSDKManager;
import com.imin.library.SystemPropManager;
import com.imin.printer.INeoPrinterCallback;
import com.imin.printer.PrinterHelper;
import com.imin.printerlib.Callback;
import com.imin.printerlib.IminPrintUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

import android.graphics.Typeface;
import android.content.Context;
import android.os.RemoteException;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.EventChannel.EventSink;
import io.flutter.plugin.common.EventChannel.StreamHandler;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;


/**
 * IminPrinterPlugin
 */
public class IminPrinterPlugin implements FlutterPlugin, MethodCallHandler, StreamHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private IminPrintUtils iminPrintUtils;
    private EventChannel eventChannel;
    private Context _context;
    private IminPrintUtils.PrintConnectType connectType = IminPrintUtils.PrintConnectType.USB;
    private EventSink eventSink;
    private static final String ACTION_PRITER_STATUS_CHANGE = "com.imin.printerservice.PRITER_STATUS_CHANGE";
    private static final String ACTION_POGOPIN_STATUS_CHANGE = "com.imin.printerservice.PRITER_CONNECT_STATUS_CHANGE";
    private static final String ACTION_PRITER_STATUS = "status";
    private static final String TAG = "IminPrinterPlugin";
    private BroadcastReceiver chargingStateChangeReceiver;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "imin_printer");
        _context = flutterPluginBinding.getApplicationContext();
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "imin_printer_event");

        if (Build.MODEL.equals("W27_Pro") || Build.MODEL.equals("I23D01") || Build.MODEL.equals("I23M01") || Build.MODEL.equals("I23M02")) {
            //初始化 2.0 的 SDK。
            PrinterHelper.getInstance().initPrinterService(Utils.getInstance().getContext());
        } else {
            //初始化 1.0 SDK
            iminPrintUtils = IminPrintUtils.getInstance(Utils.getInstance().getContext());
            String deviceModel = SystemPropManager.getModel();
            if (deviceModel.contains("M2-203") || deviceModel.contains("M2-202") || deviceModel.contains("M2-Pro")) {
                connectType = IminPrintUtils.PrintConnectType.SPI;
            } else {
                connectType = IminPrintUtils.PrintConnectType.USB;
            }
            iminPrintUtils.resetDevice();
        }
        eventChannel.setStreamHandler(this);
        channel.setMethodCallHandler(this);
    }

    @SuppressLint("NewApi")
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "sdkVersion":
                if (Build.MODEL.equals("W27_Pro") || Build.MODEL.equals("I23D01") || Build.MODEL.equals("I23M01") || Build.MODEL.equals("I23M02")) {
                    //初始化 2.0 的 SDK。
                    result.success(true);
                } else {
                    //初始化 1.0 SDK
                    result.success(false);
                }
                break;
            case "initPrinter":
                if (iminPrintUtils != null) {
                    iminPrintUtils.initPrinter(connectType);
                    result.success(true);
                } else {
                    PrinterHelper.getInstance().initPrinter(Utils.getInstance().getContext().getPackageName(), null);
                }

                break;
            case "getPrinterStatus":
                if (iminPrintUtils != null) {
                    if (connectType.equals(IminPrintUtils.PrintConnectType.SPI)) {
                        iminPrintUtils.getPrinterStatus(connectType, new Callback() {
                            @Override
                            public void callback(int status) {
                                result.success(String.format("%d", status));
                            }
                        });
                    } else {
                        int status = iminPrintUtils.getPrinterStatus(connectType);
                        result.success(String.format("%d", status));
                    }
                } else {
                    int status = PrinterHelper.getInstance().getPrinterStatus();
                    result.success(String.format("%d", status));
                }
                break;
            case "setTextSize":
                int size = call.argument("size");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setTextSize(size);
                }
                result.success(true);
                break;
            case "setTextTypeface":
                int font = call.argument("font");
                if (iminPrintUtils != null) {
                    switch (font) {
                        case 1:
                            iminPrintUtils.setTextTypeface(Typeface.MONOSPACE);
                            break;
                        case 2:
                            iminPrintUtils.setTextTypeface(Typeface.DEFAULT_BOLD);
                            break;
                        case 3:
                            iminPrintUtils.setTextTypeface(Typeface.SANS_SERIF);
                            break;
                        case 4:
                            iminPrintUtils.setTextTypeface(Typeface.SERIF);
                            break;
                        default:
                            iminPrintUtils.setTextTypeface(Typeface.DEFAULT);
                            break;
                    }
                }
                result.success(true);
                break;
            case "setTextStyle":
                int style = call.argument("style");
                if (iminPrintUtils != null) {
                    switch (style) {
                        case 1:
                            iminPrintUtils.setTextTypeface(Typeface.DEFAULT_BOLD);
                            break;
                        case 2:
                            iminPrintUtils.setTextTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
                            break;
                        case 3:
                            iminPrintUtils.setTextTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
                            break;
                        default:
                            iminPrintUtils.setTextTypeface(Typeface.DEFAULT);
                            break;
                    }
                }
                result.success(true);
                break;
            case "setTextWidth":
                int textWidth = call.argument("width");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setTextWidth(textWidth);
                }
                result.success(true);
                break;
            case "setAlignment":
                int alignment = call.argument("alignment");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setAlignment(alignment);
                }
                result.success(true);
                break;
            case "setTextLineSpacing":
                double space = call.argument("space");
                float c = ((Double) space).floatValue();
                if (iminPrintUtils != null) {
                    iminPrintUtils.setTextLineSpacing(c);
                }
                result.success(true);
                break;
            case "printColumnsText":
                String colsStr = call.argument("cols");
                try {
                    JSONArray cols = new JSONArray(colsStr);
                    String[] colsText = new String[cols.length()];
                    int[] colsWidth = new int[cols.length()];
                    int[] colsAlign = new int[cols.length()];
                    int[] colsFontSize = new int[cols.length()];
                    for (int i = 0; i < cols.length(); i++) {
                        JSONObject col = cols.getJSONObject(i);
                        String textColumn = col.getString("text");
                        int widthColumn = col.getInt("width");
                        int alignColumn = col.getInt("align");
                        int fontSizeColumn = col.getInt("fontSize");
                        colsText[i] = textColumn;
                        colsWidth[i] = widthColumn;
                        colsAlign[i] = alignColumn;
                        colsFontSize[i] = fontSizeColumn;
                    }
                    if (iminPrintUtils != null) {
                        iminPrintUtils.printColumnsText(colsText, colsWidth, colsAlign, colsFontSize);
                    } else {
                        PrinterHelper.getInstance().printColumnsText(colsText, colsWidth, colsAlign, colsFontSize, null);
                    }
                    result.success(true);
                } catch (Exception err) {
                    Log.e("IminPrinter", err.getMessage());
                }
                break;
            case "printText"://
                String text = call.argument("text");
                if (iminPrintUtils != null) {
                    iminPrintUtils.printText(text);
                } else {
                    PrinterHelper.getInstance().printTextBitmap(text, null);
                }
                result.success(true);
                break;
            case "printAntiWhiteText":
                String whiteText = call.argument("text");
                if (iminPrintUtils != null) {
                    iminPrintUtils.printAntiWhiteText(whiteText);
                }
                result.success(true);
                break;
            case "printAndLineFeed":
                if (iminPrintUtils != null) {
                    iminPrintUtils.printAndLineFeed();
                } else {
                    PrinterHelper.getInstance().printAndLineFeed();
                }
                result.success(true);
                break;
            case "printAndFeedPaper":
                int height = call.argument("height");
                if (iminPrintUtils != null) {
                    iminPrintUtils.printAndFeedPaper(height);
                } else {
                    PrinterHelper.getInstance().printAndFeedPaper(height);
                }
                result.success(true);
                break;
            case "partialCut":
                if (iminPrintUtils != null) {
                    iminPrintUtils.partialCut();
                } else {
                    PrinterHelper.getInstance().partialCut();
                }
                result.success(true);
                break;
            case "printSingleBitmap":
                try {
                    byte[] img = call.argument("bitmap");
                    Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
                    if (call.argument("alignment") != null) {
                        int align = call.argument("alignment");
                        if (iminPrintUtils != null) {
                            iminPrintUtils.printSingleBitmap(bitmap, align);
                        } else {
                            PrinterHelper.getInstance().printBitmapWithAlign(bitmap, align, null);
                        }

                    } else {
                        if (iminPrintUtils != null) {
                            iminPrintUtils.printSingleBitmap(bitmap);
                        } else {
                            PrinterHelper.getInstance().printBitmap(bitmap, null);
                        }

                    }
                    result.success(true);
                } catch (Exception err) {
                    Log.e("IminPrinter", "printSingleBitmap:" + err.getMessage());
                }
                break;
            case "printBitmapToUrl":
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (call.argument("height") != null && call.argument("width") != null) {
                                int imageWidth = call.argument("width");
                                int imageHeight = call.argument("height");
                                if (call.argument("alignment") != null) {
                                    int align = call.argument("alignment");
                                    if (call.argument("multiBitmap") != null) {
                                        ArrayList<String> multiBytes = call.argument("bitmaps");
                                        ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
                                        for (int i = 0; i < multiBytes.size(); i++) {
                                            String url = multiBytes.get(i);
                                            Bitmap image = Glide.with(_context).asBitmap().load(url).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).submit(imageWidth, imageHeight).get();
                                            bitmaps.add(image);
                                        }
                                        if (iminPrintUtils != null) {
                                            iminPrintUtils.printMultiBitmap(bitmaps, align);
                                        } else {
                                            PrinterHelper.getInstance().printMultiBitmapWithAlign(bitmaps, align, null);
                                        }
                                    } else {
                                        String img = call.argument("bitmap");
                                        Bitmap image = Glide.with(_context).asBitmap().load(img).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).submit(imageWidth, imageHeight).get();
                                        if (iminPrintUtils != null) {
                                            iminPrintUtils.printSingleBitmap(image, align);
                                        } else {
                                            if (call.argument("SingleBitmapColorChart") != null) {
                                                PrinterHelper.getInstance().printBitmapColorChartWithAlign(image, align, null);
                                            } else {
                                                PrinterHelper.getInstance().printBitmapWithAlign(image, align, null);
                                            }
                                        }
                                    }
                                } else {
                                    if (call.argument("multiBitmap") != null) {
                                        ArrayList<String> multiBytes = call.argument("bitmaps");
                                        ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
                                        for (int i = 0; i < multiBytes.size(); i++) {
                                            String url = multiBytes.get(i);
                                            Bitmap image = Glide.with(_context).asBitmap().load(url).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).submit(imageWidth, imageHeight).get();
                                            bitmaps.add(image);
                                        }
                                        if (iminPrintUtils != null) {
                                            iminPrintUtils.printMultiBitmap(bitmaps, 0);
                                        } else {
                                            PrinterHelper.getInstance().printMultiBitmap(bitmaps, null);
                                        }
                                    } else {
                                        String img = call.argument("bitmap");
                                        Bitmap bitmap = Glide.with(_context).asBitmap().load(img).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).submit(imageWidth, imageHeight).get();
                                        if (iminPrintUtils != null) {
                                            if (call.argument("blackWhite") != null) {
                                                iminPrintUtils.printSingleBitmapBlackWhite(bitmap);
                                            } else {
                                                iminPrintUtils.printSingleBitmap(bitmap);

                                            }
                                        } else {
                                            if (call.argument("SingleBitmapColorChart") != null) {
                                                PrinterHelper.getInstance().printBitmapColorChart(bitmap, null);
                                            } else {
                                                PrinterHelper.getInstance().printBitmap(bitmap, null);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception err) {
                            Log.e("IminPrinter", "printBitmapToUrl:" + err.getMessage());
                        }
                    }
                }).start();
                break;
            case "printSingleBitmapBlackWhite":
                byte[] blackWhiteBytes = call.argument("bitmap");
                try {
                    Bitmap blackWhiteBitmap = BitmapFactory.decodeByteArray(blackWhiteBytes, 0, blackWhiteBytes.length);
                    if (iminPrintUtils != null) {
                        iminPrintUtils.printSingleBitmapBlackWhite(blackWhiteBitmap);
                    }
                    result.success(true);
                } catch (Exception err) {
                    Log.e("IminPrinter", "printSingleBitmapBlackWhite:" + err.getMessage());
                }
                break;
            case "printMultiBitmap":
                ArrayList<byte[]> multiBytes = call.argument("bitmaps");
                ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>();
                try {
                    for (int i = 0; i < multiBytes.size(); i++) {
                        bitmaps.add(BitmapFactory.decodeByteArray(multiBytes.get(i), 0, multiBytes.get(i).length));
                    }
                    if (call.argument("alignment") != null) {
                        int multiAlign = call.argument("alignment");
                        if (iminPrintUtils != null) {
                            iminPrintUtils.printMultiBitmap(bitmaps, multiAlign);
                        } else {
                            PrinterHelper.getInstance().printMultiBitmapWithAlign(bitmaps, multiAlign, null);
                        }
                    } else {
                        if (iminPrintUtils != null) {
                            iminPrintUtils.printMultiBitmap(bitmaps, 0);
                        } else {
                            PrinterHelper.getInstance().printMultiBitmap(bitmaps, null);
                        }
                    }
                    result.success(true);
                } catch (Exception err) {
                    Log.e("IminPrinter", "printMultiBitmap:" + err.getMessage());
                }
                break;
            case "setQrCodeSize":
                int qrSize = call.argument("qrSize");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setQrCodeSize(qrSize);
                } else {
                    PrinterHelper.getInstance().setQrCodeSize(qrSize);
                }
                result.success(true);
                break;
            case "setLeftMargin":
                int margin = call.argument("margin");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setLeftMargin(margin);
                } else {
                    PrinterHelper.getInstance().setLeftMargin(margin);
                }
                result.success(true);
                break;
            case "setQrCodeErrorCorrectionLev":
                int level = call.argument("level");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setQrCodeErrorCorrectionLev(level);
                } else {
                    PrinterHelper.getInstance().setQrCodeErrorCorrectionLev(level);
                }
                result.success(true);
                break;
            case "printQrCode":
                String qrStr = call.argument("data");
                if (call.argument("alignment") != null) {
                    int alignmentMode = call.argument("alignment");
                    if (iminPrintUtils != null) {
                        iminPrintUtils.printQrCode(qrStr, alignmentMode);
                    } else {
                        if (call.argument("qrSize") != null && call.argument("level") != null) {
                            int qrFullSize = call.argument("qrSize");
                            int qrFullLevel = call.argument("level");
                            PrinterHelper.getInstance().printQRCodeWithFull(qrStr, qrFullSize, qrFullLevel, alignmentMode, null);
                        } else {
                            PrinterHelper.getInstance().printQrCodeWithAlign(qrStr, alignmentMode, null);
                        }
                    }
                } else {
                    if (iminPrintUtils != null) {
                        iminPrintUtils.printQrCode(qrStr);
                    } else {
                        PrinterHelper.getInstance().printQrCode(qrStr, null);
                    }
                }
                result.success(true);
                break;
            case "setBarCodeWidth":
                int barCodeWidth = call.argument("width");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setBarCodeWidth(barCodeWidth);
                } else {
                    PrinterHelper.getInstance().setBarCodeWidth(barCodeWidth);
                }
                result.success(true);
                break;
            case "setBarCodeHeight":
                int barCodeHeight = call.argument("height");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setBarCodeHeight(barCodeHeight);
                } else {
                    PrinterHelper.getInstance().setBarCodeHeight(barCodeHeight);
                }
                result.success(true);
                break;
            case "setBarCodeContentPrintPos":
                int barCodePosition = call.argument("position");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setBarCodeContentPrintPos(barCodePosition);
                } else {
                    PrinterHelper.getInstance().setBarCodeContentPrintPos(barCodePosition);
                }
                result.success(true);
                break;
            case "setPageFormat":
                int formatStyle = call.argument("style");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setPageFormat(formatStyle);
                }
                result.success(true);
                break;
            case "printBarCode":
                try {
                    String barCodeContent = call.argument("data");
                    int barCodeType = call.argument("type");
                    if (call.argument("align") != null) {
                        int barCodeAlign = call.argument("align");
                        if (iminPrintUtils != null) {
                            iminPrintUtils.printBarCode(barCodeType, barCodeContent, barCodeAlign);
                        } else {
                            if (call.argument("position") != null && call.argument("height") != null && call.argument("width") != null) {
                                int barCodeFullPosition = call.argument("position");
                                int barCodeFullHeight = call.argument("height");
                                int barCodeFullWidth = call.argument("width");
                                PrinterHelper.getInstance().printBarCodeWithFull(barCodeContent, barCodeType, barCodeFullWidth, barCodeFullHeight, barCodeAlign, barCodeFullPosition, null);
                            } else {
                                PrinterHelper.getInstance().printBarCodeWithAlign(barCodeContent, barCodeType, barCodeAlign, null);
                            }
                        }
                    } else {
                        if (iminPrintUtils != null) {
                            iminPrintUtils.printBarCode(barCodeType, barCodeContent);
                        } else {
                            Log.d("IminPrinter:printBarCode", "barCodeType:" + barCodeType);
                            Log.d("IminPrinter:printBarCode", "barCodeContent:" + barCodeContent);
                            PrinterHelper.getInstance().printBarCode(barCodeContent, barCodeType, null);
                        }
                    }
                    result.success(true);
                } catch (Exception e) {
                    Log.e("IminPrinter", e.getMessage());
                }
                break;
            case "printBarCodeToBitmapFormat":
                try {
                    // String barCodeFormatContent = call.argument("data");
                    // int barCodeFormatWidth = call.argument("width");
                    // int barCodeFormatHeight = call.argument("height");
                    // int barCodeFormat = call.argument("codeFormat");
                    // switch (barCodeFormat) {
                    //     case 0:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.AZTEC);
                    //         break;
                    //     case 1:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.CODABAR);
                    //         break;
                    //     case 2:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.CODE_39);
                    //         break;
                    //     case 3:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.CODE_93);
                    //         break;
                    //     case 4:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.CODE_128);
                    //         break;
                    //     case 5:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.DATA_MATRIX);
                    //         break;
                    //     case 6:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.EAN_13);
                    //         break;
                    //     case 7:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.ITF);
                    //         break;
                    //     case 8:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.PDF_417);
                    //         break;
                    //     case 9:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.MAXICODE);
                    //         break;
                    //     case 10:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.QR_CODE);
                    //         break;
                    //     case 11:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.RSS_14);
                    //         break;
                    //     case 12:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.RSS_EXPANDED);
                    //         break;
                    //     case 13:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.UPC_A);
                    //         break;
                    //     case 14:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.UPC_E);
                    //         break;
                    //     case 15:
                    //         iminPrintUtils.printBarCodeToBitmapFormat(barCodeFormatContent, barCodeFormatWidth, barCodeFormatHeight, CodeFormat.UPC_EAN_EXTENSION);
                    //         break;
                    // }
                    result.success(true);
                } catch (Exception e) {
                    Log.e("IminPrinter", e.getMessage());
                }
                break;
            case "setDoubleQRSize":
                int doubleQRSize = call.argument("size");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setDoubleQRSize(doubleQRSize);
                } else {
                    PrinterHelper.getInstance().setDoubleQRSize(doubleQRSize);
                }
                result.success(true);
                break;
            case "setDoubleQR1Level":
                int doubleQR1Level = call.argument("level");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setDoubleQR1Level(doubleQR1Level);
                } else {
                    PrinterHelper.getInstance().setDoubleQR1Level(doubleQR1Level);
                }
                result.success(true);
                break;
            case "setDoubleQR2Level":
                int doubleQR2Level = call.argument("level");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setDoubleQR2Level(doubleQR2Level);
                } else {
                    PrinterHelper.getInstance().setDoubleQR2Level(doubleQR2Level);
                }
                result.success(true);
                break;
            case "setDoubleQR1MarginLeft":
                int doubleQR1MarginLeft = call.argument("leftMargin");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setDoubleQR1MarginLeft(doubleQR1MarginLeft);
                } else {
                    PrinterHelper.getInstance().setDoubleQR1MarginLeft(doubleQR1MarginLeft);
                }
                result.success(true);
                break;
            case "setDoubleQR2MarginLeft":
                int doubleQR2MarginLeft = call.argument("leftMargin");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setDoubleQR2MarginLeft(doubleQR2MarginLeft);
                } else {
                    PrinterHelper.getInstance().setDoubleQR2MarginLeft(doubleQR2MarginLeft);
                }
                result.success(true);
                break;
            case "setDoubleQR1Version":
                int doubleQR1Version = call.argument("version");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setDoubleQR1Version(doubleQR1Version);
                }
                break;
            case "setDoubleQR2Version":
                int doubleQR2Version = call.argument("version");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setDoubleQR2Version(doubleQR2Version);
                }
                break;
            case "printDoubleQR":
                String qrCode1Text = call.argument("qrCode1Text");
                String qrCode2Text = call.argument("qrCode2Text");
                if (iminPrintUtils != null) {
                    iminPrintUtils.printDoubleQR(qrCode1Text, qrCode2Text);
                }
                result.success(true);
                break;
            case "openCashBox":
                if (iminPrintUtils != null) {
                    IminSDKManager.opencashBox();
                } else {
                    PrinterHelper.getInstance().openDrawer();
                }
                result.success(true);
                break;
            case "setInitIminPrinter":
                boolean isDefault = call.argument("isDefault");
                if (iminPrintUtils != null) {
                    iminPrintUtils.setInitIminPrinter(isDefault);
                }
                result.success(true);
                break;
            case "resetDevice":
                if (iminPrintUtils != null) {
                    iminPrintUtils.resetDevice();
                }
                result.success(true);
                break;
            case "initPrinterParams":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().initPrinterParams();
                }
                result.success(true);
                break;
            case "unBindService":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().deInitPrinterService(Utils.getInstance().getContext());
                }
                result.success(true);
                break;
            case "getPrinterSerialNumber":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().getPrinterSerialNumber(new INeoPrinterCallback() {
                        @Override
                        public void onRunResult(boolean isSuccess) throws RemoteException {
                            result.success(isSuccess);//"true 绑定服务成功" : "false 绑定服务失败"
                        }

                        @Override
                        public void onReturnString(String s) throws RemoteException {
                            result.success(s);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) throws RemoteException {

                        }

                        @Override
                        public void onPrintResult(int code, String msg) throws RemoteException {

                        }
                    });
                }
                break;
            case "getPrinterModelName":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().getPrinterModelName(new INeoPrinterCallback() {
                        @Override
                        public void onRunResult(boolean isSuccess) throws RemoteException {
                            result.success(isSuccess);//"true 绑定服务成功" : "false 绑定服务失败"
                        }

                        @Override
                        public void onReturnString(String s) throws RemoteException {
                            result.success(s);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) throws RemoteException {

                        }

                        @Override
                        public void onPrintResult(int code, String msg) throws RemoteException {

                        }
                    });
                }
                break;
            case "getPrinterThermalHead":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().getPrinterThermalHead(new INeoPrinterCallback() {
                        @Override
                        public void onRunResult(boolean isSuccess) throws RemoteException {
                            result.success(isSuccess);//"true 绑定服务成功" : "false 绑定服务失败"
                        }

                        @Override
                        public void onReturnString(String s) throws RemoteException {
                            result.success(s);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) throws RemoteException {

                        }

                        @Override
                        public void onPrintResult(int code, String msg) throws RemoteException {

                        }
                    });
                }
                break;
            case "getPrinterFirmwareVersion":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().getPrinterFirmwareVersion(new INeoPrinterCallback() {
                        @Override
                        public void onRunResult(boolean isSuccess) throws RemoteException {
                            result.success(isSuccess);//"true 绑定服务成功" : "false 绑定服务失败"
                        }

                        @Override
                        public void onReturnString(String s) throws RemoteException {
                            result.success(s);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) throws RemoteException {

                        }

                        @Override
                        public void onPrintResult(int code, String msg) throws RemoteException {

                        }
                    });
                }
                break;
            case "getServiceVersion":
                if (iminPrintUtils == null) {
                    result.success(PrinterHelper.getInstance().getServiceVersion());
                }
                break;
            case "getPrinterHardwareVersion":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().getPrinterHardwareVersion(new INeoPrinterCallback() {
                        @Override
                        public void onRunResult(boolean isSuccess) throws RemoteException {
                            result.success(isSuccess);//"true 绑定服务成功" : "false 绑定服务失败"
                        }

                        @Override
                        public void onReturnString(String s) throws RemoteException {
                            result.success(s);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) throws RemoteException {

                        }

                        @Override
                        public void onPrintResult(int code, String msg) throws RemoteException {

                        }
                    });
                }
                break;
            case "getUsbPrinterVidPid":
                if (iminPrintUtils == null) {
                    result.success(PrinterHelper.getInstance().getUsbPrinterVidPid());
                }
                break;
            case "getUsbDevicesName":
                if (iminPrintUtils == null) {
                    result.success(PrinterHelper.getInstance().getUsbDevicesName());
                }
                break;
            case "getPrinterDensity":
                if (iminPrintUtils == null) {
                    result.success(PrinterHelper.getInstance().getPrinterDensity());
                }
                break;
            case "getPrinterPaperDistance":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().getPrinterPaperDistance(new INeoPrinterCallback() {
                        @Override
                        public void onRunResult(boolean isSuccess) throws RemoteException {
                            result.success(isSuccess);//"true 绑定服务成功" : "false 绑定服务失败"
                        }

                        @Override
                        public void onReturnString(String s) throws RemoteException {
                            result.success(s);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) throws RemoteException {

                        }

                        @Override
                        public void onPrintResult(int code, String msg) throws RemoteException {

                        }
                    });
                }
                break;
            case "getPrinterPaperType":
                if (iminPrintUtils == null) {
                    result.success(PrinterHelper.getInstance().getPrinterPaperType());
                }
                break;
            case "getPrinterCutTimes":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().getPrinterCutTimes(new INeoPrinterCallback() {
                        @Override
                        public void onRunResult(boolean isSuccess) throws RemoteException {
                            result.success(isSuccess);//"true 绑定服务成功" : "false 绑定服务失败"
                        }

                        @Override
                        public void onReturnString(String s) throws RemoteException {
                            result.success(s);
                        }

                        @Override
                        public void onRaiseException(int code, String msg) throws RemoteException {

                        }

                        @Override
                        public void onPrintResult(int code, String msg) throws RemoteException {

                        }
                    });
                }
                break;
            case "getPrinterMode":
                if (iminPrintUtils == null) {
                    result.success(PrinterHelper.getInstance().getPrinterMode());
                }
                break;
            case "getDrawerStatus":
                if (iminPrintUtils == null) {
                    result.success(PrinterHelper.getInstance().getDrawerStatus());
                }
                break;
            case "getOpenDrawerTimes":
                if (iminPrintUtils == null) {
                    result.success(PrinterHelper.getInstance().getOpenDrawerTimes());
                }
                break;
            case "printerSelfChecking":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().printerSelfChecking(null);
                }
                break;
            case "sendRAWData":
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().sendRAWData((byte[]) call.argument("bytes"), null);
                }
                result.success(true);
                break;
            case "setCodeAlignment":
                if (iminPrintUtils == null) {
                    int align = call.argument("align");
                    PrinterHelper.getInstance().setCodeAlignment(align);
                }
                result.success(true);
                break;
            case "setTextBitmapTypeface":
                if (iminPrintUtils == null) {
                    int textBitmapFont = call.argument("font");
                    switch (textBitmapFont) {
                        case 1:
                            PrinterHelper.getInstance().setTextBitmapTypeface("Typeface.MONOSPACE");
                            break;
                        case 2:
                            PrinterHelper.getInstance().setTextBitmapTypeface("Typeface.DEFAULT_BOLD");
                            break;
                        case 3:
                            PrinterHelper.getInstance().setTextBitmapTypeface("Typeface.SANS_SERIF");
                            break;
                        case 4:
                            PrinterHelper.getInstance().setTextBitmapTypeface("Typeface.SERIF");
                            break;
                        default:
                            PrinterHelper.getInstance().setTextBitmapTypeface("Typeface.DEFAULT");
                            break;
                    }
                }
                result.success(true);
                break;
            case "setTextBitmapSize":
                if (iminPrintUtils == null) {
                    int textBitmapSize = call.argument("size");
                    PrinterHelper.getInstance().setTextBitmapSize(textBitmapSize);
                }
                result.success(true);
                break;
            case "setTextBitmapStyle":
                if (iminPrintUtils == null) {
                    int textBitmapStyle = call.argument("style");
                    if (iminPrintUtils != null) {
                        PrinterHelper.getInstance().setTextBitmapStyle(textBitmapStyle);
                    }
                }
                result.success(true);
                break;
            case "setTextBitmapStrikeThru":
                if (iminPrintUtils == null) {
                    boolean strikeThru = call.argument("strikeThru");
                    PrinterHelper.getInstance().setTextBitmapStrikeThru(strikeThru);
                }
                result.success(true);
                break;
            case "setTextBitmapUnderline":
                if (iminPrintUtils == null) {
                    boolean haveUnderline = call.argument("haveUnderline");
                    PrinterHelper.getInstance().setTextBitmapUnderline(haveUnderline);
                }
                result.success(true);
                break;
            case "setTextBitmapLineSpacing":
                if (iminPrintUtils == null) {
                    double lineHeight = call.argument("lineHeight");
                    float s = ((Double) lineHeight).floatValue();
                    PrinterHelper.getInstance().setTextBitmapLineSpacing(s);
                }
                result.success(true);
                break;
            case "setTextBitmapLetterSpacing":
                if (iminPrintUtils == null) {
                    double letterSpacing = call.argument("letterSpacing");
                    float a = ((Double) letterSpacing).floatValue();
                    PrinterHelper.getInstance().setTextBitmapLetterSpacing(a);
                }
                result.success(true);
                break;
            case "setTextBitmapAntiWhite":
                if (iminPrintUtils == null) {
                    boolean antiWhite = call.argument("antiWhite");
                    PrinterHelper.getInstance().setTextBitmapAntiWhite(antiWhite);
                }
                result.success(true);
                break;
            case "printTextBitmapWithAli":
                if (iminPrintUtils == null) {
                    String textBitmapString = call.argument("text");
                    int textBitmapAlign = call.argument("align");
                    PrinterHelper.getInstance().printTextBitmapWithAli(textBitmapString, textBitmapAlign, null);
                }
                result.success(true);
                break;
            case "printBitmapColorChart":
                try {
                    byte[] img = call.argument("bitmap");
                    Bitmap bitmap = BitmapFactory.decodeByteArray(img, 0, img.length);
                    if (call.argument("alignment") != null) {
                        int align = call.argument("alignment");
                        if (iminPrintUtils == null) {
                            PrinterHelper.getInstance().printBitmapColorChartWithAlign(bitmap, align, null);
                        }

                    } else {
                        if (iminPrintUtils == null) {
                            PrinterHelper.getInstance().printBitmapColorChart(bitmap, null);
                        }
                    }
                    result.success(true);
                } catch (Exception err) {
                    Log.e("IminPrinter", "printBitmapColorChart:" + err.getMessage());
                }
                result.success(true);
                break;
            case "printColumnsString":
                String colsString = call.argument("cols");
                try {
                    JSONArray cols = new JSONArray(colsString);
                    String[] colsText = new String[cols.length()];
                    int[] colsWidth = new int[cols.length()];
                    int[] colsAlign = new int[cols.length()];
                    int[] colsFontSize = new int[cols.length()];
                    for (int i = 0; i < cols.length(); i++) {
                        JSONObject col = cols.getJSONObject(i);
                        String textColumn = col.getString("text");
                        int widthColumn = col.getInt("width");
                        int alignColumn = col.getInt("align");
                        int fontSizeColumn = col.getInt("fontSize");
                        colsText[i] = textColumn;
                        colsWidth[i] = widthColumn;
                        colsAlign[i] = alignColumn;
                        colsFontSize[i] = fontSizeColumn;
                    }
                    if (iminPrintUtils == null) {
                        PrinterHelper.getInstance().printColumnsString(colsText, colsWidth, colsAlign, colsFontSize, null);
                    }
                    result.success(true);
                } catch (Exception err) {
                    Log.e("IminPrinter", err.getMessage());
                }
                result.success(true);
                break;
            case "getPrinterIsUpdateStatus":
                if (iminPrintUtils == null) {
                     result.success(PrinterHelper.getInstance().getPrinterIsUpdateStatus());
                }
                result.success(true);
                break;

            case "enterPrinterBuffer"://进入事务模式
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().enterPrinterBuffer(true);
                }
                result.success(true);
                break;

            case "commitPrinterBuffer"://提交事务打印
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().commitPrinterBuffer(new INeoPrinterCallback() {
                        @Override
                        public void onRunResult(boolean isSuccess) throws RemoteException {

                        }

                        @Override
                        public void onReturnString(String result) throws RemoteException {

                        }

                        @Override
                        public void onRaiseException(int code, String msg) throws RemoteException {

                        }

                        @Override
                        public void onPrintResult(int code, String msg) throws RemoteException {
                            // code 0=事务打印成功， 2=事务模式开始发打印数据给打印机 ， 其它=打印失败
                            Log.d(TAG, "code  " + code + "  , msg= " + msg);
                        }
                    });
                }
                result.success(true);
                break;

            case "exitPrinterBuffer"://exit事务模式
                if (iminPrintUtils == null) {
                    PrinterHelper.getInstance().exitPrinterBuffer(true);
                }
                result.success(true);
                break;

            default:
                result.notImplemented();
                break;
        }
    }

    private BroadcastReceiver createChargingStateChangeReceiver(EventChannel.EventSink events) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int status = intent.getIntExtra(ACTION_PRITER_STATUS, -1);
                events.success(status);
            }
        };
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onListen(Object argument, EventChannel.EventSink events) {
        eventSink = events;
        chargingStateChangeReceiver = createChargingStateChangeReceiver(eventSink);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_PRITER_STATUS_CHANGE);
        intentFilter.addAction(ACTION_POGOPIN_STATUS_CHANGE);
        _context.registerReceiver(chargingStateChangeReceiver, intentFilter);
    }

    @Override
    public void onCancel(Object argument) {
        _context.unregisterReceiver(chargingStateChangeReceiver);
        eventSink = null;
        chargingStateChangeReceiver = null;
    }

}
