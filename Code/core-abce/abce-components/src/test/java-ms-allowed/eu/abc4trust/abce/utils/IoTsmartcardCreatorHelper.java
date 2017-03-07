// Edited by Jose Luis Canovas

//* Licensed Materials - Property of                                  *
//* IBM                                                               *
//* Alexandra Instituttet A/S                                         *
//*                                                                   *
//* eu.abc4trust.pabce.1.34                                           *
//*                                                                   *
//* (C) Copyright IBM Corp. 2014. All Rights Reserved.                *
//* (C) Copyright Alexandra Instituttet A/S, Denmark. 2014. All       *
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

package eu.abc4trust.abce.utils;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Random;

import com.ibm.zurich.idmix.abc4trust.facades.SmartcardParametersFacade;
import com.ibm.zurich.idmx.buildingBlock.systemParameters.EcryptSystemParametersWrapper;
import com.ibm.zurich.idmx.exception.ConfigurationException;

import eu.abc4trust.cryptoEngine.CryptoEngineException;
import eu.abc4trust.guice.ProductionModuleFactory.CryptoEngine;
import eu.abc4trust.smartcard.BasicSmartcard;
import eu.abc4trust.smartcard.RSAKeyPair;
import eu.abc4trust.smartcard.RSASignatureSystem;
import eu.abc4trust.smartcard.RSASignatureSystemTest;
import eu.abc4trust.smartcard.RSAVerificationKey;
import eu.abc4trust.smartcard.Smartcard;
import eu.abc4trust.smartcard.SmartcardBlob;
import eu.abc4trust.smartcard.SmartcardParameters;
import eu.abc4trust.smartcard.SmartcardStatusCode;
import eu.abc4trust.smartcard.HardwareSmartcard;
import eu.abc4trust.smartcard.IoTsmartcardio.IoTCardTerminal;
import eu.abc4trust.xml.IssuerParameters;
import eu.abc4trust.xml.Secret;
import eu.abc4trust.xml.SmartcardSystemParameters;
import eu.abc4trust.xml.SystemParameters;

public class IoTsmartcardCreatorHelper {


    RSAKeyPair sk_root = RSASignatureSystemTest.getSigningKeyForTest();
    RSAVerificationKey pk_root = RSASignatureSystem
            .getVerificationKey(this.sk_root);

    HardwareSmartcard hardwareSmartcard;

    int pin = 1234; // TODO : generate at random
    int puk;


    public IoTsmartcardCreatorHelper(Random random, SystemParameters systemParameters, String host, int port) throws ConfigurationException {

        URI deviceUri = URI.create("secret://iot-smartcard-"
                + random.nextInt(9999999));

        byte[] deviceID_bytes = new byte[2];
        random.nextBytes(deviceID_bytes);
        short deviceID = ByteBuffer.wrap(deviceID_bytes).getShort();

        EcryptSystemParametersWrapper spw = new EcryptSystemParametersWrapper(systemParameters);

        SmartcardSystemParameters scSysParams = new SmartcardSystemParameters();

        BigInteger p, g, subgroupOrder;
        try {
          p = spw.getDHModulus().getValue();
          g = spw.getDHGenerator1().getValue();
          subgroupOrder = spw.getDHSubgroupOrder().getValue();
        } catch (ConfigurationException e1) {
          throw new RuntimeException(e1);
        }
        int zkChallengeSizeBytes = spw.getHashLength() / 8;
        int zkStatisticalHidingSizeBytes = spw.getStatisticalInd() / 8;
        int deviceSecretSizeBytes = spw.getAttributeLength() / 8;
        int signatureNonceLengthBytes = 128 / 8;
        int zkNonceSizeBytes = 256 / 8;
        int zkNonceOpeningSizeBytes = 256 / 8;

        scSysParams.setPrimeModulus(p);
        scSysParams.setGenerator(g);
        scSysParams.setSubgroupOrder(subgroupOrder);
        scSysParams.setZkChallengeSizeBytes(zkChallengeSizeBytes);
        scSysParams
        .setZkStatisticalHidingSizeBytes(zkStatisticalHidingSizeBytes);
        scSysParams.setDeviceSecretSizeBytes(deviceSecretSizeBytes);
        scSysParams.setSignatureNonceLengthBytes(signatureNonceLengthBytes);
        scSysParams.setZkNonceSizeBytes(zkNonceSizeBytes);
        scSysParams.setZkNonceOpeningSizeBytes(zkNonceOpeningSizeBytes);

        eu.abc4trust.smartcard.SystemParameters sc_sysParams = new eu.abc4trust.smartcard.SystemParameters(
                scSysParams);

        // Create Hardware IoT Smartcard

        IoTCardTerminal cardTerminal = new IoTCardTerminal(host, port);

        this.hardwareSmartcard = new HardwareSmartcard(cardTerminal, random);
        this.puk = this.hardwareSmartcard.init(this.pin, sc_sysParams,
                this.sk_root, deviceID);
        SmartcardBlob blob = new SmartcardBlob();
        try {
			blob.blob = deviceUri.toASCIIString().getBytes("US-ASCII");
		} catch (UnsupportedEncodingException e) {
		}
        this.hardwareSmartcard.storeBlob(pin, Smartcard.device_name, blob);
        System.out.println("SoftwareSmartcard is now init'ed "
                + this.hardwareSmartcard);
    }

    public BasicSmartcard getHardwareSmartcard() {
        return this.hardwareSmartcard;
    }

    public void addIssuerParameters(IssuerParameters issuerParameters, SystemParameters sp) {
        SmartcardParametersFacade spf = new SmartcardParametersFacade(sp, issuerParameters);
        SmartcardParameters credBases;
        try {
            credBases = spf.getSmartcardParameters();
        } catch (CryptoEngineException e) {
            throw new RuntimeException(e);
        }

        this.hardwareSmartcard.getNewNonceForSignature();
        URI parametersUri = issuerParameters.getParametersUID();

        SmartcardStatusCode universityResult = this.hardwareSmartcard
                .addIssuerParameters(this.sk_root, parametersUri,
                        credBases);
        if (universityResult != SmartcardStatusCode.OK) {
            throw new RuntimeException(
                    "Could not add IssuerParams to smartcard... "
                            + universityResult);
        }
    }

    public int getPin() {
        return this.pin;
    }
}
