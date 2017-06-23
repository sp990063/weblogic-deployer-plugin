/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.util;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deploy.weblogic.properties.WebLogicDeploymentPluginConstantes;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run.RunnerAbortedException;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;

/**
 * @author rchaumie
 *
 */
public class DeployerClassPathUtils {

	/**
	 * 
	 * @return
	 */
	public static boolean checkDefaultPathToWebLogicJar() {
		return new File(getDefaultPathToWebLogicJar()).exists();
	}
	
	/**
	 * 
	 * @return
	 */
	public static String getDefaultPathToWebLogicJar() {
		String envWlHome = System.getenv(WebLogicDeploymentPluginConstantes.WL_HOME_ENV_VAR_NAME);
		return FilenameUtils.normalize(envWlHome+WebLogicDeploymentPluginConstantes.WL_HOME_LIB_DIR+WebLogicDeploymentPluginConstantes.WL_WEBLOGIC_LIBRARY_NAME);
	}
	
	/**
	 * 
	 * @param classpath
	 * @param build
	 * @return
	 */
	public static String formatAndCheckClasspath(final String classpath, AbstractBuild<?, ?> build,  BuildListener listener){
		// execution sur un node : il faut reformatter le cp + verifier la presence
		if(! StringUtils.EMPTY.equalsIgnoreCase(build.getBuiltOnStr())){
			return formatAndCheckClasspathForNode(classpath, build,  listener);
		}
		// si c'est sur le master pour l'instant on ne fait que le check
		checkClasspath(classpath, build, listener);
		return classpath;
	}
	
	private static String formatAndCheckClasspathForNode(final String classpath, AbstractBuild<?, ?> build,  BuildListener listener){
		StringBuilder fromWorkspaceClassPath = new StringBuilder();
		try {
			VirtualChannel channel = build.getWorkspace().getChannel();
			for(String path : classpath.split(File.pathSeparator)){
				FilePath srcFile = new FilePath(new File(path));
		    	FilePath fp = new FilePath(channel, build.getWorkspace() + "/"+srcFile.getName());
		    	String remotePath = fp.getRemote();
		    	if(! fp.exists()){
					listener.error("[WeblogicDeploymentPlugin] - The following library '"+remotePath+"' declared on classpath is missing on node '"+build.getBuiltOnStr()+"'.");
					throw new RunnerAbortedException();
				}
		    	
		    	// TODO il y a toujours un dernier separator
		    	fromWorkspaceClassPath.append(remotePath).append(isUnix(channel, remotePath) ? ":" : ";");
			}
		} catch (IOException e) {
			listener.error("[WeblogicDeploymentPlugin] - Unable to compute classpath for remote invocation.", e);
			throw new RunnerAbortedException();
		} catch (InterruptedException e) {
			listener.error("[WeblogicDeploymentPlugin] - Unable to compute classpath for remote invocation.", e);
			throw new RunnerAbortedException();
		}
		return fromWorkspaceClassPath.toString();
	}
	
	public static void checkClasspath(final String classpath, AbstractBuild<?, ?> build,  BuildListener listener){
		try {
			for(String path : classpath.split(File.pathSeparator)){
				FilePath srcFile = new FilePath(new File(path));
		    	if(! srcFile.exists()){
					listener.error("[WeblogicDeploymentPlugin] - The following library '"+srcFile.getName()+"' declared on classpath is missing");
					throw new RunnerAbortedException();
				}
			}
		} catch (IOException e) {
			listener.error("[WeblogicDeploymentPlugin] - Unable to check classpath for invocation.", e);
			throw new RunnerAbortedException();
		} catch (InterruptedException e) {
			listener.error("[WeblogicDeploymentPlugin] - Unable to check classpath for invocation.", e);
			throw new RunnerAbortedException();
		}
	}
	
    /**
     * Checks if the remote path is Unix. 
     * Recuperer de la classe FilePath car methode non acessible en dehors du package
     */
    private static boolean isUnix(VirtualChannel channel, String remote) {
        // if the path represents a local path, there' no need to guess.
        if(channel == null)
            return File.pathSeparatorChar!=';';
            
        // note that we can't use the usual File.pathSeparator and etc., as the OS of
        // the machine where this code runs and the OS that this FilePath refers to may be different.

        // Windows absolute path is 'X:\...', so this is usually a good indication of Windows path
        if(remote.length()>3 && remote.charAt(1)==':' && remote.charAt(2)=='\\')
            return false;
        // Windows can handle '/' as a path separator but Unix can't,
        // so err on Unix side
        return remote.indexOf("\\")==-1;
    }
}
