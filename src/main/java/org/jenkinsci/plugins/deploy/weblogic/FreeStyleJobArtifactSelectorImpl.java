/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.deploy.weblogic.util.VarUtils;
import org.jenkinsci.remoting.RoleChecker;

import com.google.common.collect.Lists;

/**
 * @author rchaumie
 *
 */
public class FreeStyleJobArtifactSelectorImpl implements ArtifactSelector {
	
	/*
	 * (non-Javadoc)
	 * @see org.jenkinsci.plugins.deploy.weblogic.ArtifactSelector#selectArtifactRecorded(hudson.model.AbstractBuild, hudson.model.BuildListener, java.lang.String, java.lang.String)
	 */
	public FilePath selectArtifactRecorded(AbstractBuild<?, ?> build, BuildListener listener, String filteredResource, String baseDirectory) throws IOException, InterruptedException  {
		
		FilePath selectedArtifact = null;
		
        listener.getLogger().println("[WeblogicDeploymentPlugin] - Retrieving artifacts recorded [filtered resources on "+filteredResource+"]...");
        List<FilePath> artifactsRecorded = new ArrayList<FilePath>();
        
        // On parcours le workspace si aucun repertoire de base specifie a la recherche d'un fichier correspondant a l'expression reguliere
        List<FilePath> filesToCheck = Lists.newArrayList();
        FilePath baseDir = null;
        if(StringUtils.isBlank(baseDirectory)){
        	baseDir = build.getWorkspace();
            listener.getLogger().println("[WeblogicDeploymentPlugin] - No the base directory set. Looking up for workspace directory located on '"+baseDir+"'...");
        } else {

            //Recuperation des variables
            EnvVars vars = VarUtils.getEnvVars(build, listener);
            String resolvedBaseDirectory = vars.expand(baseDirectory);

            baseDir = new FilePath(build.getBuiltOn().getChannel(), resolvedBaseDirectory);
            
        	//si un repertoire est specifie mais qu'il est inacessible ou invalide on renvoit une erreur
            if(! baseDir.exists() || ! baseDir.isDirectory() || ! baseDir.toVirtualFile().canRead()){
            	listener.getLogger().println("[WeblogicDeploymentPlugin] - the following base directory '"+resolvedBaseDirectory+"' is invalid on node '"+build.getBuiltOnStr()+"' (doesn't exist or is not a directory or has insufficient privilege). Please check the job configuration");
            	throw new RuntimeException("The base directory '"+resolvedBaseDirectory+"' is invalid on node '"+build.getBuiltOnStr()+"' (doesn't exist or is not a directory or has insufficient privilege)");
            }
        }
        
        // lookup files found
        filesToCheck = baseDir.act(new RecursiveWorkspaceLookup());

        listener.getLogger().println("[WeblogicDeploymentPlugin] - "+filesToCheck.size() +" files found under directory "+baseDir.getRemote());
        // TODO : Pourquoi ne pas l'incorporer au list(...) directement ?
        for(FilePath file : filesToCheck){
            if(! file.isDirectory() && Pattern.matches(filteredResource, file.getName())){
                listener.getLogger().println("[WeblogicDeploymentPlugin] - the following resource recorded "+file+" is eligible.");
                artifactsRecorded.add(file);
            } else {
                listener.getLogger().println("[WeblogicDeploymentPlugin] - the following resource ['"+file.getRemote()+"'] doesn't match "+filteredResource);
            }
        }
        
        if(artifactsRecorded.size() < 1){
        	throw new RuntimeException("No artifact to deploy ["+filteredResource+"] found.");
        }
        
        if(artifactsRecorded.size() > 1){
        	listener.getLogger().println("[WeblogicDeploymentPlugin] - More than 1 artifact found : The first one "+artifactsRecorded.get(0)+ " will be deployed!!!");
        }
        
        selectedArtifact = artifactsRecorded.get(0);
        
    	
		
		// Erreur si l'artifact n'existe pas
		if(selectedArtifact == null){
			throw new RuntimeException("No artifact to deploy found.");
		}        
		return selectedArtifact;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jenkinsci.plugins.deploy.weblogic.ArtifactSelector#getName()
	 */
	public String getName() {
		return "FreeStyleProject";
	}
	
	private static class RecursiveWorkspaceLookup implements FileCallable<List<FilePath>> {
		private static final long serialVersionUID = 1L;
		
		@Override
		public void checkRoles(RoleChecker checker) throws SecurityException {
		}

		@Override
		public List<FilePath> invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
			List<FilePath> result = Lists.newArrayList();
			Collection<File> files = FileUtils.listFiles(f, null, true);
			for(File file : files){
				result.add(new FilePath(file));
			}
			return result;
		}
        
    }

}
