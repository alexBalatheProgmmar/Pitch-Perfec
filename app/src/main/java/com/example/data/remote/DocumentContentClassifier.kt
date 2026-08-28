package com.example.data.remote

import com.example.data.model.AIAnalysisResult
import com.example.data.model.Actionability
import com.example.data.model.ContentType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemPriority
import com.example.data.model.ItemType
import com.example.util.ExtractedPdfData
import java.util.Locale
import java.util.regex.Pattern

object DocumentContentClassifier {

    /**
     * Classifies a document using whole-document structure, headings, page evidence, and extracted data.
     */
    fun classifyAndExtract(pdfData: ExtractedPdfData): AIAnalysisResult {
        val text = pdfData.fullText.trim()
        val lower = text.lowercase(Locale.ROOT)
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val firstLine = lines.firstOrNull() ?: pdfData.fileName

        // Check if text is completely empty or unusable
        if (text.isBlank() || text.count { it.isLetterOrDigit() } < 15) {
            return buildUnknownResult(
                pdfData = pdfData,
                reason = "Document content could not be read or extracted clearly.",
                explanation = "The document appears empty or unreadable."
            )
        }

        val extractedCurrency = extractCurrency(text)
        val extractedAmounts = extractAllAmounts(text)
        val extractedDate = extractDate(text)
        val extractedDueDate = extractDueDate(text) ?: extractedDate
        val extractedTime = extractTime(text)

        // 1. INVOICE (Money owed / Payment requested)
        if (isInvoiceDocument(lower, text)) {
            val invNum = extractInvoiceNumber(text, lower)
            val customer = extractCustomer(text, lines)
            val provider = extractInvoiceProvider(text, lines)
            val amountDue = extractTotalAmountDue(text, lower) ?: extractedAmounts.maxOrNull()
            val subtotal = extractSubtotal(text, lower)
            val tax = extractTax(text, lower)
            val isPaid = lower.contains("status: paid") || lower.contains("paid in full") || lower.contains("payment received")

            val title = if (provider != null) "$provider Invoice" else if (invNum != null) "Invoice $invNum" else "Commercial Invoice"
            val actionText = if (!isPaid && amountDue != null) "Pay invoice ${extractedCurrency ?: "৳"}${amountDue.toInt()}" else if (!isPaid) "Review and pay invoice" else ""

            val evidenceLine = lines.firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("invoice") || ll.contains("total due") || ll.contains("amount due")
            } ?: lines.take(3).joinToString(" ")

            return AIAnalysisResult(
                contentType = ContentType.INVOICE.name,
                actionability = if (isPaid) Actionability.INFORMATIONAL.name else Actionability.ACTIONABLE.name,
                contentTypeConfidence = 0.96f,
                actionabilityConfidence = 0.95f,
                extractionConfidence = 0.94f,
                title = title,
                summary = "Invoice for $title with total due ${extractedCurrency ?: "৳"}${amountDue?.toInt() ?: ""}" + if (extractedDueDate != null) " by $extractedDueDate" else "",
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.FINANCE.name,
                action = actionText,
                date = extractedDueDate,
                time = extractedTime,
                amount = amountDue,
                amountDue = amountDue,
                currency = extractedCurrency,
                invoiceNumber = invNum,
                customer = customer,
                organization = provider,
                subtotal = subtotal,
                tax = tax,
                paymentStatus = if (isPaid) "PAID" else "UNPAID",
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                sourcePageEvidence = "Page 1: \"$evidenceLine\"",
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = if (isPaid) ItemPriority.LOW.name else ItemPriority.HIGH.name,
                confidence = 0.95f,
                evidence = evidenceLine,
                reason = "Document contains invoice identifiers, bill-to metadata, and amount due.",
                explanation = "Identified as Invoice from $provider.",
                isActionable = !isPaid,
                isUncertain = false
            )
        }

        // 2. UTILITY BILL (Electricity, Gas, Water, Internet, Mobile, Rent)
        if (isUtilityBillDocument(lower, text)) {
            val detectedBillType = detectBillType(lower)
            val provider = extractBillProvider(text, lines)
            val amountDue = extractTotalAmountDue(text, lower) ?: extractedAmounts.maxOrNull()
            val isPaid = lower.contains("status: paid") || lower.contains("paid on") || lower.contains("payment successful")

            val title = if (provider != null) "$provider ${detectedBillType.displayName}" else detectedBillType.displayName
            val actionText = if (!isPaid && amountDue != null) "Pay $title ${extractedCurrency ?: "৳"}${amountDue.toInt()}" else if (!isPaid) "Pay $title" else ""

            val evidenceLine = lines.firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("bill") || ll.contains("electricity") || ll.contains("total amount due") || ll.contains("meter")
            } ?: lines.take(3).joinToString(" ")

            return AIAnalysisResult(
                contentType = ContentType.BILL.name,
                actionability = if (isPaid) Actionability.INFORMATIONAL.name else Actionability.ACTIONABLE.name,
                contentTypeConfidence = 0.96f,
                actionabilityConfidence = 0.95f,
                extractionConfidence = 0.93f,
                title = title,
                summary = "${detectedBillType.displayName} of ${extractedCurrency ?: "৳"}${amountDue?.toInt() ?: ""}" + if (extractedDueDate != null) " due $extractedDueDate" else "",
                description = text,
                type = ItemType.PAYMENT.name,
                category = ItemCategory.FINANCE.name,
                action = actionText,
                date = extractedDueDate,
                time = extractedTime,
                amount = amountDue,
                amountDue = amountDue,
                currency = extractedCurrency,
                billType = detectedBillType.name,
                billProvider = provider,
                organization = provider,
                paymentStatus = if (isPaid) "PAID" else "UNPAID",
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                sourcePageEvidence = "Page 1: \"$evidenceLine\"",
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = if (isPaid) ItemPriority.LOW.name else ItemPriority.HIGH.name,
                confidence = 0.95f,
                evidence = evidenceLine,
                reason = "Detected utility bill payment obligation.",
                explanation = "Found ${detectedBillType.displayName} from ${provider ?: "provider"}.",
                isActionable = !isPaid,
                isUncertain = false
            )
        }

        // 3. STORE PURCHASE RECEIPT (Paid checkout slip)
        if (isReceiptDocument(lower, text)) {
            val merchant = extractMerchant(text, lines)
            val productName = extractProductName(text, lower)
            val totalPaid = extractReceiptTotal(text, lower) ?: extractedAmounts.maxOrNull()
            val title = if (merchant != null) "$merchant Purchase" else if (productName != null) "$productName Receipt" else "Purchase Receipt"

            val evidenceLine = lines.firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("receipt") || ll.contains("total") || ll.contains("paid") || ll.contains("cashier")
            } ?: lines.take(3).joinToString(" ")

            return AIAnalysisResult(
                contentType = ContentType.RECEIPT.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.94f,
                actionabilityConfidence = 0.95f,
                extractionConfidence = 0.90f,
                title = title,
                summary = "Purchase receipt from ${merchant ?: "Store"} for ${extractedCurrency ?: "৳"}${totalPaid?.toInt() ?: ""}",
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.SHOPPING.name,
                action = "",
                date = extractedDate,
                amount = totalPaid,
                amountPaid = totalPaid,
                currency = extractedCurrency,
                merchant = merchant,
                product = productName,
                paymentStatus = "PAID",
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                sourcePageEvidence = "Page 1: \"$evidenceLine\"",
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.LOW.name,
                confidence = 0.93f,
                evidence = evidenceLine,
                reason = "Document is a completed store purchase receipt record.",
                explanation = "Extracted purchase receipt from ${merchant ?: "store"}.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 4. EDUCATIONAL DOCUMENT / TEXTBOOK / STUDY MATERIAL
        if (isEducationalDocument(lower, text, pdfData.fileName)) {
            val (subject, topic, title) = extractEducationalTopic(text, lines, pdfData.fileName)
            val keyConcepts = extractKeyConcepts(text)

            val evidenceLine = lines.firstOrNull { l ->
                val ll = l.lowercase(Locale.ROOT)
                ll.contains("chapter") || ll.contains("modifier") || ll.contains("grammar") || ll.contains("lesson") || ll.contains("definition")
            } ?: lines.take(2).joinToString(" ")

            return AIAnalysisResult(
                contentType = ContentType.EDUCATIONAL_DOCUMENT.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.95f,
                actionabilityConfidence = 0.98f,
                extractionConfidence = 0.92f,
                title = title,
                summary = "$subject study material covering $topic",
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.EDUCATION.name,
                action = "",
                subject = subject,
                topic = topic,
                keyConcepts = keyConcepts,
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                sourcePageEvidence = "Page 1: \"$evidenceLine\"",
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.LOW.name,
                confidence = 0.95f,
                evidence = evidenceLine,
                reason = "Educational material containing curriculum, grammar rules, concepts, or academic chapters.",
                explanation = "Classified as $subject educational document on $topic.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 5. RESEARCH PAPER / ACADEMIC ARTICLE
        if (isResearchPaper(lower, text)) {
            val title = extractResearchTitle(text, lines)
            val authors = extractAuthors(text, lines)
            val abstractSnippet = extractAbstract(text)
            val keyFindings = extractKeyFindings(text)

            return AIAnalysisResult(
                contentType = ContentType.RESEARCH_PAPER.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.95f,
                actionabilityConfidence = 0.98f,
                extractionConfidence = 0.92f,
                title = title,
                summary = "Research paper by ${authors ?: "authors"}: ${abstractSnippet?.take(100) ?: title}",
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.EDUCATION.name,
                action = "",
                topic = title,
                authors = authors,
                abstractSnippet = abstractSnippet,
                keyFindings = keyFindings,
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                sourcePageEvidence = "Page 1: \"${abstractSnippet?.take(80) ?: title}\"",
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.LOW.name,
                confidence = 0.94f,
                evidence = abstractSnippet?.take(120),
                reason = "Academic research paper with abstract, methodology, and research findings.",
                explanation = "Classified as research paper.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 6. CONTRACT / LEGAL AGREEMENT
        if (isContractDocument(lower, text)) {
            val title = extractContractTitle(text, lines)
            val parties = extractContractParties(text)
            val terms = extractContractTerms(text)

            return AIAnalysisResult(
                contentType = ContentType.CONTRACT.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.94f,
                actionabilityConfidence = 0.92f,
                extractionConfidence = 0.88f,
                title = title,
                summary = "Legal agreement and terms between parties",
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.DOCUMENTS.name,
                action = "",
                date = extractedDate,
                organization = parties,
                keyConcepts = terms,
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                sourcePageEvidence = "Page 1: \"${lines.take(2).joinToString(" ")}\"",
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.MEDIUM.name,
                confidence = 0.92f,
                evidence = lines.take(3).joinToString(" "),
                reason = "Legal contract or agreement with terms and binding obligations.",
                explanation = "Classified as legal contract.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 7. CERTIFICATE / DIPLOMA
        if (isCertificateDocument(lower, text)) {
            val recipient = extractCertificateRecipient(text, lines)
            val issuer = extractCertificateIssuer(text, lines)
            val title = if (recipient != null) "Certificate for $recipient" else "Certificate of Achievement"

            return AIAnalysisResult(
                contentType = ContentType.CERTIFICATE.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.95f,
                actionabilityConfidence = 0.98f,
                extractionConfidence = 0.90f,
                title = title,
                summary = "Awarded to ${recipient ?: "recipient"} by ${issuer ?: "organization"}",
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.DOCUMENTS.name,
                action = "",
                date = extractedDate,
                person = recipient,
                organization = issuer,
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.LOW.name,
                confidence = 0.94f,
                evidence = lines.take(2).joinToString(" "),
                reason = "Certificate document recognizing achievement or completion.",
                explanation = "Classified as certificate.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 8. RESUME / CV
        if (isResumeDocument(lower, text)) {
            val candidateName = extractResumeName(text, lines)
            val skills = extractResumeSkills(text)
            val title = if (candidateName != null) "Resume — $candidateName" else "Candidate Resume / CV"

            return AIAnalysisResult(
                contentType = ContentType.RESUME.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.95f,
                actionabilityConfidence = 0.98f,
                extractionConfidence = 0.90f,
                title = title,
                summary = "Professional CV and qualifications for ${candidateName ?: "candidate"}",
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.WORK.name,
                action = "",
                person = candidateName,
                keyConcepts = skills,
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.LOW.name,
                confidence = 0.94f,
                evidence = lines.take(2).joinToString(" "),
                reason = "Curriculum vitae detailing candidate experience and skills.",
                explanation = "Classified as resume.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 9. PRODUCT MANUAL / USER GUIDE
        if (isProductManual(lower, text)) {
            val productName = extractManualProductName(text, lines)
            val title = if (productName != null) "$productName User Manual" else "Product Manual & Instructions"

            return AIAnalysisResult(
                contentType = ContentType.PRODUCT_MANUAL.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.93f,
                actionabilityConfidence = 0.95f,
                extractionConfidence = 0.88f,
                title = title,
                summary = "User guide and operating instructions for ${productName ?: "product"}",
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.DOCUMENTS.name,
                action = "",
                product = productName,
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.LOW.name,
                confidence = 0.92f,
                evidence = lines.take(2).joinToString(" "),
                reason = "Product user manual containing operating instructions and specifications.",
                explanation = "Classified as product manual.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 10. NEWS ARTICLE
        if (isNewsArticle(lower, text)) {
            val headline = lines.firstOrNull { it.length in 15..90 } ?: lines.first()
            return AIAnalysisResult(
                contentType = ContentType.NEWS_ARTICLE.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.92f,
                actionabilityConfidence = 0.95f,
                extractionConfidence = 0.88f,
                title = headline.take(60),
                summary = text.take(120),
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.DOCUMENTS.name,
                action = "",
                date = extractedDate,
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.LOW.name,
                confidence = 0.90f,
                evidence = headline,
                reason = "Journalistic news reporting article.",
                explanation = "Classified as news article.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 11. BANK / FINANCIAL STATEMENT
        if (isBankStatement(lower, text)) {
            val bankName = extractBankName(text, lines)
            val title = if (bankName != null) "$bankName Account Statement" else "Bank Statement"

            return AIAnalysisResult(
                contentType = ContentType.BANK_STATEMENT.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.93f,
                actionabilityConfidence = 0.95f,
                extractionConfidence = 0.90f,
                title = title,
                summary = "Financial account statement and transaction summary",
                description = text,
                type = ItemType.DOCUMENT.name,
                category = ItemCategory.FINANCE.name,
                action = "",
                date = extractedDate,
                organization = bankName,
                currency = extractedCurrency,
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.LOW.name,
                confidence = 0.92f,
                evidence = lines.take(3).joinToString(" "),
                reason = "Financial bank statement showing account balance and transactions.",
                explanation = "Classified as bank statement.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 12. Genuinely General Reference Document
        if (isGeneralReferenceKnowledge(lower, text)) {
            return AIAnalysisResult(
                contentType = ContentType.GENERAL_INFORMATION.name,
                actionability = Actionability.INFORMATIONAL.name,
                contentTypeConfidence = 0.85f,
                actionabilityConfidence = 0.90f,
                extractionConfidence = 0.0f,
                title = firstLine.take(50),
                summary = text.take(120),
                description = text,
                type = ItemType.NOTE.name,
                category = ItemCategory.GENERAL.name,
                action = "",
                date = extractedDate,
                fileName = pdfData.fileName,
                fileSize = pdfData.fileSize,
                pageCount = pdfData.pageCount,
                ocrConfidence = pdfData.ocrConfidence,
                isScannedPdf = pdfData.isScannedPdf,
                contentHash = pdfData.contentHash,
                priority = ItemPriority.LOW.name,
                confidence = 0.85f,
                evidence = lines.take(2).joinToString(" "),
                reason = "General reference knowledge or encyclopedic documentation.",
                explanation = "Informational reference content.",
                isActionable = false,
                isUncertain = false
            )
        }

        // 13. Ambiguous / Unidentified Document (UNKNOWN)
        return buildUnknownResult(
            pdfData = pdfData,
            reason = "Document type could not be confidently identified from content.",
            explanation = "We could read some text, but could not determine the exact document type."
        )
    }

    private fun buildUnknownResult(
        pdfData: ExtractedPdfData,
        reason: String,
        explanation: String
    ): AIAnalysisResult {
        val lines = pdfData.fullText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val title = lines.firstOrNull()?.take(40) ?: pdfData.fileName.removeSuffix(".pdf")

        return AIAnalysisResult(
            contentType = ContentType.UNKNOWN.name,
            actionability = Actionability.UNCERTAIN.name,
            contentTypeConfidence = 0.35f,
            actionabilityConfidence = 0.50f,
            extractionConfidence = 0.0f,
            title = if (title.isBlank()) "Unidentified Document" else title,
            summary = pdfData.fullText.take(120),
            description = pdfData.fullText,
            type = ItemType.DOCUMENT.name,
            category = ItemCategory.GENERAL.name,
            action = "",
            fileName = pdfData.fileName,
            fileSize = pdfData.fileSize,
            pageCount = pdfData.pageCount,
            ocrConfidence = pdfData.ocrConfidence,
            isScannedPdf = pdfData.isScannedPdf,
            contentHash = pdfData.contentHash,
            priority = ItemPriority.LOW.name,
            confidence = 0.40f,
            evidence = lines.take(2).joinToString(" "),
            reason = reason,
            explanation = explanation,
            isActionable = false,
            isUncertain = true
        )
    }

    // --- Classification Condition Checkers ---

    private fun isInvoiceDocument(lower: String, text: String): Boolean {
        val hasInvoiceWord = lower.contains("invoice") || lower.contains("tax invoice") || lower.contains("bill to") || lower.contains("inv-") || lower.contains("inv #") || lower.contains("commercial invoice")
        val hasFinancialKeywords = lower.contains("subtotal") || lower.contains("amount due") || lower.contains("total due") || lower.contains("balance due") || lower.contains("due date") || lower.contains("itemized")
        val isNotUtilityBill = !lower.contains("meter reading") && !lower.contains("kwh") && !lower.contains("electricity board") && !lower.contains("desco") && !lower.contains("dpdc")
        return hasInvoiceWord && (hasFinancialKeywords || isNotUtilityBill)
    }

    private fun isUtilityBillDocument(lower: String, text: String): Boolean {
        val utilityKeywords = lower.contains("electricity") || lower.contains("electric bill") || lower.contains("gas bill") || lower.contains("water bill") || lower.contains("internet bill") || lower.contains("broadband") || lower.contains("titas") || lower.contains("desco") || lower.contains("dpdc") || lower.contains("wasa") || lower.contains("meter reading") || lower.contains("utility bill") || lower.contains("billing cycle") || lower.contains("bill no")
        val hasAmount = lower.contains("total amount due") || lower.contains("payable amount") || lower.contains("net payable") || lower.contains("bill amount") || lower.contains("টাকা") || lower.contains("৳")
        return utilityKeywords || (lower.contains("bill") && hasAmount && !lower.contains("receipt"))
    }

    private fun isReceiptDocument(lower: String, text: String): Boolean {
        val receiptKeywords = lower.contains("receipt") || lower.contains("store purchase") || lower.contains("cashier") || lower.contains("point of sale") || lower.contains("checkout") || lower.contains("thank you for shopping") || lower.contains("order receipt") || lower.contains("sales receipt")
        val paidSignals = lower.contains("paid by") || lower.contains("cash tendered") || lower.contains("change due") || lower.contains("paid via") || lower.contains("card ending")
        return receiptKeywords || (paidSignals && lower.contains("total"))
    }

    private fun isEducationalDocument(lower: String, text: String, fileName: String): Boolean {
        val fileLower = fileName.lowercase(Locale.ROOT)
        val fileSignals = fileLower.contains("modifier") || fileLower.contains("grammar") || fileLower.contains("chapter") || fileLower.contains("lesson") || fileLower.contains("lecture") || fileLower.contains("textbook") || fileLower.contains("syllabus") || fileLower.contains("physics") || fileLower.contains("math")
        val textSignals = lower.contains("chapter") || lower.contains("modifier") || lower.contains("grammar") || lower.contains("lesson") || lower.contains("exercise") || lower.contains("learning objectives") || lower.contains("definition:") || lower.contains("examples:") || lower.contains("adjective") || lower.contains("adverb") || lower.contains("noun phrase") || lower.contains("curriculum") || lower.contains("textbook") || lower.contains("formula") || lower.contains("theorem") || lower.contains("class:") || lower.contains("grade:")
        val notInvoiceOrBill = !lower.contains("invoice") && !lower.contains("amount due") && !lower.contains("due date: sep")
        return (fileSignals || textSignals) && notInvoiceOrBill
    }

    private fun isResearchPaper(lower: String, text: String): Boolean {
        val hasAbstract = lower.contains("abstract") || lower.contains("abstract—") || lower.contains("abstract:")
        val academicKeywords = lower.contains("methodology") || lower.contains("introduction") || lower.contains("references") || lower.contains("results and discussion") || lower.contains("ieee") || lower.contains("doi:") || lower.contains("department of") || lower.contains("university") || lower.contains("journal of")
        return hasAbstract && academicKeywords
    }

    private fun isContractDocument(lower: String, text: String): Boolean {
        val contractKeywords = lower.contains("agreement") || lower.contains("contract") || lower.contains("terms and conditions") || lower.contains("between the parties") || lower.contains("whereas,") || lower.contains("in witness whereof") || lower.contains("confidentiality agreement") || lower.contains("lease agreement") || lower.contains("employment agreement")
        return contractKeywords && (lower.contains("party") || lower.contains("clause") || lower.contains("signature"))
    }

    private fun isCertificateDocument(lower: String, text: String): Boolean {
        return lower.contains("certificate of") || lower.contains("this is to certify") || lower.contains("has successfully completed") || lower.contains("awarded to") || lower.contains("in recognition of") || lower.contains("diploma")
    }

    private fun isResumeDocument(lower: String, text: String): Boolean {
        val resumeKeywords = lower.contains("curriculum vitae") || lower.contains("resume") || lower.contains("work experience") || lower.contains("employment history") || lower.contains("professional experience") || lower.contains("skills summary") || lower.contains("education:") || lower.contains("academic qualifications")
        return resumeKeywords && (lower.contains("experience") || lower.contains("skills"))
    }

    private fun isProductManual(lower: String, text: String): Boolean {
        return lower.contains("user manual") || lower.contains("instruction manual") || lower.contains("owner's manual") || lower.contains("operating instructions") || lower.contains("troubleshooting guide") || lower.contains("setup guide") || lower.contains("specifications") && lower.contains("safety instructions")
    }

    private fun isNewsArticle(lower: String, text: String): Boolean {
        return lower.contains("reuters") || lower.contains("associated press") || lower.contains("bbc news") || lower.contains("daily star") || lower.contains("reported by") || (lower.contains("staff correspondent") && lower.contains("published"))
    }

    private fun isBankStatement(lower: String, text: String): Boolean {
        return (lower.contains("bank") || lower.contains("account statement")) && (lower.contains("opening balance") || lower.contains("closing balance") || lower.contains("statement period") || lower.contains("transactions summary"))
    }

    private fun isGeneralReferenceKnowledge(lower: String, text: String): Boolean {
        val wikiSignals = lower.contains("wikipedia") || lower.contains("overview") || lower.contains("history of") || lower.contains("encyclopedia") || lower.contains("geography of") || lower.contains("the capital of")
        return wikiSignals && !lower.contains("invoice") && !lower.contains("bill")
    }

    // --- Data Extraction Helpers ---

    private fun extractInvoiceNumber(text: String, lower: String): String? {
        val pattern = Pattern.compile("(?:invoice|inv)[\\s#.:\\-_]+([A-Z0-9\\-_]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val num = matcher.group(1)?.trim()
            if (!num.isNullOrBlank() && num.length >= 3) return num
        }
        return null
    }

    private fun extractCustomer(text: String, lines: List<String>): String? {
        val pattern = Pattern.compile("(?:bill to|customer|client|sold to|billed to)[\\s:]+([A-Za-z0-9 .,'-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(40)
        }
        return null
    }

    private fun extractInvoiceProvider(text: String, lines: List<String>): String? {
        val firstLine = lines.firstOrNull()
        if (!firstLine.isNullOrBlank() && !firstLine.lowercase(Locale.ROOT).contains("invoice") && firstLine.length <= 40) {
            return firstLine
        }
        val fromPattern = Pattern.compile("(?:from|vendor|provider|biller|company)[\\s:]+([A-Za-z0-9 .,'-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = fromPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(40)
        }
        return lines.take(2).firstOrNull { it.length <= 40 && !it.contains(":") }
    }

    private fun extractBillProvider(text: String, lines: List<String>): String? {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("desco") -> "DESCO"
            lower.contains("dpdc") -> "DPDC"
            lower.contains("titas") -> "TITAS Gas"
            lower.contains("wasa") -> "WASA"
            lower.contains("grameenphone") || lower.contains("gp") -> "Grameenphone"
            lower.contains("banglalink") -> "Banglalink"
            lower.contains("robi") -> "Robi"
            lower.contains("carnival") -> "Carnival Internet"
            lower.contains("link3") -> "Link3 Technologies"
            lower.contains("dot internet") -> "Dot Internet"
            else -> lines.firstOrNull { it.length <= 35 && !it.contains(":") }
        }
    }

    private fun detectBillType(lower: String): BillTypeCategory {
        return when {
            lower.contains("electricity") || lower.contains("electric") || lower.contains("বিদ্যুৎ") || lower.contains("desco") || lower.contains("dpdc") || lower.contains("kwh") -> BillTypeCategory("Electricity Bill", "ELECTRICITY")
            lower.contains("gas") || lower.contains("গ্যাস") || lower.contains("titas") -> BillTypeCategory("Gas Bill", "GAS")
            lower.contains("water") || lower.contains("পানি") || lower.contains("wasa") -> BillTypeCategory("Water Bill", "WATER")
            lower.contains("internet") || lower.contains("broadband") || lower.contains("wifi") || lower.contains("fiber") -> BillTypeCategory("Internet Bill", "INTERNET")
            lower.contains("mobile") || lower.contains("phone") || lower.contains("cellular") -> BillTypeCategory("Mobile Bill", "MOBILE")
            lower.contains("rent") || lower.contains("ভাড়া") -> BillTypeCategory("Rent Payment", "RENT")
            lower.contains("tuition") || lower.contains("school fee") || lower.contains("college fee") -> BillTypeCategory("Tuition Fee", "TUITION")
            lower.contains("insurance") || lower.contains("বীমা") -> BillTypeCategory("Insurance Premium", "INSURANCE")
            else -> BillTypeCategory("Utility Bill", "OTHER")
        }
    }

    data class BillTypeCategory(val displayName: String, val name: String)

    private fun extractTotalAmountDue(text: String, lower: String): Double? {
        // Look specifically for Total Amount Due, Net Payable, Total Due, Grand Total
        val priorityPatterns = listOf(
            Pattern.compile("(?:total amount due|total due|amount due|net payable|grand total|total payable|balance due)[\\s:—-]+[৳$€£Rs.]?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:total|payable)[\\s:—-]+[৳$€£Rs.]?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("[৳$€£]\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:due|total)", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in priorityPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val numStr = matcher.group(1)?.replace(",", "")
                val parsed = numStr?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) return parsed
            }
        }
        return null
    }

    private fun extractSubtotal(text: String, lower: String): Double? {
        val pattern = Pattern.compile("subtotal[\\s:—-]+[৳$€£Rs.]?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        }
        return null
    }

    private fun extractTax(text: String, lower: String): Double? {
        val pattern = Pattern.compile("(?:tax|vat|gst)[\\s:—-]+[৳$€£Rs.]?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        }
        return null
    }

    private fun extractReceiptTotal(text: String, lower: String): Double? {
        val pattern = Pattern.compile("(?:total|amount paid|paid amount|final total)[\\s:—-]+[৳$€£Rs.]?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        }
        return null
    }

    private fun extractMerchant(text: String, lines: List<String>): String? {
        val pattern = Pattern.compile("(?:merchant|store|shop|retailer)[\\s:]+([A-Za-z0-9 .,'-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(40)
        }
        return lines.firstOrNull { it.length in 4..35 && !it.contains(":") }
    }

    private fun extractProductName(text: String, lower: String): String? {
        val pattern = Pattern.compile("(?:item|product|description|service)[\\s:]+([A-Za-z0-9 .,'-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(40)
        }
        return null
    }

    private fun extractEducationalTopic(text: String, lines: List<String>, fileName: String): Triple<String, String, String> {
        val lower = text.lowercase(Locale.ROOT)
        val fileLower = fileName.lowercase(Locale.ROOT)

        var subject = when {
            lower.contains("grammar") || lower.contains("english") || fileLower.contains("english") || fileLower.contains("modifier") -> "English"
            lower.contains("physics") || fileLower.contains("physics") -> "Physics"
            lower.contains("math") || lower.contains("calculus") || lower.contains("algebra") -> "Mathematics"
            lower.contains("chemistry") || fileLower.contains("chemistry") -> "Chemistry"
            lower.contains("biology") -> "Biology"
            lower.contains("computer science") || lower.contains("programming") -> "Computer Science"
            else -> "Educational"
        }

        var topic = when {
            lower.contains("modifier") || fileLower.contains("modifier") -> "English Grammar — Modifiers"
            lower.contains("mechanics") -> "Classical Mechanics"
            lower.contains("calculus") -> "Differential & Integral Calculus"
            else -> lines.firstOrNull { it.length in 5..60 && !it.contains("---") } ?: "Study Material"
        }

        val title = "$subject: $topic"
        return Triple(subject, topic, title)
    }

    private fun extractKeyConcepts(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.length in 10..100 }
        return lines.take(4).joinToString(" • ")
    }

    private fun extractResearchTitle(text: String, lines: List<String>): String {
        return lines.firstOrNull { it.length in 15..90 && !it.lowercase(Locale.ROOT).contains("abstract") }
            ?: "Academic Research Study"
    }

    private fun extractAuthors(text: String, lines: List<String>): String? {
        val pattern = Pattern.compile("(?:authors?|by)[\\s:]+([A-Za-z0-9 .,'-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(50)
        }
        return null
    }

    private fun extractAbstract(text: String): String? {
        val pattern = Pattern.compile("abstract[\\s:—-]+(.*?)(?:introduction|keywords|i\\.|1\\.|$)", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.replace("\n", " ")?.take(300)
        }
        return text.take(200)
    }

    private fun extractKeyFindings(text: String): String? {
        val pattern = Pattern.compile("(?:results|findings|conclusion)[\\s:—-]+(.*?)(?:discussion|references|$)", Pattern.CASE_INSENSITIVE or Pattern.DOTALL)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.replace("\n", " ")?.take(200)
        }
        return null
    }

    private fun extractContractTitle(text: String, lines: List<String>): String {
        return lines.firstOrNull { l ->
            val ll = l.lowercase(Locale.ROOT)
            ll.contains("agreement") || ll.contains("contract") || ll.contains("terms")
        }?.take(50) ?: "Legal Agreement"
    }

    private fun extractContractParties(text: String): String? {
        val pattern = Pattern.compile("between[\\s:]+([A-Za-z0-9 .,'-]+?)(?:and|&)([A-Za-z0-9 .,'-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val p1 = matcher.group(1)?.trim()
            val p2 = matcher.group(2)?.trim()
            if (p1 != null && p2 != null) return "$p1 & $p2"
        }
        return null
    }

    private fun extractContractTerms(text: String): String? {
        val lines = text.lines().filter { it.lowercase(Locale.ROOT).contains("clause") || it.lowercase(Locale.ROOT).contains("section") }
        return if (lines.isNotEmpty()) lines.take(3).joinToString(" | ") else null
    }

    private fun extractCertificateRecipient(text: String, lines: List<String>): String? {
        val pattern = Pattern.compile("(?:awarded to|certify that|presented to)[\\s:]+([A-Za-z0-9 .,'-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(40)
        }
        return null
    }

    private fun extractCertificateIssuer(text: String, lines: List<String>): String? {
        val pattern = Pattern.compile("(?:issued by|authorized by|director|president)[\\s:]+([A-Za-z0-9 .,'-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(40)
        }
        return null
    }

    private fun extractResumeName(text: String, lines: List<String>): String? {
        return lines.firstOrNull { it.length in 4..30 && !it.contains(":") && it.all { ch -> ch.isLetter() || ch.isWhitespace() || ch == '.' } }
    }

    private fun extractResumeSkills(text: String): String? {
        val pattern = Pattern.compile("(?:skills|technical skills|technologies)[\\s:]+([A-Za-z0-9 .,+/#-]+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(100)
        }
        return null
    }

    private fun extractManualProductName(text: String, lines: List<String>): String? {
        return lines.firstOrNull { l ->
            val ll = l.lowercase(Locale.ROOT)
            !ll.contains("manual") && !ll.contains("instructions") && l.length in 4..40
        }
    }

    private fun extractBankName(text: String, lines: List<String>): String? {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("brac bank") -> "BRAC Bank"
            lower.contains("dutch bangla") || lower.contains("dbbl") -> "Dutch-Bangla Bank"
            lower.contains("city bank") -> "City Bank"
            lower.contains("eastern bank") || lower.contains("ebl") -> "Eastern Bank Limited"
            lower.contains("scb") || lower.contains("standard chartered") -> "Standard Chartered Bank"
            lower.contains("hsbc") -> "HSBC"
            lower.contains("islamic bank") || lower.contains("ibbl") -> "Islami Bank"
            else -> lines.firstOrNull { it.lowercase(Locale.ROOT).contains("bank") }?.take(40)
        }
    }

    private fun extractCurrency(text: String): String? {
        return when {
            text.contains("৳") || text.contains("TK") || text.contains("BDT") || text.contains("টাকা") -> "৳"
            text.contains("$") || text.contains("USD") -> "$"
            text.contains("€") || text.contains("EUR") -> "€"
            text.contains("£") || text.contains("GBP") -> "£"
            text.contains("₹") || text.contains("INR") || text.contains("Rs.") -> "₹"
            else -> "৳"
        }
    }

    private fun extractAllAmounts(text: String): List<Double> {
        val amounts = mutableListOf<Double>()
        val pattern = Pattern.compile("[৳$€£Rs.]?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)")
        val matcher = pattern.matcher(text)
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "")
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed in 1.0..9999999.0) {
                amounts.add(parsed)
            }
        }
        return amounts
    }

    private fun extractDueDate(text: String): String? {
        val pattern = Pattern.compile("(?:due date|due on|due|pay by|before)[\\s:—-]+([A-Za-z0-9, -]{4,25})", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            val candidate = matcher.group(1)?.trim()?.lines()?.firstOrNull()?.take(25)
            if (!candidate.isNullOrBlank() && candidate.any { it.isDigit() }) {
                return candidate
            }
        }
        return null
    }

    private fun extractDate(text: String): String? {
        val datePattern = Pattern.compile("(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2}(?:,\\s*\\d{4})?|\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*(?:\\s+\\d{4})?|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}/\\d{1,2}/\\d{2,4}", Pattern.CASE_INSENSITIVE)
        val matcher = datePattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(0)?.trim()
        }
        return null
    }

    private fun extractTime(text: String): String? {
        val timePattern = Pattern.compile("\\b(\\d{1,2}(?::\\d{2})?\\s*(?:AM|PM|am|pm))\\b")
        val matcher = timePattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }
}
