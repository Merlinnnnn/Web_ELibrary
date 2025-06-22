package com.spkt.libraSys.service.document.programClass;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * The ProgramClassService interface defines behaviors related to program classes.
 */
public interface ProgramClassService {

    /**
     * Saves a list of ProgramClass from an Excel file.
     *
     * @param file Excel file containing ProgramClass data
     */
    ProgramClassUploadResult saveProgramClassesFromExcel(MultipartFile file);

    /**
     * Retrieves ProgramClass information by ID.
     *
     * @param id ID of the ProgramClass
     * @return Detailed information of the ProgramClass
     */
    ProgramClassResponse getProgramClassById(Long id);

    /**
     * Retrieves a list of ProgramClass (paginated).
     *
     * @param pageable Pagination information
     * @return Paginated list of ProgramClass
     */
    Page<ProgramClassResponse> getAllProgramClasses(Pageable pageable);

    /**
     * Creates a new ProgramClass.
     *
     * @param request Data for the new ProgramClass
     * @return The newly created ProgramClass
     */
    ProgramClassResponse createProgramClass(ProgramClassResponse request);

    /**
     * Updates information of a ProgramClass.
     *
     * @param id      ID of the ProgramClass to update
     * @param request New information to update
     * @return Updated ProgramClass
     */
    ProgramClassResponse updateProgramClass(Long id, ProgramClassResponse request);

    /**
     * Deletes a ProgramClass by ID.
     *
     * @param id ID of the ProgramClass to delete
     */
    void deleteProgramClass(Long id);

    /**
     * Deletes a list of ProgramClass by IDs.
     *
     * @param ids List of ProgramClass IDs to delete
     */
    void deleteProgramClasses(List<Long> ids);
}