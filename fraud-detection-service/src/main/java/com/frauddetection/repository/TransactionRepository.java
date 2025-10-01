package com.frauddetection.repository;

import com.frauddetection.entity.TransactionEntity;
import com.frauddetection.model.FraudStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    
    Optional<TransactionEntity> findByTransactionId(String transactionId);
    
    Optional<TransactionEntity> findByWorkflowId(String workflowId);
    
    List<TransactionEntity> findByCustomerId(String customerId);
    
    List<TransactionEntity> findByStatus(FraudStatus status);
    
    List<TransactionEntity> findByRequiresInvestigationTrue();
    
    @Query("SELECT t FROM TransactionEntity t WHERE t.customerId = :customerId AND t.createdAt >= :fromTime")
    List<TransactionEntity> findRecentTransactionsByCustomer(@Param("customerId") String customerId, 
                                                           @Param("fromTime") LocalDateTime fromTime);
    
    @Query("SELECT t FROM TransactionEntity t WHERE t.riskScore >= :minRiskScore ORDER BY t.riskScore DESC")
    List<TransactionEntity> findHighRiskTransactions(@Param("minRiskScore") BigDecimal minRiskScore);
    
    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.decision = 'DENIED' AND t.createdAt >= :fromTime")
    Long countDeniedTransactionsSince(@Param("fromTime") LocalDateTime fromTime);
    
    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.decision = 'APPROVED' AND t.createdAt >= :fromTime")
    Long countApprovedTransactionsSince(@Param("fromTime") LocalDateTime fromTime);
}
