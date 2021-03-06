//* Licensed Materials - Property of                                  *
//* IBM                                                               *
//*                                                                   *
//* eu.abc4trust.pabce.1.34                                           *
//*                                                                   *
//* (C) Copyright IBM Corp. 2014. All Rights Reserved.                *
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

package eu.abc4trust;

public class TestConfiguration {
  //  --- For testing locally ---
  //  --- Uncomment, but do not commit! ---
//  public static final int TEST_TIMEOUT = 10*1000*10000;
//  public static final boolean OVERRIDE_SECURITY_LEVEL = true;
  
  //  --- For testing on Jenkins ---
  public static final int TEST_TIMEOUT = 10*60*1000;
  public static final boolean OVERRIDE_SECURITY_LEVEL = false;
  public static final int MAXIMAL_NUMBER_OF_ATTRIBUTES = 10;
}
