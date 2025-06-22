package com.spkt.libraSys.service.loan;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import com.spkt.libraSys.service.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "loans")
public class LoanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    long transactionId;

    @Column(name = "loan_date")
    LocalDateTime loanDate;  // Ngày mượn sách

    @Column(name = "due_date")
    LocalDate dueDate;  // Ngày dự kiến trả

    @Column(name = "return_date")
    LocalDateTime returnDate;  // Ngày trả sách thực tế (có thể null nếu chưa trả)

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    LoanStatus status;

    @Column(name = "fine_amount", nullable = false)
    @Builder.Default
    double fineAmount = 0.0;

    @Column(name = "payment_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    PaymentStatus paymentStatus = PaymentStatus.NON_PAYMENT;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    @Column(name = "paid_at")
    LocalDateTime paidAt;

    @Column(name = "vnp_txn_ref") // Trường để lưu vnp_TxnRef
    String vnpTxnRef;

    @Column(name = "return_condition", length = 20)
    @Enumerated(EnumType.STRING)
    Condition returnCondition; // Trạng thái của sách khi trả lại
    // Mối quan hệ nhiều-một với Document (tài liệu mượn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "physical_document_id", nullable = false)
    PhysicalDocumentEntity physicalDoc;

    // Mối quan hệ nhiều-một với User (người mượn)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserEntity userEntity;

    // Thêm thông tin người cho mượn (thủ thư)
    @Column(name = "librarian_name")
    private String librarianName;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    public boolean isOverdue() {
        return returnDate == null && dueDate != null && dueDate.isBefore(LocalDate.now());
    }
    public enum Condition {
        OVERDUE,
        NORMAL, // Sách trong tình trạng bình thường
        DAMAGED // Sách bị hư hỏng
    }
    public enum PaymentStatus {
        NON_PAYMENT,
        UNPAID,
        CASH,
        VNPAY
    }
}
