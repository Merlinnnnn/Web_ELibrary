package com.spkt.libraSys.service.payment.vnpay;

import com.spkt.libraSys.exception.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller để xử lý các yêu cầu liên quan đến thanh toán với VNPay.
 * Bao gồm các hành động như gửi đơn hàng và xử lý kết quả trả về từ VNPay.
 */
@RestController
@RequestMapping("/api/v1/vnpay")
public class Controller {

    @Autowired
    private VNPayService vnPayService;
    @Autowired
    private VnPayConfig vnPayConfig;

    /**
     * Gửi đơn hàng đến VNPay để tạo đơn thanh toán.
     * Phương thức này sẽ tạo ra URL để chuyển hướng người dùng đến trang thanh toán của VNPay.
     *
     * @param loanId ID giao dịch khoản vay.
     * @return Phản hồi ApiResponse chứa URL thanh toán của VNPay.
     */
    @GetMapping("/submitOrder/{loanId}")
    public ResponseEntity<Map<String, String>> submitOrder(@PathVariable Long loanId,HttpServletResponse response) throws IOException {
        // Tạo đơn hàng và lấy URL thanh toán từ VNPay
        String vnpayUrl = vnPayService.createOrder(loanId);
        Map<String, String> response1 = new HashMap<>();
        response1.put("redirectUrl", vnpayUrl);
        return ResponseEntity.ok(response1);
    }

    /**
     * Xử lý kết quả trả về từ VNPay sau khi người dùng hoàn thành thanh toán.
     * Phương thức này nhận kết quả thanh toán và trả về thông báo thành công hoặc thất bại.
     *
     * @param request Đối tượng HttpServletRequest chứa các tham số trả về từ VNPay.
     * @return Phản hồi ApiResponse với thông báo kết quả thanh toán (thành công hoặc thất bại).
     */
    @GetMapping("/return-payment")
    public void returnPayment(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Kiểm tra trạng thái thanh toán của đơn hàng
        int paymentStatus = vnPayService.orderReturn(request);

        // Xác định thông điệp dựa trên trạng thái thanh toán
        String message = paymentStatus == 1 ? "ordersuccess" : "orderfail";
        String url_Fe = vnPayConfig.getVnp_ReturnUr_Fe()+"?status=" + (paymentStatus == 1 ? "success" : "fail");
        response.sendRedirect(url_Fe);
    }
}