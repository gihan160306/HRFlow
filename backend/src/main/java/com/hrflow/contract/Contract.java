package com.hrflow.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.hrflow.common.audit.AuditableEntity;
import com.hrflow.employee.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Giai đoạn hợp đồng — khớp bảng contract (data_dictionary.md §3.2).
 * Mỗi thay đổi lương/loại hợp đồng/gia hạn tạo dòng mới, không sửa dòng cũ (payroll_rules §13.3).
 */
@Entity
@Table(name = "contract")
public class Contract extends AuditableEntity {

    public enum ContractType { PROBATION, FIXED_TERM, INDEFINITE, INTERNSHIP }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    @Column(name = "contract_no", nullable = false, unique = true, length = 50)
    private String contractNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type", nullable = false, length = 12)
    private ContractType contractType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** NULL chỉ khi INDEFINITE. */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "base_salary", precision = 15, scale = 0)
    private BigDecimal baseSalary;

    @Column(name = "monthly_stipend", precision = 15, scale = 0)
    private BigDecimal monthlyStipend;

    @Column(name = "probation_rate", precision = 4, scale = 3)
    private BigDecimal probationRate;

    @Column(name = "insurance_salary", precision = 15, scale = 0)
    private BigDecimal insuranceSalary;

    @Column(name = "probation_insured", nullable = false)
    private boolean probationInsured = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 14)
    private ContractStatus status = ContractStatus.ACTIVE;

    @Column(name = "renewed_from_contract_id")
    private Long renewedFromContractId;

    @Column(name = "note", length = 500)
    private String note;

    /** app_user.user_id của người tạo. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee v) { this.employee = v; }
    public String getContractNo() { return contractNo; }
    public void setContractNo(String v) { this.contractNo = v; }
    public ContractType getContractType() { return contractType; }
    public void setContractType(ContractType v) { this.contractType = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal v) { this.baseSalary = v; }
    public BigDecimal getMonthlyStipend() { return monthlyStipend; }
    public void setMonthlyStipend(BigDecimal v) { this.monthlyStipend = v; }
    public BigDecimal getProbationRate() { return probationRate; }
    public void setProbationRate(BigDecimal v) { this.probationRate = v; }
    public BigDecimal getInsuranceSalary() { return insuranceSalary; }
    public void setInsuranceSalary(BigDecimal v) { this.insuranceSalary = v; }
    public boolean isProbationInsured() { return probationInsured; }
    public void setProbationInsured(boolean v) { this.probationInsured = v; }
    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus v) { this.status = v; }
    public Long getRenewedFromContractId() { return renewedFromContractId; }
    public void setRenewedFromContractId(Long v) { this.renewedFromContractId = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long v) { this.createdBy = v; }
}