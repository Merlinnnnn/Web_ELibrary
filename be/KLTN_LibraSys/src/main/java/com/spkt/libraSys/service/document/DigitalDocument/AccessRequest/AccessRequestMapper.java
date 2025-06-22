package com.spkt.libraSys.service.document.DigitalDocument.AccessRequest;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccessRequestMapper {
    //@Mapping(source = "id", target = "id")
    AccessRequestResponseDto entityToDto(AccessRequestEntity entity);

    List<AccessRequestResponseDto> toDtoList(List<AccessRequestEntity> entities);
}
