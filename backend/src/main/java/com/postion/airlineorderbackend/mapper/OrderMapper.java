package com.postion.airlineorderbackend.mapper;

import com.postion.airlineorderbackend.dto.OrderDto;
import com.postion.airlineorderbackend.model.Order;
import com.postion.airlineorderbackend.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring") // Generates a Spring Bean for injection
public interface OrderMapper {

    // MapStruct can map nested objects automatically if they have a corresponding mapping method
    @Mappings({
//            @Mapping(source = "author", target = "user"), // Map the user object
//            @Mapping(target = "flightInfo", ignore = true) // flightInfo is added later, not from the entity
    })
    OrderDto toDto(Order order);

    // This method will be used by the above toDto method to map the User entity to UserDto
    OrderDto.UserDto userToUserDto(User user);
}