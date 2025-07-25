# 🍔 Modern Minimal Restaurant System

<div align="center">

![Food Delivery](https://img.shields.io/badge/Food-Delivery-orange)
![Spring Boot](https://img.shields.io/badge/Spring-Boot-green)
![Java](https://img.shields.io/badge/Java-21-orange)

[//]: # (![License]&#40;https://img.shields.io/badge/License-MIT-blue&#41;)

</div>


## 📝 Description

A automated order taken with basic restaurant management system built with Spring Boot, providing RESTful APIs to manage tables, foods with its categories, menus, orders, and users. Perfect for automated order takes, get notified when a customer calls and manage your own restaurant manually for whos are seeking a robust backend solution.

## ✨ Features

- 🍕 **Menu Management**
    - Add, update, delete, and retrieve food items
    - Categorize items option for food item
    - Support for pricing, descriptions, (**TODO** availability status)

- 🛒 **Order Processing**
    - Place new orders with multiple items
    - Track order status (Placed, Preparing, Out for Delivery, Delivered)
    - Order history and details retrieval

- 👤 **User Management**
    - Customer registration and profile management
    - Restaurant owner/partner registration
    - Delivery personnel management
    - User authentication and authorization

- 🏪 **Restaurant Management**
    - Restaurant registration and profile
    - Opening hours, location, and cuisine types
    - Rating and review system

- 🔍 **Search Functionality**
    - Search restaurants by location and cuisine
    - Filter menu items by category, price range, and rating

- 📊 **Additional Features**
    - Global exception handling for consistent error responses
    - Comprehensive logging and monitoring
    - API documentation with Swagger
    - Performance optimizations

## 🔧 Technology Stack

<div align="center">

| Technology                                                                     | Description |
|--------------------------------------------------------------------------------|-------------|
| ![Java](https://img.shields.io/badge/Java-21-orange)                           | Core programming language |
| ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)           | Application framework |
| ![Spring MVC](https://img.shields.io/badge/Spring-MVC-lightgreen)              | Web framework |
| ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data-JPA-yellowgreen) | Data persistence |
| ![H2 Database](https://img.shields.io/badge/H2-Database-blue)                  | In-memory database (configurable) |
| ![Maven](https://img.shields.io/badge/Maven-3.6+-purple)                       | Build tool |

[//]: # (| ![JUnit]&#40;https://img.shields.io/badge/JUnit-5-red&#41;                             | Testing framework |)
[//]: # (| ![Swagger]&#40;https://img.shields.io/badge/Swagger-3.0-darkblue&#41;                  | API documentation |)

</div>


[//]: # (## 🔌 API Endpoints)

[//]: # ()
[//]: # (### Menu Items)

[//]: # ()
[//]: # (| Method | Endpoint | Description |)

[//]: # (|--------|----------|-------------|)

[//]: # (| POST | `/api/menu-items` | Add a new menu item |)

[//]: # (| GET | `/api/menu-items/{id}` | Retrieve a menu item by ID |)

[//]: # (| GET | `/api/menu-items/restaurant/{restaurantId}` | Retrieve menu items by restaurant ID |)

[//]: # (| PUT | `/api/menu-items/{id}` | Update a menu item |)

[//]: # (| DELETE | `/api/menu-items/{id}` | Delete a menu item |)

[//]: # ()
[//]: # (### Orders)

[//]: # ()
[//]: # (| Method | Endpoint | Description |)

[//]: # (|--------|----------|-------------|)

[//]: # (| POST | `/api/orders/place` | Place a new order |)

[//]: # (| GET | `/api/orders/{id}` | Retrieve an order by ID |)

[//]: # (| GET | `/api/orders/status/{status}` | Retrieve orders by status |)

[//]: # (| PUT | `/api/orders/{id}/status` | Update order status |)

[//]: # (| GET | `/api/orders/user/{userId}` | Retrieve orders by user |)

[//]: # ()
[//]: # (### Users)

[//]: # ()
[//]: # (| Method | Endpoint | Description |)

[//]: # (|--------|----------|-------------|)

[//]: # (| POST | `/api/users` | Create a new user |)

[//]: # (| GET | `/api/users/{id}` | Retrieve a user by ID |)

[//]: # (| GET | `/api/users/email/{email}` | Retrieve a user by email |)

[//]: # (| GET | `/api/users` | Retrieve all users |)

[//]: # (| PUT | `/api/users/{id}` | Update user details |)

[//]: # (| DELETE | `/api/users/{id}` | Delete a user |)

[//]: # ()
[//]: # (### Restaurants)

[//]: # ()
[//]: # (| Method | Endpoint | Description |)

[//]: # (|--------|----------|-------------|)

[//]: # (| POST | `/api/restaurants` | Add a new restaurant |)

[//]: # (| GET | `/api/restaurants/{id}` | Get restaurant by ID |)

[//]: # (| GET | `/api/restaurants/location/{location}` | Find restaurants by location |)

[//]: # (| GET | `/api/restaurants/cuisine/{cuisine}` | Find restaurants by cuisine |)

[//]: # (| PUT | `/api/restaurants/{id}` | Update restaurant details |)

[//]: # (| DELETE | `/api/restaurants/{id}` | Delete a restaurant |)

## 🔒 Security & Authentication

The system implements security features to protect user data and restrict access to API endpoints:

- JWT-based authentication
- Role-based authorization 
- HTTPS support for secure communication

  [//]: # (- Password encryption using BCrypt)

[//]: # (## 🧪 Testing)

[//]: # ()
[//]: # (The project includes comprehensive tests to ensure reliability:)

[//]: # (```bash)

[//]: # (# Run all tests)

[//]: # (mvn test)

[//]: # ()
[//]: # (# Run specific test category)

[//]: # (mvn test -Dtest=*ControllerTest)

[//]: # ()
[//]: # (# Generate test coverage report)

[//]: # (mvn jacoco:report)

[//]: # (```)

[//]: # ()
[//]: # (Test coverage can be viewed at `target/site/jacoco/index.html`)


## ⚓ Entity Relationships
#### Category
- @ManyToMany with FoodItem (via category_food_item join table)
#### Menu
- @ManyToMany with FoodItem (via menu_food_item join table)
#### FoodItem
- No direct reference to Category or Menu (inverse side of both relationships)
#### Order
- @ManyToOne to User (customer)
- @ManyToOne to Address
- @ManyToOne to TableTop
- @OneToMany to OrderItem
#### OrderItem
- @ManyToOne to Order
- @ManyToOne to FoodItem
#### User
- @OneToMany to Order (mapped by customer)
- @OneToMany to Address (via user_address join table)
#### TableTop
- Referenced by Order (as table)
#### Address
- Referenced by User and Order

## 🚀 Future Enhancements

- [ ] Restaurant analytics dashboard
- [ ] Multi-language support
- [ ] Real-time order tracking with WebSockets
- [ ] Customer loyalty program

