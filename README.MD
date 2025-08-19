# 🍔 Modern Minimal Restaurant System

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring-Boot-green)
![Java](https://img.shields.io/badge/Java-17-orange)

</div>

---

## 📝 Description

A modern, minimal, automated restaurant management system built with Spring Boot. It provides a comprehensive set of RESTful APIs to manage tables, food items, categories, menus, orders, and users. The system is designed for efficient order-taking, real-time call requests, and robust backend management for restaurant operations.

> **Note:** A Frontend for this project: [Link will be available soon] 

---

## ✨ Features

### 🍕 Menu Management
- Add, update, delete, and retrieve food items and categories
- Set pricing, descriptions, and images for food items

### 🛒 Order Processing
- Place new orders with multiple items for table or delivery
- Track order status (PLACED, PREPARING, COMPLETED, CANCELLED, etc.)
- Retrieve order history and details
- Change table for an existing order

### ✋ Call Request Processing
- Customers can call for waiter assistance with specific requests (e.g., water, payment) and custom notes.

### 👤 User Management
- Customer and employee (Waiter, Chef, Admin) registration and profile management
- User authentication and authorization (JWT-based)

### 📊 Restaurant Analytics
- Admin dashboard with statistics on revenue, top-performing items, and busiest tables.

### 🛠️ Additional Features
- API documentation with Swagger/OpenAPI
- Global exception handling for consistent error responses
- Backend infrastructure for a customer loyalty program (point system)
- **Planned:** Real-time order tracking (WebSockets)

---

## 🔧 Technology Stack

| Technology      | Description                 |
|-----------------|-----------------------------|
| Java 17         | Core programming language   |
| Spring Boot     | Application framework       |
| Maven           | Build tool                  |
| PostgreSQL      | Database                    |
| Lombok          | Boilerplate code reduction  |
| Swagger/OpenAPI | API documentation           |

---

## 🚀 Installation and Setup

1.  Navigate to the `restaurant` directory.
2.  Make sure you have Java 17 and Maven installed.
3.  Set up a PostgreSQL database and configure the connection details in `src/main/resources/application.properties`.
4.  Run the application using your IDE or with the command: `mvn spring-boot:run`

---

## 📖 API Documentation

The API documentation is generated using Swagger/OpenAPI. Once the application is running, you can access the Swagger UI at:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

---

## 🔒 Security & Authentication

- Password encryption using BCrypt
- JWT-based authentication
- Role-based authorization (ADMIN, CHEF, CUSTOMER, WAITER)

---

## ⚓ Entity Relationships

| Entity     | Relationships                                                                                 |
|------------|----------------------------------------------------------------------------------------------|
| Category   | Many-to-Many with **FoodItem** (via `category_food_item` join table)                         |
| Menu       | Many-to-Many with **FoodItem** (via `menu_food_item` join table)                             |
| FoodItem   | Inverse side of Many-to-Many with **Category** and **Menu**                                  |
| Order      | Many-to-One to **User** (customer) <br> Many-to-One to **Address** <br> Many-to-One to **TableTop** <br> One-to-Many to **OrderItem** |
| OrderItem  | Many-to-One to **Order** <br> Many-to-One to **FoodItem**                                    |
| User       | One-to-Many to **Order** (mapped by customer) <br> One-to-Many to **Address** (unidirectional) |
| TableTop   | Referenced by **Order** (as table)                                                           |
| Address    | Referenced by **User** and **Order**                                                         |

---

## 🚀 Future Enhancements

- Real-time order tracking with WebSockets
- AI assistant to admin user for auto suggest some fields
- Multi-language support with AI
- Improved test coverage

---

## 📂 Data Seeding

Sample data is provided in the `src/main/resources/seed-*.json` files and loaded via the `DataSeeder` class for development and testing.

---