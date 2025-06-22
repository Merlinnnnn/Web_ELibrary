package com.spkt.libraSys.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class PageDTO<T> {
    private List<T> content;          // Danh sách nội dung của trang hiện tại
    private int pageNumber;           // Số trang hiện tại (bắt đầu từ 0)
    private int pageSize;             // Kích thước trang (số mục trong mỗi trang)
    private long totalElements;       // Tổng số mục có thể phân trang
    private int totalPages;           // Tổng số trang
    private boolean last;             // Có phải trang cuối cùng không
    private List<SortDetail> sortDetails; // Thông tin chi tiết về sắp xếp

    public PageDTO(Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
        this.sortDetails = getSortDetails(page.getSort());
    }

    // Lấy thông tin chi tiết về sắp xếp
    private List<SortDetail> getSortDetails(Sort sort) {
        return sort.stream()
                .map(order -> new SortDetail(order.getProperty(), order.getDirection().name()))
                .collect(Collectors.toList());
    }

    @Getter
    @Setter
    public static class SortDetail {
        private String property;  // Tên của cột sắp xếp
        private String direction; // Chiều sắp xếp (ASC hoặc DESC)

        public SortDetail(String property, String direction) {
            this.property = property;
            this.direction = direction;
        }
    }
}
