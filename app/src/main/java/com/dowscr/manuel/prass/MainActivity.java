/*
 * MIT License
 *
 * Copyright (c) 2020 Manuel Gundlach <manuel.gundlach@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dowscr.manuel.prass;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Locale;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;


public class MainActivity extends AppCompatActivity implements LoginDialogFragment.LoginDialogListener, AcceptConditionsDialogFragment.AcceptConditionsDialogListener {

    private static boolean Vibrate;
    private static String CommandQuota;
    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencelistener;

    public static boolean getVibrate() {
        return Vibrate;
    }
    private static void setVibrate(boolean vibes) {
        Vibrate = vibes;
    }

    public static String getCommandQuota() {
        return CommandQuota;
    }

    private static void setCommandQuota(String c) {
        CommandQuota = c;
    }

    private SharedPreferences preferencemanager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        final androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.pager);
        assert mViewPager != null;
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // hide the given tab
//                tab.getCustomView().clearAnimation();
            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event
            }
        };

        actionBar.addTab(
                actionBar.newTab()
                        .setText(R.string.title_section1)
                        .setTabListener(tabListener));

        actionBar.addTab(
                actionBar.newTab()
                        .setText(R.string.title_section2)
                        .setTabListener(tabListener));

        Intent intent = getIntent();
        String action = intent.getAction();
//        String scheme = action.getScheme();

        if (Intent.ACTION_SEND.equals(action) /*&& type != null*/) {
//            if (scheme.equals("file")||scheme.equals("content")) {
            mViewPager.setCurrentItem(1);
//            }
        }

        preferencemanager = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        preferencelistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateCreds();
            }
        };

        preferencemanager.registerOnSharedPreferenceChangeListener(preferencelistener);

        if (!preferencemanager.contains(getString(R.string.conditionsnow)))
            (new AcceptConditionsDialogFragment()).show(getSupportFragmentManager(), "AcceptConditionsDialogFragment");
        else
            checkforCreds();
    }

    private void checkforCreds() {
        if (!preferencemanager.contains("user"))
            (new LoginDialogFragment()).show(getSupportFragmentManager(), "LoginDialogFragment");
        else
            updateCreds();
    }

    private void updateCreds() {
        SharedPreferences p = preferencemanager;
        ConnectionHandler.setCreds(p.getString("user", "user"),
                p.getString("pass", "pass"));
        setVibrate(p.getBoolean("Vibration", true));
        setCommandQuota(p.getString("commandquota", "printquotar"));
        new assureConnection().execute();
    }

    @Override
    public void onDialogPositiveClick(LoginDialogFragment dialog) {
        SharedPreferences.Editor e = preferencemanager.edit();
        e.putString("user", dialog.u);
        e.putString("pass", dialog.p);
        e.putBoolean("Vibration", true);
        e.apply();
        checkforCreds();
    }

    @Override
    public void onDialogNegativeClick(LoginDialogFragment dialog) {

    }

    @Override
    public void onDialogPositiveClick(AcceptConditionsDialogFragment dialog) {
        SharedPreferences.Editor e = preferencemanager.edit();
        e.putBoolean(getString(R.string.conditionsnow), true);
        e.apply();
        checkforCreds();
    }

    @Override
    public void onDialogNegativeClick(AcceptConditionsDialogFragment dialog) {
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, 39324);
            return true;
        }

        if (id == R.id.action_impressum) {
            final Intent intent = new Intent(this, ImpressumActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Tries to connect so SSH server in background
    private class assureConnection extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                ConnectionHandler.assureConnection();
            } catch (Exception ignore) {
            }
            return null;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0)
                return Statistics.newInstance();
            else
                return FilePrint.newInstance();
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }
    }

}
