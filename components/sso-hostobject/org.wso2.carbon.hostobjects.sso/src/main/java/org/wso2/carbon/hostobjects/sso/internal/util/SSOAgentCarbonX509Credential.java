/*
 *
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.carbon.hostobjects.sso.internal.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.core.util.KeyStoreManager;
import org.wso2.carbon.hostobjects.sso.exception.SSOHostObjectException;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

public class SSOAgentCarbonX509Credential implements SSOAgentX509Credential {

    private static Log log = LogFactory.getLog(SSOAgentCarbonX509Credential.class);

    private PublicKey publicKey = null;
    private PrivateKey privateKey = null;
    private X509Certificate entityCertificate = null;

    @Override
    public PublicKey getPublicKey() {
        return publicKey;
    }

    @Override
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public X509Certificate getEntityCertificate() {
        return entityCertificate;
    }

    public SSOAgentCarbonX509Credential(int tenantId, String tenantDomain)
            throws SSOHostObjectException {

        readCarbonX509Credentials(tenantId, tenantDomain);
    }

    protected void readCarbonX509Credentials(int tenantId, String tenantDomain) throws SSOHostObjectException {

        KeyStoreManager tenantKSM = KeyStoreManager.getInstance(tenantId);
        if (!tenantDomain.equals(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            // derive key store name
            String ksName = tenantDomain.trim().replace(".", "-");
            // derive JKS name
            String jksName = ksName + ".jks";
            KeyStore keyStore = null;
            try {
                keyStore = tenantKSM.getKeyStore(jksName);
            } catch (Exception e) { // getKeyStore() method throws a generic Exception
                handleException("Error occurred while retrieving key store of tenant " + tenantDomain, e);
                return;
            }
            try {
                entityCertificate = (X509Certificate) keyStore.getCertificate(tenantDomain);
            } catch (KeyStoreException e) {
                handleException("Error occurred while retrieving public certificate with alias " + tenantDomain +
                        " of tenant " + tenantDomain, e);
            }
            privateKey = (PrivateKey) tenantKSM.getPrivateKey(jksName, tenantDomain);
        } else {
            try {
                entityCertificate = tenantKSM.getDefaultPrimaryCertificate();
            } catch (Exception e) { // getDefaultPrimaryCertificate() method throws a generic Exception
                handleException("Error retrieving default primary certificate of " +
                        MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, e);
            }
            try {
                privateKey = tenantKSM.getDefaultPrivateKey();
            } catch (Exception e) { //getDefaultPrivateKey() method throws a generic Exception
                handleException("Error retrieving default private key of " +
                        MultitenantConstants.SUPER_TENANT_DOMAIN_NAME, e);
            }
        }
        publicKey = entityCertificate.getPublicKey();
    }

    private void handleException(String errorMessage, Throwable e) throws SSOHostObjectException {
        log.error(errorMessage);
        throw new SSOHostObjectException(errorMessage, e);
    }
}