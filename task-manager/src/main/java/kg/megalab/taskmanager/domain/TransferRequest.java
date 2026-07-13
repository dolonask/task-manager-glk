package kg.megalab.taskmanager.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "transfer_request")
public class TransferRequest {

    @Id
    private UUID id = UUID.randomUUID();

    @ManyToOne(optional = false)
    private Task task;

    /** Head of department who created the request. */
    @ManyToOne(optional = false)
    private User initiator;

    /** Curating board member expected to decide (department.curator at creation time). */
    @ManyToOne
    private User approver;

    @Column(nullable = false)
    private LocalDate currentDeadline;

    private LocalDate proposedDeadline;

    @Column(length = 2000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.DRAFT;

    @Column(length = 2000)
    private String decisionReason;

    private Instant submittedAt;
    private Instant decidedAt;
    private Instant appliedAt;

    @ManyToOne
    private User appliedBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public User getInitiator() {
        return initiator;
    }

    public void setInitiator(User initiator) {
        this.initiator = initiator;
    }

    public User getApprover() {
        return approver;
    }

    public void setApprover(User approver) {
        this.approver = approver;
    }

    public LocalDate getCurrentDeadline() {
        return currentDeadline;
    }

    public void setCurrentDeadline(LocalDate currentDeadline) {
        this.currentDeadline = currentDeadline;
    }

    public LocalDate getProposedDeadline() {
        return proposedDeadline;
    }

    public void setProposedDeadline(LocalDate proposedDeadline) {
        this.proposedDeadline = proposedDeadline;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(Instant appliedAt) {
        this.appliedAt = appliedAt;
    }

    public User getAppliedBy() {
        return appliedBy;
    }

    public void setAppliedBy(User appliedBy) {
        this.appliedBy = appliedBy;
    }
}
