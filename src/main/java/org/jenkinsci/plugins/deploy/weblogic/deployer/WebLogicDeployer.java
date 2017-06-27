/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.deployer;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicAuthenticationMode;
import org.jenkinsci.plugins.deploy.weblogic.data.WebLogicStageMode;
import org.jenkinsci.plugins.deploy.weblogic.properties.WebLogicDeploymentPluginConstantes;
import org.jenkinsci.plugins.deploy.weblogic.util.DeployerClassPathUtils;
import org.jenkinsci.plugins.deploy.weblogic.util.ParameterValueResolver;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Run.RunnerAbortedException;
import hudson.util.ArgumentListBuilder;

/**
 * @author rchaumie
 *
 */
public class WebLogicDeployer {

	/**
	 * 
	 * @param parameter
	 * @return
	 */
	public static final String[] getWebLogicCommandLine(WebLogicDeployerParameters parameter, EnvVars envars) {
		ArgumentListBuilder args = new ArgumentListBuilder();
        
		processJavaLauncher(parameter, args);
		
        // mode debug force
        args.add("-debug");
        
        //Cas d'une application stage uniquement au deploiement
        if(! WebLogicCommand.UNDEPLOY.equals(parameter.getCommand()) && !parameter.isLibrary()){
        	
        	// Job level configuration
            if(! WebLogicStageMode.bydefault.equals(parameter.getStageMode())){
            	args.add("-"+parameter.getStageMode().name());
            } else {
            	// TODO stage provoque la generation du config....
            	args.add("-stage");
//            	args.add("-nostage");
            }
        }
        
        args.add("-remote");
        args.add("-verbose");
        
        //Cas d'une application
        // Pour une librairie on copie sur le serveur puis on deploie
        if(! WebLogicCommand.UNDEPLOY.equals(parameter.getCommand()) && !parameter.isLibrary()){
        	args.add("-upload");
        }
        
        if(parameter.isSilentMode()){
        	args.add("-noexit");
        }
        
        args.add("-name");
        String targetedDeploymentName = StringUtils.isNotBlank(parameter.getDeploymentName()) ? parameter.getDeploymentName() : parameter.getArtifactName();
        args.add(
    		// targetedDeploymentName
    		ParameterValueResolver.resolveEnvVars(targetedDeploymentName, envars)
		);
        
        if(StringUtils.isNotBlank(parameter.getSource())) {
        	args.add("-source");
        	args.add("\'"+parameter.getSource()+"\'");
        }

        args.add("-targets");
        args.add(ParameterValueResolver.resolveEnvVar(parameter.getDeploymentTargets(), envars));
        args.add("-adminurl");
        
        String adminUrl = ParameterValueResolver.resolveEnvVar(parameter.getEnvironment().getHost(), envars)+":"+ParameterValueResolver.resolveEnvVar(parameter.getEnvironment().getPort(), envars);
        if(parameter.getProtocol() != null){
        	args.add(parameter.getProtocol() + "://" + adminUrl);
        } else {
        	args.add("t3://" + adminUrl);
        }
        
        // Authentication by keystore can be possible
        switch(parameter.getEnvironment().getAuthMode() != null ? parameter.getEnvironment().getAuthMode() : WebLogicAuthenticationMode.BY_LOGIN){
        	case BY_KEY:
        		args.add("-userconfigfile");
                args.add(parameter.getEnvironment().getUserconfigfile());
                args.add("-userkeyfile");
                args.add(parameter.getEnvironment().getUserkeyfile());
        		break;
        	default :
        		args.add("-user");
                args.add(ParameterValueResolver.resolveEnvVar(parameter.getEnvironment().getLogin(), envars));
                args.add("-password");
                args.add(ParameterValueResolver.resolveEnvVar(parameter.getEnvironment().getPassword(), envars));
        		break;
        }
        
        args.add("-"+parameter.getCommand().getValue());
		
        if(parameter.isLibrary()) {
        	args.add("-library");
        }
        
		if (StringUtils.isNotBlank(parameter.getDeploymentPlan())) {
        	args.add("-plan");
        	args.add(parameter.getDeploymentPlan());
        }

        return args.toCommandArray();
	}

    /**
     *
     * @param parameters
     * @param commandLine
     * @param envars
     * @return the complete command line as an Array of String
     */
	public static final String[] getWebLogicCommandLine(WebLogicDeployerParameters parameters, String commandLine, EnvVars envars) {
		
		ArgumentListBuilder args = new ArgumentListBuilder();
		
		processJavaLauncher(parameters, args);
		
		for(String param : StringUtils.split(commandLine, ' ')){
			args.add(ParameterValueResolver.resolveEnvVar(param, envars));
		}
		
		return args.toCommandArray();
	}
	
	/**
	 * 
	 * @param parameter
	 * @param args
	 */
	private static void processJavaLauncher(WebLogicDeployerParameters parameter, ArgumentListBuilder args) {
		//jdk
		if(parameter.getUsedJdk() == null) {
			parameter.getListener().error("[WeblogicDeploymentPlugin] - No JDK selected to deploy artifact.");
		    throw new RunnerAbortedException();
		}

		// On prend le path de l'exe sur le remote
		args.add(new FilePath(parameter.getBuild().getBuiltOn().getChannel(), parameter.getUsedJdk().getHome().concat("/bin/java")).getRemote());

		//java options specifique
		if(StringUtils.isNotBlank(parameter.getJavaOpts())){
			//On parse l'ensemble des options et on les rajoute des le args[]
		    String[] javaOptions = StringUtils.split(parameter.getJavaOpts(), ' ');
		    args.add(javaOptions);
		}
				
		//gestion du classpath
		args.add("-cp");
		        
		// remoting.jar
		if(StringUtils.isBlank(parameter.getClasspath())){
			parameter.getListener().error("[WeblogicDeploymentPlugin] - Classpath is not set. Please configure correctly the plugin.");
			throw new RunnerAbortedException();
		}
		// On recalcule le classpath à partir du workspace si on est en remote
		String	remotingJar = DeployerClassPathUtils.formatAndCheckClasspath(parameter.getClasspath(), parameter.getBuild(), parameter.getListener());
		args.add(remotingJar);
		args.add(WebLogicDeploymentPluginConstantes.WL_WEBLOGIC_API_DEPLOYER_MAIN_CLASS);
		        
	}
}
