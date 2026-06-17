CREATE TABLE favorite_foods (
                                favorite_food_id BIGINT AUTO_INCREMENT PRIMARY KEY,

                                user_id BIGINT NOT NULL,

                                food_name VARCHAR(100) NOT NULL,
                                amount VARCHAR(50) NULL,

                                calories INT NULL,
                                carbohydrate DECIMAL(8,2) NULL,
                                protein DECIMAL(8,2) NULL,
                                fat DECIMAL(8,2) NULL,

                                shopping_category VARCHAR(50) NULL,
                                shopping_keyword VARCHAR(100) NULL,

                                source_food_id VARCHAR(100) NULL,

                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,

                                CONSTRAINT fk_favorite_foods_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(user_id)
                                        ON DELETE CASCADE,

                                CONSTRAINT uk_favorite_foods_user_name_amount
                                    UNIQUE (user_id, food_name, amount)
);