/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.data;

import hudson.model.AbstractDescribableImpl;
import hudson.model.JDK;

import java.io.Serializable;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Raphael
 *
 */
public class DeploymentTask  extends AbstractDescribableImpl<DeploymentTask> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3924420945973321189L;

	/**
	 * Identify the task
	 */
	private String id;
	
	/**
     * Identifies {@link WeblogicEnvironment} to be used.
     */
	private String weblogicEnvironmentTargetedName;
	
	/**
	 * The deployment name. If null the artifact name will be used
	 */
	private String deploymentName;
	
	/**
	 * Deployments target. By default AdminServer
	 */
	private String deploymentTargets = "AdminServer";
	
	/**
	 * The artifact has to deploy as a library
	 */
	private boolean isLibrary;
	
	/**
	 * The regular expression applied to filter the resource to deploy if
	 * more than one resource is found.
	 */
	private String builtResourceRegexToDeploy;
	
	/**
	 * Root directory containing the resource to deploy.
	 * Used for freestyle project when the resource is not located under the workspace
	 */
	private String baseResourcesGeneratedDirectory;
	
	/**
	 * The task name (optional)
	 */
	private String taskName;
	
	/**
	 * The JDK to use
	 */
	private JDK jdk;
	
	private WebLogicStageMode stageMode;
	
	/**
	 * The command line to execute
	 */
	private String commandLine;
	
	/**
	 * Name of the deployment plan to use when deploying the resource
	 */
	private final String deploymentPlan;
	
	/**
	 * The protocol to use with operation. By default t3
	 */
	private final WebLogicOperationProcotol protocol;

    /**
     * Invoke only during data backup
     * @param id
     * @param taskName
     * @param weblogicEnvironmentTargetedName
     * @param deploymentName
     * @param deploymentTargets
     * @param isLibrary
     * @param builtResourceRegexToDeploy
     * @param baseResourcesGeneratedDirectory
     * @param jdkName
     * @param jdkHome
     * @param stageMode
     * @param commandLine
     * @param deploymentPlan
     * @param protocol
     */
	@DataBoundConstructor
	public DeploymentTask(String id, String taskName, String weblogicEnvironmentTargetedName, String deploymentName, 
  		String deploymentTargets, boolean isLibrary, String builtResourceRegexToDeploy, String baseResourcesGeneratedDirectory, String jdkName, String jdkHome, 
  		WebLogicStageMode stageMode,
  		String commandLine, String deploymentPlan, WebLogicOperationProcotol protocol) {
		if (id == null) {
			this.id = RandomStringUtils.randomAlphanumeric(10);
		} else {
			this.id = id;
		}
		this.taskName = taskName;
		this.weblogicEnvironmentTargetedName = weblogicEnvironmentTargetedName;
		this.deploymentName = deploymentName;
		this.deploymentTargets = deploymentTargets;
		this.isLibrary = isLibrary;
		this.builtResourceRegexToDeploy = builtResourceRegexToDeploy;
		this.baseResourcesGeneratedDirectory = baseResourcesGeneratedDirectory;
		if(StringUtils.isNotBlank(jdkName)){
			this.jdk = new JDK(jdkName, jdkHome);
		}
		this.stageMode = stageMode;
		this.commandLine = commandLine;
      	this.deploymentPlan = deploymentPlan;
      	this.protocol = protocol != null ? protocol : WebLogicOperationProcotol.t3;
	}
	
	public DeploymentTask(DeploymentTask deploymentTask) {
		this.id = deploymentTask.getId();
		this.taskName = deploymentTask.getTaskName();
		this.weblogicEnvironmentTargetedName = deploymentTask.getWeblogicEnvironmentTargetedName();
		this.deploymentName = deploymentTask.getDeploymentName();
		this.deploymentTargets = deploymentTask.getDeploymentTargets();
		this.isLibrary = deploymentTask.getIsLibrary();
		this.builtResourceRegexToDeploy = deploymentTask.getBuiltResourceRegexToDeploy();
		this.baseResourcesGeneratedDirectory = deploymentTask.getBaseResourcesGeneratedDirectory();
		this.jdk = deploymentTask.getJdk();
		this.stageMode = deploymentTask.getStageMode();
		this.commandLine = deploymentTask.getCommandLine();
	  	this.deploymentPlan = deploymentTask.getDeploymentPlan();
	  	this.protocol = deploymentTask.getProtocol();
	}
	
	
	
	/* (non-Javadoc)
	 * @see hudson.model.AbstractDescribableImpl#getDescriptor()
	 */
	@Override
	public DeploymentTaskDescriptor getDescriptor() {
		return (DeploymentTaskDescriptor) super.getDescriptor();
	}

	/**
	 * 
	 * @return
	 */
	public String getWeblogicEnvironmentTargetedName() {
		return weblogicEnvironmentTargetedName;
	}
	
	/**
	 * 	
	 * @return
	 */
	public String getDeploymentName() {
		return deploymentName;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getDeploymentTargets() {
		return deploymentTargets;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean getIsLibrary() {
		return isLibrary;
	}

	/**
	 * @return the builtResourceRegexToDeploy
	 */
	public String getBuiltResourceRegexToDeploy() {
		return builtResourceRegexToDeploy;
	}

	/**
	 * @param builtResourceRegexToDeploy the builtResourceRegexToDeploy to set
	 */
	public void setBuiltResourceRegexToDeploy(String builtResourceRegexToDeploy) {
		this.builtResourceRegexToDeploy = builtResourceRegexToDeploy;
	}
	
	/**
	 * @return the baseResourcesGeneratedDirectory
	 */
	public String getBaseResourcesGeneratedDirectory() {
		return baseResourcesGeneratedDirectory;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the taskName
	 */
	public String getTaskName() {
		return taskName;
	}

	/**
	 * @return the jdk
	 */
	public JDK getJdk() {
		return jdk;
	}

	/**
	 * @return the stageMode
	 */
	public WebLogicStageMode getStageMode() {
		return stageMode;
	}

	/**
	 * 
	 * @return
	 */
	public String getCommandLine() {
		return commandLine;
	}
	
	/**
	 * @return the deploymentPlan
	 */
	public String getDeploymentPlan() {
		return deploymentPlan;
	}


	/**
	 * 
	 * @param deploymentTargets
	 */
	public void setDeploymentTargets(String deploymentTargets) {
		this.deploymentTargets = deploymentTargets;
	}

	public WebLogicOperationProcotol getProtocol() {
		return protocol;
	}
	
}
