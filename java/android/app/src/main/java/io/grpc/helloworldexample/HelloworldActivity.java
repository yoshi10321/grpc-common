package io.grpc.helloworldexample;


import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.security.ProviderInstaller;

import com.squareup.okhttp.CipherSuite;
import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.TlsVersion;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;

import io.grpc.ChannelImpl;
import io.grpc.helloworldexample.Helloworld.HelloReply;
import io.grpc.helloworldexample.Helloworld.HelloRequest;
import io.grpc.transport.okhttp.OkHttpChannelBuilder;

public class HelloworldActivity extends ActionBarActivity implements ProviderInstaller.ProviderInstallListener {
  private Button mSendButton;
  private EditText mHostEdit;
  private EditText mPortEdit;
  private EditText mMessageEdit;
  private TextView mResultText;


  private static final int ERROR_DIALOG_REQUEST_CODE = 1;

  private boolean mRetryProviderInstall;

  TrustManagerFactory mTrustManagerFactory;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_helloworld);
    mSendButton = (Button) findViewById(R.id.send_button);
    mHostEdit = (EditText) findViewById(R.id.host_edit_text);
    mPortEdit = (EditText) findViewById(R.id.port_edit_text);
    mMessageEdit = (EditText) findViewById(R.id.message_edit_text);
    mResultText = (TextView) findViewById(R.id.grpc_response_text);


    initTestCa(getResources().openRawResource(R.raw.ca));
    ProviderInstaller.installIfNeededAsync(this, this);
  }

  public void sendMessage(View view) {
    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(mHostEdit.getWindowToken(), 0);
    mSendButton.setEnabled(false);
    new GrpcTask().execute();
  }

  @Override
  public void onProviderInstalled() {

  }

  @Override
  public void onProviderInstallFailed(int errorCode, Intent intent) {
    if (GooglePlayServicesUtil.isUserRecoverableError(errorCode)) {
      // Recoverable error. Show a dialog prompting the user to
      // install/update/enable Google Play services.
      GooglePlayServicesUtil.showErrorDialogFragment(
              errorCode,
              this,
              ERROR_DIALOG_REQUEST_CODE,
              new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                  // The user chose not to take the recovery action
                  onProviderInstallerNotAvailable();
                }
              });
    } else {
      // Google Play services is not available.
      onProviderInstallerNotAvailable();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == ERROR_DIALOG_REQUEST_CODE) {
      // Adding a fragment via GooglePlayServicesUtil.showErrorDialogFragment
      // before the instance state is restored throws an error. So instead,
      // set a flag here, which will cause the fragment to delay until
      // onPostResume.
      mRetryProviderInstall = true;
    }
  }

  @Override
  protected void onPostResume() {
    super.onPostResume();
    if (mRetryProviderInstall) {
      // We can now safely retry installation.
      ProviderInstaller.installIfNeededAsync(this, this);
    }
    mRetryProviderInstall = false;
  }

  private void onProviderInstallerNotAvailable() {
//     This is reached if the provider cannot be updated for some reason.
//     App should consider all HTTP communication to be vulnerable, and take
//     appropriate action.
  }

  private class GrpcTask extends AsyncTask<Void, Void, String> {
    private String mHost;
    private String mMessage;
    private int mPort;
    private ChannelImpl mChannel;

    @Override
    protected void onPreExecute() {
      mHost = mHostEdit.getText().toString();
      mMessage = mMessageEdit.getText().toString();
      String portStr = mPortEdit.getText().toString();
      mPort = TextUtils.isEmpty(portStr) ? 0 : Integer.valueOf(portStr);
      mResultText.setText("");
    }

    private String sayHello(ChannelImpl channel) {
      GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
      HelloRequest message = new HelloRequest();
      message.name = mMessage;
      HelloReply reply = stub.sayHello(message);
      return reply.message;
    }

    @Override
    protected String doInBackground(Void... nothing) {
      try {
//                ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//                        .tlsVersions(TlsVersion.TLS_1_2)
//                        .build();
//                ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
//                        .tlsVersions(TlsVersion.TLS_1_2)
//                        .cipherSuites(
//                                CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
//                                CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
//                                CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256)
//                        .build();


        OkHttpChannelBuilder channelBuilder = OkHttpChannelBuilder.forAddress(mHost, mPort);
//        channelBuilder.overrideHostForAuthority("x.test.youtube.com");
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, mTrustManagerFactory.getTrustManagers(), null);
//        sslContext.init(null, new X509TrustManager[]{new LooseTrustManager()}, null);
//        channelBuilder.sslSocketFactory(SSLContext.getDefault().getSocketFactory());

        channelBuilder.sslSocketFactory(sslContext.getSocketFactory());

        mChannel = channelBuilder.build();
        return sayHello(mChannel);
      } catch (Exception e) {
        e.printStackTrace();
        return "Failed... : " + e.getMessage();
      }
    }

    @Override
    protected void onPostExecute(String result) {
      try {
        mChannel.shutdown().awaitTerminated(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      mResultText.setText(result);
      mSendButton.setEnabled(true);
    }
  }

  public void initTestCa(InputStream testCa) {
    try {
      KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
      ks.load(null);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      X509Certificate cert = (X509Certificate) cf.generateCertificate(testCa);
      X500Principal principal = cert.getSubjectX500Principal();
      ks.setCertificateEntry(principal.getName("RFC2253"), cert);
      // Set up trust manager factory to use our key store.
      TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(ks);

      mTrustManagerFactory = trustManagerFactory;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  public class LooseTrustManager implements X509TrustManager {

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws
            CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }
  }

  public class LooseHostnameVerifier implements HostnameVerifier {
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  }
}