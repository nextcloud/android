package com.owncloud.android.datamodel;

import android.text.TextUtils;

import com.owncloud.android.lib.common.Quota;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import lombok.Getter;
import lombok.Setter;

public class UserInfo {
    public String account = "";
    public Boolean enabled;
    public String displayName;
    public String email;
    public String phone;
    public String address;
    public String website;
    public String twitter;
    @Getter @Setter public ArrayList<String> groups;
    public Quota quota;

    @NonNull
    public String getAccount() {
        return account;
    }

    public void setAccount(@NonNull String account) {
        this.account = account;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getTwitter() {
        return twitter;
    }

    public void setTwitter(String twitter) {
        this.twitter = twitter;
    }

    public Quota getQuota() {
        return quota;
    }

    public void setQuota(Quota quota) {
        this.quota = quota;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(phone) && TextUtils.isEmpty(email) && TextUtils.isEmpty(address) &&
            TextUtils.isEmpty(twitter) && TextUtils.isEmpty(website) && (groups == null || groups.size() == 0);
    }
}
