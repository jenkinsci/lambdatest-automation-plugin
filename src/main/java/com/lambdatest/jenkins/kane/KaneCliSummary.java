package com.lambdatest.jenkins.kane;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Reads a kane-cli NDJSON stream and the process exit code into a verdict,
 * one row per test, and the Test Manager link.
 *
 * Handles three stream shapes:
 *   kane-cli run          -> terminal run_end
 *   kane-cli testmd run   -> one run_end per step, terminal test_md_done
 *   kane-cli testrun run  -> testrun_plan, testrun_member_end / testrun_authored_member_end,
 *                            testrun_summary, testrun_done
 */
final class KaneCliSummary {

	private final List<JSONObject> events = new ArrayList<>();
	private final List<String> rows = new ArrayList<>();
	private final String workspacePrefix;
	private String status = "";
	private int total;
	private int passed;
	private String duration = "";
	private String link = "";
	private String detail = "";
	private String sessionDir;
	private boolean ok;

	private KaneCliSummary(String workspace) {
		this.workspacePrefix = workspace.endsWith("/") || workspace.endsWith("\\") ? workspace : workspace + "/";
	}

	static KaneCliSummary parse(String stream, int rc, String label, String workspace) {
		KaneCliSummary s = new KaneCliSummary(workspace);
		for (String line : stream.split("\\r?\\n")) {
			if (line.trim().isEmpty()) {
				continue;
			}
			try {
				s.events.add(new JSONObject(line));
			} catch (JSONException e) {
				// not JSON, skip
			}
		}
		s.summarise(rc, label);
		return s;
	}

	private void summarise(int rc, String label) {
		boolean isTestrun = false;
		for (JSONObject e : events) {
			if (e.optString("type").startsWith("testrun_")) {
				isTestrun = true;
			}
			if ("run_end".equals(e.optString("type")) && !e.optString("session_dir").isEmpty()) {
				sessionDir = e.optString("session_dir");
			}
		}

		if (isTestrun) {
			summariseTestrun();
		} else {
			JSONObject done = last("test_md_done");
			JSONObject end = last("run_end");
			if (done != null) {
				status = done.optString("overall_status");
				duration = num(done.opt("duration_s"));
				link = done.optString("share_url");
			} else if (end != null) {
				status = end.optString("status");
				duration = num(end.opt("duration"));
				link = end.optString("test_url");
			}
			detail = end != null ? clean(end.optString("summary")) : "";
			total = 1;
		}

		// Fold the process exit code in. It wins over anything the stream said.
		switch (rc) {
		case 0:
			status = status.isEmpty() ? "unknown" : status;
			break;
		case 1:
			status = status.isEmpty() ? "failed" : status;
			break;
		case 2:
			status = "invalid plan".equals(status) ? status : "error";
			break;
		case 3:
			status = isTestrun ? "cancelled" : "timeout";
			break;
		default:
			status = "exit " + rc;
		}
		ok = rc == 0 && "passed".equals(status);
		if (!isTestrun) {
			passed = ok ? 1 : 0;
			rows.clear();
			rows.add(row(label, ok, status, duration));
		} else if (rows.isEmpty()) {
			rows.add(row(label, ok, status, duration));
		}
		if (total == 0) {
			total = 1;
		}
	}

	private void summariseTestrun() {
		JSONObject plan = last("testrun_plan");
		JSONObject done = last("testrun_done");
		JSONObject sum = last("testrun_summary");
		List<JSONObject> ends = new ArrayList<>();
		for (JSONObject e : events) {
			String type = e.optString("type");
			if ("testrun_member_end".equals(type) || "testrun_authored_member_end".equals(type)) {
				ends.add(e);
			}
		}
		JSONArray members = plan != null && plan.optJSONArray("members") != null ? plan.optJSONArray("members")
				: new JSONArray();

		if (plan != null && plan.has("valid") && !plan.optBoolean("valid", true)) {
			for (int i = 0; i < members.length(); i++) {
				JSONObject m = members.optJSONObject(i);
				if (m == null) {
					continue;
				}
				String failure = clean(m.optString("failure"));
				rows.add(row(display(m.optString("path")), false,
						failure.isEmpty() ? "not run" : "rejected (" + failure + ")", ""));
			}
			status = "invalid plan";
			total = members.length();
			detail = "the suite plan is invalid, nothing ran";
			return;
		}

		boolean cancelled = (sum != null && sum.optBoolean("cancelled"))
				|| (done != null && "cancelled".equals(done.optString("overall_status")));
		Set<JSONObject> used = new HashSet<>();
		for (int i = 0; i < members.length(); i++) {
			JSONObject m = members.optJSONObject(i);
			if (m == null) {
				continue;
			}
			JSONObject end = null;
			for (JSONObject e : ends) {
				if (samePath(e.optString("path"), m.optString("path"))) {
					end = e;
					break;
				}
			}
			String st = end != null ? end.optString("status") : (cancelled ? "cancelled" : "not run");
			boolean memberOk = end != null && "passed".equals(end.optString("status"));
			rows.add(row(display(m.optString("path")), memberOk, st, end != null ? num(end.opt("duration_s")) : ""));
			if (end != null) {
				used.add(end);
			}
			if (memberOk) {
				passed++;
			}
		}
		for (JSONObject end : ends) {
			if (used.contains(end)) {
				continue;
			}
			boolean memberOk = "passed".equals(end.optString("status"));
			rows.add(row(display(end.optString("path")), memberOk, end.optString("status"),
					num(end.opt("duration_s"))));
			if (memberOk) {
				passed++;
			}
		}
		total = rows.size();
		List<String> notes = new ArrayList<>();
		if (sum != null) {
			JSONObject totals = sum.optJSONObject("totals");
			if (totals != null) {
				if (totals.has("tests")) {
					total = totals.optInt("tests", total);
				}
				if (totals.has("passed")) {
					passed = totals.optInt("passed", passed);
				}
				if (totals.optInt("broken") > 0) {
					notes.add(totals.optInt("broken") + " broken");
				}
				if (totals.optInt("skipped") > 0) {
					notes.add(totals.optInt("skipped") + " skipped");
				}
				if (totals.optInt("authored") > 0) {
					notes.add(totals.optInt("authored") + " authored on this run");
				}
			}
			duration = num(sum.opt("duration_s"));
			String upload = sum.optString("upload");
			if (!upload.isEmpty() && !"ok".equals(upload)) {
				notes.add("evidence upload " + upload);
			}
			JSONObject execution = sum.optJSONObject("execution");
			if (execution != null && !execution.optString("id").isEmpty()) {
				notes.add("execution " + execution.optString("id"));
			}
		}
		if (cancelled) {
			notes.add("cancelled before every member finished");
		}
		if (done != null) {
			status = done.optString("overall_status");
		} else if (sum != null && sum.optJSONObject("execution") != null) {
			status = sum.optJSONObject("execution").optString("status");
		}
		detail = String.join(", ", notes);
	}

	private JSONObject last(String type) {
		for (int i = events.size() - 1; i >= 0; i--) {
			if (type.equals(events.get(i).optString("type"))) {
				return events.get(i);
			}
		}
		return null;
	}

	private String display(String path) {
		return path.startsWith(workspacePrefix) ? path.substring(workspacePrefix.length()) : path;
	}

	private static boolean samePath(String a, String b) {
		if (a.isEmpty() || b.isEmpty()) {
			return false;
		}
		return a.equals(b) || a.endsWith("/" + b) || b.endsWith("/" + a);
	}

	private static String row(String name, boolean ok, String status, String duration) {
		return (ok ? "PASS  " : "FAIL  ") + clean(name) + "  " + clean(status) + "  "
				+ (duration.isEmpty() ? "?s" : duration + "s");
	}

	private static String clean(String v) {
		return v == null ? "" : v.replaceAll("\\s+", " ").trim();
	}

	private static String num(Object v) {
		if (v == null || v == JSONObject.NULL) {
			return "";
		}
		try {
			return String.format(Locale.ROOT, "%.1f", Double.parseDouble(String.valueOf(v)));
		} catch (NumberFormatException e) {
			return "";
		}
	}

	String describe() {
		StringBuilder sb = new StringBuilder("Kane CLI: ").append(ok ? "passed" : "failed").append(". ")
				.append(passed).append('/').append(total).append(" tests passed");
		if (!duration.isEmpty()) {
			sb.append(" in ").append(duration).append('s');
		}
		if (!detail.isEmpty()) {
			sb.append(". ").append(detail);
		}
		return sb.toString();
	}

	boolean isPassed() {
		return ok;
	}

	String getStatus() {
		return status;
	}

	String getLink() {
		return link;
	}

	List<String> getRows() {
		return rows;
	}

	String getSessionDir() {
		return sessionDir;
	}
}
