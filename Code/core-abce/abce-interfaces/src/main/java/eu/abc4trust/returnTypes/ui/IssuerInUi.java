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

package eu.abc4trust.returnTypes.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import eu.abc4trust.returnTypes.ui.adapters.CredentialSpecAdapter;
import eu.abc4trust.xml.CredentialSpecification;
import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.IssuerParameters;

@XmlType(namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class IssuerInUi {
  @XmlID
  @XmlAttribute
  public String uri;
  
  @XmlElement(required=false)
  public URI revocationAuthorityUri;
  
  @XmlElementWrapper
  @XmlElement(name = "description")
  public List<FriendlyDescription> description;
  
  @XmlElement
  @XmlJavaTypeAdapter(CredentialSpecAdapter.class)
  public CredentialSpecInUi spec;
  
  
  public IssuerInUi() {
    this.description = new ArrayList<FriendlyDescription>();
  }
  
  public IssuerInUi(IssuerParameters ip, CredentialSpecification spec) {
    this.uri = ip.getParametersUID().toString();
    this.description = ip.getFriendlyIssuerDescription();
    this.spec = new CredentialSpecInUi(spec);
    this.revocationAuthorityUri = ip.getRevocationParametersUID();
  }

  @Override
  public String toString() {
    return "IssuerInUi [uri=" + uri + ", description=" + description + ", spec=" + spec + "]";
  }
}
