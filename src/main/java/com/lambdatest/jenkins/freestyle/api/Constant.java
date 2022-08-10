package com.lambdatest.jenkins.freestyle.api;

import jenkins.model.Jenkins;

public interface Constant {

	String LT_REPORT_DISPLAY_NAME = "LambdaTest Report";
	String LT_APPIUM_REPORT_DISPLAY_NAME = "LambdaTest Appium Report";
	String LT_REPORT_URL = "reportLambdaTest";
	String LT_ICON_FILE_NAME = Jenkins.RESOURCE_PATH + "/plugin/lambdatest-automation/images/logo.png";

	String NOT_AVAILABLE = "NA";
	String EMPTY = "";
	String LT_PLATFORM = "LT_PLATFORM";
	String LT_BROWSERS = "LT_BROWSERS";
	String LT_BROWSER_NAME = "LT_BROWSER_NAME";
	String LT_BROWSER_VERSION = "LT_BROWSER_VERSION";
	String LT_RESOLUTION = "LT_RESOLUTION";
	String LT_GRID_URL = "LT_GRID_URL";
	String LT_BUILD_NAME = "LT_BUILD_NAME";
	String LT_BUILD_NUMBER = "LT_BUILD_NUMBER";
	String LT_USERNAME = "LT_USERNAME";
	String LT_ACCESS_KEY = "LT_ACCESS_KEY";
	String LT_PLATFORM_NAME = "LT_PLATFORM_NAME";
	String LT_BRANDS = "LT_BRANDS";
	String LT_DEVICES = "LT_DEVICES";
	String LT_BRAND_NAME = "LT_BRAND_NAME";
	String LT_DEVICE_NAME = "LT_DEVICE_NAME";
	String LT_DEVICE_VERSION = "LT_DEVICE_VERSION";
	String LT_APP_URL = "LT_APP_URL";

	String OPERATING_SYSTEM = "operatingSystem";
	String BROWSER_NAME = "browserName";
	String BROWSER_VERSION = "browserVersion";
	String RESOLUTION = "resolution";

	String PLATFORM_NAME = "platformName";
	String BRAND_NAME = "brandName";
	String DEVICE_NAME = "deviceName";
	String DEVICE_VERSION = "deviceVersion";
	String APP_ID = "appUrl"; 

	String OS_API_URL = "https://api.lambdatest.com/api/v1/capability?format=array";
	String BROWSER_API_URL = "https://api.lambdatest.com/api/v1/capability?format=array&os=";
	String AUTH_API_URL = "https://accounts.lambdatest.com/api/user/token/auth";
	String ANALYTICS_URL = "https://backend.lambdatest.com/api/analytics/automation-plugin-usage";
	String APP_URL = "https://automation.lambdatest.com";
	String HUB_URL = "@hub.lambdatest.com/wd/hub";

	String APPIUM_OS_API_URL = "https://api.lambdatest.com/api/v1/capability?grid=appium";
	String DEVICE_API_URL = "https://mobile-api.lambdatest.com/mobile-automation/api/v1/capability/generator";
	String APPIUM_APP_URL = "https://appautomation.lambdatest.com";
	String APPIUM_HUB_URL = "@mobile-hub.lambdatest.com/wd/hub";

	interface Stage {
		String APP_URL = "https://stage-automation.lambdatest.com";
		String HUB_URL = "@stage-hub.lambdatest.com/wd/hub";
		String APPIUM_HUB_URL = "@stage-mobile-hub.lambdatest.com/wd/hub";
	}

	interface Beta {
		String APP_URL = "https://beta-automation.lambdatest.com";
		String HUB_URL = "@beta-hub.lambdatest.com/wd/hub";
	}

	interface Dev {
		String APP_URL = "https://dev-automation.lambdatest.com";
		String HUB_URL = "@dev-hub.lambdatest.com/wd/hub";
	}

	String DEFAULT_OPERATING_SYSTEM_VALUE = "Select Operating System";
	String DEFAULT_BROWSER_NAME_VALUE = "Select Browser Name";
	String DEFAULT_BROWSER_VERSION_VALUE = "Select Browser Version";
	String DEFAULT_RESOLUTION_VALUE = "Select Screen Resolution";

	String DEFAULT_PLATFORM_NAME_VALUE = "Select Platform Name";
	String DEFAULT_BRAND_NAME_VALUE = "Select Brand Name";
	String DEFAULT_DEVICE_NAME_VALUE = "Select Device Name";
	String DEFAULT_DEVICE_VERSION_VALUE = "Select Device Version";
	String DEFAULT_APP_URL = "Enter App Url";

	String STAGE = "stage";
	String DEV = "dev";
	String BETA = "beta";

	String MAC_HASH_URL = "https://downloads.lambdatest.com/tunnel/v3/mac/64bit/latest";
	String MAC_BINARY_URL = "https://downloads.lambdatest.com/tunnel/v3/mac/64bit/LT_Mac.zip";

	String LINUX_HASH_URL = "https://downloads.lambdatest.com/tunnel/v3/linux/64bit/latest";
	String LINUX_BINARY_URL = "https://downloads.lambdatest.com/tunnel/v3/linux/64bit/LT_Linux.zip";

	String WIN_HASH_URL = "https://downloads.lambdatest.com/tunnel/v3/windows/64bit/latest";
	String WIN_BINARY_URL = "https://downloads.lambdatest.com/tunnel/v3/windows/64bit/LT_Windows.zip";

	String DEFAULT_TUNNEL_NAME = "jenkins-tunnel";
	String DEFAULT_TUNNEL_FOLDER_NAME= "lambda-tunnel";
	String LT_TUNNEL_NAME = "LT_TUNNEL_NAME";




	//New Binary Path for UnderPass Tunnel
	String DOWNLOAD_ALPHA_TUNNEL_LINK= "https://downloads.lambdatest.com/tunnel";
	String MAC_WS_HASH_URL = DOWNLOAD_ALPHA_TUNNEL_LINK +"/mac/64bit/latest";
	String MAC_WS_BINARY_URL = DOWNLOAD_ALPHA_TUNNEL_LINK +"/mac/64bit/ltcomponent.zip";

	String LINUX_WS_HASH_URL = DOWNLOAD_ALPHA_TUNNEL_LINK +"/linux/64bit/latest";
	String LINUX_WS_BINARY_URL = DOWNLOAD_ALPHA_TUNNEL_LINK +"/linux/64bit/ltcomponent.zip";

	String WIN_WS_HASH_URL = DOWNLOAD_ALPHA_TUNNEL_LINK +"/windows/64bit/latest";
	String WIN_WS_BINARY_URL = DOWNLOAD_ALPHA_TUNNEL_LINK +"/windows/64bit/ltcomponent.zip";

	interface Type {
		String HASH = "hash";
		String BINARY = "binary";
	}

	interface OS {
		String UNIX = "unix";
		String MAC = "mac";
		String WIN = "win";
	}

	interface Arch {
		String x32 = "32bit";
		String x64 = "64bit";
	}

	interface Report {
		public static final String NAME = "name";
		public static final String BROWSER = "browser";
		public static final String OS = "os";
		public static final String STATUS = "status";
		public static final String USER_MARKED = "userMarked";
		public static final String DURATION = "duration";
		public static final String CREATED_AT = "createdAt";
		public static final String BUILD_INFO_URL = "https://api.lambdatest.com/automation/api/v1/builds";
		public static final String SESSION_INFO_URL = "https://api.lambdatest.com/automation/api/v1/sessions";
	}

	interface AppiumReport {
		public static final String NAME = "name";
		public static final String DEVICE = "device";
		public static final String OS = "os";
		public static final String STATUS = "status";
		public static final String USER_MARKED = "userMarked";
		public static final String DURATION = "duration";
		public static final String CREATED_AT = "createdAt";
		public static final String BUILD_INFO_URL = "https://mobile-api.lambdatest.com/mobile-automation/api/v1/builds";
		public static final String SESSION_INFO_URL = "https://mobile-api.lambdatest.com/mobile-automation/api/v1/sessions";
	}

}