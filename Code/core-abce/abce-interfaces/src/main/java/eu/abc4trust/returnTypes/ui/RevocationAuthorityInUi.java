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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

import eu.abc4trust.xml.FriendlyDescription;
import eu.abc4trust.xml.RevocationAuthorityParameters;

@XmlType(namespace = "http://abc4trust.eu/wp2/abcschemav1.0")
public class RevocationAuthorityInUi {
  @XmlID
  @XmlAttribute
  public String uri;
  
  @XmlElementWrapper
  @XmlElement(name = "description")
  public List<FriendlyDescription> description;
  
  public RevocationAuthorityInUi() {
    this.description = new ArrayList<FriendlyDescription>();
  }
  
  
  public RevocationAuthorityInUi(RevocationAuthorityParameters rap) {
    this.uri = rap.getParametersUID().toString();
    // TODO(enr): Add description to RevocationAuthorityParameters instead of providing dummy one
    description = new ArrayList<FriendlyDescription>();
    FriendlyDescription dummyDescription = new FriendlyDescription();
    dummyDescription.setLang("en");
    dummyDescription.setValue("RevAuth-" + uri);
    description.add(dummyDescription);
  }


  @Override
  public String toString() {
    return "RevocationAuthorityInUi [uri=" + uri + ", description=" + description + "]";
  }
}
