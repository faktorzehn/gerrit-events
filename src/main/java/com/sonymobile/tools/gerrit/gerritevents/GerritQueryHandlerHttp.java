package com.sonymobile.tools.gerrit.gerritevents;

import com.sonymobile.tools.gerrit.gerritevents.ssh.SshException;
import io.restassured.response.Response;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;

/**
 * This class helps you call gerrit query to search for patch-sets. instead of using SSH, it calls Gerrit's Rest api
 *
 * @author Manuel Mühlberger &lt;Manuel.Muehlberger@faktorzehn.de&gt;
 */
public class GerritQueryHandlerHttp {

  static final int STATUS_OK = 200;
  static final int STATUS_BAD_REQUEST = 400;
  static final int STATUS_BAD_CREDENTIALS = 401;
  static final int STATUS_NOT_FOUND = 404;
  static final String GET_ALL_REVISIONS = "&o=ALL_REVISIONS";
  static final String GET_CURRENT_REVISION = "&o=CURRENT_REVISION";
  static final String GET_CURRENT_FILES = "&o=CURRENT_FILES";
  static final String GET_CURRENT_COMMIT = "&o=CURRENT_COMMIT";
  static final String GET_MESSAGES = "&o=MESSAGES";

  /**
   * Logger instance.
   * Set protected to allow it  to be used in subclasses.
   */
  protected static final Logger logger = LoggerFactory.getLogger(GerritQueryHandlerHttp.class);

  /**
   * The base of the query HTTP command to send to Gerrit.
   */
  private final String httpBaseUrl;
  private final String proxy;
  private final Credential credential;


  /**
   * Creates a GerritQueryHandlerHTTP with the following parameters.
   *
   * @param frontEndUrl the front-end-url specified
   * @param credential the credentials, contains the username and password
   * @param gerritProxy the (optional) proxy
   */
  public GerritQueryHandlerHttp(String frontEndUrl, Credential credential, String gerritProxy) {
    this.httpBaseUrl = frontEndUrl;
    this.credential = credential;
    this.proxy = gerritProxy;
  }


  //CS IGNORE RedundantThrows FOR NEXT 18 LINES. REASON: Informative.
  //CS IGNORE JavadocMethod FOR NEXT 17 LINES. REASON: It is there.

  /**
   * Runs the query and returns the result as a list of Java JSONObjects.
   * It is the equivalent of calling queryJava(queryString, true, true, false, false).
   *
   * @param queryString the query.
   * @return the query result as a List of JSONObjects.
   * @throws GerritQueryException if Gerrit reports an error with the query.
   * @throws SshException         if there is an error in the SSH Connection.
   * @throws IOException          for some other IO problem.
   */
  public List<JSONObject> queryJava(String queryString) throws SshException, IOException, GerritQueryException {
    return queryJava(queryString, true, true, false, false);
  }

  //CS IGNORE RedundantThrows FOR NEXT 22 LINES. REASON: Informative.
  //CS IGNORE JavadocMethod FOR NEXT 17 LINES. REASON: It is there.

  /**
   * Runs the query and returns the result as a list of Java JSONObjects.
   *
   * @param queryString        the query.
   * @param getPatchSets       getPatchSets if all patch-sets of the projects found should be included in the result.
   *                           Meaning if --patch-sets should be appended to the command call.
   * @param getCurrentPatchSet if the current patch-set for the projects found should be included in the result.
   *                           Meaning if --current-patch-set should be appended to the command call.
   * @param getFiles           if the files of the patch sets should be included in the result.
   *                           Meaning if --files should be appended to the command call.
   * @return the query result as a List of JSONObjects.
   * @throws GerritQueryException if Gerrit reports an error with the query.
   * @throws SshException         if there is an error in the SSH Connection.
   * @throws IOException          for some other IO problem.
   */
  public List<JSONObject> queryJava(String queryString, boolean getPatchSets, boolean getCurrentPatchSet,
                                    boolean getFiles) throws SshException, IOException, GerritQueryException {
    return queryJava(queryString, getPatchSets, getCurrentPatchSet, getFiles, false);
  }

  //CS IGNORE RedundantThrows FOR NEXT 22 LINES. REASON: Informative.

  /**
   * Runs the query and returns the result as a list of Java JSONObjects.
   *
   * @param queryString        the query.
   * @param getPatchSets       getPatchSets if all patch-sets of the projects found should be included in the result.
   *                           Meaning if --patch-sets should be appended to the command call.
   * @param getCurrentPatchSet if the current patch-set for the projects found should be included in the result.
   *                           Meaning if --current-patch-set should be appended to the command call.
   * @param getFiles           if the files of the patch sets should be included in the result.
   *                           Meaning if --files should be appended to the command call.
   * @param getCommitMessage   if full commit message should be included in the result.
   *                           Meaning if --commit-message should be appended to the command call.
   * @return the query result as a List of JSONObjects.
   * @throws GerritQueryException if Gerrit reports an error with the query.
   * @throws SshException         if there is an error in the SSH Connection.
   * @throws IOException          for some other IO problem.
   */
  public List<JSONObject> queryJava(String queryString, boolean getPatchSets, boolean getCurrentPatchSet,
                                    boolean getFiles, boolean getCommitMessage) throws SshException,
      IOException, GerritQueryException {
    return queryJava(queryString, getPatchSets, getCurrentPatchSet, getFiles, getCommitMessage, false);
  }

  //CS IGNORE RedundantThrows FOR NEXT 24 LINES. REASON: Informative.

  /**
   * Runs the query and returns the result as a list of Java JSONObjects.
   *
   * @param queryString        the query.
   * @param getPatchSets       getPatchSets if all patch-sets of the projects found should be included in the result.
   *                           Meaning if --patch-sets should be appended to the command call.
   * @param getCurrentPatchSet if the current patch-set for the projects found should be included in the result.
   *                           Meaning if --current-patch-set should be appended to the command call.
   * @param getFiles           if the files of the patch sets should be included in the result.
   *                           Meaning if --files should be appended to the command call.
   * @param getCommitMessage   if full commit message should be included in the result.
   *                           Meaning if --commit-message should be appended to the command call.
   * @param getComments        if patchset comments should be included in the results.
   *                           Meaning if --comments should be appended to the command call.
   * @return the query result as a List of JSONObjects.
   * @throws GerritQueryException if Gerrit reports an error with the query.
   * @throws IOException          for some other IO problem.
   */
  public List<JSONObject> queryJava(String queryString, boolean getPatchSets, boolean getCurrentPatchSet,
                                           boolean getFiles, boolean getCommitMessage, boolean getComments)
      throws IOException, GerritQueryException {

    List<JSONObject> list = new ArrayList<>();
    Consumer<JSONObject> lineVisitor = new Consumer<JSONObject>() {
      @Override
      public void accept(JSONObject jsonObject) {
        list.add(jsonObject);
      }
    };
    runQuery(queryString, getPatchSets, getCurrentPatchSet, getFiles, getCommitMessage, getComments, lineVisitor);
    return list;
  }


  //CS IGNORE RedundantThrows FOR NEXT 18 LINES. REASON: Informative.
  //CS IGNORE JavadocMethod FOR NEXT 17 LINES. REASON: It is there.

  /**
   * Runs the query and returns the result as a list of Java JSONObjects.
   *
   * @param queryString the query.
   * @return the query result as a List of JSONObjects.
   * @throws GerritQueryException if Gerrit reports an error with the query.
   * @throws SshException         if there is an error in the SSH Connection.
   * @throws IOException          for some other IO problem.
   */
  public List<JSONObject> queryFiles(String queryString) throws
      SshException, IOException, GerritQueryException {
    return queryJava(queryString, false, true, true, false);
  }

  //CS IGNORE RedundantThrows FOR NEXT 18 LINES. REASON: Informative.
  //CS IGNORE JavadocMethod FOR NEXT 17 LINES. REASON: It is there.

  /**
   * Runs the query and returns the result as a list of Java JSONObjects.
   *
   * @param queryString the query.
   * @return the query result as a List of JSONObjects.
   * @throws GerritQueryException if Gerrit reports an error with the query.
   * @throws SshException         if there is an error in the SSH Connection.
   * @throws IOException          for some other IO problem.
   */
  public List<JSONObject> queryCurrentPatchSets(String queryString) throws
      SshException, IOException, GerritQueryException {
    return queryJava(queryString, false, true, false, false);
  }


  //CS IGNORE RedundantThrows FOR NEXT 17 LINES. REASON: Informative.

  /**
   * Runs the query and returns the result as a list of JSON formatted strings.
   * This is the equivalent of calling queryJava(queryString, true, true, false, false).
   *
   * @param queryString the query.
   * @return a List of JSON formatted strings.
   * @throws IOException  for connection problems.
   * @throws GerritQueryException if Gerrit reports an error with the query
   */
  public List<String> queryJson(String queryString) throws GerritQueryException, IOException {
    return queryJson(queryString, true, true, false, false);
  }

  //CS IGNORE RedundantThrows FOR NEXT 17 LINES. REASON: Informative.

  /**
   * Runs the query and returns the result as a list of JSON formatted strings.
   *
   * @param queryString        the query.
   * @param getPatchSets       if all patch-sets of the projects found should be included in the result.
   *                           Meaning if --patch-sets should be appended to the command call.
   * @param getCurrentPatchSet if the current patch-set for the projects found should be included in the result.
   *                           Meaning if --current-patch-set should be appended to the command call.
   * @param getFiles           if the files of the patch sets should be included in the result.
   *                           Meaning if --files should be appended to the command call.
   * @return a List of JSON formatted strings.
   * @throws GerritQueryException if Gerrit reports an error with the query
   * @throws IOException  for some other IO problem.
   */
  public List<String> queryJson(String queryString, boolean getPatchSets, boolean getCurrentPatchSet, boolean getFiles)
      throws GerritQueryException, IOException {
    return queryJson(queryString, getPatchSets, getCurrentPatchSet, getFiles, false);
  }

  //CS IGNORE RedundantThrows FOR NEXT 20 LINES. REASON: Informative.

  /**
   * Runs the query and returns the result as a list of JSON formatted strings.
   *
   * @param queryString        the query.
   * @param getPatchSets       if all patch-sets of the projects found should be included in the result.
   *                           Meaning if --patch-sets should be appended to the command call.
   * @param getCurrentPatchSet if the current patch-set for the projects found should be included in the result.
   *                           Meaning if --current-patch-set should be appended to the command call.
   * @param getFiles           if the files of the patch sets should be included in the result.
   *                           Meaning if --files should be appended to the command call.
   * @param getCommitMessage   if full commit message should be included in the result.
   *                           Meaning if --commit-message should be appended to the command call.
   * @return a List of JSON formatted strings.
   * @throws GerritQueryException if Gerrit reports an error with the query
   * @throws IOException  for some other IO problem.
   */
  public List<String> queryJson(String queryString, boolean getPatchSets, boolean getCurrentPatchSet,
                                boolean getFiles, boolean getCommitMessage)
      throws GerritQueryException, IOException {

    List<String> list = new ArrayList<>();
    Consumer<JSONObject> lineVisitor = new Consumer<JSONObject>() {
      @Override
      public void accept(JSONObject jsonObject) {
        list.add(jsonObject.toString());
      }
    };
    runQuery(queryString, getPatchSets, getCurrentPatchSet, getFiles, getCommitMessage, false, lineVisitor);
    return list;
  }

  //CS IGNORE RedundantThrows FOR NEXT 24 LINES. REASON: Informative.
  //CS IGNORE JavadocMethod FOR NEXT 20 LINES. REASON: It is there.

  /**
   * Runs the query on the Gerrit server and lets the provided visitor handle each line in the result.
   *
   * @param queryString        the query. (e.g "limit=1")
   * @param getPatchSets       if all patch-sets of the projects found should be included in the result.
   *                           Meaning if --patch-sets should be appended to the command call.
   * @param getCurrentPatchSet if the current patch-set for the projects found should be included in the result.
   *                           Meaning if --current-patch-set should be appended to the command call.
   * @param getFiles           if changed files list should be included in the result.
   *                           Meaning if --files should be appended to the command call.
   * @param getCommitMessage   if full commit message should be included in the result.
   *                           Meaning if --commit-message should be appended to the command call.
   * @param getComments        if patchset comments should be included in the results.
   *                           Meaning if --comments should be appended to the command call.
   * @param lineVisitor the visitor to handle each line in the result.
   * @throws GerritQueryException if a visitor finds that Gerrit reported an error with the query.
   * @throws IOException          for some other IO problem.
   */
  private void runQuery(String queryString, boolean getPatchSets, boolean getCurrentPatchSet, boolean getFiles,
                            boolean getCommitMessage, boolean getComments, Consumer<JSONObject> lineVisitor)
      throws GerritQueryException, IOException {

    StringBuilder str = new StringBuilder(httpBaseUrl);
    str.append("/a/changes/?q=").append(queryString);

    // map the cl-arguments from: https://gerrit-review.googlesource.com/Documentation/cmd-query.html
    // to Rest-API-arguments:     https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html

    if (getPatchSets) {
      //ALL_REVISIONS: describe all revisions, not just current.
      str.append(GET_ALL_REVISIONS);
    }
    if (getCurrentPatchSet) {
      //"--current-patch-set"
      //CURRENT_REVISION: describe the current revision (patch set) of the change, including the commit SHA-1 and URLs
      //to fetch from.
      str.append(GET_CURRENT_REVISION);
    }
    if (getFiles && (getCurrentPatchSet || getPatchSets)) {
      //"--files"
      //CURRENT_FILES: list files modified by the commit and magic files, including basic line counts inserted/deleted
      //per file.
      // Only valid when the CURRENT_REVISION or ALL_REVISIONS option is selected.
      str.append(GET_CURRENT_FILES);
    }
    if (getCommitMessage && (getCurrentPatchSet || getPatchSets)) {
      //"--commit-message"
      //CURRENT_COMMITS: parse and output all header fields from the commit object, including message.
      // Only valid when the CURRENT_REVISION or ALL_REVISIONS option is selected.
      str.append(GET_CURRENT_COMMIT);
    }
    if (getComments) {
      //"--comments"
      //MESSAGES: include messages associated with the change.
      str.append(GET_MESSAGES);
    }

    logger.debug("sending: " + str);

    Response response = given().auth().preemptive()
                        .basic(credential.getUserPrincipal().getName(), credential.getPassword())
                        .given().when().request("GET", str.toString());

    logger.debug("Status received: " + response.getStatusLine());

    switch(response.statusCode()) {
      case STATUS_OK:
        logger.debug("Body received: " + response.body().asPrettyString());
        break;

      case STATUS_BAD_REQUEST:
        logger.error(httpBaseUrl + ": Bad request " + "(400)");
        throw new GerritQueryException(str + ": Bad request " + "(400)");

      case STATUS_BAD_CREDENTIALS:
        logger.error("Unable to authenticate to \"" + httpBaseUrl + "\"");
        throw new IOException("Error connecting to \"" + httpBaseUrl + "\" " + "(401)");

      case STATUS_NOT_FOUND:
        logger.error("\"" + httpBaseUrl + "\": Could not be found! (404)");
        throw new IOException("Error connecting to \"" + httpBaseUrl + "\" " + "(404)");

      default:
        logger.error("Error connecting to \"" + httpBaseUrl + "\"! (" + response.statusCode() + ")");
        throw new IOException("Error connecting to \"" + httpBaseUrl + "\" (" + response.statusCode() + ")");
    }

    String body = response.body().asString().split("\\r?\\n|\\r")[1]; //removing XSSI-Chars
    JSONArray jsonArray = JSONArray.fromObject(body);

    //jsonArray.forEach(v -> lineVisitor.accept((JSONObject) v));
    for (Object element : jsonArray) {
        lineVisitor.accept((JSONObject) element);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof GerritQueryHandlerHttp)) {
      return false;
    }
    GerritQueryHandlerHttp gerritQueryHandler = (GerritQueryHandlerHttp)o;
    return Objects.equals(httpBaseUrl, gerritQueryHandler.httpBaseUrl)
        && Objects.equals(proxy, gerritQueryHandler.proxy)
        && Objects.equals(credential, gerritQueryHandler.credential);
  }

  @Override
  public int hashCode() {
    return Objects.hash(httpBaseUrl, proxy, credential);
  }
}
