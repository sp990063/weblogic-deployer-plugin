package org.jenkinsci.plugins.deploy.weblogic.data.policy;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Cause;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.Serializable;

/**
 * @author Mustafa Ulu
 *
 * @since 3.5
 */
public abstract class AbstractDeploymentPolicy extends AbstractDescribableImpl<AbstractDeploymentPolicy> implements Serializable {

    private boolean deployingOnlyWhenUpdates;

    @DataBoundConstructor
    public AbstractDeploymentPolicy(boolean deployingOnlyWhenUpdates) {
        this.deployingOnlyWhenUpdates = deployingOnlyWhenUpdates;
    }

    public AbstractDeploymentPolicy(AbstractDeploymentPolicy deploymentPolicy) {
        this.deployingOnlyWhenUpdates = deploymentPolicy.isDeployingOnlyWhenUpdates();
    }

    public abstract Class<? extends Cause> getCauseClass();

    public boolean isDeployingOnlyWhenUpdates() {
        return deployingOnlyWhenUpdates;
    }
}
