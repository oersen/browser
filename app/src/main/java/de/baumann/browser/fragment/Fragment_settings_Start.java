package de.baumann.browser.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;

import de.baumann.browser.activity.Whitelist_Cookie;
import de.baumann.browser.activity.Whitelist_Javascript;
import de.baumann.browser.R;
import de.baumann.browser.activity.Whitelist_DOM;
import de.baumann.browser.browser.AdBlock;
import de.baumann.browser.browser.BannerBlock;

public class Fragment_settings_Start extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preference_start, rootKey);

        initSummary(getPreferenceScreen());

        Preference sp_ad_block = findPreference("sp_ad_block");
        assert sp_ad_block != null;
        sp_ad_block.setSummary(AdBlock.getHostsDate(getContext()));

        Preference start_java = findPreference("start_java");
        assert start_java != null;
        start_java.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Whitelist_Javascript.class);
            requireActivity().startActivity(intent);
            return false;
        });
        Preference start_cookie = findPreference("start_cookie");
        assert start_cookie != null;
        start_cookie.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Whitelist_Cookie.class);
            requireActivity().startActivity(intent);
            return false;
        });
        Preference start_dom = findPreference("start_dom");
        assert start_dom != null;
        start_dom.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(getActivity(), Whitelist_DOM.class);
            requireActivity().startActivity(intent);
            return false;
        });
    }

    private void initSummary(Preference p) {
        if (p instanceof PreferenceGroup) {
            PreferenceGroup pGrp = (PreferenceGroup) p;
            for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                initSummary(pGrp.getPreference(i));
            }
        } else {
            updatePrefSummary(p);
        }
    }

    private void updatePrefSummary(Preference p) {
        if (p instanceof ListPreference) {
            ListPreference listPref = (ListPreference) p;
            p.setSummary(listPref.getEntry());
        }
        if (p instanceof EditTextPreference) {
            EditTextPreference editTextPref = (EditTextPreference) p;
                if (p.getSummaryProvider()==null)   p.setSummary(editTextPref.getText());
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sp, String key) {
        if (key.equals("ab_hosts") || key.equals("sp_ad_block") || key.equals("customHostListSwitch") || key.equals("sp_custom_host_list") ) {
            AdBlock.downloadHosts(getActivity());
        } else if (key.equals("sp_deny_cookie_banners")) {
            if (sp.getBoolean("sp_deny_cookie_banners",false)) BannerBlock.downloadBanners(getActivity());
//        } else if (key.equals("sp_userAgent") ||
//                key.equals("userAgentSwitch")) {  // not needed ??
//            sp.edit().putInt("restart_changed", 1).apply();  // maybe needed for future preferences
        }
        updatePrefSummary(findPreference(key));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
