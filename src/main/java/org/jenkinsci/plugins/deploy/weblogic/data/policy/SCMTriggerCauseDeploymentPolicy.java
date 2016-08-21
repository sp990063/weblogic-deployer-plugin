package org.jenkinsci.plugins.deploy.weblogic.data.policy;

import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.triggers.Messages;
import hudson.triggers.SCMTrigger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Mustafa Ulu
 *
 * @since 3.5
 */
public class SCMTriggerCauseDeploymentPolicy extends AbstractDeploymentPolicy {

    private static Class<? extends Cause> causeClass = SCMTrigger.SCMTriggerCause.class;

    @Extension
    public static class DeploymentPolicyDescriptor extends Descriptor<AbstractDeploymentPolicy> {

        public DeploymentPolicyDescriptor() {
            super(SCMTriggerCauseDeploymentPolicy.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.SCMTrigger_SCMTriggerCause_ShortDescription();
        }
    }

    @DataBoundConstructor
    public SCMTriggerCauseDeploymentPolicy(boolean deployingOnlyWhenUpdates) {
        super(deployingOnlyWhenUpdates);
    }

    public SCMTriggerCauseDeploymentPolicy(SCMTriggerCauseDeploymentPolicy deploymentPolicy) {
        super(deploymentPolicy);
    }

    public Class<? extends Cause> getCauseClass() {
        return causeClass;
    }
}
