package com.sonymobile.tools.gerrit.gerritevents;

import com.sonymobile.tools.gerrit.gerritevents.rest.RestConnectionConfig;
import com.sonymobile.tools.gerrit.gerritevents.ssh.SshException;
import io.restassured.response.Response;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.restassured.RestAssured.given;



/**
 * This class helps you call gerrit query to search for patch-sets.
 *
 * @author Robert Sandell &lt;robert.sandell@sonyericsson.com&gt;
 */
public class GerritQueryHandlerHttp {

  /**
   * The interface used to store Credentials.
   */
  public interface Credential {
    /**
     * The user principal.
     * @return the password
     */
    Principal getUserPrincipal();

    /**
     * the password.
     * @return the password
     */
    String getPassword();
  }

  private final int statusOk = 200;
  private final int statusBadRequest = 400;
  private final int statusBadCredentials = 401;
  private final int statusNotFound = 404;

  /**
   * Logger instance.
   * Set protected to allow it  to be used in subclasses.
   */
  protected static final Logger logger = LoggerFactory.getLogger(GerritQueryHandlerHttp.class);
  /**
   * The base of the query HTTP command to send to Gerrit.
   */
  public static final String QUERY_COMMAND = "gerrit query";
  //https://gerrit-review.googlesource.com/Documentation/cmd-query.html
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

    runQuery(queryString, getPatchSets, getCurrentPatchSet, getFiles, getCommitMessage, getComments,
        new GerritQueryHandlerHttp.LineVisitor() {
          @Override
          public void visit(String line) {
            JSONObject json = (JSONObject)JSONSerializer.toJSON(line.trim());
            list.add(json);
          }
        });

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

    runQuery(queryString, getPatchSets, getCurrentPatchSet, getFiles, getCommitMessage, false,
        new GerritQueryHandlerHttp.LineVisitor() {
          @Override
          public void visit(String line) {
            list.add(line);
          }
        });
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
                            boolean getCommitMessage, boolean getComments, LineVisitor visitor)
      throws GerritQueryException, IOException {


    StringBuilder str = new StringBuilder(httpBaseUrl);

    str.append("/a/changes/?q=").append(queryString);

    // map the cl-arguments from: https://gerrit-review.googlesource.com/Documentation/cmd-query.html
    // to Rest-API-arguments:          https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html

    if (getPatchSets) {
      //ALL_REVISIONS: describe all revisions, not just current.
      str.append("&o=ALL_REVISIONS");
    }
    if (getCurrentPatchSet) {
      //.append(" --current-patch-set");
      //CURRENT_REVISION: describe the current revision (patch set) of the change, including the commit SHA-1 and URLs
      //to fetch from.
      str.append("&o=CURRENT_REVISION");
    }
    if (getFiles && (getCurrentPatchSet || getPatchSets)) {
      //str.append(" --files");
      //CURRENT_FILES: list files modified by the commit and magic files, including basic line counts inserted/deleted
      //per file.
      // Only valid when the CURRENT_REVISION or ALL_REVISIONS option is selected.
      str.append("&o=CURRENT_FILES");
    }
    if (getCommitMessage && (getCurrentPatchSet || getPatchSets)) {
      //str.append(" --commit-message");
      //CURRENT_COMMITS: parse and output all header fields from the commit object, including message.
      // Only valid when the CURRENT_REVISION or ALL_REVISIONS option is selected.
      str.append("&o=CURRENT_COMMIT");
    }
    if (getComments) {
      //str.append(" --comments");
      //MESSAGES: include messages associated with the change.
      str.append("&o=MESSAGES");
    }

    logger.debug("sending: " + str);
    //System.out.println("sending: " + str);

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

    //response.body().print();

    //removing XSSI-Chars
    String body = response.body().asString().split("\\r?\\n|\\r")[1];

    JSONArray jsonArray = JSONArray.fromObject(body);

    for (int i = 0; i < jsonArray.size(); i++) {
      visitor.visit(jsonArray.getString(i));
    }

      /*
            Response response = given().auth().basic(credentials.getUserPrincipal().toString(),
            credentials.getPassword()). //authentication: https://www.baeldung.com/rest-assured-authentication
                    //config(config).                                                                 //configuration
                            when().get(str.toString());                                     //url

            logger.debug("\n Status received => \n " + response.getStatusLine());
            logger.debug("\n Response => \n " + response.prettyPrint());



            // Specify the base URL to the RESTful web service
            RestAssured.baseURI = "https://demoqa.com/BookStore/v1/Books";
            // Get the RequestSpecification of the request to be sent to the server.
            RequestSpecification httpRequest = RestAssured.given();
            // specify the method type (GET) and the parameters if any.
            //In this case the request does not take any parameters
            Response response1 = httpRequest.request(Method.GET, "");
            // Print the status and message body of the response received from the server
            System.out.println("Status received => " + response.getStatusLine());
            System.out.println("Response=>" + response.prettyPrint());


            HttpGet httpGet = new HttpGet(frontEndUrl);
            httpGet.setHeader("", "");

            CloseableHttpClient httpClient = getConnection();

            HttpPost httpPost = new HttpPost(frontEndUrl);         //create the post to send
            httpPost.setEntity(new StringEntity(str.toString())); //add body to request

            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            String response2 = IOUtils.toString(httpResponse.getEntity().getContent(), "UTF-8");

            //check if error occurred
            if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                logger.error("Gerrit response: {}", httpResponse.getStatusLine().getReasonPhrase());
                logger.debug("ERROR Gerrit response: " + httpResponse.getStatusLine().getReasonPhrase());
            }
            else
            {
                //parse the response
                Scanner scanner = new Scanner(response2);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    logger.trace("Incoming line: {}", line);
                    visitor.visit(line);
                }
                logger.trace("Closing reader.");
                scanner.close();
            }

        } catch (Exception e) {
            logger.error("Failed to submit result to Gerrit", e);
        }
  */
  }


  /**
   * Internal visitor for handling a line of text.
   * Used by .
   */
  interface LineVisitor {
    /**
     * Visits a line of query result.
     *
     * @param line the line.
     */
    void visit(String line);
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
