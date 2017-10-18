/*
 * Copyright (c) 2017-present, salesforce.com, inc.
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
package com.salesforce.androidsdk.auth.idp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.salesforce.androidsdk.R;
import com.salesforce.androidsdk.accounts.UserAccount;
import com.salesforce.androidsdk.auth.OAuth2;
import com.salesforce.androidsdk.util.SalesforceSDKLogger;
import com.salesforce.androidsdk.util.UriFragmentParser;

import java.util.Locale;
import java.util.Map;

/**
 * Launches a WebView, runs IDP requests within it and finishes itself when done.
 *
 * @author bhariharan
 */
public class IDPWebViewActivity extends Activity {

    public static final String USER_ACCOUNT_BUNDLE_KEY = "user_account_bundle";
    public static final String SP_CONFIG_BUNDLE_KEY = "sp_config_bundle";
    private static final String ERROR_KEY = "error";
    private static final String ERROR_DESCRIPTION_KEY = "error_description";
    private static final String TAG = "IDPWebViewActivity";

    private UserAccount userAccount;
    private SPConfig spConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fetches the required extras.
        final Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            userAccount = new UserAccount(extras.getBundle(USER_ACCOUNT_BUNDLE_KEY));
            spConfig = new SPConfig(extras.getBundle(SP_CONFIG_BUNDLE_KEY));
        }

        // Protects against screenshots.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.sf__idp_webview);
        final WebView webView = findViewById(R.id.sf__webview);
        final WebSettings webSettings = webView.getSettings();
        webSettings.setUseWideViewPort(true);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        webView.setWebViewClient(new IDPWebViewClient());
        try {
            final IDPRequestHandler idpRequestHandler = new IDPRequestHandler(spConfig, userAccount);
            new RefreshAuthTokenTask(idpRequestHandler, webView).execute();
        } catch (IDPRequestHandler.IDPRequestHandlerException e) {
            SalesforceSDKLogger.e(TAG, "Building IDP request handler failed", e);
        }
    }

    /**
     * This class is used to monitor redirects within the WebView to determine
     * when the login flow is complete, parses the response and passes it back.
     */
    protected class IDPWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            boolean isDone = url.replace("///", "/").toLowerCase(Locale.US).startsWith(spConfig.getOauthCallbackUrl().
                    replace("///", "/").toLowerCase(Locale.US));
            if (isDone) {
                final Uri callbackUri = Uri.parse(url);
                final Map<String, String> params = UriFragmentParser.parse(callbackUri);

                // Determines if the authentication flow succeeded or failed.
                final String error = params.get(ERROR_KEY);
                if (error != null) {
                    final String errorDesc = params.get(ERROR_DESCRIPTION_KEY);
                    // TODO: Propagate error back.
                } else {
                    final OAuth2.TokenEndpointResponse tr = new OAuth2.TokenEndpointResponse(params);
                    // TODO: Propagate response back.
                }
            }
            return isDone;
        }
    }

    private class RefreshAuthTokenTask extends AsyncTask<Void, Void, String> {

        private IDPRequestHandler idpRequestHandler;
        private WebView webView;

        public RefreshAuthTokenTask(IDPRequestHandler idpRequestHandler, WebView webView) {
            this.idpRequestHandler = idpRequestHandler;
            this.webView = webView;
        }

        @Override
        protected String doInBackground(Void... nothings) {
            String accessToken = null;
            try {
                accessToken = idpRequestHandler.getValidAccessToken();
            } catch (Exception e) {
                SalesforceSDKLogger.e(TAG, "Refreshing token failed", e);
            }
            return accessToken;
        }

        @Override
        protected void onPostExecute(String accessToken) {
            try {
                if (accessToken != null) {
                    idpRequestHandler.makeFrontDoorRequest(accessToken, webView);
                }
            } catch (IDPRequestHandler.IDPRequestHandlerException e) {
                SalesforceSDKLogger.e(TAG, "Making frontdoor request failed", e);
            }
        }
    }
}
