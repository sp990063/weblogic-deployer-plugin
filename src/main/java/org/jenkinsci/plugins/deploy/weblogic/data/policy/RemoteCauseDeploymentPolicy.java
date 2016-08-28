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
public class RemoteCauseDeploymentPolicy extends AbstractDeploymentPolicy {

    private static Class<? extends Cause> causeClass = Cause.RemoteCause.class;

    @Extension
    public static class DeploymentPolicyDescriptor extends Descriptor<AbstractDeploymentPolicy> {

        public DeploymentPolicyDescriptor() {
            super(RemoteCauseDeploymentPolicy.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.Cause_RemoteCause_DisplayName();
        }
    }

    @DataBoundConstructor
    public RemoteCauseDeploymentPolicy(boolean deployingOnlyWhenUpdates) {
        super(deployingOnlyWhenUpdates);
    }

    public RemoteCauseDeploymentPolicy(RemoteCauseDeploymentPolicy deploymentPolicy) {
        super(deploymentPolicy);
    }

    public Class<? extends Cause> getCauseClass() {
        return causeClass;
    }
}
