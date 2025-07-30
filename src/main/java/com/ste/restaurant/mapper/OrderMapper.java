package com.ste.restaurant.mapper;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.dto.userdto.UserDtoCustomer;
import com.ste.restaurant.dto.userdto.UserDtoEmployee;
import com.ste.restaurant.entity.*;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {})
public interface OrderMapper {
    Address addressDtoToAddress(AddressDto addressDto);

    UserDto userToUserDto(User user);
    UserDtoCustomer userToUserDtoCustomer(User user);
    UserDtoEmployee userToUserDtoEmployee(User user);

    TableTopDto tableTopToTableTopDto(TableTop tableTop);

    AddressDto addressToAddressDto(Address address);

    CallRequestDto callRequestToCallRequestDto(CallRequest callRequest);

    OrderDto orderToOrderDto(Order order);
    OrderItemDto orderItemToOrderItemDto(OrderItem orderItem);
    OrderItem orderItemDtoToOrderItem(OrderItemDto orderItemDto);
    OrderItem orderItemDtoBasicToOrderItem(OrderItemDtoBasic orderItemDto);

}
