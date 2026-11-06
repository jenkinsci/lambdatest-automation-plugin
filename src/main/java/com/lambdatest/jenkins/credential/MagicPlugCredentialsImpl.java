package com.lambdatest.jenkins.credential;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.lambdatest.jenkins.freestyle.api.Constant;
import com.lambdatest.jenkins.freestyle.api.service.CapabilityService;

import hudson.Extension;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.Secret;

public class MagicPlugCredentialsImpl extends BaseStandardCredentials implements MagicPlugCredentials {

	private static final long serialVersionUID = 1L;
	private final String userName;
	private final Secret accessToken;

	private static final Logger logger = Logger.getLogger(MagicPlugCredentialsImpl.class.getName());

	@DataBoundConstructor
	public MagicPlugCredentialsImpl(CredentialsScope scope, String id, String description, String userName,
			String accessToken) throws Exception {
		super(scope, id, description);
		try {
			this.userName = userName;
			this.accessToken = Secret.fromString(accessToken);
			if (!CapabilityService.isValidUser(userName, accessToken)) {
				throw new Exception("Invalid username and access Token");
			}
		} catch (Exception e) {
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<MagicPlugCredentials> all(@SuppressWarnings("rawtypes") ItemGroup context) {
		return CredentialsProvider.lookupCredentials(MagicPlugCredentials.class, context, ACL.SYSTEM,
				Collections.EMPTY_LIST);
	}

	@SuppressWarnings("unchecked")
	@CheckForNull
	public static MagicPlugCredentials getCredentials(@Nullable String credentialsId,
			@SuppressWarnings("rawtypes") ItemGroup context) {
		if (StringUtils.isBlank(credentialsId)) {
			return null;
		}
		return (MagicPlugCredentials) CredentialsMatchers.firstOrNull(CredentialsProvider
				.lookupCredentials(MagicPlugCredentials.class, context, ACL.SYSTEM, Collections.EMPTY_LIST),
				CredentialsMatchers.withId(credentialsId));
	}

	@Extension
	public static class DescriptorImpl extends CredentialsDescriptor {

		@Override
		public String getDisplayName() {
			return "TestMu AI (Formerly LambdaTest) Credentials";
		}

		@Override
		public String getCredentialsPage() {
			return super.getCredentialsPage();
		}

		public FormValidation doVerifyCredentials(@QueryParameter("userName") final String userName,
				@QueryParameter("accessToken") final String accessToken) throws IOException, ServletException {
			if (StringUtils.isBlank(userName) || StringUtils.isBlank(accessToken)) {
				return FormValidation.error("Please enter valid username and authKey");
			}
			if (CapabilityService.isValidUser(userName, accessToken)) {
				return FormValidation.ok("Successful Authentication");
			} else {
				return FormValidation.error("Invalid Credentials");
			}
		}

		public FormValidation doCheckUsername(@QueryParameter String userName) throws IOException, ServletException {
			try {
				if (StringUtils.isBlank(userName)) {
					return FormValidation.error("Invalid username");
				}
				return FormValidation.ok();
			} catch (NumberFormatException e) {
				return FormValidation.error("Invalid username");
			}
		}

		public FormValidation doCheckAccessToken(@QueryParameter String accessToken)
				throws IOException, ServletException {
			try {
				if (StringUtils.isBlank(accessToken)) {
					return FormValidation.error("Invalid Access Token");
				}
				return FormValidation.ok();
			} catch (NumberFormatException e) {
				return FormValidation.error("Invalid Access Token");
			}
		}

	}

	@Override
	public String getUserName() {
		if (userName == null) {
			return Constant.NOT_AVAILABLE;
		} else {
			return userName;
		}
	}

	@Override
	public Secret getAccessToken() {
		if (this.accessToken == null) {
			return Secret.fromString(Constant.NOT_AVAILABLE);
		} else {
			return this.accessToken;
		}
	}

	@Override
	public String getDisplayName() {
		return getUserName();
	}

}
