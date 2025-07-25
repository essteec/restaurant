package com.ste.restaurant.mapper;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.entity.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {})
public interface OrderMapper {
    OrderDto orderToOrderDto(Order order);

    OrderItemDto orderItemToOrderItemDto(OrderItem orderItem);
    UserDto userToUserDto(User user);
    AddressDto addressToAddressDto(Address address);
    TableTopDto tableTopToTableTopDto(TableTop tableTop);
    CallRequestDto callRequestToCallRequestDto(CallRequest callRequest);

    Address addressDtoToAddress(AddressDto addressDto);
    OrderItem orderItemDtoToOrderItem(OrderItemDto orderItemDto);

    OrderItem orderItemDtoBasicToOrderItem(OrderItemDtoBasic orderItemDto);

}
