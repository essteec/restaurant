package com.ste.restaurant.mapper;

import com.ste.restaurant.dto.*;
import com.ste.restaurant.dto.userdto.UserDto;
import com.ste.restaurant.dto.userdto.UserDtoCustomer;
import com.ste.restaurant.dto.userdto.UserDtoEmployee;
import com.ste.restaurant.dto.userdto.UserDtoIO;
import com.ste.restaurant.entity.*;

import org.mapstruct.*;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {})
public interface OrderMapper {
    Address addressDtoToAddress(AddressDto addressDto);
    AddressDto addressToAddressDto(Address address);
    List<AddressDto> addressesToAddressDtos(List<Address> addresses);

    Category categoryDtoBasicToCategory(CategoryDtoBasic category);
    CategoryDto categoryToCategoryDto(Category category);
    CategoryDtoBasic categoryToCategoryDtoBasic(Category category);
    Set<CategoryDtoBasic> categoriesToCategoryDtoBasics(Set<Category> categories);
    
    CategoryTranslation categoryTranslationDtoToCategoryTranslation(CategoryTranslationDto translationDto);
    @Mapping(target = "languageCode", source = "categoryTranslationId.languageCode")
    CategoryTranslationDto categoryTranslationToCategoryTranslationDto(CategoryTranslation translation);
    List<CategoryTranslationDto> categoryTranslationsToCategoryTranslationDtos(List<CategoryTranslation> translations);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategoryTranslationFromDto(CategoryTranslationDto translationDto, @MappingTarget CategoryTranslation translation);

    FoodItem foodItemDtoToFoodItem(FoodItemDto foodItemDto);
    FoodItemDto foodItemToFoodItemDto(FoodItem foodItem);
    List<FoodItemDto> foodItemsToFoodItemDtos(List<FoodItem> foodItems);
    FoodItemMenuDto foodItemToFoodItemMenuDto(FoodItem food);

    FoodItemTranslation foodItemTranslationDtoToFoodItemTranslation(FoodItemTranslationDto foodItemTranslationDto);
    @Mapping(target = "languageCode", source = "foodItemTranslationId.languageCode")
    FoodItemTranslationDto foodItemTranslationToFoodItemTranslationDto(FoodItemTranslation foodItemTranslation);
    List<FoodItemTranslationDto> foodItemTranslationsToFoodItemTranslationDtos(List<FoodItemTranslation> translations);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFoodItemTranslationFromDto(FoodItemTranslationDto translationDto, @MappingTarget FoodItemTranslation translation);

    User userDtoIOToUser(UserDtoIO userDtoIO);
    UserDto userToUserDto(User user);
    UserDtoCustomer userToUserDtoCustomer(User user);
    UserDtoEmployee userToUserDtoEmployee(User user);

    TableTopDto tableTopToTableTopDto(TableTop tableTop);
    List<TableTopDto> tableTopsToTableTopDtos(List<TableTop> tableTops);

    CallRequest callRequestDtoBasicToCallRequest(CallRequestDtoBasic callRequestDtoBasic);
    CallRequestDto callRequestToCallRequestDto(CallRequest callRequest);

    OrderDto orderToOrderDto(Order order);
    List<OrderDto> ordersToOrderDtos(List<Order> orders);
    OrderItem orderItemDtoBasicToOrderItem(OrderItemDtoBasic orderItemDto);
    OrderItemDto orderItemToOrderItemDto(OrderItem orderItem);
    List<OrderItemDto> orderItemsToOrderItemDtos(List<OrderItem> orderItems);
    Menu menuDtoBasicToMenu(MenuDtoBasic menuDtoBasic);
    MenuDto menuToMenuDto(Menu menu);
    MenuDtoBasic menuToMenuDtoBasic(Menu savedMenu);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCategoryFromDto(CategoryDtoBasic categoryDtoBasic, @MappingTarget Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateTableFromDto(TableTopDto tableTopDto, @MappingTarget TableTop tableTop);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDtoIO(UserDtoIO userDtoIO, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDtoEmployee(UserDtoEmployee userDtoEmployee, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFoodItemFromDto(FoodItemDto foodItem, @MappingTarget FoodItem foodItemOld);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateMenuFromDto(MenuDtoBasic menu, @MappingTarget Menu menuOld);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateAddressFromDto(AddressDto addressDto, @MappingTarget Address address);
}
