package com.Udaicoders.wawbstatussaver;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.Udaicoders.wawbstatussaver.fragment.DownloadsFragment;
import com.Udaicoders.wawbstatussaver.fragment.RecentStatusFragment;
import com.Udaicoders.wawbstatussaver.util.AdController;
import com.Udaicoders.wawbstatussaver.util.SharedPrefs;
import com.Udaicoders.wawbstatussaver.util.Utils;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.ironsource.mediationsdk.IronSource;

import java.util.ArrayList;
import java.util.List;

import slidingrootnav.SlidingRootNav;
import slidingrootnav.SlidingRootNavBuilder;

public class MainActivity extends AppCompatActivity {

    private SlidingRootNav slidingRootNav;
    ImageView whatsIV, navIV, nTop;

    LinearLayout nWapp, nSaved, nLang, nShare, nRate, nPrivacy, nHow;
    RelativeLayout nDark;

    ImageView niWapp, niSaved, niDark, niLang, niShare, niRate, niPrivacy, niHow;

    TextView ntWapp, ntSaved, ntDark, ntLang, ntShare, ntRate, ntPrivacy, ntHow;

    SwitchCompat modeSwitch;

    ViewPager viewPager;
    BottomNavigationView bottomNav;
    Dialog dialog, dialogLang;
    LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLanguage(MainActivity.this, SharedPrefs.getLang(MainActivity.this));
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        viewPager.setOffscreenPageLimit(2);

        bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_images) {
                viewPager.setCurrentItem(0, true);
            } else if (id == R.id.nav_videos) {
                viewPager.setCurrentItem(1, true);
            } else if (id == R.id.nav_saved) {
                viewPager.setCurrentItem(2, true);
            }
            return true;
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bottomNav.setSelectedItemId(R.id.nav_images);
                        break;
                    case 1:
                        bottomNav.setSelectedItemId(R.id.nav_videos);
                        break;
                    case 2:
                        bottomNav.setSelectedItemId(R.id.nav_saved);
                        break;
                }

                // Refresh RecentStatusFragment only when needed
                Fragment fragment = getSupportFragmentManager()
                        .findFragmentByTag("android:switcher:" + viewPager.getId()
                                + ":" + position);
                if (fragment instanceof RecentStatusFragment) {
                    RecentStatusFragment rsf = (RecentStatusFragment) fragment;
                    String waTree = SharedPrefs.getWATree(MainActivity.this);
                    String wbTree = SharedPrefs.getWBTree(MainActivity.this);
                    boolean hasAccess = !waTree.equals("") || !wbTree.equals("");
                    if (hasAccess && (isReturnedFromWhatsApp || !rsf.isDataLoaded())) {
                        rsf.populateGrid();
                    }
                }
                isReturnedFromWhatsApp = false;
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });


        navIV = findViewById(R.id.navIV);
        navIV.setOnClickListener(v -> {
            slidingRootNav.openMenu(true);
        });

        slidingRootNav = new SlidingRootNavBuilder(this).withMenuOpened(false).withContentClickableWhenMenuOpened(false).withSavedState(savedInstanceState).withMenuLayout(R.layout.menu_left_drawer).inject();

        nTop = findViewById(R.id.nTop);
        Glide.with(this).load(R.drawable.mtop).into(nTop);

        initDrawer();

        wAppAlert();
        whatsIV = findViewById(R.id.whatsIV);
        whatsIV.setOnClickListener(v -> {
            dialog.show();
        });

        langAlert();

        container = findViewById(R.id.banner_container);
        if (AdController.isLoadIronSourceAd) {
            AdController.inItIron(MainActivity.this);
        } else {
            /*admob*/
            AdController.loadBannerAd(MainActivity.this, container);
            AdController.loadInterAd(MainActivity.this);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (AdController.isLoadIronSourceAd) {
            AdController.destroyIron();
            AdController.ironBanner(MainActivity.this, container);
            // call the IronSource onResume method
            IronSource.onResume(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (AdController.isLoadIronSourceAd) {
            // call the IronSource onPause method
            IronSource.onPause(this);
        }
    }

    boolean isReturnedFromWhatsApp = false;

    void wAppAlert() {
        dialog = new Dialog(MainActivity.this);
        dialog.setContentView(R.layout.popup_lay);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        RelativeLayout btnWapp = dialog.findViewById(R.id.btn_wapp);
        RelativeLayout btnWappBus = dialog.findViewById(R.id.btn_wapp_bus);

        btnWapp.setOnClickListener(arg0 -> {
            try {
                isReturnedFromWhatsApp = true;
                startActivity(getPackageManager().getLaunchIntentForPackage("com.whatsapp"));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Please Install WhatsApp For Download Status!!!!!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();

        });

        btnWappBus.setOnClickListener(arg0 -> {
            try {
                isReturnedFromWhatsApp = true;
                startActivity(getPackageManager().getLaunchIntentForPackage("com.whatsapp.w4b"));
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Please Install WhatsApp Business For Download Status!!!!!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

    }


    void langAlert() {
        dialogLang = new Dialog(MainActivity.this);
        dialogLang.setContentView(R.layout.lang_lay);

        dialogLang.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextView txtEn = dialogLang.findViewById(R.id.txt_en);
        TextView txtHi = dialogLang.findViewById(R.id.txt_hi);
        TextView txtAr = dialogLang.findViewById(R.id.txt_ar);

        txtEn.setOnClickListener(arg0 -> {
            SharedPrefs.setLang(MainActivity.this, "en");
            dialogLang.dismiss();
            refresh();
        });

        txtHi.setOnClickListener(arg0 -> {
            SharedPrefs.setLang(MainActivity.this, "hi");
            dialogLang.dismiss();
            refresh();
        });

        txtAr.setOnClickListener(arg0 -> {
            SharedPrefs.setLang(MainActivity.this, "ar");
            dialogLang.dismiss();
            refresh();
        });

    }

    void refresh() {
        finish();
        startActivity(getIntent());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    ViewPagerAdapter adapter;

    private void setupViewPager(ViewPager viewPager) {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());

        adapter.addFragment(RecentStatusFragment.newInstance(RecentStatusFragment.FILTER_IMAGES), "Images");
        adapter.addFragment(RecentStatusFragment.newInstance(RecentStatusFragment.FILTER_VIDEOS), "Videos");
        adapter.addFragment(new DownloadsFragment(), "Saved");

        viewPager.setAdapter(adapter);
    }

    static class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int arg0) {
            return this.mFragmentList.get(arg0);
        }

        @Override
        public int getCount() {
            return this.mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            this.mFragmentList.add(fragment);
            this.mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return this.mFragmentTitleList.get(position);
        }
    }

    public void initDrawer() {
        nWapp = findViewById(R.id.nWapp);
        nSaved = findViewById(R.id.nSaved);
        nDark = findViewById(R.id.nDark);
        nLang = findViewById(R.id.nLang);
        nShare = findViewById(R.id.nShare);
        nRate = findViewById(R.id.nRate);
        nPrivacy = findViewById(R.id.nPrivacy);
        nHow = findViewById(R.id.nHow);

        niWapp = findViewById(R.id.niWapp);
        niSaved = findViewById(R.id.niSaved);
        niDark = findViewById(R.id.niDark);
        niLang = findViewById(R.id.niLang);
        niShare = findViewById(R.id.niShare);
        niRate = findViewById(R.id.niRate);
        niPrivacy = findViewById(R.id.niPrivacy);
        niHow = findViewById(R.id.niHow);

        ntWapp = findViewById(R.id.ntWapp);
        ntSaved = findViewById(R.id.ntSaved);
        ntDark = findViewById(R.id.ntDark);
        ntLang = findViewById(R.id.ntLang);
        ntShare = findViewById(R.id.ntShare);
        ntRate = findViewById(R.id.ntRate);
        ntPrivacy = findViewById(R.id.ntPrivacy);
        ntHow = findViewById(R.id.ntHow);


        nWapp.setOnClickListener(new ClickListener());
        nSaved.setOnClickListener(new ClickListener());
        nDark.setOnClickListener(new ClickListener());
        nLang.setOnClickListener(new ClickListener());
        nShare.setOnClickListener(new ClickListener());
        nRate.setOnClickListener(new ClickListener());
        nPrivacy.setOnClickListener(new ClickListener());
        nHow.setOnClickListener(new ClickListener());

        modeSwitch = findViewById(R.id.modeSwitch);
        int mode = SharedPrefs.getAppNightDayMode(this);
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            modeSwitch.setChecked(true);
        } else {
            modeSwitch.setChecked(false);
        }
        modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                SharedPrefs.setInt(this, SharedPrefs.PREF_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                SharedPrefs.setInt(this, SharedPrefs.PREF_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    void setUnpress() {
        setPress(niWapp, ntWapp, R.color.drawer_unpress);
        setPress(niSaved, ntSaved, R.color.drawer_unpress);
        setPress(niLang, ntLang, R.color.drawer_unpress);
        setPress(niHow, ntHow, R.color.drawer_unpress);
        setPress(niShare, ntShare, R.color.drawer_unpress);
        setPress(niRate, ntRate, R.color.drawer_unpress);
        setPress(niPrivacy, ntPrivacy, R.color.drawer_unpress);
    }

    void setPress(ImageView imageView, TextView textView, int color) {
        imageView.setColorFilter(ContextCompat.getColor(MainActivity.this, color), android.graphics.PorterDuff.Mode.SRC_IN);
        textView.setTextColor(getResources().getColor(color));
    }

    private class ClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.nWapp:
                    setUnpress();
                    setPress(niWapp, ntWapp, R.color.drawer_press);
                    viewPager.setCurrentItem(0);
                    slidingRootNav.closeMenu();
                    break;

                case R.id.nSaved:
                    setUnpress();
                    setPress(niSaved, ntSaved, R.color.drawer_press);
                    viewPager.setCurrentItem(2);
                    slidingRootNav.closeMenu();
                    break;

                case R.id.nDark:
                    slidingRootNav.closeMenu();
                    break;

                case R.id.nLang:
                    setUnpress();
                    setPress(niLang, ntLang, R.color.drawer_press);
                    dialogLang.show();
                    slidingRootNav.closeMenu();
                    break;

                case R.id.nHow:
                    setUnpress();
                    setPress(niHow, ntHow, R.color.drawer_press);
                    startActivity(new Intent(MainActivity.this, HelpActivity.class));
                    slidingRootNav.closeMenu();
                    break;

                case R.id.nShare:
                    setUnpress();
                    setPress(niShare, ntShare, R.color.drawer_press);
                    slidingRootNav.closeMenu();
                    shareApp();
                    break;

                case R.id.nRate:
                    setUnpress();
                    setPress(niRate, ntRate, R.color.drawer_press);
                    slidingRootNav.closeMenu();
                    rateUs();
                    break;

                case R.id.nPrivacy:
                    setUnpress();
                    setPress(niPrivacy, ntPrivacy, R.color.drawer_press);
                    slidingRootNav.closeMenu();
                    startActivity(new Intent(MainActivity.this, PolicyActivity.class));
                    break;

            }
        }
    }

    void navigate(Intent intent) {
        if (AdController.isLoadIronSourceAd) {
            AdController.ironShowInterstitial(MainActivity.this, intent, 0);
        } else {
            AdController.showInterAd(MainActivity.this, intent, 0);
        }
    }

    public void shareApp() {
        Intent myapp = new Intent(Intent.ACTION_SEND);
        myapp.setType("text/plain");
        myapp.putExtra(Intent.EXTRA_TEXT, "Download this awesome app\n https://play.google.com/store/apps/details?id=" + getPackageName() + " \n");
        startActivity(myapp);
    }

    public void rateUs() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private long mLastBackClick = 0;

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - mLastBackClick < 1100) {
            super.onBackPressed();
        } else {
            Toast.makeText(MainActivity.this, getResources().getString(R.string.exit_alert), Toast.LENGTH_SHORT).show();
            mLastBackClick = System.currentTimeMillis();
        }
    }

}
