/*
 * Copyright 2008 The Kuali Foundation
 * 
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.opensource.org/licenses/ecl2.php
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kuali.kfs.module.ar.batch;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kuali.kfs.integration.cg.ContractsAndGrantsLetterOfCreditFund;
import org.kuali.kfs.module.ar.ArConstants;
import org.kuali.kfs.module.ar.batch.service.LetterOfCreditCreateService;
import org.kuali.kfs.module.ar.document.ContractsGrantsInvoiceDocument;
import org.kuali.kfs.module.ar.document.service.ContractsGrantsInvoiceDocumentService;
import org.kuali.kfs.sys.KFSPropertyConstants;
import org.kuali.kfs.sys.batch.AbstractStep;
import org.kuali.kfs.sys.context.SpringContext;
import org.kuali.rice.core.api.datetime.DateTimeService;
import org.kuali.rice.krad.service.KualiModuleService;
import org.kuali.rice.core.api.util.type.KualiDecimal;
import org.springframework.util.CollectionUtils;

/**
 * This step of LetterOFCreditJob would create cash control documents and payment application document for CG Invoices per LOC fund
 */
public class LetterOfCreditByLOCFundBatchStep extends AbstractStep {
    private static org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(LetterOfCreditByLOCFundBatchStep.class);


    private LetterOfCreditCreateService letterOfCreditCreateService;
    private ContractsGrantsInvoiceDocumentService contractsGrantsInvoiceDocumentService;
    private DateTimeService dateTimeService;
    protected String batchFileDirectoryName;


    /**
     * This step of LetterOFCreditJob would create cash control documents and payment application document for CG Invoices per LOC
     * fund.
     * 
     * @see org.kuali.kfs.sys.batch.Step#execute(String, Date)
     */
    public boolean execute(String jobName, Date jobRunDate) {


        String customerNumber = null;
        String locValue = null;
        String locCreationType = null;
        boolean cashControlDocumentExists = false;
        String runtimeStamp = dateTimeService.toDateTimeStringForFilename(new java.util.Date());

        String errOutputFile = batchFileDirectoryName + File.separator + ArConstants.BatchFileSystem.LOC_CREATION_BY_LOCF_ERROR_OUTPUT_FILE + "_" + runtimeStamp + ArConstants.BatchFileSystem.EXTENSION;
        // To create error file and store all the errors in it.
        File errOutPutFile = new File(errOutputFile);
        PrintStream outputFileStream = null;

        try {
            outputFileStream = new PrintStream(errOutPutFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }


        // Case 2. set locCreationType = By Letter of Credit Fund
        locCreationType = ArConstants.LOC_BY_LOC_FUND;
        // Get the list of LOC Funds from the Maintenance document.
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(KFSPropertyConstants.ACTIVE, true);
        Collection<ContractsAndGrantsLetterOfCreditFund> letterOfCreditFunds = (List) SpringContext.getBean(KualiModuleService.class).getResponsibleModuleService(ContractsAndGrantsLetterOfCreditFund.class).getExternalizableBusinessObjectsList(ContractsAndGrantsLetterOfCreditFund.class, map);
        Iterator<ContractsAndGrantsLetterOfCreditFund> it = letterOfCreditFunds.iterator();
        while (it.hasNext()) {
            ContractsAndGrantsLetterOfCreditFund letterOfCreditFund = it.next();
            // Retrieve invoices per loc fund and loc creation type specification.
            Collection<ContractsGrantsInvoiceDocument> cgInvoices = contractsGrantsInvoiceDocumentService.retrieveOpenAndFinalCGInvoicesByLOCFund(letterOfCreditFund.getLetterOfCreditFundCode(), errOutputFile);
            // When all the invoices are final.
            if (!CollectionUtils.isEmpty(cgInvoices)) {

                KualiDecimal totalAmount = KualiDecimal.ZERO;
                // Get the total amount for the cash control document.
                for (ContractsGrantsInvoiceDocument cgInvoice : cgInvoices) {
                    totalAmount = totalAmount.add(cgInvoice.getOpenAmount());
                }
                customerNumber = cgInvoices.iterator().next().getAccountsReceivableDocumentHeader().getCustomerNumber();// to get
                                                                                                                        // customer
                                                                                                                        // number
                                                                                                                        // from
                                                                                                                        // first
                                                                                                                        // invoice
                // If this is LOC by fund, then the way to retrieve invoices in payment application form is locfundCode
                locValue = letterOfCreditFund.getLetterOfCreditFundCode();
                // To validate if there are any existing cash control documents for the same letterofcreditfundgroup and customer
                // number combination.

                cashControlDocumentExists = letterOfCreditCreateService.validatecashControlDocument(customerNumber, locCreationType, locValue, outputFileStream);

                if (!cashControlDocumentExists) {

                    // Pass the parameters and error file stream to maintain a single file for recording all the errors.
                    letterOfCreditCreateService.createCashControlDocuments(customerNumber, locCreationType, locValue, totalAmount, outputFileStream);
                    
                }

            }

        }
        // Close the error file.
        outputFileStream.close();

        return true;

    }

    /**
     * Gets the letterOfCreditCreateService attribute.
     * 
     * @return Returns the letterOfCreditCreateService.
     */
    public LetterOfCreditCreateService getLetterOfCreditCreateService() {
        return letterOfCreditCreateService;
    }


    /**
     * Sets the letterOfCreditCreateService attribute value.
     * 
     * @param letterOfCreditCreateService The letterOfCreditCreateService to set.
     */
    public void setLetterOfCreditCreateService(LetterOfCreditCreateService letterOfCreditCreateService) {
        this.letterOfCreditCreateService = letterOfCreditCreateService;
    }

    /**
     * Gets the dateTimeService attribute.
     * 
     * @return Returns the dateTimeService.
     */
    public DateTimeService getDateTimeService() {
        return dateTimeService;
    }

    /**
     * Sets the dateTimeService attribute value.
     * 
     * @param dateTimeService The dateTimeService to set.
     */
    public void setDateTimeService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }


    /**
     * This method...
     * 
     * @param batchFileDirectoryName
     */
    public void setBatchFileDirectoryName(String batchFileDirectoryName) {
        this.batchFileDirectoryName = batchFileDirectoryName;
    }

    /**
     * Gets the contractsGrantsInvoiceDocumentService attribute.
     * 
     * @return Returns the contractsGrantsInvoiceDocumentService.
     */
    public ContractsGrantsInvoiceDocumentService getContractsGrantsInvoiceDocumentService() {
        return contractsGrantsInvoiceDocumentService;
    }

    /**
     * Sets the contractsGrantsInvoiceDocumentService attribute value.
     * 
     * @param contractsGrantsInvoiceDocumentService The contractsGrantsInvoiceDocumentService to set.
     */
    public void setContractsGrantsInvoiceDocumentService(ContractsGrantsInvoiceDocumentService contractsGrantsInvoiceDocumentService) {
        this.contractsGrantsInvoiceDocumentService = contractsGrantsInvoiceDocumentService;
    }


}
