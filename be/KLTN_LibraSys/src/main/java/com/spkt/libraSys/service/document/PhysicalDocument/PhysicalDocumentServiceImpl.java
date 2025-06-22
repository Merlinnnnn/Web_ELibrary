package com.spkt.libraSys.service.document.PhysicalDocument;

import com.spkt.libraSys.service.document.DocumentEntity;
import com.spkt.libraSys.service.document.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the PhysicalDocumentService interface.
 * Handles the business logic for managing physical documents in the library system.
 */
@Service
@RequiredArgsConstructor
public class PhysicalDocumentServiceImpl implements PhysicalDocumentService {

    private final PhysicalDocumentRepository physicalDocumentRepository;
    private final DocumentRepository documentRepository;
    private final PhysicalDocumentMapper physicalDocumentMapper;

    /**
     * Creates a new physical document by saving both the document entity and physical document entity.
     * @param requestDto The physical document creation request data
     * @return The created physical document response
     */
    @Override
    public PhysicalResponseDto createPhysicalDocument(PhysicalRequestDto requestDto) {
        DocumentEntity documentEntity = physicalDocumentMapper.toDocumentEntity(requestDto);
        documentEntity = documentRepository.save(documentEntity);

        PhysicalDocumentEntity physicalDocument = new PhysicalDocumentEntity();
        physicalDocument.setDocument(documentEntity);
        physicalDocument.setQuantity(requestDto.getQuantity());
        physicalDocument.setIsbn(requestDto.getIsbn());
        physicalDocumentRepository.save(physicalDocument);
        return physicalDocumentMapper.toResponseDto(physicalDocumentRepository.save(physicalDocument));
    }

    /**
     * Retrieves a physical document by its ID.
     * @param id The ID of the physical document to retrieve
     * @return The physical document response
     * @throws RuntimeException if the document is not found
     */
    @Override
    public PhysicalResponseDto getPhysicalDocumentById(Long id) {
        PhysicalDocumentEntity entity = physicalDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return physicalDocumentMapper.toResponseDto(entity);
    }

    /**
     * Retrieves all physical documents with pagination.
     * @param pageable Pagination information
     * @return A page of physical document responses
     */
    @Override
    public Page<PhysicalResponseDto> getAllPhysicalDocuments(Pageable pageable) {
        Page<PhysicalDocumentEntity> entityPage = physicalDocumentRepository.findAll(pageable);
        return entityPage.map(physicalDocumentMapper::toResponseDto);
    }

    /**
     * Updates an existing physical document.
     * @param id The ID of the physical document to update
     * @param requestDto The updated physical document data
     * @return The updated physical document response
     * @throws RuntimeException if the document is not found
     */
    @Override
    public PhysicalResponseDto updatePhysicalDocument(Long id, PhysicalRequestDto requestDto) {
        PhysicalDocumentEntity entity = physicalDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        entity.getDocument().setDocumentName(requestDto.getDocumentName());
        entity.getDocument().setAuthor(requestDto.getAuthor());
        entity.getDocument().setPublisher(requestDto.getPublisher());
        entity.getDocument().setDescription(requestDto.getDescription());
        entity.setQuantity(requestDto.getQuantity());
        entity.setIsbn(requestDto.getIsbn());

        return physicalDocumentMapper.toResponseDto(physicalDocumentRepository.save(entity));
    }

    /**
     * Deletes a physical document from the system.
     * @param id The ID of the physical document to delete
     */
    @Override
    public void deletePhysicalDocument(Long id) {
        physicalDocumentRepository.deleteById(id);
    }

    /**
     * Reserves a physical document by decreasing its quantity.
     * @param id The ID of the physical document to reserve
     * @throws RuntimeException if the document is not found or not available for reservation
     */
    @Override
    @Transactional
    public void reservePhysicalDocument(Long id) {
        PhysicalDocumentEntity entity = physicalDocumentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        
        if (entity.getQuantity() <= 0) {
            throw new RuntimeException("Document is not available for reservation");
        }
        
        entity.setQuantity(entity.getQuantity() - 1);
        physicalDocumentRepository.save(entity);
    }
}
