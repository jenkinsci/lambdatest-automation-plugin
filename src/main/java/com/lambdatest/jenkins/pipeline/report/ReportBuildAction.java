package com.lambdatest.jenkins.pipeline.report;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import hudson.model.Run;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.lambdatest.jenkins.pipeline.Constant;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.logging.Logger;
import com.lambdatest.jenkins.pipeline.enums.ProjectType;

public class ReportBuildAction extends AbstractReportBuildAction {
  private final static Logger logger = Logger.getLogger(ReportBuildAction.class.getName());
  private String lambdaTestBuildBrowserUrl;
  private String authString;
  private String buildName;
  private String product;
  private transient List < JSONObject > result = new ArrayList < JSONObject > ();

  public ReportBuildAction(final Run < ? , ? > build, String name, String password, String buildName, ProjectType product) {
    super();
    setBuild(build);
    logger.info(build.toString());
    this.authString = name + ":" + password;
    this.buildName = buildName;
    this.product = product.toString();

  }

  public void generateLambdaTestReport() {

    byte[] authEncBytes = Base64.getEncoder().encode(authString.getBytes());
    String authStringEnc = new String(authEncBytes);
    try {

      if (product == "AUTOMATION") {

        URL buildUrl = new URL(Constant.Report.BUILD_INFO_URL);
        URLConnection buildUrlConnection = buildUrl.openConnection();
        buildUrlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        InputStream is = buildUrlConnection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        int numCharsReadForBuild;
        char[] charArrayForBuild = new char[1024];
        StringBuffer sb = new StringBuffer();
        while ((numCharsReadForBuild = isr.read(charArrayForBuild)) > 0) {
          sb.append(charArrayForBuild, 0, numCharsReadForBuild);
        }
        String buildResult = sb.toString();
        try {
          ObjectMapper mapper = new ObjectMapper();
          JsonNode rootNode = mapper.readTree(buildResult);
          ArrayNode dataNode = (ArrayNode) rootNode.get("data");
          Iterator < JsonNode > dataIterator = dataNode.elements();
          while (dataIterator.hasNext()) {
            JsonNode buildDetailNode = dataIterator.next();
            if (buildDetailNode.get("name").toString().replaceAll("\"", "").equals(buildName)) {
              String build_id = buildDetailNode.get("build_id").toString();

              lambdaTestBuildBrowserUrl = "https://automation.lambdatest.com/logs/?build=" + build_id;

              logger.info("lambdaTestBuildBrowserUrl : " + lambdaTestBuildBrowserUrl);
              URL sessionUrl = new URL(Constant.Report.SESSION_INFO_URL + "?limit=100&build_id=" + build_id);
              URLConnection sessionUrlConnection = sessionUrl.openConnection();
              sessionUrlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
              is = sessionUrlConnection.getInputStream();
              isr = new InputStreamReader(is);
              int numCharsReadForSession;
              char[] charArrayForSession = new char[1024];
              sb = new StringBuffer();
              while ((numCharsReadForSession = isr.read(charArrayForSession)) > 0) {
                sb.append(charArrayForSession, 0, numCharsReadForSession);
              }
              String sessionResult = sb.toString();

              JsonNode sessionRootNode = mapper.readTree(sessionResult);
              ArrayNode sessionDataNode = (ArrayNode) sessionRootNode.get("data");
              Iterator < JsonNode > sessionDataIterator = sessionDataNode.elements();

              JSONArray array = new JSONArray();
              while (sessionDataIterator.hasNext()) {
                JsonNode sessionDetailNode = sessionDataIterator.next();
                String testUrl = "https://automation.lambdatest.com/logs/?testID=" + sessionDetailNode.get("test_id").toString().replaceAll("\"", "") + "&page=1&build=" + build_id;
                logger.info("testUrl : " + testUrl);
                JSONObject resultJSON = new JSONObject();
                resultJSON.put("url", testUrl);
                resultJSON.put("status", sessionDetailNode.get("status_ind").toString().replaceAll("\"", ""));
                if (sessionDetailNode.has("device")) {
                  resultJSON.put("browser", sessionDetailNode.get("device").toString().replaceAll("\"", ""));
                } else {
                  resultJSON.put("browser", sessionDetailNode.get("browser").toString().replaceAll("\"", ""));
                }
                if(sessionDetailNode.has("version")) {
                  resultJSON.put("browserVersion", sessionDetailNode.get("version").toString().replaceAll("\"", ""));
                } else {
                  resultJSON.put("browserVersion","1.0");
                }
                resultJSON.put("OS", sessionDetailNode.get("platform").toString().replaceAll("\"", ""));
                resultJSON.put("name", sessionDetailNode.get("name").toString().replaceAll("\"", ""));
                resultJSON.put("testId", sessionDetailNode.get("test_id").toString().replaceAll("\"", ""));
                resultJSON.put("testDuration", sessionDetailNode.get("duration").toString().replaceAll("\"", ""));
                array.put(resultJSON);
              }
              for (int i = 0; i < array.length(); i++) {
                result.add(array.getJSONObject(i));
              }
              logger.info(result.toString());
              break;
            }
          }
        } catch (JsonParseException e) {
          logger.warning(e.getMessage());
        } catch (JsonMappingException e) {
          logger.warning(e.getMessage());
        } catch (IOException e) {
          logger.warning(e.getMessage());
        } catch (JSONException e) {
          logger.warning(e.getMessage());
        }
      } else {

        URL buildUrl = new URL(Constant.Report.BUILD_APP_INFO_URL);
        URLConnection buildUrlConnection = buildUrl.openConnection();
        buildUrlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        InputStream is = buildUrlConnection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        int numCharsReadForBuild;
        char[] charArrayForBuild = new char[1024];
        StringBuffer sb = new StringBuffer();
        while ((numCharsReadForBuild = isr.read(charArrayForBuild)) > 0) {
          sb.append(charArrayForBuild, 0, numCharsReadForBuild);
        }
        String buildResult = sb.toString();
        try {
          ObjectMapper mapper = new ObjectMapper();
          JsonNode rootNode = mapper.readTree(buildResult);
          ArrayNode dataNode = (ArrayNode) rootNode.get("data");
          Iterator < JsonNode > dataIterator = dataNode.elements();
          while (dataIterator.hasNext()) {
            JsonNode buildDetailNode = dataIterator.next();
            if (buildDetailNode.get("name").toString().replaceAll("\"", "").equals(buildName)) {
              String build_id = buildDetailNode.get("build_id").toString();

              lambdaTestBuildBrowserUrl = "https://appautomation.lambdatest.com/build?buildid=" + build_id;

              logger.info("lambdaTestBuildBrowserUrl : " + lambdaTestBuildBrowserUrl);
              URL sessionUrl = new URL(Constant.Report.SESSION_APP_INFO_URL + "?limit=100&build_id=" + build_id);
              URLConnection sessionUrlConnection = sessionUrl.openConnection();
              sessionUrlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
              is = sessionUrlConnection.getInputStream();
              isr = new InputStreamReader(is);
              int numCharsReadForSession;
              char[] charArrayForSession = new char[1024];
              sb = new StringBuffer();
              while ((numCharsReadForSession = isr.read(charArrayForSession)) > 0) {
                sb.append(charArrayForSession, 0, numCharsReadForSession);
              }
              String sessionResult = sb.toString();

              JsonNode sessionRootNode = mapper.readTree(sessionResult);
              ArrayNode sessionDataNode = (ArrayNode) sessionRootNode.get("data");
              Iterator < JsonNode > sessionDataIterator = sessionDataNode.elements();

              JSONArray array = new JSONArray();
              while (sessionDataIterator.hasNext()) {
                JsonNode sessionDetailNode = sessionDataIterator.next();
                String testUrl = "https://appautomation.lambdatest.com/test?testID=" + sessionDetailNode.get("test_id").toString().replaceAll("\"", "") + "&page=1&build=" + build_id;
                logger.info("testUrl : " + testUrl);
                JSONObject resultJSON = new JSONObject();

                logger.info(sessionDetailNode.toString());
                resultJSON.put("url", testUrl);
                resultJSON.put("status", sessionDetailNode.get("status_ind").toString().replaceAll("\"", ""));

                resultJSON.put("name", sessionDetailNode.get("name").toString().replaceAll("\"", ""));
                if (sessionDetailNode.has("version")) {
                  String version = sessionDetailNode.get("version").toString().replaceAll("\"", "");
                  String OS = sessionDetailNode.get("platform").toString().replaceAll("\"", "");
                  resultJSON.put("OS", OS + " " + version);
                } else {
                  resultJSON.put("OS", sessionDetailNode.get("platform").toString().replaceAll("\"", ""));
                }
                if (sessionDetailNode.has("device")) {
                  resultJSON.put("browser", sessionDetailNode.get("device").toString().replaceAll("\"", ""));
                }
                resultJSON.put("testDuration", sessionDetailNode.get("duration").toString().replaceAll("\"", ""));

                resultJSON.put("testId", sessionDetailNode.get("test_id").toString().replaceAll("\"", ""));

                array.put(resultJSON);
              }
              for (int i = 0; i < array.length(); i++) {
                result.add(array.getJSONObject(i));
              }
              logger.info(result.toString());
              break;
            }
          }
        } catch (JsonParseException e) {
          logger.warning(e.getMessage());
        } catch (JsonMappingException e) {
          logger.warning(e.getMessage());
        } catch (IOException e) {
          logger.warning(e.getMessage());
        } catch (JSONException e) {
          logger.warning(e.getMessage());
        }
      }

    } catch (MalformedURLException e) {
      logger.warning(e.getMessage());
    } catch (IOException e) {
      logger.warning(e.getMessage());
    }
  }

  public List < JSONObject > getResult() {
    return result;

  }

  public String getProduct() {
    return product.toString();

  }

  public String getLambdaTestBuildBrowserUrl() {
    return lambdaTestBuildBrowserUrl;
  }

  public String getBuildName() {
    return buildName;
  }
}