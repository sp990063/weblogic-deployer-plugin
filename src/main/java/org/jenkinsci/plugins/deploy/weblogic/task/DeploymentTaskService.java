package org.jenkinsci.plugins.deploy.weblogic.task;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import org.jenkinsci.plugins.deploy.weblogic.data.DeploymentTaskResult;
import org.jenkinsci.plugins.deploy.weblogic.data.DeploymentTask;
import org.jenkinsci.plugins.deploy.weblogic.exception.DeploymentTaskException;

public interface DeploymentTaskService {

	/**
	 * 
	 * @param task
	 */
	DeploymentTaskResult perform(DeploymentTask task, String globalJdk, AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher) throws DeploymentTaskException;

}