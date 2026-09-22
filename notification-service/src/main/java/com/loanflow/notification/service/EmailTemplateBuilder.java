package com.loanflow.notification.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class EmailTemplateBuilder {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String BRAND = "#1a5276";

    public String loanApproved(String fullName, String loanId,
                               BigDecimal amount, BigDecimal emiAmount,
                               BigDecimal rate, Integer tenure,
                               LocalDate disbursementDate) {
        return html("Your Loan is Approved! 🎉", String.format("""
            <h2 style="color:%s">Congratulations, %s!</h2>
            <p>Your loan <strong>%s</strong> has been
               <span style="color:green">approved</span>.</p>
            <table style="border-collapse:collapse;width:100%%">
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Approved Amount</b></td>
                  <td style="padding:8px;border:1px solid #ddd">₹%s</td></tr>
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Interest Rate</b></td>
                  <td style="padding:8px;border:1px solid #ddd">%s%% p.a.</td></tr>
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Tenure</b></td>
                  <td style="padding:8px;border:1px solid #ddd">%d months</td></tr>
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Monthly EMI</b></td>
                  <td style="padding:8px;border:1px solid #ddd">₹%s</td></tr>
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Disbursement Date</b></td>
                  <td style="padding:8px;border:1px solid #ddd">%s</td></tr>
            </table>
            """, BRAND, fullName, loanId,
                fmt(amount), rate, tenure,
                fmt(emiAmount), disbursementDate.format(DATE_FMT)));
    }

    public String loanRejected(String fullName, String loanId,
                               List<String> reasons) {
        String list = reasons.stream()
                .map(r -> "<li>" + r + "</li>")
                .reduce("", String::concat);
        return html("Loan Application Update", String.format("""
            <h2 style="color:%s">Dear %s,</h2>
            <p>We regret that loan <strong>%s</strong> could not be approved.</p>
            <p><b>Reasons:</b></p><ul>%s</ul>
            <p>You may re-apply after 90 days.</p>
            """, BRAND, fullName, loanId, list));
    }

    public String emiReminder(String fullName, String loanId,
                              BigDecimal emiAmount, LocalDate dueDate,
                              int daysLeft, BigDecimal outstanding) {
        return html("EMI Due Reminder", String.format("""
            <h2 style="color:%s">Hi %s,</h2>
            <p>Your EMI is due in <strong>%d day(s)</strong>.</p>
            <table style="border-collapse:collapse;width:100%%">
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Loan ID</b></td>
                  <td style="padding:8px;border:1px solid #ddd">%s</td></tr>
              <tr><td style="padding:8px;border:1px solid #ddd"><b>EMI Amount</b></td>
                  <td style="padding:8px;border:1px solid #ddd">₹%s</td></tr>
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Due Date</b></td>
                  <td style="padding:8px;border:1px solid #ddd">%s</td></tr>
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Outstanding</b></td>
                  <td style="padding:8px;border:1px solid #ddd">₹%s</td></tr>
            </table>
            <p style="color:orange">Please ensure sufficient funds to avoid late charges.</p>
            """, BRAND, fullName, daysLeft, loanId,
                fmt(emiAmount), dueDate.format(DATE_FMT), fmt(outstanding)));
    }

    public String paymentReceived(String fullName, String loanId,
                                  BigDecimal amount,
                                  BigDecimal outstanding) {
        return html("Payment Received ✅", String.format("""
            <h2 style="color:%s">Hi %s,</h2>
            <p>We received your payment of <strong>₹%s</strong>
               for loan <strong>%s</strong>.</p>
            <p>Outstanding balance: <strong>₹%s</strong></p>
            <p>Thank you for your timely payment!</p>
            """, BRAND, fullName, fmt(amount), loanId, fmt(outstanding)));
    }

    public String paymentOverdue(String fullName, String loanId,
                                 BigDecimal overdueAmount,
                                 int overdueDays, BigDecimal penalty) {
        return html("⚠️ Overdue Payment Alert", String.format("""
            <h2 style="color:red">Urgent: Overdue — %s</h2>
            <p>Dear %s, your EMI is
               <strong style="color:red">%d days overdue</strong>.</p>
            <table style="border-collapse:collapse;width:100%%">
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Overdue Amount</b></td>
                  <td style="padding:8px;border:1px solid #ddd">₹%s</td></tr>
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Late Penalty</b></td>
                  <td style="padding:8px;border:1px solid #ddd">₹%s</td></tr>
              <tr><td style="padding:8px;border:1px solid #ddd"><b>Total Due</b></td>
                  <td style="padding:8px;border:1px solid #ddd">₹%s</td></tr>
            </table>
            <p style="color:red">Continued non-payment may affect your credit score.</p>
            """, loanId, fullName, overdueDays,
                fmt(overdueAmount), fmt(penalty),
                fmt(overdueAmount.add(penalty))));
    }

    public String kycResult(String fullName, boolean approved, String reason) {
        String color  = approved ? "green" : "red";
        String status = approved ? "Verified ✅" : "Rejected ❌";
        String extra  = approved
                ? "<p>You can now apply for a loan.</p>"
                : "<p>Reason: " + reason + "</p><p>Please re-submit after addressing the issue.</p>";
        return html("KYC Status Update", String.format("""
            <h2 style="color:%s">Dear %s,</h2>
            <p>Your KYC status:
               <strong style="color:%s">%s</strong></p>%s
            """, BRAND, fullName, color, status, extra));
    }

    private String html(String title, String content) {
        return """
            <!DOCTYPE html><html><head><meta charset="UTF-8">
            <style>
              body{font-family:Arial,sans-serif;color:#333;
                   max-width:600px;margin:auto;padding:20px}
              .footer{margin-top:32px;font-size:12px;color:#999;
                      border-top:1px solid #eee;padding-top:16px}
            </style></head><body>
            """ + content + """
            <div class="footer">
              Automated message from LoanFlow. Do not reply.<br>
              © 2024 LoanFlow Financial Services Pvt. Ltd.
            </div></body></html>
            """;
    }

    private String fmt(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}