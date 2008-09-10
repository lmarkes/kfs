/*
 * Copyright 2008 The Kuali Foundation.
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
package org.kuali.kfs.module.purap.service;

import java.util.List;

import org.kuali.kfs.module.purap.businessobject.ElectronicInvoice;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceLoad;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceLoadSummary;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceOrder;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceRejectReason;
import org.kuali.kfs.module.purap.document.ElectronicInvoiceRejectDocument;

public interface ElectronicInvoiceParserService {

    public boolean loadElectronicInvoices();
    
    public ElectronicInvoiceLoadSummary getOrCreateLoadSummary(ElectronicInvoiceLoad eInvoiceLoad,String fileDunsNumber);
    
    public void doMatchingProcess(ElectronicInvoiceRejectDocument rejectDocument);
    
    public ElectronicInvoiceRejectDocument createAndSaveRejectDocument(ElectronicInvoiceLoad eInvoiceLoad, 
                                                                       ElectronicInvoice eInvoice,
                                                                       ElectronicInvoiceOrder electronicInvoiceOrder);
    
}
