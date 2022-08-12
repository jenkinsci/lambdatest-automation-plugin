package com.lambdatest.jenkins.freestyle.report;

import com.lambdatest.jenkins.freestyle.report.ReportBuildAction;
import com.lambdatest.jenkins.freestyle.api.service.CapabilityService;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import hudson.tasks.Recorder;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

import com.lambdatest.jenkins.freestyle.api.Constant;

public class LambdaTestReportPublisher extends Recorder implements SimpleBuildStep {
    private final static Logger logger = Logger.getLogger(LambdaTestReportPublisher.class.getName());

    @DataBoundConstructor
    public LambdaTestReportPublisher() {
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        final EnvVars parentEnvs = build.getEnvironment(listener);
        final String username = parentEnvs.get(Constant.LT_USERNAME);
        final String accessKey = parentEnvs.get(Constant.LT_ACCESS_KEY);
        final String buildName = parentEnvs.get(Constant.LT_BUILD_NAME);

        logger.info("Generating LambdaTest Report");
        logger.info("username : " + username);
        logger.info("accessKey : " + accessKey);
        logger.info("buildName : " + buildName);
        ReportBuildAction ltReportAction = new ReportBuildAction(build, username, accessKey, buildName);
        ltReportAction.generateLambdaTestReport();
        build.addAction(ltReportAction);
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        @SuppressWarnings("rawtypes")
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return Constant.LT_REPORT_DISPLAY_NAME;
        }
    }
}
