package com.ste.restaurant.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ste.restaurant.entity.*;
import com.ste.restaurant.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final AddressRepository addressRepository;
    private final CategoryRepository categoryRepository;
    private final FoodItemRepository foodItemRepository;
    private final MenuRepository menuRepository;
    private final TableTopRepository tableTopRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CallRequestRepository callRequestRepository;
    private final PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public DataSeeder(
            AddressRepository addressRepository,
            CategoryRepository categoryRepository,
            FoodItemRepository foodItemRepository,
            MenuRepository menuRepository,
            TableTopRepository tableTopRepository,
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CallRequestRepository callRequestRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.addressRepository = addressRepository;
        this.categoryRepository = categoryRepository;
        this.foodItemRepository = foodItemRepository;
        this.menuRepository = menuRepository;
        this.tableTopRepository = tableTopRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.callRequestRepository = callRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting database seeding...");
        seedAddresses();
        Map<String, Address> addressMap = getAddressMap();
        seedCategories();
        Map<String, Category> categoryMap = getCategoryMap();
        seedFoodItems(categoryMap);
        Map<String, FoodItem> foodItemMap = getFoodItemMap();
        seedMenus(foodItemMap);
        seedTables();
        Map<String, TableTop> tableMap = getTableMap();
        seedUsers(addressMap);
        Map<String, User> userMap = getUserMap();
        seedOrders(userMap, addressMap, tableMap);
        List<Order> allOrders = orderRepository.findAll();
        seedOrderItems(foodItemMap, allOrders);
        seedCallRequests();
        logger.info("Database seeding completed.");
    }

    private void seedAddresses() throws IOException {
        if (addressRepository.count() == 0) {
            logger.info("Seeding addresses...");
            List<Address> addresses = readJsonList("seed-addresses.json", new TypeReference<List<Address>>() {});
            addressRepository.saveAll(addresses);
        }
    }

    private Map<String, Address> getAddressMap() {
        Map<String, Address> map = new HashMap<>();
        for (Address a : addressRepository.findAll()) {
            map.put(a.getName(), a);
        }
        return map;
    }

    private void seedCategories() throws IOException {
        if (categoryRepository.count() == 0) {
            logger.info("Seeding categories...");
            List<Map<String, Object>> categoriesRaw = readJsonList("seed-categories.json", new TypeReference<List<Map<String, Object>>>() {});
            List<Category> categories = new ArrayList<>();
            for (Map<String, Object> raw : categoriesRaw) {
                Category c = new Category();
                c.setCategoryName((String) raw.get("categoryName"));
                categories.add(c);
            }
            categoryRepository.saveAll(categories);
        }
    }

    private Map<String, Category> getCategoryMap() {
        Map<String, Category> map = new HashMap<>();
        for (Category c : categoryRepository.findAll()) {
            map.put(c.getCategoryName(), c);
        }
        return map;
    }

    private void seedFoodItems(Map<String, Category> categoryMap) throws IOException {
        if (foodItemRepository.count() == 0) {
            logger.info("Seeding food items...");
            List<Map<String, Object>> foodItemsRaw = readJsonList("seed-fooditems.json", new TypeReference<List<Map<String, Object>>>() {});
            List<FoodItem> foodItems = new ArrayList<>();
            for (Map<String, Object> raw : foodItemsRaw) {
                FoodItem fi = new FoodItem();
                fi.setFoodName((String) raw.get("foodName"));
                fi.setDescription((String) raw.get("description"));
                fi.setPrice(new BigDecimal(raw.get("price").toString()));
                foodItems.add(fi);
            }
            foodItemRepository.saveAll(foodItems);
            // Now set up category associations
            for (Map<String, Object> raw : foodItemsRaw) {
                String foodName = (String) raw.get("foodName");
                List<String> categories = (List<String>) raw.get("categories");
                if (categories != null) {
                    FoodItem fi = foodItemRepository.findAll().stream().filter(f -> f.getFoodName().equals(foodName)).findFirst().orElse(null);
                    if (fi != null) {
                        for (String catName : categories) {
                            Category cat = categoryMap.get(catName);
                            if (cat != null) {
                                if (cat.getFoodItems() == null) cat.setFoodItems(new HashSet<>());
                                cat.getFoodItems().add(fi);
                                categoryRepository.save(cat);
                            }
                        }
                    }
                }
            }
        }
    }

    private Map<String, FoodItem> getFoodItemMap() {
        Map<String, FoodItem> map = new HashMap<>();
        for (FoodItem f : foodItemRepository.findAll()) {
            map.put(f.getFoodName(), f);
        }
        return map;
    }

    private void seedMenus(Map<String, FoodItem> foodItemMap) throws IOException {
        if (menuRepository.count() == 0) {
            logger.info("Seeding menus...");
            List<Map<String, Object>> menusRaw = readJsonList("seed-menus.json", new TypeReference<List<Map<String, Object>>>() {});
            List<Menu> menus = new ArrayList<>();
            for (Map<String, Object> raw : menusRaw) {
                Menu menu = new Menu();
                menu.setMenuName((String) raw.get("menuName"));
                menu.setDescription((String) raw.get("description"));
                menu.setActive(Boolean.TRUE.equals(raw.get("active")));
                Set<FoodItem> menuItems = new HashSet<>();
                if (raw.get("foodItems") instanceof List) {
                    for (String foodName : (List<String>) raw.get("foodItems")) {
                        FoodItem fi = foodItemMap.get(foodName);
                        if (fi != null) menuItems.add(fi);
                    }
                }
                menu.setFoodItems(menuItems);
                menus.add(menu);
            }
            menuRepository.saveAll(menus);
        }
    }

    private void seedTables() throws IOException {
        if (tableTopRepository.count() == 0) {
            logger.info("Seeding tables...");
            List<Map<String, Object>> tablesRaw = readJsonList("seed-tables.json", new TypeReference<List<Map<String, Object>>>() {});
            List<TableTop> tables = new ArrayList<>();
            for (Map<String, Object> raw : tablesRaw) {
                TableTop t = new TableTop();
                t.setTableNumber((String) raw.get("tableNumber"));

                t.setCapacity((Integer) raw.get("capacity"));

                Object statusRaw = raw.get("tableStatus");
                t.setTableStatus(statusRaw != null ? TableStatus.valueOf((String) statusRaw) : TableStatus.AVAILABLE); // or whatever your default is

                tables.add(t);
            }
            tableTopRepository.saveAll(tables);
        }
    }

    private Map<String, TableTop> getTableMap() {
        Map<String, TableTop> map = new HashMap<>();
        for (TableTop t : tableTopRepository.findAll()) {
            map.put(t.getTableNumber(), t);
        }
        return map;
    }

    private void seedUsers(Map<String, Address> addressMap) throws IOException, ParseException {
        if (userRepository.count() == 0) {
            logger.info("Seeding users...");
            List<Map<String, Object>> usersRaw = readJsonList("seed-users.json", new TypeReference<List<Map<String, Object>>>() {});
            List<User> users = new ArrayList<>();
            for (Map<String, Object> raw : usersRaw) {
                User u = new User();
                u.setFirstName((String) raw.get("firstName"));
                u.setLastName((String) raw.get("lastName"));
                u.setEmail((String) raw.get("email"));
                u.setPassword(passwordEncoder.encode((String) raw.get("password")));
                u.setRole(UserRole.valueOf((String) raw.get("role")));
                if (raw.get("birthday") != null) u.setBirthday(dateFormat.parse((String) raw.get("birthday")));
                if (raw.get("loyaltyPoints") != null) u.setLoyaltyPoints((Integer) raw.get("loyaltyPoints"));
                if (raw.get("salary") != null) u.setSalary(new BigDecimal(raw.get("salary").toString()));
                List<Address> userAddresses = new ArrayList<>();
                if (raw.get("addresses") instanceof List) {
                    for (String addrName : (List<String>) raw.get("addresses")) {
                        Address addr = addressMap.get(addrName);
                        if (addr != null) userAddresses.add(addr);
                    }
                }
                u.setAddresses(userAddresses);
                users.add(u);
            }
            userRepository.saveAll(users);
        }
    }

    private Map<String, User> getUserMap() {
        Map<String, User> map = new HashMap<>();
        for (User u : userRepository.findAll()) {
            map.put(u.getEmail(), u);
        }
        return map;
    }

    private void seedOrders(Map<String, User> userMap, Map<String, Address> addressMap, Map<String, TableTop> tableMap) throws IOException {
        if (orderRepository.count() == 0) {
            logger.info("Seeding orders...");
            List<Map<String, Object>> ordersRaw = readJsonList("seed-orders.json", new TypeReference<List<Map<String, Object>>>() {});
            List<Order> orders = new ArrayList<>();
            for (Map<String, Object> raw : ordersRaw) {
                Order o = new Order();
                o.setOrderTime(LocalDateTime.parse((String) raw.get("orderTime"), dateTimeFormat));
                o.setStatus(OrderStatus.valueOf((String) raw.get("status")));
                o.setTotalPrice(new BigDecimal(raw.get("totalPrice").toString()));
                o.setNotes((String) raw.get("notes"));
                if (raw.get("customer") != null) {
                    User user = userMap.get(raw.get("customer"));
                    if (user != null) o.setCustomer(user);
                }
                if (raw.get("address") != null) {
                    Address addr = addressMap.get(raw.get("address"));
                    if (addr != null) o.setAddress(addr);
                }
                if (raw.get("table") != null) {
                    TableTop table = tableMap.get(raw.get("table"));
                    if (table != null) o.setTable(table);
                }
                orders.add(o);
            }
            orderRepository.saveAll(orders);
        }
    }

    private void seedOrderItems(Map<String, FoodItem> foodItemMap, List<Order> allOrders) throws IOException {
        if (orderItemRepository.count() == 0) {
            logger.info("Seeding order items...");
            List<Map<String, Object>> orderItemsRaw = readJsonList("seed-orderitems.json", new TypeReference<List<Map<String, Object>>>() {});
            List<OrderItem> orderItems = new ArrayList<>();
            for (Map<String, Object> raw : orderItemsRaw) {
                OrderItem oi = new OrderItem();
                oi.setQuantity((Integer) raw.get("quantity"));
                oi.setUnitPrice(new BigDecimal(raw.get("unitPrice").toString()));
                oi.setTotalPrice(new BigDecimal(raw.get("totalPrice").toString()));
                if (raw.get("foodItem") != null) {
                    FoodItem fi = foodItemMap.get(raw.get("foodItem"));
                    if (fi != null) oi.setFoodItem(fi);
                }
                if (raw.get("order") != null) {
                    int orderIdx = ((Number) raw.get("order")).intValue() - 1;
                    if (orderIdx >= 0 && orderIdx < allOrders.size()) {
                        oi.setOrder(allOrders.get(orderIdx));
                    }
                }
                orderItems.add(oi);
            }
            orderItemRepository.saveAll(orderItems);
        }
    }

    private void seedCallRequests() throws IOException {
        if (callRequestRepository.count() == 0) {
            logger.info("Seeding call requests...");
            List<Map<String, Object>> callsRaw = readJsonList("seed-callrequests.json", new TypeReference<List<Map<String, Object>>>() {});
            List<CallRequest> calls = new ArrayList<>();
            for (Map<String, Object> raw : callsRaw) {
                CallRequest cr = new CallRequest();
                cr.setType(RequestType.valueOf((String) raw.get("type")));
                cr.setActive((Boolean) raw.get("active"));
                cr.setCreatedAt(LocalDateTime.parse((String) raw.get("createdAt"), dateTimeFormat));
                calls.add(cr);
            }
            callRequestRepository.saveAll(calls);
        }
    }

    // Generic helper to read a JSON list from the classpath
    private <T> T readJsonList(String resource, TypeReference<T> typeRef) throws IOException {
        try (InputStream is = new ClassPathResource(resource).getInputStream()) {
            return mapper.readValue(is, typeRef);
        }
    }
}
