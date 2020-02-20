package lk.slbi.catalog;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import lk.slbi.catalog.fcm.FirebaseMessageReceiver;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    // URL of object to be parsed
    static String API_URL = "http://slbi.lk/rest-api/slbi-shopping-android-app.json";
    static String VERSION_NAME = BuildConfig.VERSION_NAME;
    Context context = this;
    // This hashmap will hold the results
    HashMap<String, String> appAPI = new HashMap<>();
    // Defining the Volley request queue that handles the URL request concurrently
    RequestQueue requestQueue;
    private String host;
    private String domain;
    private Intent appLinkIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //for sending to all device using own server code subscribe your app to one topic
        Log.d("TOken ",""+ FirebaseInstanceId.getInstance().getToken());
        FirebaseMessaging.getInstance().subscribeToTopic("allDevices");

        String url = "http://catalog.slbi.lk";


        Log.d("Context", this.getBaseContext().toString());

        if (!isConnected()) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Internet Connection Alert")
                    .setMessage("Check Your Internet Connection and Try again!")
                    .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        } else {

            // Creates the Volley request queue
            requestQueue = Volley.newRequestQueue(this);

            // Creating the JsonObjectRequest class called obreq, passing required parameters:
            //GET is used to fetch data from the server, JsonURL is the URL to be fetched from.
            final JsonObjectRequest obreq = new JsonObjectRequest(Request.Method.GET, API_URL, null,
                    // The third parameter Listener overrides the method onResponse() and passes
                    //JSONObject as a parameter
                    new Response.Listener<JSONObject>() {

                        // Takes the response from the JSON request
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                // JSONObject obj = response.getJSONObject("colorObject");
                                // Retrieves the string labeled "colorName" and "description" from
                                //the response JSON Object
                                // Adds strings from object to the "appAPI" HashMap
                                //App Info
                                appAPI.put("protocol", response.getString("protocol"));
                                appAPI.put("hostName", response.getString("hostName"));
                                appAPI.put("versionName", response.getString("versionName"));
                                appAPI.put("storeLink", response.getString("storeLink"));

                                JSONObject routes = (JSONObject) response.get("routes");

                                appAPI.put("cart", routes.getString("cart"));
                                appAPI.put("checkout", routes.getString("checkout"));
                                appAPI.put("myAccount", routes.getString("my-account"));
                                appAPI.put("productCategory", routes.getString("product-category"));
                                appAPI.put("product", routes.getString("product"));
                                appAPI.put("orders", routes.getString("orders"));
                                appAPI.put("orderReceived", routes.getString("order-received"));
                                appAPI.put("viewOrder", routes.getString("view-order"));
                                appAPI.put("editAccount", routes.getString("edit-account"));
                                appAPI.put("editAddress", routes.getString("edit-address"));

                                host = appAPI.get("hostName");
                                domain = appAPI.get("protocol") + host + "/";

                                webView = findViewById(R.id.webView);
                                progressBar = findViewById(R.id.progressBar);
                                progressBar.setMax(100);
                                WebSettings webSettings = webView.getSettings();
                                webSettings.setJavaScriptEnabled(true);
                                webSettings.setDisplayZoomControls(false);

                                // Dark Text on Status Bar
                                if (android.os.Build.VERSION.SDK_INT >= 23) {
                                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                                }

                                if (VERSION_NAME.equals(appAPI.get("versionName"))) {

                                    // ATTENTION: This was auto-generated to handle app links.
                                    appLinkIntent = getIntent();

                                    if (appLinkIntent.getDataString() != null) {
                                        Uri appLinkData = appLinkIntent.getData();
                                        String browserUrl = appAPI.get("protocol") + appLinkData.getHost() + appLinkData.getPath();
                                        webView.loadUrl(browserUrl);

                                    } else if (getIntent().hasExtra("notification_url")) {
                                        webView.loadUrl(getIntent().getStringExtra("notification_url"));
                                    }else {
                                        webView.loadUrl(domain);

                                    }


                                    //Only Open Own Domain in WebView. Other Links will open in other apps
                                    webView.setWebViewClient(new WebViewClient() {
                                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                                            if (url != null && !Uri.parse(url).getHost().equals(host)) {
                                                view.getContext().startActivity(
                                                        new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                                                return true;
                                            } else {
                                                return false;
                                            }
                                        }

                                        @Override
                                        public void onPageFinished(WebView view, String url) {
                                            super.onPageFinished(view, url);
                                            if (!webView.getUrl().equals(domain)) {
                                                String backLinkUrl = onBackLinkPressed();
                                                webView.loadUrl("javascript:(function() { " +
                                                        "$(\".active-mobile i\").removeClass(\"icon-menu icons\");"+
                                                        "$(\".active-mobile i\").addClass(\"fa fa-arrow-left\");"+
                                                        "$(\".active-mobile i\").addClass(\"fa fa-arrow-left\");"+
                                                        "$(\".active-mobile i\").attr(\"id\",\"androidBackButton\");"+
                                                        "$(\".active-mobile a:first-child\").attr(\"href\", \""+backLinkUrl+"\");"+
                                                        "})()");



                                            }
                                        }

                                        @Override
                                        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                                            super.onReceivedError(view, request, error);
                                            webView.setVisibility(WebView.GONE);
                                            websiteNotWork();
                                        }
                                    });
                                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                                        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
                                    } else {
                                        CookieManager.getInstance().setAcceptCookie(true);
                                    }

                                    webView.setWebChromeClient(new WebChromeClient() {
                                        @Override
                                        public void onProgressChanged(WebView view, int progress) {
                                            super.onProgressChanged(view, progress);
                                            if (progress < 100 && progressBar.getVisibility() == ProgressBar.GONE) {
                                                progressBar.setVisibility(ProgressBar.VISIBLE);
                                            }

                                            progressBar.setProgress(progress);
                                            if (progress == 100) {
                                                progressBar.setVisibility(ProgressBar.GONE);
                                            }
                                        }


                                        @Override
                                        public void onReceivedTitle(WebView view, String title) {
                                            super.onReceivedTitle(view, title);
                                        }

                                        @Override
                                        public void onReceivedIcon(WebView view, Bitmap icon) {
                                            super.onReceivedIcon(view, icon);
                                        }
                                    });

                                } else {
                                    //Update App Dialog Box
                                    versionUpdate();
                                }

                            }
                            // Try and catch are included to handle any errors due to JSON
                            catch (JSONException e) {
                                // If an error occurs, this prints the error to the log
                                e.printStackTrace();
                            }
                        }
                    },
                    // The final parameter overrides the method onErrorResponse() and passes VolleyError
                    //as a parameter
                    new Response.ErrorListener() {
                        @Override
                        // Handles errors that occur due to Volley
                        public void onErrorResponse(VolleyError error) {
                            Log.e("Volley", "Error");
                            websiteNotWork();
                        }
                    }
            );
            // Adds the JSON object request "obreq" to the request queue
            requestQueue.add(obreq);
        }

    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            String currentUrl = webView.getUrl();
            if (currentUrl.equals(domain+appAPI.get("cart"))) {
                webView.loadUrl(domain);
            } else if (currentUrl.equals(domain+appAPI.get("checkout"))) {
                webView.loadUrl(domain+appAPI.get("cart"));
            } else if (currentUrl.equals(domain+appAPI.get("myAccount"))) {
                webView.loadUrl(domain);
            } else if (currentUrl.equals(domain+appAPI.get("orders"))) {
                webView.loadUrl(domain);
            } else if (currentUrl.equals(domain+appAPI.get("editAccount"))) {
                webView.loadUrl(domain);
            } else if (currentUrl.equals(domain+appAPI.get("editAddress"))) {
                webView.loadUrl(domain);
            } else if (currentUrl.equals(domain)) {
                exitApp();
            } else {
                if (currentUrl.contains(domain+appAPI.get("productCategory"))) {
                    webView.loadUrl(domain);
                } else if (currentUrl.contains(domain+appAPI.get("product"))) {
                    webView.goBack();
                }  else if ((currentUrl.contains(domain+appAPI.get("orderReceived"))) || (currentUrl.contains(domain+appAPI.get("viewOrder")))) {
                    webView.loadUrl(domain+appAPI.get("orders"));
                } else if (currentUrl.contains(domain+appAPI.get("editAddress"))) {
                    webView.loadUrl(domain+appAPI.get("editAddress"));
                } else if (currentUrl.contains(domain+appAPI.get("myAccount"))) {
                    webView.loadUrl(domain+appAPI.get("myAccount"));
                } else {

                    webView.loadUrl(domain);
                }
            }

        } else {
            if (appLinkIntent.getDataString() != null) {
                webView.loadUrl(domain);
            } else {
                exitApp();
            }
        }
    }

    private Boolean isConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void versionUpdate() {
        progressBar.setVisibility(ProgressBar.GONE);
        new AlertDialog.Builder(context)
                .setMessage("New Version Available! Update SLBI Online Shopping App for Continuous Shopping.")
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                })
                .setNegativeButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(appAPI.get("storeLink")));
                        startActivity(intent);
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void exitApp() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to close app?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    private void websiteNotWork() {

        new AlertDialog.Builder(this)
                .setMessage("We're doing scheduled maintenance in our app. Please try again in few minutes."+"\n\nThank You!")
                .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Call Us", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:+94 77 309 9944"));
                        startActivity(intent);
                    }
                })
                .setCancelable(false)
                .show();
    }

    public String onBackLinkPressed() {
        String currentUrl = webView.getUrl();
        if (currentUrl.equals(domain+appAPI.get("cart"))) {
            return domain;
        } else if (currentUrl.equals(domain+appAPI.get("checkout"))) {
            return domain+appAPI.get("cart");
        } else if (currentUrl.equals(domain+appAPI.get("myAccount"))) {
            return domain;
        } else if (currentUrl.equals(domain+appAPI.get("orders"))) {
            return domain;
        } else if (currentUrl.equals(domain+appAPI.get("editAccount"))) {
            return domain;
        } else if (currentUrl.equals(domain+appAPI.get("editAddress"))) {
            return domain;
        }  else {
            if (currentUrl.contains(domain+appAPI.get("productCategory"))) {
                return domain;
            } else if (currentUrl.contains(domain+appAPI.get("product"))) {
                String historyUrl="";
                WebBackForwardList mWebBackForwardList = webView.copyBackForwardList();
                if (mWebBackForwardList.getCurrentIndex() > 0)
                    historyUrl = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex()-1).getUrl();
                return historyUrl;

            }  else if ((currentUrl.contains(domain+appAPI.get("orderReceived"))) || (currentUrl.contains(domain+appAPI.get("viewOrder")))) {
                return domain+appAPI.get("orders");
            } else if (currentUrl.contains(domain+appAPI.get("editAddress"))) {
                return domain+appAPI.get("editAddress");
            } else if (currentUrl.contains(domain+appAPI.get("myAccount"))) {
                return domain+appAPI.get("myAccount");
            } else {

                return domain;
            }
        }
    }

}