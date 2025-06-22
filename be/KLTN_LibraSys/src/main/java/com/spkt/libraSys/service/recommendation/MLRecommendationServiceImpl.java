package com.spkt.libraSys.service.recommendation;

import com.spkt.libraSys.service.access.AuthService;
import com.spkt.libraSys.service.document.*;
import com.spkt.libraSys.service.document.DigitalDocument.AccessRequest.AccessRequestEntity;
import com.spkt.libraSys.service.document.DigitalDocument.AccessRequest.AccessRequestRepository;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentEntity;
import com.spkt.libraSys.service.document.DigitalDocument.DigitalDocumentRepository;
import com.spkt.libraSys.service.document.favorite.FavoriteDocumentEntity;
import com.spkt.libraSys.service.document.favorite.FavoriteDocumentRepository;
import com.spkt.libraSys.service.document.upload.UploadEntity;
import com.spkt.libraSys.service.document.upload.UploadRepository;
import com.spkt.libraSys.service.document.viewer.ApprovalStatus;
import com.spkt.libraSys.service.role.RoleEntity;
import com.spkt.libraSys.service.user.UserEntity;
import com.spkt.libraSys.service.PageDTO;
import com.spkt.libraSys.service.loan.LoanEntity;
import com.spkt.libraSys.service.loan.LoanRepository;
import com.spkt.libraSys.service.user.UserRepository;
import com.spkt.libraSys.service.document.PhysicalDocument.PhysicalDocumentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.CollectionSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MLRecommendationServiceImpl implements MLRecommendationService {
    private final DocumentRepository documentRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final FavoriteDocumentRepository favoriteRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final UploadRepository uploadRepository;
    private final DocumentMapper documentMapper;
    private final AuthService authService;
    private final DigitalDocumentRepository digitalDocumentRepository;

    private Word2Vec word2Vec;
    private Map<String, INDArray> documentVectors;
    private Map<String, INDArray> userVectors;
    
    // Cache for recommendations
    private final Map<String, List<DocumentResponseDto>> recommendationCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> cacheTimestamps = new ConcurrentHashMap<>();
    private static final Duration CACHE_DURATION = Duration.ofMinutes(30);

    // Cache for document vectors
    private final Map<Long, INDArray> documentVectorCache = new ConcurrentHashMap<>();
    
    // Cache for user vectors
    private final Map<String, INDArray> userVectorCache = new ConcurrentHashMap<>();

    private static final String MODEL_PATH = "models/word2vec.zip";
    private static final String DOC_VECTORS_PATH = "models/document_vectors.ser";
    private static final String USER_VECTORS_PATH = "models/user_vectors.ser";

    @Override
    @Transactional
    public void trainModel() {
        try {
            // Clear caches when retraining
            recommendationCache.clear();
            cacheTimestamps.clear();
            documentVectorCache.clear();
            userVectorCache.clear();

            // 1. Thu thập dữ liệu với eager loading
            List<DocumentEntity> documents = documentRepository.findAllWithDocumentTypes();
            List<UserEntity> users = userRepository.findAll();
            
            if (documents.isEmpty() || users.isEmpty()) {
                log.warn("No documents or users found in database");
                return;
            }

            // 2. Xử lý dữ liệu văn bản
            TokenizerFactory t = new DefaultTokenizerFactory();
            t.setTokenPreProcessor(new CommonPreprocessor());

            // 3. Tạo dữ liệu huấn luyện với các yếu tố chung của user
            List<String> trainingData = new ArrayList<>();
            
            // 3.1 Thêm thông tin sách - Ensure document types are loaded
            for (DocumentEntity doc : documents) {
                if (doc.getDocumentTypes() == null) {
                    log.warn("Document types is null for document: {}", doc.getDocumentId());
                    continue;
                }
                
                trainingData.add(doc.getAuthor());
                trainingData.add(doc.getDescription());
                trainingData.add(doc.getDocumentCategory().toString());
                trainingData.add(doc.getPublisher());
                trainingData.add(doc.getLanguage());
                
                doc.getDocumentTypes().forEach(type -> 
                    trainingData.add(type.getTypeName())
                );
            }

            // 3.2 Thêm thông tin ngành học
            Set<String> majors = users.stream()
                .map(UserEntity::getMajorCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            trainingData.addAll(majors);

            // 3.3 Thêm thông tin khóa học
            Set<Integer> batches = users.stream()
                .map(UserEntity::getStudentBatch)
                .filter(batch -> batch > 0)
                .collect(Collectors.toSet());
            trainingData.addAll(batches.stream().map(String::valueOf).collect(Collectors.toList()));

            // 4. Huấn luyện Word2Vec
            SentenceIterator iter = new CollectionSentenceIterator(trainingData);
            word2Vec = new Word2Vec.Builder()
                .minWordFrequency(3)
                .iterations(10)
                .layerSize(200)
                .seed(42)
                .windowSize(8)
                .iterate(iter)
                .tokenizerFactory(t)
                .build();

            word2Vec.fit();

            // 5. Tạo vector cho sách - Ensure document types are loaded
            documentVectors = new HashMap<>();
            for (DocumentEntity doc : documents) {
                if (doc.getDocumentTypes() == null) {
                    log.warn("Document types is null for document: {}", doc.getDocumentId());
                    continue;
                }
                
                StringBuilder textBuilder = new StringBuilder();
                textBuilder.append(doc.getAuthor()).append(" ");
                textBuilder.append(doc.getDescription()).append(" ");
                textBuilder.append(doc.getDocumentCategory().toString()).append(" ");
                textBuilder.append(doc.getPublisher()).append(" ");
                textBuilder.append(doc.getLanguage()).append(" ");
                
                doc.getDocumentTypes().forEach(type -> 
                    textBuilder.append(type.getTypeName()).append(" ")
                );
                
                INDArray vector = createDocumentVector(textBuilder.toString().trim());
                documentVectors.put(doc.getDocumentId().toString(), vector);
            }

            // 6. Tạo vector cho người dùng với các yếu tố chung
            userVectors = new HashMap<>();
            for (UserEntity user : users) {
                INDArray userVector = Nd4j.zeros(200);
                double totalWeight = 0.0;

                // 6.1 Thông tin từ profile (weight: 0.25)
                String userProfile = String.join(" ",
                    user.getMajorCode(),
                    String.valueOf(user.getStudentBatch()),
                    user.getRoleEntities().stream()
                        .map(RoleEntity::getRoleName)
                        .collect(Collectors.joining(" "))
                );
                INDArray profileVector = createDocumentVector(userProfile);
                userVector.addi(profileVector.mul(0.25));
                totalWeight += 0.25;

                // 6.2 Thông tin từ lịch sử mượn (weight: 0.25)
                List<LoanEntity> userLoans = loanRepository.findByUserEntityWithDocuments(user);
                if (!userLoans.isEmpty()) {
                    INDArray loanVector = Nd4j.zeros(200);
                    int loanCount = 0;
                    Map<String, Integer> documentTypeCount = new HashMap<>();
                    
                    for (LoanEntity loan : userLoans) {
                        try {
                            PhysicalDocumentEntity physicalDoc = loan.getPhysicalDoc();
                            if (physicalDoc != null && physicalDoc.getDocument() != null) {
                                DocumentEntity doc = physicalDoc.getDocument();
                                // Ensure document types are loaded
                                DocumentEntity loadedDoc = documentRepository.findByIdWithDocumentTypes(doc.getDocumentId())
                                    .orElse(null);
                                if (loadedDoc != null && loadedDoc.getDocumentTypes() != null) {
                                    INDArray docVector = documentVectors.get(loadedDoc.getDocumentId().toString());
                                    if (docVector != null) {
                                        loanVector.addi(docVector);
                                        loanCount++;
                                        
                                        loadedDoc.getDocumentTypes().forEach(type -> 
                                            documentTypeCount.merge(type.getTypeName(), 1, Integer::sum)
                                        );
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Error processing loan for user {}: {}", user.getUserId(), e.getMessage());
                        }
                    }
                    if (loanCount > 0) {
                        loanVector.divi(loanCount);
                        userVector.addi(loanVector.mul(0.25));
                        totalWeight += 0.25;
                        
                        log.info("User {} document type preferences: {}", user.getUserId(), documentTypeCount);
                    }
                }

                // 6.3 Thông tin từ sách yêu thích (weight: 0.2)
                List<FavoriteDocumentEntity> favorites = favoriteRepository.findByUser(user);
                if (!favorites.isEmpty()) {
                    INDArray favoriteVector = Nd4j.zeros(200);
                    int favoriteCount = 0;
                    Map<String, Integer> favoriteTypeCount = new HashMap<>();
                    
                    for (FavoriteDocumentEntity favorite : favorites) {
                        DocumentEntity doc = favorite.getDocument();
                        if (doc != null) {
                            // Ensure document types are loaded
                            DocumentEntity loadedDoc = documentRepository.findByIdWithDocumentTypes(doc.getDocumentId())
                                .orElse(null);
                            if (loadedDoc != null && loadedDoc.getDocumentTypes() != null) {
                                INDArray docVector = documentVectors.get(loadedDoc.getDocumentId().toString());
                                if (docVector != null) {
                                    favoriteVector.addi(docVector);
                                    favoriteCount++;
                                    
                                    loadedDoc.getDocumentTypes().forEach(type -> 
                                        favoriteTypeCount.merge(type.getTypeName(), 1, Integer::sum)
                                    );
                                }
                            }
                        }
                    }
                    if (favoriteCount > 0) {
                        favoriteVector.divi(favoriteCount);
                        userVector.addi(favoriteVector.mul(0.2));
                        totalWeight += 0.2;
                        
                        log.info("User {} favorite document types: {}", user.getUserId(), favoriteTypeCount);
                    }
                }

                // 6.4 Thông tin từ yêu cầu truy cập (weight: 0.15)
                List<AccessRequestEntity> accessRequests = accessRequestRepository.findByRequesterId(user.getUserId());
                if (!accessRequests.isEmpty()) {
                    INDArray accessVector = Nd4j.zeros(200);
                    int accessCount = 0;
                    Map<String, Integer> accessTypeCount = new HashMap<>();
                    
                    for (AccessRequestEntity request : accessRequests) {
                        long digitalId = request.getDigitalId();
                        DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(digitalId)
                                .orElse(null);
                        if (digitalDocument != null && digitalDocument.getDocument() != null) {
                            // Ensure document types are loaded
                            DocumentEntity loadedDoc = documentRepository.findByIdWithDocumentTypes(
                                    digitalDocument.getDocument().getDocumentId()
                            ).orElse(null);
                            
                            if (loadedDoc != null && loadedDoc.getDocumentTypes() != null) {
                                INDArray docVector = documentVectors.get(loadedDoc.getDocumentId().toString());
                                if (docVector != null) {
                                    accessVector.addi(docVector);
                                    accessCount++;
                                    
                                    loadedDoc.getDocumentTypes().forEach(type -> 
                                        accessTypeCount.merge(type.getTypeName(), 1, Integer::sum)
                                    );
                                }
                            }
                        }
                    }
                    if (accessCount > 0) {
                        accessVector.divi(accessCount);
                        userVector.addi(accessVector.mul(0.15));
                        totalWeight += 0.15;
                        
                        log.info("User {} access document types: {}", user.getUserId(), accessTypeCount);
                    }
                }

                // 6.5 Thêm thông tin từ các user tương tự (weight: 0.15)
                Map<String, Double> commonFactors = analyzeCommonUserFactors(user);
                if (!commonFactors.isEmpty()) {
                    INDArray similarUserVector = Nd4j.zeros(200);
                    double similarWeight = 0.0;

                    List<UserEntity> similarUsers = findSimilarUsersML(user);
                    if (!similarUsers.isEmpty()) {
                        for (UserEntity similarUser : similarUsers) {
                            INDArray similarVector = userVectors.get(similarUser.getUserId());
                            if (similarVector != null) {
                                similarUserVector.addi(similarVector);
                                similarWeight += 1.0;
                            }
                        }
                        if (similarWeight > 0) {
                            similarUserVector.divi(similarWeight);
                            userVector.addi(similarUserVector.mul(0.15));
                            totalWeight += 0.15;
                        }
                    }
                }

                // 6.6 Chuẩn hóa vector theo tổng trọng số
                if (totalWeight > 0) {
                    userVector.divi(totalWeight);
                }

                userVectors.put(user.getUserId(), userVector);
                log.info("Created user vector for user {} with total weight {}", user.getUserId(), totalWeight);
            }

            // Lưu mô hình và vectors
            saveModel();
            log.info("Model training completed successfully with user common factors and document types");
            
            // After training, pre-compute and cache document vectors
            documentRepository.findAllWithDocumentTypes().forEach(doc -> {
                if (doc.getDocumentId() != null) {
                    documentVectorCache.put(doc.getDocumentId(), documentVectors.get(doc.getDocumentId().toString()));
                }
            });

            log.info("Model training completed successfully with optimized caching");
        } catch (Exception e) {
            log.error("Error training model: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to train model", e);
        }
    }

    private void saveModel() {
        try {
            // Tạo thư mục nếu chưa tồn tại
            new File("models").mkdirs();

            // Lưu mô hình Word2Vec
            WordVectorSerializer.writeWord2VecModel(word2Vec, new File(MODEL_PATH));

            // Lưu document vectors
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DOC_VECTORS_PATH))) {
                oos.writeObject(documentVectors);
            }

            // Lưu user vectors
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_VECTORS_PATH))) {
                oos.writeObject(userVectors);
            }

            log.info("Model saved successfully");
        } catch (IOException e) {
            log.error("Error saving model", e);
        }
    }

    @PostConstruct
    @Transactional
    public void loadModel() {
        try {
            // Kiểm tra xem file mô hình có tồn tại không
            File modelFile = new File(MODEL_PATH);
            if (!modelFile.exists()) {
                log.info("No existing model found. Will train new model on first request.");
                trainModel();
                return;
            }

            // Đọc mô hình Word2Vec
            word2Vec = WordVectorSerializer.readWord2VecModel(modelFile);
            if (word2Vec == null) {
                log.warn("Failed to load Word2Vec model. Will train new model on first request.");
                trainModel();
                return;
            }

            // Đọc document vectors
            File docVectorsFile = new File(DOC_VECTORS_PATH);
            if (docVectorsFile.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(docVectorsFile))) {
                    documentVectors = (Map<String, INDArray>) ois.readObject();
                }
            } else {
                documentVectors = new HashMap<>();
            }

            // Đọc user vectors
            File userVectorsFile = new File(USER_VECTORS_PATH);
            if (userVectorsFile.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(userVectorsFile))) {
                    userVectors = (Map<String, INDArray>) ois.readObject();
                }
            } else {
                userVectors = new HashMap<>();
            }

            log.info("Model loaded successfully");
        } catch (Exception e) {
            log.error("Error loading model: {}", e.getMessage());
            // Khởi tạo các map rỗng để tránh NPE
            documentVectors = new HashMap<>();
            userVectors = new HashMap<>();
        }
    }

    @Override
    public double predictUserPreference(UserEntity user, DocumentEntity document) {
        if (user == null || user.getUserId() == null || document == null || document.getDocumentId() == null) {
            return 0.0;
        }

        // Check vector caches first
        INDArray userVector = userVectors.get(user.getUserId());
        if (userVector != null) {
            userVectorCache.put(user.getUserId(), userVector);
        }
        
        INDArray docVector = documentVectorCache.computeIfAbsent(document.getDocumentId(), 
            (Long k) -> documentVectors.get(k.toString()));
        
        if (userVector == null || docVector == null) {
            return 0.0;
        }

        // Optimize vector calculations
        double dotProduct = userVector.mul(docVector).sumNumber().doubleValue();
        double norm1 = Math.sqrt(userVector.mul(userVector).sumNumber().doubleValue());
        double norm2 = Math.sqrt(docVector.mul(docVector).sumNumber().doubleValue());
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (norm1 * norm2);
    }

    @Override
    @Transactional
    public PageDTO<DocumentResponseDto> getMLRecommendations(Pageable pageable) {
        UserEntity user = authService.getCurrentUser();
        if (user == null || user.getUserId() == null) {
            return new PageDTO<>(new PageImpl<>(Collections.emptyList()));
        }

        // 1. Lấy danh sách sách
        List<DocumentEntity> allDocuments = documentRepository.findAllWithDocumentTypes();
        
        // 2. Tính điểm dự đoán cho mỗi sách
        List<DocumentEntity> recommendedDocs = allDocuments.stream()
            .filter(doc -> doc != null && doc.getDocumentId() != null
                    &&doc.getApprovalStatus().equals(ApprovalStatus.APPROVED) && doc.getStatus().equals(DocumentStatus.ENABLED))
            .map(doc -> {
                try {
                    double score = predictUserPreference(user, doc);
                    return new AbstractMap.SimpleEntry<>(doc, score);
                } catch (Exception e) {
                    log.warn("Error predicting preference for document {}: {}", doc.getDocumentId(), e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull)
            //.filter(entry -> entry.getValue() >= 0)
            .sorted(Map.Entry.<DocumentEntity, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 3. Phân trang kết quả
        return paginateResults(recommendedDocs, pageable);
    }

    @Override
    @Transactional
    public Map<DocumentEntity, Double> getBookCorrelations(DocumentEntity book) {
        if (book == null || book.getDocumentId() == null) {
            log.warn("Book is null or missing ID");
            return Collections.emptyMap();
        }

        // Load book with document types
        DocumentEntity loadedBook = documentRepository.findByIdWithDocumentTypes(book.getDocumentId())
            .orElse(null);
        if (loadedBook == null) {
            log.warn("Book not found: {}", book.getDocumentId());
            return Collections.emptyMap();
        }

        INDArray bookVector = documentVectors.get(loadedBook.getDocumentId().toString());
        if (bookVector == null) {
            log.warn("Book vector is null for document: {}", loadedBook.getDocumentId());
            return Collections.emptyMap();
        }
        
        return documentRepository.findAllWithDocumentTypes().stream()
            .filter(doc -> doc != null && doc.getDocumentId() != null && !doc.equals(loadedBook))
            .map(doc -> {
                INDArray docVector = documentVectors.get(doc.getDocumentId().toString());
                if (docVector == null) {
                    return null;
                }
                return new AbstractMap.SimpleEntry<DocumentEntity, Double>(doc, calculateCosineSimilarity(bookVector, docVector));
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }

    @Override
    public void updateModel() {
        trainModel();
    }

    @Override
    public Map<String, Double> analyzeReadingTrendsML() {
        Map<String, Double> trends = new HashMap<>();
        
        // Phân tích theo nhiều tiêu chí
        // 1. Xu hướng theo thời gian
        Map<String, Long> timeBasedTrends = loanRepository.findRecentLoans(LocalDateTime.now().minusDays(30))
            .stream()
            .collect(Collectors.groupingBy(
                loan -> String.valueOf(loan.getPhysicalDoc().getDocument().getDocumentCategory()),
                Collectors.counting()
            ));

        // 2. Xu hướng theo người dùng
        Map<String, Long> userBasedTrends = favoriteRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(
                favorite -> favorite.getDocument().getDocumentCategory().toString(),
                Collectors.counting()
            ));

        // 3. Xu hướng theo yêu cầu truy cập
        Map<String, Long> accessBasedTrends = accessRequestRepository.findAll()
            .stream()
            .collect(Collectors.groupingBy(
                request -> {
                    DigitalDocumentEntity digitalDocument = digitalDocumentRepository.findById(request.getDigitalId())
                            .orElse(null);


                       if (digitalDocument != null &&  digitalDocument.getDocument() != null) {
                        return String.valueOf(digitalDocument.getDocument().getDocumentCategory());
                    }
                    return "Unknown";
                },
                Collectors.counting()
            ));

        // Kết hợp các xu hướng
        for (String category : timeBasedTrends.keySet()) {
            double score = 0.0;
            score += timeBasedTrends.getOrDefault(category, 0L) * 0.4;
            score += userBasedTrends.getOrDefault(category, 0L) * 0.3;
            score += accessBasedTrends.getOrDefault(category, 0L) * 0.3;
            trends.put(category, score);
        }
        
        return trends;
    }

    @Override
    public List<UserEntity> findSimilarUsersML(UserEntity user) {
        if (user == null || user.getUserId() == null) {
            return Collections.emptyList();
        }

        // Phân tích các yếu tố chung
        Map<String, Double> commonFactors = analyzeCommonUserFactors(user);
        log.info("Common factors for user {}: {}", user.getUserId(), commonFactors);

        // Tìm user tương tự dựa trên các yếu tố
        return userRepository.findAll().stream()
            .filter(u -> !u.getUserId().equals(user.getUserId()))
            .map(u -> {
                double similarity = 0.0;
                
                // Tính điểm tương đồng dựa trên các yếu tố
                String userMajorCode = user.getMajorCode();
                String otherMajorCode = u.getMajorCode();
                if (userMajorCode != null && otherMajorCode != null && 
                    userMajorCode.equals(otherMajorCode)) {
                    similarity += commonFactors.getOrDefault("major_similarity", 0.0) * 0.3;
                }
                
                Integer userBatch = user.getStudentBatch();
                Integer otherBatch = u.getStudentBatch();
                if (userBatch != null && otherBatch != null && 
                    userBatch.equals(otherBatch)) {
                    similarity += commonFactors.getOrDefault("batch_similarity", 0.0) * 0.3;
                }
                
                similarity += commonFactors.getOrDefault("loan_similarity", 0.0) * 0.2;
                similarity += commonFactors.getOrDefault("favorite_similarity", 0.0) * 0.2;
                
                return new AbstractMap.SimpleEntry<>(u, similarity);
            })
            .sorted(Map.Entry.<UserEntity, Double>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PageDTO<DocumentResponseDto> getRecommendedDocumentsForUser(Pageable pageable) {
        UserEntity user = authService.getCurrentUser();
        if (user == null || user.getUserId() == null) {
            return new PageDTO<>(new PageImpl<>(Collections.emptyList()));
        }

        // Check cache first
        String cacheKey = user.getUserId();
        LocalDateTime cacheTime = cacheTimestamps.get(cacheKey);
        if (cacheTime != null && 
            Duration.between(cacheTime, LocalDateTime.now()).compareTo(CACHE_DURATION) < 0) {
            List<DocumentResponseDto> cachedRecommendations = recommendationCache.get(cacheKey);
            if (cachedRecommendations != null) {
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), cachedRecommendations.size());
                List<DocumentResponseDto> pagedContent = start < cachedRecommendations.size() 
                    ? cachedRecommendations.subList(start, end) 
                    : Collections.emptyList();
                return new PageDTO<>(new PageImpl<>(pagedContent, pageable, cachedRecommendations.size()));
            }
        }

        // Get user's preferences and history
        String userMajor = user.getMajorCode();
        Integer userBatch = user.getStudentBatch();
        
        // Get user's loan history with eager loading
        Set<Long> userLoanedDocIds = loanRepository.findByUserEntityWithDocuments(user).stream()
            .map(loan -> loan.getPhysicalDoc().getDocument().getDocumentId())
            .collect(Collectors.toSet());

        // Get user's favorite documents
        Set<Long> userFavoriteDocIds = favoriteRepository.findByUser(user).stream()
            .map(fav -> fav.getDocument().getDocumentId())
            .collect(Collectors.toSet());

        // Get all documents with efficient filtering
        List<DocumentEntity> allDocuments = documentRepository.findAllWithDocumentTypes().stream()
            .filter(doc -> doc != null && doc.getDocumentId() != null)
            .filter(doc -> !userLoanedDocIds.contains(doc.getDocumentId())) // Exclude already loaned
            .filter(doc -> !userFavoriteDocIds.contains(doc.getDocumentId())) // Exclude favorites
            .collect(Collectors.toList());

        // Calculate recommendations using multiple factors
        List<DocumentResponseDto> recommendations = allDocuments.stream()
            .map(doc -> {
                double score = 0.0;
                
                // 1. Document Type Score (30%)
                if (doc.getDocumentTypes() != null) {
                    score += 0.3 * doc.getDocumentTypes().size();
                }

                // 2. Category Score (20%)
                if (doc.getDocumentCategory() != null) {
                    score += 0.2;
                }

                // 3. Major Relevance Score (20%)
                if (userMajor != null && doc.getDocumentTypes() != null) {
                    boolean isRelevantToMajor = doc.getDocumentTypes().stream()
                        .anyMatch(type -> type.getTypeName().toLowerCase().contains(userMajor.toLowerCase()));
                    if (isRelevantToMajor) {
                        score += 0.2;
                    }
                }

                // 4. Batch Relevance Score (15%)
                if (userBatch != null && doc.getDocumentTypes() != null) {
                    boolean isRelevantToBatch = doc.getDocumentTypes().stream()
                        .anyMatch(type -> type.getTypeName().toLowerCase().contains("batch " + userBatch));
                    if (isRelevantToBatch) {
                        score += 0.15;
                    }
                }

                // 5. Popularity Score (15%)
                // Get all loans and favorites for this document with eager loading
                List<LoanEntity> docLoans = loanRepository.findByUserEntityWithDocuments(user).stream()
                    .filter(loan -> loan.getPhysicalDoc() != null && 
                                  loan.getPhysicalDoc().getDocument() != null &&
                                  loan.getPhysicalDoc().getDocument().getDocumentId().equals(doc.getDocumentId()))
                    .collect(Collectors.toList());

                List<FavoriteDocumentEntity> docFavorites = favoriteRepository.findByUser(user).stream()
                    .filter(fav -> fav.getDocument() != null && 
                                 fav.getDocument().getDocumentId().equals(doc.getDocumentId()))
                    .collect(Collectors.toList());

                double popularityScore = 0.15 * (docLoans.size() + docFavorites.size()) / (allDocuments.size() + 1);
                score += popularityScore;

                return new AbstractMap.SimpleEntry<>(convertToDto(doc), score);
            })
            .filter(entry -> entry.getValue() > 0)
            .sorted(Map.Entry.<DocumentResponseDto, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // Update cache
        recommendationCache.put(cacheKey, recommendations);
        cacheTimestamps.put(cacheKey, LocalDateTime.now());

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), recommendations.size());
        List<DocumentResponseDto> pagedContent = start < recommendations.size() 
            ? recommendations.subList(start, end) 
            : Collections.emptyList();
            
        return new PageDTO<>(new PageImpl<>(pagedContent, pageable, recommendations.size()));
    }

    // Helper methods
    private INDArray createDocumentVector(String text) {
        String[] words = text.toLowerCase().split("\\s+");
        INDArray vector = Nd4j.zeros(200);
        
        for (String word : words) {
            if (word2Vec.hasWord(word)) {
                vector.addi(word2Vec.getWordVectorMatrix(word));
            }
        }
        
        return vector.divi(words.length);
    }

    private double calculateCosineSimilarity(INDArray vec1, INDArray vec2) {
        if (vec1 == null || vec2 == null) {
            return 0.0;
        }
        
        double dotProduct = vec1.mul(vec2).sumNumber().doubleValue();
        double norm1 = Math.sqrt(vec1.mul(vec1).sumNumber().doubleValue());
        double norm2 = Math.sqrt(vec2.mul(vec2).sumNumber().doubleValue());
        
        return dotProduct / (norm1 * norm2);
    }

    private PageDTO<DocumentResponseDto> paginateResults(List<DocumentEntity> documents, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), documents.size());
        List<DocumentEntity> pagedDocs = start < documents.size() 
            ? documents.subList(start, end) 
            : new ArrayList<>();
            
        List<DocumentResponseDto> content = pagedDocs.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        Page<DocumentResponseDto> page = new PageImpl<>(content, pageable, documents.size());
        return new PageDTO<>(page);
    }

    private DocumentResponseDto convertToDto(DocumentEntity document) {
        // Implement conversion logic here
        return documentMapper.toDocumentResponse(document);
    }

    // Thêm các phương thức mới để phân tích hành vi và yếu tố chung của user

    /**
     * Phân tích các yếu tố chung giữa các user
     */
    private Map<String, Double> analyzeCommonUserFactors(UserEntity user) {
        Map<String, Double> commonFactors = new HashMap<>();
        
        // 1. Phân tích theo ngành học
        String majorCode = user.getMajorCode();
        if (majorCode != null) {
            List<UserEntity> sameMajorUsers = userRepository.findByMajorCode(majorCode);
            double majorSimilarity = calculateMajorSimilarity(user, sameMajorUsers);
            commonFactors.put("major_similarity", majorSimilarity);
        }

        // 2. Phân tích theo khóa học
        int studentBatch = user.getStudentBatch();
        if (studentBatch > 0) {
            List<UserEntity> sameBatchUsers = userRepository.findByStudentBatch(studentBatch);
            double batchSimilarity = calculateBatchSimilarity(user, sameBatchUsers);
            commonFactors.put("batch_similarity", batchSimilarity);
        }

        // 3. Phân tích theo lịch sử mượn sách
        List<LoanEntity> userLoans = loanRepository.findByUserEntity(user);
        if (!userLoans.isEmpty()) {
            double loanSimilarity = calculateLoanSimilarity(user, userLoans);
            commonFactors.put("loan_similarity", loanSimilarity);
        }

        // 4. Phân tích theo sách yêu thích
        List<FavoriteDocumentEntity> userFavorites = favoriteRepository.findByUser(user);
        if (!userFavorites.isEmpty()) {
            double favoriteSimilarity = calculateFavoriteSimilarity(user, userFavorites);
            commonFactors.put("favorite_similarity", favoriteSimilarity);
        }

        return commonFactors;
    }

    /**
     * Tính độ tương đồng về ngành học
     */
    private double calculateMajorSimilarity(UserEntity user, List<UserEntity> sameMajorUsers) {
        if (sameMajorUsers.isEmpty()) return 0.0;

        double totalSimilarity = 0.0;
        int count = 0;

        for (UserEntity otherUser : sameMajorUsers) {
            if (!otherUser.getUserId().equals(user.getUserId())) {
                // So sánh các yếu tố khác ngoài ngành học
                double similarity = 0.0;
                
                // So sánh khóa học
                if (otherUser.getStudentBatch() == user.getStudentBatch()) {
                    similarity += 0.3;
                }

                // So sánh sách yêu thích
                List<FavoriteDocumentEntity> userFavorites = favoriteRepository.findByUser(user);
                List<FavoriteDocumentEntity> otherFavorites = favoriteRepository.findByUser(otherUser);
                if (!userFavorites.isEmpty() && !otherFavorites.isEmpty()) {
                    Set<Long> userFavoriteIds = userFavorites.stream()
                        .map(f -> f.getFavoriteId())
                        .collect(Collectors.toSet());
                    Set<Long> otherFavoriteIds = otherFavorites.stream()
                        .map(f -> f.getFavoriteId())
                        .collect(Collectors.toSet());
                    
                    double favoriteOverlap = calculateSetOverlap(userFavoriteIds, otherFavoriteIds);
                    similarity += favoriteOverlap * 0.4;
                }

                // So sánh lịch sử mượn
                List<LoanEntity> userLoans = loanRepository.findByUserEntity(user);
                List<LoanEntity> otherLoans = loanRepository.findByUserEntity(otherUser);
                if (!userLoans.isEmpty() && !otherLoans.isEmpty()) {
                    Set<Long> userLoanDocIds = userLoans.stream()
                        .map(l -> l.getPhysicalDoc().getDocument().getDocumentId())
                        .collect(Collectors.toSet());
                    Set<Long> otherLoanDocIds = otherLoans.stream()
                        .map(l -> l.getPhysicalDoc().getDocument().getDocumentId())
                        .collect(Collectors.toSet());
                    
                    double loanOverlap = calculateSetOverlap(userLoanDocIds, otherLoanDocIds);
                    similarity += loanOverlap * 0.3;
                }

                totalSimilarity += similarity;
                count++;
            }
        }

        return count > 0 ? totalSimilarity / count : 0.0;
    }

    /**
     * Tính độ tương đồng về khóa học
     */
    private double calculateBatchSimilarity(UserEntity user, List<UserEntity> sameBatchUsers) {
        if (sameBatchUsers.isEmpty()) return 0.0;

        double totalSimilarity = 0.0;
        int count = 0;

        for (UserEntity otherUser : sameBatchUsers) {
            if (!otherUser.getUserId().equals(user.getUserId())) {
                double similarity = 0.0;

                // So sánh ngành học
                if (otherUser.getMajorCode().equals(user.getMajorCode())) {
                    similarity += 0.4;
                }

                // So sánh sách yêu thích
                List<FavoriteDocumentEntity> userFavorites = favoriteRepository.findByUser(user);
                List<FavoriteDocumentEntity> otherFavorites = favoriteRepository.findByUser(otherUser);
                if (!userFavorites.isEmpty() && !otherFavorites.isEmpty()) {
                    Set<Long> userFavoriteIds = userFavorites.stream()
                        .map(f -> f.getFavoriteId())
                        .collect(Collectors.toSet());
                    Set<Long> otherFavoriteIds = otherFavorites.stream()
                        .map(f -> f.getFavoriteId())
                        .collect(Collectors.toSet());
                    
                    double favoriteOverlap = calculateSetOverlap(userFavoriteIds, otherFavoriteIds);
                    similarity += favoriteOverlap * 0.3;
                }

                // So sánh lịch sử mượn
                List<LoanEntity> userLoans = loanRepository.findByUserEntity(user);
                List<LoanEntity> otherLoans = loanRepository.findByUserEntity(otherUser);
                if (!userLoans.isEmpty() && !otherLoans.isEmpty()) {
                    Set<Long> userLoanDocIds = userLoans.stream()
                        .map(l -> l.getPhysicalDoc().getDocument().getDocumentId())
                        .collect(Collectors.toSet());
                    Set<Long> otherLoanDocIds = otherLoans.stream()
                        .map(l -> l.getPhysicalDoc().getDocument().getDocumentId())
                        .collect(Collectors.toSet());
                    
                    double loanOverlap = calculateSetOverlap(userLoanDocIds, otherLoanDocIds);
                    similarity += loanOverlap * 0.3;
                }

                totalSimilarity += similarity;
                count++;
            }
        }

        return count > 0 ? totalSimilarity / count : 0.0;
    }

    /**
     * Tính độ tương đồng về lịch sử mượn
     */
    private double calculateLoanSimilarity(UserEntity user, List<LoanEntity> userLoans) {
        if (userLoans.isEmpty()) return 0.0;

        // Load current user's loans with documents
        List<LoanEntity> loadedUserLoans = loanRepository.findByUserEntityWithDocuments(user);
        if (loadedUserLoans.isEmpty()) return 0.0;

        // Lấy danh sách user khác có mượn sách
        Set<Long> userLoanDocIds = loadedUserLoans.stream()
            .map(l -> l.getPhysicalDoc().getDocument().getDocumentId())
            .collect(Collectors.toSet());

        List<UserEntity> otherUsers = userRepository.findAll().stream()
            .filter(u -> !u.getUserId().equals(user.getUserId()))
            .collect(Collectors.toList());

        double totalSimilarity = 0.0;
        int count = 0;

        for (UserEntity otherUser : otherUsers) {
            // Use eager loading for loans
            List<LoanEntity> otherLoans = loanRepository.findByUserEntityWithDocuments(otherUser);
            if (!otherLoans.isEmpty()) {
                Set<Long> otherLoanDocIds = otherLoans.stream()
                    .map(l -> l.getPhysicalDoc().getDocument().getDocumentId())
                    .collect(Collectors.toSet());

                double overlap = calculateSetOverlap(userLoanDocIds, otherLoanDocIds);
                if (overlap > 0) {
                    // Thêm các yếu tố khác
                    double similarity = overlap;
                    
                    // Cùng ngành học - Thêm null check
                    String userMajorCode = user.getMajorCode();
                    String otherMajorCode = otherUser.getMajorCode();
                    if (userMajorCode != null && otherMajorCode != null && 
                        userMajorCode.equals(otherMajorCode)) {
                        similarity += 0.2;
                    }
                    
                    // Cùng khóa học - Thêm null check
                    Integer userBatch = user.getStudentBatch();
                    Integer otherBatch = otherUser.getStudentBatch();
                    if (userBatch != null && otherBatch != null && 
                        userBatch.equals(otherBatch)) {
                        similarity += 0.2;
                    }

                    totalSimilarity += similarity;
                    count++;
                }
            }
        }

        return count > 0 ? totalSimilarity / count : 0.0;
    }

    /**
     * Tính độ tương đồng về sách yêu thích
     */
    private double calculateFavoriteSimilarity(UserEntity user, List<FavoriteDocumentEntity> userFavorites) {
        if (userFavorites.isEmpty()) return 0.0;

        Set<Long> userFavoriteIds = userFavorites.stream()
            .map(f -> f.getFavoriteId())
            .collect(Collectors.toSet());

        List<UserEntity> otherUsers = userRepository.findAll().stream()
            .filter(u -> !u.getUserId().equals(user.getUserId()))
            .collect(Collectors.toList());

        double totalSimilarity = 0.0;
        int count = 0;

        for (UserEntity otherUser : otherUsers) {
            List<FavoriteDocumentEntity> otherFavorites = favoriteRepository.findByUser(otherUser);
            if (!otherFavorites.isEmpty()) {
                Set<Long> otherFavoriteIds = otherFavorites.stream()
                    .map(f -> f.getFavoriteId())
                    .collect(Collectors.toSet());

                double overlap = calculateSetOverlap(userFavoriteIds, otherFavoriteIds);
                if (overlap > 0) {
                    // Thêm các yếu tố khác
                    double similarity = overlap;
                    
                    // Cùng ngành học
                    if (otherUser.getMajorCode().equals(user.getMajorCode())) {
                        similarity += 0.2;
                    }
                    
                    // Cùng khóa học
                    if (otherUser.getStudentBatch() == user.getStudentBatch()) {
                        similarity += 0.2;
                    }

                    totalSimilarity += similarity;
                    count++;
                }
            }
        }

        return count > 0 ? totalSimilarity / count : 0.0;
    }

    /**
     * Tính độ chồng lấp giữa hai tập hợp
     */
    private double calculateSetOverlap(Set<Long> set1, Set<Long> set2) {
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;
        
        Set<Long> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<Long> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return (double) intersection.size() / union.size();
    }

    // Add cache cleanup method
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupCache() {
        LocalDateTime now = LocalDateTime.now();
        cacheTimestamps.entrySet().removeIf(entry -> 
            Duration.between(entry.getValue(), now).compareTo(CACHE_DURATION) >= 0);
        recommendationCache.keySet().removeIf(key -> !cacheTimestamps.containsKey(key));
        log.info("Cache cleanup completed");
    }

    // Add new method for scheduled ML training
    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    public void scheduledModelTraining() {
        log.info("Starting scheduled ML model training");
        try {
            trainModel();
            log.info("Scheduled ML model training completed successfully");
        } catch (Exception e) {
            log.error("Error in scheduled ML model training: {}", e.getMessage());
        }
    }
} 