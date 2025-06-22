package com.spkt.libraSys.service.payment.vnpay;


import com.spkt.libraSys.exception.AppException;
import com.spkt.libraSys.exception.ErrorCode;
import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.loan.LoanEntity;
import com.spkt.libraSys.service.loan.LoanRepository;
import com.spkt.libraSys.service.user.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VNPayService {
    @Autowired
    LoanRepository loanRepository;
    @Autowired
    VnPayConfig vnp_config;
    @Autowired
    AuthService authService;

    public String createOrder(Long loanTransactionId) {
        UserEntity currentUser = authService.getCurrentUser();
        LoanEntity loanTransaction = loanRepository.findById(loanTransactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if(!currentUser.getUserId().equals(loanTransaction.getUserEntity().getUserId())){
            throw new AppException(ErrorCode.UNAUTHORIZED,"Ban khong co quyen");
        }

        double fineAmount = loanTransaction.getFineAmount();
        if(fineAmount <= 0){
            throw new AppException(ErrorCode.FINE_NOT_FOUND,"Giao dịch không có khoản phạt");
        }
        if(loanTransaction.getPaymentStatus() != LoanEntity.PaymentStatus.UNPAID){
            throw new AppException(ErrorCode.FINE_NOT_FOUND,"Giao dịch da được thanh toán");
        }

        int total = (int) (fineAmount * 100);
        String orderInfor = "Thanh Toan: " + loanTransaction.getReturnCondition().toString();


        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = Config.getRandomNumber(8);
        String vnp_IpAddr = "127.0.0.1";
        String vnp_TmnCode = vnp_config.getVnp_TmnCode();
        String orderType = "order-type";

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(total));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfor);
        vnp_Params.put("vnp_OrderType", orderType);

        String locate = "vn";
        vnp_Params.put("vnp_Locale", locate);

        String urlReturn = vnp_config.getVnp_ReturnUrl();
        vnp_Params.put("vnp_ReturnUrl", urlReturn);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    //Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = Config.hmacSHA512(vnp_config.getSecretKey(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnp_config.getVnp_PayUrl() + "?" + queryUrl;
        loanTransaction.setVnpTxnRef(vnp_TxnRef);
        loanRepository.save(loanTransaction);
        return paymentUrl;
    }

    public int orderReturn(HttpServletRequest request) {
        Map fields = new HashMap();
        for (Enumeration params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = null;
            String fieldValue = null;
            try {
                fieldName = URLEncoder.encode((String) params.nextElement(), StandardCharsets.US_ASCII.toString());
                fieldValue = URLEncoder.encode(request.getParameter(fieldName), StandardCharsets.US_ASCII.toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }
        String signValue = Config.hashAllFields(fields, vnp_config.getSecretKey());
        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_TransactionStatus"))) {
                // xac dinh status Fine
                // Tìm kiếm giao dịch theo vnp_TxnRef
                String vnp_TxnRef = request.getParameter("vnp_TxnRef");
                LoanEntity loanTransaction = loanRepository.findByVnpTxnRef(vnp_TxnRef)
                        .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND, "LoanTransaction not found"));
                // Lưu thông tin về phương thức thanh toán và mã giao dịch VNPay
                if(loanTransaction.getFineAmount() <= 0){
                    throw new AppException(ErrorCode.FINE_NOT_FOUND,"Giao dịch không có khoản phạt");
                }
                if(loanTransaction.getPaymentStatus() != LoanEntity.PaymentStatus.UNPAID){
                    throw new AppException(ErrorCode.FINE_NOT_FOUND,"Giao dịch da được thanh toán");
                }
                loanTransaction.setPaidAt(LocalDateTime.now());
                loanTransaction.setPaymentStatus(LoanEntity.PaymentStatus.VNPAY);
                loanRepository.save(loanTransaction);
                return 1;

            } else {
                return -1;
            }
        }
        return 0;
    }
}