package com.kms.katalon.execution.video;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.kms.katalon.core.constants.StringConstants;
import com.kms.katalon.core.helper.screenrecorder.VideoRecorder;
import com.kms.katalon.core.helper.screenrecorder.VideoRecorderBuilder;
import com.kms.katalon.core.helper.screenrecorder.VideoRecorderException;
import com.kms.katalon.core.helper.screenrecorder.VideoSubtitleWriter;
import com.kms.katalon.core.logging.LogLevel;
import com.kms.katalon.core.logging.XmlLogRecord;
import com.kms.katalon.entity.report.ReportEntity;
import com.kms.katalon.execution.configuration.IRunConfiguration;
import com.kms.katalon.execution.launcher.listener.LauncherEvent;
import com.kms.katalon.execution.launcher.listener.LauncherListener;
import com.kms.katalon.execution.launcher.listener.LauncherNotifiedObject;
import com.kms.katalon.execution.launcher.result.LauncherStatus;
import com.kms.katalon.execution.logging.LogEvaluator;
import com.kms.katalon.execution.setting.VideoRecorderSetting;
import com.kms.katalon.logging.LogUtil;

public class VideoRecorderService implements LauncherListener, LogEvaluator {
    private int logDepth = 0;

    private int testCaseIndex = 0;

    private VideoRecorder videoRecorder;

    private VideoRecorderSetting videoSetting;

    private LogLevel currentTestCaseResult = LogLevel.NOT_RUN;

    private IRunConfiguration runConfig;

    private VideoSubtitleWriter videoSubtitleWriter;

    private long videoStartTime;

    private long actionStartTime;

    private String currentActionDescription = StringUtils.EMPTY;

    public VideoRecorderService(IRunConfiguration runConfig) {
        this.runConfig = runConfig;

        getVideoSetting(runConfig);
    }

    private void getVideoSetting(IRunConfiguration runConfig) {
        @SuppressWarnings("unchecked")
        Map<String, Object> reportSettings = (Map<String, Object>) runConfig.getExecutionSetting()
                .getGeneralProperties()
                .get(StringConstants.CONF_PROPERTY_REPORT);
        videoSetting = (VideoRecorderSetting) reportSettings.get(StringConstants.CONF_PROPERTY_VIDEO_RECORDER_OPTION);
    }

    @Override
    public void handleLauncherEvent(LauncherEvent event, LauncherNotifiedObject notifiedObject) {
        if (!isRecordingAllowed()) {
            return;
        }

        switch (event) {
            case UPDATE_RECORD:
                handleLogUpdated(notifiedObject);
                return;
            case UPDATE_STATUS:
                handleLauncherStatus(notifiedObject);
                return;
            default:
                return;
        }
    }

    private boolean isRecordingAllowed() {
        return videoSetting.isEnable()
                && (videoSetting.isAllowedRecordIfFailed() || videoSetting.isAllowedRecordIfPassed());
    }

    private void handleLauncherStatus(LauncherNotifiedObject notifiedObject) {
        LauncherStatus status = (LauncherStatus) notifiedObject.getObject();
        if (status == LauncherStatus.TERMINATED) {
            stopVideoRecording();
            deleteVideo();
        }
    }

    private void handleLogUpdated(LauncherNotifiedObject notifiedObject) {
        XmlLogRecord logRecord = (XmlLogRecord) notifiedObject.getObject();

        LogLevel logLevel = LogLevel.valueOf(logRecord.getLevel().getName());
        if (logLevel == null) {
            return;
        }

        switch (logLevel) {
            case START:
                logDepth++;
                if (isStartTestCaseLog(logRecord) && isLogUnderTestCaseMainLevel(runConfig, logDepth)) {
                    initVideoRecorder(logRecord);
                    startVideoRecording();
                    testCaseIndex++;
                }

                if (isStartStep(logRecord)) {
                    writeSub();

                    actionStartTime = System.currentTimeMillis();
                    currentActionDescription = (String) logRecord.getProperties()
                            .get(StringConstants.XML_LOG_DESCRIPTION_PROPERTY);
                }

                break;
            case END:
                if (isEndTestCaseLog(logRecord) && isLogUnderTestCaseMainLevel(runConfig, logDepth)) {
                    stopVideoRecording();
                    switch (currentTestCaseResult) {
                        case FAILED:
                            if (!videoSetting.isAllowedRecordIfFailed()) {
                                deleteVideo();
                            }
                            break;
                        case PASSED:
                            if (!videoSetting.isAllowedRecordIfPassed()) {
                                deleteVideo();
                            }
                            break;
                        default:
                            deleteVideo();
                    }
                    currentTestCaseResult = LogLevel.NOT_RUN;
                }
                logDepth--;
                break;
            default:
                if (LogLevel.getResultLogs().contains(logLevel) && isLogUnderTestCaseMainLevel(runConfig, logDepth)) {
                    currentTestCaseResult = logLevel;
                }
                break;
        }
    }

    private void deleteVideo() {
        if (videoRecorder != null) {
            videoRecorder.delete();
        }
        if (videoSubtitleWriter != null) {
            videoSubtitleWriter.delete();
        }
    }

    private void writeSub() {
        try {
            if (StringUtils.isEmpty(currentActionDescription) || videoSubtitleWriter == null) {
                return;
            }
            videoSubtitleWriter.writeSub(actionStartTime - videoStartTime, System.currentTimeMillis() - videoStartTime,
                    currentActionDescription);
        } catch (IOException e) {
            LogUtil.logError(e);
        }
    }

    protected void initVideoRecorder(XmlLogRecord logRecord) {
        try {
            String videoName = String.format("test_%d", testCaseIndex + 1);
            String videoFolderName = new File(runConfig.getExecutionSetting().getFolderPath(),
                    ReportEntity.VIDEO_RECORDED_FOLDER).getAbsolutePath();
            videoRecorder = VideoRecorderBuilder.get()
                    .setVideoConfig(videoSetting.toVideoConfiguration())
                    .setOutputDirLocation(videoFolderName)
                    .setOutputVideoName(videoName)
                    .create();

            videoSubtitleWriter = new VideoSubtitleWriter(new File(videoFolderName, videoName).getAbsolutePath());
        } catch (VideoRecorderException e) {
            LogUtil.printAndLogError(e);
        }
    }

    protected void startVideoRecording() {
        if (videoRecorder == null) {
            return;
        }
        try {
            videoStartTime = actionStartTime = System.currentTimeMillis();
            videoRecorder.start();
        } catch (VideoRecorderException e) {
            LogUtil.printAndLogError(e);
        }
    }

    protected void stopVideoRecording() {
        if (videoRecorder == null || !videoRecorder.isStarted()) {
            return;
        }
        try {
            writeSub();
            videoRecorder.stop();
        } catch (VideoRecorderException e) {
            LogUtil.printAndLogError(e);
        }
    }
}
