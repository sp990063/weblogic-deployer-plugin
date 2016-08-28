package org.jenkinsci.plugins.deploy.weblogic.data.policy;

import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Descriptor;
import hudson.triggers.Messages;
import hudson.triggers.TimerTrigger;
import org.jenkinsci.plugins.deploy.weblogic.trigger.DeploymentTrigger;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Mustafa Ulu
 *
 * @since 3.5
 */
public class TimerTriggerCauseDeploymentPolicy extends AbstractDeploymentPolicy {

    private static Class<? extends Cause> causeClass = TimerTrigger.TimerTriggerCause.class;

    @Extension
    public static class DeploymentPolicyDescriptor extends Descriptor<AbstractDeploymentPolicy> {

        public DeploymentPolicyDescriptor() {
            super(TimerTriggerCauseDeploymentPolicy.class);
        }

        @Override
        public String getDisplayName() {
            return Messages.TimerTrigger_TimerTriggerCause_ShortDescription();
        }
    }

    @DataBoundConstructor
    public TimerTriggerCauseDeploymentPolicy(boolean deployingOnlyWhenUpdates) {
        super(deployingOnlyWhenUpdates);
    }

    public TimerTriggerCauseDeploymentPolicy(TimerTriggerCauseDeploymentPolicy deploymentPolicy) {
        super(deploymentPolicy);
    }

    public Class<? extends Cause> getCauseClass() {
        return causeClass;
    }
}
