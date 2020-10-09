package com.kms.katalon.feature;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.kms.katalon.application.utils.LicenseUtil;

public class FeatureConfigurations implements IFeatureService {

    private Properties coreFeatures;

    private Properties customFeatures;

    @SuppressWarnings("serial")
    private static final Map<KSEFeature, Boolean> coreFeaturesMap = new HashMap<KSEFeature, Boolean>() {
        {
            put(KSEFeature.SELF_HEALING, Boolean.TRUE);
            put(KSEFeature.WEB_LOCATOR_SETTINGS, Boolean.TRUE);
            put(KSEFeature.CUSTOM_WEB_SERVICE_METHOD, Boolean.TRUE);
            put(KSEFeature.CUSTOM_WEB_SERVICE_REQUEST_TIMEOUT, Boolean.TRUE);
            put(KSEFeature.CUSTOM_WEB_SERVICE_RESPONSE_SIZE_LIMIT, Boolean.TRUE);
            put(KSEFeature.ORACLE_EXTERNAL_DATA, Boolean.TRUE);
            put(KSEFeature.SQL_SERVER_EXTERNAL_DATA, Boolean.TRUE);
            put(KSEFeature.ADDTIONAL_TEST_DATA_SOURCE, Boolean.TRUE);
            put(KSEFeature.MULTIPLE_DATA_SOURCE_COMBINATION, Boolean.TRUE);
            put(KSEFeature.CHECKPOINT, Boolean.TRUE);
            put(KSEFeature.REPORT_HISTORY, Boolean.TRUE);
            put(KSEFeature.EXPORT_JUNIT_REPORT, Boolean.TRUE);
            put(KSEFeature.TEST_OBJECT_REFACTORING, Boolean.TRUE);
            put(KSEFeature.EXPORT_TEST_ARTIFACTS, Boolean.TRUE);
            put(KSEFeature.IMPORT_TEST_ARTIFACTS, Boolean.TRUE);
            put(KSEFeature.DYNAMIC_TEST_SUITE, Boolean.TRUE);
            put(KSEFeature.CONSOLE_LOG_CUSTOMIZATION, Boolean.TRUE);
            put(KSEFeature.DEBUG_MODE, Boolean.TRUE);
            put(KSEFeature.SOURCE_CODE_FOR_DEBUGGING, Boolean.TRUE);
            put(KSEFeature.CONFIGURE_USAGE_TRACKING, Boolean.TRUE);
            put(KSEFeature.SSL_CLIENT_CERTIFICATE, Boolean.TRUE);
            put(KSEFeature.GIT_SSH, Boolean.TRUE);
            put(KSEFeature.PRIVATE_PLUGINS, Boolean.TRUE);
            put(KSEFeature.CREATE_GENERIC_PROJECT_TYPE, Boolean.TRUE);
            put(KSEFeature.OVERRIDE_TESTOPS_AUTHENTICATION, Boolean.TRUE);
            put(KSEFeature.RECORDER_RUN_FROM_SELECTED_STEP, Boolean.TRUE);
            put(KSEFeature.RECORDER_RUN_SELECTED_STEPS, Boolean.TRUE);
            put(KSEFeature.LAUNCH_ARGUMENTS_SETTINGS, Boolean.TRUE);
            put(KSEFeature.TEST_CASE_RUN_FROM_SELECTED_STEP, Boolean.TRUE);
            put(KSEFeature.TEST_CASE_DEBUG_FROM_SELECTED_STEP, Boolean.TRUE);
            put(KSEFeature.TEST_CASE_TOGGLE_STEP, Boolean.TRUE);
            put(KSEFeature.RERUN_IMMEDIATELY, Boolean.TRUE);
            put(KSEFeature.TEST_SUITE_COLLECTION_EXECUTION_EMAIL, Boolean.TRUE);
            put(KSEFeature.WINDOWS_NATIVE_RECORDER, Boolean.TRUE);
            put(KSEFeature.IMPORT_OPENAPI, Boolean.TRUE);
            put(KSEFeature.IMPORT_SELENIUM_IDE_V3, Boolean.FALSE);
            put(KSEFeature.FILTER_ALREADY_ADDED_TEST_CASES, Boolean.TRUE);
            put(KSEFeature.APPLITOOLS_PLUGIN, Boolean.TRUE);
        }
    };

    public Map<KSEFeature, Boolean> getCoreFeaturesMap() {
        return coreFeaturesMap;
    }

    public FeatureConfigurations() {
        loadFeatures();
    }

    protected void loadFeatures() {
        loadCoreFeatures();
        loadCustomFeatures();
    }

    protected void loadCoreFeatures() {
        if (coreFeatures == null) {
            coreFeatures = new Properties();
        }
        getCoreFeaturesMap().entrySet().stream().forEach(action -> {
            coreFeatures.put(action.getKey().name(), action.getValue().toString());
        });
    }

    protected void loadCustomFeatures() {
        if (customFeatures == null) {
            customFeatures = new Properties();
        }
    }

    public boolean canUse(KSEFeature feature) {
        return canUse(feature.name());
    }

    @Override
    public boolean canUse(String featureKey) {
        boolean isCustomFeature = customFeatures.containsKey(featureKey);
        if (isCustomFeature && getBoolean(customFeatures, featureKey)) {
            return true;
        }

        boolean isValidFeature = coreFeatures.containsKey(featureKey);
        if (!isValidFeature) {
            return false;
        }

        boolean hasKSELicense = LicenseUtil.isNotFreeLicense();
        boolean isKSEFeature = getBoolean(coreFeatures, featureKey);
        boolean isFreeFeature = !isKSEFeature;
        return isFreeFeature || (isKSEFeature && hasKSELicense);
    }

    @Override
    public void enable(String customFeature) {
        boolean isValidCustomFeature = !coreFeatures.containsKey(customFeature);
        if (!isValidCustomFeature) {
            return;
        }
        customFeatures.setProperty(customFeature, Boolean.TRUE.toString());
    }

    @Override
    public void disable(String customFeature) {
        boolean isValidCustomFeature = !coreFeatures.containsKey(customFeature);
        if (!isValidCustomFeature) {
            return;
        }
        customFeatures.setProperty(customFeature, Boolean.FALSE.toString());
    }

    @Override
    public void clear() {
        customFeatures.clear();
    }

    private boolean getBoolean(Properties features, String featureKey) {
        return Boolean.valueOf(features.getProperty(featureKey));
    }
}