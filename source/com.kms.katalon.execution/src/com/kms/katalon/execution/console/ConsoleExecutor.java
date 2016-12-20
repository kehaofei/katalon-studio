package com.kms.katalon.execution.console;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.execution.collector.ConsoleOptionCollector;
import com.kms.katalon.execution.console.entity.ConsoleOption;
import com.kms.katalon.execution.console.entity.ConsoleOptionContributor;
import com.kms.katalon.execution.console.entity.LauncherOptionParser;
import com.kms.katalon.execution.console.entity.TestSuiteLauncherOptionParser;
import com.kms.katalon.execution.console.entity.TestSuiteCollectionLauncherOptionParser;
import com.kms.katalon.execution.constants.StringConstants;
import com.kms.katalon.execution.exception.InvalidConsoleArgumentException;
import com.kms.katalon.execution.launcher.manager.LauncherManager;

import joptsimple.OptionSet;

public class ConsoleExecutor {

    private List<LauncherOptionParser> launcherOptions;

    private List<ConsoleOptionContributor> optionalOptions;

    public ConsoleExecutor() {
        launcherOptions = Arrays.asList(new TestSuiteLauncherOptionParser(),
                new TestSuiteCollectionLauncherOptionParser());
        optionalOptions = ConsoleOptionCollector.getInstance().getOptionContributors();
    }

    public List<ConsoleOption<?>> getAllConsoleOptions() {
        List<ConsoleOption<?>> consoleOptions = new ArrayList<>();

        consoleOptions.addAll(getConsoleOptionInContributors(launcherOptions));
        consoleOptions.addAll(getConsoleOptionInContributors(optionalOptions));

        return consoleOptions;
    }

    private List<ConsoleOption<?>> getConsoleOptionInContributors(
            List<? extends ConsoleOptionContributor> contributors) {
        List<ConsoleOption<?>> consoleOptions = new ArrayList<>();
        for (ConsoleOptionContributor contributor : contributors) {
            consoleOptions.addAll(contributor.getConsoleOptionList());
        }
        return consoleOptions;
    }

    public void execute(ProjectEntity projectEntity, OptionSet optionSet) throws Exception {
        setValueForOptionalOptions(optionalOptions, optionSet);

        LauncherOptionParser launcherOption = new LauncherOptionSelector().getSelectedOption(optionSet);
        for (ConsoleOption<?> consoleOption : launcherOption.getConsoleOptionList()) {
            if (optionSet.has(consoleOption.getOption())) {
                launcherOption.setArgumentValue(consoleOption,
                        String.valueOf(optionSet.valueOf(consoleOption.getOption())));
            }
        }

        LauncherManager launcherManager = LauncherManager.getInstance();
        launcherManager.addLauncher(launcherOption.getConsoleLauncher(projectEntity, launcherManager));
    }

    private void setValueForOptionalOptions(List<ConsoleOptionContributor> optionContributors, OptionSet optionSet)
            throws Exception {
        for (ConsoleOptionContributor contributor : optionContributors) {
            for (ConsoleOption<?> consoleOption : contributor.getConsoleOptionList()) {
                validateRequiredArgument(consoleOption, optionSet);
                String optionName = consoleOption.getOption();
                if (optionSet.has(optionName) && consoleOption.hasArgument()) {
                    contributor.setArgumentValue(consoleOption, (String) optionSet.valueOf(optionName));
                }
            }
        }
    }

    private void validateRequiredArgument(ConsoleOption<?> consoleOption, OptionSet optionSet)
            throws InvalidConsoleArgumentException {
        if (consoleOption.isRequired() && !optionSet.has(consoleOption.getOption())) {
            String optionName = consoleOption.getOption();
            throw new InvalidConsoleArgumentException(
                    MessageFormat.format(StringConstants.MNG_PRT_MISSING_REQUIRED_ARG, optionName));
        }
    }

    private class LauncherOptionSelector {
        public LauncherOptionParser getSelectedOption(OptionSet optionSet) throws InvalidConsoleArgumentException {
            Iterator<LauncherOptionParser> iterator = launcherOptions.iterator();
            while (iterator.hasNext()) {
                LauncherOptionParser launcherOption = iterator.next();
                if (evaluate(launcherOption, optionSet)) {
                    return launcherOption;
                }
            }
            throw buildMissingLauncherArgumentException();
        }

        private InvalidConsoleArgumentException buildMissingLauncherArgumentException() {
            Iterator<LauncherOptionParser> iterator = launcherOptions.iterator();
            StringBuilder messageBuilder = new StringBuilder();
            while (iterator.hasNext()) {
                LauncherOptionParser launcherOption = iterator.next();
                if (StringUtils.isNotEmpty(messageBuilder.toString())) {
                    messageBuilder.append(" or ");
                }
                messageBuilder.append(getOptions(launcherOption));
            }
            return new InvalidConsoleArgumentException(
                    MessageFormat.format(StringConstants.MNG_PRT_MISSING_REQUIRED_ARG, messageBuilder.toString()));
        }

        private String getOptions(LauncherOptionParser launcherOption) {
            Iterator<ConsoleOption<?>> iterator = launcherOption.getConsoleOptionList().iterator();
            StringBuilder optionStringBuilder = new StringBuilder();
            while (iterator.hasNext()) {
                if (StringUtils.isNotEmpty(optionStringBuilder.toString())) {
                    optionStringBuilder.append(", ");
                }
                optionStringBuilder.append(iterator.next().getOption());
            }
            return MessageFormat.format("[{0}]", optionStringBuilder.toString());
        }

        private boolean evaluate(LauncherOptionParser optionParser, OptionSet optionSet)
                throws InvalidConsoleArgumentException {
            Iterator<ConsoleOption<?>> iterator = optionParser.getConsoleOptionList().iterator();
            boolean anyRequiesExisted = false;
            while (iterator.hasNext()) {
                ConsoleOption<?> consoleOption = iterator.next();
                if (!consoleOption.isRequired()) {
                    continue;
                }
                if (optionSet.has(consoleOption.getOption())) {
                    anyRequiesExisted = true;
                    continue;
                }

                if (anyRequiesExisted) {
                    throw new InvalidConsoleArgumentException(MessageFormat
                            .format(StringConstants.MNG_PRT_MISSING_REQUIRED_ARG, consoleOption.getOption()));
                }
                return false;
            }
            return true;
        }
    }
}
