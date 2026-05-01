package com.example.demomcpai.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FinancialStatementPdfGenerator {

    // Column X boundaries (in PDF points) — tune to your layout
    static final float DESC_COL_X_MIN = 100f;
    static final float DESC_COL_X_MAX = 380f;

    // ─── Transaction Model ────────────────────────────────────────────────────

    static class Transaction {
        int id;
        String description;
        double amount;

        Transaction(int id, String description, double amount) {
            this.id = id;
            this.description = description;
            this.amount = amount;
        }
    }

    // ─── Sample Data ──────────────────────────────────────────────────────────

    static List<Transaction> getSampleTransactions() {
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(1,  "Opening Balance",        10000.00));
        list.add(new Transaction(2,  "Salary Credit",           5000.00));
        list.add(new Transaction(3,  "Rent Payment",           -1500.00));
        list.add(new Transaction(4,  "Grocery Shopping",        -250.75));
        list.add(new Transaction(5,  "Electricity Bill",        -120.50));
        list.add(new Transaction(6,  "Freelance Income",        2000.00));
        list.add(new Transaction(7,  "Internet Subscription",    -49.99));
        list.add(new Transaction(8,  "Dining Out",               -85.30));
        list.add(new Transaction(9,  "Insurance Premium",       -300.00));
        list.add(new Transaction(10, "Investment Dividend",      450.00));
        return list;
    }

    // ─── PDF Generation ───────────────────────────────────────────────────────

    public static void generatePDF(String outputPath) throws IOException {

        List<Transaction> transactions = getSampleTransactions();

        // Colors
        Color headerBg    = new Color(31, 73, 125);   // Dark blue
        Color subHeaderBg = new Color(68, 114, 196);  // Medium blue
        Color rowEven     = new Color(235, 241, 255);  // Light blue
        Color rowOdd      = Color.WHITE;
        Color creditGreen = new Color(0, 128, 0);
        Color debitRed    = new Color(200, 0, 0);
        Color summaryBg   = new Color(214, 220, 240);

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont fontBold    = PDType1Font.HELVETICA_BOLD;
            PDFont fontRegular = PDType1Font.HELVETICA;
            PDFont fontOblique = PDType1Font.HELVETICA_OBLIQUE;

            float pageWidth  = PDRectangle.A4.getWidth();   // 595
            float pageHeight = PDRectangle.A4.getHeight();  // 842
            float margin     = 45f;
            float tableWidth = pageWidth - 2 * margin;
            float y          = pageHeight - margin;

            // Column X positions and widths
            float colIdX    = margin;
            float colDescX  = margin + 55;
            float colAmtX   = margin + tableWidth - 95;
            float colIdW    = 50;
            float colDescW  = tableWidth - 55 - 95;
            float colAmtW   = 90;
            float rowHeight = 22f;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // ── Banner Header ────────────────────────────────────────────
                drawRect(cs, margin, y - 50, tableWidth, 50, headerBg);
                drawCenteredText(cs, fontBold, 18, Color.WHITE,
                        "FINANCIAL STATEMENT", pageWidth, y - 20);
                String date = "Generated: " +
                        LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
                drawCenteredText(cs, fontOblique, 9, new Color(200, 220, 255),
                        date, pageWidth, y - 37);
                y -= 60;

                // ── Account Info Row ─────────────────────────────────────────
                drawRect(cs, margin, y - 25, tableWidth, 25, subHeaderBg);
                drawText(cs, fontBold, 9, Color.WHITE,
                        "Account Holder: John Doe", margin + 8, y - 16);
                drawText(cs, fontBold, 9, Color.WHITE,
                        "Account No: XXXX-XXXX-4321", margin + 200, y - 16);
                drawText(cs, fontBold, 9, Color.WHITE,
                        "Period: Jan 2025 – Dec 2025", margin + 380, y - 16);
                y -= 32;

                // ── Table Column Headers ──────────────────────────────────────
                drawRect(cs, margin, y - rowHeight, tableWidth, rowHeight, subHeaderBg);
                drawText(cs, fontBold, 10, Color.WHITE, "TXN ID",
                        colIdX + 8, y - 15);
                drawText(cs, fontBold, 10, Color.WHITE, "Description",
                        colDescX + 8, y - 15);
                drawRightAlignedText(cs, fontBold, 10, Color.WHITE, "Amount (USD)",
                        colAmtX + colAmtW - 8, y - 15);
                y -= rowHeight + 2;

                // ── Transaction Rows ─────────────────────────────────────────
                double totalCredits = 0, totalDebits = 0;
                int rowIndex = 0;

                for (Transaction t : transactions) {
                    Color rowBg = (rowIndex % 2 == 0) ? rowEven : rowOdd;
                    drawRect(cs, margin, y - rowHeight, tableWidth, rowHeight, rowBg);

                    // Vertical dividers
                    drawLine(cs, colDescX, y, colDescX, y - rowHeight, new Color(180, 190, 220), 0.5f);
                    drawLine(cs, colAmtX,  y, colAmtX,  y - rowHeight, new Color(180, 190, 220), 0.5f);

                    // ID
                    drawText(cs, fontBold, 9, new Color(60, 60, 60),
                            String.format("%03d", t.id), colIdX + 8, y - 14);

                    // Description
                    drawText(cs, fontRegular, 9, new Color(40, 40, 40),
                            t.description, colDescX + 8, y - 14);

                    // Amount (color-coded)
                    String amtStr = String.format("%,.2f", Math.abs(t.amount));
                    Color  amtColor;
                    String amtLabel;
                    if (t.amount >= 0) {
                        amtColor = creditGreen;
                        amtLabel = "+ " + amtStr;
                        totalCredits += t.amount;
                    } else {
                        amtColor = debitRed;
                        amtLabel = "- " + amtStr;
                        totalDebits += t.amount;
                    }
                    drawRightAlignedText(cs, fontBold, 9, amtColor,
                            amtLabel, colAmtX + colAmtW - 8, y - 14);

                    // Row bottom border
                    drawLine(cs, margin, y - rowHeight, margin + tableWidth,
                            y - rowHeight, new Color(200, 210, 230), 0.5f);

                    y -= rowHeight;
                    rowIndex++;
                }

                // ── Summary Section ───────────────────────────────────────────
                y -= 10;
                float summaryH = 28f;

                // Total Credits
                drawRect(cs, margin, y - summaryH, tableWidth, summaryH, summaryBg);
                drawText(cs, fontBold, 10, new Color(40, 40, 80),
                        "Total Credits", colDescX + 8, y - 18);
                drawRightAlignedText(cs, fontBold, 10, creditGreen,
                        String.format("+ %,.2f", totalCredits),
                        colAmtX + colAmtW - 8, y - 18);
                y -= summaryH + 2;

                // Total Debits
                drawRect(cs, margin, y - summaryH, tableWidth, summaryH, summaryBg);
                drawText(cs, fontBold, 10, new Color(40, 40, 80),
                        "Total Debits", colDescX + 8, y - 18);
                drawRightAlignedText(cs, fontBold, 10, debitRed,
                        String.format("- %,.2f", Math.abs(totalDebits)),
                        colAmtX + colAmtW - 8, y - 18);
                y -= summaryH + 4;

                // Net Balance
                double netBalance = totalCredits + totalDebits;
                Color  netBg    = new Color(31, 73, 125);
                Color  netColor = netBalance >= 0 ? new Color(100, 255, 100)
                        : new Color(255, 120, 120);
                drawRect(cs, margin, y - 32, tableWidth, 32, netBg);
                drawText(cs, fontBold, 12, Color.WHITE,
                        "NET BALANCE", colDescX + 8, y - 20);
                drawRightAlignedText(cs, fontBold, 12, netColor,
                        String.format("USD %,.2f", netBalance),
                        colAmtX + colAmtW - 8, y - 20);
                y -= 42;

                // ── Legend ────────────────────────────────────────────────────
                y -= 15;
                drawRect(cs, margin, y - 20, 12, 12, creditGreen);
                drawText(cs, fontRegular, 8, new Color(60, 60, 60), "Credit (Inflow)",
                        margin + 16, y - 11);
                drawRect(cs, margin + 110, y - 20, 12, 12, debitRed);
                drawText(cs, fontRegular, 8, new Color(60, 60, 60), "Debit (Outflow)",
                        margin + 126, y - 11);

                // ── Footer ────────────────────────────────────────────────────
                drawLine(cs, margin, 50, margin + tableWidth, 50, new Color(150, 160, 180), 0.8f);
                drawCenteredText(cs, fontOblique, 8, new Color(130, 130, 130),
                        "This is a system-generated statement. For queries, contact support@bank.com",
                        pageWidth, 38);
            }

            doc.save(outputPath);
            System.out.println("✅ PDF saved to: " + outputPath);
        }
    }

    // ─── Drawing Helpers ──────────────────────────────────────────────────────

    static void drawRect(PDPageContentStream cs, float x, float y,
                         float w, float h, Color color) throws IOException {
        cs.setNonStrokingColor(color);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    static void drawLine(PDPageContentStream cs, float x1, float y1,
                         float x2, float y2, Color color, float width) throws IOException {
        cs.setStrokingColor(color);
        cs.setLineWidth(width);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    static void drawText(PDPageContentStream cs, PDFont font, float size,
                         Color color, String text, float x, float y) throws IOException {
        cs.setNonStrokingColor(color);
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    static void drawRightAlignedText(PDPageContentStream cs, PDFont font, float size,
                                     Color color, String text, float rightX, float y)
            throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        drawText(cs, font, size, color, text, rightX - textWidth, y);
    }

    static void drawCenteredText(PDPageContentStream cs, PDFont font, float size,
                                 Color color, String text, float pageWidth, float y)
            throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * size;
        drawText(cs, font, size, color, text, (pageWidth - textWidth) / 2, y);
    }

    // ─── Extract descriptions from PDF ───────────────────────────────────────────────────
    public static List<String> extractDescriptions(MultipartFile file) throws IOException {

        try (PDDocument doc = PDDocument.load(file.getInputStream())) {

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);

            // A4 page = 595 x 842 pts
            // Description column starts at x=100, width=~280, rows from y=190 to y=420
            // Adjust these bounds to match your PDF layout
            Rectangle descriptionRegion = new Rectangle(
                    100,  // x: start of Description column
                    190,  // y: top of first transaction row (from top of page)
                    280,  // width: span of Description column
                    230   // height: covering all transaction rows
            );

            stripper.addRegion("descriptions", descriptionRegion);
            stripper.extractRegions(doc.getPage(0));

            String regionText = stripper.getTextForRegion("descriptions");
            System.out.println("=== Region Text ===\n" + regionText);

            // Split by newline, trim, filter blanks
            return Arrays.stream(regionText.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());
        }
    }
}