package com.kms.katalon.console.application;

import java.util.Map;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.kms.katalon.console.application.ApplicationRunningMode.RunningMode;

/**
 * This class controls all aspects of the application's execution
 */
public class Application implements IApplication {
	private static final String CONSOLE_RUNNING_MODE_ARGUMENT = "-runMode=console";
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.
	 * IApplicationContext)
	 */
	public Object start(IApplicationContext context) {
		final Map<?, ?> args = context.getArguments();
		final String[] appArgs = (String[]) args.get("application.args");
		for (final String arg : appArgs) {
			if (arg.toLowerCase().equals(CONSOLE_RUNNING_MODE_ARGUMENT.toLowerCase())) {
				ApplicationRunningMode runningMode = ApplicationRunningMode.getInstance();
				runningMode.setRunnningMode(RunningMode.Console);
				runningMode.setRunArguments(appArgs);
				break;
			}
		}

		Display display = PlatformUI.createDisplay();
		try {
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART)
				return IApplication.EXIT_RESTART;
			else
				return IApplication.EXIT_OK;
		} catch (Exception e) {
			e.printStackTrace();
			return IApplication.EXIT_OK;
		} finally {
			display.dispose();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
	}
}