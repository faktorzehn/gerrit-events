package com.sonymobile.tools.gerrit.gerritevents;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import net.sf.json.JSONObject;
import org.apache.http.auth.Credentials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * {@link GerritQueryHandlerHttpTest#url}, {@link GerritQueryHandlerHttpTest#port}
 *  {@link GerritQueryHandlerHttpTest#username} and {@link GerritQueryHandlerHttpTest#password}
 *  can be changed to test on a real gerrit server. (instead of on the localhost-mock)
 */
public class GerritQueryHandlerHttpTest {

    /*
    -schauen, ob die parameter richtig in die urls umgewandelt werden
    -schauen, ob die Antworten richtig empfangen werden, XSSI-Chars entfernt werden und als List<JSONObject> zurück-
        gegeben werden
    -beide setzen korrekte Credentials vorraus
    -example: https://www.swtestacademy.com/wiremock-junit-5-rest-assured/
    -auth: https://wiremock.org/docs/request-matching/

    -antwort von server anschauen
     */

    /**
     * Logger instance.
     * Set protected to allow it  to be used in subclasses.
     */
    protected static final Logger logger = LoggerFactory.getLogger(GerritQueryHandlerHttpTest.class);

    private final int port = 8020;

    private final String url = "http://localhost:" + port;

    private final int statusOk = 200;
    private final int statusBadRequest = 400;
    private final int statusBadCredentials = 401;
    private final int statusNotFound = 404;
    private final int statusRandomCode = 501;

    private final String username = "user123";

    private final String password = "password123";

    private Credentials credentials;

    private GerritQueryHandlerHttp gerritQueryHandlerHTTP;

    private WireMockServer wireMockServer;

    /**
     *initialise SUT
     */
    private void setupQueryHandlerHttp() {
        GerritQueryHandlerHttp.Credential credential = new GerritQueryHandlerHttp.Credential() {
            @Override
            public Principal getUserPrincipal() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return username;
                    }
                };
                //() -> ""; lambdas are tripping up CheckStyle
            }

            @Override
            public String getPassword() {
                return password;
            }
        };
        gerritQueryHandlerHTTP = new GerritQueryHandlerHttp(url, credential, null);
    }

    /**
     * Initialise the mock-REST-API, call {@link GerritQueryHandlerHttpTest#setupQueryHandlerHttp()}.
     */
    @BeforeEach
    public void setupMockServer() {
        wireMockServer = new WireMockServer(new WireMockConfiguration().port(port)
            .usingFilesUnderDirectory("src/test/resources/com/sonymobile/tools/gerrit/gerritevents/SampleResponses"));
        wireMockServer.start();
        setupQueryHandlerHttp();
    }

    /**
     * Shut down the mock-REST-API.
     */
    @AfterEach
    public void shutdownMockServer() {
        wireMockServer.stop();
    }

    /**
     * debug.
     */
    public void debug() {
        //debugStub();
        setupQueryHandlerHttp();
        /*
        given().auth().preemptive().basic(credentials.getUserPrincipal().getName(), credentials.getPassword())
            .when().get("https://gerrit.faktorzehn.de/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_REVI
            SION&o=CURRENT_FILES&o=CURRENT_COMMITS&o=MESSAGES").then()
            .assertThat().statusCode(200);
         */

/*
        try {
            Response response = gerritQueryHandlerHTTP.runQuery("limit:2", true, true, true, true, true);
            System.out.println();
            String s = response.body().asPrettyString();
            System.out.println(s);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GerritQueryException e) {
            e.printStackTrace();
        }
 */
    }

    /**
     * setup stub for {@link GerritQueryHandlerHttpTest#urlConversionTest()}, which can be found at url + query.
     * @param query a String added to url
     */
    public void setupStubForUrlConversion(String query) {
        ///a/changes/?q=limit%3A1
        wireMockServer.stubFor(get(urlEqualTo(query))
            .withBasicAuth(username, password)
            .willReturn(aResponse().withHeader("Content-Type", "text/plain")
                .withStatus(statusOk)
                .withBody(")]}'\n []")));
    }

    /**
     * setup stub for {@link GerritQueryHandlerHttpTest#responseParsingTest()}; return a JSON body.
     * @param fileName the name of the file (in ~/__files/..) to be returned
     */
    public void setupStubForResponseParsing(String fileName) {
        //using some random testUrl
        wireMockServer.stubFor(get(urlEqualTo("/a/changes/?q=x&o=ALL_REVISIONS&o=CURRENT_REVISION"))
            .withBasicAuth(username, password)
            .willReturn(aResponse().withHeader("Content-Type", "text/plain")
                .withStatus(statusOk)
                .withBodyFile(fileName)));
    }

    /**
     * setup stub for {@link GerritQueryHandlerHttpTest#invalidRequestsThrowsTest()}.
     * @param expectedCode the HTTP statuscode
     */
    public void setupStubForInvalidRequests(int expectedCode) {
        ///a/changes/?q=limit%3A1
        wireMockServer.stubFor(get(urlEqualTo("/a/changes/?q=x&o=ALL_REVISIONS&o=CURRENT_REVISION"))
            .withBasicAuth(username, password)
            .willReturn(aResponse().withHeader("Content-Type", "text/plain")
                .withStatus(expectedCode)
                .withBody(")]}'\n" + "[{\"Id\":\"value1\"}]")));
    }

    /**
     * see {@link GerritQueryHandlerHttpTest#urlConversionTest()}.
     * @throws Exception when something wrong
     */
    private void urlConversionTest1True() throws Exception {
        setupStubForUrlConversion("/a/changes/?q=test");
        gerritQueryHandlerHTTP.queryJava("test", false, false, false, false, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, false, false, false, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, false, false, false, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=CURRENT_REVISION");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, true, false, false, false);

        //illegal combination
        setupStubForUrlConversion("/a/changes/?q=limit%3A1");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, false, true, false, false);

        //illegal combination
        setupStubForUrlConversion("/a/changes/?q=limit%3A1");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, false, false, true, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, false, false, false, true);
    }

    /**
     * see {@link GerritQueryHandlerHttpTest#urlConversionTest()}.
     * @throws Exception when something wrong
     */
    private void urlConversionTest2True() throws Exception {
        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_REVISION");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, true, false, false, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_FILES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, false, true, false, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_COMMIT");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, false, false, true, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, false, false, false, true);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=CURRENT_REVISION&o=CURRENT_FILES");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, true, true, false, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=CURRENT_REVISION&o=CURRENT_COMMIT");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, true, false, true, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=CURRENT_REVISION&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, true, false, false, true);

        //illegal combination
        setupStubForUrlConversion("/a/changes/?q=limit%3A1");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, false, true, true, false);

        //illegal combination
        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, false, true, false, true);

        //illegal combination
        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, false, false, true, true);
    }

    /**
     * see {@link GerritQueryHandlerHttpTest#urlConversionTest()}.
     * @throws Exception when something wrong
     */
    private void urlConversionTest3True() throws Exception {
        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_REVISION&o=CURRENT_FILES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, true, true, false, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_REVISION&o=CURRENT_COMMIT");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, true, false, true, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_REVISION&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, true, false, false, true);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_FILES&o=CURRENT_COMMIT");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, false, true, true, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_FILES&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, false, true, false, true);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_COMMIT&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, false, false, true, true);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=CURRENT_REVISION&o=CURRENT_FILES&o=CURRENT_COMMIT");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, true, true, true, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=CURRENT_REVISION&o=CURRENT_FILES&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, true, true, false, true);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=CURRENT_REVISION&o=CURRENT_COMMIT&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, true, false, true, true);

        //illegal combination
        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, false, true, true, true);
    }

    /**
     * see {@link GerritQueryHandlerHttpTest#urlConversionTest()}.
     * @throws Exception when something wrong
     */
    private void urlConversionTest4True() throws Exception {
        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_REVISION&o=CURRENT_FILES"
            + "&o=CURRENT_COMMIT");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, true, true, true, false);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_REVISION&o=CURRENT_FILES"
            + "&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, true, true, false, true);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_REVISION&o=CURRENT_COMMIT"
            + "&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, true, false, true, true);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_FILES&o=CURRENT_COMMIT&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, false, true, true, true);

        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=CURRENT_REVISION&o=CURRENT_FILES&o=CURRENT_COMMIT"
            + "&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", false, true, true, true, true);
    }

    /**
     * see {@link GerritQueryHandlerHttpTest#urlConversionTest()}.
     * @throws Exception when something wrong
     */
    private void urlConversionTest5True() throws Exception {
        setupStubForUrlConversion("/a/changes/?q=limit%3A1&o=ALL_REVISIONS&o=CURRENT_REVISION&o=CURRENT_FILES"
            + "&o=CURRENT_COMMIT&o=MESSAGES");
        gerritQueryHandlerHTTP.queryJava("limit:1", true, true, true, true, true);
    }
    //https://www.baeldung.com/parameterized-tests-junit-5

    /**
     * check, if runQuery() correctly transforms its options and adds them to the request.
     * @throws Exception if the server rejects the query (or something wrong)
     */
    @Test
    public void urlConversionTest() throws Exception {
        urlConversionTest1True();
        urlConversionTest2True();
        urlConversionTest3True();
        urlConversionTest4True();
        urlConversionTest5True();
    }

    /**
     * Checking, if the server-response is parsed correctly. (also removing XSSI-Chars)
     * @throws Exception when something wrong
     */
    @Test
    public void responseParsingTest() throws Exception {

        //match short response
        setupStubForResponseParsing("restApiTest_shortResponse");
        List<JSONObject> response = gerritQueryHandlerHTTP.queryJava("x");
        assertEquals("value1", response.get(0).get("Id"));
        assertEquals("value2", response.get(1).get("Id"));

        //match long response
        setupStubForResponseParsing("restApiTest_longResponse");
        response = gerritQueryHandlerHTTP.queryJava("x");
        assertEquals("test0%2Ftest0.test~master~Id145cfc4s2d6fek18c3539gbi8me1d3e37501568", response.get(0).get("id"));
        assertEquals("{\"309\":{\"account\":{\"_account_id\":309},\"last_update\":\"2022-03-24 "
                    + "09:59:36.000000000\",\"reason\":\"Username0 replied on the change\"}}",
                    response.get(0).get("attention_set").toString());

        //now queryJson()
        setupStubForResponseParsing("restApiTest_shortResponse");
        List<String> response1 = gerritQueryHandlerHTTP.queryJson("x");
        assertEquals("{\"Id\":\"value1\"}", response1.get(0));
        assertEquals("{\"Id\":\"value2\"}", response1.get(1));


    }

    /**
     * test that invalid requests are throwing exceptions.
     */
    @Test
    public void invalidRequestsThrowsTest() {

        //Statuscode 200
        setupStubForInvalidRequests(statusOk);
        try {
            gerritQueryHandlerHTTP.queryJava("x");
        } catch (Exception e) {
            fail("An exception was thrown although Gerrit responded with statuscode 200 (" + e.getMessage() + ")");
        }

        //Statuscode 400
        setupStubForInvalidRequests(statusBadRequest);
        //assertThrows(IOException.class, () -> gerritQueryHandlerHTTP.queryJava("x"));
        try {
            gerritQueryHandlerHTTP.queryJava("x");
            fail("GerritQueryException should have been thrown");
        } catch (Exception e) {
            if (!(e instanceof GerritQueryException)) {
                fail("wrong exception was thrown (" + e.getMessage() + ")");
            }
        }

        //Statuscode 401
        setupStubForInvalidRequests(statusBadCredentials);
        //assertThrows(IOException.class, () -> gerritQueryHandlerHTTP.queryJava("x"));
        try {
            gerritQueryHandlerHTTP.queryJava("x");
            fail("IOException should have been thrown");
        } catch (Exception e) {
            if (!(e instanceof IOException)) {
                fail("wrong exception was thrown (" + e.getMessage() + ")");
            }
        }

        //Statuscode 404
        setupStubForInvalidRequests(statusNotFound);
        //assertThrows(IOException.class, () -> gerritQueryHandlerHTTP.queryJava("x"));
        try {
            gerritQueryHandlerHTTP.queryJava("x");
            fail("IOException should have been thrown");
        } catch (Exception e) {
            if (!(e instanceof IOException)) {
                fail("wrong exception was thrown (" + e.getMessage() + ")");
            }
        }

        //other Statuscode
        setupStubForInvalidRequests(statusRandomCode);
        //assertThrows(IOException.class, () -> gerritQueryHandlerHTTP.queryJava("x"));
        try {
            gerritQueryHandlerHTTP.queryJava("x");
            fail("IOException should have been thrown");
        } catch (Exception e) {
            if (!(e instanceof IOException)) {
                fail("wrong exception was thrown (" + e.getMessage() + ")");
            }
        }
    }
}
