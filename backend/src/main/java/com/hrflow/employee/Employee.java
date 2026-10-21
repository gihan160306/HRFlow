package com.hrflow.employee;

import java.time.LocalDate;

import com.hrflow.common.audit.AuditableEntity;
import com.hrflow.department.Department;

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

/** Hồ sơ nhân sự — khớp bảng employee trong data_dictionary.md §3.1. */
@Entity
@Table(name = "employee")
public class Employee extends AuditableEntity {

    /** Nhóm tính lương. "Thử việc" là loại hợp đồng, không phải employee_type. */
    public enum EmployeeType { STAFF, SALES, LEADER, MANAGER, INTERN }

    public enum Status { ACTIVE, TERMINATED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 20)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "work_email", nullable = false, unique = true, length = 255)
    private String workEmail;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "address", length = 255)
    private String address;

    // ----- Dữ liệu nhạy cảm: chỉ HR/Admin được xem, không ghi vào log -----
    @Column(name = "national_id", unique = true, length = 12)
    private String nationalId;

    @Column(name = "tax_code", length = 14)
    private String taxCode;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_account_no", length = 30)
    private String bankAccountNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "job_title", nullable = false, length = 100)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "employee_type", nullable = false, length = 10)
    private EmployeeType employeeType;

    /** Quản lý trực tiếp; NULL thì HR duyệt đơn (BR-05). */
    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private Status status = Status.ACTIVE;

    @Column(name = "dependents_count", nullable = false)
    private short dependentsCount = 0;

    @Column(name = "tax_commitment_08", nullable = false)
    private boolean taxCommitment08 = false;

    @Column(name = "ot_eligible", nullable = false)
    private boolean otEligible = false;

    @Column(name = "commission_eligible", nullable = false)
    private boolean commissionEligible = false;

    @Column(name = "kpi_eligible", nullable = false)
    private boolean kpiEligible = false;

    @Column(name = "performance_bonus_eligible", nullable = false)
    private boolean performanceBonusEligible = false;

    @Column(name = "team_bonus_eligible", nullable = false)
    private boolean teamBonusEligible = false;

    @Column(name = "lunch_allowance_eligible", nullable = false)
    private boolean lunchAllowanceEligible = false;

    @Column(name = "parking_allowance_eligible", nullable = false)
    private boolean parkingAllowanceEligible = false;

    @Column(name = "phone_allowance_eligible", nullable = false)
    private boolean phoneAllowanceEligible = false;

    @Column(name = "position_allowance_eligible", nullable = false)
    private boolean positionAllowanceEligible = false;

    public boolean isTerminated() { return status == Status.TERMINATED; }

    // ----- getters / setters -----
    public Long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public void setEmployeeCode(String v) { this.employeeCode = v; }
    public String getFullName() { return fullName; }
    public void setFullName(String v) { this.fullName = v; }
    public String getWorkEmail() { return workEmail; }
    public void setWorkEmail(String v) { this.workEmail = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate v) { this.dateOfBirth = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String v) { this.nationalId = v; }
    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String v) { this.taxCode = v; }
    public String getBankName() { return bankName; }
    public void setBankName(String v) { this.bankName = v; }
    public String getBankAccountNo() { return bankAccountNo; }
    public void setBankAccountNo(String v) { this.bankAccountNo = v; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department v) { this.department = v; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String v) { this.jobTitle = v; }
    public EmployeeType getEmployeeType() { return employeeType; }
    public void setEmployeeType(EmployeeType v) { this.employeeType = v; }
    public Long getManagerId() { return managerId; }
    public void setManagerId(Long v) { this.managerId = v; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate v) { this.hireDate = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { this.status = v; }
    public short getDependentsCount() { return dependentsCount; }
    public void setDependentsCount(short v) { this.dependentsCount = v; }
    public boolean isTaxCommitment08() { return taxCommitment08; }
    public void setTaxCommitment08(boolean v) { this.taxCommitment08 = v; }
    public boolean isOtEligible() { return otEligible; }
    public void setOtEligible(boolean v) { this.otEligible = v; }
    public boolean isCommissionEligible() { return commissionEligible; }
    public void setCommissionEligible(boolean v) { this.commissionEligible = v; }
    public boolean isKpiEligible() { return kpiEligible; }
    public void setKpiEligible(boolean v) { this.kpiEligible = v; }
    public boolean isPerformanceBonusEligible() { return performanceBonusEligible; }
    public void setPerformanceBonusEligible(boolean v) { this.performanceBonusEligible = v; }
    public boolean isTeamBonusEligible() { return teamBonusEligible; }
    public void setTeamBonusEligible(boolean v) { this.teamBonusEligible = v; }
    public boolean isLunchAllowanceEligible() { return lunchAllowanceEligible; }
    public void setLunchAllowanceEligible(boolean v) { this.lunchAllowanceEligible = v; }
    public boolean isParkingAllowanceEligible() { return parkingAllowanceEligible; }
    public void setParkingAllowanceEligible(boolean v) { this.parkingAllowanceEligible = v; }
    public boolean isPhoneAllowanceEligible() { return phoneAllowanceEligible; }
    public void setPhoneAllowanceEligible(boolean v) { this.phoneAllowanceEligible = v; }
    public boolean isPositionAllowanceEligible() { return positionAllowanceEligible; }
    public void setPositionAllowanceEligible(boolean v) { this.positionAllowanceEligible = v; }
}