/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 pwlin - pwlin05@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.pdl.cordova.fileopenerpdl;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class FileOpenerPDL extends CordovaPlugin {

    /**
     * Executes the request and returns a boolean.
     *
     * @param action          The action to execute.
     * @param args            JSONArray of arguments for the plugin.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return boolean.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)
            throws JSONException {
        if (action.equals("open")) {
            String fileUrl = args.getString(0);
            String contentType = args.getString(1);
            Boolean openWithDefault = true;
            if (args.length() > 2) {
                openWithDefault = args.getBoolean(2);
            }
            this._open(fileUrl, contentType, openWithDefault, callbackContext);
        } else if (action.equals("appIsInstalled")) {
            JSONObject successObj = new JSONObject();
            if (this._appIsInstalled(args.getString(0))) {
                successObj.put("status", PluginResult.Status.OK.ordinal());
                successObj.put("message", "Installed");
            } else {
                successObj.put("status", PluginResult.Status.NO_RESULT.ordinal());
                successObj.put("message", "Not installed");
            }
            callbackContext.success(successObj);
        } else {
            JSONObject errorObj = new JSONObject();
            errorObj.put("status", PluginResult.Status.INVALID_ACTION.ordinal());
            errorObj.put("message", "Invalid action");
            callbackContext.error(errorObj);
        }
        return true;
    }

    private void _open(String fileArg, String contentType, Boolean openWithDefault,
                       CallbackContext callbackContext) throws JSONException {
        String fileName = "";
        try {
            CordovaResourceApi resourceApi = webView.getResourceApi();
            Uri fileUri = resourceApi.remapUri(Uri.parse(fileArg));
            fileName = fileUri.getPath();
        } catch (Exception e) {
            fileName = fileArg;
        }
        File file = new File(fileName);
        if (file.exists()) {
            try {
                if (contentType == null || contentType.trim().equals("")) {
                    contentType = _getMimeType(fileName);
                }

                Intent intent = new Intent(Intent.ACTION_VIEW);
                Context context = cordova.getActivity().getApplicationContext();
                Uri path = FileProvider.getUriForFile(context,
                        cordova.getActivity().getPackageName() + ".fileOpener2.provider", file);
                intent.setDataAndType(path, contentType);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                if (openWithDefault) {
                    cordova.getActivity().startActivity(intent);
                } else {
                    cordova.getActivity()
                            .startActivity(Intent.createChooser(intent, "Open File in..."));
                }

                callbackContext.success();
            } catch (android.content.ActivityNotFoundException e) {
                JSONObject errorObj = new JSONObject();
                errorObj.put("status", PluginResult.Status.ERROR.ordinal());
                errorObj.put("message", "Activity not found: " + e.getMessage());
                callbackContext.error(errorObj);
            }
        } else {
            JSONObject errorObj = new JSONObject();
            errorObj.put("status", PluginResult.Status.ERROR.ordinal());
            errorObj.put("message", "File not found");
            callbackContext.error(errorObj);
        }
    }

    private String _getMimeType(String url) {
        String mimeType = "*/*";
        int extensionIndex = url.lastIndexOf('.');
        if (extensionIndex > 0) {
            String extMimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(url.substring(extensionIndex + 1));
            if (extMimeType != null) {
                mimeType = extMimeType;
            }
        }
        return mimeType;
    }

    private boolean _appIsInstalled(String packageId) {
        PackageManager pm = cordova.getActivity().getPackageManager();
        boolean appInstalled = false;
        try {
            pm.getPackageInfo(packageId, PackageManager.GET_ACTIVITIES);
            appInstalled = true;
        } catch (PackageManager.NameNotFoundException e) {
            appInstalled = false;
        }
        return appInstalled;
    }
}