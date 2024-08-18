/*      Copyright (C) 2023 woheller69

        This program is free software: you can redistribute it and/or modify
        it under the terms of the GNU General Public License as published by
        the Free Software Foundation, either version 3 of the License, or
        (at your option) any later version.

        This program is distributed in the hope that it will be useful,
        but WITHOUT ANY WARRANTY; without even the implied warranty of
        MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
        GNU General Public License for more details.

        You should have received a copy of the GNU General Public License
        along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.baumann.browser.browser;

import android.app.Activity;
import android.content.Context;

import android.content.SharedPreferences;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;

import de.baumann.browser.unit.BrowserUnit;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class BannerBlock {
    private static final String FILE = "banners.txt";
    private static String configString = "";


    private static void loadHosts(final Context context) {
        try {
            File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/"+FILE);

            String jsonDataString =  new String(Files.readAllBytes(Paths.get(file.getPath())));
            JSONObject jsonData = new JSONObject(jsonDataString);
            JSONArray data = jsonData.getJSONArray("data");
            configString = data.toString().replaceAll("\\\\\"", "\\\\\\\\\"");
        } catch (IOException | JSONException i ) {
            Log.w("browser", "Error loading cookie banner rules", i);
            ((Activity) context).runOnUiThread(() -> {
                Toast.makeText(context, "Error loading cookie banner rules", Toast.LENGTH_LONG).show();
            });
        }
    }

    public static void downloadBanners(final Context context) {
        Thread thread = new Thread(() -> {

            String hostURL = "https://raw.githubusercontent.com/mozilla/cookie-banner-rules-list/main/cookie-banner-rules-list.json";

            try {
                URL url = new URL(hostURL);
                Log.d("browser","Download Mozilla cookie banner rules");

                SpannableStringBuilder biggerText = new SpannableStringBuilder("\u27f3 " + "cookie-banner-rules-list.json");
                biggerText.setSpan(new RelativeSizeSpan(1.35f), 0, 1, 0);
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, biggerText, Toast.LENGTH_LONG).show();
                });
                URLConnection ucon = url.openConnection();
                ucon.setReadTimeout(5000);
                ucon.setConnectTimeout(10000);

                InputStream is = ucon.getInputStream();
                BufferedInputStream inStream = new BufferedInputStream(is, 1024 * 5);

                File bannerFile = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/" +FILE);

                if (bannerFile.exists())
                {
                    bannerFile.delete();
                }
                bannerFile.createNewFile();

                FileOutputStream outStream = new FileOutputStream(bannerFile);
                byte[] buff = new byte[5 * 1024];

                int len;
                while ((len = inStream.read(buff)) != -1)
                {
                    outStream.write(buff, 0, len);
                }

                outStream.flush();
                outStream.close();
                inStream.close();

                loadHosts(context);  //reload hosts after update
                Log.w("browser", "Mozilla cookie banner rules updated");

            } catch (IOException i) {
                Log.w("browser", "Error updating Mozilla cookie banner rules", i);
                ((Activity) context).runOnUiThread(() -> {
                    Toast.makeText(context, "Error updating Mozilla cookie banner rules", Toast.LENGTH_LONG).show();
                });
            }
        });
        thread.start();
    }

    public BannerBlock(Context context) {
        File file = new File(context.getDir("filesdir", Context.MODE_PRIVATE) + "/"+FILE);
        if (!file.exists()) {
            downloadBanners(context);
        } else {
            Calendar time = Calendar.getInstance();

            if (BrowserUnit.isUnmeteredConnection(context))
                time.add(Calendar.DAY_OF_YEAR,-3);
            else
                time.add(Calendar.DAY_OF_YEAR,-7);

            Date lastModified = new Date(file.lastModified());
            if (lastModified.before(time.getTime())) {  //also download again if something is wrong with the file
                //update if file is older than 7 days and Feature switched on
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                if (sp.getBoolean("sp_deny_cookie_banners",false)) downloadBanners(context);
            }
            loadHosts(context);
        }
    }

    public static String getBannerBlockScriptPageStarted() {
        if (configString.equals("")) return null;
        else {
            String bannerBlockScript =
                    "        var bbs_configString = '" + configString + "';\n" +
                    "        var bbs_config = JSON.parse(bbs_configString);\n" +
                    "        var currentDomain = window.location.hostname;\n" +
                    "        function isSubdomain(subdomain, domain) {\n" +
                    "            return subdomain.endsWith(\".\" + domain) || subdomain === domain;\n" +
                    "        }\n" +
                    "        for (let i = 0; i < bbs_config.length; i++) {\n" +
                    "            var item = bbs_config[i];\n" +
                    "            // Check if the current domain is in the list of specified domains or a subdomain \n" +
                    "            if (item.domains.some(domain => isSubdomain(currentDomain, domain))) {\n" +
                    "                  if (item.cookies) {\n"+
                    "                      var optOutCookies = item.cookies.optOut;\n"+
                    "                      if (optOutCookies && Array.isArray(optOutCookies) && optOutCookies.length > 0) {\n"+
                    "                          for (var k = 0; k < optOutCookies.length; k++) {\n"+
                    "                              var cookie = optOutCookies[k];\n"+
                    "                              document.cookie = cookie.name + \"=\" + cookie.value + \"; path=/; domain=\" + currentDomain;\n"+
                    "                          }\n"+
                    "                      }\n"+
                    "                  }\n"+
                    "            }\n" +
                    "        }";
            return bannerBlockScript;
        }
    }

    public static String getBannerBlockScriptPageFinished() {
        String exceptionIDs = "[\"borlabs\"]";  //Exceptions where we need to check if the button has an area before clicking to avoid endless loop
        if (configString.equals("")) return null;
        else {
            String bannerBlockScript =
                    "        var bbf_configString = '" + configString + "';\n" +
                    "        var bbf_config = JSON.parse(bbf_configString);\n" +
                    "        var exceptionIds = '" + exceptionIDs + "';\n" +
                    "        var currentDomain = window.location.hostname;\n" +
                    "        // isSubdomain is used to check if 'subdomain' is a subdomain of 'domain'\n" +
                    "        function isSubdomain(subdomain, domain) {\n" +
                    "            return subdomain.endsWith(\".\" + domain) || subdomain === domain;\n" +
                    "        }\n" +
                    "        // createOptOutHandler is used to create a closure that captures the current value of item for each iteration\n" +
                    "        function createOptOutHandler(item) {\n" +
                    "           return function() {\n" +
                    "               var optOutElements = document.querySelectorAll(item.click.optOut);\n" +
                    "               for (let j = 0; j < optOutElements.length; j++) {\n" +
                    "                   optOutElements[j].click();\n" +
                    "               }\n" +
                    "           };\n" +
                    "        }\n" +
                    "        function createOptOutHandlerException(item) {\n" +
                    "           return function() {\n" +
                    "               var optOutElements = document.querySelectorAll(item.click.optOut);\n" +
                    "               for (let j = 0; j < optOutElements.length; j++) {\n" +
                    "                   var rect = optOutElements[j].getBoundingClientRect();\n" +
                    "                   // Check if the button has an area > 0 \n" +
                    "                   if (rect.width * rect.height > 0 ){\n"+
                    "                       optOutElements[j].click();\n" +
                    "                   }\n" +
                    "               }\n" +
                    "           };\n" +
                    "        }\n" +
                    "        //MAIN LOOP\n" +
                    "        for (let i = 0; i < bbf_config.length; i++) {\n" +
                    "            var item = bbf_config[i];\n" +
                    "            // Check if the current domain is in the list of specified domains or a subdomain \n" +
                    "            if (item.domains.length === 0 || item.domains.some(domain => isSubdomain(currentDomain, domain))) {\n" +
                    "               // If there are clickable items, proceed with the presence check\n" +
                    "               if (item.click) {\n" +
                    "                   var presenceElements = document.querySelectorAll(item.click.presence);\n" +
                    "                   if (presenceElements.length > 0) {\n" +
                    "                       console.log(\"CookieBannerRuleID:\"+item.id);\n" +
                    "                       if (exceptionIds.includes(item.id)) {\n" +
                    "                           // Introduce a short delay before clicking the opt-out button, but only click if button has an area\n" +
                    "                           setTimeout(createOptOutHandlerException(item), 250);\n" +
                    "                       } else {\n" +
                    "                           // Introduce a short delay before clicking the opt-out button\n" +
                    "                           setTimeout(createOptOutHandler(item), 250);\n" +
                    "                       }\n" +
                    "                   }\n" +
                    "               }\n" +
                    "            }\n" +
                    "        }";
            return bannerBlockScript;
        }
    }

}
