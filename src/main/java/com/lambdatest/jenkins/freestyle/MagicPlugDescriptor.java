package com.lambdatest.jenkins.freestyle;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.springframework.util.CollectionUtils;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.lambdatest.jenkins.credential.MagicPlugCredentialsImpl;
import com.lambdatest.jenkins.freestyle.api.Constant;
import com.lambdatest.jenkins.freestyle.api.service.CapabilityService;
import com.lambdatest.jenkins.freestyle.data.AppAutomationCapabilityRequest;
import com.lambdatest.jenkins.freestyle.api.service.AppAutomationCapabilityService;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.ItemGroup;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

@Extension
public class MagicPlugDescriptor extends BuildWrapperDescriptor {

	private final static Logger logger = Logger.getLogger(MagicPlugDescriptor.class.getName());

	@Override
	public boolean isApplicable(AbstractProject<?, ?> item) {
		return true;
	}

	@Override
	public String getDisplayName() {
		return "LAMBDATEST";
	}

	public MagicPlugDescriptor() {
		super(MagicPlugBuildWrapper.class);
		load();
	}

	public FormValidation doPing() throws IOException, ServletException {
		logger.info("doPing");
		if (new CapabilityService().ping()) {
			return FormValidation.ok("Ping Successful");
		} else {
			return FormValidation.error("Ping Failed");
		}
	}

	/**
	 * Return list of supported `operatingSystem`
	 * 
	 * @return ListBoxModel
	 */
	public ListBoxModel doFillOperatingSystemItems() {
		Map<String, String> supportedOS = CapabilityService.getOperatingSystems();
		ListBoxModel items = new ListBoxModel();
		items.add(Constant.DEFAULT_OPERATING_SYSTEM_VALUE, Constant.EMPTY);
		supportedOS.forEach((key, value) -> {
			items.add(value, key);
		});
		return items;
	}

	public ListBoxModel doFillBrowserNameItems(@QueryParameter String operatingSystem) {
		ListBoxModel items = new ListBoxModel();
		if (StringUtils.isBlank(operatingSystem)) {
			items.add(Constant.DEFAULT_BROWSER_NAME_VALUE, Constant.EMPTY);
			return items;
		}
		logger.info("operatingSystem : +" + operatingSystem);
		Set<String> supportedBrowsers = CapabilityService.getBrowserNames(operatingSystem);
		if (!CollectionUtils.isEmpty(supportedBrowsers)) {
			supportedBrowsers.forEach(br -> {
				items.add(br, br);
			});
		}
		return items;
	}

	public ListBoxModel doFillBrowserVersionItems(@QueryParameter String operatingSystem,
			@QueryParameter String browserName) {
		ListBoxModel items = new ListBoxModel();
		logger.info(operatingSystem + "::" + browserName);
		if (!StringUtils.isBlank(operatingSystem) && StringUtils.isBlank(browserName)) {
			browserName = "Chrome";
			logger.info("Chrome added");
		} else if (StringUtils.isBlank(operatingSystem) || StringUtils.isBlank(browserName)) {
			items.add(Constant.DEFAULT_BROWSER_VERSION_VALUE, Constant.EMPTY);
			return items;
		}
		Set<String> supportedBrowserVersions = CapabilityService.getBrowserVersions(operatingSystem, browserName);
		if (!CollectionUtils.isEmpty(supportedBrowserVersions)) {
			supportedBrowserVersions.forEach(ver -> {
				items.add(ver, ver);
			});
		}
		return items;
	}

	public ListBoxModel doFillResolutionItems(@QueryParameter String operatingSystem) {
		ListBoxModel items = new ListBoxModel();
		if (StringUtils.isBlank(operatingSystem)) {
			items.add(Constant.DEFAULT_RESOLUTION_VALUE, Constant.EMPTY);
			return items;
		}
		logger.info("operatingSystem : " + operatingSystem);
		List<String> supportedBrowsers = CapabilityService.getResolutions(operatingSystem);
		if (!CollectionUtils.isEmpty(supportedBrowsers)) {
			supportedBrowsers.forEach(br -> {
				items.add(br, br);
			});
		}
		return items;
	}

	public ListBoxModel doFillPlatformNameItems() {
		Map<String, String> supportedPlatforms = AppAutomationCapabilityService.getPlatformNames();
		logger.info("OS triggered : " + supportedPlatforms);
		ListBoxModel items = new ListBoxModel();
		items.add(Constant.DEFAULT_PLATFORM_NAME_VALUE, Constant.EMPTY);
		supportedPlatforms.forEach((key, value) -> {
			items.add(value, key);
		});
		return items;
	}

	public ListBoxModel doFillBrandNameItems(@QueryParameter String platformName) {
		ListBoxModel items = new ListBoxModel();
		if (StringUtils.isBlank(platformName)) {
			items.add(Constant.DEFAULT_BRAND_NAME_VALUE, Constant.EMPTY);
			return items;
		}
		logger.info("platformName : " + platformName);
		Set<String> supportedBrands = AppAutomationCapabilityService.getBrandNames(platformName);
		logger.info("Brand Names triggered : " + supportedBrands);
		if (!CollectionUtils.isEmpty(supportedBrands)) {
			supportedBrands.forEach(br -> {
				items.add(br, br);
			});
		}
		return items;
	}
	
	public ListBoxModel doFillDeviceNameItems(@QueryParameter String platformName, @QueryParameter String brandName) {
		ListBoxModel items = new ListBoxModel();
		logger.info(platformName + "::" + brandName);
		if (!StringUtils.isBlank(platformName) && StringUtils.isBlank(brandName)) {
			brandName = "Asus";
			logger.info("Asus added");
		} else if (StringUtils.isBlank(platformName) || StringUtils.isBlank(brandName)) {
			items.add(Constant.DEFAULT_DEVICE_NAME_VALUE, Constant.EMPTY);
			return items;
		}
		logger.info("platformName : " + platformName + "\n" + "brandName : " + brandName);
		Set<String> supportedDevices = AppAutomationCapabilityService.getDeviceNames(platformName, brandName);
		logger.info("Device Names triggered : " + supportedDevices);
		if (!CollectionUtils.isEmpty(supportedDevices)) {
			supportedDevices.forEach(br -> {
				items.add(br, br);
			});
		}
		return items;
	}

	public ListBoxModel doFillDeviceVersionItems(@QueryParameter String platformName,
			@QueryParameter String deviceName) {
		ListBoxModel items = new ListBoxModel();
		logger.info(platformName + "::" + deviceName);
		if (!StringUtils.isBlank(platformName) && StringUtils.isBlank(deviceName)) {
			deviceName = "Zenfone 6";
			logger.info("Zenfone 6 added");
		} else if (StringUtils.isBlank(platformName) || StringUtils.isBlank(deviceName)) {
			items.add(Constant.DEFAULT_DEVICE_VERSION_VALUE, Constant.EMPTY);
			return items;
		}
		Set<String> supportedDeviceVersions = AppAutomationCapabilityService.getDeviceVersions(platformName, deviceName);
		logger.info("Device Versions triggered : " + supportedDeviceVersions);
		if (!CollectionUtils.isEmpty(supportedDeviceVersions)) {
			supportedDeviceVersions.forEach(ver -> {
				items.add(ver, ver);
			});
		}
		return items;
	}

	public ListBoxModel doFillAppId() {
		ListBoxModel items = new ListBoxModel();
		items.add(Constant.DEFAULT_APP_URL, Constant.EMPTY);
		return items;
	}
	
	public ListBoxModel doFillCredentialsIdItems(@QueryParameter String credentialsId) {
		if (!StringUtils.isBlank(credentialsId)) {
			logger.info(credentialsId);
		} else {
			logger.info("Not Found");
		}
		return new ListBoxModel();
	}

	public ListBoxModel doFillCredentialsIdItems(ItemGroup context) {
		return new StandardListBoxModel().withEmptySelection().withMatching(CredentialsMatchers.always(),
				MagicPlugCredentialsImpl.all(context));
	}
}
