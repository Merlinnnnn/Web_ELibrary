package com.spkt.libraSys.service.helper;

import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.favorite.FavoriteDocumentRepository;
import com.spkt.libraSys.service.loan.LoanRepository;
import com.spkt.libraSys.service.loan.LoanService;
import com.spkt.libraSys.service.loan.LoanStatus;
import com.spkt.libraSys.service.role.RoleService;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.user.UserRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class HelperServiceImpl implements HelperService {
    private final RoleService roleService;
    private final LoanService loanService;
    private final LoanRepository loanRepository;
    private final FavoriteDocumentRepository favoriteDocumentRepository;
    private UserRepository userRepository;
    private AuthService authService;


    @Override
    public Map<String, Object> getStaticDocForUser(String userId) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        UserEntity userCurr = authService.getCurrentUser();

        boolean isAdminRole = roleService.isAdmin(userCurr);

        if(!userEntity.getUserId().equals(userCurr.getUserId()) && !isAdminRole) {
            throw new AppException(ErrorCode.FORBIDDEN,"Ban khong co  quyen truy cap tai nguyen nay");
        }

        Map<String, Object> doc = new HashMap<>();

        long borrowedCurr =  userCurr.getCurrentBorrowedCount();
        long borrowedTotal  = loanRepository.countByUserEntity_UserIdAndStatusNotIn(
                userId, List.of(LoanStatus.CANCELLED_AUTO,LoanStatus.RESERVED)
        );
        long favorTotal = favoriteDocumentRepository.countByUser(userEntity);

        doc.put("borrowedCurr", borrowedCurr);
        doc.put("borrowedTotal", borrowedTotal);
        doc.put("favorTotal", favorTotal);

        return doc;
    }
}
