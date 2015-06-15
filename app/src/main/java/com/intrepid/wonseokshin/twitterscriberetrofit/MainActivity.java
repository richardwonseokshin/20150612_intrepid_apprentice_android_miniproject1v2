package com.intrepid.wonseokshin.twitterscriberetrofit;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends ActionBarActivity {

    private Thread threadGetPin;
    private Thread threadSignIn;
    private OAuthService service;
    private Token requestToken;
    private Token accessToken;
    private String request_base_url;
    private String oAuthConsumerKey;
    private String stringNonce;
    private String oauth_signature_method;
    private long timeStamp;
    private String oauth_token;
    private String oauth_version;
    private String signature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        threadGetPin = new Thread(){
            @Override
            public void run() {
                super.run();

                service = new ServiceBuilder()
                        .provider(TwitterApi.class)
                        .apiKey("1RvbG1B0GewG7h40txjiqD50x")
                        .apiSecret("Bk3hdEHgzW3Ivi49kZ5PjzBf03MailerbRA9YMJIXdpuFIRXk0")
                        .build();
                requestToken = service.getRequestToken();

                final String authUrl = service.getAuthorizationUrl(requestToken);


                final WebView wvLogin = (WebView) findViewById(R.id.webview_login);
                wvLogin.post(new Runnable() {
                    @Override
                    public void run() {
                        wvLogin.loadUrl(authUrl);
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView tvTesting = (TextView) findViewById(R.id.test_textview);
                        tvTesting.append(authUrl);
                        tvTesting.postInvalidate();
                    }
                });

                try {
                    threadGetPin.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        threadGetPin.start();


        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getPinFromEditText(View view){
        EditText etPinNumber = (EditText)findViewById(R.id.et_pin_number);
        final String  pinNumber = etPinNumber.getText().toString();

        if(pinNumber != null && pinNumber.length() == 0){
            Toast.makeText(MainActivity.this, "Please Input the Pin Number", Toast.LENGTH_LONG).show();
            return;
        }

        if(requestToken != null){
            threadSignIn = new Thread(){
                @Override
                public void run() {
                    super.run();
                    Verifier v = new Verifier(pinNumber);
                    accessToken = service.getAccessToken(requestToken, v);

                    final OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.twitter.com/1.1/account/verify_credentials.json");

                    if(accessToken != null && request != null) {
                        service.signRequest(accessToken, request); // the access token from step 4
                        final Response response = request.send();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ((TextView)findViewById(R.id.tv_signin_response)).setText(response.getBody());
                                ((WebView) findViewById(R.id.webview_login)).clearCache(true);
                            }
                        });


                        testHTTPRequest();


                        try {
                            threadSignIn.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            threadSignIn.start();
        }
    }


    /**
     * twitter public api, notes
     Authorization parmeters for http requests:
     OAuth oauth_consumer_key="1RvbG1B0GewG7h40txjiqD50x",
     oauth_nonce="kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg",

     oauth_signature="tnnArxj06cWHq44gCs1OSKk%2FjLY%3D",

     oauth_signature_method="HMAC-SHA1",

     oauth_timestamp="1318622958",
     oauth_token="370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb",
     oauth_version="1.0"
     */
    public void testHTTPRequest(){
        //GET statuses/home_timeline
        request_base_url = "https://api.twitter.com/1.1/statuses/home_timeline.json";

        oAuthConsumerKey = "1RvbG1B0GewG7h40txjiqD50x";

        stringNonce = generateNonce();

        oauth_signature_method = "HMAC-SHA1";

        //number of seconds since unix epoch, 1970 Jan 1
        timeStamp = System.currentTimeMillis() / 1000;

        //access token for twitter app
        oauth_token = accessToken.getToken();

        oauth_version = "1.0";


        Map <String,String> mapKeyValue = new HashMap<>();
        mapKeyValue.put(OAuth.percentEncode("oauth_consumer_key"), OAuth.percentEncode(oAuthConsumerKey));
        mapKeyValue.put(OAuth.percentEncode("oauth_nonce"), OAuth.percentEncode(stringNonce));
        mapKeyValue.put(OAuth.percentEncode("oauth_signature_method"), OAuth.percentEncode(oauth_signature_method));
        mapKeyValue.put(OAuth.percentEncode("oauth_timestamp"), OAuth.percentEncode("" + timeStamp));
        mapKeyValue.put(OAuth.percentEncode("oauth_token"), OAuth.percentEncode(oauth_token));
        mapKeyValue.put(OAuth.percentEncode("oauth_version"), OAuth.percentEncode(oauth_version));


        String stringRequestParams = "";


        Object[] keys = mapKeyValue.keySet().toArray();
        Arrays.sort(keys);
        for(Object key : keys) {
            stringRequestParams = stringRequestParams + key + "=" + mapKeyValue.get(key) + "&";
        }
        stringRequestParams = stringRequestParams.substring(0, stringRequestParams.length() - 1); //to remove the last "&"

        //Create request signature: https://dev.twitter.com/oauth/overview/creating-signatures
        String outputString = "";
        //Convert the HTTP Method to uppercase and set the output string equal to this value.
        outputString = "GET";
        //Append the ‘&’ character to the output string.
        outputString = outputString + "&";
        //Percent encode the URL and append it to the output string.
        outputString = outputString + OAuth.percentEncode(request_base_url);
        //Append the ‘&’ character to the output string.
        outputString = outputString + "&";
        //Percent encode the parameter string and append it to the output string.
        outputString = outputString + OAuth.percentEncode(stringRequestParams);
        final String stringSignatureBase = outputString;

        //Calculate signature: https://dev.twitter.com/oauth/overview/creating-signatures
        String signing_key = "";
        String consumer_secret = "Bk3hdEHgzW3Ivi49kZ5PjzBf03MailerbRA9YMJIXdpuFIRXk0";
        String access_token_secret = accessToken.getSecret();
        signing_key = OAuth.percentEncode(consumer_secret) + "&" + OAuth.percentEncode(access_token_secret);
        signature = "";
        try {
            signature =  Base64.encodeToString(calculateRFC2104HMAC(stringSignatureBase, signing_key), Base64.NO_WRAP);
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        final String homeTimeline = getHomeTimeline();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView tvHTTPRequest = (TextView)findViewById(R.id.tv_signin_response);
                tvHTTPRequest.setText(homeTimeline);
            }
        });
    }

    private String getHomeTimeline(){
        String url = request_base_url;
        String authorizationHeader = generateAuthorizationHeader();

        DefaultHttpClient httpclient = new DefaultHttpClient(); // create new httpClient
        HttpGet httpGet = new HttpGet(url); // create new httpGet object
        StringBuilder body = new StringBuilder();

        httpGet.setHeader("Authorization", authorizationHeader);

        try {
            HttpResponse response = httpclient.execute(httpGet); // execute httpGet
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                // System.out.println(statusLine);
                body.append(statusLine + "\n");
                HttpEntity e = response.getEntity();
                String entity = EntityUtils.toString(e);
                body.append(entity);
            } else {
                body.append(statusLine + "\n");
                // System.out.println(statusLine);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return body.toString();
    }


    //generates the authorization header for the twitter home timeline retrieval api call using http get
    //based on the consumer key, consumer key secret, access token, and the other required headers
    private String generateAuthorizationHeader(){
        String DST = "";
        DST = DST + "OAuth ";

        DST = DST + OAuth.percentEncode("oauth_consumer_key");
        DST = DST + "=";
        DST = DST + "\"";
        DST = DST + OAuth.percentEncode(oAuthConsumerKey);
        DST = DST + "\"";
        DST = DST + ", ";

        DST = DST + OAuth.percentEncode("oauth_nonce");
        DST = DST + "=";
        DST = DST + "\"";
        DST = DST + OAuth.percentEncode(stringNonce);
        DST = DST + "\"";
        DST = DST + ", ";

        DST = DST + OAuth.percentEncode("oauth_signature");
        DST = DST + "=";
        DST = DST + "\"";
        DST = DST + OAuth.percentEncode(signature);
        DST = DST + "\"";
        DST = DST + ", ";

        DST = DST + OAuth.percentEncode("oauth_signature_method");
        DST = DST + "=";
        DST = DST + "\"";
        DST = DST + OAuth.percentEncode(oauth_signature_method);
        DST = DST + "\"";
        DST = DST + ", ";

        DST = DST + OAuth.percentEncode("oauth_timestamp");
        DST = DST + "=";
        DST = DST + "\"";
        DST = DST + OAuth.percentEncode("" + timeStamp);
        DST = DST + "\"";
        DST = DST + ", ";

        DST = DST + OAuth.percentEncode("oauth_token");
        DST = DST + "=";
        DST = DST + "\"";
        DST = DST + OAuth.percentEncode(oauth_token);
        DST = DST + "\"";
        DST = DST + ", ";

        DST = DST + OAuth.percentEncode("oauth_version");
        DST = DST + "=";
        DST = DST + "\"";
        DST = DST + OAuth.percentEncode(oauth_version);
        DST = DST + "\"";

        return DST;
    }


    //HMAC-SHA1, modified from https://gist.github.com/ishikawa/88599
    //returns the byte array of the sha1 instead of the string representation
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();

        for (byte b : bytes) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    public static byte[] calculateRFC2104HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException
    {
        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return mac.doFinal(data.getBytes());
    }

    /**
     * generate a nonce
     * generated by base64 encoding 32 bytes of random data, and stripping out all non-word characters
     * any approach which produces a relatively random alphanumeric string should be OK here.
     */
    private static int NONCE_LENGTH = 32;
    private String generateNonce(){
        try {
            SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
            byte[] nonceByteArray = new byte[NONCE_LENGTH];

            //get 32 bytes of randomized data from SecureRandom class to generate a nonce
            secureRandom.nextBytes(nonceByteArray);

            //as in twitter's example authorizing http requests, use a base64 encoding
            String nonceString = Base64.encodeToString(nonceByteArray, Base64.NO_WRAP);

            //as in twitter's example authorizing http requests, strip the nonce of nonword characters
            return nonceString.replaceAll("[^\\p{L}\\p{Nd}]+", "");
        }
        catch (Exception e) {
            //throw new AuthenticationException(e.getMessage(),e);
            e.printStackTrace();
        }
        return null;
    }

}
