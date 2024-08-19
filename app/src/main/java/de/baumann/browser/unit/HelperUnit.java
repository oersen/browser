/*
    This file is part of the browser WebApp.

    browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.unit;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.graphics.drawable.Icon;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.os.Environment;
import android.os.Handler;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.ImageView;


import java.io.File;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.browser.DataURIParser;
import de.baumann.browser.browser.FilenameExtractor;
import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;

import static android.content.Context.DOWNLOAD_SERVICE;

public class HelperUnit {

    private static final int REQUEST_CODE_PERMISSION_LOC = 1234;
    private static final int REQUEST_CODE_PERMISSION_MIC = 2345;
    private static final int REQUEST_CODE_PERMISSION_CAM = 3456;
    private static SharedPreferences sp;

    public static void grantPermissionsLoc(final Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_PERMISSION_LOC);
        }
    }

    public static boolean checkPermissionsLoc(final Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public static void grantPermissionsMic(final Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION_MIC);
        }
    }

    public static boolean checkPermissionsMic(final Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public static void grantPermissionsCam(final Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION_CAM);
        }
    }

    public static boolean checkPermissionsCam(final Activity activity) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }


    public static void saveAs(AlertDialog dialogToCancel, final Activity activity, final String url, final String guessMimeType) {

        try {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            View dialogView = View.inflate(activity, R.layout.dialog_edit_extension, null);

            final EditText editTitle = dialogView.findViewById(R.id.dialog_edit_1);
            final EditText editExtension = dialogView.findViewById(R.id.dialog_edit_2);

            String filename = HelperUnit.guessFileName(url, null, guessMimeType);
            if (!filename.contains(".")) filename = filename + ".bin";  //if filename has no extension offer .bin as fall back
            editTitle.setText(filename.substring(0,filename.lastIndexOf(".")));
            String extension = filename.substring(filename.lastIndexOf("."));
            if(extension.length() <= 8) {
                editExtension.setText(extension);
            }

            builder.setView(dialogView);
            builder.setTitle(R.string.menu_save_as);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {

                String title = editTitle.getText().toString().trim();
                String extension1 = editExtension.getText().toString().trim();
                String filename1 = title + extension1;

                if (title.isEmpty() || extension1.isEmpty() || !extension1.startsWith(".")) {
                    NinjaToast.show(activity, activity.getString(R.string.toast_input_empty));
                } else {
                    if (BackupUnit.checkPermissionStorage(activity)) {
                        String download_url;
                        // remove view-source: if available
                        if (url.startsWith(BrowserUnit.URL_SCHEME_VIEW_SOURCE)) download_url = url.substring(BrowserUnit.URL_SCHEME_VIEW_SOURCE.length());
                        else download_url = url;

                        Uri source = Uri.parse(download_url);
                        DownloadManager.Request request = new DownloadManager.Request(source);
                        request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(download_url));
                        request.addRequestHeader("Accept", "text/html, application/xhtml+xml, *" + "/" + "*");
                        request.addRequestHeader("Accept-Language", "en-US,en;q=0.7,he;q=0.3");
                        request.addRequestHeader("Referer", download_url);
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename1);
                        DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                        assert dm != null;
                        dm.enqueue(request);
                        hideSoftKeyboard(editExtension, activity);
                        dialogToCancel.cancel();
                    }else {
                        BackupUnit.requestPermission(activity);
                    }
                }
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> {
                hideSoftKeyboard(editExtension, activity);
                dialogToCancel.cancel();
            });

            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void createShortcut (Context context, String title, String url) {
        try {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);
            assert shortcutManager != null;
            if (shortcutManager.isRequestPinShortcutSupported()) {
                ShortcutInfo pinShortcutInfo =
                        new ShortcutInfo.Builder(context, url)
                                .setShortLabel(title)
                                .setLongLabel(title)
                                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                                .setIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                .build();
                shortcutManager.requestPinShortcut(pinShortcutInfo, null);
            } else {
                System.out.println("failed_to_add");
            }
        } catch (Exception e) {
            System.out.println("failed_to_add");
        }
    }

    public static String guessFileName(String url, String contentDisposition, String mimeType) {
        //Alternative to URLUtil.guessFileName, using FilnameExtractor from DuckDuckGo
        //Also alternative to newer URLUtilCompat.guessFileName from WebKit 1.11

        File directory =  new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"/");

        FilenameExtractor filenameExtractor = new FilenameExtractor();
        FilenameExtractor.FilenameExtractionResult extractionResult = filenameExtractor.extract(url, contentDisposition, mimeType, directory);
        String name;
        if (extractionResult instanceof FilenameExtractor.FilenameExtractionResult.Extracted){
            name = ((FilenameExtractor.FilenameExtractionResult.Extracted) extractionResult).getFilename();
        } else {
            name = ((FilenameExtractor.FilenameExtractionResult.Guess) extractionResult).getBestGuess();
        }

        /* old code
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        String domain = Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
        return domain.replace(".", "_").trim() + "_" + currentTime.trim(); */

        return name;
    }

    public static String domain (String url) {
        if(url == null){
            return "";
        }else {
            try {
                return Objects.requireNonNull(Uri.parse(url).getHost()).replace("www.", "").trim();
            } catch (Exception e) {
                return "";
            }
        }
    }

    public static void initTheme(Context context) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        switch (Objects.requireNonNull(sp.getString("sp_theme", "1"))) {
            case "2":
                context.setTheme(R.style.AppThemeDay);
                break;
            case "3":
                context.setTheme(R.style.AppThemeNight);
                break;
            default:
                int nightModeFlags = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                switch (nightModeFlags) {
                    case Configuration.UI_MODE_NIGHT_YES:
                        context.setTheme(R.style.AppThemeNight);
                        break;
                    case Configuration.UI_MODE_NIGHT_NO:
                    case Configuration.UI_MODE_NIGHT_UNDEFINED:
                        context.setTheme(R.style.AppThemeDay);
                        break;
                }
        }
    }

    public static void addFilterItems (Activity activity, List<GridItem> gridList) {
        GridItem item_01 = new GridItem(R.drawable.bookmark_red_big, sp.getString("icon_01", activity.getResources().getString(R.string.color_red)),  11);
        GridItem item_02 = new GridItem(R.drawable.bookmark_pink_big, sp.getString("icon_02", activity.getResources().getString(R.string.color_pink)),  10);
        GridItem item_03 = new GridItem(R.drawable.bookmark_purple_big, sp.getString("icon_03", activity.getResources().getString(R.string.color_purple)),  9);
        GridItem item_04 = new GridItem(R.drawable.bookmark_blue_big, sp.getString("icon_04", activity.getResources().getString(R.string.color_blue)),  8);
        GridItem item_05 = new GridItem(R.drawable.bookmark_teal_big, sp.getString("icon_05", activity.getResources().getString(R.string.color_teal)),  7);
        GridItem item_06 = new GridItem(R.drawable.bookmark_green_big, sp.getString("icon_06", activity.getResources().getString(R.string.color_green)),  6);
        GridItem item_07 = new GridItem(R.drawable.bookmark_lime_big, sp.getString("icon_07", activity.getResources().getString(R.string.color_lime)),  5);
        GridItem item_08 = new GridItem(R.drawable.bookmark_yellow_big, sp.getString("icon_08", activity.getResources().getString(R.string.color_yellow)),  4);
        GridItem item_09 = new GridItem(R.drawable.bookmark_orange_big, sp.getString("icon_09", activity.getResources().getString(R.string.color_orange)),  3);
        GridItem item_10 = new GridItem(R.drawable.bookmark_brown_big, sp.getString("icon_10", activity.getResources().getString(R.string.color_brown)),  2);
        GridItem item_11 = new GridItem(R.drawable.bookmark_grey_big, sp.getString("icon_11", activity.getResources().getString(R.string.color_grey)),  1);

        if (sp.getBoolean("filter_01", true)){ gridList.add(gridList.size(), item_01); }
        if (sp.getBoolean("filter_02", true)){ gridList.add(gridList.size(), item_02); }
        if (sp.getBoolean("filter_03", true)){ gridList.add(gridList.size(), item_03); }
        if (sp.getBoolean("filter_04", true)){ gridList.add(gridList.size(), item_04); }
        if (sp.getBoolean("filter_05", true)){ gridList.add(gridList.size(), item_05); }
        if (sp.getBoolean("filter_06", true)){ gridList.add(gridList.size(), item_06); }
        if (sp.getBoolean("filter_07", true)){ gridList.add(gridList.size(), item_07); }
        if (sp.getBoolean("filter_08", true)){ gridList.add(gridList.size(), item_08); }
        if (sp.getBoolean("filter_09", true)){ gridList.add(gridList.size(), item_09); }
        if (sp.getBoolean("filter_10", true)){ gridList.add(gridList.size(), item_10); }
        if (sp.getBoolean("filter_11", true)){ gridList.add(gridList.size(), item_11); }
    }


    public static void setFilterIcons (ImageView ib_icon, long newIcon) {
        if (newIcon == 11) {
            ib_icon.setImageResource(R.drawable.bookmark_red_big);
        } else if (newIcon == 10) {
            ib_icon.setImageResource(R.drawable.bookmark_pink_big);
        } else if (newIcon == 9) {
            ib_icon.setImageResource(R.drawable.bookmark_purple_big);
        } else if (newIcon == 8) {
            ib_icon.setImageResource(R.drawable.bookmark_blue_big);
        } else if (newIcon == 7) {
            ib_icon.setImageResource(R.drawable.bookmark_teal_big);
        } else if (newIcon == 6) {
            ib_icon.setImageResource(R.drawable.bookmark_green_big);
        } else if (newIcon == 5) {
            ib_icon.setImageResource(R.drawable.bookmark_lime_big);
        } else if (newIcon == 4) {
            ib_icon.setImageResource(R.drawable.bookmark_yellow_big);
        } else if (newIcon == 3) {
            ib_icon.setImageResource(R.drawable.bookmark_orange_big);
        } else if (newIcon == 2) {
            ib_icon.setImageResource(R.drawable.bookmark_brown_big);
        } else if (newIcon == 1) {
            ib_icon.setImageResource(R.drawable.bookmark_grey_big);
        }
    }

    public static void saveDataURI(AlertDialog dialogToCancel, Activity activity, DataURIParser dataUriParser) {

        byte[] imagedata = dataUriParser.getImagedata();
        String filename = dataUriParser.getFilename();

        FilenameExtractor filenameExtractor = new FilenameExtractor();
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "/");
        filename = filenameExtractor.handleDuplicates(filename,directory);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
        View dialogView = View.inflate(activity, R.layout.dialog_edit_extension, null);

        final EditText editTitle = dialogView.findViewById(R.id.dialog_edit_1);
        final EditText editExtension = dialogView.findViewById(R.id.dialog_edit_2);

        editTitle.setText(filename.substring(0,filename.indexOf(".")));

        String extension = filename.substring(filename.lastIndexOf("."));
        if(extension.length() <= 8) {
            editExtension.setText(extension);
        }

        builder.setView(dialogView);
        builder.setTitle(R.string.menu_save_as);
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
            hideSoftKeyboard(dialogView,activity);
            String title = editTitle.getText().toString().trim();
            String extension1 = editExtension.getText().toString().trim();
            String filename1 = title + extension1;

            if (title.isEmpty() || extension1.isEmpty() || !extension1.startsWith(".")) {
                NinjaToast.show(activity, activity.getString(R.string.toast_input_empty));
            } else {
                if (BackupUnit.checkPermissionStorage(activity)) {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename1);
                    try {FileOutputStream fos = new FileOutputStream(file);
                        fos.write(imagedata);
                        fos.flush();
                        fos.close();
                        openDialogDownloads(activity);
                    }catch (Exception e){
                        NinjaToast.show(activity,e.toString());
                        System.out.println("Error Downloading File: " + e.toString());
                        e.printStackTrace();
                    }
                    dialogToCancel.cancel();
                }else {
                    BackupUnit.requestPermission(activity);
                }
            }
        });
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> builder.setCancelable(true));

        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
    }

    public static void showSoftKeyboard(View view, Activity context){
        assert view != null;
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            if (view.requestFocus()) {
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 50);
    }

    public static void hideSoftKeyboard(View view, Context context){
        assert view != null;
        InputMethodManager imm =(InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void openDialogDownloads(Context context) {
        ((Activity) context).runOnUiThread(() -> {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_downloadComplete);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> context.startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            Dialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        });
    }

    public static void print(Context context, NinjaWebView ninjaWebView) {
        ((Activity) context).runOnUiThread(() -> {
            String title = HelperUnit.guessFileName(ninjaWebView.getUrl(), null, null);
            PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
            PrintDocumentAdapter printAdapter = ninjaWebView.createPrintDocumentAdapter(title);
            Objects.requireNonNull(printManager).print(title, printAdapter, new PrintAttributes.Builder().build());
        });
    }

}