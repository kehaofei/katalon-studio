package com.kms.katalon.execution.console.entity;

import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.execution.exception.ExecutionException;
import com.kms.katalon.execution.exception.InvalidConsoleArgumentException;
import com.kms.katalon.execution.launcher.IConsoleLauncher;
import com.kms.katalon.execution.launcher.ILauncher;
import com.kms.katalon.execution.launcher.manager.LauncherManager;

public interface LauncherOptionParser extends ConsoleOptionContributor {
    IConsoleLauncher getConsoleLauncher(ProjectEntity project, LauncherManager manager)
            throws ExecutionException, InvalidConsoleArgumentException;

    ILauncher getIDELauncher(ProjectEntity project, LauncherManager manager)
            throws ExecutionException, InvalidConsoleArgumentException;
}