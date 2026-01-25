-- 샘플 사용자 데이터
INSERT INTO users (name, email, status) VALUES
('John Doe', 'john@example.com', 'ACTIVE'),
('Jane Smith', 'jane@example.com', 'ACTIVE'),
('Bob Wilson', 'bob@example.com', 'INACTIVE'),
('Alice Brown', 'alice@example.com', 'ACTIVE');

-- 샘플 카테고리 데이터
INSERT INTO categories (name, description) VALUES
('Electronics', '전자 제품'),
('Books', '도서'),
('Clothing', '의류');

-- 샘플 상품 데이터
INSERT INTO products (name, description, price, stock_quantity, category_id) VALUES
('MacBook Pro', '14인치 M3 Pro', 2499000, 10, 1),
('iPhone 15', '256GB', 1350000, 50, 1),
('Clean Code', '로버트 마틴의 클린 코드', 33000, 100, 2),
('jOOQ 마스터', 'jOOQ 완벽 가이드', 45000, 30, 2),
('기본 티셔츠', '면 100%', 29000, 200, 3);

-- 샘플 주문 데이터
INSERT INTO orders (user_id, total_amount, status) VALUES
(1, 2532000, 'COMPLETED'),
(1, 1350000, 'PENDING'),
(2, 78000, 'COMPLETED');

-- 샘플 주문 상품 데이터
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(1, 1, 1, 2499000),
(1, 3, 1, 33000),
(2, 2, 1, 1350000),
(3, 3, 1, 33000),
(3, 4, 1, 45000);
