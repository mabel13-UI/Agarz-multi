package com.agarz.multi;

import android.os.Bundle;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {

    private static final int ACCOUNT_COUNT = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Tam ekran, uyku modunu engelle
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout  = findViewById(R.id.tabLayout);

        // Her hesap için ayrı fragment — her birinin cookie'si izole
        AccountPagerAdapter adapter = new AccountPagerAdapter(this, ACCOUNT_COUNT);
        viewPager.setAdapter(adapter);

        // Tüm sayfaları önceden yükle (swipe anında lag olmaz)
        viewPager.setOffscreenPageLimit(ACCOUNT_COUNT);

        // Sekme başlıkları
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
            tab.setText("H" + (position + 1))
        ).attach();
    }

    @Override
    public void onBackPressed() {
        // Geri tuşu çıkış yerine ana ekrana gönderir
        moveTaskToBack(true);
    }
}
