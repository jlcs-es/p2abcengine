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

package eu.abc4trust.ri.service.revocation;

import java.io.File;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import eu.abc4trust.abce.internal.revocation.RevocationConstants;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.returnTypes.RevocationMessageAndBoolean;
import eu.abc4trust.ri.servicehelper.revocation.RevocationHelper;
import eu.abc4trust.ri.servicehelper.revocation.RevocationHelper.RevocationReferences;
import eu.abc4trust.xml.Attribute;
import eu.abc4trust.xml.AttributeDescription;
import eu.abc4trust.xml.CryptoParams;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.RevocationInformation;
import eu.abc4trust.xml.RevocationMessage;
import eu.abc4trust.xml.util.XmlUtils;


/** class RevocationService 
 *  This is a demo implementation. This particular service will either accept or fail all 
 *  RevocationMessage requests in a number of rounds.
 *  It is expected that 3d party providers implement this interface and do what needs to be done
 */
@Path("/")
public class RevocationService {
  
  public static final URI soderhamnRevocationAuthority = URI.create("urn:soderhamn:revocationauthority:default");

  private ObjectFactory of = new ObjectFactory();

  public RevocationService() {
      System.out.println("RevocationService created");
  }

  public void initRevocationHelper(String testcase) throws Exception {
    System.out.println("RevocationService - initHelper : " + testcase);
    String fileStoragePrefix;
    String systemParametersResource; 
    String[] issuerParamsResourceList = {};
    if (new File("target").exists()) {
      fileStoragePrefix = "target/revocation_";
      systemParametersResource = "target/issuer_bridged_system_params_bridged";
      issuerParamsResourceList = new String[] { "target/issuer_bridged_issuer_params_urn_soderhamn_issuer_credSchool_idemix",
                                                "target/issuer_bridged_issuer_params_urn_soderhamn_issuer_credSchool_uprove"};

    } else {
      fileStoragePrefix = "integration-test-revocation/target/revocation_";
      systemParametersResource = "integration-test-revocation/target/issuer_bridged_system_params_bridged";
      issuerParamsResourceList = new String[] { "integration-test-revocation/target/issuer_bridged_issuer_params_urn_soderhamn_issuer_credSchool_idemix",
                                                "integration-test-revocation/target/issuer_bridged_issuer_params_urn_soderhamn_issuer_credSchool_uprove"};
    }

    String[] credSpecResourceList =
        {"/eu/abc4trust/sampleXml/soderhamn/credentialSpecificationSoderhamnSchoolWithRevocation.xml"};

    RevocationHelper.resetInstance();
    URI revocationInfoReference = URI.create("http://localhost:9094/integration-test-revocation/revocation/info");
    URI nonRevocationEvidenceReference = URI.create("http://localhost:9094/integration-test-revocation/nonrevocation/evidence");
    URI nonRevocationUpdateReference = URI.create("http://localhost:9094/integration-test-revocation/nonrevocation/update");;
    RevocationReferences revocationReferences = new RevocationReferences(soderhamnRevocationAuthority, revocationInfoReference, nonRevocationEvidenceReference, nonRevocationUpdateReference);
    RevocationHelper.initInstance(CryptoEngine.BRIDGED, fileStoragePrefix, fileStoragePrefix, systemParametersResource, issuerParamsResourceList , credSpecResourceList, revocationReferences );
  }

  @GET()
  @Path("/init/{testcase}")
  @Produces(MediaType.TEXT_PLAIN)
  public String init(@PathParam("testcase") final String testcase) throws Exception {
    System.out.println("revocation service.init - for testcase : " + testcase);
    initRevocationHelper(testcase);
    return "OK";
  }

    // INFO

  @POST()
  @Path("/revocation/revokeAttribute/{revParUid}")
//  @Produces(MediaType.APPLICATION_XML)
  public JAXBElement<RevocationInformation> revoke(@PathParam ("revParUid") final URI revParUid, final Attribute in) throws Exception {
      System.out.println("=========== R E V O K E ===========");
      System.out.println("revoke attribute! " + revParUid + " " + in.getAttributeUID() + " : " + in.getAttributeValue());
      System.out.println("XML " + XmlUtils.toXml(of.createAttribute(in), false));
      List<Attribute> attributes = new ArrayList<Attribute>();
      attributes.add(in);
      RevocationInformation ri = RevocationHelper.getInstance().engine.revoke(revParUid, attributes);
      System.out.println("RevocationInformation : " + ri + " : " + ri.getInformationUID());
      return of.createRevocationInformation(ri);
  }
  @POST()
  @Path("/revocation/revokeHandle/{revParUid}")
//  @Produces(MediaType.APPLICATION_XML)
  public JAXBElement<RevocationInformation> revoke(@PathParam ("revParUid") final URI revParUid, @QueryParam("revocationHandle") final BigInteger revocationHandle) throws Exception {
      System.out.println("=========== R E V O K E ===========");
      System.out.println("revoke attribute! " + revParUid + " " + revocationHandle);
      
      // create Attribute based on handle...
      Attribute in = new Attribute();
      AttributeDescription ad = new AttributeDescription();

      ad.setType(RevocationConstants.REVOCATION_HANDLE);
      ad.setDataType(RevocationConstants.REVOCATION_HANDLE_DATA_TYPE);
      ad.setEncoding(RevocationConstants.REVOCATION_HANDLE_ENCODING);
      
      in.setAttributeDescription(ad );
      in.setAttributeValue(revocationHandle);
      
//      in.getAttributeUID() + " : " + in.getAttributeValue());
      System.out.println("XML " + XmlUtils.toXml(of.createAttribute(in), false));
      List<Attribute> attributes = new ArrayList<Attribute>();
      attributes.add(in);
      RevocationInformation ri = RevocationHelper.getInstance().engine.revoke(revParUid, attributes);
      System.out.println("RevocationInformation : " + ri + " : " + ri.getInformationUID());
      return of.createRevocationInformation(ri);
  }

    @POST()
    @Path("/revocation/info")
//    @Produces(MediaType.APPLICATION_XML)
    public JAXBElement<RevocationMessage> revocationInfo(final RevocationMessage in) throws Exception {
        System.out.println("===========================================");
        System.out.println("revocationInfo");
        System.out.println("XML " + XmlUtils.toXml(of.createRevocationMessage(in), false));
    
        RevocationMessageAndBoolean out = RevocationHelper.getInstance().revocationProxyAuthority.processRevocationMessage(in);

        System.out.println("XML OUT : " + XmlUtils.toXml(of.createRevocationMessage(out.revmess), false));

        return of.createRevocationMessage(out.revmess);
    }

  @POST()
  @Path("/nonrevocation/evidence")
//  @Produces(MediaType.APPLICATION_XML)
    public JAXBElement<RevocationMessage> nonRevocationEvidence(final RevocationMessage in) throws Exception {
        System.out.println("===========================================");
        System.out.println("nonRevocationEvidence");
        System.out.println("XML " + XmlUtils.toXml(new ObjectFactory().createRevocationMessage(in), false));
        
        RevocationMessageAndBoolean out = RevocationHelper.getInstance().revocationProxyAuthority.processRevocationMessage(in);

        System.out.println("XML OUT : " + XmlUtils.toXml(of.createRevocationMessage(out.revmess), false));

        return of.createRevocationMessage(out.revmess);
    }

    @POST()
    @Path("/nonrevocation/update")
//     @Produces(MediaType.APPLICATION_XML)
    public JAXBElement<RevocationMessage> nonRevocationUpdate(final RevocationMessage in) throws Exception {

      System.out.println("===========================================");
        System.out.println("nonRevocationUpdate");
        System.out.println("XML " + XmlUtils.toXml(new ObjectFactory().createRevocationMessage(in), false));
        
        RevocationMessageAndBoolean out = RevocationHelper.getInstance().revocationProxyAuthority.processRevocationMessage(in);

        System.out.println("XML OUT : " + XmlUtils.toXml(of.createRevocationMessage(out.revmess), false));
        
        return of.createRevocationMessage(out.revmess);
    }
  
}
