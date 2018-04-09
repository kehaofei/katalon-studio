package com.kms.katalon.integration.kobiton.preferences;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.kms.katalon.integration.kobiton.constants.KobitonPreferenceConstants;
import com.kms.katalon.integration.kobiton.entity.KobitonApiKey;
import com.kms.katalon.integration.kobiton.entity.KobitonLoginInfo;
import com.kms.katalon.integration.kobiton.providers.KobitonApiProvider;
import com.kms.katalon.preferences.internal.PreferenceStoreManager;
import com.kms.katalon.preferences.internal.ScopedPreferenceStore;

public class KobitonPreferencesProvider {
    private static ScopedPreferenceStore getPreferencetStore() {
        return PreferenceStoreManager.getPreferenceStore(KobitonPreferenceConstants.KOBITON_QUALIFIER);
    }

    public static boolean isKobitonIntegrationEnabled() {
        return getPreferencetStore().getBoolean(KobitonPreferenceConstants.KOBITON_INTEGRATION_ENABLE);
    }

    public static String getKobitonUserName() {
        return getPreferencetStore().getString(KobitonPreferenceConstants.KOBITON_AUTHENTICATION_USERNAME);
    }
    
    public static void saveKobitonUserName(String userName) {
        if (StringUtils.isEmpty(userName)) {
            return;
        }
        getPreferencetStore().setValue(KobitonPreferenceConstants.KOBITON_AUTHENTICATION_USERNAME, userName);
    }

    public static String getKobitonPassword() {
        return getPreferencetStore().getString(KobitonPreferenceConstants.KOBITON_AUTHENTICATION_PASSWORD);
    }
    
    public static void saveKobitonPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return;
        }
        getPreferencetStore().setValue(KobitonPreferenceConstants.KOBITON_AUTHENTICATION_PASSWORD, password);
    }
    
    public static String getKobitonToken() {
        String kobitonToken = getPreferencetStore().getString(KobitonPreferenceConstants.KOBITON_AUTHENTICATION_TOKEN);

        // Console mode: we just passed username and password. So, we need to generate token automatically.
        if (StringUtils.isEmpty(kobitonToken)) {
            KobitonLoginInfo loginInfo;
            try {
                loginInfo = KobitonApiProvider.login(getKobitonUserName(), getKobitonPassword());
                kobitonToken = loginInfo.getToken();
                KobitonPreferencesProvider.saveKobitonToken(loginInfo.getToken());
                List<KobitonApiKey> apiKeys = KobitonApiProvider.getApiKeyList(loginInfo.getToken());
                if (!apiKeys.isEmpty()) {
                    KobitonPreferencesProvider.saveKobitonApiKey(apiKeys.get(0).getKey());
                }

            } catch (Exception e) {
                throw new RuntimeException(
                        "Authentication kobiton system failed:  Please check username and password again.", e);
            }
        }

        return kobitonToken;
    }
    
    public static void saveKobitonToken(String token) {
        if (StringUtils.isEmpty(token)) {
            return;
        }
        getPreferencetStore().setValue(KobitonPreferenceConstants.KOBITON_AUTHENTICATION_TOKEN, token);
    }

    public static String getKobitonApiKey() {
        return getPreferencetStore().getString(KobitonPreferenceConstants.KOBITON_API_KEY);
    }
    
    public static void saveKobitonApiKey(String apiKey) {
        if (StringUtils.isEmpty(apiKey)) {
            return;
        }
        getPreferencetStore().setValue(KobitonPreferenceConstants.KOBITON_API_KEY, apiKey);
    }
}
