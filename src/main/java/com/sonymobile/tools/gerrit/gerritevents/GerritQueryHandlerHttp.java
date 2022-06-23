package com.sonymobile.tools.gerrit.gerritevents;

import com.sonymobile.tools.gerrit.gerritevents.rest.RestConnectionConfig;
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
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

/**
 * This class helps you call gerrit query to search for patch-sets. instead of using SSH, it calls Gerrit's Rest api
 *
 * @author Manuel Mühlberger &lt;Manuel.Muehlberger@faktorzehn.de&gt;
 */
public class GerritQueryHandlerHttp {

  static final int statusOk = 200;
  static final int statusBadRequest = 400;
  static final int statusBadCredentials = 401;
  static final int statusNotFound = 404;

  /**
   * Logger instance.
   * Set protected to allow it  to be used in subclasses.
   */
  protected static final Logger logger = LoggerFactory.getLogger(GerritQueryHandlerHttp.class);

  private final String getAllRevisions = "&o=ALL_REVISIONS";
  private final String getCurrentRevision = "&o=CURRENT_REVISION";
  private final String getCurrentFiles = "&o=CURRENT_FILES";
  private final String getCurrentCommit = "&o=CURRENT_COMMIT";
  private final String getMessages = "&o=MESSAGES";

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

  /**
   * Creates a GerritQueryHandlerHTTP with the specified config.
   *
   * @param config the RestConnectionConfig, which contains the url, credentials and proxy
   */
  public GerritQueryHandlerHttp(RestConnectionConfig config) {
    this(config.getGerritFrontEndUrl(),
        (Credential)config.getHttpCredentials(),
        config.getGerritProxy());
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

    Consumer<JSONObject> lineVisitor = list::add;

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

    Consumer<JSONObject> lineVisitor = (JSONObject o) -> list.add(o.toString());

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
   *                           //@param visitor the visitor to handle each line in the result.
   * @throws GerritQueryException if a visitor finds that Gerrit reported an error with the query.
   * @throws IOException          for some other IO problem.
   */
  private void runQuery(String queryString, boolean getPatchSets, boolean getCurrentPatchSet, boolean getFiles,
                            boolean getCommitMessage, boolean getComments, Consumer<JSONObject> lineVisitor)
      throws GerritQueryException, IOException {


    StringBuilder str = new StringBuilder(httpBaseUrl);

    str.append("/a/changes/?q=").append(queryString);

    // map the cl-arguments from: https://gerrit-review.googlesource.com/Documentation/cmd-query.html
    // to Rest-API-arguments:          https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html

    if (getPatchSets) {
      //ALL_REVISIONS: describe all revisions, not just current.
      str.append(getAllRevisions);
    }
    if (getCurrentPatchSet) {
      //.append(" --current-patch-set");
      //CURRENT_REVISION: describe the current revision (patch set) of the change, including the commit SHA-1 and URLs
      //to fetch from.
      str.append(getCurrentRevision);
    }
    if (getFiles && (getCurrentPatchSet || getPatchSets)) {
      //str.append(" --files");
      //CURRENT_FILES: list files modified by the commit and magic files, including basic line counts inserted/deleted
      //per file.
      // Only valid when the CURRENT_REVISION or ALL_REVISIONS option is selected.
      str.append(getCurrentFiles);
    }
    if (getCommitMessage && (getCurrentPatchSet || getPatchSets)) {
      //str.append(" --commit-message");
      //CURRENT_COMMITS: parse and output all header fields from the commit object, including message.
      // Only valid when the CURRENT_REVISION or ALL_REVISIONS option is selected.
      str.append(getCurrentCommit);
    }
    if (getComments) {
      //str.append(" --comments");
      //MESSAGES: include messages associated with the change.
      str.append(getMessages);
    }

    logger.debug("sending: " + str);

    Response response = given().auth().preemptive()
                        .basic(credential.getUserPrincipal().getName(), credential.getPassword())
                        .given().when().request("GET", str.toString());

    logger.debug("Status received: " + response.getStatusLine());

    switch(response.statusCode()) {
      case statusOk:
        logger.debug("Body received: " + response.body().asPrettyString());
        break;

      case statusBadRequest:
        logger.error(httpBaseUrl + ": Bad request " + "(400)");
        throw new GerritQueryException(str + ": Bad request " + "(400)");

      case statusBadCredentials:
        logger.error("Unable to authenticate to \"" + httpBaseUrl + "\"");
        throw new IOException("Error connecting to \"" + httpBaseUrl + "\": " + "(401)");

      case statusNotFound:
        logger.error("\"" + httpBaseUrl + "\": Could not be found! (404)");
        throw new IOException("Error connecting to \"" + httpBaseUrl + "\": " + "(404)");

      default:
        logger.error("Error connecting to \"" + httpBaseUrl + "\"! (" + response.statusCode() + ")");
        throw new IOException("Error connecting to \"" + httpBaseUrl + "\"! (" + response.statusCode() + ")");
    }

    //removing XSSI-Chars
    String body = response.body().asString().split("\\r?\\n|\\r")[1];

    JSONArray jsonArray = JSONArray.fromObject(body);

    for (int i = 0; i < jsonArray.size(); i++) {
      lineVisitor.accept(jsonArray.getJSONObject(i));
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
