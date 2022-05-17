/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.init.Initializer;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.AutoCompletionCandidates;
import hudson.model.BuildListener;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.model.JDK;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Saveable;
import hudson.model.TopLevelItem;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.DescribableList;
import hudson.util.FormValidation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deploy.weblogic.configuration.WeblogicDeploymentConfiguration;
import org.jenkinsci.plugins.deploy.weblogic.data.DeploymentTask;
import org.jenkinsci.plugins.deploy.weblogic.data.DeploymentTaskResult;
import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicDeploymentStatus;
import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicOperationProcotol;
import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicPreRequisteStatus;
import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicStageMode;
import org.jenkinsci.plugins.deploy.weblogic.data.WeblogicEnvironment;
import org.jenkinsci.plugins.deploy.weblogic.data.policy.AbstractDeploymentPolicy;
import org.jenkinsci.plugins.deploy.weblogic.exception.DeploymentTaskException;
import org.jenkinsci.plugins.deploy.weblogic.exception.LoadingFileException;
import org.jenkinsci.plugins.deploy.weblogic.jdk.JdkToolService;
import org.jenkinsci.plugins.deploy.weblogic.properties.WebLogicDeploymentPluginConstantes;
import org.jenkinsci.plugins.deploy.weblogic.task.DeploymentTaskService;
import org.jenkinsci.plugins.deploy.weblogic.task.PreRequisiteStatusUnSuccesfullPredicate;
import org.jenkinsci.plugins.deploy.weblogic.task.TaskStatusUnSuccesfullPredicate;
import org.jenkinsci.plugins.deploy.weblogic.util.BuildCauseUtils;
import org.jenkinsci.plugins.deploy.weblogic.util.DeployerClassPathUtils;
import org.jenkinsci.plugins.deploy.weblogic.util.URLUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import com.google.inject.Inject;



/**
 * @author rchaumie
 *
 */
@Extension
public class WeblogicDeploymentPlugin extends Recorder {

	public static transient final String DEFAULT_JAVA_OPTIONS_DEPLOYER = "-Xms256M -Xmx256M";
	
	@Inject
	private DeploymentTaskService deploymentTaskService;
	
	/**
	 * Le build doit se terminer en erreur. Configurable.
	 */
	private boolean mustExitOnFailure = true;
	
	/**
	 * Le plugin stoppe les deploiements au premier echec
	 */
	private boolean forceStopOnFirstFailure = false;
	
	/**
	 * strategies de deploiement (rattache a un trigger de build)
	 */
	private transient List<String> selectedDeploymentStrategyIds;

	/**
	 * le deploiement est effectif uniquement si les sources ont changes
	 */
	private boolean isDeployingOnlyWhenUpdates;

	private boolean buildUnstableWhenDeploymentUnstable;

	/**
	 * Liste des deploiements dont depends l'execution du projet sur un job
	 */
	private String deployedProjectsDependencies;
	
	/**
	 * Deployment task list
	 */
	private List<DeploymentTask> tasks = new ArrayList<DeploymentTask>();

	/**
	 * Deployment policy list
	 *
	 * @since 3.5
	 */
	private DescribableList<AbstractDeploymentPolicy, Descriptor<AbstractDeploymentPolicy>> policies;
	
	public WeblogicDeploymentPlugin() {
		super();
	}
	
//	@Initializer
//	public static void load(){
//		JdkToolService.loadJdkToolAvailables();
//	}
	
	/**
	 * Invoque lors de la mise a jour des configurations des projets ayant active le plugin et de la mise a jour globale
	 * @param tasks
	 * @param mustExitOnFailure
	 * @param selectedDeploymentStrategyIds
	 * @param deployedProjectsDependencies
	 * @param isDeployingOnlyWhenUpdates
	 * @param forceStopOnFirstFailure
	 * @param buildUnstableWhenDeploymentUnstable
	 * @param weblogicEnvironmentTargetedName
	 * @param deploymentName
	 * @param deploymentTargets
	 * @param isLibrary
	 * @param builtResourceRegexToDeploy
	 * @param baseResourcesGeneratedDirectory
	 * @param deploymentPlan
	 * @param policies
	 * @since 2.0
	 */
	@DataBoundConstructor
    public WeblogicDeploymentPlugin(List<DeploymentTask> tasks, boolean mustExitOnFailure, List<String> selectedDeploymentStrategyIds, 
    		String deployedProjectsDependencies, boolean isDeployingOnlyWhenUpdates, boolean forceStopOnFirstFailure,
    		boolean buildUnstableWhenDeploymentUnstable, String weblogicEnvironmentTargetedName, String deploymentName, 
    		String deploymentTargets, boolean isLibrary, String builtResourceRegexToDeploy, String baseResourcesGeneratedDirectory, 
    		String deploymentPlan, List<AbstractDeploymentPolicy> policies) {
        // ATTENTION : Appele au moment de la sauvegarde : On conserve la compatibilite ascendante
		this.tasks = CollectionUtils.isNotEmpty(tasks) ? tasks : Arrays.asList(new DeploymentTask[]{
				new DeploymentTask(null, null, weblogicEnvironmentTargetedName, deploymentName, deploymentTargets, isLibrary,
						builtResourceRegexToDeploy, baseResourcesGeneratedDirectory , null, null, null, null, deploymentPlan, null)
				});
		this.mustExitOnFailure = mustExitOnFailure;
		this.selectedDeploymentStrategyIds = selectedDeploymentStrategyIds;
        this.deployedProjectsDependencies = deployedProjectsDependencies;
        this.isDeployingOnlyWhenUpdates = isDeployingOnlyWhenUpdates;
        this.forceStopOnFirstFailure = forceStopOnFirstFailure;
        this.buildUnstableWhenDeploymentUnstable = buildUnstableWhenDeploymentUnstable;
		this.policies = new DescribableList<AbstractDeploymentPolicy, Descriptor<AbstractDeploymentPolicy>>(Saveable.NOOP, Util.fixNull(policies));
    }

	protected Object readResolve() {
		if (CollectionUtils.isNotEmpty(selectedDeploymentStrategyIds)) {
			this.policies = new DescribableList<AbstractDeploymentPolicy, Descriptor<AbstractDeploymentPolicy>>(Saveable.NOOP, Util.fixNull(toDeploymentPolicyList(clearDeploymentStrategyIds(selectedDeploymentStrategyIds), isDeployingOnlyWhenUpdates)));
		}
		return this;
	}

	private List<AbstractDeploymentPolicy> toDeploymentPolicyList(List<String> deploymentStrategyIds, boolean deployingOnlyWhenUpdates) {
		List<AbstractDeploymentPolicy> policies = new ArrayList<AbstractDeploymentPolicy>();
		List<Descriptor<AbstractDeploymentPolicy>> descriptors = Jenkins.getInstance().getDescriptorList(AbstractDeploymentPolicy.class);
		for (Descriptor<AbstractDeploymentPolicy> descriptor : descriptors) {
			try {
				AbstractDeploymentPolicy policy = descriptor.clazz.getConstructor(Boolean.TYPE).newInstance(deployingOnlyWhenUpdates);
				for (String deploymentStrategyId : deploymentStrategyIds) {
					if (policy.getCauseClass().getName().equals(deploymentStrategyId)) {
						policies.add(policy);
					}
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return policies;
	}

	/**
	 * On ne controle la desactivation que si la strategie a ete definie
	 * gestion des classes privees : les tokens \$ sont transformees en $
	 */
	private List<String> clearDeploymentStrategyIds(List<String> deploymentStrategyIds) {
		List<String> cleared = new ArrayList<String>();
		for (String deploymentStrategyId : deploymentStrategyIds){
			cleared.add(StringUtils.remove(deploymentStrategyId, '\\'));
		}
		return cleared;
	}

	/**
	 * 
	 * @return
	 */
	public boolean getMustExitOnFailure() {
		return mustExitOnFailure;
	}
	
	/**
	 * @return the selectedDeploymentStrategyIds
	 */
	public List<String> getSelectedDeploymentStrategyIds() {
		return selectedDeploymentStrategyIds;
	}
	
	/**
	 * @return the deployedProjectsDependencies
	 */
	public String getDeployedProjectsDependencies() {
		return deployedProjectsDependencies;
	}
	
	/**
	 * @return the isDeployingOnlyWhenUpdates
	 */
	public boolean getIsDeployingOnlyWhenUpdates() {
		return isDeployingOnlyWhenUpdates;
	}

	/**
	 * @param isDeployingOnlyWhenUpdates the isDeployingOnlyWhenUpdates to set
	 */
	public void setDeployingOnlyWhenUpdates(boolean isDeployingOnlyWhenUpdates) {
		this.isDeployingOnlyWhenUpdates = isDeployingOnlyWhenUpdates;
	}
	
	/**
	 * @return the tasks
	 */
	public List<DeploymentTask> getTasks() {
		return tasks;
	}

	/**
	 * @return the policies
	 */
	public List<AbstractDeploymentPolicy> getPolicies() {
		return policies;
	}

	/**
	 * @return the forceStopOnFirstFailure
	 */
	public boolean getForceStopOnFirstFailure() {
		return forceStopOnFirstFailure;
	}

	/*
	 * (non-Javadoc)
	 * @see hudson.tasks.BuildStepCompatibilityLayer#getProjectAction(hudson.model.AbstractProject)
	 */
	@Override
	public Action getProjectAction(AbstractProject<?, ?> project) {
		return new PrintingWebLogicDeploymentLastSuccessResultAction(project);
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
	 */
	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {

        //Pre-requis ko , arret du traitement
        List<DeploymentTaskResult> results = new ArrayList<DeploymentTaskResult>();
        WebLogicPreRequisteStatus check = checkPreRequisites(build, listener);
        if(check != WebLogicPreRequisteStatus.OK){
        	results.add(new DeploymentTaskResult(check, WebLogicDeploymentStatus.DISABLED, null, null));
        	return exitPerformAction(build, listener, results);
        }

		// En attendant plus propre
		if(this.deploymentTaskService == null){
			this.deploymentTaskService = Jenkins.getInstance().getInjector().getInstance(DeploymentTaskService.class);
		}
		
		// Parcours des taches de deploiement
		for(DeploymentTask task : getTasks()){
			try {
				results.add(this.deploymentTaskService.perform(task, getDescriptor().getJdkSelected(), build, listener, launcher));
			} catch(DeploymentTaskException dte) {
				results.add(dte.getResult());
				if(getForceStopOnFirstFailure()){
					break;
				}
			}
		}
		
        return exitPerformAction(build, listener, results);
	}
	
	/**
	 * 
	 * @param build
	 * @param listener
	 * @return
	 */
	private WebLogicPreRequisteStatus checkPreRequisites(AbstractBuild<?, ?> build, BuildListener listener){
		
		//Verification desactivation plugin
		if(getDescriptor().isPluginDisabled()){
			listener.getLogger().println("[WeblogicDeploymentPlugin] - The plugin execution is disabled.");
			return WebLogicPreRequisteStatus.PLUGIN_DISABLED;
		}

		if (policies != null  && !policies.isEmpty() && !hasAtLeastOneBuildCauseChecked(build)) {
			listener.getLogger().println("[WeblogicDeploymentPlugin] - Current build cause(s) \"" + BuildCauseUtils.formatToString(build.getCauses()) + "\" do(es) not contain any of the configured \"Deployment Policies\". The plugin execution is disabled.");
			listener.getLogger().println("[WeblogicDeploymentPlugin] - Verify at \"Post-build Actions > Deploy the artifact to any Weblogic environments  > Deployment policies\" if all the appropriate policies are applied according to enabled build triggers.");
			return WebLogicPreRequisteStatus.OTHER_TRIGGER_CAUSE;
		}
		
		//Verification strategie relative a la gestion des sources (systematique (par defaut) / uniquement sur modification(actif) )
		if(isDeployingOnlyWhenUpdates && build.getChangeSet().isEmptySet()) {
			listener.getLogger().println("[WeblogicDeploymentPlugin] - No changes. The plugin execution is disabled.");
			return WebLogicPreRequisteStatus.NO_CHANGES;
		}
		
		
		// Verification condition de dependance remplie
		boolean satisfiedDependenciesDeployments = true;
		if(StringUtils.isNotBlank(deployedProjectsDependencies)){
			String[] listeDependances = StringUtils.split(StringUtils.trim(deployedProjectsDependencies), ',');
			for(int i = 0; i<listeDependances.length; i++){
				TopLevelItem item = Jenkins.getInstance().getItem(listeDependances[i]);
				if(item instanceof Job){
					WatchingWeblogicDeploymentAction deploymentAction = ((Job<?,?>) item).getLastBuild().getAction(WatchingWeblogicDeploymentAction.class);
					listener.getLogger().println("[WeblogicDeploymentPlugin] - Satisfying dependencies project involved: " + item.getName());
					if(deploymentAction != null && CollectionUtils.exists(deploymentAction.getResults(), new TaskStatusUnSuccesfullPredicate())){
						satisfiedDependenciesDeployments = false;
					}
				}
			}
			
			if(!satisfiedDependenciesDeployments){
				listener.getLogger().println("[WeblogicDeploymentPlugin] - Not satisfied project dependencies deployment. The plugin execution is disabled.");
				return WebLogicPreRequisteStatus.UNSATISFIED_DEPENDENCIES;
			}
		}
				
		// Verification build SUCCESS
		if (build.getResult().isWorseThan(Result.SUCCESS)) {
			listener.getLogger().println("[WeblogicDeploymentPlugin] - Build didn't finish successfully. The plugin execution is disabled.");
			return WebLogicPreRequisteStatus.BUILD_FAILED;
		}

		return WebLogicPreRequisteStatus.OK;		
	}

	/**
	 *
	 * @param build
	 * @return
	 */
	private boolean hasAtLeastOneBuildCauseChecked(AbstractBuild<?, ?> build) {
		for (Cause cause : build.getCauses()) {
			for (AbstractDeploymentPolicy policy : policies) {
				if (policy.getCauseClass().getName().equals(cause.getClass().getName())) {
					if (!policy.isDeployingOnlyWhenUpdates()) {
						return true;
					} else if (!build.getChangeSet().isEmptySet()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/*
	 * 	(non-Javadoc)
	 * @see hudson.model.AbstractDescribableImpl#getDescriptor()
	 */
	@Override
	public WeblogicDeploymentPluginDescriptor getDescriptor() {
		return (WeblogicDeploymentPluginDescriptor) super.getDescriptor();
	}
	
	/*
	 * (non-Javadoc)
	 * @see hudson.tasks.BuildStep#getRequiredMonitorService()
	 */
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Extension
	public static final class WeblogicDeploymentPluginDescriptor extends BuildStepDescriptor<Publisher> {
		
		private transient final Logger logger = Logger.getLogger(getClass().getName());
		
		public static transient final String PLUGIN_XSD_SCHEMA_CONFIG_FILE_PATH = WebLogicDeploymentPluginConstantes.PLUGIN_RESOURCES_PATH + "/defaultConfig/plugin-configuration.xsd";
		
		private String configurationFilePath;
		
		private boolean pluginDisabled;
		
		private transient WeblogicEnvironment[] weblogicEnvironments;
		
		/**
		 * Pattern des artifacts a exclure
		 */
		private String excludedArtifactNamePattern;
		
		/**
		 * 
		 */
		private String jdkSelected;
		
		/**
		 * classpath additionnel (librairie weblogic a utiliser)
		 */
		private String extraClasspath;
		
		/**
		 * 
		 */
		private String javaOpts;
		
		/**
		 * 
		 */
		
		private String workspace;
		
		/**
		 * 
		 */
		
		
		public WeblogicDeploymentPluginDescriptor(){
			super(WeblogicDeploymentPlugin.class);
			
			//on charge les annotations XStream
			Jenkins.XSTREAM2.processAnnotations(
	        		new Class[]{org.jenkinsci.plugins.deploy.weblogic.configuration.WeblogicDeploymentConfiguration.class, org.jenkinsci.plugins.deploy.weblogic.data.WeblogicEnvironment.class});
			
			//charge les donnees de configuration du plugin dans l'instance
			load();
			
			//initialisation specifique
			init();
			
		}
		
		/**
		 * customisation des donnees de conf
		 */
		private void init(){
			//gestion java option utilise par defaut si non charge
			if(StringUtils.isBlank(javaOpts)){
				javaOpts = DEFAULT_JAVA_OPTIONS_DEPLOYER;
			}
		}
		
		/**
		 * 
		 * @return
		 */
		public WeblogicEnvironment[] getWeblogicEnvironments() {
			
			if(weblogicEnvironments == null){
				loadWeblogicEnvironments();
			}
			
			try {
				if (this.getDescriptorFullUrl() != null) {
					this.workspace = this.getDescriptorFullUrl().split("descriptorByName")[0];	
				}	
			}catch (NullPointerException e) {
				this.workspace = "";
			}
			
						
			List<WeblogicEnvironment> wlEnvs = new ArrayList<WeblogicEnvironment>();
			
			for (WeblogicEnvironment wlEnv : weblogicEnvironments) {
				if (this.workspace.contains(wlEnv.getJobFolderPath())){
					wlEnvs.add(wlEnv);
				}
			}
			
			return wlEnvs.toArray(new WeblogicEnvironment[wlEnvs.size()]);
			
		}
		
		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#getDisplayName()
		 */
		@Override
		public String getDisplayName() {
			return Messages.WeblogicDeploymentPluginDescriptor_DisplayName();
		}

		/**
		 * 
		 * @return
		 */
		public String getConfigurationFilePath() {
			return configurationFilePath;
		}
		
		/**
		 * 
		 * @param configurationFilePath
		 */
		public void setConfigurationFilePath(String configurationFilePath) {
			this.configurationFilePath = configurationFilePath;
		}
		
		/**
		 * 		
		 * @return
		 */
		public boolean isPluginDisabled() {
			return pluginDisabled;
		}
		
		/**
		 * 
		 * @param pluginDisabled
		 */
		public void setPluginDisabled(boolean pluginDisabled) {
			this.pluginDisabled = pluginDisabled;
		}
		
		/**
		 * 
		 * @return
		 */
		public String getExcludedArtifactNamePattern() {
			return excludedArtifactNamePattern;
		}
		
		/**
		 * @return the extraClasspath
		 */
		public String getExtraClasspath() {
			return extraClasspath;
		}

		/**
		 * @param extraClasspath the extraClasspath to set
		 */
		public void setExtraClasspath(String extraClasspath) {
			this.extraClasspath = extraClasspath;
		}

		/**
		 * @return the javaOpts
		 */
		public String getJavaOpts() {
			return javaOpts;
		}

		/**
		 * @param javaOpts the javaOpts to set
		 */
		public void setJavaOpts(String javaOpts) {
			this.javaOpts = javaOpts;
		}
		
		/**
		 * 
		 * @return
		 */
		public String getJdkSelected() {
			return jdkSelected;
		}

		/**
		 * 
		 * @param jdkSelected
		 */
		public void setJdkSelected(String jdkSelected) {
			this.jdkSelected = jdkSelected;
		}

		public String getWorkspace() {
			return workspace;
		}

		public void setWorkspace(String workspace) {
			this.workspace = workspace;
		}

		/**
		 * @return the list of available stage modes
		 */
		public WebLogicStageMode[] getWeblogicStageModes() {
			return WebLogicStageMode.values();
		}
		
		/**
		 * @return the list of available protocols
		 */
		public WebLogicOperationProcotol[] getWeblogicOperationProtocols() {
			return WebLogicOperationProcotol.values();
		}
		
		/*
		 * (non-Javadoc)
		 * @see hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest, net.sf.json.JSONObject)
		 */
		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			
			pluginDisabled = json.getBoolean("pluginDisabled");
			excludedArtifactNamePattern = json.getString("excludedArtifactNamePattern");
			
			// Sauvegarde de la valeur par defaut
			if(StringUtils.isNotBlank(json.getString("extraClasspath"))){
				extraClasspath = json.getString("extraClasspath");
			} else {
				extraClasspath = DeployerClassPathUtils.getDefaultPathToWebLogicJar();
			}
			
			javaOpts = json.getString("javaOpts");
			
			// Sauvegarde du jdk selectionne
			jdkSelected = json.getString("jdkSelected");
			
			//Chargement des weblogicTargets
			configurationFilePath = json.getString("configurationFilePath");
			loadWeblogicEnvironments();
			
			//Sauvegarde de la configuration du plugin
			save();
			return true;
		}
		
		/**
		 * Charge les environnements weblogic declares dans le fichier de conf
		 */
		private void loadWeblogicEnvironments(){
			InputStream configurationFileInputStream =  null;
			
			try {
		        
				WeblogicDeploymentConfiguration weblogicDeploymentConfiguration =null;
		        
				if(StringUtils.isBlank(configurationFilePath)){
					return;
				}

                if(configurationFilePath.startsWith(URLUtils.HTTP_PROTOCOL_PREFIX)){
		        	URI uri = new URI(configurationFilePath);
		        	URL url = uri.toURL();
		        	configurationFileInputStream =  url.openStream();
		        } else if (new File(configurationFilePath).exists()) {
		        	configurationFileInputStream = new FileInputStream(new File(configurationFilePath));
		        } else {
		        	throw new LoadingFileException("The file content doesn't exists");
		        }
		        
		        weblogicDeploymentConfiguration = (WeblogicDeploymentConfiguration) Jenkins.XSTREAM2.fromXML(configurationFileInputStream);
		        
		        if(weblogicDeploymentConfiguration != null && ! ArrayUtils.isEmpty(weblogicDeploymentConfiguration.getWeblogicEnvironments())){
		        	weblogicEnvironments = weblogicDeploymentConfiguration.getWeblogicEnvironments();
		        }
		    
			} catch(Exception e){
	        	logger.log(Level.SEVERE, e.getMessage(), e);
	        	throw new RuntimeException("Unable to load file", e);
	        } finally {
        		IOUtils.closeQuietly(configurationFileInputStream);
        	}
		}

		/**
		 * Performs on-the-fly validation of the form field 'configurationFilePath'
		 * @param value
		 * @return
		 * @throws IOException
		 * @throws ServletException
		 */
        public FormValidation doCheckConfigurationFilePath(@QueryParameter String value) throws IOException, ServletException {

        	if(value.startsWith(URLUtils.HTTP_PROTOCOL_PREFIX)) {
        		if(! URLUtils.exists(value)){
        			return FormValidation.error("The url " + value + " can't be reached.");
        		}
        		return FormValidation.ok();
        	}
        	
        	if(! new File(value).exists()) {
        		return FormValidation.error("The file " + value + " does not exists.");
        	}
            return FormValidation.ok();
        }
        
        /**
         * Controle a la volee du champ 'extraClasspath'
         * @param value
         * @return
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckExtraClasspath(@QueryParameter String value) throws IOException, ServletException {
        	
        	if(value.length() == 0){
        		
        		// Si aucun jar specifie. On tente le WL_HOME.
        		// On verifie que la librairie existe bien
        		if(DeployerClassPathUtils.checkDefaultPathToWebLogicJar()){
        			return FormValidation.warning("By default, the "+WebLogicDeploymentPluginConstantes.WL_WEBLOGIC_LIBRARY_NAME+" library found into "+System.getenv(WebLogicDeploymentPluginConstantes.WL_HOME_ENV_VAR_NAME)+WebLogicDeploymentPluginConstantes.WL_HOME_LIB_DIR+" will be used.");
        		}
        		
        		return FormValidation.error("The weblogic library has to be filled in.");
        	}
        	
        	// If more than one library is specified, we check them each.
        	String[] libs = value.split(File.pathSeparator);
        	for(String lib : libs) {
        		if(! new File(lib).exists()) {
            		return FormValidation.error("The file " + lib + " does not exists.");
            	}	
        	}
        	
        	return FormValidation.ok();
        }
        
        /**
         * 
         * @param value
         * @return
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckJavaOpts(@QueryParameter String value) throws IOException, ServletException {
        	
        	if(value.length() == 0){
        		return FormValidation.warning("The default options -Xms256M -Xmx256M will be used.");
        	}
        	
        	return FormValidation.ok();
        }
        
//        /**
//         * This method provides auto-completion items for the 'jdkName' field.
//         * Stapler finds this method via the naming convention.
//         *
//         * @param value
//         *      The text that the user entered.
//         */
//        public AutoCompletionCandidates doAutoCompleteJdkName(@QueryParameter String value) {
//            AutoCompletionCandidates c = new AutoCompletionCandidates();
//            for (JDK jdk : JdkToolService.getJdkToolAvailables()) {
//                if (jdk.getName().contains(value.toLowerCase())) {
//                	c.add(jdk.getName());
//                }
//            }
//            return c;
//        }
//
//        @JavaScriptMethod
//        public String completeJdkHome(String jdkName) {
//        	JDK jdk = JdkToolService.getJDKByName(jdkName);
//        	if(jdk != null){
//        		 return jdk.getHome();
//        	}
//        	return "";
//        }
        
        /**
         * 
         * @param value
         * @return
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckJdkName(@QueryParameter String value) throws IOException, ServletException {
        	
        	if(value.length() == 0){
        		return FormValidation.error("The name is mandatory");
        	}
        	
        	return FormValidation.ok();
        }
        
        /**
         * 
         * @param value
         * @return
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckJdkHome(@QueryParameter String value) throws IOException, ServletException {
        	
        	if(value.length() == 0){
        		return FormValidation.error("The path home is mandatory");
        	}
        	
        	if(! new File(value).exists()) {
        		return FormValidation.error("The file " + value + " does not exists.");
        	}
        	
        	return FormValidation.ok();
        }
		/*
		 * (non-Javadoc)
		 * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
		 */
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
				return true;
		}

		public List<? extends Descriptor<AbstractDeploymentPolicy>> getDeploymentPolicyDescriptors() {
			return Jenkins.getInstance().getDescriptorList(AbstractDeploymentPolicy.class);
		}
	}

	/**
	 * 
	 * @param build
	 * @param listener
	 * @param results
	 * @return true
	 */
	private boolean exitPerformAction(AbstractBuild<?, ?> build, BuildListener listener, List<DeploymentTaskResult> results){

		boolean hasUnsuccessfullPrerequisite = CollectionUtils.exists(results, new PreRequisiteStatusUnSuccesfullPredicate());
        boolean hasUnsuccessfullTask = CollectionUtils.exists(results, new TaskStatusUnSuccesfullPredicate());

        if (hasUnsuccessfullPrerequisite) {
            if (buildUnstableWhenDeploymentUnstable) {
                build.setResult(Result.UNSTABLE);
            }
		} else if (mustExitOnFailure && hasUnsuccessfullTask) {
	        // On teste si au moins une des taches est KO
			build.setResult(Result.FAILURE);
		}
		
		//Ajout de la build action
		build.addAction(new WatchingWeblogicDeploymentAction(results, build));
		
		listener.getLogger().println("[INFO] ------------------------------------------------------------------------");
		listener.getLogger().println("[INFO] DEPLOYMENT " + (hasUnsuccessfullPrerequisite ? Result.UNSTABLE : (hasUnsuccessfullTask ? Result.FAILURE : Result.SUCCESS)));
		listener.getLogger().println("[INFO] ------------------------------------------------------------------------");
		return true;
	}

    public boolean isBuildUnstableWhenDeploymentUnstable() {
        return buildUnstableWhenDeploymentUnstable;
    }

    public void setBuildUnstableWhenDeploymentUnstable(boolean buildUnstableWhenDeploymentUnstable) {
        this.buildUnstableWhenDeploymentUnstable = buildUnstableWhenDeploymentUnstable;
    }

	
}
