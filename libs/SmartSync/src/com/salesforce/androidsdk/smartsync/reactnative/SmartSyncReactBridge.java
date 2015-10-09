/*
 * Copyright (c) 2015, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.androidsdk.smartsync.reactnative;

import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.salesforce.androidsdk.reactnative.ReactBridgeHelper;
import com.salesforce.androidsdk.smartstore.app.SalesforceSDKManagerWithSmartStore;
import com.salesforce.androidsdk.smartsync.manager.SyncManager;
import com.salesforce.androidsdk.smartsync.util.SyncDownTarget;
import com.salesforce.androidsdk.smartsync.util.SyncOptions;
import com.salesforce.androidsdk.smartsync.util.SyncState;
import com.salesforce.androidsdk.smartsync.util.SyncUpTarget;

import org.json.JSONException;
import org.json.JSONObject;

public class SmartSyncReactBridge extends ReactContextBaseJavaModule {

    // Keys in json from/to javascript
    static final String TARGET = "target";
    static final String SOUP_NAME = "soupName";
    static final String OPTIONS = "options";
    static final String SYNC_ID = "syncId";
    static final String IS_GLOBAL_STORE = "isGlobalStore";

    public SmartSyncReactBridge(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "SmartSyncReactBridge";
    }

    /**
     * Native implementation of syncUp
     * @param args
     * @param successCallback
     * @param errorCallback
     */
    @ReactMethod
    public void syncUp(ReadableMap args,
                       final Callback successCallback, final Callback errorCallback) {
        // Parse args
        JSONObject target = new JSONObject(ReactBridgeHelper.toJavaMap(args.getMap(TARGET)));
        String soupName = args.getString(SOUP_NAME);
        JSONObject options = new JSONObject(ReactBridgeHelper.toJavaMap(args.getMap(OPTIONS)));
        final boolean isGlobal = args.getBoolean(IS_GLOBAL_STORE);

        SyncManager syncManager = getSyncManager(isGlobal);
        try {
            syncManager.syncUp(SyncUpTarget.fromJSON(target), SyncOptions.fromJSON(options), soupName, new SyncManager.SyncUpdateCallback() {
                @Override
                public void onUpdate(SyncState sync) {
                    handleSyncUpdate(sync, successCallback);
                }
            });
        } catch (JSONException e) {
            errorCallback.invoke(e.toString());
        }
    }

    /**
     * Native implementation of syncDown
     * @param args
     * @param successCallback
     * @param errorCallback
     */
    public void syncDown(ReadableMap args,
                         final Callback successCallback, final Callback errorCallback) {
        // Parse args
        JSONObject target = new JSONObject(ReactBridgeHelper.toJavaMap(args.getMap(TARGET)));
        String soupName = args.getString(SOUP_NAME);
        JSONObject options = new JSONObject(ReactBridgeHelper.toJavaMap(args.getMap(OPTIONS)));
        final boolean isGlobal = args.getBoolean(IS_GLOBAL_STORE);

        SyncManager syncManager = getSyncManager(isGlobal);
        try {
            syncManager.syncDown(SyncDownTarget.fromJSON(target), SyncOptions.fromJSON(options), soupName, new SyncManager.SyncUpdateCallback() {
                @Override
                public void onUpdate(SyncState sync) {
                    handleSyncUpdate(sync, successCallback);
                }
            });
        } catch (JSONException e) {
            errorCallback.invoke(e.toString());
        }
    }

    /**
     * Native implementation of getSyncStatus
     * @param args
     * @param successCallback
     * @param errorCallback
     */
    public void getSyncStatus(ReadableMap args,
                              final Callback successCallback, final Callback errorCallback) {
        // Parse args
        long syncId = args.getInt(SYNC_ID);
        boolean isGlobal = args.getBoolean(IS_GLOBAL_STORE);

        SyncManager syncManager = getSyncManager(isGlobal);
        try {
            SyncState sync = syncManager.getSyncStatus(syncId);
            successCallback.invoke(sync.asJSON().toString());
        } catch (JSONException e) {
            errorCallback.invoke(e.toString());
        }
    }

    /**
     * Native implementatino of reSync
     * @param args
     * @param successCallback
     * @param errorCallback
     */
    public void reSync(ReadableMap args,
                       final Callback successCallback, final Callback errorCallback) {
        // Parse args
        long syncId = args.getInt(SYNC_ID);
        final boolean isGlobal = args.getBoolean(IS_GLOBAL_STORE);

        SyncManager syncManager = getSyncManager(isGlobal);
        try {
            syncManager.reSync(syncId, new SyncManager.SyncUpdateCallback() {
                @Override
                public void onUpdate(SyncState sync) {
                    handleSyncUpdate(sync, successCallback);
                }
            });
        } catch (JSONException e) {
            errorCallback.invoke(e.toString());
        }
    }

    /**
     * Sync update handler
     * @param sync
     *
     */
    private void handleSyncUpdate(final SyncState sync, Callback successCallback) {
        if (sync.getStatus() == SyncState.Status.DONE) {
            try {
                successCallback.invoke(sync.asJSON().toString());
            } catch (JSONException e) {
                Log.e("SmartSyncReactBridge", "handleSyncUpdate", e);
            }
        }
    }

    /**
     * Return sync manager to use
     * @param isGlobal
     * @return
     */
    private SyncManager getSyncManager(boolean isGlobal) {
        SyncManager syncManager = isGlobal
                ? SyncManager.getInstance(null, null, SalesforceSDKManagerWithSmartStore.getInstance().getGlobalSmartStore())
                : SyncManager.getInstance();

        return syncManager;
    }

}