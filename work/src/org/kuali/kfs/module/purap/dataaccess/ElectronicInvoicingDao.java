/*
 * Created on Mar 2, 2006
 *
 */
package org.kuali.kfs.module.purap.dataaccess;

import java.util.List;
import java.util.Map;

import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceLoadSummary;
import org.kuali.kfs.module.purap.businessobject.ElectronicInvoiceReject;

/**
 * @author delyea
 *
 */
public interface ElectronicInvoicingDao {
  public ElectronicInvoiceLoadSummary getElectronicInvoiceLoadSummary(Integer loadId,String vendorDunsNumber);
  public ElectronicInvoiceLoadSummary saveElectronicInvoiceLoadSummary(ElectronicInvoiceLoadSummary loadSummary);
  public ElectronicInvoiceReject saveElectronicInvoiceReject(ElectronicInvoiceReject reject);
  public List getPendingElectronicInvoices();
  public Map getDefaultItemMappingMap();
  public Map getItemMappingMap(Integer vendorHeaderId, Integer vendorDetailId);
}
