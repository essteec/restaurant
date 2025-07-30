# 🍔 Modern Minimal Restaurant System

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring-Boot-green)
![Java](https://img.shields.io/badge/Java-21-orange)

</div>

---

## 📝 Description

A modern, minimal, automated restaurant management system built with Spring Boot, providing RESTful APIs to manage tables, food items, categories, menus, orders, and users. Designed for efficient order taking, real-time call requests, and robust backend management for restaurant operations.

> **Note:** A frontend project is planned but not yet available.

---

## ✨ Features

### 🍕 Menu Management
- Add, update, delete, and retrieve food items
- Categorize food items
- Set pricing and descriptions
- **Planned:** Food item availability status

### 🛒 Order Processing
- Place new orders with multiple items
- Track order status (PLACED, PREPARING, COMPLETED, CANCELLED, etc.)
- Retrieve order history and details
- **Planned:** Change table for an existing order
- **Planned:** Add items to an existing order

### ✋ Call Request Processing
- Customers can call for waiter assistance with specific requests and notes

### 👤 User Management
- Customer registration and profile management
- Restaurant owner/partner registration
- Delivery personnel management
- User authentication and authorization (JWT-based)

### 📊 Additional Features
- API documentation with Swagger/OpenAPI
- **Planned:** Restaurant analytics dashboard
- **Planned:** Global exception handling for consistent error responses
- **Planned:** Real-time order tracking (WebSockets)
- **Planned:** Customer loyalty program

---

## 🔧 Technology Stack

| Technology   | Description                |
|--------------|---------------------------|
| Java 21      | Core programming language |
| Spring Boot  | Application framework     |
| Maven        | Build tool                |
| Swagger      | API documentation         |

---

## 🔒 Security & Authentication

- Password encryption using BCrypt
- JWT-based authentication
- Role-based authorization (ADMIN, CUSTOMER, etc.)

---

## ⚓ Entity Relationships

| Entity     | Relationships                                                                                 |
|------------|----------------------------------------------------------------------------------------------|
| Category   | Many-to-Many with **FoodItem** (via `category_food_item` join table)                         |
| Menu       | Many-to-Many with **FoodItem** (via `menu_food_item` join table)                             |
| FoodItem   | Inverse side of Many-to-Many with **Category** and **Menu**                                  |
| Order      | Many-to-One to **User** (customer) <br> Many-to-One to **Address** <br> Many-to-One to **TableTop** <br> One-to-Many to **OrderItem** |
| OrderItem  | Many-to-One to **Order** <br> Many-to-One to **FoodItem**                                    |
| User       | One-to-Many to **Order** (mapped by customer) <br> One-to-Many to **Address** (via `user_address` join table) |
| TableTop   | Referenced by **Order** (as table)                                                           |
| Address    | Referenced by **User** and **Order**                                                         |

---

## 🚀 Future Enhancements

- Restaurant analytics dashboard
- Multi-language support
- Real-time order tracking with WebSockets
- Customer loyalty program
- Improved test coverage
- Frontend client

---


## 📂 Data Seeding

Sample data is provided in the `src/main/resources/seed-*.json` files and loaded via the `DataSeeder` class for development and testing.

---
