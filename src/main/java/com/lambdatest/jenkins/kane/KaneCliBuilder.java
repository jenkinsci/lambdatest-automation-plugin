package com.lambdatest.jenkins.kane;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.io.output.TeeOutputStream;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.lambdatest.jenkins.credential.MagicPlugCredentials;
import com.lambdatest.jenkins.pipeline.Constant;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.slaves.WorkspaceList;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import jenkins.util.BuildListenerAdapter;

/**
 * Build step that runs Kane CLI tests by TestMu AI (Formerly LambdaTest): an
 * inline natural language objective, or a suite of committed _test.md files,
 * either in Chrome on the agent or on the HyperExecute grid.
 */
public class KaneCliBuilder extends Builder implements SimpleBuildStep {

	static final String OUT_DIR = "kane-out";
	private static final String PACKAGE = "@testmuai/kane-cli";
	private static final String CHROME_PATH_VAR = "KANE_CLI_CHROME_PATH";
	private static final Pattern EVIDENCE_HINT = Pattern.compile("kane-cli evidence serve ([^`\\s]+\\.evidence)");

	private String credentialsId;
	private String objective;
	private String testFiles;
	private String url;
	private int timeout = 300;
	private int parallel = 1;
	private boolean failFast;
	private boolean remote;
	private boolean installChrome = true;
	private String kaneVersion = "latest";
	private String extraArgs;
	private boolean archiveResults = true;

	@DataBoundConstructor
	public KaneCliBuilder() {
	}

	public String getCredentialsId() {
		return credentialsId;
	}

	@DataBoundSetter
	public void setCredentialsId(String credentialsId) {
		this.credentialsId = Util.fixEmptyAndTrim(credentialsId);
	}

	public String getObjective() {
		return objective;
	}

	@DataBoundSetter
	public void setObjective(String objective) {
		this.objective = Util.fixEmptyAndTrim(objective);
	}

	public String getTestFiles() {
		return testFiles;
	}

	@DataBoundSetter
	public void setTestFiles(String testFiles) {
		this.testFiles = Util.fixEmptyAndTrim(testFiles);
	}

	public String getUrl() {
		return url;
	}

	@DataBoundSetter
	public void setUrl(String url) {
		this.url = Util.fixEmptyAndTrim(url);
	}

	public int getTimeout() {
		return timeout;
	}

	@DataBoundSetter
	public void setTimeout(int timeout) {
		this.timeout = timeout > 0 ? timeout : 300;
	}

	public int getParallel() {
		return parallel;
	}

	@DataBoundSetter
	public void setParallel(int parallel) {
		this.parallel = parallel > 0 ? parallel : 1;
	}

	public boolean isFailFast() {
		return failFast;
	}

	@DataBoundSetter
	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	public boolean isRemote() {
		return remote;
	}

	@DataBoundSetter
	public void setRemote(boolean remote) {
		this.remote = remote;
	}

	public boolean isInstallChrome() {
		return installChrome;
	}

	@DataBoundSetter
	public void setInstallChrome(boolean installChrome) {
		this.installChrome = installChrome;
	}

	public String getKaneVersion() {
		return kaneVersion;
	}

	@DataBoundSetter
	public void setKaneVersion(String kaneVersion) {
		String v = Util.fixEmptyAndTrim(kaneVersion);
		this.kaneVersion = v == null ? "latest" : v;
	}

	public String getExtraArgs() {
		return extraArgs;
	}

	@DataBoundSetter
	public void setExtraArgs(String extraArgs) {
		this.extraArgs = Util.fixEmptyAndTrim(extraArgs);
	}

	public boolean isArchiveResults() {
		return archiveResults;
	}

	@DataBoundSetter
	public void setArchiveResults(boolean archiveResults) {
		this.archiveResults = archiveResults;
	}

	@Override
	public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
			@Nonnull TaskListener listener) throws InterruptedException, IOException {
		perform(run, workspace, run.getEnvironment(listener), launcher, listener);
	}

	/**
	 * Called by Jenkins 2.241 and later with the step's own environment, so pipeline
	 * withEnv and withCredentials values are visible. Not annotated with @Override
	 * because the plugin still compiles against an older core.
	 */
	public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull EnvVars env,
			@Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
		PrintStream log = listener.getLogger();

		if (objective == null && testFiles == null) {
			throw new AbortException("Kane CLI: set either an objective or test files.");
		}
		if (objective != null && testFiles != null) {
			throw new AbortException("Kane CLI: set only one of objective and test files.");
		}
		if (remote && testFiles == null) {
			throw new AbortException("Kane CLI: HyperExecute runs a test-files suite; set test files.");
		}

		String username;
		String accessKey;
		if (credentialsId != null) {
			MagicPlugCredentials credentials = CredentialsProvider.findCredentialById(credentialsId,
					MagicPlugCredentials.class, run, Collections.emptyList());
			if (credentials == null) {
				throw new AbortException("Kane CLI: could not find TestMu AI credentials with ID " + credentialsId + ".");
			}
			CredentialsProvider.track(run, credentials);
			username = credentials.getUserName();
			accessKey = credentials.getAccessToken().getPlainText();
		} else {
			username = env.get(Constant.LT_USERNAME);
			accessKey = env.get(Constant.LT_ACCESS_KEY);
		}
		if (StringUtils.isBlank(username) || StringUtils.isBlank(accessKey)) {
			throw new AbortException("Kane CLI: choose TestMu AI credentials, or set LT_USERNAME and LT_ACCESS_KEY "
					+ "(TestMu AI dashboard > Settings > Keys).");
		}

		EnvVars runEnv = new EnvVars(env);
		runEnv.put(Constant.LT_USERNAME, username);
		runEnv.put(Constant.LT_ACCESS_KEY, accessKey);
		putIfBlank(runEnv, "KANE_CLI_USER_AGENT", "jenkins-plugin");
		// Runner defaults for a cold CI Chrome; job env with the same names wins.
		putIfBlank(runEnv, "KANE_CLI_CDP_TIMEOUT_MS", "60000");
		putIfBlank(runEnv, "KANE_CLI_CDP_RETRIES", "2");

		FilePath toolDir = WorkspaceList.tempDir(workspace).child("kane-cli");
		toolDir.mkdirs();

		if (installChrome && !remote && StringUtils.isBlank(runEnv.get(CHROME_PATH_VAR))) {
			log.println("[Kane CLI] Installing Chrome");
			ArgumentListBuilder chromeCmd = new ArgumentListBuilder("npx", "-y", "@puppeteer/browsers", "install",
					"chrome@stable", "--path", toolDir.child("chrome").getRemote());
			ByteArrayOutputStream chromeOut = new ByteArrayOutputStream();
			int rc = launch(launcher, chromeCmd).envs(runEnv).pwd(workspace).stdout(chromeOut)
					.stderr(log).join();
			String chromeLine = lastLine(chromeOut.toString(StandardCharsets.UTF_8.name()));
			int space = chromeLine.indexOf(' ');
			if (rc != 0 || space < 0) {
				log.print(chromeOut.toString(StandardCharsets.UTF_8.name()));
				throw new AbortException("Kane CLI: could not install Chrome. Linux agents need unzip for this; "
						+ "or install Chrome on the agent and untick Install Chrome.");
			}
			runEnv.put(CHROME_PATH_VAR, chromeLine.substring(space + 1).trim());
			log.println("[Kane CLI] Using " + chromeLine);
		}

		log.println("[Kane CLI] Installing " + PACKAGE + "@" + kaneVersion);
		ArgumentListBuilder installCmd = new ArgumentListBuilder("npm", "install", "--no-audit", "--no-fund",
				"--prefix", toolDir.getRemote(), PACKAGE + "@" + kaneVersion);
		if (launch(launcher, installCmd).envs(runEnv).pwd(workspace).stdout(listener).join() != 0) {
			throw new AbortException("Kane CLI: npm install failed. The agent needs Node.js 20 or later with npm.");
		}
		String kane = toolDir.child("node_modules").child(".bin").child(launcher.isUnix() ? "kane-cli" : "kane-cli.cmd")
				.getRemote();
		launch(launcher, new ArgumentListBuilder(kane, "--version")).envs(runEnv).pwd(workspace).stdout(listener)
				.join();
		if (remote && launch(launcher, new ArgumentListBuilder(kane, "plugin", "install", "remote-execution"))
				.envs(runEnv).pwd(workspace).stdout(listener).join() != 0) {
			throw new AbortException("Kane CLI: could not install the remote-execution plugin for HyperExecute.");
		}

		ArgumentListBuilder cmd = new ArgumentListBuilder(kane);
		if (testFiles != null) {
			FilePath[] matches = workspace.list(testFiles);
			if (matches.length == 0) {
				throw new AbortException("Kane CLI: no files matched " + testFiles + ".");
			}
			Arrays.sort(matches, Comparator.comparing(FilePath::getRemote));
			cmd.add("testrun", "run");
			for (FilePath match : matches) {
				cmd.add(relativize(workspace, match));
			}
			cmd.add("--parallel", String.valueOf(parallel));
			cmd.add("--name", env.get("JOB_NAME", "kane") + " #" + env.get("BUILD_NUMBER", "0"));
			cmd.add(remote ? "--remote" : "--headless");
			if (failFast) {
				cmd.add("--on-failure", "fail-fast");
			}
		} else {
			cmd.add("run", objective, "--agent", "--headless", "--timeout", String.valueOf(timeout));
			if (url != null) {
				cmd.add("--url", url);
			}
		}
		if (extraArgs != null) {
			cmd.add(Util.tokenize(extraArgs));
		}
		cmd.add("--username", username);
		cmd.add("--access-key");
		cmd.addMasked(accessKey);

		FilePath out = workspace.child(OUT_DIR);
		if (out.exists()) {
			out.deleteContents();
		}
		FilePath evidenceDir = out.child("evidence");
		evidenceDir.mkdirs();
		FilePath stream = out.child("stream.ndjson");
		FilePath errLog = out.child("stderr.log");

		log.println("[Kane CLI] Running " + (testFiles != null ? "the test suite" : "the objective")
				+ (remote ? " on HyperExecute" : ""));
		int rc;
		try (OutputStream streamFile = stream.write(); OutputStream errFile = errLog.write()) {
			rc = launch(launcher, cmd).envs(runEnv).pwd(workspace)
					.stdout(new TeeOutputStream(log, streamFile))
					.stderr(new TeeOutputStream(log, errFile)).join();
		}

		KaneCliSummary summary = KaneCliSummary.parse(stream.readToString(), rc,
				testFiles != null ? "suite" : "inline objective", workspace.getRemote());
		collectEvidence(workspace, evidenceDir, errLog.readToString(), summary.getSessionDir(), runEnv);

		log.println();
		log.println(summary.describe());
		for (String row : summary.getRows()) {
			log.println("  " + row);
		}
		if (StringUtils.isNotBlank(summary.getLink())) {
			log.println("Test Manager: " + summary.getLink());
		}

		if (archiveResults) {
			Map<String, String> artifacts = new LinkedHashMap<>();
			for (FilePath f : out.list("**/*")) {
				String path = relativize(workspace, f);
				artifacts.put(path, path);
			}
			if (!artifacts.isEmpty()) {
				run.pickArtifactManager().archive(workspace, launcher, new BuildListenerAdapter(listener), artifacts);
				log.println("[Kane CLI] Archived the evidence pack and logs under " + OUT_DIR
						+ "/. Open a pack with kane-cli evidence serve <pack>.");
			}
		}

		if (!summary.isPassed()) {
			throw new AbortException("Kane CLI: " + summary.getStatus() + " (exit code " + rc + ").");
		}
	}

	/** Copies sealed evidence packs from the places kane-cli reports into kane-out/evidence. */
	private static void collectEvidence(FilePath workspace, FilePath evidenceDir, String stderr,
			@CheckForNull String sessionDir, EnvVars env) throws IOException, InterruptedException {
		String home = env.get("HOME", env.get("USERPROFILE", ""));
		String hinted = null;
		Matcher m = EVIDENCE_HINT.matcher(stderr);
		while (m.find()) {
			hinted = m.group(1);
		}
		if (hinted != null) {
			FilePath pack = new FilePath(workspace.getChannel(), expandHome(hinted, home));
			if (pack.exists() && !pack.isDirectory()) {
				copyIfMissing(pack, evidenceDir);
			}
		}
		FilePath[] dirs = {
				sessionDir == null ? null
						: new FilePath(workspace.getChannel(), expandHome(sessionDir, home)).child("evidence"),
				workspace.child(".testmuai").child("evidence") };
		for (FilePath dir : dirs) {
			if (dir != null && dir.isDirectory()) {
				for (FilePath pack : dir.list("*.evidence")) {
					copyIfMissing(pack, evidenceDir);
				}
			}
		}
	}

	private static void copyIfMissing(FilePath pack, FilePath dir) throws IOException, InterruptedException {
		FilePath target = dir.child(pack.getName());
		if (!target.exists()) {
			pack.copyTo(target);
		}
	}

	private static String expandHome(String path, String home) {
		return path.startsWith("~") && !home.isEmpty() ? home + path.substring(1) : path;
	}

	private static Launcher.ProcStarter launch(Launcher launcher, ArgumentListBuilder args) {
		return launcher.launch().cmds(launcher.isUnix() ? args : args.toWindowsCommand());
	}

	private static void putIfBlank(EnvVars env, String key, String value) {
		if (StringUtils.isBlank(env.get(key))) {
			env.put(key, value);
		}
	}

	private static String lastLine(String text) {
		String[] lines = text.trim().split("\\r?\\n");
		return lines[lines.length - 1].trim();
	}

	private static String relativize(FilePath workspace, FilePath file) {
		String base = workspace.getRemote();
		String path = file.getRemote();
		if (path.startsWith(base) && path.length() > base.length()) {
			path = path.substring(base.length() + 1);
		}
		return path.replace('\\', '/');
	}

	@Symbol("kaneCli")
	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		@Override
		public boolean isApplicable(@SuppressWarnings("rawtypes") Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Run Kane CLI tests (TestMu AI)";
		}

		public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
			StandardListBoxModel result = new StandardListBoxModel();
			if (item == null ? !Jenkins.get().hasPermission(Jenkins.ADMINISTER)
					: !item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
				return result.includeCurrentValue(credentialsId);
			}
			return result.includeEmptyValue()
					.withMatching(CredentialsMatchers.always(), CredentialsProvider.lookupCredentials(
							MagicPlugCredentials.class, item, ACL.SYSTEM, Collections.emptyList()))
					.includeCurrentValue(credentialsId);
		}

		public FormValidation doCheckTestFiles(@QueryParameter String testFiles, @QueryParameter String objective) {
			if (StringUtils.isNotBlank(testFiles) && StringUtils.isNotBlank(objective)) {
				return FormValidation.error("Set only one of objective and test files.");
			}
			return FormValidation.ok();
		}
	}
}
