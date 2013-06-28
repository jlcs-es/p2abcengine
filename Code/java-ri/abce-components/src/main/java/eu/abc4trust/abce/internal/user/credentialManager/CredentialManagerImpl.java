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

package eu.abc4trust.abce.internal.user.credentialManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.uuid.Generators;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import eu.abc4trust.abce.internal.user.policyCredentialMatcher.PolicyCredentialMatcherImpl;
import eu.abc4trust.cryptoEngine.CredentialWasRevokedException;
import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.cryptoEngine.user.CredentialSerializer;
import eu.abc4trust.cryptoEngine.user.CredentialSerializerSmartcard;
import eu.abc4trust.cryptoEngine.user.CryptoEngineUser;
import eu.abc4trust.cryptoEngine.user.PseudonymSerializer;
import eu.abc4trust.cryptoEngine.user.PseudonymSerializerObjectGzip;
import eu.abc4trust.keyManager.KeyManager;
import eu.abc4trust.smartcard.BasicSmartcard;
import eu.abc4trust.smartcard.CardStorage;
import eu.abc4trust.smartcard.SecretBasedSmartcard;
import eu.abc4trust.smartcard.Smartcard;
import eu.abc4trust.smartcard.SmartcardBlob;
import eu.abc4trust.smartcard.SmartcardStatusCode;
import eu.abc4trust.util.StorageUtil;
import eu.abc4trust.xml.Credential;
import eu.abc4trust.xml.CredentialDescription;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.Pseudonym;
import eu.abc4trust.xml.PseudonymMetadata;
import eu.abc4trust.xml.PseudonymWithMetadata;
import eu.abc4trust.xml.Secret;
import eu.abc4trust.xml.SecretDescription;

public class CredentialManagerImpl implements CredentialManager {

    public static final String PSEUDONYM_PREFIX = "pseudonym_";
	private final KeyManager keyManager;
    private final CryptoEngineUser cryptoEngineUser;
    private final CredentialStorage storage;
    private final SecretStorage sstorage;
    private final CardStorage cardStorage;
    private final Random prng;
    private final ImageCache imageCache;
    private final CredentialSerializer credentialSerializer;
    private final PseudonymSerializer pseudonymSerializer;

    // TODO : hgk - FIND SOLUTION : for Soderhamn - save pseudonyms in map - to be able to run through presentation when pseudonyms arenot stored on card
    private final Map<String,PseudonymWithMetadata> soderhamnTmpPseudonymMap = new HashMap<String, PseudonymWithMetadata>();
    
    @Inject
    public CredentialManagerImpl(CredentialStorage credentialStore,
            SecretStorage secretStore, KeyManager keyManager,
            ImageCache imageCache, CryptoEngineUser cryptoEngineUser,
            @Named("RandomNumberGenerator") Random prng, CardStorage cardStorage,
            CredentialSerializerSmartcard serializer) {
        // WARNING: Due to circular dependencies you MUST NOT dereference cryptoEngineUser
        // in this constructor.
        // (Guice does some magic to support circular dependencies).

        this.keyManager = keyManager;
        this.cryptoEngineUser = cryptoEngineUser;
        this.storage = credentialStore;
        this.cardStorage = cardStorage;
        this.sstorage = secretStore;
        this.prng = prng;
        this.imageCache = imageCache;
        // CredentialSerializerDelegator wraps injected optimized serializer for Idemix + a standard serializer for UProve
        //this.credentialSerializer = new CredentialSerializerDelegator(serializer, new CredentialSerializerObjectGzip());
        //The optimized serializer should also work for UProve now.
        this.credentialSerializer = serializer;
        this.pseudonymSerializer = new PseudonymSerializerObjectGzip(cardStorage);        
    }
    
    private URI getSmartcardUri(){
    	Map<URI, BasicSmartcard> scs = this.cardStorage.getSmartcards();
    	for(URI uri : scs.keySet()){
    		if(!(scs.get(uri) instanceof SecretBasedSmartcard)){    			
    			return uri;
    		}
    	}
    	return null;
    }
    
    private URI escapeUri(URI uri){
    	if(uri.toString().contains(":") && !uri.toString().contains("_")){
        	uri = URI.create(uri.toString().replaceAll(":", "_")); //change all ':' to '_'
        }
    	return uri;
    }

    @Override
    public void attachMetadataToPseudonym(Pseudonym pseudonym,
            PseudonymMetadata md)
                    throws CredentialManagerException {
        try {
        	PseudonymWithMetadata pwm = null;
        	try{
        		pwm = this.getPseudonymWithMetadata(pseudonym);
        	}catch(Exception e){
        		//pwm not present, creating new.
        	}
            if (pwm != null) {
                URI pseudonymUri = pseudonym.getPseudonymUID();
                URI SCuri = this.getSmartcardUri();
                if(SCuri == null){
                	this.storage.deletePseudonymWithMetadata(pseudonymUri);
                }else{
                	Smartcard sc = (Smartcard)this.cardStorage.getSmartcard(SCuri);
                	sc.deletePseudonym(this.cardStorage.getPin(SCuri), pseudonymUri);
                }
            } else {
                pwm = new PseudonymWithMetadata();
                pwm.setPseudonym(pseudonym);
            }
            pwm.setPseudonymMetadata(md);
            this.storePseudonym(pwm);
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
    }

    @Override
    public Credential getCredential(URI creduid)
            throws CredentialManagerException {
        if (creduid == null) {
            throw new CredentialManagerException("Credential UID is null");
        }
        ObjectInputStream objectInput = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
        	//System.out.println("CredentialManagerImpl - fetching credential. Looking first in SC.");
        	for(URI uri : this.cardStorage.getSmartcards().keySet()){
        		BasicSmartcard sc = this.cardStorage.getSmartcards().get(uri);
        		if(sc instanceof Smartcard){
        			//System.out.println("CredentialManagerImpl - fetching credential. got a SC. trying to fetch the cred: " + creduid);
        			Credential cred = sc.getCredential(this.cardStorage.getPin(uri), creduid, credentialSerializer);        			        			
        			if(cred != null){
        				//We have to supply certain things here since they are lost in compression
        				CredentialDescription descr = cred.getCredentialDescription();
        				descr.setCredentialUID(creduid);
        				descr.setSecretReference(uri);        				
        				return cred;
        			}
        			System.err.println("Fetching credential failed. It was null.. ");
        		}
        	}
            byte[] tokenBytes = this.storage.getCredential(creduid);
            if (tokenBytes == null) {
                throw new CredentialNotInStorageException(
                        "Credential with UID: \"" + creduid
                        + "\" is not in storage");
            }
            byteArrayInputStream = new ByteArrayInputStream(
                    tokenBytes);
            objectInput = new ObjectInputStream(byteArrayInputStream);
            Credential cred = (Credential) objectInput.readObject();
            return cred;
        } catch (CredentialNotInStorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        } finally {
            // Close the streams.
            StorageUtil.closeIgnoringException(objectInput);
            StorageUtil.closeIgnoringException(byteArrayInputStream);
        }
    }

    @Override
    public List<CredentialDescription> getCredentialDescription(
            List<URI> issuers, List<URI> credspecs)
                    throws CredentialManagerException {
        List<CredentialDescription> ls = new LinkedList<CredentialDescription>();
        try {
        	List<URI> credUris = this.listCredentials();
            for (URI credUri : credUris) {
                Credential cred = this.getCredential(credUri);
                CredentialDescription credentialDescription = cred
                        .getCredentialDescription();
                if (issuers
                        .contains(credentialDescription.getIssuerParametersUID())) {
                    if (credspecs.contains(credentialDescription
                            .getCredentialSpecificationUID())) {
                    	System.out.println("ADDED A CRED DESCRIPTION: " + credentialDescription.getCredentialUID());
                        ls.add(credentialDescription);
                    }
                }
            }
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
        return ls;
    }
    

    @Override
    public CredentialDescription getCredentialDescription(URI creduid)
            throws CredentialManagerException {
        try {
            Credential cred = this.getCredential(creduid);
            CredentialDescription credDesc = cred.getCredentialDescription();
            return credDesc;
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
    }

    private PseudonymWithMetadata parseBytesAsPseudonymWithMetaData(byte[] bytes)
            throws CredentialManagerException {
        ByteArrayInputStream byteArrayInputStream = null;
        ObjectInputStream objectInput = null;
        try {
            PseudonymWithMetadata pwm = null;
            if (bytes != null) {
                byteArrayInputStream = new ByteArrayInputStream(bytes);
                objectInput = new ObjectInputStream(byteArrayInputStream);
                pwm = (PseudonymWithMetadata) objectInput.readObject();
            }
            return pwm;
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        } finally {
            // Close the streams.
            StorageUtil.closeIgnoringException(objectInput);
            StorageUtil.closeIgnoringException(byteArrayInputStream);
        }
    }

    @Override
    public boolean hasBeenRevoked(URI creduid, URI revparsuid,
            List<URI> revokedatts) throws CredentialManagerException {
        Credential cred = this.getCredential(creduid);
        try {
            Credential updatedCred = this.cryptoEngineUser
                    .updateNonRevocationEvidence(cred, revparsuid, revokedatts);
            this.storeCredential(updatedCred);
            return false;
        } catch (CredentialWasRevokedException ex) {
          cred.getCredentialDescription().setRevokedByIssuer(true);
          this.storeCredential(cred);
          return true;
        } catch (CryptoEngineException ex) {
          throw new CredentialManagerException(ex);
        }
    }

    @Override
    public boolean hasBeenRevoked(URI creduid, URI revparsuid,
            List<URI> revokedatts, URI revinfouid)
                    throws CredentialManagerException {
        Credential cred = this.getCredential(creduid);
        try {
            Credential updatedCred = this.cryptoEngineUser
                    .updateNonRevocationEvidence(cred, revparsuid, revokedatts,
                            revinfouid);
            this.storeCredential(updatedCred);
            return false;
        } catch (CredentialWasRevokedException ex) {
            cred.getCredentialDescription().setRevokedByIssuer(true);
            this.storeCredential(cred);
            return true;
        } catch (CryptoEngineException ex) {
            throw new CredentialManagerException(ex);
        }
    }

    @Override
    public List<URI> listCredentials() throws CredentialManagerException {
    	List<URI> ls = new ArrayList<URI>();
    	try {
    		if(this.cardStorage.getSmartcards().size() != 0){
	        	for(URI uri: this.cardStorage.getSmartcards().keySet()){
	        		BasicSmartcard bsc = this.cardStorage.getSmartcard(uri);
	        		if(bsc instanceof SecretBasedSmartcard) break;
	        		Smartcard sc = (Smartcard)bsc;
	        		int pin = this.cardStorage.getPin(uri);
	        		Set<URI> setList = sc.listCredentialsUris(pin);
	        		ls.addAll(setList);
	        		return ls;
	        	}
    		}
    		return this.storage.listCredentials();
    	}catch(Exception ex){
    		throw new CredentialManagerException(ex);
    	}    	
    }

    @Override
    public URI storeCredential(Credential cred)
            throws CredentialManagerException {

        CredentialDescription credentialDescription = cred
                .getCredentialDescription();
        URI credUid = credentialDescription.getCredentialUID();

        if((credUid == null) || credUid.equals(URI.create(""))) {
            UUID uuid = Generators.randomBasedGenerator(this.prng).generate();
            credUid = URI.create(uuid.toString());
            credentialDescription.setCredentialUID(credUid);
        }

        this.storeImageAndUpdateCredentialDescription(credentialDescription);

        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutput = null;
        try {            
            URI cardUid = cred.getCredentialDescription().getSecretReference();
            BasicSmartcard bsc = this.cardStorage.getSmartcards().get(cardUid);
            if(cardUid != null &&  bsc != null && !(bsc instanceof SecretBasedSmartcard)){            	
              if(!cred.getCredentialDescription().isRevokedByIssuer()) {
                Smartcard sc = (Smartcard)this.cardStorage.getSmartcards().get(cardUid);            	            	
        		SmartcardStatusCode status = sc.storeCredential(this.cardStorage.getPin(cardUid), credUid, cred, credentialSerializer);
        		if(status != SmartcardStatusCode.OK){
        			throw new CredentialManagerException("Could not store credential. Reason: " + status);
        		}
        		return credUid;
              } else {
            	  throw new CredentialManagerException("Should never get here - trying to store a revoked credential");
              }
            }else{
            	byteArrayOutputStream = new ByteArrayOutputStream();
                objectOutput = new ObjectOutputStream(
                        byteArrayOutputStream);
                objectOutput.writeObject(cred);
                byte[] credBytes = byteArrayOutputStream.toByteArray();
            	this.storage.addCredential(credUid, credBytes);
            }
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        } finally {
            // Close the streams.
            StorageUtil.closeIgnoringException(objectOutput);
            StorageUtil.closeIgnoringException(byteArrayOutputStream);
        }

        return credUid;
    }
    
    @Override
    public void updateCredential(Credential cred) throws CredentialManagerException{
    	URI credUid = cred.getCredentialDescription().getCredentialUID();
    	try {
    		URI cardUid = cred.getCredentialDescription().getSecretReference();
    		BasicSmartcard bsc = this.cardStorage.getSmartcards().get(cardUid);
    		if(cardUid != null &&  bsc != null && !(bsc instanceof SecretBasedSmartcard)){
    			Smartcard sc = (Smartcard)this.cardStorage.getSmartcards().get(cardUid);
    			if(cred.getCredentialDescription().isRevokedByIssuer()){
    				this.deleteCredential(credUid);
    			}else{
    				sc.removeCredentialUri(this.cardStorage.getPin(cardUid), credUid);
    				this.storeCredential(cred);
    			}
    		}else{
    			this.deleteCredential(credUid);
    			this.storeCredential(cred);
    		}
    	}
    	catch(Exception e){
    		throw new CredentialManagerException(e);
    	}
    }

    private void storeImageAndUpdateCredentialDescription(
            CredentialDescription credentialDescription)
                    throws CredentialManagerException {
        try {
            URI imageRef = credentialDescription.getImageReference();
            URL url = this.imageCache.getDefaultImage();
            if (imageRef != null) {
                url = this.imageCache.storeImage(credentialDescription
                        .getImageReference());
            }
            try {
                String urlString = "" + url;
                if(urlString.contains(" ")) {
                    URL fixed = new URL(("" + url).replaceAll(" ", "%20"));
                    url = fixed;
                }
            } catch(Exception e) {
                System.err.println("storeImageAndUpdateCredentialDescription - fix failed : " + url + " : " + e);
            }
            System.out.println("storeImageAndUpdateCredentialDescription : " + url);
            credentialDescription.setImageReference(url.toURI());
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
    }

    @Override
    public void storePseudonym(PseudonymWithMetadata pwm)
            throws CredentialManagerException {

        Pseudonym pseudonym = pwm.getPseudonym();        

        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutput = null;
        try {
            URI pseudonymUri = pseudonym.getPseudonymUID();
            URI scUri = this.getSmartcardUri();
            System.out.println("\n\n scUri: " + scUri+" \n");
            System.out.println("Store pseudonym under uri: " + pseudonymUri.toString()+"\n");
            if(scUri == null){
                byteArrayOutputStream = new ByteArrayOutputStream();
                objectOutput = new ObjectOutputStream(
                        byteArrayOutputStream);
                objectOutput.writeObject(pwm);
                byte[] pwmBytes = byteArrayOutputStream.toByteArray();

            	this.storage.addPseudonymWithMetadata(pseudonymUri, pwmBytes);
            }else{
                if(pseudonym.isExclusive()){
                    // TODO: hgk : find solution to specify if pseudonyms needs to be stored.
                    // System.out.println("For Soederhamn pilot, we do not store scope-exclusive pseudonyms. Returning without storing.");
                    String key = scUri + "::" + pseudonymUri;
                    System.out.println("For Soederhamn pilot, we do not store scope-exclusive pseudonyms on Smartcard. Store in Memory Map with key : " + key + " : " + pwm.getPseudonym().getScope());
                    soderhamnTmpPseudonymMap.put(key, pwm);
                    return;
            	}
            	Smartcard sc = (Smartcard)this.cardStorage.getSmartcard(scUri);
            	pseudonymUri = this.escapeUri(pseudonymUri);
            	pseudonymUri = URI.create(PSEUDONYM_PREFIX+pseudonymUri.toString());
            	SmartcardStatusCode code = sc.storePseudonym(this.cardStorage.getPin(scUri), pseudonymUri, pwm, pseudonymSerializer);
            	System.out.println("Result of storing pseudonym on card: " + code);
            }
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        } finally {
            // Close the streams.
            StorageUtil.closeIgnoringException(objectOutput);
            StorageUtil.closeIgnoringException(byteArrayOutputStream);
        }
    }

    @Override
    public void updateNonRevocationEvidence()
            throws CredentialManagerException,
            IssuerParametersNotInKeystoreException {
        try {
            for (URI credUid : this.listCredentials()) {
                Credential cred = this.getCredential(credUid);
                CredentialDescription credDesc = this
                        .getCredentialDescription(credUid);
                List<URI> revokedatts = new LinkedList<URI>();
                revokedatts
                .add(new URI(
                        "http://abc4trust.eu/wp2/abcschemav1.0/revocationhandle"));

                URI issuerParamUid = credDesc.getIssuerParametersUID();
                IssuerParameters issuerParameters = this.keyManager
                        .getIssuerParameters(issuerParamUid);
                if (issuerParameters == null) {
                    throw new IssuerParametersNotInKeystoreException(
                            "Issuer parameters are not available in the Keystore.");
                }
                URI revocationAuthorityParameters = issuerParameters
                        .getRevocationParametersUID();

                cred = this.cryptoEngineUser.updateNonRevocationEvidence(cred,
                        revocationAuthorityParameters, revokedatts);

                this.updateCredential(cred);
            }
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
    }

    @Override
    public boolean deleteCredential(URI creduid)
            throws CredentialManagerException {
        try {
            Credential cred = this.getCredential(creduid);
            if (cred != null) {
            	URI cardUid = cred.getCredentialDescription().getSecretReference();
            	BasicSmartcard sc = this.cardStorage.getSmartcards().get(cardUid);
                if(cardUid != null && sc != null && !(sc instanceof SecretBasedSmartcard)){
                	sc.deleteCredential(this.cardStorage.getPin(cardUid), creduid);
                }else{
                	this.storage.deleteCredential(creduid);
                }
                return true;
            }
            return false;
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
    }

    @Override
    public List<PseudonymWithMetadata> listPseudonyms(String scope, boolean onlyExclusive)
            throws CredentialManagerException {
        try {
        	URI scUri = this.getSmartcardUri();
        	if(scUri == null){        	
	            List<byte[]> listPseudonyms = this.storage.listPseudonyms();
	            List<PseudonymWithMetadata> ls = new ArrayList<PseudonymWithMetadata>(
	                    listPseudonyms.size());
	            for (byte[] bytes : listPseudonyms) {
	                PseudonymWithMetadata pwm = this
	                        .parseBytesAsPseudonymWithMetaData(bytes);
	                Pseudonym pseudonym = pwm.getPseudonym();
	                if (pseudonym.getScope().equals(scope)
	                        && !(onlyExclusive && !pseudonym.isExclusive())) {
	                    ls.add(pwm);
	                }
	            }
	            return ls;
        	}else{
        		List<PseudonymWithMetadata> ls = new ArrayList<PseudonymWithMetadata>();
        		Smartcard sc = (Smartcard)this.cardStorage.getSmartcard(scUri);
        		Map<URI, SmartcardBlob> blobs = sc.getBlobs(this.cardStorage.getPin(scUri));
        		for(URI uri: blobs.keySet()){
        			String uriString = uri.toString();
        			if(uriString.startsWith(PSEUDONYM_PREFIX)){
                        URI pseudonymUri = URI.create(uriString.substring(0, uriString.length()-2));
                        PseudonymWithMetadata pwm = sc.getPseudonym(this.cardStorage.getPin(scUri), pseudonymUri, pseudonymSerializer);
        				Pseudonym pseudonym = pwm.getPseudonym();
        				if (pseudonym.getScope().equals(scope)
    	                        && !(onlyExclusive && !pseudonym.isExclusive())) {
    	                    ls.add(pwm);
    	                }
        			}
        		}        		
        		return ls;
        	}
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
    }

    @Override
    public PseudonymWithMetadata getPseudonymWithMetadata(Pseudonym pseudonym)
            throws CredentialManagerException {
        URI pseudonymUid = pseudonym.getPseudonymUID();
        // TODO: hgk : I think this is correct 
        // delegate to getPseudonym(...
        return getPseudonym(pseudonymUid);
/*      
        try {
            URI pseudonymUid = pseudonym.getPseudonymUID();
            URI scUri = this.getSmartcardUri();
            if(scUri == null){              
                byte[] bytes = this.storage.getPseudonymWithData(pseudonymUid);
                return this.parseBytesAsPseudonymWithMetaData(bytes);
            }else{
                Smartcard sc = (Smartcard)this.cardStorage.getSmartcard(scUri);
                pseudonymUid = this.escapeUri(pseudonymUid);
                pseudonymUid = URI.create(PSEUDONYM_PREFIX+pseudonymUid.toString());
                return sc.getPseudonym(this.cardStorage.getPin(scUri), pseudonymUid, pseudonymSerializer);
            }
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
*/        
    }

    @Override
    public PseudonymWithMetadata getPseudonym(URI pseudonymUid)
            throws CredentialManagerException, PseudonymIsNoInStorageException {
        if (pseudonymUid == null) {
            throw new CredentialManagerException("Pseudonym UID is null");
        }
        URI scUri = this.getSmartcardUri();
        if(scUri == null) {
            // storage based
            byte[] tokenBytes = null;
            try {
                tokenBytes = this.storage.getPseudonymWithData(pseudonymUid);
            } catch (Exception ex) {
                throw new CredentialManagerException(ex);
            }
            if (tokenBytes == null) {
                throw new PseudonymIsNoInStorageException(
                    "Pseudonym with UID: \"" + pseudonymUid
	                        + "\"is not in storage");
            }
            return this.parseBytesAsPseudonymWithMetaData(tokenBytes);
        } else{
            // smartcard based
            try {
        		Smartcard sc = (Smartcard)this.cardStorage.getSmartcard(scUri);
        		int pin = this.cardStorage.getPin(scUri);
        		URI cardPseudonymUid = this.escapeUri(pseudonymUid);
        		cardPseudonymUid = URI.create(PSEUDONYM_PREFIX+cardPseudonymUid.toString());
        		return sc.getPseudonym(pin, cardPseudonymUid, pseudonymSerializer);
            } catch (RuntimeException ex) {
                // TODO : Fixup needed 
                // Exception from Smartcard/PseudonymSerializer could have been more specific - eg : PseudonymIsNoInStorageException
                // -1 from inputstream means empty - returns exception like :
                //      Cannot unserialize this pseudonym: header was -1 expected header 68
                if(ex.getMessage().toString().indexOf("header was -1") != -1) {
                    
                    String key = scUri + "::" + pseudonymUid;
                    PseudonymWithMetadata saved = soderhamnTmpPseudonymMap.get(key);
                    if(saved!=null) {
                        System.out.println("For Soderhamn Pilot. Found Pseudonym in Memory Map : " + key + " - Pwd : " + saved + " : " + saved.getPseudonym().getScope()) ;
                        return saved;
                    } else {
                        System.out.println("For Soderhamn Pilot. Pseudonym NOT found in Memory Map : " + key );
                    }
                    throw new PseudonymIsNoInStorageException("Pseudonym is not stored on card!" + scUri);
                } else {
                    throw new CredentialManagerException(ex);
                }
            } catch (Exception ex) {
                throw new CredentialManagerException(ex);
            }
        }
    }

    @Override
    public boolean deletePseudonym(URI pseudonymUid) throws CredentialManagerException {
        try {
            PseudonymWithMetadata pseudonym = this.getPseudonym(pseudonymUid);
            if (pseudonym != null) {
            	URI scUri = this.getSmartcardUri();
            	if(scUri == null){            		
            		this.storage.deletePseudonymWithMetadata(pseudonymUid);
            		return true;
            	}else{
            		Smartcard sc = (Smartcard)this.cardStorage.getSmartcard(scUri);
            		sc.deletePseudonym(this.cardStorage.getPin(scUri), pseudonymUid);
            	}
            }
            return false;
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
    }

    @Override
    public void storeSecret(Secret secret)
            throws CredentialManagerException {

        ByteArrayOutputStream byteArrayOutputStream = null;
        ObjectOutputStream objectOutput = null;
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutput = new ObjectOutputStream(
                    byteArrayOutputStream);
            objectOutput.writeObject(secret);
            byte[] pwmBytes = byteArrayOutputStream.toByteArray();

            URI secretUri = secret.getSecretDescription().getSecretUID();
            this.sstorage.addSecret(secretUri, pwmBytes);
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        } finally {
            // Close the streams.
            StorageUtil.closeIgnoringException(objectOutput);
            StorageUtil.closeIgnoringException(byteArrayOutputStream);
        }
    }

    @Override
    public List<SecretDescription> listSecrets()
            throws CredentialManagerException {

        try {        	        	
            List<URI> listSecrets = this.sstorage.listSecrets();
            List<SecretDescription> ls = new ArrayList<SecretDescription>(listSecrets.size());
            for(URI uri : listSecrets){
                ls.add(this.getSecret(uri).getSecretDescription());
            }
            
            URI scUri = this.getSmartcardUri();
        	if(scUri != null){
        		Smartcard sc = (Smartcard)this.cardStorage.getSmartcard(scUri);
        		SecretDescription secretDescr = new SecretDescription();
        		secretDescr.setDeviceBoundSecret(true);
        		secretDescr.setSecretUID(sc.getDeviceURI(this.cardStorage.getPin(scUri)));
                FriendlyDescription fd = new FriendlyDescription();
                fd.setLang("en");
                fd.setValue("Placeholder for Smartcard secret");
                secretDescr.getFriendlySecretDescription().add(fd);
        		ls.add(secretDescr);
        	}
            
            return ls;
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
    }

    @Override
    public boolean deleteSecret(URI secuid)
            throws CredentialManagerException {
        try {
            Secret secret = this.getSecret(secuid);
            if (secret != null) {
                this.sstorage.deleteSecret(secuid);
                return true;
            }
            return false;
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }
    }

    @Override
    public Secret getSecret(URI secuid) throws CredentialManagerException,
    SecretNotInStorageException {
        if (secuid == null) {
            throw new IllegalArgumentException("Secret UID is null");
        }
        ObjectInputStream objectInput = null;
        ByteArrayInputStream byteArrayInputStream = null;
        try {
            byte[] secretBytes = this.sstorage.getSecret(secuid);
            if (secretBytes == null) {
                throw new SecretNotInStorageException(
                        "Secret is not in storage");
            }
            byteArrayInputStream = new ByteArrayInputStream(secretBytes);
            objectInput = new ObjectInputStream(byteArrayInputStream);
            Secret secret = (Secret) objectInput
                    .readObject();
            return secret;
        } catch (SecretNotInStorageException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        } finally {
            // Close the streams.
            StorageUtil.closeIgnoringException(objectInput);
            StorageUtil.closeIgnoringException(byteArrayInputStream);
        }
    }

    @Override
    public void updateSecretDescription(SecretDescription desc) throws CredentialManagerException {
        if (desc  == null) {
            throw new IllegalArgumentException("Secret Description is null");
        }
        try {
            Secret sec = this.getSecret(desc.getSecretUID());
            sec.setSecretDescription(desc);
            this.sstorage.deleteSecret(desc.getSecretUID());
            this.storeSecret(sec);
        } catch (Exception ex) {
            throw new CredentialManagerException(ex);
        }

    }

}
