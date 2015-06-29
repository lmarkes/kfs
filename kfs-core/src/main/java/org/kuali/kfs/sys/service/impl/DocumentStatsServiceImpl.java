package org.kuali.kfs.sys.service.impl;

import org.kuali.kfs.sys.dataaccess.DocumentStatsDao;
import org.kuali.kfs.sys.service.DocumentStatsService;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Transactional
public class DocumentStatsServiceImpl implements DocumentStatsService {
    protected DocumentStatsDao documentStatsDao;

    @Override
    public List<Map<String, Integer>> reportNumInitiatedDocsByDocType(int limit, int days) {
        try {
            return getDocumentStatsDao().reportNumInitiatedDocsByDocType(limit, days);
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Map<String, Map<String, Integer>> reportCompletedActionRequestsByPrincipal(int limit) {
        return getDocumentStatsDao().reportCompletedActionRequestsByPrincipal(limit);
    }

    @Override
    public Map<String, Map<String, Integer>> reportUncompletedActionRequstsByPrincipal(int limit) {
        return getDocumentStatsDao().reportUncompletedActionRequestsByPrincipal(limit);
    }

    @Override
    public Map<String, Integer> reportUncompletedActionRequestsByType() {
        return getDocumentStatsDao().reportUncompletedActionRequestsByType();
    }

    @Override
    public Map<String, Integer> reportCompletedActionRequestsByType() {
        return getDocumentStatsDao().reportCompletedActionRequestsByType();
    }

    @Override
    public List<Map<String, Map<String, Integer>>> reportNumDocsByStatusByDocType(int limit, int days) {
        try {
            return getDocumentStatsDao().reportNumDocsByStatusByDocType(limit, days);
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

    @Override
    public Map<String, Integer> reportNumDocsByStatus(int days) {
        try {
            return getDocumentStatsDao().reportNumDocsByStatus(days);
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

    public DocumentStatsDao getDocumentStatsDao() {
        return documentStatsDao;
    }

    public void setDocumentStatsDao(DocumentStatsDao documentStatsDao) {
        this.documentStatsDao = documentStatsDao;
    }
}