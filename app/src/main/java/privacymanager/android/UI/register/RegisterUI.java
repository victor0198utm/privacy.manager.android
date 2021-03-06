package privacymanager.android.UI.register;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import javax.crypto.NoSuchPaddingException;

import privacymanager.android.R;
import privacymanager.android.UI.dialogs.ConfirmExit;
import privacymanager.android.utils.props.Props;
import privacymanager.android.utils.security.Crypto;
import privacymanager.android.utils.account.SharedPreferencesEditor;
import privacymanager.android.utils.internet.InternetConnection;
import privacymanager.android.utils.security.FileSecurityUtils;

public class RegisterUI extends AppCompatActivity {
    private static final String TAG = RegisterUI.class.getSimpleName();
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private String HOST_ADDRESS;
    private Intent intent;
    private Context ctx;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        intent = getIntent();
        ctx = getApplicationContext();
        HOST_ADDRESS = Props.getAppProperty(ctx,"HOST_ADDRESS");

        checkSecureLock();

        setListeners();
    }

    @Override
    public void onBackPressed() {
        ConfirmExit confirmExit = new ConfirmExit();
        FragmentManager fragmentManager = getSupportFragmentManager();
        confirmExit.show(fragmentManager, "exitDialog");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setListeners() {
        findViewById(R.id.toAuth).setOnClickListener(view -> {
            setResult(RESULT_CANCELED, intent);
            finish();
        });

        findViewById(R.id.register).setOnClickListener(view -> {
            Context ctx = getApplicationContext();

            if (!InternetConnection.checkConnection(ctx)){
                Toast.makeText(ctx,
                        "Connect to the internet for registration.",
                        Toast.LENGTH_LONG)
                        .show();
                return;
            }

            // registration url
            String url = HOST_ADDRESS.concat(Props.getAppProperty(ctx, "REGISTRATION"));

            EditText username = (EditText) findViewById(R.id.rPersonName);
            EditText password = (EditText) findViewById(R.id.rPassword);

            // registration payload
            JSONObject bodyParameters = new JSONObject();
            try {
                bodyParameters.put(USERNAME, username.getText().toString());
                bodyParameters.put(PASSWORD, password.getText().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // build the request
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, bodyParameters,
                    response -> {
                        Log.d(TAG, "createRegisterRequest() :: onResponse() ::" + response);

                        try {
                            closeActivityWithId(response.get("ownerId").toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                            closeActivityWithId("0");
                        }

                        boolean stored = storeAccount();
                        Log.d(TAG, "createRegisterRequest() :: storeAccount : " + (stored?"stored":"not sored"));

                    },
                    error -> {
                        Log.d(TAG, "createBookingRequest() :: onErrorResponse() ::" + error);
                        Toast.makeText(ctx,
                                "Could not connect to the server.",
                                Toast.LENGTH_LONG)
                                .show();
                    }
            );

            // make request
            RequestQueue requestQueue = Volley.newRequestQueue(ctx);
            requestQueue.add(jsonObjectRequest);

        });
    }

    /**
     * Check if the screen lock feature is enabled on the device.
     *
     * @return secured status
     */
    private boolean checkSecureLock(){
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if(keyguardManager.isKeyguardSecure()){
            return true;
        }
        Toast.makeText(this,
                "Secure lock screen was not set up.",
                Toast.LENGTH_LONG)
                .show();
        return false;
    }

    /**
     * Stores the account username and identifier
     *
     * @return stored status
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean storeAccount(){
        try{
            EditText usernameET = (EditText) findViewById(R.id.rPersonName);
            EditText passwordET = (EditText) findViewById(R.id.rPassword);
            EditText passwordConfirmET = (EditText) findViewById(R.id.rPasswordConfirm);
            String name = usernameET.getText().toString();
            String pass = passwordET.getText().toString();

            if (!pass.equals(passwordConfirmET.getText().toString())){
                Toast.makeText(this,
                        "Passwords does not match.",
                        Toast.LENGTH_LONG)
                        .show();
                return false;
            }

            // make account identifier
            String encryptedIdentifier = Crypto.encrypt(
                    name.getBytes(UTF_8),
                    pass);

            // store account identifier
            SharedPreferencesEditor.saveInSharedPreferences(this, "username", name);
            SharedPreferencesEditor.saveInSharedPreferences(this, "identifier", encryptedIdentifier);

        }catch (Exception e){
            throw new RuntimeException(e);
        }

        return true;
    }

    /**
     * Save the @id in SP and close the current activity.
     *
     * @param id user id
     */
    private void closeActivityWithId(String id){
        // store id
        SharedPreferencesEditor.saveInSharedPreferences(this, "id", id);

        Log.d(TAG, "createRegisterRequest() :: onResponse() :: CloseActivity() :: user_id: " + id);

        setResult(RESULT_OK, intent);
        finish();
    }

}
