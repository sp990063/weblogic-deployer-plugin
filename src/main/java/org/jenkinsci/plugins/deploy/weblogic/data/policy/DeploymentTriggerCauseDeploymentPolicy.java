package org.jenkinsci.plugins.deploy.weblogic.data.policy;

import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Descriptor;
import org.jenkinsci.plugins.deploy.weblogic.Messages;
import org.jenkinsci.plugins.deploy.weblogic.trigger.DeploymentTrigger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Mustafa Ulu
 *
 * @since 3.5
 */
public class DeploymentTriggerCauseDeploymentPolicy extends AbstractDeploymentPolicy {

    private static Class<? extends Cause> causeClass = DeploymentTrigger.DeploymentTriggerCause.class;

    @Extension
    public static class DeploymentPolicyDescriptor extends Descriptor<AbstractDeploymentPolicy> {

        public DeploymentPolicyDescriptor() {
            super(DeploymentTriggerCauseDeploymentPolicy.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.DeploymentTrigger_DeploymentTriggerCause_ShortDescription();
        }
    }

    @DataBoundConstructor
    public DeploymentTriggerCauseDeploymentPolicy(boolean deployingOnlyWhenUpdates) {
        super(deployingOnlyWhenUpdates);
    }

    public DeploymentTriggerCauseDeploymentPolicy(DeploymentTriggerCauseDeploymentPolicy deploymentPolicy) {
        super(deploymentPolicy);
    }

    public Class<? extends Cause> getCauseClass() {
        return causeClass;
    }
}
