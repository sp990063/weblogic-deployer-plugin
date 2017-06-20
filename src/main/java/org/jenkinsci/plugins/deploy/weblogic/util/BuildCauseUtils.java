/**
 * 
 */
package org.jenkinsci.plugins.deploy.weblogic.util;

import java.util.List;


/**
 * @author rchaumie
 *
 */
public final class BuildCauseUtils {

	private BuildCauseUtils(){}
	
	public static final String formatToString(List<hudson.model.Cause> buildCauses){
		StringBuilder result = new StringBuilder();
		
		int arrSize = buildCauses.size();
		for(int i =0; i < arrSize;i++) {
			result.append(buildCauses.get(i).getClass().getSimpleName());
			if(i + 1 < arrSize){
				result.append(",");
			}
		}
		return result.toString();
	}
}
