/*
 * Copyright 2010 The Kuali Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.module.endow.document;


public abstract class EndowmentSecurityDetailsDocumentBase extends EndowmentTransactionalDocumentBase implements EndowmentSecurityDetailsDocument {

    private String securityId;
    private String securityClassCode;
    private String securityTaxLotIndicator;
    private String securityTransactionCode;
    private String registrationCode;

    public String getRegistrationCode() {
        return registrationCode;
    }


    public String getSecurityClassCode() {
        return securityClassCode;
    }


    public String getSecurityId() {
        return securityId;
    }


    public String getSecurityTaxLotIndicator() {
        return securityTaxLotIndicator;
    }


    public String getSecurityTransactionCode() {
        return securityTransactionCode;
    }


    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;

    }


    public void setSecurityClassCode(String securityClassCode) {
        this.securityClassCode = securityClassCode;

    }


    public void setSecurityId(String securityId) {
        this.securityId = securityId;

    }


    public void setSecurityTaxLotIndicator(String securityTaxLotIndicator) {
        this.securityTaxLotIndicator = securityTaxLotIndicator;

    }
}
