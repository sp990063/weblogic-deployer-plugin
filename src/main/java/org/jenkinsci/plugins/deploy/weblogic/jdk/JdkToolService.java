/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.jdk;

import java.io.File;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.util.NullStream;
import hudson.util.StreamTaskListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

/**
 * @author Raphael
 *
 */
public class JdkToolService {

public static final String EXTERNAL_ENV_JDK = "environment";
	
	public static final String SYSTEM_JDK = "system";
	
	public static final String JAVA_VERSION_COMMAND_VERSION_LINE_REGEX = ".*\\r*\\n*(java version )(\")(.+)(\").*\\r*\\n*.*\\r*\\n*.*\\r*\\n*";
	
	
//	static List<JDK> jdkToolAvailables = new ArrayList<JDK>();
	
	public static List<JDK> getJdkToolAvailables()  throws IOException, InterruptedException {

		List<JDK> jdkToolAvailables = new ArrayList<JDK>();
		
		// TODO Concerne uniquement le master : a refactorer ....
		//Ajout de la java home system
		if (StringUtils.isNotBlank(SystemUtils.JAVA_HOME)){
			jdkToolAvailables.add(new JDK(SYSTEM_JDK, System.getProperty("java.home")));
		}
		//Ajout de la java home avec lequel est demarre Jenkins
		if(StringUtils.isNotBlank(System.getenv("JAVA_HOME"))){
			jdkToolAvailables.add(new JDK(EXTERNAL_ENV_JDK, System.getenv("JAVA_HOME")));
		}
		// Ajout de tous les jdk declares dans Jenkins
//		jdkToolAvailables.addAll(Jenkins.getInstance().getJDKs());
		
		//Ajout des outils JDK declare sur le master (configuration globale -> Emplacement des outils)
		for(JDK jdk : Jenkins.getInstance().getJDKs()){
			// Le getNode ne cherche que dans les slaves pas le master libelle (master)
//			Node masterNode = Jenkins.getInstance().getNode(nodeName);
			Node masterNode = Jenkins.getInstance();
			JDK jdkToolOnMasterNode = jdk.forNode(masterNode, new StreamTaskListener(new NullStream()));
			jdkToolAvailables.add(jdkToolOnMasterNode);
		}
		return jdkToolAvailables;
	}
	
	public static JDK getJDKByName(Node node, String name) throws IOException, InterruptedException {
		JDK out = null;
		for(JDK jdk : getJdkToolAvailables()) {
			if(name.equalsIgnoreCase(jdk.getName())){
				// Recuperer le jdk declare au niveau du node correspondant
				return jdk.forNode(node, new StreamTaskListener(new NullStream()));
			}
		}
		return out;
	}
	
//	public static String getJDKHomeByName(String name) {
//		String out = null;
//		for(JDK jdk : getJdkToolAvailables()) {
//			if(jdk.getName().equalsIgnoreCase(name)){
//				out = new File(jdk.getHome()).getAbsolutePath();
//			}
//		}
//		return out;
//	}
	
	public static boolean checkJdkVersion(Node node, JDK jdk, PrintStream logger){
		
		if(jdk == null || ! jdk.getExists()){
			return false;
		}
		
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			TaskListener listener = new StreamTaskListener(out);
			Launcher launcher = node.createLauncher(listener);
			String cmd = new FilePath(node.getChannel(), jdk.getHome().concat("/bin/java")).getRemote();
//			int result = launcher.launch().cmds(cmd,"-fullversion").stdout(out).join();
			int result = launcher.launch().cmds(cmd,"-version").stdout(out).join();
//			L'executable n'existe pas
			if(result  != 0){
				logger.println("[WeblogicDeploymentPlugin] - Unable to detect JDK version");
				return false;
			}
			
			// TODO ne semble pas fonctionner a distance
			Pattern pattern = Pattern.compile(JAVA_VERSION_COMMAND_VERSION_LINE_REGEX);
			Matcher matcher = pattern.matcher(out.toString());
			if(matcher.matches()){
				logger.println("[WeblogicDeploymentPlugin] - Pay attention to JDK version {selected version is "+matcher.group(3)+"} compatibility with WebLogic Deployer API (see Oracle documentation).");
			}
			
		} catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        } catch(IllegalStateException ise){
        	return false;
        } catch(IndexOutOfBoundsException ioobe) {
        	return false;
        }
		
		return true;
	}
	
    /**
     * Checks if JDK exists on the given node.
     *
     * <p>
     * If it's not, then the user must specify a configured JDK,
     * so this is often useful for form field validation.
     */
    public static boolean isJDKValid(Node node, JDK jdk) {
        try {
            TaskListener listener = new StreamTaskListener(new NullStream());
            Launcher launcher = node.createLauncher(listener);
            String cmd = new FilePath(node.getChannel(), jdk.getHome().concat("/bin/java")).getRemote();
            return launcher.launch().cmds(cmd,"-fullversion").stdout(listener).join()==0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
