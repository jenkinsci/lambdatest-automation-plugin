package com.lambdatest.jenkins.freestyle.service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import java.util.logging.Logger;
import com.lambdatest.jenkins.freestyle.api.Constant;
import com.lambdatest.jenkins.freestyle.api.service.CapabilityService;
import com.lambdatest.jenkins.freestyle.data.LocalTunnel;
import com.lambdatest.jenkins.freestyle.exception.TunnelHashNotFoundException;
import com.lambdatest.jenkins.freestyle.util.PortAvailabilityUtils;

import hudson.FilePath;

public class LambdaTunnelService {
	private final static Logger logger = Logger.getLogger(LambdaTunnelService.class.getName());
//	private static final Logger logger = LogManager.getLogger(LambdaTunnelService.class);

	protected static Process tunnelProcess;
	private static String tunnelFolderName = "/";

	public static Process setUp(String user, String key, LocalTunnel localTunnel, String buildnumber, String tunnelName,
								FilePath workspacePath) {
		if (OSValidator.isUnix()) {
			logger.info("Jenkins configured on Unix/Linux, getting latest hash");
			try {
				// Get Latest Hash
				String latestHash = getLatestHash(Constant.LINUX_HASH_URL);
				logger.info(latestHash);
				// Verify Latest binary version
				ClassLoader loader = LambdaTunnelService.class.getClassLoader();
				if (loader != null) {
					URL tunnelFolderPath = loader.getResource(tunnelFolderName);
					if (tunnelFolderPath != null) {
						String tunnelBinaryLocation = tunnelFolderPath.getPath() + latestHash;
						logger.info("Tunnel Binary Location :" + tunnelBinaryLocation);
						File tunnelBinary = new File(tunnelBinaryLocation);
						if (tunnelBinary.exists()) {
							logger.info("Tunnel Binary already exists");
						} else {
							logger.info("Tunnel Binary doesn't exists, Downloading new binary ...");
							downloadAndUnZipBinaryFile(tunnelFolderPath.getPath(), latestHash,
									Constant.LINUX_BINARY_URL);
							logger.info("Tunnel Binary downloaded from " + Constant.LINUX_BINARY_URL);
						}
						// Get Tunnel Log path name
						String tunnelLogPath = getTunnelLogPath(workspacePath, buildnumber);
						logger.info("Tunnel Log Path:" + tunnelLogPath);
						return runCommandLine(tunnelBinaryLocation, tunnelLogPath, user, key, tunnelName,localTunnel);
					} else {
						logger.warning("tunnelFolderPath empty");
					}
				} else {
					logger.warning("loader empty");
				}
			} catch (Exception e) {
				logger.warning(e.getMessage());
			}
		} else if (OSValidator.isMac()) {
			logger.info("Jenkins configured on Mac, getting latest hash");
			try {
				// Get Latest Hash
				String latestHash = getLatestHash(Constant.MAC_HASH_URL);
				logger.info(latestHash);
				// Verify Latest binary version
				// Checking for the tunnel log file exists or not
				ClassLoader loader = LambdaTunnelService.class.getClassLoader();
				if (loader != null) {
					URL tunnelFolderPath = loader.getResource(tunnelFolderName);
					if (tunnelFolderPath != null) {
						String tunnelBinaryLocation = tunnelFolderPath.getPath() + latestHash;
						logger.info("Tunnel Binary Location :" + tunnelBinaryLocation);
						File tunnelBinary = new File(tunnelBinaryLocation);
						if (tunnelBinary.exists()) {
							logger.info("Tunnel Binary already exists");
						} else {
							logger.info("Tunnel Binary not exists, downloading...");
							// saveFileFromUrlWithJavaIO(tunnelBinaryLocation, Constant.MAC_BINARY_URL);
							downloadAndUnZipBinaryFile(tunnelFolderPath.getPath(), latestHash, Constant.MAC_BINARY_URL);
							logger.info("Tunnel Binary downloaded from " + Constant.MAC_BINARY_URL);
						}
						// Get Tunnel Log path name
						String tunnelLogPath = getTunnelLogPath(workspacePath, buildnumber);
						logger.info("Tunnel Log Path:" + tunnelLogPath);
						return runCommandLine(tunnelBinaryLocation, tunnelLogPath, user, key, tunnelName,localTunnel);
					} else {
						logger.warning("tunnelFolderPath empty");
					}
				} else {
					logger.warning("loader empty");
				}
			} catch (Exception e) {
				logger.warning(e.getMessage());
			}
		} else if (OSValidator.isWindows()) {
			logger.info("Jenkins configured on Windows");
			logger.info("Checking for updates "+ Constant.WIN_HASH_URL);
			try {
				// Get Latest Hash
				String latestHash = getLatestHash(Constant.WIN_HASH_URL);
				logger.info(latestHash);
				String tunnelBinaryDirLocation = getTunnelBinaryDirLocation(localTunnel, workspacePath) + "\\";
				logger.info("Tunnel Directory :" + tunnelBinaryDirLocation);
				// Verify Latest binary version
				if (!tunnelBinaryDirLocation.isEmpty()) {
					String tunnelBinaryLocation = tunnelBinaryDirLocation + latestHash;
					logger.info("Tunnel Binary Location :" + tunnelBinaryLocation);
					File tunnelBinary = new File(tunnelBinaryLocation);
					if (tunnelBinary.exists()) {
						logger.info("Tunnel Binary already exists");
					} else {
						String binaryURL = Constant.WIN_BINARY_URL;
						logger.info("Tunnel Binary doesn't exists, Downloading new binary from ..."+ binaryURL);
						downloadAndUnZipBinaryFile(tunnelBinaryDirLocation, latestHash,
								binaryURL);
						logger.info("Tunnel Binary downloaded from " + binaryURL);
					}
					// Get Tunnel Log path name
					String tunnelLogPath = getTunnelLogPathForWindows(workspacePath, buildnumber);
					logger.info("Tunnel Log Path:" + tunnelLogPath);
					return runCommandLine(tunnelBinaryLocation, tunnelLogPath, user, key, tunnelName,localTunnel,Constant.OS.WIN);
				} else {
					logger.warning("tunnelFolderPath empty");
				}
			} catch (Exception e) {
				logger.warning(e.getMessage());
			}
		} else {
			logger.info("Tunnel Option Not Available for this configuration");
		}
		return null;
	}

	private static String getTunnelBinaryDirLocation(LocalTunnel localTunnel, FilePath workspacePath) {
		String tunnelBinaryDirLocation = "";
		if(localTunnel!=null && localTunnel.isUseWorkspacePath()) {
			if (workspacePath != null) {
				// Create a Folder in workspace
				FilePath tunnelFolderPath = new FilePath(workspacePath, Constant.DEFAULT_TUNNEL_FOLDER_NAME);
				File folder = new File(tunnelFolderPath.getRemote());
				if (!folder.exists()) {
					if (folder.mkdir()) {
						logger.info("Directory is created! at " + tunnelFolderPath.getRemote());
						return tunnelFolderPath.getRemote();
					} else {
						logger.info("Failed to create directory! at " + tunnelFolderPath.getRemote());
						return workspacePath.getRemote();
					}
				} else {
					return tunnelFolderPath.getRemote();
				}
			}
		}

		if(localTunnel!=null && !localTunnel.getDownloadTunnelPath().isEmpty()) {
			return localTunnel.getDownloadTunnelPath();
		}
		ClassLoader loader = LambdaWebSocketTunnelService.class.getClassLoader();
		if (loader != null) {
			URL tunnelFolderPath = loader.getResource(tunnelFolderName);
			if (tunnelFolderPath != null) {
				return tunnelFolderPath.getPath();
			}
		}
		return tunnelBinaryDirLocation;
	}

	private static String getTunnelLogPath(FilePath workspacePath, String buildnumber) {
		String tunnelLogPath = "tunnel.log";
		try {
			if (workspacePath != null) {
				// Create Tunnel Log Path
				tunnelLogPath = new StringBuilder("tunnel").append("-").append(buildnumber).append(".log").toString();

				// Create a Folder in workspace
				FilePath tunnelFolderPath = new FilePath(workspacePath, Constant.DEFAULT_TUNNEL_FOLDER_NAME);
				File folder = new File(tunnelFolderPath.getRemote());
				if (!folder.exists()) {
					if (folder.mkdir()) {
						logger.info("Directory is created! at " + tunnelFolderPath.getRemote());
						FilePath tunnelPath = new FilePath(tunnelFolderPath, tunnelLogPath);
						return tunnelPath.getRemote();
					} else {
						logger.info("Failed to create directory! at " + tunnelFolderPath.getRemote());
						FilePath tunnelPath = new FilePath(workspacePath, tunnelLogPath);
						return tunnelPath.getRemote();
					}
				} else {
					FilePath tunnelPath = new FilePath(tunnelFolderPath, tunnelLogPath);
					return tunnelPath.getRemote();
				}
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		return tunnelLogPath;
	}

	private static String getTunnelLogPathForWindows(FilePath workspacePath, String buildnumber) {
		String tunnelLogPath = "tunnel.log";
		try {
			if (workspacePath != null) {
				// Create Tunnel Log Path
				tunnelLogPath = new StringBuilder("tunnel").append("-").append(buildnumber).append(".log").toString();

				// Create a Folder in workspace
				FilePath tunnelFolderPath = new FilePath(workspacePath, Constant.DEFAULT_TUNNEL_FOLDER_NAME);
				File folder = new File(tunnelFolderPath.getRemote());
				if (!folder.exists()) {
					if (folder.mkdir()) {
						logger.info("Directory is created! at " + tunnelFolderPath.getRemote());
						FilePath tunnelPath = new FilePath(tunnelFolderPath, tunnelLogPath);
						return tunnelPath.getRemote();
					} else {
						logger.info("Failed to create directory! at " + tunnelFolderPath.getRemote());
						FilePath tunnelPath = new FilePath(workspacePath, tunnelLogPath);
						return tunnelPath.getRemote();
					}
				} else {
					FilePath tunnelPath = new FilePath(tunnelFolderPath, tunnelLogPath);
					return tunnelPath.getRemote();
				}
			}
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
		return tunnelLogPath;
	}

	private static String getLatestHash(String url) throws TunnelHashNotFoundException {
		try {
			return CapabilityService.sendGetRequest(url);
		} catch (Exception e) {
			throw new TunnelHashNotFoundException(e.getMessage(), e);
		}
	}

	// Using Java IO
	public static void saveFileFromUrlWithJavaIO(String fileName, String fileUrl)
			throws MalformedURLException, IOException {
		BufferedInputStream in = null;
		FileOutputStream fout = null;
		try {
			in = new BufferedInputStream(new URL(fileUrl).openStream());
			fout = new FileOutputStream(fileName);

			byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data, 0, 1024)) != -1) {
				fout.write(data, 0, count);
			}
		} finally {
			if (in != null)
				in.close();
			if (fout != null)
				fout.close();
		}
	}

	private static void downloadAndUnZipBinaryFile(String folderPath, String latestHash, String linuxBinaryUrl) {
		String tunnelBinaryFileName = folderPath + latestHash;
		String tunnelBinaryZipFileName = folderPath + latestHash + ".zip";
		downloadFile(linuxBinaryUrl, tunnelBinaryZipFileName);
		unZipIt(tunnelBinaryZipFileName, tunnelBinaryFileName, folderPath);
	}

	public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}
		in.close();
		out.close();
	}

	private static void downloadFile(String linuxBinaryUrl, String tunnelBinaryFileName) {
		try {
			logger.info(tunnelBinaryFileName);
			BufferedInputStream in = new BufferedInputStream(new URL(linuxBinaryUrl).openStream());
			try (FileOutputStream fileOutputStream = new FileOutputStream(tunnelBinaryFileName)) {
				byte dataBuffer[] = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
					fileOutputStream.write(dataBuffer, 0, bytesRead);
				}
			}
			logger.info("Binary Downloaded.\n");
		} catch (Exception e) {
			logger.info(e.getMessage());
		}
	}

	public static void unZipIt(String tunnelBinaryZipFileName, String tunnelBinaryFileName, String folderPath) {
		try {

			ZipFile zipFile = new ZipFile(tunnelBinaryZipFileName);

			Enumeration zipEntries = zipFile.entries();
			String OUTDIR = folderPath + File.separator;
			while (zipEntries.hasMoreElements()) {
				ZipEntry zipEntry = (ZipEntry) zipEntries.nextElement();
				logger.info("       Extracting file: " + tunnelBinaryFileName);
				copyInputStream(zipFile.getInputStream(zipEntry),
						new BufferedOutputStream(new FileOutputStream(tunnelBinaryFileName)));
			}
			zipFile.close();
		} catch (IOException ioe) {
			logger.info("Unhandled exception:");
			logger.info(ioe.getMessage());
			return;
		}
	}

	public static Process runCommandLine(String filePath, String tunnelLogPath, String user, String key,
										 String tunnelName, LocalTunnel localTunnel,String ...args) throws IOException {
		try {
			int availablePort= PortAvailabilityUtils.randomFreePort();
			//Updating permissions
			if(args.length < 1) {
				Runtime.getRuntime().exec("chmod 777 " + filePath);
			}

			// creating list of process
			List<String> list = new ArrayList<String>();
			list.add(filePath);list.add("--user");list.add(user);
			list.add("--key");list.add(key);list.add("--logFile");list.add(tunnelLogPath);
			list.add("--tunnelName");list.add(tunnelName);list.add("--controller");list.add("jenkins");
			if(localTunnel!=null && localTunnel.isSharedTunnel()) {
				list.add("--shared-tunnel");
			}
			if(availablePort > 0) {
				list.add("--port");list.add(availablePort+"");
			}
			if(localTunnel!=null && !localTunnel.getTunnelExtCommand().isEmpty()) {
				String[] extCommands=localTunnel.getTunnelExtCommand().split(" ");
				list.addAll(Arrays.asList(extCommands));
			}
			list.add("-v");

			// create the process
			ProcessBuilder processBuilder = new ProcessBuilder(list);
			logger.info("Tunnel Binary Command "+processBuilder.command());
			Process tunnelProcess = processBuilder.start();
			Thread commandLineThread = new Thread(() -> {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(tunnelProcess.getInputStream()));
					String line = null;
					while ((line = reader.readLine()) != null) {
						logger.info(line);
					}
				} catch (IOException ex) {
					logger.info(ex.getMessage());
				}
			});
			commandLineThread.setDaemon(true);
			commandLineThread.start();
			logger.info("Tunnel Binary Executed");
			return tunnelProcess;
		} catch (Exception e) {
			logger.info(e.getMessage());
			return null;
		}

	}
}
