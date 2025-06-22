package com.spkt.libraSys.service.user;

import com.spkt.libraSys.service.role.RoleEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", nullable = false, unique = true)
    String userId;

    @Column(name = "username", nullable = false, unique = true)
    String username;

    @Column(name = "password", nullable = false)
    String password;

    @Column(name = "first_name")
    String firstName;

    @Column(name = "last_name")
    String lastName;

    @Column(name = "dob")
    LocalDate dob;  // Ngày sinh

    @Column(name = "phone_number")
    String phoneNumber;

    @Column(name = "address")
    String address;

    @Column(name = "registration_date")
    LocalDate registrationDate = LocalDate.now(); // Mặc định là ngày đăng ký

    @Column(name = "expiration_date")
    LocalDate expirationDate;

    @Column(name = "current_borrowed_count", nullable = false)
    @Builder.Default
    int currentBorrowedCount = 0; // Số sách đang mượn

    @Column(name = "max_borrow_limit", nullable = false)
    @Builder.Default
    int maxBorrowLimit = 5; // Giới hạn mượn sách

    @Column(name = "locked_at")
    LocalDateTime lockedAt; // Ngày tài khoản bị khóa
//
    @Column(name = "lock_reason")
    String lockReason; // Lý do tài khoản bị khóa

    @Column(name = "student_batch")
    int studentBatch;
    @Column(name = "major_code")
    String majorCode;


    @Enumerated(EnumType.STRING)
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    UserStatus isActive = UserStatus.PENDING;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roleEntities = new HashSet<>();

    /**
     * Kiểm tra xem người dùng có thể mượn thêm sách không.
     *
     * @return true nếu người dùng chưa đạt giới hạn mượn sách.
     */
    public boolean canBorrowMoreBooks() {
        return currentBorrowedCount < maxBorrowLimit;
    }


    /**
     * Tăng số lượng sách mượn lên 1.
     */
    public void borrowBook() {
        if (canBorrowMoreBooks()) {
            currentBorrowedCount++;
        } else {
            throw new IllegalStateException("Bạn đã đạt giới hạn mượn sách.");
        }
    }

    /**
     * Giảm số lượng sách mượn xuống 1 khi người dùng trả sách.
     */
    public void returnBook() {
        if (currentBorrowedCount > 0) {
            currentBorrowedCount--;
        }
    }
}
