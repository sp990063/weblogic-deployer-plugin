package org.jenkinsci.plugins.deploy.weblogic.data.policy;

import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Descriptor;
import org.jenkinsci.plugins.deploy.weblogic.Messages;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Mustafa Ulu
 *
 * @since 3.5
 */
public class UserIdCauseDeploymentPolicy extends AbstractDeploymentPolicy {

    private static Class<? extends Cause> causeClass = Cause.UserIdCause.class;

    @Extension
    public static class DeploymentPolicyDescriptor extends Descriptor<AbstractDeploymentPolicy> {

        public DeploymentPolicyDescriptor() {
            super(UserIdCauseDeploymentPolicy.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.Cause_UserIdCause_DisplayName();
        }
    }

    @DataBoundConstructor
    public UserIdCauseDeploymentPolicy(boolean deployingOnlyWhenUpdates) {
        super(deployingOnlyWhenUpdates);
    }

    public UserIdCauseDeploymentPolicy(UserIdCauseDeploymentPolicy deploymentPolicy) {
        super(deploymentPolicy);
    }

    public Class<? extends Cause> getCauseClass() {
        return causeClass;
    }
}
