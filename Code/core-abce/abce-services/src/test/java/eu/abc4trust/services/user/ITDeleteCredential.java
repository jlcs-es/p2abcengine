//* Licensed Materials - Property of IBM, Miracle A/S, and            *
//* Alexandra Instituttet A/S                                         *
//* eu.abc4trust.pabce.1.0                                            *
//* (C) Copyright IBM Corp. 2012. All Rights Reserved.                *
//* (C) Copyright Miracle A/S, Denmark. 2012. All Rights Reserved.    *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2012. All       *
//* Rights Reserved.                                                  *
//* US Government Users Restricted Rights - Use, duplication or       *
//* disclosure restricted by GSA ADP Schedule Contract with IBM Corp. *
//*                                                                   *
//* This file is licensed under the Apache License, Version 2.0 (the  *
//* "License"); you may not use this file except in compliance with   *
//* the License. You may obtain a copy of the License at:             *
//*   http://www.apache.org/licenses/LICENSE-2.0                      *
//* Unless required by applicable law or agreed to in writing,        *
//* software distributed under the License is distributed on an       *
//* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY            *
//* KIND, either express or implied.  See the License for the         *
//* specific language governing permissions and limitations           *
//* under the License.                                                *
//*/**/****************************************************************

package eu.abc4trust.services.user;

import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.junit.Test;

import eu.abc4trust.services.ITAbstract;
import eu.abc4trust.services.TestScenarioFactory;
import eu.abc4trust.xml.ObjectFactory;
import eu.abc4trust.xml.URISet;

public class ITDeleteCredential extends ITAbstract {

    static ObjectFactory of = new ObjectFactory();

    final String baseUrl = "http://localhost:9500/abce-services/issuer";

    @Test
    public void issuanceProtocolIdemix() throws Exception {

        String engineSuffix = "idemix";

        this.deleteStorageDirectory("user_storage");

        UserServiceFactory userServiceFactory = new UserServiceFactory();

        URISet credentials = userServiceFactory.listCredentials();
        assertTrue(credentials.getURI().isEmpty());

        TestScenarioFactory testScenarioFactory = new TestScenarioFactory();
        URI credentialUid = testScenarioFactory.issuanceProtocol(engineSuffix);

        this.deleteCredential(credentialUid);
    }

    @Test
    public void issuanceProtocolUProve() throws Exception {

        String engineSuffix = "uprove";

        TestScenarioFactory testScenarioFactory = new TestScenarioFactory();
        URI credentialUid = testScenarioFactory.issuanceProtocol(engineSuffix);

        this.deleteCredential(credentialUid);
    }

    private void deleteCredential(URI credentialUid) {
        UserServiceFactory userServiceFactory = new UserServiceFactory();

        URISet credentials = userServiceFactory.listCredentials();
        assertTrue(credentials.getURI().size() == 1);
        assertTrue(credentials.getURI().get(0).compareTo(credentialUid) == 0);

        userServiceFactory.deleteCredential(credentialUid);

        credentials = userServiceFactory.listCredentials();
        assertTrue(credentials.getURI().isEmpty());
    }

}