package com.spkt.libraSys.service.document.briefDocs;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

@Service
public class TextProcessingService {

    // Extract text from PDF file
    public String extractTextFromPDF(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    // Extract text from DOCX file
    public String extractTextFromDocx(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                sb.append(para.getText()).append("\n");
            }
        }
        return sb.toString();
    }

    // Clean text
    public String cleanText(String rawText) {
        return rawText.replaceAll("\\s+", " ").trim();  // Remove extra whitespace
    }

    // Split text into smaller sections
    public String[] splitTextBySections(String text) {
        return text.split("(?<=\\n)(?=\\p{Upper})");  // Split by paragraphs
    }

    // Estimate token count for text
    public int estimateTokenCount(String text) {
        return text.length() / 4;  // 1 token ≈ 4 characters
    }
}
