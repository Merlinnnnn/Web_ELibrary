package com.spkt.libraSys.service.document.course;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    CourseEntity toEntity(CourseRequest request);

    CourseResponse toResponse(CourseEntity entity);
}
