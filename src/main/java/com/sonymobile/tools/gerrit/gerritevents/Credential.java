package com.sonymobile.tools.gerrit.gerritevents;

import java.security.Principal;

/**
 * The interface which GerritQueryHandlerHttp uses for authentication
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
