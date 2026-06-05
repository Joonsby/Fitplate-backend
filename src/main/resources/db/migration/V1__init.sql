CREATE TABLE `users` (
                         `user_id` bigint NOT NULL AUTO_INCREMENT,
                         `toss_user_key` varchar(100) NOT NULL,
                         `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (`user_id`),
                         UNIQUE KEY `uk_users_toss_user_key` (`toss_user_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_profiles` (
                                 `user_profile_id` bigint NOT NULL AUTO_INCREMENT,
                                 `toss_user_key` varchar(100) NOT NULL,
                                 `height_cm` int NOT NULL,
                                 `weight_kg` int NOT NULL,
                                 `age` int NOT NULL,
                                 `gender` varchar(10) NOT NULL,
                                 `bmi` decimal(5,2) DEFAULT NULL,
                                 `body_fat_rate` decimal(5,2) DEFAULT NULL,
                                 `created_at` datetime NOT NULL,
                                 `updated_at` datetime NOT NULL,
                                 PRIMARY KEY (`user_profile_id`),
                                 UNIQUE KEY `uk_user_profiles_toss_user_key` (`toss_user_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `meal_plans` (
                              `meal_plan_id` bigint NOT NULL AUTO_INCREMENT,
                              `user_id` bigint NOT NULL,
                              `goal` varchar(20) NOT NULL,
                              `duration_days` int NOT NULL,
                              `height_cm` int NOT NULL,
                              `weight_kg` int NOT NULL,
                              `age` int NOT NULL,
                              `gender` varchar(10) NOT NULL,
                              `target_calories` int NOT NULL,
                              `bmr` int NOT NULL,
                              `tdee` int NOT NULL,
                              `protein_gram` int NOT NULL,
                              `carbs_gram` int NOT NULL,
                              `fat_gram` int NOT NULL,
                              `ai_response_json` json NOT NULL,
                              `ai_response_hash` varchar(64) NOT NULL,
                              `started_at` datetime NOT NULL,
                              `expires_at` datetime NOT NULL,
                              `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                              `bmi` decimal(5,2) NOT NULL,
                              `body_fat_rate` decimal(5,2) DEFAULT NULL,
                              PRIMARY KEY (`meal_plan_id`),
                              UNIQUE KEY `uk_meal_plans_user_response_hash` (`user_id`,`ai_response_hash`),
                              KEY `idx_meal_plans_user_id` (`user_id`),
                              CONSTRAINT `fk_meal_plans_user`
                                  FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `ai_generation_logs` (
                                      `generation_log_id` bigint NOT NULL AUTO_INCREMENT,
                                      `user_id` bigint NOT NULL,
                                      `goal` varchar(20) NOT NULL,
                                      `duration_days` int NOT NULL,
                                      `profile_hash` varchar(255) NOT NULL,
                                      `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`generation_log_id`),
                                      KEY `idx_generation_logs_user_created` (`user_id`,`created_at`),
                                      CONSTRAINT `fk_generation_logs_user`
                                          FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;