//* Licensed Materials - Property of IBM                              *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*/**/****************************************************************

package eu.abc4trust.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.abc4trust.returnTypes.UiIssuanceArguments;
import eu.abc4trust.returnTypes.UiIssuanceReturn;
import eu.abc4trust.returnTypes.ui.CredentialInUi;
import eu.abc4trust.returnTypes.ui.PseudonymInUi;
import eu.abc4trust.returnTypes.ui.TokenCandidate;
import eu.abc4trust.xml.PseudonymMetadata;

public class MyUiIssuanceReturn {

  public final UiIssuanceReturn ret;
  public final List<URI> chosenCredentials;
  public final List<URI> chosenPseudonyms;
  public final List<URI> chosenInspectors;
  public final Map<URI, PseudonymMetadata> metadataToChange;
  public final MyCandidateToken token;
  
  public MyUiIssuanceReturn(UiIssuanceArguments arg, UiIssuanceReturn ret, List<MyCandidateToken> candidateTokens) {
    this.ret = ret;

    TokenCandidate tc = arg.tokenCandidates.get(ret.chosenIssuanceToken);

    this.chosenCredentials = new ArrayList<URI>();
    for (CredentialInUi o : tc.credentials) {
      this.chosenCredentials.add(o.desc.getCredentialUID());
    }

    this.chosenPseudonyms = new ArrayList<URI>();
    if (tc.pseudonymCandidates.size() > 0) {
      for (PseudonymInUi o : tc.pseudonymCandidates.get(ret.chosenPseudonymList).pseudonyms) {
        this.chosenPseudonyms.add(o.pseudonym.getPseudonymUID());
      }
    }

    this.chosenInspectors = new ArrayList<URI>();
    for (String ins : ret.chosenInspectors) {
      this.chosenInspectors.add(URI.create(ins));
    }

    this.metadataToChange = new HashMap<URI, PseudonymMetadata>();
    for (String key : ret.metadataToChange.keySet()) {
      this.metadataToChange.put(URI.create(key), ret.metadataToChange.get(key));
    }
    
    this.token = candidateTokens.get(ret.chosenIssuanceToken);
  }
}
