//* Licensed Materials - Property of IBM, Miracle A/S, and            *
//* Alexandra Instituttet A/S                                         *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2012. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2012. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*/**/****************************************************************

package eu.abc4trust.cryptoEngine.issuer;

import java.net.URI;

import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.returnTypes.IssuerParametersAndSecretKey;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.IssuanceMessage;
import eu.abc4trust.xml.IssuanceMessageAndBoolean;
import eu.abc4trust.xml.IssuancePolicy;
import eu.abc4trust.xml.SystemParameters;

public interface CryptoEngineReIssuer {

	/**
	 * This method 
	 * @param issuancePolicy
	 * @param context
	 * @return
	 * @throws CryptoEngineException
	 */
	IssuanceMessageAndBoolean initReIssuanceProtocol(IssuancePolicy issuancePolicy, 
			URI context) throws CryptoEngineException;

    /**
     * On input an incoming issuance message m, this method first extracts the
     * context attribute and obtains the cryptographic state information that is
     * stored under the same context value. It then invokes the
     * mechanism-specific cryptographic routines for one step in an interactive
     * issuance protocol and returns an outgoing issuance message. The method
     * eventually also stores new cryptographic state information associated to
     * the context attribute, and attaches the context attribute to the outgoing
     * message. The returned boolean indicates whether this is the last flow of
     * the issuance protocol. If so, the method deletes all temporary state
     * information. If the credential to be issued is subject to Issuer-driven
     * revocation, then, depending on the revocation mechanism, the CryptoEngine
     * may have to interact with the Revocation Authority. If so, then it
     * prepares a mechanism-specific Revocation Message m and calling
     * RevocationProxy.processRevocationMessage(m, revpars).
     * 
     * This method also verifies that the cryptographic evidence contained
     * within the issuance message (for example an issuance token) are correct.
     * For issuance tokens this method will particularly check whether the
     * requirements concerning the carried-over and (jointly)-random attributes
     * are met. In order to verify the token, this method may call upon the
     * KeyManager to obtain Issuer parameters, Inspector public keys, Revocation
     * Authority parameters, and the current revocation information. If the
     * evidence is deemed invalid, this method will throw a RuntimeException.
     * 
     * @param m
     * @return
     * @throws CryptoEngineException
     */
    public IssuanceMessageAndBoolean reIssuanceProtocolStep(IssuanceMessage m)
            throws CryptoEngineException;

    /**
     * This method generates a fresh set of system parameters for the given key length and
     * cryptographic mechanism. Issuers can generate their own system parameters, but can also reuse
     * system parameters generated by a different entity.
     * 
     * @param keyLength
     * @param cryptographicMechanism
     * @return
     */
    public SystemParameters setupSystemParameters(int keyLength, URI cryptographicMechanism);

    /**
     * This method generates and returns a fresh issuance key and the corresponding Issuer parameters.
     * The input to this method specify the credential specification credspec of the credentials that
     * will be issued with these parameters, the system parameters syspars, the unique identifier uid
     * of the generated parameters, the hash algorithm identifier hash, and, optionally, the
     * parameters identifier for any Issuer-driven Revocation Authority.
     * 
     * @param credspec
     * @param syspars
     * @param uid
     * @param hash
     * @param revParsUid
     * @return
     */
    public IssuerParametersAndSecretKey setupIssuerParameters(CredentialSpecification credspec,
            SystemParameters syspars, URI uid, URI hash, URI revParsUid);
	
}
