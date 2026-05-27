package com.agarz.multi;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AccountPagerAdapter extends FragmentStateAdapter {

    private final int count;

    public AccountPagerAdapter(@NonNull FragmentActivity fa, int count) {
        super(fa);
        this.count = count;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Her pozisyon için ayrı WebView fragment — cookie izolasyonu için accountId geç
        return WebViewFragment.newInstance(position + 1);
    }

    @Override
    public int getItemCount() {
        return count;
    }
}
