package com.intrepid.wonseokshin.twitterscriberetrofit;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;


public class MainActivity extends ActionBarActivity {

    private Thread threadGetPin;
    private Thread threadSignIn;
    private OAuthService service;
    private Token requestToken;
    private Token accessToken;

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
}
