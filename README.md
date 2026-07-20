# Suuq API

A full-featured e-commerce REST API built with Spring Boot 3.3.11, featuring role-based access control, product management, shopping cart, order lifecycle, Paystack payment integration, and email notifications.

> **Suuq** (سوق) is an Arabic/Hausa word for market — reflecting the Nigerian market context this API is designed for.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.3.11 |
| Language | Java 21 |
| Security | Spring Security + JWT (JJWT 0.12.3) |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Payments | Paystack |
| Email | Spring Mail (Gmail SMTP) |
| Documentation | Swagger UI (springdoc-openapi 2.5.0) |
| Build | Maven |
| Testing | JUnit 5 + Mockito |

---

## Features

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (ADMIN / CUSTOMER)
- Method-level security with `@PreAuthorize`

### User Management
- User registration and login
- View and update profile
- Change password (with current password verification)
- Forgot password (email reset link, 15-minute expiry)
- Reset password via token
- Admin: view all users, activate/deactivate accounts

### Product Management
- Full CRUD (admin only for write operations)
- Category management
- Soft delete (products are deactivated, not removed)
- Product search with filtering by name, description, category, and price range
- Pagination and sorting

### Shopping Cart
- Add, update, and remove items
- Quantity validation against stock
- Cart auto-created on first item add
- Cart cleared automatically after checkout

### Order Management
- Checkout from cart
- Order history (customer) and all orders (admin)
- Order status lifecycle: PENDING → PAID → PROCESSING → SHIPPED → DELIVERED
- Admin: manually update order status

### Payments (Paystack)
- Initialize payment (returns Paystack checkout URL)
- Manual payment verification by reference
- Webhook handler for automatic order status updates on payment success

### Product Reviews
- Submit reviews (only for purchased and paid products)
- One review per product per customer (enforced at database level)
- Rating validation (1–5 stars)
- Average rating and review count per product
- Delete review (author or admin only)

### Email Notifications
- Order confirmation on checkout
- Password changed confirmation
- Password reset link

---

## Architecture

The project follows a **feature-based package structure** with a layered architecture within each feature:
com.peter.suuq
- auth          - Registration, login, JWT
- cart          - Cart and cart item management
- config        - Security, Swagger, WebClient config
- exception     - Global exception handling
- order         - Order lifecycle
- payment       - Paystack integration
- product       - Products and categories
- review        - Product reviews and ratings 
- user          - User profile, password, admin

Each feature package contains:
- controller    - REST endpoints
- dto           - Request/response objects
- entity        - JPA entities
- repository    - Spring Data JPA repositories
- service       - Business logic

---

## Getting Started

### Prerequisites
- Java 21
- Maven
- PostgreSQL
- A Paystack account (free at [paystack.com](https://paystack.com))
- A Gmail account with App Password enabled

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/yourusername/suuq-api.git
cd suuq-api
```

**2. Create the database**
```sql
CREATE DATABASE suuq_db;
```

**3. Configure environment**

Copy the example properties file:
```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Then fill in your values in `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/suuq_db
spring.datasource.username=your_postgres_username
spring.datasource.password=your_postgres_password

jwt.secret=your_jwt_secret_at_least_32_characters_long
jwt.expiration=86400000

paystack.secret.key=sk_test_your_paystack_secret_key
paystack.base.url=https://api.paystack.co

spring.mail.username=your_gmail@gmail.com
spring.mail.password=your_gmail_app_password

app.base.url=http://localhost:8080
```

**4. Run the application**
```bash
./mvnw spring-boot:run
```

The API will start on `http://localhost:8080`

**5. Access Swagger UI**

http://localhost:8080/swagger-ui.html

---

## Authentication

This API uses JWT Bearer token authentication.

**1. Register or login to get a token:**
```http
POST /api/auth/register
POST /api/auth/login
```

**2. Include the token in all subsequent requests:**

Authorization: Bearer your_token_here

**3. In Swagger UI:** Click the **Authorize** button (padlock icon) and enter:
Bearer your_token_here

---

## API Endpoints

### Auth
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register new account |
| POST | `/api/auth/login` | Public | Login and get token |
| POST | `/api/auth/forgot-password` | Public | Request password reset email |
| POST | `/api/auth/reset-password` | Public | Reset password with token |

### Users
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/users/me` | Any | View own profile |
| PUT | `/api/users/me` | Any | Update own profile |
| PUT | `/api/users/me/password` | Any | Change password |
| GET | `/api/admin/users` | Admin | List all users |
| GET | `/api/admin/users/{id}` | Admin | Get user by ID |
| PUT | `/api/admin/users/{id}/activate` | Admin | Activate user |
| PUT | `/api/admin/users/{id}/deactivate` | Admin | Deactivate user |

### Products
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/products` | Any | List all active products |
| GET | `/api/products/{id}` | Any | Get product by ID |
| GET | `/api/products/search` | Any | Search and filter products |
| GET | `/api/products/category/{categoryId}` | Any | Products by category |
| POST | `/api/admin/products` | Admin | Create product |
| PUT | `/api/admin/products/{id}` | Admin | Update product |
| DELETE | `/api/admin/products/{id}` | Admin | Soft delete product |

### Categories
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/categories` | Any | List all categories |
| POST | `/api/admin/categories` | Admin | Create category |

### Cart
| Method | Endpoint | Access | Description |
|---|---|---|---|
| GET | `/api/cart` | Customer | View cart |
| POST | `/api/cart/items` | Customer | Add item to cart |
| PUT | `/api/cart/items/{cartItemId}` | Customer | Update item quantity |
| DELETE | `/api/cart/items/{cartItemId}` | Customer | Remove item from cart |

### Orders
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/orders` | Customer | Place order from cart |
| GET | `/api/orders` | Customer | View own order history |
| GET | `/api/orders/{orderId}` | Customer | Get order by ID |
| GET | `/api/admin/orders` | Admin | View all orders |
| PUT | `/api/admin/orders/{orderId}/status` | Admin | Update order status |

### Payments
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/payments/initialize/{orderId}` | Customer | Initialize Paystack payment |
| POST | `/api/payments/verify/{reference}` | Customer | Verify payment by reference |
| POST | `/api/payments/webhook` | Public | Paystack webhook handler |

### Reviews
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/products/{productId}/reviews` | Customer | Submit a review |
| GET | `/api/products/{productId}/reviews` | Any | Get product reviews |
| GET | `/api/products/{productId}/rating` | Any | Get average rating |
| DELETE | `/api/reviews/{reviewId}` | Author/Admin | Delete a review |

### Search Query Parameters

GET /api/products/search?name=earbuds&categoryId=1&minPrice=5000&maxPrice=50000&page=0&size=10&sortBy=price&sortDir=asc

| Parameter | Type | Required | Description |
|---|---|---|---|
| `name` | String | No | Search in name and description |
| `categoryId` | Long | No | Filter by category |
| `minPrice` | BigDecimal | No | Minimum price |
| `maxPrice` | BigDecimal | No | Maximum price |
| `page` | int | No | Page number (default: 0) |
| `size` | int | No | Page size (default: 10) |
| `sortBy` | String | No | Field to sort by (default: id) |
| `sortDir` | String | No | asc or desc (default: asc) |

---

## Running Tests

```bash
./mvnw test
```

26 unit tests across 5 service classes:
- `AuthServiceTest` — registration, duplicate email, login
- `ProductServiceTest` — CRUD, soft delete, category validation
- `CartServiceTest` — add/update/remove items, stock validation
- `OrderServiceTest` — checkout, empty cart, access control, status updates
- `ReviewServiceTest` — purchase verification, duplicate review, delete authorization, ratings

---

## Key Design Decisions

**Why `BigDecimal` for prices?**
Floating point types (`double`, `float`) have precision issues with decimal arithmetic. `BigDecimal` is exact and is the standard for monetary values in Java.

**Why soft delete for products?**
Deleting a product that appears in existing orders would break order history. Setting `active = false` preserves data integrity while hiding the product from customers.

**Why feature-based package structure?**
Organizing by feature (`product`, `order`, `cart`) rather than by layer (`entity`, `repository`, `service`) makes the codebase easier to navigate and closer to how real production codebases are structured.

**Why both webhook and verify for payments?**
Webhooks provide instant automatic updates when Paystack confirms payment. The verify endpoint acts as a reliable fallback — if a webhook is missed or delayed, payments can be confirmed manually. Paystack recommends this dual approach in production.

**Why purchase verification for reviews?**
Restricting reviews to customers who actually paid for a product prevents fake reviews and maintains trust in the rating system.

---

## Author

**Obi Peter Somto**
- GitHub: [@Petersobi](https://github.com/petersobi)
- LinkedIn: [linkedin.com/in/peter-obi-9425771b5](https://linkedin.com/in/peter-obi-9425771b5)

---

## License

This project is open source and available under the [MIT License](LICENSE).