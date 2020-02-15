package lk.slbi.catalog;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity implements Runnable {

    Thread mThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);
        mThread = new Thread(this);

        mThread.start();
    }

    @Override
    public void run(){
        try {
            Thread.sleep(5000);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            startActivity(new Intent(getApplicationContext(), MainActivity.class));

            finish();
        }
    }


}
