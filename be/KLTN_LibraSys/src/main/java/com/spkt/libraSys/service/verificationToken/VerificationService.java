package com.spkt.libraSys.service.verificationToken;

import com.spkt.libraSys.service.access.VerificationResponse;

/**
 * The VerificationService interface defines behaviors related to user account verification.
 * Methods include account verification, resending verification codes, and creating new verification codes.
 */
public interface VerificationService {

   /**
    * Verifies a user account through a verification code.
    *
    * @param request Contains verification request information, including email and verification code.
    * @return true if the account is successfully verified, false if verification fails.
    */
   VerificationResponse verifyAccount(String request);

   /**
    * Resends the verification code to the registered email.
    *
    * @param email The email address of the user requesting the verification code resend.
    * @return true if the verification code is successfully resent, false if there is an error during sending.
    */
   boolean resendVerificationCode(String email);

   /**
    * Sends a verification code to the user's email address.
    *
    * @param email The email address of the user who needs to receive the verification code.
    */
   void verificationCode(String email);

   /**
    * Requests to send a password reset code to the user's email.
    *
    * @param email The email of the user requesting password reset.
    */
   void requestPasswordReset(String email);

   /**
    * Verifies the password reset code and updates the new password.
    *
    * @param resetPasswordRequest The password reset request containing the token and new password.
    * @return true if reset is successful, false if the code is invalid or expired.
    */
   boolean resetPassword(ResetPasswordRequest resetPasswordRequest);
}